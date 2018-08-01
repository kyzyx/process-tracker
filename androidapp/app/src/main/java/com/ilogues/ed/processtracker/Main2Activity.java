package com.ilogues.ed.processtracker;

import android.app.FragmentManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import java.util.List;

public class Main2Activity extends AppCompatActivity {
    JobsListRequestTask requestTask;
    Handler handler;

    static final String CHANNEL_ID = "ProcessTrackerNotifications";
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
        createNotificationChannel();
    }

    // Menu icons are inflated just as they were with actionbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.actionbarmenu, menu);
        return true;
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    private class NotificationTask extends AsyncTask<Void, Void, Void>
    {
        String jobid;
        String jobName;

        public NotificationTask(String jobName, String started) {
            this.jobid = jobName + "_" + started;
            this.jobName = jobName;
        }

        @Override
        protected Void doInBackground (Void... voids){
            JobNotificationDB db = JobNotificationDB.getDatabase(Main2Activity.this);
            JobNotificationRecord rec = db.jnDao().findRecord(jobid);
            if (rec == null) {
                Log.i("Main2Activity", "NotifyCompletion: notify " + jobid);
                // Create an explicit intent for an Activity in your app
                Intent intent = new Intent(Main2Activity.this, Main2Activity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(Main2Activity.this, 0, intent, 0);
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(Main2Activity.this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_completed)
                        .setContentTitle("Process Completed")
                        .setContentText("Process '" + jobName + "' completed.")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);;
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(Main2Activity.this);
                rec = new JobNotificationRecord(jobid);
                notificationManager.notify(rec.hashCode(), mBuilder.build());
                db.jnDao().insert(rec);
            }
            return null;
        }
    }
    private void NotifyCompletion(String jobName, String started) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean notificationsenabled = prefs.getBoolean("notifications", false);
        if (notificationsenabled) {
            NotificationTask t = new NotificationTask(jobName, started);
            t.execute();
        }
    }

    public void onRefreshClick(MenuItem mi) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String sheeturl = prefs.getString("sheets_url", "");
        final String apikey = getString(R.string.gapikey);

        if (sheeturl.isEmpty()) return;
        requestTask = new JobsListRequestTask(apikey, sheeturl, new JobsListRequestTask.JobsListRequestCallback() {
            @Override
            public void processResponse(JobsList l) {
                // TODO: Sort by updated? (or do that in request)
                // TODO: Keep the same fragment for the same job for display settings
                for (JobsList.Job job : l.jobs) {
                    if (job.completed) NotifyCompletion(job.jobName, job.started);
                }
                for (int i = 0; i < frags.size(); i++) {
                    ProcessViewFragment v = frags.get(i);
                    if (i < l.jobs.size()) {
                        v.setJobname(l.jobs.get(i).jobName);
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
