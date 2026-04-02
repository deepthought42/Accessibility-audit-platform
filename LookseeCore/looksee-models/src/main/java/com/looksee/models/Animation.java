package com.looksee.models;

import java.util.Collections;
import java.util.List;

import com.looksee.models.enums.AnimationType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents an animation.
 *
 * invariant: imageUrls != null
 * invariant: animationType != null
 */
@Getter
@Setter
@NoArgsConstructor
public class Animation extends LookseeObject {

	private List<String> imageUrls;
	private List<String> imageChecksums;
	private AnimationType animationType;
	
	/**
	 * Constructor for {@link Animation}
	 *
	 * @param image_urls the list of image URLs
	 * @param image_checksums the list of image checksums
	 * @param type the type of animation
	 *
	 * precondition: image_urls != null
	 * precondition: image_checksums != null
	 * precondition: type != null
	 */
	public Animation(List<String> image_urls, List<String> image_checksums, AnimationType type) {
		assert image_urls != null;
		assert image_checksums != null;
		assert type != null;
		setImageUrls(image_urls);
		setImageChecksums(image_checksums);
		setAnimationType(type);
		setKey(generateKey());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey() {
		String key = "";
		Collections.sort(imageUrls);
		for(String url : imageUrls){
			key += url;
		}
		
		return "animation:"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(key);
	}
}
