package com.livetracker;

public class MyLocation {
	private int id;
	private double lat;
	private double lng;
	private String time;
	
	
	public MyLocation(int id, double lat, double lng, String time) {
		super();
		this.id = id;
		this.lat = lat;
		this.lng = lng;
		this.time = time;
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public double getLat() {
		return lat;
	}
	public void setLat(double lat) {
		this.lat = lat;
	}
	public double getLng() {
		return lng;
	}
	public void setLng(double lng) {
		this.lng = lng;
	}
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}
	
	@Override
	public String toString() {
		return id + ", " + lat + ", " + lng
				+ ", " + time;
	}
}
