package com.looksee.models.audit;

import com.looksee.models.SimplePage;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 * Stores the audits of a page state
 */
@Getter
@Setter
public class PageStateAudits {

	private SimplePage page;
	private Set<Audit> audits;
	
	/**
	 * Constructs a new {@link PageStateAudits}
	 *
	 * @param page the page of the page state audits
	 * @param audits the audits of the page state audits
	 *
	 * precondition: page != null
	 * precondition: audits != null
	 */
	public PageStateAudits(SimplePage page, Set<Audit> audits) {
		assert page != null;
		assert audits != null;

		setPage(page);
		setAudits(audits);
	}
}
