package com.looksee.models.audit;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents the audit score of a page.
 *
 * <p><b>Invariants:</b>
 * <ul>
 *   <li>All score values represent valid numeric scores.</li>
 * </ul>
 */
@Getter
@Setter
public class AuditScore {

	private double contentScore;
	private double readability;
	private double spellingGrammar;
	private double imageQuality;
	private double altText;
	private double informationArchitectureScore;
	private double links;
	private double metadata;
	private double seo;
	private double security;
	private double aestheticsScore;
	private double colorContrastScore;
	private double whitespaceScore;
	private double interactivityScore;
	private double accessibilityScore;
	private double textContrastScore;
	private double nonTextContrastScore;
	
	/**
	 * Constructor for the AuditScore class
	 *
	 * @param content_score the content score
	 * @param readability the readability score
	 * @param spelling_grammar the spelling and grammar score
	 * @param image_quality the image quality score
	 * @param alt_text the alt text score
	 * @param information_architecture_score the information architecture score
	 * @param links the links score
	 * @param metadata the metadata score
	 * @param seo the seo score
	 * @param security the security score
	 * @param aesthetic_score the aesthetics score
	 * @param color_contrast the color contrast score
	 * @param whitespace the whitespace score
	 * @param interactivity_score the interactivity score
	 * @param accessibility_score the accessibility score
	 * @param text_contrast the text contrast score
	 * @param non_text_contrast the non-text contrast score
	 */
	public AuditScore(double content_score,
						double readability,
						double spelling_grammar,
						double image_quality,
						double alt_text,
						double information_architecture_score,
						double links,
						double metadata,
						double seo,
						double security,
						double aesthetic_score,
						double color_contrast,
						double whitespace,
						double interactivity_score,
						double accessibility_score,
						double text_contrast,
						double non_text_contrast) {
		setContentScore(content_score);
		setReadability(readability);
		setSpellingGrammar(spelling_grammar);
		setImageQuality(image_quality);
		setAltText(alt_text);
		
		setInformationArchitectureScore(information_architecture_score);
		setLinks(links);
		setMetadata(metadata);
		setSeo(seo);
		setSecurity(security);
		
		setAestheticsScore(aesthetic_score);
		setColorContrastScore(color_contrast);
		setWhitespaceScore(whitespace);
		
		setInteractivityScore(interactivity_score);
		setAccessibilityScore(accessibility_score);
		
		setTextContrastScore(text_contrast);
		setNonTextContrastScore(non_text_contrast);
	}
}
