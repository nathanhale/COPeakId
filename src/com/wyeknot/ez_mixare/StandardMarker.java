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
package com.wyeknot.ez_mixare;

import android.graphics.Color;

public class StandardMarker extends Marker {

	public static final int MAX_OBJECTS = MixConstants.standardMarkerMaxObjects;

	public StandardMarker(String key, String title, double latitude, double longitude, double altitude, ClickHandler h) {
		super(key, title, latitude, longitude, altitude, h);
	}

	@Override
	public int getMaxObjects() {
		return MAX_OBJECTS;
	}
	
	public int getMarkerRadarColor() {
		//Must be the background color in order to be seen on the radar!
		return MixConstants.standardMarkerBackgroundColor;
	}
	
	private int changeAlpha(int alpha, int color) {
		return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
	}
	
	/* This function demonstrates how the color of the marker can
	 * be lightened for a point that is farther away
	 */
	@Override
	public void setDistance(double distance) {
		super.setDistance(distance);
		
		if (markerColorChangeThresholdDistance == 0) {
			return;
		}
		
		if (distance > markerColorChangeThresholdDistance) {
			markerBgColor = changeAlpha(128,markerBgColor);
			markerColor = changeAlpha(128,markerColor);
			textColor = markerBgColor;
			textBgColor = changeAlpha(128,textBgColor);
		}
		else {
			if (markerColor != MixConstants.standardMarkerColor) {
				markerColor = MixConstants.standardMarkerColor;
				markerBgColor = MixConstants.standardMarkerBackgroundColor;
				textColor = MixConstants.standardMarkerTextColor;
				textBgColor = MixConstants.standardMarkerTextBackgroundColor;
			}
		}
	}
	
	@Override
	public String getDistanceString() {
		return "\nElev: " + GenericMixUtils.formatElevation(realAltitude) +
				"\n(" + GenericMixUtils.formatDist((float)distance) + " away)";
	}
}
