package com.ilogues.ed.processtracker;

import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

/**
 * Created by edzhang on 7/26/18.
 */

public class JobsListRequestTask extends AsyncTask<Void, Void, JobsList> {
    private static String SHEETSAPI = "https://sheets.googleapis.com/v4/spreadsheets/";
    private static int DAYS_BETWEEN_EPOCHS = 25569; // Dec 30 1899 to Jan 1 1970

    private String apikey;
    private String sheetId;
    private JobsListRequestCallback cb;

    public interface JobsListRequestCallback {
        void processResponse(JobsList l);
    }

    public JobsListRequestTask(String apikey, String sheetId, JobsListRequestCallback cb) {
        this.apikey = apikey;
        this.cb = cb;
        this.sheetId = sheetId;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected JobsList doInBackground(Void... voids) {
        JobsList ret = new JobsList();
        try {
            String range = String.format("_JobMap!A2:E6");
            String params = "valueRenderOption=UNFORMATTED_VALUE&dateTimeRenderOption=SERIAL_NUMBER&key=" + apikey;
            URL url = new URL(SHEETSAPI + sheetId + "/values/" + range + "?" + params);
            Log.i("JobsListRequestTask", url.getPath());
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {
                JsonReader reader = new JsonReader(new InputStreamReader(urlConnection.getInputStream()));
                try{
                    reader.beginObject();
                    while (reader.hasNext()) {
                        String name = reader.nextName();
                        if (name.equals("range")) {
                            String cellrange = reader.nextString();
                        } else if (name.equals("majorDimension")) {
                            String dimension = reader.nextString();
                        } else if (name.equals("values") && reader.peek() != JsonToken.NULL) {
                            reader.beginArray();
                            while (reader.hasNext()) {
                                reader.beginArray();
                                String sheetName = reader.nextString();
                                String jobName = reader.nextString();
                                String completed = reader.nextString();
                                String started = "0";
                                if (reader.hasNext()) started = reader.nextString();
                                double serialdate = DAYS_BETWEEN_EPOCHS;
                                if (reader.hasNext()) serialdate = Double.parseDouble(reader.nextString());
                                if (jobName.compareTo(".") != 0) {
                                    serialdate -= DAYS_BETWEEN_EPOCHS;
                                    long seconds = (long) (serialdate * 24 * 60 * 60);
                                    Date timestamp = new Date(seconds*1000L);

                                    ret.addJob(sheetName, jobName, started, timestamp, JobsList.stringToStatus(completed));
                                }
                                reader.endArray();
                            }
                            reader.endArray();
                        } else {
                            reader.skipValue();
                        }
                    }
                    reader.endObject();
                } finally {
                    reader.close();
                }
            }
            finally {
                urlConnection.disconnect();
            }

        } catch (Exception e) {
            Log.i("JobsListRequestTask","Error: " + e.toString());
        }
        return ret;
    }

    @Override
    protected void onPostExecute(JobsList l) {
        super.onPostExecute(l);
        cb.processResponse(l);
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }
}
