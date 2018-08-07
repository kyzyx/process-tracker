package com.ilogues.ed.processtracker;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by edzhang on 7/26/18.
 */

public class JobsList {
    public JobsList() {
        jobs = new ArrayList<Job>();
    }

    public enum Status {
        RUNNING,  ERROR, COMPLETED, DORMANT
    }
    public static Status stringToStatus(String s) {
        if (s.compareTo("Yes") == 0) return Status.COMPLETED;
        else if (s.compareTo("Error") == 0) return Status.ERROR;
        else return Status.RUNNING;
    }
    private static Date inactivetime() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, -8);
        return cal.getTime();
    }
    public class Job implements Comparable<Job>{
        public Job(String sheetName, String jobName, String started, Date lastupdated, Status status) {
            this.sheetName = sheetName;
            this.jobName = jobName;
            this.started = started;
            this.status = status;
            this.lastupdated = lastupdated;
            if (status == Status.RUNNING && lastupdated.before(inactivetime())) status = Status.DORMANT;
        }
        public boolean isCompleted() { return status == Status.COMPLETED; }
        public boolean isError() { return status == Status.ERROR; }
        public String sheetName;
        public String jobName;
        public String started;
        public Status status;
        public Date lastupdated;

        public boolean isSameJob(@NonNull Job job) {
            return jobName.compareTo(job.jobName) == 0 && started.compareTo(job.started) == 0;
        }
        @Override
        public int compareTo(@NonNull Job job) {
            int statuscmp = status.compareTo(job.status);
            if (statuscmp != 0) return statuscmp;

            if (status == Status.RUNNING) {
                return Double.parseDouble(started) < Double.parseDouble(job.started)?1:-1;
            } else {
                return lastupdated.compareTo(job.lastupdated);
            }
        }
    }

    public List<Job> jobs;

    void addJob(String sheetName, String jobName, String started, Date timestamp, Status status) {
        jobs.add(new Job(sheetName, jobName, started, timestamp, status));
    }
}
