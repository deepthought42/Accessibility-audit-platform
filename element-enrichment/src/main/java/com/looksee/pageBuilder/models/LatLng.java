package com.looksee.pageBuilder.models;

/**
 * Latitude and Longitude coordinate object
 */
public class LatLng extends LookseeObject {
	private double latitude;
	private double longitude;
	
	public LatLng() {
		setLatitude(0.0);
		setLongitude(0.0);
	}
	
	public LatLng(double latitude, double longitude) {
		setLatitude(latitude);
		setLongitude(longitude);
		setKey(generateKey());
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	@Override
	public String generateKey() {
		return "latlng::"+getLatitude()+getLongitude();
	}
}
