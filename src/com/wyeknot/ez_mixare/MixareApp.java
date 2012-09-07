/*
 * Copyright (C) 2010- Peer internet solutions
 * Modifications (C) 2012- Nathan Hale
 * 
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

import java.util.ArrayList;

import com.wyeknot.ez_mixare.data.DataHandler;

import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;

public class MixareApp extends Application {

	private DataHandler dataHandler;
	
	public DataHandler getMixareDataHandler() {
		return dataHandler;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		
		dataHandler = new DataHandler();
		
		final ArrayList<Marker> markers = new ArrayList<Marker>();
		
		Marker.ClickHandler h = new Marker.ClickHandler() {
			@Override
			public boolean handle(final Marker m) {
				AlertDialog.Builder builder = new AlertDialog.Builder(m.getContext());

				builder.setTitle(m.getTitle())
				.setMessage(m.getDistanceString())
				.setCancelable(true)
				.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				})
				.setNegativeButton("Hide This Marker", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						m.setUserActive(false);
						m.setActive(false);
						dialog.cancel();
					}
				})
				.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});
				
				builder.create().show();
				return true;
			}
		};
		
		StandardMarker m;
		
		m = new StandardMarker("p1","Cumnor Hill",51.763325,-1.329346, 147, h);
		markers.add(m);
		
		m = new StandardMarker("p2","Shop on Botley Road",51.752805,-1.284413, 124, h);
		markers.add(m);
		
		m = new StandardMarker("p3","Keble College",51.759507,-1.257586, 124, h);
		markers.add(m);
		
		m = new StandardMarker("p4","Thom Building",51.760287,-1.259678, 155, h);
		markers.add(m);
		
		m = new StandardMarker("p5","Exeter College Hall",51.753556,-1.255547, 124, h);
		markers.add(m);
		
		m = new StandardMarker("p6","Radcliffe Camera",51.7534,-1.253981, 160, h);
		markers.add(m);

		m = new StandardMarker("p7","Balliol College",51.754373,-1.257806, 135, h);
		markers.add(m);
		
		m = new StandardMarker("p8","Bodleian Library",51.753981,-1.254635, 150, h);
		markers.add(m);
		
		m = new StandardMarker("p9","Golden Gate Bridge",37.809513,-122.477646, 25, h);
		markers.add(m);
		
		m = new StandardMarker("p9","Creekside Inn",37.419322,-122.135316, 50, h);
		markers.add(m);
		
		m = new StandardMarker("p10","Stanford Law School",37.42406,-122.167904, 135, h);
		markers.add(m);
		
		m = new StandardMarker("p11","Caltrain Station",37.428695,-122.14252, 150, h);
		markers.add(m);
		
		m = new StandardMarker("p12","Fry's Electronics",37.423455,-122.136812, 25, h);
		markers.add(m);
		
		m = new StandardMarker("p13","Akin's Body Shop",37.425546,-122.137333, 50, h);
		markers.add(m);
		
		dataHandler.addMarkers(markers);
		dataHandler.sortMarkerList();
		
	}
}
