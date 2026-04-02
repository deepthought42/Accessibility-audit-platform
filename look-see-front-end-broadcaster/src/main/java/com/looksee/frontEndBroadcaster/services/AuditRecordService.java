package com.looksee.frontEndBroadcaster.services;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.frontEndBroadcaster.models.AuditRecord;
import com.looksee.frontEndBroadcaster.models.repository.AuditRecordRepository;

import io.github.resilience4j.retry.annotation.Retry;

/**
 * Contains business logic for interacting with and managing audits
 *
 */
@Service
@Retry(name = "neoforj")
public class AuditRecordService {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(AuditRecordService.class);
	
	@Autowired
	private AuditRecordRepository audit_record_repo;
	
	public AuditRecord save(AuditRecord audit, Long account_id, Long domain_id) {
		assert audit != null;

		AuditRecord audit_record = audit_record_repo.save(audit);

		return audit_record;
	}

	public Optional<AuditRecord> findById(long id) {
		return audit_record_repo.findById(id);
	}
}
