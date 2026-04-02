package com.looksee.pageBuilder.gcp;

import java.util.UUID;

import com.looksee.pageBuilder.models.LookseeObject;


public class ImageSafeSearchAnnotation extends LookseeObject{
	private String spoof;
	private String medical;
	private String adult;
	private String violence;
	private String racy;
	
	public ImageSafeSearchAnnotation() {
		setAdult("");
		setMedical("");
		setSpoof("");
		setViolence("");
		setRacy("");
	}
	
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
	

	@Override
	public String generateKey() {
		return "imagesearchannotation::"+UUID.randomUUID();
	}

	public String getSpoof() {
		return spoof;
	}

	public void setSpoof(String spoof) {
		this.spoof = spoof;
	}

	public String getMedical() {
		return medical;
	}

	public void setMedical(String medical) {
		this.medical = medical;
	}

	public String getAdult() {
		return adult;
	}

	public void setAdult(String adult) {
		this.adult = adult;
	}

	public String getViolence() {
		return violence;
	}

	public void setViolence(String violence) {
		this.violence = violence;
	}

	public String getRacy() {
		return racy;
	}

	public void setRacy(String racy) {
		this.racy = racy;
	}
}
