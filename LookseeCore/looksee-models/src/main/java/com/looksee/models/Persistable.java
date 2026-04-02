package com.looksee.models;

/**
 * Interface for persistable objects which allows objects to generate a key before saving 
 */
public interface Persistable {
	/**
	 * Generates a key for the object
	 * 
	 * @return string of hashCodes identifying unique fingerprint of object by the contents of the object
	 */
	public String generateKey();
}
