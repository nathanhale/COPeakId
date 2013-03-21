package com.wyeknot.ez_mixare;

import java.text.DecimalFormat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.MenuItem;

import com.wyeknot.copeakid.CoPeakIdApp;
import com.wyeknot.copeakid.HelpView;
import com.wyeknot.copeakid.LicenseView;
import com.wyeknot.copeakid.R;
import com.wyeknot.ez_mixare.data.DataHandler;

public class CustomUtils {
	public static final double METERS_TO_FEET = (double)(1d / 0.3048);
	public static final double FEET_TO_METERS = 0.3048;
	public static final double FEET_TO_MILES = (double)(1d / 5280d);
	
	
	public static boolean onOptionsItemSelected(Context context, MenuItem item) {
		/* Handles menu items from the MixView that aren't specific to Mixare */
		switch(item.getItemId()) {
			case R.id.menu_item_help:
				context.startActivity(new Intent(context, HelpView.class));
				break;
			case R.id.menu_item_license:
				context.startActivity(new Intent(context, LicenseView.class));
				break;
		}
		
		return true;
	}
	
	public static String formatElevation(double elevationInMeters) {
		double feet = elevationInMeters * METERS_TO_FEET;
		
		DecimalFormat df = new DecimalFormat("#,###");
		return df.format(feet) + "'";
	}
	
	public static DataHandler getDataHandler(Activity a) {
		CoPeakIdApp appInfo = (CoPeakIdApp)a.getApplication();
		return appInfo.getMixareDataHandler();
	}
	
	public static String formatDist(double distanceInMeters) {
		return CustomUtils.formatDistFeet(distanceInMeters);
	}
	
	private static String formatDistFeet(double distanceInMeters) {
		double feet = distanceInMeters * METERS_TO_FEET;
		
		DecimalFormat df = new DecimalFormat("@#");
		
		if ((feet * FEET_TO_MILES) < 1) {
			return df.format(feet) + " ft";
		} else {
			String result = df.format(feet * FEET_TO_MILES);
			if (result.equals("1")) {
				result += " mile";
			}
			else {
				result += " miles";
			}
			return result;
		}
	}
	
	public static int metersToFeet(double distanceInMeters) {
		return (int)(distanceInMeters * METERS_TO_FEET);
	}
	
	
	public static SharedPreferences getSharedPreferences(Context context) {
		return context.getSharedPreferences(MixConstants.mixarePreferencesFile, Context.MODE_PRIVATE);
	}
	
	/*private static String formatDistMetric(double distanceInMeters) {
		if (distanceInMeters < 1000) {
			return ((int) distanceInMeters) + "m";
		} else if (distanceInMeters < 10000) {
			return MixUtils.formatDec(distanceInMeters / 1000f, 1) + "km";
		} else {
			return ((int) (distanceInMeters / 1000f)) + "km";
		}
	}
	
	static String formatDec(double val, int dec) {
		int factor = (int) Math.pow(10, dec);

		int front = (int) (val );
		int back = (int) Math.abs(val * (factor) ) % factor;

		return front + "." + back;
	}
	*/
}
