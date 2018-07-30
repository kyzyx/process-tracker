package com.ilogues.ed.processtracker;

import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
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

        requestTask = new JobsListRequestTask(apikey, sheeturl, new JobsListRequestTask.JobsListRequestCallback() {
            @Override
            public void processResponse(JobsList l) {
                // TODO: Sort by updated? (or do that in request)
                // TODO: Keep the same fragment for the same job for display settings
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
