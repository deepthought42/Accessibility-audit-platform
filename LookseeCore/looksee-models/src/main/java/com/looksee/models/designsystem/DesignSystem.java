package com.looksee.models.designsystem;

import com.looksee.models.LookseeObject;
import com.looksee.models.enums.AudienceProficiency;
import com.looksee.models.enums.WCAGComplianceLevel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Node;


/**
 * Defines a design system for use in defining and evaluating standards based on
 * the settings withing the design system
 *
 * invariant: wcagComplianceLevel is always a valid {@link WCAGComplianceLevel} string
 * invariant: audienceProficiency is always a valid {@link AudienceProficiency} string
 * invariant: allowedImageCharacteristics != null
 * invariant: colorPalette != null
 */
@Node
@Getter
@Setter
public class DesignSystem extends LookseeObject{

	/**
	 * The WCAG compliance level of the design system
	 */
	private String wcagComplianceLevel;

	/**
	 * The audience proficiency of the design system
	 */
	private String audienceProficiency;
	
	/**
	 * The allowed image characteristics of the design system
	 */
	private List<String> allowedImageCharacteristics;

	/**
	 * The color palette of the design system
	 */
	private List<String> colorPalette;

	/**
	 * Constructs a new {@link DesignSystem}
	 */
	public DesignSystem() {
		wcagComplianceLevel = WCAGComplianceLevel.AAA.toString();
		audienceProficiency = AudienceProficiency.GENERAL.toString();
		allowedImageCharacteristics = new ArrayList<String>();
		colorPalette = new ArrayList<>();
	}
	
	/**
	 * Returns the WCAG compliance level of the design system
	 *
	 * @return the WCAG compliance level of the design system
	 */
	public WCAGComplianceLevel getWcagComplianceLevel() {
		return WCAGComplianceLevel.create(wcagComplianceLevel);
	}

	/**
	 * Sets the WCAG compliance level of the design system
	 *
	 * @param wcag_compliance_level the WCAG compliance level of the design system
	 *
	 * precondition: wcag_compliance_level != null
	 */
	public void setWcagComplianceLevel(WCAGComplianceLevel wcag_compliance_level) {
		assert wcag_compliance_level != null;

		this.wcagComplianceLevel = wcag_compliance_level.toString();
	}

	/**
	 * Returns the audience proficiency of the design system
	 *
	 * @return the audience proficiency of the design system
	 */
	public AudienceProficiency getAudienceProficiency() {
		return AudienceProficiency.create(audienceProficiency);
	}

	/**
	 * Sets the audience proficiency of the design system
	 *
	 * @param audience_proficiency the audience proficiency of the design system
	 *
	 * precondition: audience_proficiency != null
	 */
	public void setAudienceProficiency(AudienceProficiency audience_proficiency) {
		assert audience_proficiency != null;

		this.audienceProficiency = audience_proficiency.toString();
	}
	
	/**
	 * Generates a key for the design system
	 *
	 * @return the key
	 */
	@Override
	public String generateKey() {
		return "designsystem"+UUID.randomUUID();
	}

	/**
	 * Adds a color to the design system
	 *
	 * @param color the color to add
	 * @return true if the color was added, false otherwise
	 *
	 * precondition: color != null
	 */
	public boolean addColor(String color){
		assert color != null;

		if(!getColorPalette().contains(color)) {
			return getColorPalette().add(color);
		}
		
		return true;
	}

	/**
	 * Removes a color from the design system
	 *
	 * @param color the color to remove
	 * @return true if the color was removed, false otherwise
	 *
	 * precondition: color != null
	 */
	public boolean removeColor(String color) {
		assert color != null;

		return getColorPalette().remove(color);
	}
}
