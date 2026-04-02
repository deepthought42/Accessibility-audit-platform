package com.looksee.frontEndBroadcaster.services;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.frontEndBroadcaster.models.Domain;
import com.looksee.frontEndBroadcaster.models.repository.DomainRepository;

@Service
public class DomainService {
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private DomainRepository domain_repo;
	
	public Domain save(Domain domain) {
		return domain_repo.save(domain);
	}

	public Optional<Domain> findById(long domain_id) {
		return domain_repo.findById(domain_id);
	}

	/**
	 * Find Domain that is associated with Audit record
	 * @param audit_record_id
	 * @return
	 */
	public Domain findByAuditRecord(long audit_record_id) {
		return domain_repo.findByAuditRecord(audit_record_id);
	}

}
