package com.wyeknot.copeakid;


public class Utils {

	//Sometimes in parsing there are gaps due to buffering and/or newline characters.
	public static String parsedStringHandleGaps(String curVal, String add) {
		
		if (add.length() == 1) {
			byte byteVal = add.getBytes()[0];
			
			if (byteVal == 13) {
				add = "\n";
			}
			else if (byteVal == 34) {
				add = "\"";
			}
			else if (byteVal == (byte)'&') {
				add = "";
			}
		}
		
		if (add.length() >= 4) {
			add = add.replace("quot;", "\"");
			add = add.replace("amp;","&");
			add = add.replace("deg;",Character.toString('\u00b0'));
			add = add.replace("nbsp;", " ");
		}

		if (null == curVal) {
			return add;
		}
		else {
			return curVal + add;
		}
	}
	
	//Assumes that parseString is checked as non-null
	public static int parseIntWithDefault(String parseString, int defaultVal) {
		try {
			return Integer.parseInt(parseString);
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}
	
	public static double parseDoubleWithDefault(String parseString, double defaultVal) {
		try {
			return Double.parseDouble(parseString);
	 	} catch (NumberFormatException e) {
	 		return defaultVal;
	 	}
	}
	

	public static String getErrorString(int error) {
		switch (error) {
		case CoPeakIdApp.INTERNAL_ERROR_RESULT:
			return "An internal error occurred: <" + error + ">";
		case CoPeakIdApp.PARSE_SUCCESS_RESULT:
			return "Parse succeeded. You shouldn't be seeing this message: <" + error + ">";
		case CoPeakIdApp.PARSE_FAILED_RESULT:
			return "Couldn't parse XML Data: <" + error + ">";
		case CoPeakIdApp.PARSE_FAILED_FILE_NOT_FOUND:
			return "File not found: <" + error + ">";
		case CoPeakIdApp.PARSE_FAILED_MALFORMED_XML:
			return "Couldn't parse XML data: malformed XML from server: <" + error + ">";
		case CoPeakIdApp.PARSE_FAILED_REQUIRED_FIELD_MISSING:
			return "Couldn't parse XML data: a required field was missing from server data: <" + error + ">";
		case CoPeakIdApp.PARSE_FAILED_NETWORK_ERROR:
			return "Network connection error <" + error + ">";
		case CoPeakIdApp.PARSE_FAILED_PEAK_NOT_FOUND:
			return "Couldn't parse XML data: peak not found: <" + error + ">";
		default:
			return "Error " + error;
		}
	}
}
