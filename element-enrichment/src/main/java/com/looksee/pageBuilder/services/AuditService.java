package com.looksee.pageBuilder.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.IterableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.pageBuilder.models.Audit;
import com.looksee.pageBuilder.models.ElementState;
import com.looksee.pageBuilder.models.UXIssueMessage;
import com.looksee.pageBuilder.models.enums.AuditName;
import com.looksee.pageBuilder.models.enums.AuditSubcategory;
import com.looksee.pageBuilder.models.repository.AuditRepository;

import io.github.resilience4j.retry.annotation.Retry;

/**
 * Contains business logic for interacting with and managing audits
 *
 */
@Service
@Retry(name = "neoforj")
public class AuditService {
	private static Logger log = LoggerFactory.getLogger(AuditService.class);

	@Autowired
	private AuditRepository audit_repo;
	
	@Autowired
	private UXIssueMessageService ux_issue_service;
	
	@Autowired
	private PageStateService page_state_service;

	public Audit save(Audit audit) {
		assert audit != null;
		
		return audit_repo.save(audit);
	}

	public Optional<Audit> findById(long id) {
		return audit_repo.findById(id);
	}
	
	public Audit findByKey(String key) {
		return audit_repo.findByKey(key);
	}

	public List<Audit> saveAll(List<Audit> audits) {
		assert audits != null;
		
		List<Audit> audits_saved = new ArrayList<Audit>();
		
		for(Audit audit : audits) {
			if(audit == null) {
				continue;
			}
			
			Audit audit_record = audit_repo.findByKey(audit.getKey());
			if(audit_record != null) {
				log.warn("audit already exists!!!");
				audits_saved.add(audit_record);
				continue;
			}

			Audit saved_audit = audit_repo.save(audit);
			audits_saved.add(saved_audit);
		}
		
		return audits_saved;
	}

	public List<Audit> findAll() {
		// TODO Auto-generated method stub
		return IterableUtils.toList(audit_repo.findAll());
	}

	public Set<UXIssueMessage> getIssues(long audit_id) {
		Set<UXIssueMessage> raw_issue_set = audit_repo.findIssueMessages(audit_id);
		
		return raw_issue_set.parallelStream()
							.filter(issue -> issue.getPoints() != issue.getMaxPoints())
							.distinct()
							.collect(Collectors.toSet());
	}

	public void addAllIssues(long id, List<Long> issue_ids) {
		audit_repo.addAllIssues(id, issue_ids);
	}

	public List<ElementState> getIssuesByNameAndScore(AuditName audit_name, int score) {
		return audit_repo.getIssuesByNameAndScore(audit_name.toString(), score);
	}
	
	public List<ElementState> findGoodExample(AuditName audit_name, int score) {
		return getIssuesByNameAndScore(audit_name, score);
	}
	
	public int countAuditBySubcategory(Set<Audit> audits, AuditSubcategory category) {
		assert audits != null;
		assert category != null;
	
		int issue_count = audits.parallelStream()
				  .filter((s) -> (s.getTotalPossiblePoints() > 0 && category.equals(s.getSubcategory())))
				  .mapToInt(s -> audit_repo.getMessageCount(s.getId()))
				  .sum();
		return issue_count;
	}

	public int countIssuesByAuditName(Set<Audit> audits, AuditName name) {
		assert audits != null;
		assert name != null;
	
		int issue_count = audits.parallelStream()
				  .filter((s) -> (s.getTotalPossiblePoints() > 0 && name.equals(s.getName())))
				  .mapToInt(s -> audit_repo.getMessageCount(s.getId()))
				  .sum();
		
		return issue_count;	
	}

	public void addAllIssues(long audit_id, Set<UXIssueMessage> issue_messages) {
		List<Long> issue_ids = issue_messages.stream().map(x -> x.getId()).collect(Collectors.toList());
		addAllIssues(audit_id, issue_ids);
	}
}
