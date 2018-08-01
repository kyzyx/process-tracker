package com.ilogues.ed.processtracker;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * A simple {@link Fragment} subclass.
 */
public class ProcessViewFragment extends Fragment implements ProcessRequestCallback {

    ProcessRequestTask webtask;
    ProcessStatus lastProcessStatus;

    String jobname;

    TextView titlebar;
    TextView lines;
    TextView updatedbar;
    ProgressBar progressbar;
    Context context;
    public ProcessViewFragment() {
        // Required empty public constructor
        lastProcessStatus = new ProcessStatus();
    }

    public void setJobname(String jobname) { this.jobname = jobname; }
    public Date getLastUpdateTime() { return lastProcessStatus.timestamp; }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.processview, container);
        titlebar = v.findViewById(R.id.titleView);
        lines = v.findViewById(R.id.outputView);
        lines.setHorizontallyScrolling(true);
        progressbar = v.findViewById(R.id.taskProgressBar);
        updatedbar = v.findViewById(R.id.timestampView);
        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    void update(String apikey, String sheeturl, String sheetName) {
        webtask = new ProcessRequestTask(apikey, sheeturl, sheetName, this);
        webtask.execute();
    }

    private static boolean isErrorStatus(String status) {
        return status.length() >= 5 && status.substring(0,5).compareToIgnoreCase("error") == 0;
    }
    private static Date inactivetime() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, -8);
        return cal.getTime();
    }
    @Override
    public void processResponse(ProcessStatus status) {
        this.lastProcessStatus = status;
        String title = jobname;
        if (!status.task.isEmpty()) title += " " + status.task;
        if (!status.status.isEmpty()) title += " (" + status.status + ")";
        titlebar.setText(title);
        lines.setText(status.lines);
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG);
        String updated;
        int p = (int) Math.round(status.progress);
        progressbar.setProgress(p);
        if (p == 100) {
            titlebar.setBackgroundColor(getResources().getColor(R.color.colorComplete));
            titlebar.setTextColor(Color.BLACK);
            progressbar.setProgressDrawable(getResources().getDrawable(R.drawable.progress_complete));
            updated = "Completed";
        } else if (isErrorStatus(status.status)) {
            titlebar.setBackgroundColor(Color.RED);
            titlebar.setTextColor(Color.WHITE);
            progressbar.setProgressDrawable(getResources().getDrawable(R.drawable.progress_err));
            updated = "Terminated";
        } else if (status.timestamp.before(inactivetime())) {
            titlebar.setBackgroundColor(getResources().getColor(R.color.colorInactive));
            titlebar.setTextColor(Color.BLACK);
            progressbar.setProgressDrawable(getResources().getDrawable(R.drawable.progress_main));
            updated = "Last updated";
        } else {
            titlebar.setBackgroundColor(getResources().getColor(R.color.colorAccent));
            titlebar.setTextColor(Color.WHITE);
            progressbar.setProgressDrawable(getResources().getDrawable(R.drawable.progress_main));
            updated = "Last updated";
        }
        updatedbar.setText(updated + ": " + dateFormat.format(status.timestamp));
    }
}
