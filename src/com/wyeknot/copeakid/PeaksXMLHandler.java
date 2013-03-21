package com.wyeknot.copeakid;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.wyeknot.ez_mixare.CustomUtils;
import com.wyeknot.ez_mixare.Marker;
import com.wyeknot.ez_mixare.StandardMarker;

public class PeaksXMLHandler extends DefaultHandler {
	private Peak curPeak = null;
	private String curField = null;
	private String curChars = null;

	private ArrayList<Marker> mMarkersList;
 
	PeaksXMLHandler(ArrayList<Marker> markersList) {
		super();
		
		this.mMarkersList = markersList;
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {		
		curField = localName;
		curChars = null;
		
		if (localName.equals("peaks")) {
			return;
		}

		if (localName.equals("peak")) {
			//Should always be null when an element is started
			if (curPeak != null) {
				throw new XMLParseException(CoPeakIdApp.PARSE_FAILED_MALFORMED_XML);
			}

			curPeak = new Peak();
		}
	}
 
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		
		if (localName.equals("peak")) {
			if (curPeak.isDataComplete()) {
				
				StandardMarker marker = new StandardMarker("" + curPeak.key, curPeak.name,
						curPeak.latitude, curPeak.longitude, (double)curPeak.elevation * CustomUtils.FEET_TO_METERS,
						CoPeakIdApp.peakClickHandler);
				mMarkersList.add(marker);
				curPeak = null;
				return;
			}
			else {
				throw new XMLParseException(CoPeakIdApp.PARSE_FAILED_REQUIRED_FIELD_MISSING); 
			}
		}
		
		if (localName.equals("peaks")) {
			return;
		}
		
		if ((curPeak == null) || (curField == null)) {
			throw new XMLParseException(CoPeakIdApp.PARSE_FAILED_MALFORMED_XML);
		}
		
		if (null == curChars) {
			return;
		}
		
		if (curField.equals("pname_full")) {	
			curPeak.setName(curChars);
		}
		else if (curField.equals("pelev")) {
			curPeak.setElevation(Utils.parseIntWithDefault(curChars,13999));
		}
		else if (curField.equals("pkey")) {
			curPeak.setKey(Utils.parseIntWithDefault(curChars,0));
		}
		else if (curField.equals("plat")) {
			curPeak.setLatitude(Utils.parseDoubleWithDefault(curChars, 0));
		}
		else if (curField.equals("plon")) {
			curPeak.setLongitude(Utils.parseDoubleWithDefault(curChars, 0));
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		curChars = Utils.parsedStringHandleGaps(curChars, new String(ch,start,length));		
	}

}
