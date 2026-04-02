package com.looksee.pageBuilder.models;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.neo4j.core.schema.Relationship;


public class ImageLandmarkInfo extends LookseeObject{
	
	@Relationship(type="EXISTS_AT")
	private Set<LatLng> latLng;
	private String description;
	private double score;
	
	public ImageLandmarkInfo() {
		setLatLngSet(new HashSet<>());
		setDescription("");
		setScore(0.0);
	}
	
	public ImageLandmarkInfo(Set<LatLng> lat_lng_set, String description, double score) {
		setLatLngSet(lat_lng_set);
		setDescription(description);
		setScore(score);
	}
	
	public Set<LatLng> getLatLngSet() {
		return latLng;
	}
	public void setLatLngSet(Set<LatLng> lat_lng) {
		this.latLng = lat_lng;
	}
	public double getScore() {
		return score;
	}
	public void setScore(double score) {
		this.score = score;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	@Override
	public String generateKey() {
		return "landmarkinfo::"+UUID.randomUUID();
	}
	
}
