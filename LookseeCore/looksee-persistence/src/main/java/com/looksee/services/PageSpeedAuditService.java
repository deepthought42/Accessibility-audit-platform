package com.looksee.services;

import com.looksee.audits.performance.PageSpeedAudit;
import com.looksee.models.repository.PageSpeedAuditRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for page speed audits
 */
@Service
public class PageSpeedAuditService {

	/**
	 * Repository for page speed audits
	 */
	@Autowired
	private PageSpeedAuditRepository audit_repo;
	
	/**
	 * Objects are expected to be immutable as of 3/14/19. When this method is
	 * ran, if the object already exists then it will be loaded from the
	 * database by key, otherwise it will be saved
	 *
	 * @param audit {@link PageSpeedAudit}
	 * @return {@link PageSpeedAudit}
	 */
	public PageSpeedAudit save(PageSpeedAudit audit){
		assert audit != null;
		
		/*for(AuditDetail detail : audit.getDetails()) {
		List<AuditDetail> audit_details = new ArrayList<AuditDetail>();
		
		//save audit details
			audit_details.add(audit_detail_repo.save(detail));
		}
		audit.setDetails(audit_details);
		 */
		return audit_repo.save(audit);
	}

	/**
	 * Retrieve data from database
	 *
	 * @param key key of the {@link PageSpeedAudit} to retrieve
	 * @return {@link PageSpeedAudit}
	 *
	 * precondition: key != null
	 * precondition: !key.isEmpty()
	 */
	public PageSpeedAudit findByKey(String key) {
		assert key != null;
		assert !key.isEmpty();

		return audit_repo.findByKey(key);
	}
}
