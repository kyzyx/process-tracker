package com.ilogues.ed.processtracker;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

/**
 * Created by edzhang on 7/30/18.
 */

@Database(entities = {JobNotificationRecord.class}, version = 1)
public abstract class JobNotificationDB extends RoomDatabase {

    public abstract JobNotificationDao jnDao();

    private static JobNotificationDB INSTANCE;


    static JobNotificationDB getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (JobNotificationDB.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            JobNotificationDB.class, "jn_database")
                            .build();

                }
            }
        }
        return INSTANCE;
    }

}