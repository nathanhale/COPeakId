/*
 * Copyright (C) 2010- Peer internet solutions
 * 
 * This file is part of mixare.
 * 
 * Modified and simplified as ez_mixare by Nathan Hale, 2012
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
package com.wyeknot.ez_mixare;

import android.app.Activity;
import android.content.ContextWrapper;
import android.location.Location;

import com.wyeknot.copeakid.CoPeakIdApp;
import com.wyeknot.ez_mixare.data.DataHandler;
import com.wyeknot.ez_mixare.reality.LocationHandler;
import com.wyeknot.ez_mixare.render.Matrix;

/**
 * This class is intended to be a global repository of data for our AR activity.
 * 
 * It stores the current rotation matrix, current data, current location, and
 * declination so that they are available to lots of classes. 
 */
public class MixContext extends ContextWrapper {
	
	/** in meters */
	public static final int MINIMUM_RANGE = MixConstants.rangeSetterMinimum;
	/** in meters */
	public static final int MAXIMUM_RANGE = MixConstants.rangeSetterMaximum; 

	public Activity context;

	private Matrix rotationM = new Matrix();
	public float declination = 0f;

	private Location currentLocation;
	
	private DataHandler dataHandler;

	
	/* For updating our augmentedView when either the location
	 * or orientation of the device changes.
	 * 
	 * This hides the actual view from any of the classes except
	 * the activity.
	 */
	private DevicePositionChangedListener listener;

	
	public MixContext(Activity ctx, DevicePositionChangedListener l) {
		super(ctx);

		localInit(ctx, null, l);
	}
	
	public MixContext(Activity ctx, DataHandler h, DevicePositionChangedListener l) {
		super(ctx);
		
		localInit(ctx, h, l);
	}
	
	private void localInit(Activity ctx, DataHandler h, DevicePositionChangedListener l) {
		context = ctx;		
		rotationM.toIdentity();
		
		setDataHandler(h);

		listener = l;
	}
	

	public void setDataHandler(DataHandler h) {
		dataHandler = h;
		dataHandler.setContext(context);
	}

	
	public CoPeakIdApp.PeaksToShow getElevationsToDisplay() {
		return ((CoPeakIdApp)context.getApplication()).getElevationsToDisplay();
	}

	public void setElevationsToDisplay(CoPeakIdApp.PeaksToShow elevationsToDisplay) {		
		((CoPeakIdApp)context.getApplication()).setElevationsToDisplay(elevationsToDisplay);
		dataHandler.updateActivationStatus(this);
	}
	
	public double getRange() {
		return ((CoPeakIdApp)context.getApplication()).getRange();
	}
	
	public void setRange(double range) {
		((CoPeakIdApp)context.getApplication()).setRange(range);
	}
	
	public boolean shouldBeDisplayedByElevation(Marker marker) {
		if (marker.getRealAltitude() > CoPeakIdApp.PeaksToShow.elevCutoff14ers) {
			//System.out.println("Got marker " + marker.title + " with elevation " + marker.realAltitude);
			//System.out.println("And elevationsToDisplay.show14ers is " + elevationsToDisplay.show14ers);
			return getElevationsToDisplay().show14ers;
		}
		else if (marker.getRealAltitude() > CoPeakIdApp.PeaksToShow.elevCutoffCentennials) {
			return getElevationsToDisplay().showCentennials;
		}
		else if (marker.getRealAltitude() > CoPeakIdApp.PeaksToShow.elevCutoffBiCentennials) {
			return getElevationsToDisplay().showBiCentennials;
		}
		else {
			return getElevationsToDisplay().showLow13ers;
		}
	}
	
	public void getRotationMatrix(Matrix dest) {
		synchronized (rotationM) {
			dest.set(rotationM);
		}
	}
	
	public void setRotationMatrix(Matrix newVal) {
		synchronized (rotationM) {
			rotationM.set(newVal);
		}
	}

	public Location getCurrentLocation() {
		if (null != currentLocation) {
			synchronized (currentLocation) {
				return currentLocation;
			}
		}
		else {
			return null;
		}
	}
	
	public void setCurrentLocation(Location l) {
		//Can't synchronize on a null value 
		if (null == currentLocation) {
			currentLocation = l;
		}
		else { 
			synchronized (currentLocation) {
				currentLocation = l;
			}
		}

		dataHandler.onLocationChanged(l, this);
	}

	public boolean isCurrentLocationAccurateEnough() {
		float accuracy = 0;
		
		if (currentLocation == null) {
			return false;
		}
		else {
			accuracy = getCurrentLocation().getAccuracy();
		}
		
		if (accuracy == 0) {
			return false;
		}
		else if (accuracy < LocationHandler.MINIMUM_ACCURACY) {
			return true;
		}
		else {
			return false;
		}
	}
	
	public void devicePositionChanged() {
		listener.viewNeedsUpdate();
	}

	public void resetUserActiveState() {
		dataHandler.resetUserActiveForAllMarkers();
		dataHandler.updateActivationStatus(this);
	}
	
	public static abstract class DevicePositionChangedListener {
		public abstract void viewNeedsUpdate();
	}
}
