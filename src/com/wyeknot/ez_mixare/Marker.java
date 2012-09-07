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

import java.text.DecimalFormat;

import com.wyeknot.ez_mixare.gui.ScreenLine;
import com.wyeknot.ez_mixare.gui.ScreenObj;
import com.wyeknot.ez_mixare.gui.ScreenPainter;
import com.wyeknot.ez_mixare.gui.TextObj;
import com.wyeknot.ez_mixare.reality.PhysicalPlace;
import com.wyeknot.ez_mixare.render.CameraCalculator;
import com.wyeknot.ez_mixare.render.MixVector;

import android.content.Context;
import android.location.Location;

/**
 * The class represents a marker and contains its information.
 * It draws the marker itself and the corresponding label.
 * All markers are specific markers like SocialMarkers or
 * NavigationMarkers, since this class is abstract
 */

abstract public class Marker implements Comparable<Marker> {

	protected String key;
	protected String title;
	protected boolean underline = false;
	protected PhysicalPlace mGeoLoc;
	
	/** 
	 * In case our GPS updates ever don't have this we want to be
	 * able to go back. This is in contrast to the altitude in
	 * mGeoLoc, which can be set to zero depending on the readings
	 * we're getting.
	 */
	protected double realAltitude; 
	
	protected double distance; // distance from user to mGeoLoc in meters
	private boolean active;
	/**
	 * userActive provides a way for the user to set whether a given marker is
	 * active. This is useful, for example, if one marker is impeding the view
	 * of another that the user doesn't care about.
	 */
	private boolean userActive;

	// Draw properties
	protected boolean isVisible;
	protected int markerColor = MixConstants.standardMarkerColor;
	protected int markerBgColor = MixConstants.standardMarkerBackgroundColor;
	protected int textColor = MixConstants.standardMarkerTextColor;
	protected int textBgColor = MixConstants.standardMarkerTextBackgroundColor;
	protected float textBorderStrokeWidth = MixConstants.standardMarkerLabelOutlineStrokeWidth;
	protected int maxLabelWidth = MixConstants.standardMarkerLabelMaximumWidth;
	protected float markerColorChangeThresholdDistance = MixConstants.standardMarkerColorChangeThresholdDistance;

	public MixVector cMarker = new MixVector();
	protected MixVector signMarker = new MixVector();
	
	protected MixVector locationVector = new MixVector();
	private MixVector origin = new MixVector(0, 0, 0);
	private MixVector upV = new MixVector(0, 1, 0);
	private ScreenLine pPt = new ScreenLine();

	protected MarkerLabel txtLab = new MarkerLabel();
	protected TextObj textBlock; //The block contained by txtLab
	
	private ClickHandler clickHandler;

	/* Stores what the context is when the marker is placed so that
	 * external click handlers can use that context to display things
	 * (such as Alerts and other Dialogs) or move to new views.
	 */
	private Context markerContext; 
	
	public Marker(String key, String title, double latitude, double longitude, double altitude, ClickHandler h) {
		super();

		this.active = false;
		this.userActive = true; //true by default
		this.isVisible = false;
		this.key = key;
		this.title = title;
		this.mGeoLoc = new PhysicalPlace(latitude, longitude, altitude);
		this.realAltitude = altitude;
		
		this.clickHandler = h;
	}
	
	public String getTitle(){
		return title;
	}

	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}
	
	//This is intended to allow different units as needed for different applications
	public String getDistanceString() {
		double d = distance;
		
		DecimalFormat df = new DecimalFormat("@#");
		
		if (d < 1000.0) {
			return " (" + df.format(d) + "m)";
		} else {
			d = d / 1000.0;
			return " (" + df.format(d) + "km)";
		}
	}
	
	public double getLatitude() {
		return mGeoLoc.getLatitude();
	}
	
	public double getLongitude() {
		return mGeoLoc.getLongitude();
	}
	
	public double getAltitude() {
		return mGeoLoc.getAltitude();
	}
	
	public double getRealAltitude() {
		return realAltitude;
	}
	
	public MixVector getLocationVector() {
		return locationVector;
	}
	
	public void setContext(Context c) {
		this.markerContext = c;
	}
	
	public Context getContext() {
		return markerContext;
	}
	
	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
	
	public boolean isUserActive() {
		return userActive;
	}
	
	public void setUserActive(boolean active) {
		this.userActive = active;
	}
	
	private void cCMarker(MixVector originalPoint, CameraCalculator viewCam, float addX, float addY) {

		// Temp properties
		MixVector tmpa = new MixVector(originalPoint);
		MixVector tmpc = new MixVector(upV);
		tmpa.add(locationVector); //3 
		tmpc.add(locationVector); //3
		tmpa.sub(viewCam.lco); //4
		tmpc.sub(viewCam.lco); //4
		tmpa.prod(viewCam.transform); //5
		tmpc.prod(viewCam.transform); //5

		MixVector tmpb = new MixVector();
		viewCam.projectPoint(tmpa, tmpb, addX, addY); //6
		cMarker.set(tmpb); //7
		viewCam.projectPoint(tmpc, tmpb, addX, addY); //6
		signMarker.set(tmpb); //7
	}

	private void calcV(CameraCalculator viewCam) {
		isVisible = false;

		if (cMarker.z < -1f) {
			isVisible = true;
		}
	}

	public void updateRelativeLocation(Location curGPSFix) {
		if (null == curGPSFix) {
			locationVector = new MixVector();
			return;
		}
		
		if (curGPSFix.getAltitude() == 0) {
			//This means that we don't have an elevation with our fix, so we need to set this point's altitude to 0
			mGeoLoc = new PhysicalPlace(mGeoLoc.getLatitude(),mGeoLoc.getLongitude(),0);
		}
		else if (mGeoLoc.getAltitude() == 0) {
			//Okay, we have an altitude with this fix, so set it back to what it's supposed to be
			mGeoLoc = new PhysicalPlace(mGeoLoc.getLatitude(),mGeoLoc.getLongitude(), realAltitude);
		}
		
		// If it's STILL 0 at this point, then the following applies:
		// An elevation of 0.0 probably means that the elevation of the
		// POI is not known and should be set to the user's GPS height
		// Note: this could be improved with calls to 
		// http://www.geonames.org/export/web-services.html#astergdem 
		// to estimate the correct height with DEM models like SRTM, AGDEM or GTOPO30
		if (mGeoLoc.getAltitude() == 0.0) {
			mGeoLoc.setAltitude(curGPSFix.getAltitude());
		}
		
		// compute the relative position vector from user position to POI location
		PhysicalPlace.convLocToVec(curGPSFix, mGeoLoc, locationVector);
	}

	
	public void calcPaint(CameraCalculator viewCam, float addX, float addY) {
		cCMarker(origin, viewCam, addX, addY);
		calcV(viewCam);
	}

	
	private boolean isClickValid(float x, float y) {
		//if the marker is not active (i.e. not shown in AR view) we don't have to check it for clicks
		if (!isActive() || !isVisible) {
			return false;
		}
		
		float currentAngle = MixUtils.getAngle(cMarker.x, cMarker.y, signMarker.x, signMarker.y);
		
		//TODO: adapt the following to the variable radius for marker shapes!
		pPt.x = x - signMarker.x;
		pPt.y = y - signMarker.y;
		pPt.rotate(Math.toRadians(-(currentAngle + 90)));
		pPt.x += txtLab.getX();
		pPt.y += txtLab.getY();

		float objX = txtLab.getX() - txtLab.getWidth() / 2;
		float objY = txtLab.getY() - txtLab.getHeight() / 2;
		float objW = txtLab.getWidth();
		float objH = txtLab.getHeight();
		
		//Now scale everything to make it a bit easier to touch
		objX -= 0.05 * objW;
		objW += 0.05 * objW;
		
		objY -= 0.1 * objH;
		objH += 0.1 * objH;
		

		if ((pPt.x > objX) && (pPt.x < (objX + objW)) && (pPt.y > objY) && (pPt.y < (objY + objH))) {
			return true;
		} else {
			return false;
		}
	}
	
	/* The next few protected methods are designed to make it cleaner for subclasses to draw
	 * markers.
	 */
	
	/* maxHeight is the maximum height of the screen */
	protected double getMarkerRadius(float maxHeight) {
		// draw circle with radius depending on distance
		// 0.44 is approx. vertical fov in radians
		double angle = 2.0 * Math.atan2(10, distance); 
		
		return Math.max(Math.min(angle / 0.44 * maxHeight, maxHeight), maxHeight / 25f);
	}
	
	/* maxHeight is the maximum height of the screen */
	protected float getMarkerStrokeWidth(float maxHeight) {
		return maxHeight / 100f;
	}
	
	protected float getCurrentAngle() {
		return MixUtils.getAngle(cMarker.x, cMarker.y, signMarker.x, signMarker.y);
	}
	
	public void draw(ScreenPainter dw) {
		if (isVisible) {
			drawMarker(dw);
			drawTextBlock(dw);
		}
	}

	public void drawMarker(ScreenPainter dw) {
		float maxHeight = dw.getHeight();
		
		double radius = getMarkerRadius(maxHeight);
		float strokeWidth = getMarkerStrokeWidth(maxHeight);
		
		dw.setFill(false);

		dw.setStrokeWidth(strokeWidth * 2);
		dw.setColor(markerBgColor);
		dw.paintCircle(cMarker.x, cMarker.y, (float)radius);
		
		
		dw.setStrokeWidth(strokeWidth);
		dw.setColor(markerColor);
		dw.paintCircle(cMarker.x, cMarker.y, (float)radius);
	}

	protected void drawTextBlock(ScreenPainter dw) {
		float maxHeight = Math.round(dw.getHeight() / 10f) + 1;

		String textStr = this.getTitle() + this.getDistanceString();

		textBlock = new TextObj(textStr, Math.round(maxHeight / 2f) + 1, maxLabelWidth, dw, underline);
		textBlock.setBgColor(textBgColor);
		textBlock.setBorderColor(textColor);

		float currentAngle = MixUtils.getAngle(cMarker.x, cMarker.y, signMarker.x, signMarker.y);
		txtLab.prepare(textBlock);
		dw.setStrokeWidth(textBorderStrokeWidth);
		dw.paintObj(txtLab, signMarker.x - txtLab.getWidth() / 2,
				    signMarker.y + maxHeight, currentAngle + 90, 1);

	}

	public boolean clickMarker(float x, float y) {
		boolean evtHandled = false;

		if (isClickValid(x, y)) {
			evtHandled = clickHandler.handle(this);
		}

		return evtHandled;
	}

	public int compareTo(Marker another) {

		Marker leftPm = this;
		Marker rightPm = another;

		return Double.compare(leftPm.getDistance(), rightPm.getDistance());
	}

	@Override
	public boolean equals (Object marker) {
		return this.key.equals(((Marker)marker).key);
	}


	abstract public int getMaxObjects();
	
	abstract public int getMarkerRadarColor();
	
	public static abstract class ClickHandler {
		//Return true if it is handled
		abstract public boolean handle(Marker m);
	}
	
	public class MarkerLabel implements ScreenObj {
		private float x, y;
		private float width, height;
		private ScreenObj obj;

		public void prepare(ScreenObj drawObj) {
			obj = drawObj;
			float w = obj.getWidth();
			float h = obj.getHeight();

			x = w / 2;
			y = 0;

			width = w * 2;
			height = h * 2;
		}

		public void paint(ScreenPainter dw) {
			dw.paintObj(obj, x, y, 0, 1);
		}
		
		public float getX() {
			return x;
		}
		
		public float getY() {
			return y;
		}

		public float getWidth() {
			return width;
		}

		public float getHeight() {
			return height;
		}
	}
}