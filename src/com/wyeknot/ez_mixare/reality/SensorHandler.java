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

import static android.hardware.SensorManager.SENSOR_DELAY_GAME;

import java.util.List;

import com.wyeknot.ez_mixare.Compatibility;
import com.wyeknot.ez_mixare.MixContext;
import com.wyeknot.ez_mixare.render.Matrix;

import android.app.Activity;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.widget.Toast;

public class SensorHandler implements SensorEventListener {

	private SensorManager sensorMgr;
	private List<Sensor> sensors;
	private Sensor sensorGrav, sensorMag;

	private int compassErrorDisplayed;
	
	private Activity owner; //Used for getting the rotation matrix
	private MixContext mixContext;

	
	private float RTmp[] = new float[9];
	private float Rot[] = new float[9];
	private float I[] = new float[9];
	private float grav[] = new float[3];
	private float mag[] = new float[3];

	private int rHistIdx = 0;
	private Matrix tempR = new Matrix();
	private Matrix finalR = new Matrix();
	private Matrix smoothR = new Matrix();
	private Matrix histR[] = new Matrix[60];
	private Matrix m1 = new Matrix();
	private Matrix m2 = new Matrix();
	private Matrix m3 = new Matrix();
	private Matrix m4 = new Matrix();

	
	public SensorHandler(Activity a, MixContext m) {
		owner = a;
		mixContext = m;
		compassErrorDisplayed = 0;
	}
	
	
	public void initializeMatrices() {
		double angleX, angleY;

		int marker_orientation = -90;

		int rotation = Compatibility.getRotation(owner);
		
		//display text from left to right and keep it horizontal
		angleX = Math.toRadians(marker_orientation);
		m1.set(	1f,	0f, 						0f, 
				0f,	(float) Math.cos(angleX),	(float) -Math.sin(angleX),
				0f,	(float) Math.sin(angleX),	(float) Math.cos(angleX)
		);
		angleX = Math.toRadians(marker_orientation);
		angleY = Math.toRadians(marker_orientation);
		if (rotation == 1) {
			m2.set(	1f,	0f,							0f,
					0f,	(float) Math.cos(angleX),	(float) -Math.sin(angleX),
					0f,	(float) Math.sin(angleX),	(float) Math.cos(angleX));
			m3.set(	(float) Math.cos(angleY),	0f,	(float) Math.sin(angleY),
					0f,							1f,	0f,
					(float) -Math.sin(angleY),	0f,	(float) Math.cos(angleY));
		} else {
			m2.set(	(float) Math.cos(angleX),	0f,	(float) Math.sin(angleX),
					0f,							1f,	0f,
					(float) -Math.sin(angleX),	0f, (float) Math.cos(angleX));
			m3.set(	1f,	0f,							0f, 
					0f,	(float) Math.cos(angleY),	(float) -Math.sin(angleY),
					0f,	(float) Math.sin(angleY),	(float) Math.cos(angleY));
		}
		
		m4.toIdentity();

		for (int i = 0; i < histR.length; i++) {
			histR[i] = new Matrix();
		}
	}
	

	public void registerListeners(SensorManager s) {
		sensorMgr = s;
		
		sensors = sensorMgr.getSensorList(Sensor.TYPE_ACCELEROMETER);
		if (sensors.size() > 0) {
			sensorGrav = sensors.get(0);
		}

		sensors = sensorMgr.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
		if (sensors.size() > 0) {
			sensorMag = sensors.get(0);
		}

		sensorMgr.registerListener(this, sensorGrav, SENSOR_DELAY_GAME);
		sensorMgr.registerListener(this, sensorMag, SENSOR_DELAY_GAME);
		
		updateGeomagneticField();
		
	}
	
	public void updateGeomagneticField() {
		
		Location curLoc = mixContext.getCurrentLocation();
		if (null == curLoc) {
			return;
		}
		
		GeomagneticField gmf = new GeomagneticField((float)curLoc.getLatitude(),
				(float)curLoc.getLongitude(),
				(float)curLoc.getAltitude(),
				System.currentTimeMillis());

		double angleY = Math.toRadians(-gmf.getDeclination());
		m4.set((float) Math.cos(angleY), 0f,
				(float) Math.sin(angleY), 0f, 1f, 0f, (float) -Math.sin(angleY), 0f, (float) Math.cos(angleY));

		mixContext.declination = gmf.getDeclination();
	}
	
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		if ((sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) && 
		    ((accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) && (compassErrorDisplayed == 0))) {
			
			for(int i = 0 ; i < 2 ; i++) {
				Toast.makeText(owner, "Compass data unreliable. Please recalibrate compass.", Toast.LENGTH_LONG).show();
			}

			compassErrorDisplayed++;
		}
	}

	@Override
	public void onSensorChanged(SensorEvent evt) {
		try {
			if (evt.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				grav[0] = evt.values[0];
				grav[1] = evt.values[1];
				grav[2] = evt.values[2];

				mixContext.devicePositionChanged();
			}
			else if (evt.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
				mag[0] = evt.values[0];
				mag[1] = evt.values[1];
				mag[2] = evt.values[2];

				mixContext.devicePositionChanged();
			}

			SensorManager.getRotationMatrix(RTmp, I, grav, mag);
			
			int rotation = Compatibility.getRotation(owner);
			
			if (rotation == 1) {
				SensorManager.remapCoordinateSystem(RTmp, SensorManager.AXIS_X, SensorManager.AXIS_MINUS_Z, Rot);
			} else {
				SensorManager.remapCoordinateSystem(RTmp, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_Z, Rot);
			}
			tempR.set(Rot[0], Rot[1], Rot[2], Rot[3], Rot[4], Rot[5], Rot[6], Rot[7],
					Rot[8]);

			finalR.toIdentity();
			finalR.prod(m4);
			finalR.prod(m1);
			finalR.prod(tempR);
			finalR.prod(m3);
			finalR.prod(m2);
			finalR.invert(); 

			histR[rHistIdx].set(finalR);
			rHistIdx++;
			if (rHistIdx >= histR.length) {
				rHistIdx = 0;
			}

			smoothR.set(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f);
			for (int i = 0; i < histR.length; i++) {
				smoothR.add(histR[i]);
			}
			smoothR.mult(1 / (float) histR.length);
			
			mixContext.setRotationMatrix(smoothR);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}


	public void unregisterListeners() {
		sensorMgr.unregisterListener(this, sensorGrav);
		sensorMgr.unregisterListener(this, sensorMag);
		sensorMgr = null;
	}
}
