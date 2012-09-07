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

public class MixConstants {
	public static final int standardMarkerLabelOutlineStrokeWidth = 2;
	public static final int standardMarkerLabelMaximumWidth = 300;
	public static final int standardMarkerMaxObjects = 30;
	/** in meters */
	public static final int standardMarkerColorChangeThresholdDistance = 1000;
    
    /** in meters - this is about 5 miles */
	public static final int defaultRange = 8050;   
    
	/** in seconds - this is 2 hours */
	public static final long staleLocationCutoffTime = 7200;

	public static final float minimumAccuracy = 40.0f;

	/** in meters -- this is about 1000 feet */
	public static final int rangeSetterMinimum = 305;
	/** in meters - this is about 50 miles */
	public static final int rangeSetterMaximum = 80467;
	
	public static final int radarXPos = 5;
	public static final int radarYPos = 5;
	
	public static final int radarTextPaddingX = 5;
	public static final int radarTextPaddingY = 3;
	
	
	/*** Colors ***/
	public static final int radarBackgroundColor = Color.argb(200, 5, 5, 5);
	public static final int radarFieldOfViewLinesColor = Color.argb(200, 225, 225, 225);
	public static final int standardMarkerBackgroundColor = Color.WHITE;
	public static final int standardMarkerColor = Color.BLACK;
	public static final int standardMarkerTextBackgroundColor = Color.argb(192, 0, 0, 0);
	public static final int standardMarkerTextColor = Color.WHITE;
	public static final int warningBackgroundColor = Color.argb(180, 255, 0, 0);
	public static final int warningTextColor = Color.WHITE;
	public static final int bearingTextColor = Color.WHITE;
	public static final int bearingBackgroundColor = Color.BLACK;
	public static final int rangeTextColor = Color.WHITE;
	
	
	/*** Strings ***/
	public static final String warningText1 = "Exact Location Unknown! Marker positions inaccurate!";
	public static final String warningText2 = "Acquiring GPS...";
	
	public static final String mixarePreferencesFile = "ezmixare_preferences";
	public static final String mixareRangeItemName = "range";
}
