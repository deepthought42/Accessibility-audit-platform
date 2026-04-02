package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.looksee.models.audit.Audit;

/**
 * Defines the categories of {@link Audit audits} that exist in the system.
 * Each category represents a different aspect of website evaluation:
 * <ul>
 *   <li>{@link #CONTENT} - Evaluates the quality and relevance of website content</li>
 *   <li>{@link #INFORMATION_ARCHITECTURE} - Assesses the organization and structure of information</li>
 *   <li>{@link #AESTHETICS} - Reviews the visual design and appearance</li>
 *   <li>{@link #ACCESSIBILITY} - Checks compliance with accessibility standards</li>
 * </ul>
 *
 * <p><b>Class Invariants:</b>
 * <ul>
 *   <li>Every category must have a non-null, non-empty short name</li>
 *   <li>The short name must be unique across all categories</li>
 *   <li>Categories are immutable after creation</li>
 * </ul>
 *
 * @see Audit
 */
public enum AuditCategory {
	/**
	 * Content audit category.
	 * Evaluates the quality, relevance, and effectiveness of website content.
	 */
	CONTENT("CONTENT"),

	/**
	 * Information Architecture audit category.
	 * Assesses how information is organized, structured, and presented on the website.
	 */
	INFORMATION_ARCHITECTURE("INFORMATION_ARCHITECTURE"),

	/**
	 * Aesthetics audit category.
	 * Reviews the visual design, layout, and overall appearance of the website.
	 */
	AESTHETICS("AESTHETICS"),

	/**
	 * Accessibility audit category.
	 * Evaluates compliance with accessibility standards and guidelines.
	 */
	ACCESSIBILITY("ACCESSIBILITY");
	
	/**
	 * The short name identifier for this audit category.
	 * Used for serialization and string representation.
	 */
	private final String shortName;

	/**
	 * Creates a new audit category with the specified short name.
	 *
	 * @param shortName the short name identifier for this category. Must not be null or empty
	 */
	AuditCategory(String shortName) {
		this.shortName = shortName;
	}

	/**
	 * Returns the short name of this audit category.
	 * This method is used for string representation and serialization.
	 *
	 * @return the short name of this audit category
	 */
	@Override
	public String toString() {
		return shortName;
	}

	/**
	 * Creates an AuditCategory instance from a string value.
	 * This method is used for JSON deserialization and is case-insensitive.
	 *
	 * @param value the string value to convert to an AuditCategory
	 * @return the corresponding AuditCategory instance
	 * @throws IllegalArgumentException if the value does not match any audit category
	 */
	@JsonCreator
	public static AuditCategory create(String value) {
		if (value == null) {
			throw new IllegalArgumentException("Audit category value cannot be null");
		}
		for (AuditCategory v : values()) {
			if (value.equalsIgnoreCase(v.getShortName())) {
				return v;
			}
		}
		throw new IllegalArgumentException("Invalid audit category: " + value);
	}

	/**
	 * Gets the short name identifier of this audit category.
	 *
	 * @return the short name of this audit category
	 */
	public String getShortName() {
		return shortName;
	}
}