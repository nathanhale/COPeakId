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

import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;

import com.wyeknot.ez_mixare.data.DataHandler;
import com.wyeknot.ez_mixare.gui.DataPainter;
import com.wyeknot.ez_mixare.gui.ScreenPainter;
import com.wyeknot.ez_mixare.gui.UIEventHandler;


public class AugmentedView extends View {

	private ScreenPainter screenPainter;
	private MixContext mixContext;
	
	private UIEventHandler uiEventHandler;
	
	private DataHandler dataHandler;
	
	private DataPainter dataPainter;

	public AugmentedView(Context context, DataHandler d, MixContext m, ScreenPainter s) {
		super(context);
		
		screenPainter = s;
		mixContext = m;
		dataHandler = d;
		
		uiEventHandler = new UIEventHandler(d);
		dataPainter = new DataPainter(mixContext, dataHandler, uiEventHandler,
				screenPainter.getWidth(), screenPainter.getHeight());;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		try {			
			screenPainter.setWidth(canvas.getWidth());
			screenPainter.setHeight(canvas.getHeight());
			screenPainter.setCanvas(canvas);
			
			dataPainter.init(screenPainter.getWidth(), screenPainter.getHeight());
			dataPainter.draw(screenPainter);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		try {
			float xPress = event.getX();
			float yPress = event.getY();
			
			if (event.getAction() == MotionEvent.ACTION_UP) {
				uiEventHandler.addClickEventToQueue(xPress, yPress);
			}

			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			return super.onTouchEvent(event);
		}
	}
	
	public void takeOnResumeAction() {
		uiEventHandler.clearEvents();
	}
}
