package com.looksee.models.message;

import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Message that contains a {@link PageState} that is ready for analysis
 */
@Getter
@Setter
public class AuditSetMessage extends Message {
	private List<Audit> audits;
	private String url;
	

	/**
	 * Constructs an {@link AuditSetMessage} object
	 *
	 * @param account_id id of the account
	 * @param audits list of audits
	 * @param url url of the audit
	 *
	 * precondition: audits != null
	 * precondition: url != null
	 */
	public AuditSetMessage(long account_id, List<Audit> audits, String url){
		assert audits != null : "audits must not be null";
		assert url != null : "url must not be null";

		setAccountId(account_id);
		setAudits(audits);
		setUrl(url);
	}
}
