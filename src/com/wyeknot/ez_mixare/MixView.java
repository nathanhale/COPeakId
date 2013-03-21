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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
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
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.wyeknot.copeakid.CoPeakIdApp;
import com.wyeknot.copeakid.HelpView;
import com.wyeknot.copeakid.LicenseView;
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

	private Set<Dialog> currentDialogs;

	//Used for the error dialog so that it can still be built with showDialog
	private Exception currentException = null;

	
	private SharedPreferences mPrefs;
	

	private static final int STARTUP_DIALOG_ID = 1;
	private static final int ELEVATION_SELECTOR_DIALOG_ID = 2;
	private static final int NO_GPS_ALERT_DIALOG_ID = 3;
	private static final int ERROR_DIALOG_ID = 4;
	private static final int GPS_INFO_DIALOG_ID = 5;
	private static final int MAX_DIALOG_ID = 5;

	public void showErrorMessage(Exception ex) {		
		currentException = ex;
		showDialog(ERROR_DIALOG_ID);
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		currentDialogs = new HashSet<Dialog>(MAX_DIALOG_ID);
		
		mPrefs = CustomUtils.getSharedPreferences(this);

		try {

			final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "CoPeakId");

			requestWindowFeature(Window.FEATURE_NO_TITLE);

			DataHandler dataHandler = CustomUtils.getDataHandler(this);

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
			mCurrentRangeText.setText(CustomUtils.formatDist(mMixContext.getRange()));

			mRangeSetterBar = (SeekBar)mRangeSetterLayout.findViewById(R.id.range_setter_bar);
			mRangeSetterBar.setMax(10000);
			mRangeSetterBar.setProgress(getProgressFromRange(mMixContext.getRange(),mRangeSetterBar.getMax()));
			mRangeSetterBar.setOnSeekBarChangeListener(this);

			TextView minRange = (TextView)mRangeSetterLayout.findViewById(R.id.minimum_range);
			minRange.setText(CustomUtils.formatDist(MixContext.MINIMUM_RANGE));

			TextView maxRange = (TextView)mRangeSetterLayout.findViewById(R.id.maximum_range);
			maxRange.setText(CustomUtils.formatDist(MixContext.MAXIMUM_RANGE));

			Button doneButton = (Button)mRangeSetterLayout.findViewById(R.id.dismiss_range_setter_button);
			doneButton.setOnClickListener(new View.OnClickListener() {
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
			for (Dialog d : currentDialogs) {
				d.dismiss();
			}
		} catch (Exception ignore) { }
	}

	@Override
	protected void onResume() {
		super.onResume();

		try {
			this.mWakeLock.acquire();

			LocationManager manager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);

			if (shouldShowStartupDialog()) {
				showDialog(MixView.STARTUP_DIALOG_ID);
			}
			
			if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				showDialog(MixView.NO_GPS_ALERT_DIALOG_ID);
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

	
	public boolean shouldShowStartupDialog() {
		return mPrefs.getBoolean("shouldShowStartupDialog", true);
	}

	public void hideStartupDialogInTheFuture() {
		SharedPreferences.Editor edit = mPrefs.edit();
		edit.putBoolean("shouldShowStartupDialog", false);
		edit.commit();
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

		case R.id.mixare_menu_item_gps_info:
			showDialog(GPS_INFO_DIALOG_ID);
			break;

		case R.id.mixare_menu_item_show_hidden_markers:
			mMixContext.resetUserActiveState();
			break;

		//Normally this would be handled in CustomUtils, but it needs access to the dialog, so we do it here
		case R.id.menu_item_elev_range:
			showDialog(MixView.ELEVATION_SELECTOR_DIALOG_ID);
			break;
			
		default:
			return CustomUtils.onOptionsItemSelected(this, item);
		}
		return true;
	}


	@Override
	protected Dialog onCreateDialog(int id) {

		Dialog d;

		switch (id) {
		case MixView.ERROR_DIALOG_ID:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("An error occurred: " + currentException.getMessage());
			builder.setCancelable(true);
			builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			d = builder.create();
			break;

		case MixView.STARTUP_DIALOG_ID:
			d = getStartupDialog();
			break;

		case MixView.NO_GPS_ALERT_DIALOG_ID:
			d = getNoGPSAlertDialog();
			break;

		case MixView.GPS_INFO_DIALOG_ID:
			d = getGPSInfoDialog();
			break;

		case MixView.ELEVATION_SELECTOR_DIALOG_ID:
			d = getElevationSelectorDialog();
			break;

		default: return null;
		}

		currentDialogs.add(d);
		return d;
	}


	private Dialog getNoGPSAlertDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setMessage("This application works much better if GPS is enabled. Enable GPS now?")
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
				dialog.dismiss();
			}
		});

		return builder.create();
	}

	private Dialog getStartupDialog() {
		final Dialog dialog = new Dialog(this, R.style.StartupDialogTheme);
		dialog.setContentView(R.layout.startup_dialog);

		Button okayButton = (Button)dialog.findViewById(R.id.dialog_dismiss_button);
		okayButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				
				CheckBox c = (CheckBox)dialog.findViewById(R.id.dialog_hide_in_future);
				if (c.isChecked()) {
					hideStartupDialogInTheFuture();
				}

				dialog.dismiss();
			}
		});

		Button helpButton = (Button)dialog.findViewById(R.id.dialog_help_button);
		helpButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dialog.dismiss();
				view.getContext().startActivity(new Intent(view.getContext(), HelpView.class));
			}
		});

		Button licenseButton = (Button)dialog.findViewById(R.id.dialog_license_button);
		licenseButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dialog.dismiss();
				view.getContext().startActivity(new Intent(view.getContext(), LicenseView.class));
			}
		});

		return dialog;
	}

	private Dialog getGPSInfoDialog() {
		Location currentGPSInfo = mMixContext.getCurrentLocation();
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setTitle("GPS Information");

		if (null != currentGPSInfo) {
			builder.setMessage("Latitude: " + currentGPSInfo.getLatitude() + "\n" + 
					"Longitude: " + currentGPSInfo.getLongitude() + "\n" +
					"Elevation: " + CustomUtils.metersToFeet(currentGPSInfo.getAltitude()) + " ft.\n" +
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
		return alert;
	}

	private boolean[] getElevationsToDisplayForDialog(ArrayList<String> selectedItems) {
		Resources res = getResources();
		TypedArray strings = res.obtainTypedArray(R.array.elevation_ranges);
		
		boolean[] checkedItems = new boolean[strings.length()];
		
		//This makes us perfectly safe if the order of the dialog changes for any reason, but is a bit clunky
		String range14ers = getResources().getString(R.string.elev_range_14ers);
		String rangeCentennials = getResources().getString(R.string.elev_range_centennials);
		String rangeBiCentennials = getResources().getString(R.string.elev_range_bicentennials);
		String rangeLow13ers = getResources().getString(R.string.elev_range_low_13ers);
		
		for (int ii = 0 ; ii < strings.length() ; ii++) {
			String s = strings.getString(ii);
			
			if (s.equals(range14ers)) {
				checkedItems[ii] = mMixContext.getElevationsToDisplay().show14ers;
			}
			else if (s.equals(rangeCentennials)) {
				checkedItems[ii] = mMixContext.getElevationsToDisplay().showCentennials; 
			}
			else if (s.equals(rangeBiCentennials)) {
				checkedItems[ii] = mMixContext.getElevationsToDisplay().showBiCentennials;
			}
			else if (s.equals(rangeLow13ers)) {
				checkedItems[ii] = mMixContext.getElevationsToDisplay().showLow13ers;
			}
			
			if (checkedItems[ii]) {
				selectedItems.add(s);
			}
		}

		return checkedItems;
	}
	
	private void saveElevationsToDisplay(ArrayList<String> selectedItems) {
		CoPeakIdApp.PeaksToShow peaksToShow = new CoPeakIdApp.PeaksToShow();
		
		peaksToShow.show14ers = selectedItems.contains(getResources().getString(R.string.elev_range_14ers));
		peaksToShow.showCentennials = selectedItems.contains(getResources().getString(R.string.elev_range_centennials));
		peaksToShow.showBiCentennials = selectedItems.contains(getResources().getString(R.string.elev_range_bicentennials)); 
		peaksToShow.showLow13ers = selectedItems.contains(getResources().getString(R.string.elev_range_low_13ers));
		
		mMixContext.setElevationsToDisplay(peaksToShow);
	}

	private Dialog getElevationSelectorDialog() {
		//mSelectedItems = new ArrayList();  // Where we track the selected items
		final ArrayList<String> mSelectedItems = new ArrayList<String>();
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.select_elev_range_dialog_title);
		
		builder.setMultiChoiceItems(R.array.elevation_ranges,
				getElevationsToDisplayForDialog(mSelectedItems),
				new DialogInterface.OnMultiChoiceClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				Resources res = getResources();
				TypedArray strings = res.obtainTypedArray(R.array.elevation_ranges);
				String string = strings.getString(which);
				
				if (isChecked) {
					// If the user checked the item, add it to the selected items
					mSelectedItems.add(string);
				} else if (mSelectedItems.contains(string)) {
					// Else, if the item is already in the array, remove it 
					mSelectedItems.remove(string);
				}
			}
		});

		// Set the action buttons
		builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				saveElevationsToDisplay(mSelectedItems);
				dialog.dismiss();
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				dialog.dismiss();
			}
		});
		
		return builder.create();
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if (!fromUser) {
			//We don't care -- we already know about this change because we're doing it
			return;
		}

		this.mCurrentRangeText.setText(CustomUtils.formatDist(getRangeFromProgress(progress, seekBar.getMax())));
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
