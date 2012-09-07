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

/**
 * This class is the main application which uses the other classes for different
 * functionalities.
 * It sets up the camera screen and the augmented screen which is in front of the
 * camera screen.
 * It also handles the main sensor events, touch events and location events.
 */

import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.wyeknot.copeakid.R;
import com.wyeknot.ez_mixare.data.DataHandler;
import com.wyeknot.ez_mixare.gui.ScreenPainter;
import com.wyeknot.ez_mixare.reality.LocationHandler;
import com.wyeknot.ez_mixare.reality.SensorHandler;

public class MixView extends Activity implements OnSeekBarChangeListener {

	private CameraSurface camScreen;
	private AugmentedView augScreen;

	private boolean isInitialized;

	private MixContext mMixContext;

	private ScreenPainter mScreenPainter;
	
	private SensorHandler mSensorHandler;
	private LocationHandler mLocationHandler;

	private WakeLock mWakeLock;

	private RelativeLayout mRangeSetterLayout;
	private SeekBar mRangeSetterBar;
	private TextView mCurrentRangeText;
	
	private AlertDialog currentDialog = null;



	public void showErrorMessage(Exception ex) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
		builder.setMessage("An error occurred: " + ex.getMessage());
		builder.setCancelable(true);
		builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
		currentDialog = alert;
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		try {

			final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag");
			
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			
			DataHandler dataHandler = GenericMixUtils.getDataHandler(this);

			if (!isInitialized) {
				mMixContext = new MixContext(this, dataHandler, new MixContext.DevicePositionChangedListener() {
					@Override
					public void viewNeedsUpdate() {
						augScreen.postInvalidate();
						mSensorHandler.updateGeomagneticField();
					}
				});
				mScreenPainter = new ScreenPainter();
				isInitialized = true;		
			}

			
			/***** Now initialize the UI ******/
			
			camScreen = new CameraSurface(this);
			augScreen = new AugmentedView(this, dataHandler, mMixContext, mScreenPainter);
			
			setContentView(camScreen);
			addContentView(augScreen, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			
			LayoutInflater inflater = LayoutInflater.from(this);

			mRangeSetterLayout = (RelativeLayout)inflater.inflate(R.layout.range_setter_layout, null);
			mRangeSetterLayout.setVisibility(View.GONE);
			
			mCurrentRangeText = (TextView)mRangeSetterLayout.findViewById(R.id.current_range);
			mCurrentRangeText.setText(GenericMixUtils.formatDist(mMixContext.getRange()));
			
			mRangeSetterBar = (SeekBar)mRangeSetterLayout.findViewById(R.id.range_setter_bar);
			mRangeSetterBar.setMax(10000);
			mRangeSetterBar.setProgress(getProgressFromRange(mMixContext.getRange(),mRangeSetterBar.getMax()));
			mRangeSetterBar.setOnSeekBarChangeListener(this);
			
			TextView minRange = (TextView)mRangeSetterLayout.findViewById(R.id.minimum_range);
			minRange.setText(GenericMixUtils.formatDist(MixContext.MINIMUM_RANGE));
			
			TextView maxRange = (TextView)mRangeSetterLayout.findViewById(R.id.maximum_range);
			maxRange.setText(GenericMixUtils.formatDist(MixContext.MAXIMUM_RANGE));
			
			Button doneButton = (Button)mRangeSetterLayout.findViewById(R.id.dismiss_range_setter_button);
			doneButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mRangeSetterLayout.setVisibility(View.GONE);
				}
			});

			addContentView(mRangeSetterLayout, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.FILL_PARENT,
                    ViewGroup.LayoutParams.FILL_PARENT));
		} catch (Exception ex) {
			ex.printStackTrace();
			showErrorMessage(ex);
		}
	}


	@Override
	protected void onPause() {
		super.onPause();

		try {
			this.mWakeLock.release();

			mSensorHandler.unregisterListeners();
			mLocationHandler.stopLocationUpdates();
			
			//Prevent any leaked dialogs -- this may throw an exception, but it's caught so that's okay
			currentDialog.dismiss();
		} catch (Exception ignore) { }
	}

	@Override
	protected void onResume() {
		super.onResume();

		try {
			this.mWakeLock.acquire();

			LocationManager manager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);

		    if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
		        showNoGPSAlert();
		    }
			
			augScreen.takeOnResumeAction();

			mLocationHandler = new LocationHandler(this, mMixContext);
			mLocationHandler.startLocationUpdates();			

			mSensorHandler = new SensorHandler(this, mMixContext);
			mSensorHandler.initializeMatrices();
			mSensorHandler.registerListeners((SensorManager)getSystemService(SENSOR_SERVICE));
		} catch (Exception ex) {
			try {
				if (mSensorHandler != null) {
					mSensorHandler.unregisterListeners();
				}

				if (mLocationHandler != null) {
					mLocationHandler.stopLocationUpdates();
				}
				
			} catch (Exception ignore) { }
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_menu, menu);
	    
	    /* This provides a means of having other items in the menu without
	     * editing the mixare code. Just set all items to non-visible by
	     * default except for the ones you want to show up here along with
	     * the mixare menu items. In other screens you can modify the
	     * visibility yourself as is done here.
	     * 
	     * To see how to handle those menu item clicks, see
	     * onOptionsItemSelected
	     */
	    menu.setGroupVisible(R.id.mixare_menu_items, true);
	    
	    return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch (item.getItemId()) {

		case R.id.mixare_menu_item_set_range:
			mRangeSetterLayout.setVisibility(View.VISIBLE);
			break;

		/* NOTE: If this feature isn't desired, just remove it from the menu.xml file */
		case R.id.mixare_menu_item_gps_info:
			Location currentGPSInfo = mMixContext.getCurrentLocation();
			AlertDialog.Builder builder = new AlertDialog.Builder(this);

			if (null != currentGPSInfo) {
				builder.setMessage("Latitude: " + currentGPSInfo.getLatitude() + "\n" + 
						"Longitude: " + currentGPSInfo.getLongitude() + "\n" +
						"Elevation: " + currentGPSInfo.getAltitude() + " m\n" +
						"Speed: " + currentGPSInfo.getSpeed() + " km/h\n" +
						"Accuracy: " + currentGPSInfo.getAccuracy() + " m\n" +
						"Last Fix: " + new Date(currentGPSInfo.getTime()).toString());
			}
			else {
				builder.setMessage("GPS Info Not Available!");
			}

			builder.setNegativeButton("Done", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.dismiss();
				}
			});
			AlertDialog alert = builder.create();
			alert.setTitle("GPS Information");
			alert.show();
			currentDialog = alert;
			break;

		/* NOTE: If this feature isn't desired, just remove it from the menu.xml file */
		case R.id.mixare_menu_item_show_hidden_markers:
			mMixContext.resetUserActiveState();
		default:
			return GenericMixUtils.onOptionsItemSelected(this, item);
		}
		return true;
	}


	private void showNoGPSAlert() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setMessage("This feature works much better if GPS is enabled. Enable GPS now?")
		.setCancelable(false)
		.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				((Dialog)dialog).getContext().startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
				dialog.dismiss();
			}
		})
		.setNegativeButton("No", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});
		
		currentDialog = builder.create();
		currentDialog.show();
	}
	
	
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if (!fromUser) {
			//We don't care -- we already know about this change because we're doing it
			return;
		}
		
		this.mCurrentRangeText.setText(GenericMixUtils.formatDist(getRangeFromProgress(progress, seekBar.getMax())));
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) { }

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		mMixContext.setRange(getRangeFromProgress(seekBar.getProgress(), seekBar.getMax()));
	}
	
	
	/* For the progress bar we want the zoom to be pseudo-logarithmic so that they
	 * have more fine control.
	 * 
	 * To accomplish this, we divide the progress range into quartiles, with the
	 * quartiles containing 10%, 20%, 30% and 40% of all the possible values for
	 * the range. Thus the higher end of the control will be less fine-grained.
	 */
	public int getProgressFromRange(double range, int progressBarMax) {
		double possibleRangeSize = MixContext.MAXIMUM_RANGE - MixContext.MINIMUM_RANGE;
		
		double lowerQuartileUpperBound = (double)(0.10 * possibleRangeSize + MixContext.MINIMUM_RANGE);
		double lowerMidQuartileUpperBound = (double)(0.20 * possibleRangeSize + lowerQuartileUpperBound);
		double upperMidQuartileUpperBound = (double)(0.30 * possibleRangeSize + lowerMidQuartileUpperBound);
		
		if (range < lowerQuartileUpperBound) {
			return (int)(((range - MixContext.MINIMUM_RANGE) / (lowerQuartileUpperBound - MixContext.MINIMUM_RANGE)) * 0.25 * progressBarMax);
		}
		else if (range < lowerMidQuartileUpperBound) {
			return (int)(((((range - lowerQuartileUpperBound) / (lowerMidQuartileUpperBound - lowerQuartileUpperBound)) * 0.25) + 0.25) * progressBarMax);
		}
		else if (range < upperMidQuartileUpperBound) {
			return (int)(((((range - lowerMidQuartileUpperBound) / (upperMidQuartileUpperBound - lowerMidQuartileUpperBound)) * 0.25) + 0.5) * progressBarMax);
		}
		else {
			return (int)(((((range - upperMidQuartileUpperBound) / (MixContext.MAXIMUM_RANGE - upperMidQuartileUpperBound)) * 0.25) + 0.75) * progressBarMax);
		}
	}


	public double getRangeFromProgress(int progress, int progressBarMax) {
		
		double possibleRangeSize = MixContext.MAXIMUM_RANGE - MixContext.MINIMUM_RANGE;
		
		double lowerQuartileUpperBound = (double)(0.10 * possibleRangeSize + MixContext.MINIMUM_RANGE);
		double lowerMidQuartileUpperBound = (double)(0.20 * possibleRangeSize + lowerQuartileUpperBound);
		double upperMidQuartileUpperBound = (double)(0.30 * possibleRangeSize + lowerMidQuartileUpperBound);
		
		double progressPercentage = ((double)progress / (double)progressBarMax);
		
		if (progressPercentage < 0.25) {
			return ((progressPercentage / 0.25f) * (lowerQuartileUpperBound - MixContext.MINIMUM_RANGE)) + MixContext.MINIMUM_RANGE;
		}
		else if (progressPercentage < 0.5) {
			return (((progressPercentage - 0.25f) / 0.25f) * (lowerMidQuartileUpperBound - lowerQuartileUpperBound)) + lowerQuartileUpperBound;
		}
		else if (progressPercentage < 0.75) {
			return (((progressPercentage - 0.5f) / 0.25f) * (upperMidQuartileUpperBound - lowerMidQuartileUpperBound)) + lowerMidQuartileUpperBound;
		}
		else {
			return (((progressPercentage - 0.75f) / 0.25f) * (MixContext.MAXIMUM_RANGE - upperMidQuartileUpperBound)) + upperMidQuartileUpperBound;
		}
	}
}
