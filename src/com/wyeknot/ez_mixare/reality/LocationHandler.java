/*
 * Copyright (C) 2010- Peer internet solutions
 * Modifications (C) 2012- Nathan Hale
 * 
 * This file is part of mixare.
 * 
 * This program is free software: you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version. 
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details. 
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program. If not, see <http://www.gnu.org/licenses/>
 */
package com.wyeknot.ez_mixare.reality;

import java.util.Date;

import com.wyeknot.ez_mixare.MixConstants;
import com.wyeknot.ez_mixare.MixContext;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class LocationHandler {

	private LocationManager locationManager;
	
	private Context mContext;
	
	private MixContext mixContext;

	private static final long STALE_LOCATION_CUTOFF = MixConstants.staleLocationCutoffTime;
	public static final float MINIMUM_ACCURACY = MixConstants.minimumAccuracy;
	
	public LocationHandler(Context context, MixContext m) {
		
		mContext = context;
		mixContext = m;
		
		locationManager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
	}


	public void startLocationUpdates() {		
		Criteria c = new Criteria();
		//try to use the coarse provider first to get a rough position
		c.setAccuracy(Criteria.ACCURACY_COARSE);
		String coarseProvider = locationManager.getBestProvider(c, true);
		try {
			locationManager.requestLocationUpdates(coarseProvider, 0 , 0, coarseLocationListener);
		} catch (Exception e) {
			Log.d("LocationHandler","Could not initialize the coarse provider");
		}

		//need to be precise
		c.setAccuracy(Criteria.ACCURACY_FINE);				
		//fineProvider will be used for the initial phase (requesting fast updates)
		//as well as during normal program usage
		//NB: using "true" as second parameters means we get the provider only if it's enabled
		String fineProvider = locationManager.getBestProvider(c, true);
		try {
			locationManager.requestLocationUpdates(fineProvider, 0, 0, fineLocationListener);
		} catch (Exception e) {
			Log.d("LocationHandler","Could not initialize the initial fine location provider");
		}

		//frequency and minimum distance for update
		//these values will only be used after there's a good GPS fix
		//see back-off pattern discussion 
		//http://stackoverflow.com/questions/3433875/how-to-force-gps-provider-to-get-speed-in-android
		//thanks Reto Meier for his presentation at gddde 2010
		
		//TODO: add a preference for this -- allows the user to decide, e.g. if they're in a car
		long locationMinimumUpdateFrequency = 60000;	     //60 seconds
		float locationMinumumMovementDistanceForUpdate = 15; //in meters
		
		try {
			locationManager.requestLocationUpdates(fineProvider,
					locationMinimumUpdateFrequency,
					locationMinumumMovementDistanceForUpdate,
					ongoingLocationListener);
		} catch (Exception e) {
			Log.d("LocationHandler", "Could not initialize the normal provider");
		}
		
		try {
			Location lastFinePos = locationManager.getLastKnownLocation(fineProvider);
			Location lastCoarsePos = locationManager.getLastKnownLocation(coarseProvider);

			if ((lastFinePos != null) && ((lastFinePos.getTime() - (new Date().getTime())) < STALE_LOCATION_CUTOFF)) {
				mixContext.setCurrentLocation(lastFinePos);
			}
			else if ((lastCoarsePos != null) && ((lastCoarsePos.getTime() - (new Date().getTime())) < (STALE_LOCATION_CUTOFF / 2))) {
				mixContext.setCurrentLocation(lastCoarsePos);
			}
			else {
				/* Setting this to null indicates that no location is available
				 * and that a message to that effect should be displayed to the
				 * user.
				 */
				mixContext.setCurrentLocation(null);
			}
		} catch (Exception e) {
			/* Setting this to null indicates that no location is available
			 * and that a message to that effect should be displayed to the
			 * user.
			 */
			mixContext.setCurrentLocation(null);
		}
	}
	
	public void stopLocationUpdates() {
		if (null != locationManager) {
			locationManager.removeUpdates(coarseLocationListener);
			locationManager.removeUpdates(fineLocationListener);
			locationManager.removeUpdates(ongoingLocationListener);
			locationManager = null;
		}
	}
	

	public void onLocationChangedCommon(final Location location) {
		mixContext.setCurrentLocation(location);
		mixContext.devicePositionChanged();
	}
	
	
	private LocationListener fineLocationListener = new LocationListener() {
		@Override
		public void onLocationChanged(Location location) {
			Log.d("LocationHandler", "fineLocationListener location Changed: " + location.getProvider() + 
					" lat: " + location.getLatitude() +
					" lon: " + location.getLongitude() +
					" alt: " + location.getAltitude() + 
					" acc: " + location.getAccuracy());
			
			try {
				if (location.getAccuracy() < MINIMUM_ACCURACY) {
					locationManager.removeUpdates(coarseLocationListener);
					locationManager.removeUpdates(fineLocationListener);
				}
				
				if (location.getAccuracy() < mixContext.getCurrentLocation().getAccuracy()) {
					onLocationChangedCommon(location);
				}
			} catch (Exception ignore) { }
		}

		@Override
		public void onProviderDisabled(String arg0) { }

		@Override
		public void onProviderEnabled(String arg0) { }

		@Override
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {}
	};
	
	private LocationListener coarseLocationListener = new LocationListener() {

		@Override
		public void onLocationChanged(Location location) {
			Log.d("LocationHandler", "coarseLocationListener Location Changed: " +location.getProvider() + 
					" lat: " + location.getLatitude() + 
					" lon: " + location.getLongitude() + 
					" alt: " + location.getAltitude() + 
					" acc: " + location.getAccuracy());
			
			try {
				locationManager.removeUpdates(coarseLocationListener);
				onLocationChangedCommon(location);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		@Override
		public void onProviderDisabled(String arg0) {}

		@Override
		public void onProviderEnabled(String arg0) {}

		@Override
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {}
	};

	
	private LocationListener ongoingLocationListener = new LocationListener() {
		@Override
		public void onLocationChanged(Location location) {
			Log.v("LocationHandler","ongoingLocationListener Location Changed: " + location.getProvider() + 
					" lat: " + location.getLatitude() + 
					" lon: " + location.getLongitude() + 
					" alt: " + location.getAltitude() + 
					" acc: " + location.getAccuracy());
			
			
			onLocationChangedCommon(location);
		}
		
		@Override
		public void onProviderDisabled(String provider) {}

		@Override
		public void onProviderEnabled(String provider) {}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {}
	};
}
