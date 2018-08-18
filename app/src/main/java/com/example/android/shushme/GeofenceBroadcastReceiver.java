package com.example.android.shushme;

/*
* Copyright (C) 2017 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*  	http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.concurrent.Executors;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    //Constant used for logs
    public static final String TAG = GeofenceBroadcastReceiver.class.getSimpleName();

    public static final int APP_PENDING_INTENT_ID = 5;
    public static final int APP_NOTIFICATION_ID = 5;
    public static final String APP_NOTIFICATION_CHANNEL_STR_ID = BuildConfig.APPLICATION_ID;

    /***
     * Handles the Broadcast message sent when the Geofence Transition is triggered
     * Careful here though, this is running on the main thread so make sure you start an AsyncTask for
     * anything that takes longer than say 10 second to run
     *
     * @param context The Context in which the receiver is running.
     * @param intent The Intent being received.
     */
    @Override
    public void onReceive(final Context context, final Intent intent) {
        Log.i(TAG, "onReceive called");

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // COMPLETED (4) Use GeofencingEvent.fromIntent to retrieve the GeofencingEvent that caused the transition
                //Retrieve the GeofencingEvent that caused the transition
                GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
                if(geofencingEvent.hasError()){
                    //On Error, log the error and exit
                    Log.e(TAG, "onReceive: Geofence Event Error Code " + geofencingEvent.getErrorCode());
                    return;
                }

                // COMPLETED (5) Call getGeofenceTransition to get the transition type and use AudioManager to set the
                // phone ringer mode based on the transition type. Feel free to create a helper method (setRingerMode)
                //Get the Geofence Transition Type
                int geofenceTransition = geofencingEvent.getGeofenceTransition();
                if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER){
                    //Set Ringer Mode to Silent when the device enters the Geofence
                    setRingerMode(context, AudioManager.RINGER_MODE_SILENT);
                } else if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT){
                    //Set Ringer Mode to Normal when the device leaves the Geofence
                    setRingerMode(context, AudioManager.RINGER_MODE_NORMAL);
                } else {
                    //On Unknown Transition
                    //Log the error
                    Log.e(TAG, "onReceive: Unknown Transition " + geofenceTransition);
                    //Exit on unknown transition
                    return;
                }

                // COMPLETED (6) Show a notification to alert the user that the ringer mode has changed.
                // Feel free to create a helper method (sendNotification)

                //Show a notification to alert the user that the ringer mode has changed.
                sendNotification(context, geofenceTransition);
            }
        });

    }

    /**
     * Alters the ringer mode on the device to either silent or back to normal
     *
     * @param context The context to access the NOTIFICATION_SERVICE and AUDIO_SERVICE
     * @param mode The desired ringer mode to switch to on the device. This can be
     *             {@link AudioManager#RINGER_MODE_SILENT} or {@link AudioManager#RINGER_MODE_NORMAL}
     */
    private void setRingerMode(Context context, int mode){
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        //Check for DND permissions on Android M and above
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager != null && !notificationManager.isNotificationPolicyAccessGranted())){
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if(audioManager != null){
                audioManager.setRingerMode(mode);
            }
        }
    }

    /**
     * Creates and dispatches a notification to the device depending
     * on the Geofence Transition {@code transitionType}
     *
     * @param context The context to access the NOTIFICATION_SERVICE to build the Notification content
     * @param transitionType The Geofence Transition type which can be either {@link Geofence#GEOFENCE_TRANSITION_ENTER}
     *                       or {@link Geofence#GEOFENCE_TRANSITION_EXIT}
     */
    private void sendNotification(Context context, int transitionType){
        //Retrieving the instance of NotificationManager to notify the user of the events
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(notificationManager == null){
            //Quit when we cannot get the NotificationManager instance
            return;
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            //Building the Notification Channel for devices with Android API level 26+
            NotificationChannel notificationChannel = new NotificationChannel(
                    //Unique string of the Notification Channel
                    APP_NOTIFICATION_CHANNEL_STR_ID,
                    //The user visible name of the Channel
                    context.getString(R.string.app_notification_channel_name),
                    //High importance to show Heads-up notification
                    NotificationManager.IMPORTANCE_HIGH
            );

            //Registering the channel with the system
            notificationManager.createNotificationChannel(notificationChannel);
        }

        //Constructing the Notification content with the NotificationCompat.Builder
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, APP_NOTIFICATION_CHANNEL_STR_ID)
                .setAutoCancel(true) //Clears/Cancels Notification on click
                .setContentIntent(getContentIntent(context)) //PendingIntent for the Notification
                //Sets the Context text (second row) of the Notification
                .setContentText(context.getString(R.string.touch_to_relaunch_notification_text));

        //Building the rest of the Notification content based on the Transition type
        if(transitionType == Geofence.GEOFENCE_TRANSITION_ENTER){
            //When the device enters the Geofence, it goes into Silent Mode
            notificationBuilder.setSmallIcon(R.drawable.ic_volume_off_white_24dp) //Sets the Small Icon
                    //Sets the Large Icon
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_volume_off_white_24dp))
                    //Sets the Title (first row) of the Notification
                    .setContentTitle(context.getString(R.string.silent_mode_activated_notification_title));
        } else if(transitionType == Geofence.GEOFENCE_TRANSITION_EXIT){
            //When the device leaves the Geofence, it goes into Normal Mode
            notificationBuilder.setSmallIcon(R.drawable.ic_volume_up_white_24dp)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_volume_up_white_24dp))
                    .setContentTitle(context.getString(R.string.back_to_normal_notification_title));
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.O){
            //Specifying the Notification Importance through priority for devices
            //with Android API level 16+ and less than 26
            notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        }

        //Post a Notification with the Notification content built
        notificationManager.notify(
                //Unique ID for this notification of the app, which can be used
                //for updating/removing the notification if required
                APP_NOTIFICATION_ID,
                //Notification object shown to the user
                notificationBuilder.build()
        );
    }

    /**
     * Method that prepares and returns a {@link PendingIntent} for the Notification
     * to launch the {@link MainActivity}
     *
     * @param context {@link Context} to create an {@link Intent}
     *
     * @return Instance of {@link PendingIntent} that launches the {@link MainActivity}
     */
    @NonNull
    private PendingIntent getContentIntent(Context context){
        //Define an explicit Intent to launch the MainActivity
        Intent mainActivityIntent = new Intent(context, MainActivity.class);
        //Launch in a separate task if not found and clear the task stack to the root activity if found
        mainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        //Returning the PendingIntent for the Notification
        return PendingIntent.getActivity(context,
                APP_PENDING_INTENT_ID,
                mainActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
    }

}
