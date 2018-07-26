import gspread, os, time, re, sys
from ringbuffer import RingBuffer
from gspread.models import Cell
from oauth2client.service_account import ServiceAccountCredentials

CHECK_INTERVAL = 10   # Seconds between updates
LOGS_DIR = "." if len(sys.argv) < 2 else sys.argv[1]
LOGS_EXTENSION = ".log"
MAX_JOBS = 5
MAX_LINES = 10
SHEET_TITLE = "Process Tracker Data"

STATUS_NEEDS_AUTH = 401
UPDATED_COL = 2
COMPLETED_COL = 3
PROGRESS_COL = 4
FP_COL = 5

def findpercentprogress(line):
    progress = -1
    m = re.search("\\d*.?\\d+%", line)
    if m is not None:
        progress = float(m.group(0)[:-1])
    return progress

def findfractionalprogress(line, begin_only=True):
    m = re.findall("(?<!/)\\d+ ?/ ?\\d+(?!/)", line)
    progress = 0
    den = 1
    if len(m) > 0:
        if begin_only and not line.strip().startswith(m[0]):
            return -1
        for s in m:
            idx = s.find('/')
            den *= float(s[idx+1:])
            progress += float(s[:idx]) / den
        return 100*progress
    else:
        return -1

def findprogress(line, fractions_only_at_beginning=True):
    progress = findpercentprogress(line)
    if progress < 0:
        progress = findfractionalprogress(line, begin_only=fractions_only_at_beginning)
    return progress

def idx2sheet(idx):
    return "Sheet%d"%idx

def login():
    scope = ['https://spreadsheets.google.com/feeds',
             'https://www.googleapis.com/auth/drive']
    credentials = ServiceAccountCredentials.from_json_keyfile_name('auth.json', scope)
    return gspread.authorize(credentials)

def reauth():
    ss = None
    gc = login()
    try:
        ss = gc.open(SHEET_TITLE)
        #ss.share('edward64@gmail.com', perm_type='user', role='writer')
    except gspread.SpreadsheetNotFound:
        ss = gc.create(SHEET_TITLE)
        gc.insert_permission(ss.id, None, perm_type='anyone', role='reader')
    return ss
    

lastupdate = 0
ss = reauth()

def updatefiles():
    global lastupdate
    currtime = time.time()

    try:
        jobs = ss.worksheet("_Jobs")
        jobsmap = ss.worksheet("_JobMap")
    except gspread.WorksheetNotFound:
        jobs = ss.add_worksheet("_Jobs", 101, 4)
        jobs.append_row(["Created", "Updated", "Completed", "Progress", "FP", "Name"])
        jobsmap = ss.add_worksheet("_JobMap", MAX_JOBS+1, 3)
        jobsmap.append_row(["SheetID", "JobName", "Completed"])
        for i in range(MAX_JOBS):
            jobsmap.append_row(["Sheet%d"%(i+1), ".", "No"])

    files = [f for f in os.listdir(LOGS_DIR) if f.endswith(LOGS_EXTENSION)]

    for f in files:
        epochtime = currtime/(60*60*24) + 25569
        name = f[:-len(LOGS_EXTENSION)]
        if name == ".":
            print("Invalid job name '.'")
            continue
        f = os.path.join(LOGS_DIR, f)
        last_modified = os.stat(f).st_mtime
        if last_modified < lastupdate:
            continue

        print("Updating %s"%name)
        # Find sheet for job
        currjobs = jobsmap.col_values(2)
        try:
            idx = currjobs.index(name)
            jobsheet = ss.worksheet(idx2sheet(idx))
            jobsmap.update_cell(idx+1, 3, 'No')
        except ValueError:
            try:
                idx = currjobs.index('.')
            except ValueError:
                currcompletion = jobsmap.col_values(3)
                try:
                    idx = currcompletion.index('Yes')
                except ValueError:
                    continue
            jobsmap.update_cells([Cell(idx+1, 2, name), Cell(idx+1, 3, 'No')])
            currjobs[idx] = name
            sheetid = idx2sheet(idx)
            try:
                jobsheet = ss.worksheet(sheetid)
                jobsheet.clear()
            except gspread.WorksheetNotFound:
                jobsheet = ss.add_worksheet(sheetid, 50, 5)
            jobsheet.append_row(['Timestamp', 'Line', 'Progress', 'Status', 'Task'])
            jobs.append_row([epochtime, epochtime, '', 0, 0, name])

        # Update job info
        rowidx = jobs.find(name).row
        row = jobs.row_values(rowidx)
        completed = False
        fp = int(row[FP_COL-1])
        fd = open(f, "r")
        fd.seek(0,2)
        filelen = fd.tell()
        # TODO: Check previous N characters match previous line
        if filelen < fp:
            # This job got restarted; update status
            jobsmap.update_cells([Cell(idx+1, 2, name), Cell(idx+1, 3, 'No')])
            jobsheet.clear()
            jobsheet.append_row(['Timestamp', 'Line', 'Progress', 'Status', 'Task'])
            jobs.update_cells([Cell(rowidx, 1, epochtime),
                               Cell(rowidx, UPDATED_COL, epochtime),
                               Cell(rowidx, COMPLETED_COL, ''),
                               Cell(rowidx, PROGRESS_COL, 0),
                               Cell(rowidx, FP_COL, 0)])
            fp = 0
            percentage = 0
        else:
            percentage = float(row[PROGRESS_COL-1])

        fd.seek(fp)
        linesbuffer = RingBuffer(MAX_LINES)

        status = ""
        task = ""
        for line in fd.readlines():
            if line.strip():
                if line.startswith("status:") or line.startswith("Status:"):
                    status = line[len("status:"):].strip()
                if line.startswith("task:") or line.startswith("Task:"):
                    task = line[len("task:"):].strip()
                line_progress = findprogress(line)
                if line_progress > 0:
                    percentage = line_progress
                linesbuffer.push([epochtime, line[:-1], percentage, status if status else "=D3", task if task else "=E3"])

        for line in linesbuffer:
            jobsheet.insert_row(line, 2, value_input_option='USER_ENTERED')

        if percentage == 100:
            completed = True
            # FIXME: Other completion criteria
            jobsmap.update_cell(currjobs.index(name)+1, 3, 'Yes')

        fp = fd.tell()
        updated_cells = [
                Cell(rowidx, UPDATED_COL, epochtime),
                Cell(rowidx, PROGRESS_COL, percentage),
                Cell(rowidx, FP_COL, fp)
                ]
        if completed:
            updated_cells.append(Cell(rowidx, COMPLETED_COL, epochtime))
        jobs.update_cells(updated_cells)
    lastupdate = currtime

while True:
    try:
        updatefiles()
    except gspread.exceptions.APIError as err:
        print(err)
        print(err.response)
        if err.response.status_code == STATUS_NEEDS_AUTH:
            ss = reauth()

    time.sleep(CHECK_INTERVAL)
