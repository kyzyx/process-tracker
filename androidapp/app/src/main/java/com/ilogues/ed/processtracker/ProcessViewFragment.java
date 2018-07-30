package com.ilogues.ed.processtracker;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * A simple {@link Fragment} subclass.
 */
public class ProcessViewFragment extends Fragment implements ProcessRequestCallback {

    ProcessRequestTask webtask;

    String jobname;

    TextView titlebar;
    TextView lines;
    ProgressBar progressbar;

    public ProcessViewFragment() {
        // Required empty public constructor
    }

    public void setJobname(String jobname) { this.jobname = jobname; }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.processview, container);
        titlebar = v.findViewById(R.id.titleView);
        lines = v.findViewById(R.id.outputView);
        lines.setHorizontallyScrolling(true);
        progressbar = v.findViewById(R.id.taskProgressBar);
        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    void update(String apikey, String sheeturl, String sheetName) {
        webtask = new ProcessRequestTask(apikey, sheeturl, sheetName, this);
        webtask.execute();
    }

    @Override
    public void processResponse(ProcessStatus status) {
        String title = jobname;
        if (!status.task.isEmpty()) title += status.task;
        if (!status.status.isEmpty()) title += "(" + status.status + ")";
        titlebar.setText(title);
        lines.setText(status.lines);
        int p = (int) Math.round(status.progress);
        progressbar.setProgress(p);
        if (p == 100) {
            titlebar.setBackgroundColor(Color.LTGRAY);
            titlebar.setTextColor(Color.BLACK);
        } else if (status.status.compareToIgnoreCase("error") == 0) {
            titlebar.setBackgroundColor(Color.RED);
            titlebar.setTextColor(Color.WHITE);
        } else {
            titlebar.setBackgroundColor(getResources().getColor(R.color.colorAccent));
            titlebar.setTextColor(Color.WHITE);
        }
    }
}
