package com.ilogues.ed.processtracker;

/**
 * Created by edzhang on 7/25/18.
 */

public class ProcessStatus {
    public String lines;
    public String status;
    public String task;
    public String timestamp;
    public double progress;

    public ProcessStatus() {
        this("");
    }
    public ProcessStatus(String lines) {
        this.progress = -1;
        this.lines = lines;
        this.status = "";
        this.task = "";
        this.timestamp = "";
    }
}
