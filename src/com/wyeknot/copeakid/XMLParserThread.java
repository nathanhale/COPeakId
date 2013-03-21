package com.wyeknot.copeakid;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class XMLParserThread extends Thread {
	Context mContext; //The associated context
	int mParseType;
	Handler mHandler;
	DefaultHandler mXMLHandler;
	String mXmlLocalSource;

	XMLParserThread(Context context, int parseType, Handler handler, DefaultHandler xmlHandler, String xmlLocalSource) {
		this.mContext = context;
		this.mParseType = parseType;
		this.mHandler = handler;
		this.mXMLHandler = xmlHandler;
		this.mXmlLocalSource = xmlLocalSource;		
	}
	
	public void run() {
		int result = CoPeakIdApp.PARSE_FAILED_RESULT;

		if (null != mXmlLocalSource) {
			result = parseXMLLocal();
		}

		sendResponse(result);
		System.gc();		
	}
	
	private void sendResponse(int result) {
		if (null == mHandler) {
			return;
		}

		Message msg = mHandler.obtainMessage();
		msg.arg1 = mParseType;
		msg.arg2 = result;
        mHandler.sendMessage(msg);
	}
	
	private int parseXMLLocal() {
		Log.i("XMLParserThread","parseXMLLocal from " + mXmlLocalSource);
		try {			
			//This is for a local file in the assets dir, most likely, so needs to be opened differently
			if (mXmlLocalSource.contains("assets/")) {
				String fileName = mXmlLocalSource.split("/")[1];

				return parseXML(mContext.getAssets().open(fileName));
			}
			else {
				return parseXML(mContext.openFileInput(mXmlLocalSource));
			}
			
		} catch (FileNotFoundException e) {
			Log.i("XMLParserThread","couldn't parse from local source " + mXmlLocalSource);
			return CoPeakIdApp.PARSE_FAILED_FILE_NOT_FOUND;
		} catch (IOException e) {
			Log.i("XMLParserThread","couldn't parse from assets directory: " + mXmlLocalSource);
			return CoPeakIdApp.PARSE_FAILED_FILE_NOT_FOUND;
		}
	}

	private int parseXML(InputStream i) {
		try {
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();

			xr.setContentHandler(mXMLHandler);			
			xr.parse(new InputSource(i));
			
		} catch (XMLParseException e) {
			Log.i("XMLParserThread","Couldn't parse xml from input stream: " + e.getExceptionType());
			return e.getExceptionType();
		} catch (Exception e) {
			e.printStackTrace();
			return CoPeakIdApp.PARSE_FAILED_RESULT;
		}

		return CoPeakIdApp.PARSE_SUCCESS_RESULT;
	}
}
