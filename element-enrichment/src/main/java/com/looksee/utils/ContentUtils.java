package com.looksee.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ContentUtils {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(ContentUtils.class);

	public static String getReadingGradeLevel(double ease_of_reading_score) {
		if(ease_of_reading_score >= 90) {
			return "5th grade";
		}
		else if(ease_of_reading_score < 90 && ease_of_reading_score >= 80) {
			return "6th grade";
		}
		else if(ease_of_reading_score < 80 && ease_of_reading_score >= 70) {
			return "7th grade";
		}
		else if(ease_of_reading_score < 70 && ease_of_reading_score >= 60) {
			return "8th and 9th grade";
		}
		else if(ease_of_reading_score < 60 && ease_of_reading_score >= 50) {
			return "10th to 12th grade";
		}
		else if(ease_of_reading_score < 50 && ease_of_reading_score >= 30) {
			return "college";
		}
		else if(ease_of_reading_score < 30 && ease_of_reading_score >= 10) {
			return "college graduate";
		}
		else if(ease_of_reading_score < 10) {
			return "professional";
		}
		return "unknown";
	}
	
	public static String getReadingDifficultyRating(double ease_of_reading_score) {
		if(ease_of_reading_score >= 90) {
			return "very easy";
		}
		else if(ease_of_reading_score < 90 && ease_of_reading_score >= 80) {
			return "easy";
		}
		else if(ease_of_reading_score < 80 && ease_of_reading_score >= 70) {
			return "fairly easy";
		}
		else if(ease_of_reading_score < 70 && ease_of_reading_score >= 60) {
			return "somewhat difficult";
		}
		else if(ease_of_reading_score < 60 && ease_of_reading_score >= 50) {
			return "fairly difficult";
		}
		else if(ease_of_reading_score < 50 && ease_of_reading_score >= 30) {
			return "difficult";
		}
		else if(ease_of_reading_score < 30 && ease_of_reading_score >= 10) {
			return "very difficult";
		}
		else if(ease_of_reading_score < 10) {
			return "extremely difficult";
		}
		return "unknown";
	}

	/**
	 * Returns a reading difficulty string based on the provided education level
	 * @param ease_of_reading_score
	 * @param targetUserEducation
	 * @return
	 */
	public static String getReadingDifficultyRatingByEducationLevel(double ease_of_reading_score,
																	String targetUserEducation) {
		if(targetUserEducation == null) {
			return getReadingDifficultyRatingForHS(ease_of_reading_score);
		}
		else if("HS".contentEquals(targetUserEducation)){
			return getReadingDifficultyRatingForHS(ease_of_reading_score);
		}
		else if("College".contentEquals(targetUserEducation)) {
			return getReadingDifficultyRatingForCollege(ease_of_reading_score);
		}
		else if("Advanced".contentEquals(targetUserEducation)) {
			return getReadingDifficultyRatingForAdvancedDegrees(ease_of_reading_score);
		}
		else {
			return getReadingDifficultyRatingForHS(ease_of_reading_score);
		}
	}
	
	
	private static String getReadingDifficultyRatingForHS(double ease_of_reading_score) {
		if(ease_of_reading_score >= 90) {
			return "very easy";
		}
		else if(ease_of_reading_score < 90 && ease_of_reading_score >= 80) {
			return "easy";
		}
		else if(ease_of_reading_score < 80 && ease_of_reading_score >= 70) {
			return "fairly easy";
		}
		else if(ease_of_reading_score < 70 && ease_of_reading_score >= 60) {
			return "somewhat difficult";
		}
		else if(ease_of_reading_score < 60 && ease_of_reading_score >= 50) {
			return "fairly difficult";
		}
		else if(ease_of_reading_score < 50 && ease_of_reading_score >= 30) {
			return "difficult";
		}
		else if(ease_of_reading_score < 30 && ease_of_reading_score >= 10) {
			return "very difficult";
		}
		else if(ease_of_reading_score < 10) {
			return "extremely difficult";
		}
		return "unknown";
	}
	
	private static String getReadingDifficultyRatingForCollege(double ease_of_reading_score) {
		if(ease_of_reading_score >= 90) {
			return "very easy";
		}
		else if(ease_of_reading_score < 90 && ease_of_reading_score >= 80) {
			return "very easy";
		}
		else if(ease_of_reading_score < 80 && ease_of_reading_score >= 70) {
			return "easy";
		}
		else if(ease_of_reading_score < 70 && ease_of_reading_score >= 60) {
			return "fairly easy";
		}
		else if(ease_of_reading_score < 60 && ease_of_reading_score >= 50) {
			return "somewhat difficult";
		}
		else if(ease_of_reading_score < 50 && ease_of_reading_score >= 30) {
			return "fairly difficult";
		}
		else if(ease_of_reading_score < 30 && ease_of_reading_score >= 10) {
			return "difficult";
		}
		else if(ease_of_reading_score < 10) {
			return "very difficult";
		}
		return "unknown";
	}
	
	private static String getReadingDifficultyRatingForAdvancedDegrees(double ease_of_reading_score) {
		if(ease_of_reading_score >= 90) {
			return "very easy";
		}
		else if(ease_of_reading_score < 90 && ease_of_reading_score >= 80) {
			return "very easy";
		}
		else if(ease_of_reading_score < 80 && ease_of_reading_score >= 70) {
			return "easy";
		}
		else if(ease_of_reading_score < 70 && ease_of_reading_score >= 60) {
			return "easy";
		}
		else if(ease_of_reading_score < 60 && ease_of_reading_score >= 50) {
			return "fairly easy";
		}
		else if(ease_of_reading_score < 50 && ease_of_reading_score >= 30) {
			return "somewhat difficult";
		}
		else if(ease_of_reading_score < 30 && ease_of_reading_score >= 10) {
			return "fairly difficult";
		}
		else if(ease_of_reading_score < 10) {
			return "difficult";
		}
		return "unknown";
	}

}
