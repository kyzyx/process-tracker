package com.ilogues.ed.processtracker;

import android.content.Context;
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
 * Activities that contain this fragment must implement the
 * {@link ProcessViewFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class ProcessViewFragment extends Fragment implements ProcessRequestCallback {

    private OnFragmentInteractionListener mListener;

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
        progressbar = v.findViewById(R.id.taskProgressBar);
        return v;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void processRequest(ProcessStatus status) {
        String title = jobname;
        if (!status.task.isEmpty()) title += status.task;
        if (!status.status.isEmpty()) title += "(" + status.status + ")";
        titlebar.setText(title);
        lines.setText(status.lines);
        progressbar.setProgress((int) Math.round(status.progress));
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
