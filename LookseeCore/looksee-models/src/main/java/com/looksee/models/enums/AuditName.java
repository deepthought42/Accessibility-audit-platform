package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.looksee.models.audit.Audit;

/**
 * Defines all types of {@link Audit audits} that exist in the system.
 * Each audit type represents a specific aspect of website accessibility and design that can be evaluated.
 * The enum provides both a programmatic name and a human-readable short name for each audit type.
 *
 * <p><b>Class Invariants:</b>
 * <ul>
 *   <li>Every audit name must have a non-null, non-empty short name</li>
 *   <li>The short name must be unique across all audit types</li>
 *   <li>The UNKNOWN audit type must always exist as a fallback</li>
 *   <li>All audit names must be immutable after creation</li>
 * </ul>
 *
 * @see Audit
 */
public enum AuditName {
	/**
	 * Evaluates the color palette used throughout the website for consistency and accessibility
	 */
	COLOR_PALETTE("Color_Palette"),

	/**
	 * Checks the contrast ratio between text and its background to ensure readability
	 */
	TEXT_BACKGROUND_CONTRAST("Text_Background_Contrast"),

	/**
	 * Evaluates contrast ratios for non-text elements against their backgrounds
	 */
	NON_TEXT_BACKGROUND_CONTRAST("Non_Text_Background_Contrast"),

	/**
	 * Analyzes link styling, visibility, and accessibility features
	 */
	LINKS("Links"),

	/**
	 * Reviews the typefaces used throughout the website for consistency and readability
	 */
	TYPEFACES("Typefaces"),

	/**
	 * Evaluates font usage, including size, weight, and style consistency
	 */
	FONT("Font"),

	/**
	 * Checks padding usage and consistency across elements
	 */
	PADDING("Padding"),

	/**
	 * Analyzes margin usage and consistency across elements
	 */
	MARGIN("Margin"),

	/**
	 * Evaluates the units of measurement used throughout the website
	 */
	MEASURE_UNITS("Measure_Units"),

	/**
	 * Reviews heading hierarchy and title formatting
	 */
	TITLES("Titles"),

	/**
	 * Checks for presence and quality of alternative text for images
	 */
	ALT_TEXT("Alt_Text"),

	/**
	 * Evaluates paragraph formatting and text block organization
	 */
	PARAGRAPHING("Paragraphing"),

	/**
	 * Analyzes metadata completeness and accuracy
	 */
	METADATA("Metadata"),

	/**
	 * Represents an unknown or unspecified audit type
	 */
	UNKNOWN("Unknown"),

	/**
	 * Checks image copyright compliance and attribution
	 */
	IMAGE_COPYRIGHT("Image_Copyright"),

	/**
	 * Evaluates compliance with image usage policies
	 */
	IMAGE_POLICY("Image Policy"),

	/**
	 * Analyzes text complexity and readability scores
	 */
	READING_COMPLEXITY("Reading_Complexity"),

	/**
	 * Evaluates the security of the website
	 */
	ENCRYPTED("ENCRYPTED"),

	/**
	 * Evaluates the structure of the header
	 */
	HEADER_STRUCTURE("Header_Structure"),
	
	/**
	 * Evaluates the structure of the table
	 */
	TABLE_STRUCTURE("Table_Structure"),
	
	/**
	 * Evaluates the structure of the form
	 */
	FORM_STRUCTURE("Form_Structure"),
	
	/**
	 * Evaluates the orientation of the page
	 */
	ORIENTATION("Orientation"),
	
	/**
	 * Evaluates the purpose of the input
	 */
	INPUT_PURPOSE("Input_Purpose"),

	/**
	 * Evaluates the purpose of the identify
	 */
	IDENTIFY_PURPOSE("Identify_Purpose"),
	
	/**
	 * Evaluates the use of color
	 */
	USE_OF_COLOR("Use_of_Color"),
	
	/**
	 * Evaluates the control of the audio
	 */
	AUDIO_CONTROL("Audio_Control"),
	
	/**
	 * Evaluates the visual presentation of the page
	 */
	VISUAL_PRESENTATION("Visual_Presentation"),
	
	/**
	 * Evaluates the reflow of the page
	 */
	TEXT_SPACING("Text_Spacing"),

	/**
	 * Evaluates the language of the page
	 */
	PAGE_LANGUAGE("Page_Language"),
	
	/**
	 * Evaluates the structure of the video
	 */
	VIDEO_STRUCTURE("Video_Structure"),
	
	/**
	 * Evaluates the structure of the link
	 */
	LINK_STRUCTURE("Link_Structure"),
	
	/**
	 * Evaluates the structure of the button
	 */
	BUTTON_STRUCTURE("Button"),
	
	/**
	 * Evaluates the structure of the input
	 */
	INPUT_STRUCTURE("Input"),
	
	/**
	 * Evaluates the structure of the list
	 */
	LIST_STRUCTURE("List"),

	/**
	 * Evaluates the structure of the reflow
	 */
	REFLOW("Reflow");

	/**
	 * The human-readable name of the audit type
	 */
	private final String shortName;

	/**
	 * Constructs an AuditName enum value with its corresponding short name.
	 *
	 * @param shortName the human-readable name for this audit type
	 */
	AuditName(String shortName) {
		this.shortName = shortName;
	}

	/**
	 * Returns the human-readable short name of this audit type.
	 *
	 * @return the short name of the audit type
	 */
	@Override
	public String toString() {
		return shortName;
	}

	/**
	 * Creates an AuditName enum value from a string representation.
	 * This method is used by Jackson for JSON deserialization.
	 *
	 * @param value the string value to convert to an AuditName
	 * @return the corresponding AuditName enum value
	 * @throws IllegalArgumentException if the value does not match any audit name
	 * @see JsonCreator
	 */
	@JsonCreator
	public static AuditName create(String value) {
		if(value == null) {
			return UNKNOWN;
		}
		for(AuditName v : values()) {
			if(value.equalsIgnoreCase(v.getShortName())) {
				return v;
			}
		}
		throw new IllegalArgumentException("Invalid audit name: " + value);
	}

	/**
	 * Gets the human-readable short name of this audit type.
	 *
	 * @return the short name of the audit type
	 */
	public String getShortName() {
		return shortName;
	}
}
