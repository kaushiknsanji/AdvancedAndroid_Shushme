package com.example.android.shushme;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * @author Kaushik N Sanji
 */
public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    // COMPLETED (4) Create a GeofenceBroadcastReceiver class that extends BroadcastReceiver and override
    // onReceive() to simply log a message when called. Don't forget to add a receiver tag in the Manifest
    private static final String TAG = GeofenceBroadcastReceiver.class.getSimpleName();

    /**
     * This method is called when the BroadcastReceiver is receiving an Intent
     * broadcast.  During this time you can use the other methods on
     * BroadcastReceiver to view/modify the current result values.
     *
     * @param context The Context in which the receiver is running.
     * @param intent  The Intent being received.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        //Handles the broadcast message sent when the Geofence transition is triggered
        Log.i(TAG, "onReceive: Called");
    }

}
