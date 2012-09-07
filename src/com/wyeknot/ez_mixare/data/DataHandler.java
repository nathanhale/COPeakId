/*
 * Copyright (C) 2010- Peer internet solutions
 * 
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

package com.wyeknot.ez_mixare.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import com.wyeknot.ez_mixare.Marker;

import android.content.Context;
import android.location.Location;

/**
 * DataHandler is the model which provides the Marker Objects with their data.
 */
public class DataHandler {
	
	// complete marker list
	private List<Marker> markerList = new ArrayList<Marker>();
	
	private Context context = null;
	
	/**
	 * This variable allows the data points to be updated by some class which
	 * doesn't have access to the current location of the MixContext. This will
	 * be checked on draw cycles by DataPainter and the locations updated as
	 * needed.
	 */
	private boolean mNeedsLocationUpdate = true;
	
	public void addMarkers(List<Marker> markers) {

		for(Marker marker : markers) {
			if(!markerList.contains(marker)) {
				if (context != null) {
					marker.setContext(context);
				}
				markerList.add(marker);
			}
		}
		
		//Now that we've added some markers we need a location update
		setLocationUpdateNeeded(true);
	}
	
	public boolean needsLocationUpdate() {
		return mNeedsLocationUpdate;
	}

	public void setLocationUpdateNeeded(boolean needed) {
		mNeedsLocationUpdate = needed;
	}

	public void sortMarkerList() {
		Collections.sort(markerList); 
	}
	
	public void updateDistances(Location location) {
		if (location == null) {
			return;
		}
		
		for(Marker marker : markerList) {
			float[] dist = new float[3];
			Location.distanceBetween(marker.getLatitude(),
					marker.getLongitude(),
					location.getLatitude(),
					location.getLongitude(),
					dist);
			marker.setDistance(dist[0]);
		}
	}
	
	public void updateActivationStatus() {
		
		Hashtable<Class<? extends Marker>, Integer> map = new Hashtable<Class<? extends Marker>, Integer>();
				
		/* This determines which markers are active by choosing the
		 * N closest markers for each class where N is that marker
		 * class's maximum number of objects.
		 * 
		 * Remember, the markers are sorted by distance, so the
		 * closest ones are going to be the active ones.
		 */
		for (Marker marker : markerList) {
			if (marker.isUserActive()) {
				Class<? extends Marker> mClass = marker.getClass();
				//Increment the number of instances of this type of marker
				map.put(mClass, (map.containsKey(mClass)) ? (map.get(mClass) + 1) : 1);

				//See if this marker should be active or not
				boolean belowMax = (map.get(mClass) <= marker.getMaxObjects());

				//Set the marker to be either active or not
				marker.setActive(belowMax);
			}
			else {
				marker.setActive(false);
			}
		}
	}
	
	public void resetUserActiveForAllMarkers() {
		for (Marker m : markerList) {
			m.setUserActive(true);
		}
	}

	public void onLocationChanged(Location location) {
		//Must update distances before sorting because the sort is on the distances
		updateDistances(location);
		
		sortMarkerList();
		
		updateActivationStatus();

		for(Marker marker : markerList) {
			marker.updateRelativeLocation(location);
		}
	}
	
	public void setContext(Context c) {
		this.context = c;
		
		for (Marker marker : markerList) {
			marker.setContext(context);
		}
	}

	public int getMarkerCount() {
		return markerList.size();
	}
	
	public Marker getMarker(int index) {
		return markerList.get(index);
	}
}
