package com.ilogues.ed.processtracker;

import java.util.Date;

/**
 * Created by edzhang on 7/25/18.
 */

public class ProcessStatus {
    public String lines;
    public String status;
    public String task;
    public Date timestamp;
    public Date ETA;
    public double progress;

    public ProcessStatus() {
        this("");
    }
    public ProcessStatus(String lines) {
        this.progress = -1;
        this.lines = lines;
        this.status = "";
        this.task = "";
        this.timestamp = new Date(0L);
        this.ETA = new Date(0L);
    }
}
