package com.looksee.pageBuilder.models;

import java.util.UUID;

import com.google.cloud.vision.v1.BoundingPoly;
import com.google.cloud.vision.v1.Likelihood;

/**
 * Stores image face annotations
 *
 */
public class ImageFaceAnnotation extends LookseeObject{
	private String locale;
	private float score;
	
	private int x1;
	private int y1;
	
	private int x2;
	private int y2;
	
	private int x3;
	private int y3;
	
	private int x4;
	private int y4;
	
    private String joyLikelihood;
    private String sorrowLikelihood;
    private String angerLikelihood;
    private String surpriseLikelihood;
    private String underExposedLikelihood;
    private String blurredLikelihood;
    private String headwearLikelihood;
    
	public ImageFaceAnnotation() {
		setLocale("");
		setScore(0.0F);
		setX1(0);
		setY1(0);
		
		setX2(0);
		setY2(0);
		
		setX3(0);
		setY3(0);
		
		setX4(0);
		setY4(0);
		
	    setJoyLikelihood("");
	    setSorrowLikelihood("");
	    setAngerLikelihood("");
	    setSurpriseLikelihood("");
	    setUnderExposedLikelihood("");
	    setBlurredLikelihood("");
	    setHeadwearLikelihood("");
    }

	public ImageFaceAnnotation(Likelihood angerLikelihood,
							   Likelihood joyLikelihood,
							   Likelihood blurredLikelihood,
							   Likelihood headwearLikelihood,
							   Likelihood sorrowLikelihood,
							   Likelihood surpriseLikelihood,
							   Likelihood underExposedLikelihood,
							   BoundingPoly bounding_poly
	) {
		setAngerLikelihood(angerLikelihood.toString());
		setJoyLikelihood(joyLikelihood.toString());
		setBlurredLikelihood(blurredLikelihood.toString());
		setHeadwearLikelihood(headwearLikelihood.toString());
		setSorrowLikelihood(sorrowLikelihood.toString());
		setSurpriseLikelihood(surpriseLikelihood.toString());
		setUnderExposedLikelihood(underExposedLikelihood.toString());
		
		setX1(bounding_poly.getVerticesList().get(0).getX());
		setY1(bounding_poly.getVerticesList().get(0).getY());
		
		setX2(bounding_poly.getVerticesList().get(1).getX());
		setY2(bounding_poly.getVerticesList().get(1).getY());
		
		setX3(bounding_poly.getVerticesList().get(2).getX());
		setY3(bounding_poly.getVerticesList().get(2).getY());

		setX4(bounding_poly.getVerticesList().get(3).getX());
		setY4(bounding_poly.getVerticesList().get(3).getY());
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public float getScore() {
		return score;
	}

	public void setScore(float score) {
		this.score = score;
	}

	public int getX1() {
		return x1;
	}

	public void setX1(int x1) {
		this.x1 = x1;
	}

	public int getY1() {
		return y1;
	}

	public void setY1(int y1) {
		this.y1 = y1;
	}

	public int getX2() {
		return x2;
	}

	public void setX2(int x2) {
		this.x2 = x2;
	}

	public int getY2() {
		return y2;
	}

	public void setY2(int y2) {
		this.y2 = y2;
	}

	public int getX3() {
		return x3;
	}

	public void setX3(int x3) {
		this.x3 = x3;
	}

	public int getY3() {
		return y3;
	}

	public void setY3(int y3) {
		this.y3 = y3;
	}

	public int getX4() {
		return x4;
	}

	public void setX4(int x4) {
		this.x4 = x4;
	}

	public int getY4() {
		return y4;
	}

	public void setY4(int y4) {
		this.y4 = y4;
	}

	public String getJoyLikelihood() {
		return joyLikelihood;
	}

	public void setJoyLikelihood(String joyLikelihood) {
		this.joyLikelihood = joyLikelihood;
	}

	public String getSorrowLikelihood() {
		return sorrowLikelihood;
	}

	public void setSorrowLikelihood(String sorrowLikelihood) {
		this.sorrowLikelihood = sorrowLikelihood;
	}

	public String getAngerLikelihood() {
		return angerLikelihood;
	}

	public void setAngerLikelihood(String angerLikelihood) {
		this.angerLikelihood = angerLikelihood;
	}

	public String getSurpriseLikelihood() {
		return surpriseLikelihood;
	}

	public void setSurpriseLikelihood(String surpriseLikelihood) {
		this.surpriseLikelihood = surpriseLikelihood;
	}

	public String getUnderExposedLikelihood() {
		return underExposedLikelihood;
	}

	public void setUnderExposedLikelihood(String underExposedLikelihood) {
		this.underExposedLikelihood = underExposedLikelihood;
	}

	public String getBlurredLikelihood() {
		return blurredLikelihood;
	}

	public void setBlurredLikelihood(String blurredLikelihood) {
		this.blurredLikelihood = blurredLikelihood;
	}

	public String getHeadwearLikelihood() {
		return headwearLikelihood;
	}

	public void setHeadwearLikelihood(String headwearLikelihood) {
		this.headwearLikelihood = headwearLikelihood;
	}

	@Override
	public String generateKey() {
		return "imagefaceannotation::"+UUID.randomUUID();
	}
	
	
}
