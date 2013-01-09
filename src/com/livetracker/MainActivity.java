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
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends android.support.v4.app.FragmentActivity {

	private GoogleMap mMap;
	private LocationManager mLocationManager;
	private LatLng lastLocation;
	private static final int TEN_SECONDS = 30000;
	private static final int TEN_METERS = 10;
	private static final LatLng NCSU = new LatLng(35.77183, -78.673789);

	private ArrayList<MyLocation> currArray = new ArrayList<MyLocation>();
	private ArrayList<MyLocation> demoArray = new ArrayList<MyLocation>();

	private Toast msg = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		this.mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		initDemo();
		setUpMapIfNeeded();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_demo:
			showLocArray(demoArray);
			mLocationManager.removeUpdates(mLocationListener);
			return true;
		case R.id.menu_curr:
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
					TEN_SECONDS, TEN_METERS, mLocationListener);
			showLocArray(currArray);
			return true;
		}
		return (super.onOptionsItemSelected(item));
	}

	private void initDemo(){
		double[] latlng = {35.771691, -78.673503,35.771343, 
				-78.674748,35.770576, -78.675499,35.770350, 
				-78.675735,35.770089, -78.676851,35.770141, 
				-78.677344,35.770246, -78.678954,35.770368, 
				-78.679962,35.770485, -78.681062,35.770507, 
				-78.681381,35.770916, -78.682516,35.770890, 
				-78.683084,35.770489, -78.684812,35.770542, 
				-78.685219,35.770655, -78.685734,35.770881, 
				-78.685927,35.771012, -78.686796,35.771299, 
				-78.687129};
		
		for (int i=0; i<latlng.length-1; i+=2){
			demoArray.add(new MyLocation(1063710221, latlng[i], latlng[i+1], "Jan 08, 2013"));
		}
	}
	
	private void showLocArray(ArrayList<MyLocation> locArray){
		mMap.clear();
		
		LatLng lastLoc = null, newLoc = null;

		for (MyLocation loc : locArray) {
			lastLoc = newLoc;
			newLoc = new LatLng(loc.getLat(), loc.getLng());
			mMap.addMarker(new MarkerOptions().position(newLoc).title(
					loc.getTime()));

			if (lastLoc != null) {
				mMap.addPolyline((new PolylineOptions()).add(lastLoc, newLoc)
						.width(5).color(Color.RED).geodesic(true));
			}
		}
	}
	
	protected void onPause() {
		super.onPause();
		mLocationManager.removeUpdates(mLocationListener);
		finish();
	}

	@Override
	protected void onResume() {
		super.onResume();
		setUpMapIfNeeded();
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				TEN_SECONDS, TEN_METERS, mLocationListener);
	}

	private void setUpMapIfNeeded() {
		// Do a null check to confirm that we have not already instantiated the
		// map.
		if (mMap == null) {
			// Try to obtain the map from the SupportMapFragment.
			mMap = ((SupportMapFragment) getSupportFragmentManager()
					.findFragmentById(R.id.map)).getMap();
			// Check if we were successful in obtaining the map.
			if (mMap != null) {
				mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(NCSU, 14));
				Location location = mLocationManager
						.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				lastLocation = new LatLng(location.getLatitude(),
						location.getLongitude());
				String mydate = java.text.DateFormat.getDateTimeInstance()
						.format(Calendar.getInstance().getTime());
				mMap.addMarker(new MarkerOptions().position(lastLocation)
						.title(mydate));
				currArray.add(new MyLocation(1063710221, lastLocation.latitude,
						lastLocation.longitude, mydate));
			}
		}
	}

	private void addPath(Location location) {
		LatLng newLocation = new LatLng(location.getLatitude(),
				location.getLongitude());
		String mydate = java.text.DateFormat.getDateTimeInstance().format(
				Calendar.getInstance().getTime());
		mMap.addMarker(new MarkerOptions().position(newLocation).title(mydate));
		mMap.addPolyline((new PolylineOptions()).add(lastLocation, newLocation)
				.width(5).color(Color.RED).geodesic(true));
		lastLocation = newLocation;

		// record the path, check if there are 50 records, upload to the server
		currArray.add(new MyLocation(1063710221, lastLocation.latitude,
				lastLocation.longitude, mydate));

		if (currArray.size() >= 20) {
			new UploadTask().execute(currArray);
			// locArray.clear();
		}
	}

	private class UploadTask extends
			AsyncTask<ArrayList<MyLocation>, Void, Void> {

		@Override
		protected Void doInBackground(ArrayList<MyLocation>... locArrays) {
			Socket clientSock = null;
			PrintWriter out = null;
			// BufferedReader in = null;

			try {
				clientSock = new Socket("152.14.93.145", 4444);
				out = new PrintWriter(clientSock.getOutputStream(), true);
				// in = new BufferedReader(new
				// InputStreamReader(clientSock.getInputStream()));
				System.out.println(locArrays[0].size());
				for (MyLocation loc : locArrays[0]) {
					out.write(loc.getId() + " || " + loc.getLat() + " || "
							+ loc.getLng() + " || " + loc.getTime() + "\n");
				}

				out.write("end");
				out.close();
				// in.close();
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
		new UploadTask().execute(currArray);
		mLocationManager.removeUpdates(mLocationListener);
	}
}
