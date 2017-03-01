package com.ianmrae.torrentviewer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Base64;
import android.util.Log;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final String TAG = "TorrentViewer";
    private static final int REFRESH_TIMER = 5000;
	private static final String USERNAME = "ian";
	private static final String PASSWORD = "TODO:ABetterWayToStoreCredentials....";

    private ListView mListView;
    private Timer mTimer;
    private TorrentAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mListView = (ListView) findViewById(R.id.torrent_list_view);
        //torrents = new JSONArray();
        adapter = new TorrentAdapter(this);
        mListView.setAdapter(adapter);

        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                getTorrentInfoThread();
            }
        }, 0, REFRESH_TIMER);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mTimer.cancel();
        Log.e(TAG, "Paused...");
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                getTorrentInfoThread();
            }
        }, 0, REFRESH_TIMER);
        Log.e(TAG, "Resumed...");
    }

    private void errorAndExit(final String err) {
        Log.e(TAG, "errorandExit: " + err);
        ErrorRunnable errorRunnable = new ErrorRunnable(this, err);
        synchronized (errorRunnable) {
            runOnUiThread(errorRunnable);
            try {
                errorRunnable.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void getTorrentInfo() {
        new Thread() {
            public void run() {
                getTorrentInfoThread();
            }
        }.start();
    }

    private void getTorrentInfoThread() {
        URL url = null;
        HttpURLConnection urlConnection = null;
        String token = null;
        String guid = null;
        String data = null;

		final String userPass = USERNAME + ":" + PASSWORD;
        final String basicAuth = "Basic " + Base64.encodeToString(userPass.getBytes(), Base64.NO_WRAP);

        try {
            url = new URL("http://pegasus:8080/gui/token.html");
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Authorization", basicAuth);
            urlConnection.setConnectTimeout(5000);

            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            data = readStream(in);

            // Parse response header for GUID Cookie
            String cookieHeader = urlConnection.getHeaderField("Set-Cookie");
            guid = cookieHeader.split(";")[0];
            System.out.println("Cookie: " + guid);

            // Parse HTML body for Token
            //System.out.println(data);
            Pattern re = Pattern.compile("^.*>(.*)</div>.*$");
            Matcher matcher = re.matcher(data);
            matcher.find();
            token = matcher.group(1);
            System.out.println("Token: " + token);

        } catch (MalformedURLException e) {
            Log.e(TAG, "BAD URL" + e);
            errorAndExit(e.toString());
        } catch (IOException e) {
            Log.e(TAG, "IO Exception" + e);
            errorAndExit(e.toString());
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        try {
            url = new URL("http://pegasus:8080/gui/?list=1&token=" + token);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Authorization", basicAuth);
            urlConnection.setRequestProperty("Cookie", guid);
            urlConnection.setConnectTimeout(5000);

            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            data = readStream(in);

        } catch (MalformedURLException e) {
            Log.e(TAG, "BAD URL" + e);
            errorAndExit(e.toString());
        } catch (IOException e) {
            Log.e(TAG, "IO Exception" + e);
            errorAndExit(e.toString());
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        try {
            JSONObject obj = new JSONObject(data);
            final JSONArray torrents = obj.getJSONArray("torrents");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.changeData(torrents);
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "JSON" + e);
        }
    }

    private String readStream(InputStream is) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            int i = is.read();
            while(i != -1) {
                bo.write(i);
                i = is.read();
            }
            return bo.toString();
        } catch (IOException e) {
            errorAndExit(e.toString());
            return "";
        }
    }
}
