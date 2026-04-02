package com.looksee.pageBuilder.models;


public interface IExecutablePageStateAudit {
	/**
	 * Executes audit on {@link PageState page}
	 * 
	 * @param page_state
	 * @param audit_record TODO
	 * @param design_system TODO
	 * @return
	 */
	public Audit execute(PageState page_state, AuditRecord audit_record, DesignSystem design_system);
}
