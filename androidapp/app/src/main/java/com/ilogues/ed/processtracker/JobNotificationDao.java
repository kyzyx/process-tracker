package com.ilogues.ed.processtracker;

/**
 * Created by edzhang on 7/30/18.
 */

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

@Dao
public interface JobNotificationDao {
    @Query("SELECT * FROM jobs WHERE jobid = :jobid LIMIT 1")
    JobNotificationRecord findRecord(String jobid);

    @Insert
    void insert(JobNotificationRecord record);
}
