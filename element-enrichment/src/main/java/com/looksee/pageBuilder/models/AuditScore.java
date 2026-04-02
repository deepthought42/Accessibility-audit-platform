package com.looksee.pageBuilder.models;

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
	private double colorContrast;
	private double whitespace;
	
	private double interactivityScore;
	private double accessibilityScore;
	
	private double textContrast;
	private double nonTextContrast;
	
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
		setSEO(seo);
		setSecurity(security);
		
		setAestheticsScore(aesthetic_score);
		setColorContrast(color_contrast);
		setWhitespace(whitespace);
		
		setInteractivityScore(interactivity_score);
		setAccessibilityScore(accessibility_score);
		
		setTextContrastScore(text_contrast);
		setNonTextContrastScore(non_text_contrast);
	}
	
	
	public double getContentScore() {
		return contentScore;
	}
	
	public void setContentScore(double content_score) {
		this.contentScore = content_score;
	}

	public double getInformationArchitectureScore() {
		return informationArchitectureScore;
	}

	public void setInformationArchitectureScore(double information_architecture_score) {
		this.informationArchitectureScore = information_architecture_score;
	}

	public double getAestheticsScore() {
		return aestheticsScore;
	}

	public void setAestheticsScore(double aesthetics_score) {
		this.aestheticsScore = aesthetics_score;
	}

	public double getInteractivityScore() {
		return interactivityScore;
	}

	public void setInteractivityScore(double interactivity_score) {
		this.interactivityScore = interactivity_score;
	}

	public double getAccessibilityScore() {
		return accessibilityScore;
	}

	public void setAccessibilityScore(double accessibility_score) {
		this.accessibilityScore = accessibility_score;
	}


	public double getReadability() {
		return readability;
	}


	public void setReadability(double readability) {
		this.readability = readability;
	}


	public double getSpellingGrammar() {
		return spellingGrammar;
	}


	public void setSpellingGrammar(double spelling_grammar) {
		this.spellingGrammar = spelling_grammar;
	}

	public double getImageQuality() {
		return imageQuality;
	}


	public void setImageQuality(double image_quality) {
		this.imageQuality = image_quality;
	}


	public double getAltText() {
		return altText;
	}


	public void setAltText(double alt_text) {
		this.altText = alt_text;
	}


	public double getLinks() {
		return links;
	}


	public void setLinks(double links) {
		this.links = links;
	}


	public double getMetadata() {
		return metadata;
	}


	public void setMetadata(double metadata) {
		this.metadata = metadata;
	}


	public double getSEO() {
		return seo;
	}


	public void setSEO(double seo) {
		this.seo = seo;
	}


	public double getSecurity() {
		return security;
	}


	public void setSecurity(double security) {
		this.security = security;
	}


	public double getColorContrast() {
		return colorContrast;
	}


	public void setColorContrast(double color_contrast) {
		this.colorContrast = color_contrast;
	}


	public double getWhitespace() {
		return whitespace;
	}


	public void setWhitespace(double whitespace) {
		this.whitespace = whitespace;
	}


	public double getTextContrastScore() {
		return textContrast;
	}


	public void setTextContrastScore(double text_contrast) {
		this.textContrast = text_contrast;
	}


	public double getNonTextContrastScore() {
		return nonTextContrast;
	}


	public void setNonTextContrastScore(double non_text_contrast) {
		this.nonTextContrast = non_text_contrast;
	}
}
