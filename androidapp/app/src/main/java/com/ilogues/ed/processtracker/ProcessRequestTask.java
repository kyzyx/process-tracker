package com.ilogues.ed.processtracker;

import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by edzhang on 7/25/18.
 */

public class ProcessRequestTask extends AsyncTask<Void, Void, ProcessStatus> {
    // private static String GOOGLESHEET = "https://docs.google.com/spreadsheets/d/";
    private static String SHEETSAPI = "https://sheets.googleapis.com/v4/spreadsheets/";

    private String sheetId;
    private String sheetName;
    private String apikey;
    private ProcessRequestCallback cb;
    public ProcessRequestTask(String apikey, String sheetId, String sheetName, ProcessRequestCallback cb) {
        this.sheetId = sheetId;
        this.sheetName = sheetName;
        this.apikey = apikey;
        this.cb = cb;
    }
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected ProcessStatus doInBackground(Void... voids) {
        ProcessStatus ret = new ProcessStatus();
        try {
            String range = sheetName + "!A2:E6";
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
                            String status = "";
                            String task = "";
                            String lines = "";
                            String timestamp = "";
                            double progress = 0;
                            while (reader.hasNext()) {
                                reader.beginArray();
                                timestamp = reader.nextString();
                                String line = reader.nextString();
                                lines = line + "\n" + lines;
                                if (reader.hasNext()) progress = reader.nextDouble();
                                if (reader.hasNext()) status = reader.nextString();
                                if (reader.hasNext()) task = reader.nextString();
                                reader.endArray();
                            }
                            reader.endArray();
                            ret.lines = lines;
                            ret.progress = progress;
                            ret.status = status;
                            ret.task = task;
                            ret.timestamp = timestamp;

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
        cb.processRequest(s);
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }
}
