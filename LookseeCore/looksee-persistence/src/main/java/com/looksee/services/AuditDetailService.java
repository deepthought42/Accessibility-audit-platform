package com.looksee.services;

import com.looksee.audits.performance.AuditDetail;
import com.looksee.models.repository.AuditDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for interacting with {@link AuditDetail} objects
 */
@Service
public class AuditDetailService {
	
	@Autowired
	private AuditDetailRepository audit_detail_repo;
	
	/**
	 * Saves an {@link AuditDetail} object to the database
	 * 
	 * @param audit_detail {@link AuditDetail} to save
	 * @return {@link AuditDetail}
	 * 
	 * precondition: audit_detail != null
	 */
	public AuditDetail save(AuditDetail audit_detail){
		assert audit_detail != null;

		return audit_detail_repo.save( audit_detail );
	}
}
