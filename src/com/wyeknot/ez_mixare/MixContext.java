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

import com.wyeknot.ez_mixare.data.DataHandler;
import com.wyeknot.ez_mixare.reality.LocationHandler;
import com.wyeknot.ez_mixare.render.Matrix;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.location.Location;

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

	public Context context;

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

	/** in meters */
	private double range = MixConstants.defaultRange; //This is the default, the current val will be read from preferences
	
	public MixContext(Context ctx, DevicePositionChangedListener l) {
		super(ctx);

		localInit(ctx, null, l);
	}
	
	public MixContext(Context ctx, DataHandler h, DevicePositionChangedListener l) {
		super(ctx);
		
		localInit(ctx, h, l);
	}
	
	private void localInit(Context ctx, DataHandler h, DevicePositionChangedListener l) {
		context = ctx;		
		rotationM.toIdentity();

		//Make sure to call this before calling anything that could potentially need it
		getRangeFromPrefs();
		
		setDataHandler(h);

		listener = l;
	}
	
	private void getRangeFromPrefs() {
		SharedPreferences settings = context.getSharedPreferences(MixConstants.mixarePreferencesFile, 0);
		float newRange = settings.getFloat(MixConstants.mixareRangeItemName, (float)range);	
		range = (double)newRange;
	}	

	public void setDataHandler(DataHandler h) {
		dataHandler = h;
		dataHandler.setContext(context);
	}
	
	public void setRange(double r) {
		range = r;

		SharedPreferences settings = context.getSharedPreferences(MixConstants.mixarePreferencesFile, 0);
		SharedPreferences.Editor editor = settings.edit();
		/* store the zoom range of the zoom bar selected by the user */
		editor.putFloat(MixConstants.mixareRangeItemName, (float)r);
		editor.commit();
	}
	
	public double getRange() {
		return range;
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

		dataHandler.onLocationChanged(l);
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
		dataHandler.updateActivationStatus();
	}
	
	public static abstract class DevicePositionChangedListener {
		public abstract void viewNeedsUpdate();
	}
}
