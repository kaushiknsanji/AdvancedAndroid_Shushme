package com.example.android.shushme;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBufferResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kaushik N Sanji
 */
public class Geofencing implements OnCompleteListener<Void> {
    // COMPLETED (1) Create a Geofencing class with a Context and GoogleApiClient constructor that
    // initializes a private member ArrayList of Geofences called mGeofenceList

    //Constants
    private static final String TAG = Geofencing.class.getSimpleName();
    private static final long GEOFENCE_TIMEOUT = 24 * 60 * 60 * 1000; //24Hrs
    private static final float GEOFENCE_RADIUS = 50.0f; //50 metres
    private static final int GEOFENCE_PENDING_INTENT_REQUEST = 0;

    //Members
    private final Context mContext;
    private final GoogleApiClient mGoogleApiClient;
    private List<Geofence> mGeofenceList;
    private PendingIntent mGeofencePendingIntent;


    public Geofencing(Context context, GoogleApiClient googleApiClient) {
        mContext = context;
        mGoogleApiClient = googleApiClient;

        //Initializing the mGeofenceList
        mGeofenceList = new ArrayList<>();
    }

    // COMPLETED (2) Inside Geofencing, implement a public method called updateGeofencesList that
    // given a PlaceBuffer will create a Geofence object for each Place using Geofence.Builder
    // and add that Geofence to mGeofenceList

    /**
     * Method updates the ArrayList of {@link Geofence} using the data passed in {@code places}.
     * Uses the Place ID defined by the Places API as the object ID of the Geofence.
     *
     * @param places Instance of {@link PlaceBufferResponse} containing a collection of {@link Place}
     *               information
     */
    public void updateGeofencesList(PlaceBufferResponse places){
        if(places.getCount() > 0){
            for (Place place : places) {
                //Get the Place object
                //Build a Geofence object using the Place information
                Geofence geofence = new Geofence.Builder()
                        //A unique ID for the request
                        .setRequestId(place.getId())
                        //Expiration for the Geofence set to 24Hrs
                        .setExpirationDuration(GEOFENCE_TIMEOUT)
                        //Geofence perimeter with radius of 50metres
                        .setCircularRegion(
                                place.getLatLng().latitude,
                                place.getLatLng().longitude,
                                GEOFENCE_RADIUS
                        )
                        //Interested in Enter and Exit Geofence transition
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                        .build();
                //Add it to the list of Geofences
                mGeofenceList.add(geofence);
            }
        }
    }

    // COMPLETED (3) Inside Geofencing, implement a private helper method called getGeofencingRequest that
    // uses GeofencingRequest.Builder to return a GeofencingRequest object from the Geofence list

    /**
     * Method that creates and returns a {@link GeofencingRequest} object using an ArrayList of
     * {@link Geofence}.
     *
     * @return Instance of {@link GeofencingRequest}
     */
    private GeofencingRequest getGeofencingRequest(){
        //Preparing and returning the GeofencingRequest
        return new GeofencingRequest.Builder()
                //triggers an entry event transition immediately if
                //already in a Geofence at the time of registering
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                //attach the list of Geofences we are interested in
                .addGeofences(mGeofenceList)
                .build(); //Build the GeofencingRequest
    }

    // COMPLETED (5) Inside Geofencing, implement a private helper method called getGeofencePendingIntent that
    // returns a PendingIntent for the GeofenceBroadcastReceiver class

    /**
     * Method that creates and returns a {@link PendingIntent} for launching an IntentService
     * when a Geofence event occurs.
     *
     * @return Instance of {@link PendingIntent}
     */
    private PendingIntent getGeofencePendingIntent(){
        //Reuse the PendingIntent if we already have it
        if(mGeofencePendingIntent != null){
            return mGeofencePendingIntent;
        }
        //Creating a Broadcast Intent
        Intent broadcastIntent = new Intent(mContext, GeofenceBroadcastReceiver.class);
        //Creating a Pending Intent for the Broadcast Intent
        mGeofencePendingIntent = PendingIntent.getBroadcast(mContext,
                GEOFENCE_PENDING_INTENT_REQUEST, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        //Returning the Pending Intent
        return mGeofencePendingIntent;
    }

    // COMPLETED (6) Inside Geofencing, implement a public method called registerAllGeofences that
    // registers the GeofencingRequest by calling LocationServices.GeofencingApi.addGeofences
    // using the helper functions getGeofencingRequest() and getGeofencePendingIntent()

    /**
     * Method that registers all the Geofences created by this app with the Google Location Services API
     * using the {@link GeofencingRequest} built using {@link #getGeofencingRequest()}
     * and the Geofence {@link PendingIntent} built using {@link #getGeofencePendingIntent()}
     * to launch the IntentService when the Geofence is triggered.
     *
     * <p>Triggers {@link #onComplete(Task)}</p> when all the Geofences have been registered successfully.
     */
    @SuppressLint("MissingPermission")
    public void registerAllGeofences(){
        //Check the Google API Client is initialized and connected,
        //and that the list has Geofences in it
        if(mGoogleApiClient == null || !mGoogleApiClient.isConnected()
                || mGeofenceList == null || mGeofenceList.size() == 0){
            return;
        }

        //Registering the Geofences with the Google LocationServices API
        try{
            LocationServices.getGeofencingClient(mContext)
                    .addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                    .addOnCompleteListener(this);
        } catch (SecurityException securityException) {
            //Catch the exception generated if the App does not use ACCESS_FINE_LOCATION permission
            Log.e(TAG, "registerAllGeofences: " + securityException.getMessage(), securityException);
        }

    }

    // COMPLETED (7) Inside Geofencing, implement a public method called unRegisterAllGeofences that
    // unregisters all geofences by calling LocationServices.GeofencingApi.removeGeofences
    // using the helper function getGeofencePendingIntent()

    /**
     * Method that unregisters all the Geofences created by this app with the Google Location Services API
     * using the Geofence {@link PendingIntent} built using {@link #getGeofencePendingIntent()}
     * that was passed while registering the Geofences in the first place.
     * <p>Triggers {@link #onComplete(Task)}</p> when all the Geofences have been unregistered successfully.
     */
    public void unRegisterAllGeofences(){
        //Check the Google API Client is initialized and connected
        if(mGoogleApiClient == null || !mGoogleApiClient.isConnected()){
            return;
        }

        //UnRegistering the Geofences with the Google LocationServices API
        try{
            LocationServices.getGeofencingClient(mContext)
                    .removeGeofences(getGeofencePendingIntent())
                    .addOnCompleteListener(this);
        } catch (SecurityException securityException) {
            //Catch the exception generated if the App does not use ACCESS_FINE_LOCATION permission
            Log.e(TAG, "unRegisterAllGeofences: " + securityException.getMessage(), securityException);
        }
    }

    /**
     * Method invoked when all the Geofences have been registered successfully.
     *
     * @param task Represents the asynchronous operation of adding/removing the Geofences
     */
    @Override
    public void onComplete(@NonNull Task<Void> task) {
        if(task.isSuccessful()){
            Log.i(TAG, "onComplete: Geofences added/removed successfully");
        } else {
            Log.e(TAG, "onComplete: Error while adding/removing Geofences", task.getException());
        }
    }
}
