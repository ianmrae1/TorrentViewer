package com.ianmrae.torrentviewer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;

/**
 * Created by ian on 1/30/17.
 */

public class ErrorRunnable implements Runnable {
    private String mError;
    private Activity mActivity;

    public ErrorRunnable(Activity activity, String err) {
        mActivity = activity;
        mError = err;
    }

    @Override
    public void run() {
        AlertDialog alertDialog = new AlertDialog.Builder(mActivity).create();
        alertDialog.setTitle("Error");
        alertDialog.setMessage(mError);
        alertDialog.setIcon(android.R.drawable.ic_dialog_alert);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Exit",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Log.e("ErrorRunnable", "clicked");
                        mActivity.finish();
                    }
                });
        alertDialog.show();
    }
}

