package com.looksee.pageBuilder.models;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ImageSearchAnnotation extends LookseeObject{
	private float score;
	private Set<String> bestGuessLabel;
	private Set<String> fullMatchingImages;
	private Set<String> similarImages;
	
	public ImageSearchAnnotation() {
		setScore(0.0F);
		setBestGuessLabel(new HashSet<>());
		setFullMatchingImages(new HashSet<>());
		setSimilarImages(new HashSet<>());
	}
	
	public ImageSearchAnnotation(Set<String> best_guess_label,
								 Set<String> full_matching_images,
								 Set<String> similar_images
	) {
		setScore(score);
		setBestGuessLabel(best_guess_label);
		setFullMatchingImages(full_matching_images);
		setSimilarImages(similar_images);
	}
	
	public float getScore() {
		return score;
	}
	public void setScore(float score) {
		this.score = score;
	}
	public Set<String> getBestGuessLabel() {
		return bestGuessLabel;
	}
	public void setBestGuessLabel(Set<String> best_guess_label) {
		this.bestGuessLabel = best_guess_label;
	}
	public Set<String> getFullMatchingImages() {
		return fullMatchingImages;
	}
	public void setFullMatchingImages(Set<String> full_matching_images) {
		this.fullMatchingImages = full_matching_images;
	}
	public Set<String> getSimilarImages() {
		return similarImages;
	}
	public void setSimilarImages(Set<String> similar_images) {
		this.similarImages = similar_images;
	}

	@Override
	public String generateKey() {
		return "imagesearchannotation::"+UUID.randomUUID();
	}
}
