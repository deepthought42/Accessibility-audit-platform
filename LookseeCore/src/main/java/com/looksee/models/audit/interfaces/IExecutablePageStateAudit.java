package com.looksee.models.audit.interfaces;

import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.designsystem.DesignSystem;

/**
 * An interface for an executable page state audit
 */
public interface IExecutablePageStateAudit {
	/**
	 * Executes audit on {@link PageState page}
	 * 
	 * @param page_state the page state to audit
	 * @param audit_record the audit record to audit
	 * @param design_system the design system to audit
	 * @return the audit result
	 */
	public Audit execute(PageState page_state, AuditRecord audit_record, DesignSystem design_system);
}
