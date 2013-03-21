package com.wyeknot.copeakid;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.wyeknot.ez_mixare.CustomUtils;
import com.wyeknot.ez_mixare.Marker;
import com.wyeknot.ez_mixare.MixConstants;
import com.wyeknot.ez_mixare.data.DataHandler;

public class CoPeakIdApp extends Application {
	
	public static final int INTERNAL_ERROR_RESULT = -2;
	public static final int PARSE_SUCCESS_RESULT = 1;
	public static final int PARSE_FAILED_RESULT = 2;
	public static final int PARSE_FAILED_FILE_NOT_FOUND = 3;
	public static final int PARSE_FAILED_MALFORMED_XML = 4;
	public static final int PARSE_FAILED_REQUIRED_FIELD_MISSING = 5;
	public static final int PARSE_FAILED_NETWORK_ERROR = 6;
	public static final int PARSE_FAILED_PEAK_NOT_FOUND = 7;
	
	public static final String SHOW_14ERS_PREFS_KEY = "show_14ers";
	public static final String SHOW_CENTENNIALS_PREFS_KEY = "show_centennials";
	public static final String SHOW_BICENTENNIALS_PREFS_KEY = "show_bicentennials";
	public static final String SHOW_LOW_13ERS_PREFS_KEY = "show_low_13ers";
	

	private SharedPreferences mPrefs;
	
	/** in meters */
	private double range = MixConstants.defaultRange; //This is the default, the current val will be read from preferences
	
	private CoPeakIdApp.PeaksToShow elevationsToDisplay = MixConstants.defaultElevationsToDisplay;
	
	
	
	public static class PeaksToShow {
		public boolean show14ers = true;
		public boolean showCentennials = true;
		public boolean showBiCentennials = true;
		public boolean showLow13ers = true;
		
		//In meters, the low part of that elevation range
		public static final double elevCutoff14ers = 4267;
		public static final double elevCutoffCentennials = 4208.5;
		public static final double elevCutoffBiCentennials = 4139; //Will show a couple of extras
		//These are just in case I need them later, and some of them will show a couple of extras
		public static final double elevCutoffTriCentennials = 4093.77;
		public static final double elevCutoffQuadCentennials = 4053.53;
		public static final double elevCutoffQuintCentennials = 4016.95;
		public static final double elevCutoffLow13ers = 0;
		
		//Just uses the default values
		public PeaksToShow() { }
		
		public PeaksToShow(boolean show14ers, boolean showCentennials, boolean showBiCentennials, boolean showLow13ers) {
			this.show14ers = show14ers;
			this.showCentennials = showCentennials;
			this.showBiCentennials = showBiCentennials;
			this.showLow13ers = showLow13ers;
		}
	}
	
	/* These values are sent back as the arg1 param of our messages */
	public static final int PARSE_PEAKS_MSG = 1;
	
	public static final String PEAKS_XML_LOCAL_SOURCE = "assets/all_peaks.xml";
	
	//Mixare data handler
	private DataHandler mDataHandler;
	private ArrayList<Marker> mMarkersList;

	//Message handler
	private Handler currentActivityResultsHandler = null;
	
	private ParseResult peaksParseResult = null;
	
	private final Handler resultsHandler = new Handler() {
		public void handleMessage(Message msg) {
			ParseResult parseResultVar = null;
			
			if (CoPeakIdApp.PARSE_PEAKS_MSG == msg.arg1) {
				parseResultVar = peaksParseResult;
				
				setMixareMarkers();
			}
			else {
				Log.e("CoPeakIdApp","unknown message " + msg.arg1 + " complete. This shouldn't happen");
				return;
			}

			setAndSendResults(msg.arg1, msg.arg2, parseResultVar);
		}
	};
	
	public static Marker.ClickHandler peakClickHandler = new Marker.ClickHandler() {
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


	/* These functions could be used to display error messages to the user in various activities.
	 * Because this xml file is completely local, that's not an issue right now, though it
	 * might be if files were later pulled from the internet.
	 */
	public synchronized ParseResult getParseStatusAndRegisterForResult(int type, Handler handler) {
		ParseResult parseResultVar = getParseResults(type);
		if (null == parseResultVar) {
			return null;
		}
		
		if (!parseResultVar.dataAvailable()) {
			if (parseResultVar.parseWaitNeeded()) {
				currentActivityResultsHandler = handler;
			}
		}

		return parseResultVar;
	}
	
	public synchronized void unregisterResultsHandler(Handler handler) {
		if (handler == currentActivityResultsHandler) {
			currentActivityResultsHandler = null;
		}
	}
	
	public synchronized ParseResult getParseResults(int type) {
		switch (type) {
		case CoPeakIdApp.PARSE_PEAKS_MSG:
			return peaksParseResult;
		default:
			return null;
		}
	}
	
	private synchronized void setAndSendResults(int type, int result, ParseResult pr) {
		pr.setResult(result);
		if (currentActivityResultsHandler != null) {
			Message msg = currentActivityResultsHandler.obtainMessage();
			msg.arg1 = type;
			msg.arg2 = result;
			currentActivityResultsHandler.sendMessage(msg);
		}
	}
	
	private void setMixareMarkers() {
		mDataHandler.addMarkers(mMarkersList);
		mDataHandler.sortMarkerList();
	}
	
	
	public DataHandler getMixareDataHandler() {
		return mDataHandler;
	}
	
	public double getRange() {
		return range;
	}
	
	private void getRangeFromPrefs() {
		float newRange = mPrefs.getFloat(MixConstants.mixareRangeItemName, (float)range);	
		range = newRange;
	}
	
	public void setRange(double r) {
		range = r;

		SharedPreferences.Editor editor = mPrefs.edit();
		/* store the zoom range of the zoom bar selected by the user */
		editor.putFloat(MixConstants.mixareRangeItemName, (float)r);
		editor.commit();
	}
	
	public CoPeakIdApp.PeaksToShow getElevationsToDisplay() {
		return elevationsToDisplay;
	}
	
	private void getElevationsToDisplayFromPrefs() {
		elevationsToDisplay.show14ers = mPrefs.getBoolean(CoPeakIdApp.SHOW_14ERS_PREFS_KEY, true);
		elevationsToDisplay.showCentennials = mPrefs.getBoolean(CoPeakIdApp.SHOW_CENTENNIALS_PREFS_KEY, true);
		elevationsToDisplay.showBiCentennials = mPrefs.getBoolean(CoPeakIdApp.SHOW_BICENTENNIALS_PREFS_KEY, true);
		elevationsToDisplay.showLow13ers = mPrefs.getBoolean(CoPeakIdApp.SHOW_LOW_13ERS_PREFS_KEY, true);
	}

	public void setElevationsToDisplay(CoPeakIdApp.PeaksToShow elevationsToDisplay) {	
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putBoolean(CoPeakIdApp.SHOW_14ERS_PREFS_KEY, elevationsToDisplay.show14ers);
		editor.putBoolean(CoPeakIdApp.SHOW_CENTENNIALS_PREFS_KEY, elevationsToDisplay.showCentennials);
		editor.putBoolean(CoPeakIdApp.SHOW_BICENTENNIALS_PREFS_KEY, elevationsToDisplay.showBiCentennials);
		editor.putBoolean(CoPeakIdApp.SHOW_LOW_13ERS_PREFS_KEY, elevationsToDisplay.showLow13ers);
		editor.commit();
		
		this.elevationsToDisplay = elevationsToDisplay;
	}
	

	@Override
	public void onCreate() {
		super.onCreate();
		
		peaksParseResult = new ParseResult();
		
		mPrefs = CustomUtils.getSharedPreferences(getBaseContext());
		
		//Make sure to call this before calling anything that could potentially need it
		getRangeFromPrefs();
		getElevationsToDisplayFromPrefs();
		
		mDataHandler = new DataHandler();
		mMarkersList = new ArrayList<Marker>(100);
		
		parsePeaks(mMarkersList);
	}
	
	private void parsePeaks(ArrayList<Marker> markers) {
		PeaksXMLHandler peakssXMLHandler = new PeaksXMLHandler(markers);
		XMLParserThread peaksThread = new XMLParserThread(this,CoPeakIdApp.PARSE_PEAKS_MSG,resultsHandler,
				peakssXMLHandler,PEAKS_XML_LOCAL_SOURCE);
		peaksThread.start();
	}
	
	
	public static class ParseResult {
		private boolean parseComplete;
		private int parseResult;
		
		ParseResult() {
			parseComplete = false;
			parseResult = 0;
		}
		
		ParseResult(boolean isManual) {
			parseComplete = false;
			parseResult = 0;
		}
		
		private void setResult(int result) {
			parseComplete = true;
			parseResult = result;
		}
		
		//This tells an Activity whether it should bother waiting for data. Will be called only if data is not available.
		public boolean parseWaitNeeded() {
			return (parseComplete == false);
		}
		
		public int getResult() {
			return parseResult;
		}
		
		public boolean dataAvailable() {
			return (parseComplete && (parseResult == CoPeakIdApp.PARSE_SUCCESS_RESULT));
		}
	}
}
