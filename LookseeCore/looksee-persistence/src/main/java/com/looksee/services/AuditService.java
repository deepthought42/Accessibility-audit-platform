package com.looksee.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.IterableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.models.ElementState;
import com.looksee.models.ImageElementState;
import com.looksee.models.PageState;
import com.looksee.models.SimpleElement;
import com.looksee.models.SimplePage;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.PageStateAudits;
import com.looksee.models.audit.messages.ElementStateIssueMessage;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.models.enums.JourneyStatus;
import com.looksee.models.enums.ObservationType;
import com.looksee.models.repository.AuditRepository;
import com.looksee.models.repository.JourneyRepository;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.NoArgsConstructor;

/**
 * Contains business logic for interacting with and managing audits
 */
@NoArgsConstructor
@Service
@Retry(name = "neoforj")
public class AuditService {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(AuditService.class);

	@Autowired
	private AuditRepository audit_repo;
	
	@Autowired
	private UXIssueMessageService ux_issue_service;
	
	@Autowired
	private PageStateService page_state_service;

	@Autowired
	private JourneyRepository journey_repo;

	/**
	 * Saves an audit
	 *
	 * @param audit the audit to save
	 * @return the saved audit
	 *
	 * precondition: audit != null
	 */
	public Audit save(Audit audit) {
		assert audit != null;
		
		return audit_repo.save(audit);
	}

	/**
	 * Finds an audit by id
	 *
	 * @param id the id of the audit
	 * @return the audit
	 *
	 * precondition: id > 0
	 */
	public Optional<Audit> findById(long id) {
		assert id > 0;
		
		return audit_repo.findById(id);
	}
	
	/**
	 * Finds an audit by key
	 *
	 * @param key the key of the audit
	 * @return the audit
	 *
	 * precondition: key != null
	 */
	public Audit findByKey(String key) {
		assert key != null;
		
		return audit_repo.findByKey(key);
	}

	/**
	 * Saves a list of audits
	 *
	 * @param audits the list of audits to save
	 * @return the list of saved audits
	 *
	 * precondition: audits != null
	 */
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

	/**
	 * Retrieves all audits
	 *
	 * @return the list of audits
	 */
	public List<Audit> findAll() {
		return IterableUtils.toList(audit_repo.findAll());
	}

	/**
	 * Retrieves all issues for an audit
	 *
	 * @param audit_id the id of the audit
	 * @return the set of issues
	 *
	 * precondition: audit_id > 0
	 */
	public Set<UXIssueMessage> getIssues(long audit_id) {
		assert audit_id > 0;
		
		Set<UXIssueMessage> raw_issue_set = audit_repo.findIssueMessages(audit_id);
		
		return raw_issue_set.parallelStream()
							.filter(issue -> issue.getPoints() != issue.getMaxPoints())
							.distinct()
							.collect(Collectors.toSet());
	}
	
	/**
	 * using a list of audits, sorts the list by page and packages results into list 
	 * 	of {@linkplain PageStateAudits}
	 * 
	 * @param audits the set of {@link Audit} to group by page
	 * @return the list of {@link PageStateAudits}
	 *
	 * precondition: audits != null
	 */
	public List<PageStateAudits> groupAuditsByPage(Set<Audit> audits) {
		assert audits != null;

		Map<String, Set<Audit>> audit_url_map = new HashMap<>();
		
		for(Audit audit : audits) {
			//if url of pagestate already exists 
			if(audit_url_map.containsKey(audit.getUrl())) {
				audit_url_map.get(audit.getUrl()).add(audit);
			}
			else {
				Set<Audit> page_audits = new HashSet<>();
				page_audits.add(audit);
				
				audit_url_map.put(audit.getUrl(), page_audits);
			}
		}
		
		List<PageStateAudits> page_audits = new ArrayList<>();
		for(String url : audit_url_map.keySet()) {
			//load page state by url
			PageState page_state = page_state_service.findByUrl(url);
			SimplePage simple_page = new SimplePage(
											page_state.getUrl(),
											page_state.getViewportScreenshotUrl(),
											page_state.getFullPageScreenshotUrl(),
											page_state.getFullPageWidth(),
											page_state.getFullPageHeight(),
											page_state.getSrc(),
											page_state.getKey(),
											page_state.getId());
			PageStateAudits page_state_audits = new PageStateAudits(simple_page, audit_url_map.get(url));
			page_audits.add( page_state_audits ) ;
		}
		
		return page_audits;
	}
	
	
	/**
	 * Generates a {@linkplain Map} with element keys for it's keys and a set
	 * of issue keys associated with each element as the values
	 *
	 * @param audits the {@link Set} of {@link Audit}s
	 * @return the {@link Map}
	 *
	 * precondition: audits != null
	 */
	public Map<String, Set<String>> generateElementIssuesMap(Set<Audit> audits)  {
		assert audits != null;

		Map<String, Set<String>> element_issues = new HashMap<>();
				
		for(Audit audit : audits) {
			Set<UXIssueMessage> issues = getIssues(audit.getId());

			for(UXIssueMessage issue_msg : issues ) {
				
				ElementState element = ux_issue_service.getElement(issue_msg.getId());
				if(element == null) {
					continue;
				}
				
				//associate issue with element
				if(!element_issues.containsKey(element.getKey())) {	
					Set<String> issue_keys = new HashSet<>();
					issue_keys.add(issue_msg.getKey());
					
					element_issues.put(element.getKey(), issue_keys);
				}
				else {
					element_issues.get(element.getKey()).add(issue_msg.getKey());
				}

			}
		}

		return element_issues;
	}
	
	/**
	 * Generates a {@linkplain Map} with issue keys for it's keys and a set of element keys associated 
	 * 	with each issue as the values
	 * 
	 * @param audits the {@link Set} of {@link Audit}s
	 * @return the {@link Map}
	 *
	 * precondition: audits != null
	 */
	public Map<String, String> generateIssueElementMap(Set<Audit> audits)  {
		assert audits != null;

		Map<String, String> issue_element_map = new HashMap<>();
				
		for(Audit audit : audits) {	
			Set<UXIssueMessage> issues = getIssues(audit.getId());

			for(UXIssueMessage issue_msg : issues ) {
				if(issue_msg.getType().equals(ObservationType.COLOR_CONTRAST) ||
						issue_msg.getType().equals(ObservationType.ELEMENT) ) {
					ElementState element = ux_issue_service.getElement(issue_msg.getId());
					if(element == null) {
						log.warn("element issue map:: element is null for issue msg ... "+issue_msg.getId());
						continue;
					}
					
					//associate issue with element
					issue_element_map.put(issue_msg.getKey(), element.getKey());
				}
				else {
					// DO NOTHING FOR NOW
				}
			
			}
		}

		return issue_element_map;
	}

	/**
	 * Add an issue message to an audit
	 *
	 * @param key the key of the audit
	 * @param issue_key the key of the issue message
	 * @return the issue message
	 */
	public UXIssueMessage addIssue(
			String key, 
			String issue_key) {
		assert key != null;
		assert !key.isEmpty();
		assert issue_key != null;
		assert !issue_key.isEmpty();
		
		return audit_repo.addIssueMessage(key, issue_key);
	}

	/**
	 * Retrieve UX issues for a set of audits
	 *
	 * @param audits the set of audits
	 * @return the collection of UX issues
	 *
	 * precondition: audits != null
	 */
	public Collection<UXIssueMessage> retrieveUXIssues(Set<Audit> audits) {
		assert audits != null;

		Map<String, UXIssueMessage> issues = new HashMap<>();
		
		for(Audit audit : audits) {	
			Set<UXIssueMessage> issue_set = getIssues(audit.getId());
			
			for(UXIssueMessage ux_issue: issue_set) {
				if(ObservationType.ELEMENT.equals(ux_issue.getType())) {
					ElementStateIssueMessage element_issue = (ElementStateIssueMessage)ux_issue;
					/*
					ElementState good_example = ux_issue_service.getGoodExample(ux_issue.getId());
					element_issue.setGoodExample(good_example);
					*/
					issues.put(ux_issue.getKey(), element_issue);
				}
				else {
					issues.put(ux_issue.getKey(), ux_issue);
				}
			}
		}
		return issues.values();
	}
	

	/**
	 * Returns a {@linkplain Set} of {@linkplain ElementState} objects that are associated 
	 * 	with the {@linkplain UXIssueMessage} provided
	 *
	 * @param issue_set the set of UX issue messages
	 * @return the collection of simple elements
	 *
	 * precondition: issue_set != null
	 */
	public Collection<SimpleElement> retrieveElementSet(Collection<? extends UXIssueMessage> issue_set) {
		assert issue_set != null;
		Map<String, SimpleElement> element_map = new HashMap<>();
		
		for(UXIssueMessage ux_issue: issue_set) {
			if(ux_issue.getType().equals(ObservationType.COLOR_CONTRAST) || 
					ux_issue.getType().equals(ObservationType.ELEMENT) ) {

				ElementState element = ux_issue_service.getElement(ux_issue.getId());
				if(element == null) {
					return element_map.values();
				}
				if(element instanceof ImageElementState) {
					ImageElementState img_element = (ImageElementState)element;
					
					SimpleElement simple_element = 	new SimpleElement(img_element.getKey(),
																		img_element.getScreenshotUrl(), 
																		img_element.getXLocation(), 
																		img_element.getYLocation(), 
																		img_element.getWidth(), 
																		img_element.getHeight(),
																		img_element.getCssSelector(),
																		img_element.getAllText(),
																		img_element.isImageFlagged(),
																		img_element.isAdultContent());

					element_map.put(img_element.getKey(), simple_element);
				}
				else {
					SimpleElement simple_element = 	new SimpleElement(element.getKey(),
																		element.getScreenshotUrl(),
																		element.getXLocation(),
																		element.getYLocation(),
																		element.getWidth(),
																		element.getHeight(),
																		element.getCssSelector(),
																		element.getAllText(),
																		element.isImageFlagged(),
																		false);
					
					element_map.put(element.getKey(), simple_element);
				}
			}
			else {
				//DO NOTHING FOR NOW
			}
				
		}
		return element_map.values();
	}

	/**
	 * Add all issues to an audit
	 *
	 * @param id the id of the audit
	 * @param issue_ids the ids of the issues
	 *
	 * precondition: id > 0
	 * precondition: issue_ids != null
	 */
	public void addAllIssues(long id, List<Long> issue_ids) {
		assert id > 0;
		assert issue_ids != null;

		audit_repo.addAllIssues(id, issue_ids);
	}

	/**
	 * Get issues by name and score
	 *
	 * @param audit_name the name of the audit
	 * @param score the score of the issues
	 * @return the collection of element states
	 * 
	 * precondition: audit_name != null
	 */
	public List<ElementState> getIssuesByNameAndScore(AuditName audit_name, int score) {
		assert audit_name != null;

		return audit_repo.getIssuesByNameAndScore(audit_name.toString(), score);
	}

	/**
	 * Find a good example for an audit
	 *
	 * @param audit_name the name of the audit
	 * @param score the score of the issues
	 * @return the collection of element states
	 *
	 * precondition: audit_name != null
	 */
	public List<ElementState> findGoodExample(AuditName audit_name, int score) {
		assert audit_name != null;
		return getIssuesByNameAndScore(audit_name, score);
	}

	/**
	 * Count audits by subcategory
	 *
	 * @param audits the set of audits
	 * @param category the subcategory of the audits
	 * @return the count of audits
	 * 
	 * precondition: audits != null
	 * precondition: category != null
	 */
	public int countAuditBySubcategory(Set<Audit> audits, AuditSubcategory category) {
		assert audits != null;
		assert category != null;
	
		int issue_count = audits.parallelStream()
					.filter((s) -> (s.getTotalPossiblePoints() > 0 && category.equals(s.getSubcategory())))
					.mapToInt(s -> audit_repo.getMessageCount(s.getId()))
					.sum();
		return issue_count;
	}

	/**
	 * Count issues by audit name
	 *
	 * @param audits the set of audits
	 * @param name the name of the audit
	 * @return the count of issues
	 *
	 * precondition: audits != null
	 * precondition: name != null
	 */
	public int countIssuesByAuditName(Set<Audit> audits, AuditName name) {
		assert audits != null;
		assert name != null;
	
		int issue_count = audits.parallelStream()
				.filter((s) -> (s.getTotalPossiblePoints() > 0 && name.equals(s.getName())))
				.mapToInt(s -> audit_repo.getMessageCount(s.getId()))
				.sum();
		
		return issue_count;
	}

	/**
	 * Adds all issues to an audit
	 *
	 * @param audit_id the id of the audit
	 * @param issue_messages the set of {@link UXIssueMessage} to add
	 *
	 * precondition: audit_id > 0
	 * precondition: issue_messages != null
	 */
	public void addAllIssues(long audit_id, Set<UXIssueMessage> issue_messages) {
		assert audit_id > 0;
		assert issue_messages != null;
		
		List<Long> issue_ids = issue_messages.stream().map(x -> x.getId()).collect(Collectors.toList());
		addAllIssues(audit_id, issue_ids);
	}

	/**
	 * Calculates the data extraction progress for an audit
	 *
	 * @param audit_id the id of the audit
	 * @return the data extraction progress
	 *
	 * precondition: audit_id > 0
	 */
	public double calculateDataExtractionProgress(long audit_id) {
		assert audit_id > 0;
//		int verified_journeys = journey_repo.findAllJourneysForDomainAudit(audit_id, JourneyStatus.VERIFIED.toString());
		int verified_journeys = journey_repo.findAllNonStatusJourneysForDomainAudit(audit_id, JourneyStatus.CANDIDATE.toString());
		log.warn("verified journeys = "+verified_journeys);

		int candidates = journey_repo.findAllJourneysForDomainAudit(audit_id, JourneyStatus.CANDIDATE.toString());
		log.warn("candidates :: " + candidates);
		
		if( (candidates+verified_journeys) == 0) {
			return 0.01;
		}
		else {
			return verified_journeys / (double)(verified_journeys+candidates);
		}
	}
}
