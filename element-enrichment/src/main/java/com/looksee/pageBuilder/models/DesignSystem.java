package com.looksee.pageBuilder.models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.neo4j.core.schema.Node;

import com.looksee.pageBuilder.models.enums.AudienceProficiency;
import com.looksee.pageBuilder.models.enums.WCAGComplianceLevel;


/**
 * Defines a design system for use in defining and evaluating standards based on 
 * the settings withing the design system
 */
@Node
public class DesignSystem extends LookseeObject{

	private String wcagComplianceLevel;
	private String audienceProficiency;
	
	private List<String> allowedImageCharacteristics;
	private List<String> colorPalette;

	public DesignSystem() {
		wcagComplianceLevel = WCAGComplianceLevel.AAA.toString();
		audienceProficiency = AudienceProficiency.GENERAL.toString();
		allowedImageCharacteristics = new ArrayList<String>();
		colorPalette = new ArrayList<>();
	}
	
	public WCAGComplianceLevel getWcagComplianceLevel() {
		return WCAGComplianceLevel.create(wcagComplianceLevel);
	}

	public void setWcagComplianceLevel(WCAGComplianceLevel wcag_compliance_level) {
		this.wcagComplianceLevel = wcag_compliance_level.toString();
	}

	public AudienceProficiency getAudienceProficiency() {
		return AudienceProficiency.create(audienceProficiency);
	}

	/**
	 * sets the reading and topic proficiency level 
	 * 
	 * @param audience_proficiency {@link AudienceProficiency} string value
	 */
	public void setAudienceProficiency(AudienceProficiency audience_proficiency) {
		this.audienceProficiency = audience_proficiency.toString();
	}
	
	@Override
	public String generateKey() {
		return "designsystem"+UUID.randomUUID();
	}

	public List<String> getAllowedImageCharacteristics() {
		return allowedImageCharacteristics;
	}

	public void setAllowedImageCharacteristics(List<String> allowed_image_characteristics) {
		this.allowedImageCharacteristics = allowed_image_characteristics;
	}

	public List<String> getColorPalette() {
		return colorPalette;
	}

	public void setColorPalette(List<String> color_palette) {
		this.colorPalette = color_palette;
	}
	
	public boolean addColor(String color){
		if(!getColorPalette().contains(color)) {
			return getColorPalette().add(color);
		}
		
		return true;	
	}

	public boolean removeColor(String color) {
		return getColorPalette().remove(color);
	}
}
