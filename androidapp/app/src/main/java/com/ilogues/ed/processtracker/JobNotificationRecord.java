package com.ilogues.ed.processtracker;

/**
 * Created by edzhang on 7/30/18.
 */
import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity(tableName="jobs")
public class JobNotificationRecord {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "jobid")
    private String jobid;

    public JobNotificationRecord(@NonNull String jobid) {this.jobid = jobid;}

    public String getJobid(){return this.jobid;}
}