package com.looksee.pageBuilder.models;

import java.util.Set;


public class PageStateAudits {
	
	private SimplePage page;
	private Set<Audit> audits;
	
	public PageStateAudits(SimplePage page, Set<Audit> audits) {
		setPage(page);
		setAudits(audits);
	}

	public Set<Audit> getAudits() {
		return audits;
	}

	public void setAudits(Set<Audit> audits) {
		this.audits = audits;
	}

	public SimplePage getPage() {
		return page;
	}

	public void setPage(SimplePage page) {
		this.page = page;
	}

}
