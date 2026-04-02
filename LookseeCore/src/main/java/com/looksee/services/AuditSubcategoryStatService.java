package com.looksee.services;

import com.looksee.models.audit.stats.AuditSubcategoryStat;
import com.looksee.models.repository.AuditSubcategoryStatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for {@link AuditSubcategoryStat}
 */
@Service
public class AuditSubcategoryStatService {
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private AuditSubcategoryStatRepository audit_stat_repository;
	
	/**
	 * Saves an {@link AuditSubcategoryStat}
	 *
	 * @param audit_stat {@link AuditSubcategoryStat} to save
	 * @return saved {@link AuditSubcategoryStat}
	 *
	 * precondition: audit_stat != null
	 */
	public AuditSubcategoryStat save(AuditSubcategoryStat audit_stat) {
		assert audit_stat != null;

		return audit_stat_repository.save(audit_stat);
	}
}
