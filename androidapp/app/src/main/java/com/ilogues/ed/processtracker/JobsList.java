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

    public class Job {
        public Job(String sheetName, String jobName, boolean completed, String started, Date lastupdated) {
            this.sheetName = sheetName;
            this.jobName = jobName;
            this.started = started;
            this.completed = completed;
            this.lastupdated = lastupdated;
        }
        public String sheetName;
        public String jobName;
        public String started;
        public boolean completed;
        public Date lastupdated;
    }

    public List<Job> jobs;

    void addJob(String sheetName, String jobName, boolean completed, String started, Date timestamp) {
        jobs.add(new Job(sheetName, jobName, completed, started, timestamp));
    }
}
