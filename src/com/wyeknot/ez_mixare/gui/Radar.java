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
package com.wyeknot.ez_mixare.gui;

import com.wyeknot.ez_mixare.Marker;
import com.wyeknot.ez_mixare.MixConstants;
import com.wyeknot.ez_mixare.MixContext;
import com.wyeknot.ez_mixare.data.DataHandler;

/** Takes care of the small radar in the top left corner and of its points
 * @author daniele
 *
 */
public class Radar implements ScreenObj {
	/** Radius in pixels on screen */
	public static final float RADIUS = 40;
	
	/** Position on screen */
	static float originX = 0 , originY = 0;
	
	/** Color */
	public static final int RADAR_COLOR = MixConstants.radarBackgroundColor;
	public static final int RADAR_LINES_COLOR = MixConstants.radarFieldOfViewLinesColor;
	
	/** The markers list to be drawn in this radar */
	private DataHandler dataHandler;
	
	/** The current range is taken from here */
	private MixContext mixContext;
	
	public Radar(MixContext m, DataHandler d) {
		this.mixContext = m;
		this.dataHandler = d;
	}

	public void paint(ScreenPainter dw) {
		double range = mixContext.getRange();
		
		/** Draw the radar */
		dw.setFill(true);
		dw.setColor(RADAR_COLOR);
		dw.paintCircle(originX + RADIUS, originY + RADIUS, RADIUS);

		/** put the markers in it */
		float scale = (float)range / RADIUS;

		for (int ii = 0 ; ii < dataHandler.getMarkerCount() ; ii++) {
			Marker m = dataHandler.getMarker(ii);
			float x = m.getLocationVector().x / scale;
			float y = m.getLocationVector().z / scale;

			if (m.isActive() && (x * x + y * y < RADIUS * RADIUS)) {
				dw.setFill(true);
				dw.setColor(m.getMarkerRadarColor());
				dw.paintRect(x + RADIUS - 1, y + RADIUS - 1, 2, 2);
			}
		}
	}

	/** Width on screen */
	public float getWidth() {
		return RADIUS * 2;
	}

	/** Height on screen */
	public float getHeight() {
		return RADIUS * 2;
	}
}

