package com.ianmrae.torrentviewer;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Created by ian on 1/27/17.
 */

public class TorrentAdapter extends BaseAdapter {
    private static final int STATUS_STARTED = 1;
    private static final int STATUS_PAUSED = 32;
    private static final int STATUS_QUEUED = 64;
    private static final int STATUS_LOADED = 128;

    private static final String TAG = "TorrentAdapter";
    private Context mContext;
    private LayoutInflater mInflater;
    private JSONArray mDataSource;

    public TorrentAdapter(Context context, JSONArray torrents) {
        mContext = context;
        changeData(torrents);
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public TorrentAdapter(Context context) {
        mContext = context;
        mDataSource = new JSONArray();
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void changeData(JSONArray torrents)
    {
        List<JSONArray> jsonValues = new ArrayList<JSONArray>();

        mDataSource = new JSONArray();

        for (int i=0; i < torrents.length(); i++) {
            try {
                jsonValues.add(torrents.getJSONArray(i));
            } catch (JSONException e) {
                Log.e(TAG, "JSON" + e);
            }
        }

        Collections.sort(jsonValues, new Comparator<JSONArray>() {
            @Override
            public int compare(JSONArray a, JSONArray b) {
                String titleA = new String();
                String titleB = new String();
                boolean startedA = false;
                boolean startedB = false;
                int percentA = 0;
                int percentB = 0;

                try {
                    titleA = a.getString(2);
                    titleB = b.getString(2);

                    startedA = (a.getInt(1) & STATUS_STARTED) != 0;
                    startedB = (b.getInt(1) & STATUS_STARTED) != 0;

                    percentA = a.getInt(4);
                    percentB = b.getInt(4);
                }
                catch (JSONException e) {
                    Log.e(TAG, "JSON" + e);
                }

                if ( (startedA && percentA < 1000) || (startedB && percentB < 1000)) {
                    if (startedA && !startedB) {
                        return -1;
                    } else if (startedB && !startedA) {
                        return 1;
                    } else if (startedA && startedB) {
                        // both started, go by alpha
                        return titleA.compareToIgnoreCase(titleB);
                    }
                }
                return titleA.compareToIgnoreCase(titleB);
            }
        });

        for (int i = 0; i < torrents.length(); i++) {
            mDataSource.put(jsonValues.get(i));
        }

        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mDataSource.length();
    }

    @Override
    public Object getItem(int position) {
        try {
            return mDataSource.getJSONArray(position);
        } catch (JSONException e) {
            Log.e(TAG, "JSON" + e);
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get view for row item
        View rowView = mInflater.inflate(R.layout.list_item_torrent, parent, false);

        // Get title element
        TextView titleTextView =
                (TextView) rowView.findViewById(com.ianmrae.torrentviewer.R.id.torrent_list_title);

        // Get subtitle element
        TextView subtitleTextView =
                (TextView) rowView.findViewById(com.ianmrae.torrentviewer.R.id.torrent_list_subtitle);

        // Get detail element
        TextView detailTextView =
                (TextView) rowView.findViewById(com.ianmrae.torrentviewer.R.id.torrent_list_detail);

        // Get thumbnail element
        ImageView thumbnailImageView =
                (ImageView) rowView.findViewById(com.ianmrae.torrentviewer.R.id.torrent_list_thumbnail);

        try {
            JSONArray torrent = (JSONArray) getItem(position);

            titleTextView.setText(torrent.getString(2));
            subtitleTextView.setText(torrent.getString(21));

            int percent = torrent.getInt(4);
            boolean started = (torrent.getInt(1) & STATUS_STARTED) != 0;
            if (percent == 1000) {
                thumbnailImageView.setImageResource(R.drawable.ic_done);
            } else if (started) {
                thumbnailImageView.setImageResource(R.drawable.ic_downloading);
            }

            int eta = torrent.getInt(10);

            if (eta > 0) {
                int day = (int) TimeUnit.SECONDS.toDays(eta);
                long hour = TimeUnit.SECONDS.toHours(eta) - (day *24);
                long minute = TimeUnit.SECONDS.toMinutes(eta) - (TimeUnit.SECONDS.toHours(eta)* 60);
                long second = TimeUnit.SECONDS.toSeconds(eta) - (TimeUnit.SECONDS.toMinutes(eta) *60);

                String eta_str;// = new String();

                if (day > 0) {
                    eta_str = String.format(Locale.US, "%dd %dh", day, hour);
                } else if (hour > 0) {
                    eta_str = String.format(Locale.US, "%dh %dm", hour, minute);
                } else if (minute > 0) {
                    eta_str = String.format(Locale.US, "%dm %ds", minute, second);
                } else {
                    eta_str = String.format(Locale.US, "%ds", second);
                }

                detailTextView.setText(eta_str);
            }

        } catch (JSONException e) {
            Log.e(TAG, "JSON" + e);
        }

        return rowView;
    }
}
