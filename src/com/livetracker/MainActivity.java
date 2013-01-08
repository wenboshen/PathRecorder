/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.livetracker;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;

import com.livetracker.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;

/**
 * This shows how to create a simple activity with a map and a marker on the map.
 * <p>
 * Notice how we deal with the possibility that the Google Play services APK is not
 * installed/enabled/updated on a user's device.
 */
public class MainActivity extends android.support.v4.app.FragmentActivity {
    /**
     * Note that this may be null if the Google Play services APK is not available.
     */
    private GoogleMap mMap;
    private LocationManager mLocationManager;
    private LatLng lastLocation;
	private static final int TEN_SECONDS = 30000;
	private static final int TEN_METERS = 10;
    private static final LatLng NCSU = new LatLng(35.77183,-78.673789);
    
    private ArrayList<MyLocation> locArray = new ArrayList<MyLocation>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

		this.mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        
		setUpMapIfNeeded();
    }

    protected void onPause(){
    	super.onPause();
    	mLocationManager.removeUpdates(mLocationListener);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 
				TEN_SECONDS, TEN_METERS, mLocationListener);
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
            	mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(NCSU, 14));
            	Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            	lastLocation = new LatLng(location.getLatitude(), location.getLongitude());
            	String mydate = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
            	mMap.addMarker(new MarkerOptions().position(lastLocation).title(mydate));
            	locArray.add(new MyLocation("1063710221", lastLocation.latitude, lastLocation.longitude, mydate));
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void addPath(Location location) {
    	LatLng newLocation = new LatLng(location.getLatitude(), location.getLongitude());
    	String mydate = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
        mMap.addMarker(new MarkerOptions().position(newLocation).title(mydate));
        mMap.addPolyline((new PolylineOptions())
                .add(lastLocation, newLocation)
                .width(5)
                .color(Color.RED)
                .geodesic(true));
        lastLocation = newLocation;
        
        //record the path, check if there are 50 records, upload to the server
        locArray.add(new MyLocation("1063710221", lastLocation.latitude, lastLocation.longitude, mydate));
        
        if (locArray.size() >= 20){
        	new UploadTask().execute(locArray);
//        	locArray.clear();
        }
    }
    
    private class UploadTask extends AsyncTask<ArrayList<MyLocation>, Void, Void> {

		@Override
		protected Void doInBackground(ArrayList<MyLocation>... locArrays) {
			Socket clientSock = null;
	        PrintWriter out = null;
//	        BufferedReader in = null;
	 
	        try {
	        	clientSock = new Socket("152.14.93.145", 4444);
	            out = new PrintWriter(clientSock.getOutputStream(), true);
//	            in = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));
	            System.out.println(locArrays[0].size());
	            for (MyLocation loc : locArrays[0]){
	            	out.write(loc.getId()+" || "+loc.getLat()+" || "+loc.getLng()+" || "+loc.getTime() + "\n");            	
	            }
	            
	            out.write("end");
	            out.close();
//	            in.close();
	            clientSock.close();
	            locArrays[0].clear();
	        } catch (UnknownHostException e) {
	            System.err.println("UnknownHoistException");
	            System.exit(1);
	        } catch (IOException e) {
	            System.err.println("Couldn't get I/O for the connection.");
	            System.exit(1);
	        }
			return null;
		}
    	
    }
    
	private LocationListener mLocationListener = new LocationListener() {
		public void onLocationChanged(Location location) {
			addPath(location);
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

		public void onProviderEnabled(String provider) {
		}

		public void onProviderDisabled(String provider) {
		}
	};
	
    protected void onStop() {
        super.onStop();
    	new UploadTask().execute(locArray);
        mLocationManager.removeUpdates(mLocationListener);
    }
}
