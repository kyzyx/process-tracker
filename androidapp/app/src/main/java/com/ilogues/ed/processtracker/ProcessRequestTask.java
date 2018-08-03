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
 * Created by edzhang on 7/25/18.
 */

public class ProcessRequestTask extends AsyncTask<Void, Void, ProcessStatus> {
    private static String SHEETSAPI = "https://sheets.googleapis.com/v4/spreadsheets/";
    private static int DAYS_BETWEEN_EPOCHS = 25569; // Dec 30 1899 to Jan 1 1970

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

    private static Date stringToDate(String s) {
        double serialdate = Double.parseDouble(s);
        serialdate -= DAYS_BETWEEN_EPOCHS;
        long seconds = (long) (serialdate * 24 * 60 * 60);
        return new Date(seconds*1000L);
    }

    private enum Range {
        UNKNOWN, TIMESTAMP, INFO, LINES, ETA
    }
    private String readSingleCell(JsonReader reader) throws java.io.IOException {
        String ret = "";
        reader.beginArray();
        reader.beginArray();
        if (reader.hasNext()) ret = reader.nextString();
        while (reader.hasNext()) reader.skipValue();     // ignore remaining cells
        reader.endArray();
        while (reader.hasNext()) reader.skipValue();     // ignore remaining rows
        reader.endArray();
        return ret;
    }
    private String readLinesInfo(JsonReader reader) throws java.io.IOException {
        reader.beginArray();
        String lines = "";
        while (reader.hasNext()) {
            reader.beginArray();
            String line = reader.nextString();
            if (lines.isEmpty()) lines = line;
            else lines = line + "\n" + lines;
            reader.endArray();
        }
        reader.endArray();
        return lines;
    }
    private void readBaseInfo(JsonReader reader, ProcessStatus ret) throws java.io.IOException {
        reader.beginArray();
        reader.beginArray();
        ret.progress = reader.nextDouble();
        ret.status = reader.nextString();
        ret.task = reader.nextString();
        reader.endArray();
        reader.endArray();
    }
    @Override
    protected ProcessStatus doInBackground(Void... voids) {
        ProcessStatus ret = new ProcessStatus();
        try {
            String inforange = String.format("%s!C2:E2", sheetName, num_lines + 1);
            String etarange = String.format("%s!J2", sheetName);
            String timestamprange = String.format("%s!A2", sheetName);
            String linesrange = String.format("%s!B2:B%d", sheetName, num_lines + 1);

            String ranges = "ranges=" + String.join("&ranges=", inforange, etarange, timestamprange, linesrange);
            String params = "valueRenderOption=UNFORMATTED_VALUE&dateTimeRenderOption=SERIAL_NUMBER&key=" + apikey;
            URL url = new URL(SHEETSAPI + sheetId + "/values:batchGet?" + ranges + "&" + params);
            Log.i("ProcessRequestTask", url.toString());
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {
                JsonReader reader = new JsonReader(new InputStreamReader(urlConnection.getInputStream()));
                try{
                    reader.beginObject();
                    while (reader.hasNext()) {
                        String name = reader.nextName();
                        if (name.equals("spreadSheetId")) {
                            String ssid = reader.nextString();
                        } else if (name.equals("valueRanges")) {
                            reader.beginArray();
                            while (reader.hasNext()) {
                                reader.beginObject();
                                Range which = Range.UNKNOWN;
                                while (reader.hasNext()) {
                                    name = reader.nextName();
                                    if (name.equals("range")) {
                                        String s = reader.nextString();
                                        if (s.compareTo(inforange) == 0) {
                                            which = Range.INFO;
                                        } else if (s.compareTo(etarange) == 0) {
                                            which = Range.ETA;
                                        } else if (s.compareTo(timestamprange) == 0) {
                                            which = Range.TIMESTAMP;
                                        } else if (s.compareTo(linesrange) == 0) {
                                            which = Range.LINES;
                                        }
                                    } else if (name.equals("majorDimension")) {
                                        String rows = reader.nextString();
                                    } else if (name.equals("values")) {
                                        switch (which) {
                                            case ETA:
                                                ret.ETA = stringToDate(readSingleCell(reader));
                                                break;
                                            case TIMESTAMP:
                                                ret.timestamp = stringToDate(readSingleCell(reader));
                                                break;
                                            case LINES:
                                                ret.lines = readLinesInfo(reader);
                                                break;
                                            case INFO:
                                                readBaseInfo(reader, ret);
                                                break;
                                            default:
                                                reader.skipValue();
                                        }
                                    } else {
                                        reader.skipValue();
                                    }
                                }
                                reader.endObject();
                            }
                            reader.endArray();
                        }
                        else {
                            reader.skipValue();
                        }
                    }
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
