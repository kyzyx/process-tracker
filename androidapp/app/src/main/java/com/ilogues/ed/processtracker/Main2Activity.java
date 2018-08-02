package com.ilogues.ed.processtracker;

import android.app.FragmentManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.Menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Main2Activity extends AppCompatActivity {
    JobsListRequestTask requestTask;
    Handler handler;

    static final int JOB_ID = 7428;
    static final int IMMEDIATE_JOB_ID = 7429;
    List<ProcessViewFragment> frags;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        setTitle("Process Tracker");
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Main2Activity.this);
        FragmentManager fm = getFragmentManager();
        frags = new ArrayList<>();
        frags.add((ProcessViewFragment) fm.findFragmentById(R.id.processview1));
        frags.add((ProcessViewFragment) fm.findFragmentById(R.id.processview2));
        frags.add((ProcessViewFragment) fm.findFragmentById(R.id.processview3));
        frags.add((ProcessViewFragment) fm.findFragmentById(R.id.processview4));

        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                onRefreshClick(null);

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Main2Activity.this);
                int delay_s = Math.max(Integer.parseInt(prefs.getString("refresh_every", "10")), 10);
                Log.i("Main2Activity", String.format("delay: %d s", delay_s));
                handler.postDelayed(this, delay_s*1000);
            }
        }, 1000);
        JobScheduler jobScheduler =
                (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(new JobInfo.Builder(JOB_ID,
                new ComponentName(this, UpdateService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(JobInfo.getMinPeriodMillis(), JobInfo.getMinFlexMillis())
                .setPersisted(true)
                .build());
    }

    public void checkNotificationsImmediate() {
        JobScheduler jobScheduler =
                (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(new JobInfo.Builder(IMMEDIATE_JOB_ID,
                new ComponentName(this, UpdateService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(1)
                .setOverrideDeadline(1)
                .build());
    }

    // Menu icons are inflated just as they were with actionbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.actionbarmenu, menu);
        return true;
    }

    public void onRefreshClick(MenuItem mi) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String sheeturl = prefs.getString("sheets_url", "");
        final String apikey = getString(R.string.gapikey);

        if (sheeturl.isEmpty()) return;
        requestTask = new JobsListRequestTask(apikey, sheeturl, new JobsListRequestTask.JobsListRequestCallback() {
            @Override
            public void processResponse(JobsList l) {
                Collections.sort(l.jobs);
                for (int i = 0; i < frags.size(); i++) {
                    ProcessViewFragment v = frags.get(i);
                    if (i < l.jobs.size()) {
                        if (l.jobs.get(i).status != JobsList.Status.RUNNING) checkNotificationsImmediate();
                        v.setJobname(l.jobs.get(i).jobName);
                        if (v.getLastUpdateTime().before(l.jobs.get(i).lastupdated))
                            v.update(apikey, sheeturl, l.jobs.get(i).sheetName);
                    } else {
                        FragmentManager fm = getFragmentManager();
                        fm.beginTransaction().hide(v).commitAllowingStateLoss();
                    }
                }
            }
        });
        requestTask.execute();
    }
    public void onSettingsClick(MenuItem mi) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
}
