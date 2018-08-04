package com.ilogues.ed.processtracker;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.DateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class ProcessViewFragment extends Fragment implements ProcessRequestCallback {

    ProcessRequestTask webtask;
    ProcessStatus lastProcessStatus;

    String jobname;

    TextView titlebar;
    TextView lines;
    TextView updatedbar;
    Chronometer etabar;
    boolean iscounting;
    ProgressBar progressbar;
    Context context;

    List<DoneUpdatingObserver> observers;
    public interface DoneUpdatingObserver {
        void DoneUpdating();
    }

    public ProcessViewFragment() {
        // Required empty public constructor
        lastProcessStatus = new ProcessStatus();
        observers = new ArrayList<>();
    }

    public void setJobname(String jobname) { this.jobname = jobname; }
    public Date getLastUpdateTime() { return lastProcessStatus.timestamp; }
    public void addDoneUpdatingObserver(DoneUpdatingObserver obs) {
        observers.add(obs);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        iscounting = false;
        View v = inflater.inflate(R.layout.processview, container);
        titlebar = v.findViewById(R.id.titleView);
        lines = v.findViewById(R.id.outputView);
        lines.setHorizontallyScrolling(true);
        progressbar = v.findViewById(R.id.taskProgressBar);
        updatedbar = v.findViewById(R.id.timestampView);
        etabar = v.findViewById(R.id.etaView);
        etabar.setCountDown(true);
        etabar.setTextColor(getResources().getColor(R.color.colorTimestampBar));
        etabar.setFormat("");
        etabar.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            public void onChronometerTick(Chronometer cArg) {
                long time = SystemClock.elapsedRealtime() - cArg.getBase();
                if (cArg.isCountDown()) time = -time;
                int h = (int) (time / 3600000);
                int m = (int) (time - h * 3600000) / 60000;
                String timer = String.format("%02d:%02d", h, m);
                cArg.setText(String.format(cArg.getFormat(), timer));
                if (time > 0) cArg.setTextColor(getResources().getColor(R.color.colorTimestampText));
                else cArg.setTextColor(getResources().getColor(R.color.colorTimestampBar));
            }
        });
        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (iscounting) {
            etabar.stop();
            iscounting = false;
        }
    }

    void update(String apikey, String sheeturl, String sheetName) {
        webtask = new ProcessRequestTask(apikey, sheeturl, sheetName, this);
        webtask.execute();
    }

    private static boolean isErrorStatus(String status) {
        return status.length() >= 5 && status.substring(0,5).compareToIgnoreCase("error") == 0;
    }
    private static Date hoursago(int hours) {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, -hours);
        return cal.getTime();
    }
    @Override
    public void processResponse(ProcessStatus status) {
        this.lastProcessStatus = status;
        String title = jobname;
        if (!status.task.isEmpty()) title += " " + status.task;
        if (!status.status.isEmpty()) title += " (" + status.status + ")";
        titlebar.setText(title);
        lines.setText(status.lines);
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG);
        String updated;
        int p = (int) Math.round(status.progress);
        progressbar.setProgress(p);
        etabar.setTextColor(getResources().getColor(R.color.colorTimestampBar));
        if (status.progress == 100) {
            if (status.timestamp.before(hoursago(16))) titlebar.setBackgroundColor(getResources().getColor(R.color.colorInactive));
            else titlebar.setBackgroundColor(getResources().getColor(R.color.colorComplete));
            titlebar.setTextColor(Color.BLACK);
            progressbar.setProgressDrawable(getResources().getDrawable(R.drawable.progress_complete));
            if (iscounting) {
                etabar.stop();
                iscounting = false;
            }
            updated = "Completed";
        } else if (isErrorStatus(status.status)) {
            titlebar.setBackgroundColor(Color.RED);
            titlebar.setTextColor(Color.WHITE);
            progressbar.setProgressDrawable(getResources().getDrawable(R.drawable.progress_err));
            if (iscounting) {
                etabar.stop();
                iscounting = false;
            }
            updated = "Terminated";
        } else if (status.timestamp.before(hoursago(8))) {
            titlebar.setBackgroundColor(getResources().getColor(R.color.colorInactive));
            titlebar.setTextColor(Color.BLACK);
            progressbar.setProgressDrawable(getResources().getDrawable(R.drawable.progress_main));
            if (iscounting) {
                etabar.stop();
                iscounting = false;
            }
            updated = "Last updated";
        } else {
            titlebar.setBackgroundColor(getResources().getColor(R.color.colorAccent));
            titlebar.setTextColor(Color.WHITE);
            progressbar.setProgressDrawable(getResources().getDrawable(R.drawable.progress_main));
            updated = "Last updated";
            if (status.timestamp.before(status.ETA)) {
                long boottime = (new Date()).getTime() - SystemClock.elapsedRealtime();
                etabar.setBase(status.ETA.getTime() - boottime);
                long remaining = (status.ETA.getTime() - status.timestamp.getTime()) / (1000);
                if (remaining < 60) etabar.setFormat("<1 min remaining");
                else {
                    etabar.setFormat("%s remaining");
                    if (!iscounting) {
                        etabar.start();
                        iscounting = true;
                    }
                }
            }
        }
        updatedbar.setText(updated + ": " + dateFormat.format(status.timestamp));
        for (DoneUpdatingObserver obs : observers) {
            obs.DoneUpdating();
        }
    }
}
