package com.ilogues.ed.processtracker;

import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.Menu;

public class Main2Activity extends AppCompatActivity {
    ProcessRequestTask webtask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        setTitle("Process Tracker");
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
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
        ProcessViewFragment v = (ProcessViewFragment) getFragmentManager().findFragmentById(R.id.processview1);
        v.setJobname("train0");
        webtask = new ProcessRequestTask(getString(R.string.gapikey), prefs.getString("sheets_url", ""), "Sheet1", v);
        webtask.execute();
    }
    public void onSettingsClick(MenuItem mi) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
}
