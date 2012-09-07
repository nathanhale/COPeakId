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

import com.wyeknot.ez_mixare.GenericMixUtils;
import com.wyeknot.ez_mixare.Marker;
import com.wyeknot.ez_mixare.MixConstants;
import com.wyeknot.ez_mixare.MixContext;
import com.wyeknot.ez_mixare.MixState;
import com.wyeknot.ez_mixare.data.DataHandler;
import com.wyeknot.ez_mixare.render.CameraCalculator;

import android.location.Location;


/**
 * This class is able to update the markers and the radar.
 * It also handles some user events
 * 
 * @author daniele
 *
 */
public class DataPainter {

	/**current context */
	private MixContext mixContext;
	
	/** width and height of the view*/
	private int width, height;

	private CameraCalculator cam;

	private MixState state = new MixState();
	
	private UIEventHandler uiEventHandler;

	//private Location curFix;
	private DataHandler dataHandler;

	private Radar radar;
	
	//These adjust the positioning of markers on the screen, which we don't really use
	private final float addX = 0;
	private final float addY = 0;
	
	private final float radarXPos = MixConstants.radarXPos;
	private final float radarYPos = MixConstants.radarYPos;
	
	//These are the positions for the CENTER of the text
	private final float bearingXPos = radarXPos + Radar.RADIUS;
	private final float bearingYPos = radarYPos + Radar.RADIUS * 2 + 20;
	
	//These are the positions for the CENTER of the text
	private final float rangeXPos = bearingXPos;
	private final float rangeYPos = radarYPos + Radar.RADIUS * 1.5f;
	
	private final float textXPadding = MixConstants.radarTextPaddingX;
	private final float textYPadding = MixConstants.radarTextPaddingY;

	private ScreenLine leftRadarLine = new ScreenLine();
	private ScreenLine rightRadarLine = new ScreenLine();
	
	private final float warningXPos = radarXPos + (Radar.RADIUS * 2) + 10;
	private final float warningYPos = radarYPos + Radar.RADIUS;


	/**
	 * Constructor
	 */
	public DataPainter(MixContext ctx, DataHandler d, UIEventHandler h, int width, int height) {
		this.mixContext = ctx;
		uiEventHandler = h;
		dataHandler = d;
		radar = new Radar(ctx,d);
		init(width, height);
	}

	public void init(int widthInit, int heightInit) {
		try {
			width = widthInit;
			height = heightInit;

			cam = new CameraCalculator(width, height, true);
			cam.setViewAngle(CameraCalculator.DEFAULT_VIEW_ANGLE);
			
			leftRadarLine.set(0, -Radar.RADIUS);
			leftRadarLine.rotate(CameraCalculator.DEFAULT_VIEW_ANGLE / 2);
			leftRadarLine.add(radarXPos + Radar.RADIUS, radarYPos + Radar.RADIUS);
			rightRadarLine.set(0, -Radar.RADIUS);
			rightRadarLine.rotate(-CameraCalculator.DEFAULT_VIEW_ANGLE / 2);
			rightRadarLine.add(radarXPos + Radar.RADIUS, radarYPos + Radar.RADIUS);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void draw(ScreenPainter dw) {
		
		mixContext.getRotationMatrix(cam.transform);
		//curFix = mixContext.getCurrentLocation();

		state.calcPitchBearing(cam.transform);
		
		// Draw markers
		drawMarkers(dw);
		
		// Draw radar screen
		drawRadar(dw);
		
		// Draw bearing text
		drawBearing(dw);
		
		if (!mixContext.isCurrentLocationAccurateEnough()) {
			drawAccuracyWarning(dw);
		}
		
		uiEventHandler.handleNextEvent();
	}
	
	private void drawBearing(ScreenPainter dw) {
		String	dirTxt = ""; 
		int bearing = (int)state.getCurBearing(); 
		int bearingRange = (int)(state.getCurBearing() / (360f / 16f)); 
		
		switch (bearingRange) {
		case 15:
		case 0: dirTxt = "N"; break;
		case 1:
		case 2: dirTxt = "NE"; break;
		case 3:
		case 4: dirTxt = "E"; break;
		case 5:
		case 6: dirTxt = "SE"; break;
		case 7:
		case 8: dirTxt= "S"; break;
		case 9:
		case 10: dirTxt = "SW"; break;
		case 11:
		case 12: dirTxt = "W"; break;
		case 13:
		case 14: dirTxt = "NW"; break;
		}

		String bearingText = "" + bearing + ((char) 176) + " " + dirTxt;
		
		//Must be set before the size is calculated
		dw.setFontSize(12);
		
		float textWidth = dw.getTextWidth(bearingText) + textXPadding * 2;
		float textHeight = dw.getTextAsc() + dw.getTextDesc() + textYPadding * 2;
		
		float rectWidth = bearingXPos - textWidth / 2;
		float rectHeight = bearingYPos - textHeight / 2;
		
		//Paint background of the bearing text
		dw.setColor(MixConstants.bearingBackgroundColor);
		dw.setFill(true);
		dw.paintRect(rectWidth, rectHeight, textWidth, textHeight);
		
		//Paint the outline of the bearing text
		dw.setColor(MixConstants.bearingTextColor);
		dw.setFill(false);
		dw.paintRect(rectWidth, rectHeight, textWidth, textHeight);

		//Paint the bearing text itself
		dw.paintText(textXPadding + rectWidth, textYPadding + dw.getTextAsc() + rectHeight, bearingText, false);
	}
	
	private void drawRadar(ScreenPainter dw) {
		//Paint the circle and the points
		dw.paintObj(radar, radarXPos, radarYPos, -state.getCurBearing(), 1); 
		
		//Paint the field of view lines
		dw.setFill(false);
		dw.setColor(Radar.RADAR_LINES_COLOR); 
		dw.paintLine( leftRadarLine.x, leftRadarLine.y, radarXPos + Radar.RADIUS, radarYPos + Radar.RADIUS); 
		dw.paintLine( rightRadarLine.x, rightRadarLine.y, radarXPos + Radar.RADIUS, radarYPos + Radar.RADIUS);
		
		//Paint the text indicating the current range
		String rangeText = GenericMixUtils.formatDist(mixContext.getRange());

		dw.setFontSize(14); //Must be set before the width and height are calculated
		
		float width = dw.getTextWidth(rangeText) + textXPadding * 2;
		float height = dw.getTextAsc() + dw.getTextDesc() + textYPadding * 2;

		dw.paintText(textXPadding + rangeXPos - width / 2, textYPadding + dw.getTextAsc() + rangeYPos - height / 2, rangeText, false);
	}
	
	private void drawMarkers(ScreenPainter dw) {
		if (dataHandler.needsLocationUpdate()) {
			Location location = mixContext.getCurrentLocation(); 
			if (null != location) {
				dataHandler.onLocationChanged(location);
				dataHandler.setLocationUpdateNeeded(false);
			}
		}
		
		for (int i = dataHandler.getMarkerCount() - 1 ; i >= 0 ; i--) {
			Marker marker = dataHandler.getMarker(i);
			
			if (marker.isActive() && (marker.getDistance() < mixContext.getRange())) {
				//TODO:
				// To increase performance don't recalculate position vector
				// for every marker on every draw call, instead do this only 
				// after onLocationChanged and after downloading new marker
				//NH -- and after changing the zoom?
				marker.calcPaint(cam, addX, addY);
				marker.draw(dw);
			}
		}
	}

	public void drawAccuracyWarning(ScreenPainter dw) {
		//Must be set before the size is calculated
		float dpToPx = mixContext.getResources().getDisplayMetrics().density;

		dw.setFontSize(10 * dpToPx);
		
		float textWidth = dw.getTextWidth(MixConstants.warningText1) + textXPadding * 2;
		float textHeight = dw.getTextAsc() + dw.getTextDesc() + textYPadding * 2;

		//Paint background of the warning text
		dw.setColor(MixConstants.warningBackgroundColor);
		dw.setFill(true);
		dw.paintRect(warningXPos, warningYPos, textWidth, textHeight);

		//Paint the outline of the warning text background
		dw.setColor(MixConstants.warningTextColor);
		dw.setFill(false);
		dw.paintRect(warningXPos, warningYPos, textWidth, textHeight);

		//Paint the warning text itself
		dw.paintText(textXPadding + warningXPos, textYPadding + dw.getTextAsc() + warningYPos, MixConstants.warningText1, false);


		
		//Now repeat the process for the next warning text
		float warning2YPos = warningYPos + textHeight + 5;
		
		textWidth = dw.getTextWidth(MixConstants.warningText2) + textXPadding * 2;
		textHeight = dw.getTextAsc() + dw.getTextDesc() + textYPadding * 2;

		//Paint background of the warning text
		dw.setColor(MixConstants.warningBackgroundColor);
		dw.setFill(true);
		dw.paintRect(warningXPos, warning2YPos, textWidth, textHeight);

		//Paint the outline of the warning text background
		dw.setColor(MixConstants.warningTextColor);
		dw.setFill(false);
		dw.paintRect(warningXPos, warning2YPos, textWidth, textHeight);

		//Paint the warning text itself
		dw.paintText(textXPadding + warningXPos, textYPadding + dw.getTextAsc() + warning2YPos, MixConstants.warningText2, false);
	}
}
