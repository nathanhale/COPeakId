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

import com.wyeknot.ez_mixare.gui.ScreenPainter;

import android.graphics.Path;


/**
 * This markers represent the points of interest.
 * On the screen they appear as circles, since this
 * class inherits the draw method of the Marker.
 * 
 * @author hannes
 * 
 */
public class TriangleMarker extends Marker {

	public static final int MAX_OBJECTS = 20;
	

	public TriangleMarker(String key, String title, double latitude, double longitude, double altitude, ClickHandler h) {
		super(key, title, latitude, longitude, altitude, h);
	}

	@Override
	public int getMaxObjects() {
		return MAX_OBJECTS;
	}
	
	public int getMarkerRadarColor() {
		return markerBgColor;
	}
	
	@Override
	public String getDistanceString() {
		return "\nElev: " + GenericMixUtils.formatElevation(realAltitude) +
				"\n(" + GenericMixUtils.formatDist((float)distance) + " away)";
	}
	
	@Override
	public void drawMarker(ScreenPainter dw) {
		float maxHeight = dw.getHeight();
		
		float radius = (float)getMarkerRadius(maxHeight);
		float strokeWidth = getMarkerStrokeWidth(maxHeight);
		float currentAngle = getCurrentAngle();
		
		dw.setColor(markerBgColor);
		dw.setStrokeWidth(strokeWidth * 2);
		dw.setFill(false);
		
		Path triangle = new Path();
		triangle.moveTo(0, 0);
		triangle.lineTo(-radius,-radius);
		triangle.lineTo(radius,-radius);
		triangle.close();

		dw.paintPath(triangle, cMarker.x, cMarker.y, radius * 2, radius * 2, currentAngle + 90, 1);
		
		dw.setColor(markerColor);
		dw.setStrokeWidth(strokeWidth);
		
		dw.paintPath(triangle, cMarker.x, cMarker.y, radius * 2, radius * 2, currentAngle + 90, 1);
	}
}
