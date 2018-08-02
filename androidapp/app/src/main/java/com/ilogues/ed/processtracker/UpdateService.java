package com.ilogues.ed.processtracker;

import android.app.FragmentManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.arch.persistence.room.Update;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

public class UpdateService extends JobService {
    static final String CHANNEL_ID = "ProcessTrackerNotifications";

    JobsListRequestTask requestTask;

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
    private class NotificationTask extends AsyncTask<Void, Void, Void> {
        String jobid;
        String jobName;
        String status;
        int iconid;

        public NotificationTask(String jobName, String started) {
            this.jobid = jobName + "_" + started;
            this.jobName = jobName;
        }

        @Override
        protected Void doInBackground (Void... voids){
            JobNotificationDB db = JobNotificationDB.getDatabase(UpdateService.this);
            JobNotificationRecord rec = db.jnDao().findRecord(jobid);
            if (rec == null) {
                Log.i("UpdateService", "NotifyCompletion: notify " + jobid);
                Intent intent = new Intent(UpdateService.this, Main2Activity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(UpdateService.this, 0, intent, 0);
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(UpdateService.this, CHANNEL_ID)
                        .setSmallIcon(iconid)
                        .setContentTitle("Process " + status)
                        .setContentText("Process '" + jobName + "' " + status)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(UpdateService.this);
                rec = new JobNotificationRecord(jobid);
                notificationManager.notify(rec.hashCode(), mBuilder.build());
                db.jnDao().insert(rec);
            }
            return null;
        }
    }
    private class CompletedNotificationTask extends NotificationTask {
        public CompletedNotificationTask(String jobName, String started) {
            super(jobName, started);
            iconid = R.drawable.ic_completed;
            status = "complete";
        }
    }
    private class ErrorNotificationTask extends NotificationTask {
        public ErrorNotificationTask(String jobName, String started) {
            super(jobName, started);
            iconid = R.drawable.ic_error;
            status = "error";
        }
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String sheeturl = prefs.getString("sheets_url", "");
        final String apikey = getString(R.string.gapikey);
        if (sheeturl.isEmpty()) {
            jobFinished(params, false);
            return false;
        }
        Log.i("UpdateService", "Checking for completion");
        requestTask = new JobsListRequestTask(apikey, sheeturl, new JobsListRequestTask.JobsListRequestCallback() {
            @Override
            public void processResponse(JobsList l) {
                createNotificationChannel();
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(UpdateService.this);
                boolean notificationsenabled = prefs.getBoolean("notifications", false);
                if (notificationsenabled) {
                    for (JobsList.Job job : l.jobs) {
                        if (job.isCompleted()) {
                            NotificationTask t = new CompletedNotificationTask(job.jobName, job.started);
                            t.execute();
                        } else if (job.isError()) {
                            NotificationTask t = new ErrorNotificationTask(job.jobName, job.started);
                            t.execute();
                        }
                    }
                }
                jobFinished(params, false);
            }
        });
        requestTask.execute();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        requestTask.cancel(true);
        return true;
    }
}
