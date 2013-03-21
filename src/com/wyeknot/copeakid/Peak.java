package com.wyeknot.copeakid;

public class Peak {
	
	public String name;
	public int elevation = 0;
	public double latitude = 0;
	public double longitude = 0;
	public int key = 0;
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setElevation(int elevation) {
		this.elevation = elevation;
	}
	
	public void setLatitude(double lat) {
		this.latitude = lat;
	}
	
	public void setLongitude(double lon) {
		this.longitude = lon;
	}
	
	public void setKey(int key) {
		this.key = key;
	}
	
	public boolean isDataComplete() {
		return ((name != null) && (elevation != 0) && (latitude != 0) && (longitude != 0) && (key != 0));
	}
}
