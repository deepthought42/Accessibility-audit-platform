package com.looksee.models;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Stores image search annotation
 */
@Getter
@Setter
public class ImageSearchAnnotation extends LookseeObject{
	private float score;
	private Set<String> bestGuessLabel;
	private Set<String> fullMatchingImages;
	private Set<String> similarImages;
	
	/**
	 * Constructs a new {@link ImageSearchAnnotation}
	 */
	public ImageSearchAnnotation() {
		setScore(0.0F);
		setBestGuessLabel(new HashSet<>());
		setFullMatchingImages(new HashSet<>());
		setSimilarImages(new HashSet<>());
	}
	
	/**
	 * Constructs a new {@link ImageSearchAnnotation}
	 *
	 * @param best_guess_label the best guess label of the image search annotation
	 * @param full_matching_images the full matching images of the image search annotation
	 * @param similar_images the similar images of the image search annotation
	 *
	 * precondition: best_guess_label != null
	 * precondition: full_matching_images != null
	 * precondition: similar_images != null
	 */
	public ImageSearchAnnotation(Set<String> best_guess_label,
								Set<String> full_matching_images,
								Set<String> similar_images
	) {
		assert best_guess_label != null;
		assert full_matching_images != null;
		assert similar_images != null;

		setScore(score);
		setBestGuessLabel(best_guess_label);
		setFullMatchingImages(full_matching_images);
		setSimilarImages(similar_images);
	}

	/**
	 * Generates a key for the image search annotation
	 *
	 * @return the key for the image search annotation
	 */
	@Override
	public String generateKey() {
		return "imagesearchannotation::"+UUID.randomUUID();
	}
}
