package com.ilogues.ed.processtracker;

import java.util.ArrayList;
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
        COMPLETED, RUNNING, ERROR
    }
    public static Status stringToStatus(String s) {
        if (s.compareTo("Yes") == 0) return Status.COMPLETED;
        else if (s.compareTo("Error") == 0) return Status.ERROR;
        else return Status.RUNNING;
    }
    public class Job {
        public Job(String sheetName, String jobName, String started, Date lastupdated, Status status) {
            this.sheetName = sheetName;
            this.jobName = jobName;
            this.started = started;
            this.status = status;
            this.lastupdated = lastupdated;
        }
        public boolean isCompleted() { return status == Status.COMPLETED; }
        public boolean isError() { return status == Status.ERROR; }
        public String sheetName;
        public String jobName;
        public String started;
        public Status status;
        public Date lastupdated;
    }

    public List<Job> jobs;

    void addJob(String sheetName, String jobName, String started, Date timestamp, Status status) {
        jobs.add(new Job(sheetName, jobName, started, timestamp, status));
    }
}
