package com.looksee.frontEndBroadcaster.models;

/**
 * Interface for persistable objects which allows objects to generate a key before saving 
 *
 * @param <V>
 */
public interface Persistable {
	/**
	 * @return string of hashCodes identifying unique fingerprint of object by the contents of the object
	 */
	public String generateKey();
}
