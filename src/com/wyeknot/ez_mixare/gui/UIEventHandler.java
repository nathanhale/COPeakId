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

import java.util.ArrayList;

import com.wyeknot.ez_mixare.Marker;
import com.wyeknot.ez_mixare.data.DataHandler;

public class UIEventHandler {

	private DataHandler dataHandler;	
	private ArrayList<UIEvent> uiEvents = new ArrayList<UIEvent>();
	
	public UIEventHandler(DataHandler h) {		
		uiEvents = new ArrayList<UIEvent>();
		dataHandler = h;
	}
	
	public void handleNextEvent() {
		UIEvent event = null;
		
		synchronized (uiEvents) {
			if (uiEvents.size() > 0) {
				event = uiEvents.remove(0);
			}
		}
		
		if (event != null) {
			switch (event.type) {
				case UIEvent.CLICK: handleClickEvent((ClickEvent)event); break;
			}
		}
	}


	boolean handleClickEvent(ClickEvent event) {
		boolean eventHandled = false;

		//the following will traverse the markers in ascending order (by distance) the first marker that 
		//matches triggers the event.
		for (int i = 0 ; ((i < dataHandler.getMarkerCount()) && !eventHandled) ; i++) {
			Marker marker = dataHandler.getMarker(i);

			eventHandled = marker.clickMarker(event.x, event.y);
		}

		return eventHandled;
	}
	
	public void addClickEventToQueue(float x, float y) {
		synchronized (uiEvents) {
			uiEvents.add(new ClickEvent(x, y));
		}
	}

	public void addKeyEventToQueue(int keyCode) {
		synchronized (uiEvents) {
			uiEvents.add(new KeyEvent(keyCode));
		}
	}

	public void clearEvents() {
		synchronized (uiEvents) {
			uiEvents.clear();
		}
	}
	
	
	/*
	private void handleKeyEvent(KeyEvent evt) {
		/** Adjust marker position with keypad *
		final float CONST = 10f;
		switch (evt.keyCode) {
			case KEYCODE_DPAD_LEFT:		addX -= CONST;		break;
			case KEYCODE_DPAD_RIGHT:	addX += CONST;		break;
			case KEYCODE_DPAD_DOWN:		addY += CONST;		break;
			case KEYCODE_DPAD_UP:		addY -= CONST;		break;
			case KEYCODE_DPAD_CENTER:	frozen = !frozen;		break;
			case KEYCODE_CAMERA:		frozen = !frozen;	break;	// freeze the overlay with the camera button
		}
	}*/
}
