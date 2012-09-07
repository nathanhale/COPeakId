package com.wyeknot.ez_mixare;

import java.text.DecimalFormat;

import com.wyeknot.ez_mixare.data.DataHandler;

import android.app.Activity;
import android.content.Context;
import android.view.MenuItem;

public class GenericMixUtils {
	public static final double METERS_TO_FEET = (double)(1d / 0.3048);
	public static final double FEET_TO_METERS = 0.3048;
	public static final double FEET_TO_MILES = (double)(1d / 5280d);

	public static String formatDist(double distanceInMeters) {
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
	
	public static String formatElevation(double elevationInMeters) {
		double feet = elevationInMeters * METERS_TO_FEET;
		
		DecimalFormat df = new DecimalFormat("#,###");
		return df.format(feet) + "'";
	}
	
	public static boolean onOptionsItemSelected(Context context, MenuItem item) {
		/* If you wanted to handle some non-mixare menu items from within mixare
		 * without editing the other code, you could handle them here instead.
		 * 
		 * e.g.:
		 * 
		 * switch (item) {
		 *   case R.id.menu_item_help:
		 *   	//Launch Activity
		 * }
		 */
		return true;
	}
	
	public static DataHandler getDataHandler(Activity a) {
		MixareApp appInfo = (MixareApp)a.getApplication();
		return appInfo.getMixareDataHandler();
	}
}
