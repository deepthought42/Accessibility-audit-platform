package com.looksee.models;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * Stores image landmark info
 */
@Getter
@Setter
public class ImageLandmarkInfo extends LookseeObject{
	
	private String description;
	private double score;

	@Relationship(type="EXISTS_AT")
	private Set<LatLng> latLngSet;
	
	/**
	 * Constructs a new {@link ImageLandmarkInfo}
	 */
	public ImageLandmarkInfo() {
		setLatLngSet(new HashSet<>());
		setDescription("");
		setScore(0.0);
	}
	
	/**
	 * Constructs a new {@link ImageLandmarkInfo}
	 *
	 * @param lat_lng_set the lat lng set of the image landmark info
	 * @param description the description of the image landmark info
	 * @param score the score of the image landmark info
	 *
	 * precondition: lat_lng_set != null
	 * precondition: description != null
	 */
	public ImageLandmarkInfo(Set<LatLng> lat_lng_set, String description, double score) {
		assert lat_lng_set != null;
		assert description != null;

		setLatLngSet(lat_lng_set);
		setDescription(description);
		setScore(score);
	}
	
	/**
	 * Generates a key for the image landmark info
	 *
	 * @return the key for the image landmark info
	 */
	@Override
	public String generateKey() {
		return "landmarkinfo::"+UUID.randomUUID();
	}
	
}
