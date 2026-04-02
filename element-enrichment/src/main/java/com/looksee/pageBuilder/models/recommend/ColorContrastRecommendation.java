package com.looksee.pageBuilder.models.recommend;

public class ColorContrastRecommendation extends Recommendation{
	private String color1_rgb;
	private String color2_rgb;
	
	public ColorContrastRecommendation(String color1_rgb, String color2_rgb) {
		setColor1Rgb(color1_rgb);
		setColor2Rgb(color2_rgb);
	}
	
	public String getColor1Rgb() {
		return color1_rgb;
	}
	public void setColor1Rgb(String color1_rgb) {
		this.color1_rgb = color1_rgb;
	}
	public String getColor2Rgb() {
		return color2_rgb;
	}
	public void setColor2Rgb(String color2_rgb) {
		this.color2_rgb = color2_rgb;
	}
}
