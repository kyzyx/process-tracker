package com.ilogues.ed.processtracker;

import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by edzhang on 7/25/18.
 */

public class ProcessRequestTask extends AsyncTask<Void, Void, ProcessStatus> {
    private static String SHEETSAPI = "https://sheets.googleapis.com/v4/spreadsheets/";

    private String sheetId;
    private String sheetName;
    private String apikey;
    private ProcessRequestCallback cb;
    private int num_lines;

    public ProcessRequestTask(String apikey, String sheetId, String sheetName, ProcessRequestCallback cb) {
        this(apikey, sheetId, sheetName, cb, 5);
    }
    public ProcessRequestTask(String apikey, String sheetId, String sheetName, ProcessRequestCallback cb, int numlines) {
        this.sheetId = sheetId;
        this.sheetName = sheetName;
        this.apikey = apikey;
        this.cb = cb;
        this.num_lines = numlines;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected ProcessStatus doInBackground(Void... voids) {
        ProcessStatus ret = new ProcessStatus();
        try {
            String range = String.format("%s!A2:E%d", sheetName, num_lines+1);
            String params = "dateTimeRenderOption=FORMATTED_STRING&key=" + apikey;
            URL url = new URL(SHEETSAPI + sheetId + "/values/" + range + "?" + params);
            Log.i("ProcessRequestTask", url.getPath());
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
                            String lines = "";
                            double progress = 0;
                            while (reader.hasNext()) {
                                reader.beginArray();
                                String timestamp = reader.nextString();
                                String line = reader.nextString();
                                if (lines.isEmpty()) lines = line;
                                else lines = line + "\n" + lines;
                                String status = "";
                                String task = "";
                                if (reader.hasNext()) progress = reader.nextDouble();
                                if (reader.hasNext()) status = reader.nextString();
                                if (reader.hasNext()) task = reader.nextString();

                                if (ret.progress < 0) ret.progress = progress;
                                if (ret.timestamp.isEmpty()) ret.timestamp = timestamp;
                                if (ret.status.isEmpty()) ret.status = status;
                                if (ret.task.isEmpty()) ret.task = task;
                                reader.endArray();
                            }
                            reader.endArray();
                            ret.lines = lines;

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
            Log.i("ProcessRequestTask","Error: " + e.toString());
        }
        return ret;
    }

    @Override
    protected void onPostExecute(ProcessStatus s) {
        super.onPostExecute(s);
        cb.processResponse(s);
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }
}
