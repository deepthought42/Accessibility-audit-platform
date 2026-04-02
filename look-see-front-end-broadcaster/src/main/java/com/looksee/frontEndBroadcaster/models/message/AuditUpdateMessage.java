package com.looksee.frontEndBroadcaster.models.message;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * 
 */
public class AuditUpdateMessage extends Message {

	@Getter
	@Setter
	private long id;

	@Getter
	@Setter
	private double infoArchitectureProgress;

	@Getter
	@Setter
	private double contentAuditProgress;

	@Getter
	@Setter
	private double aestheticAuditProgress;

	@Getter
	@Setter
	private double dataExtractionProgress;
	
	public AuditUpdateMessage( long id,
							   double info_architecture_audit_progress,
							   double content_audit_progress,
							   double aesthetic_progress,
							   double data_extraction)
	{
		setId(id);
		setInfoArchitectureProgress(info_architecture_audit_progress);
		setContentAuditProgress(content_audit_progress);
		setAestheticAuditProgress(aesthetic_progress);
		setDataExtractionProgress(data_extraction);
	}
	
	public AuditUpdateMessage clone(){
		return new AuditUpdateMessage(  getId(),
								  getInfoArchitectureProgress(),
								  getContentAuditProgress(),
								  getAestheticAuditProgress(),
								  getDataExtractionProgress());
	}
}
