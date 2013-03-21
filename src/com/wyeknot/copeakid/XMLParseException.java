package com.wyeknot.copeakid;

import org.xml.sax.SAXException;


public class XMLParseException extends SAXException {

	/* Because of the way messages are passed within the application, the exceptionTypes are
	 * all defined in FourteenersApp
	 */
	
	private static final long serialVersionUID = 5875996559060923181L;
	
	private final int exceptionType;

	XMLParseException(String message, int id) {
		super(message);
		this.exceptionType = id;
	}
	
	XMLParseException(int id) {
		super(new String("XML Parse Exception " + id));
		this.exceptionType = id;
	}
	
	public int getExceptionType() {
		return exceptionType;
	}
}
