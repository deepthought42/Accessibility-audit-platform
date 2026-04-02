package com.looksee.gcp;

import java.util.UUID;

import com.looksee.models.LookseeObject;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a safe search annotation for an image.
 */
@Getter
@Setter
public class ImageSafeSearchAnnotation extends LookseeObject{
	private String spoof;
	private String medical;
	private String adult;
	private String violence;
	private String racy;
	
	/**
	 * Constructs a new ImageSafeSearchAnnotation with all fields set to empty strings.
	 */
	public ImageSafeSearchAnnotation() {
		setAdult("");
		setMedical("");
		setSpoof("");
		setViolence("");
		setRacy("");
	}
	
	/**
	 * Constructs a new ImageSafeSearchAnnotation with the specified fields.
	 * @param spoof the spoof value
	 * @param medical the medical value
	 * @param adult the adult value
	 * @param violence the violence value
	 * @param racy the racy value
	 */
	public ImageSafeSearchAnnotation(String spoof,
										String medical,
										String adult,
										String violence,
										String racy
	) {
		setSpoof(spoof);
		setMedical(medical);
		setAdult(adult);
		setViolence(violence);
		setRacy(racy);
	}
	
	/**
	 * Generates a unique key for the image safe search annotation.
	 * @return the unique key
	 */
	@Override
	public String generateKey() {
		return "imagesearchannotation::"+UUID.randomUUID();
	}
}
