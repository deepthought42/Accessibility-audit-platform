package com.looksee.models.competitiveanalysis;

import com.looksee.models.message.Message;
import lombok.Getter;
import lombok.Setter;

/**
 * Core Message object that defines global fields that are to be used by all Message objects
 */
@Getter
@Setter
public abstract class CompetitiveAnalysisMessage extends Message{
	private long competitorId;
	private long auditRecordId;
	
	/**
	 * Constructs a {@link CompetitiveAnalysisMessage}
	 */
	public CompetitiveAnalysisMessage(){
		super(-1);
	}
	
	/**
	 * Constructor for {@link CompetitiveAnalysisMessage}
	 * 
	 * @param competitor_id id of the competitor
	 * @param account_id id of the account
	 * @param audit_record_id id of the audit record
	 */
	public CompetitiveAnalysisMessage(long competitor_id, long account_id, long audit_record_id){
		super(account_id);
		setCompetitorId(competitor_id);
		setAuditRecordId(audit_record_id);
	}
}
