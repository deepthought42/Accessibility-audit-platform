package com.looksee.audit.informationArchitecture.audits;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.Score;
import com.looksee.models.audit.interfaces.IExecutablePageStateAudit;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.models.enums.ObservationType;
import com.looksee.models.enums.Priority;
import com.looksee.services.AuditService;
import com.looksee.services.UXIssueMessageService;
import com.looksee.utils.BrowserUtils;
import com.looksee.utils.ElementStateUtils;

/**
 * Audits page titles, favicons, and heading structure for SEO and information
 * architecture quality.
 *
 * <p><b>Class invariant:</b> All {@code @Autowired} dependencies ({@code audit_service},
 * {@code issue_message_service}) are non-null after Spring construction.</p>
 */
@Component
public class TitleAndHeaderAudit implements IExecutablePageStateAudit {
	private static Logger log = LoggerFactory.getLogger(TitleAndHeaderAudit.class);
	
	@Autowired
	private AuditService audit_service;
	
	@Autowired
	private UXIssueMessageService issue_message_service;
	
	public TitleAndHeaderAudit() {}
	
	/**
	 * {@inheritDoc}
	 *
	 * Evaluates page titles, favicons, and heading structure for quality.
	 *
	 * @pre {@code page_state != null}
	 * @post returned {@code Audit} is non-null and persisted
	 */
	@Override
	public Audit execute(PageState page_state,
						 AuditRecord audit_record,
						 DesignSystem design_system
	 ) {
		Objects.requireNonNull(page_state, "page_state must not be null");

		Set<UXIssueMessage> issue_messages = new HashSet<>();

		Score title_score = scorePageTitles(page_state);
		Score favicon_score = scoreFavicon(page_state);
		
		//TODO : heading scores need to have issue messages created and added to issues list
		Score heading_score = scoreHeadings(page_state);
		
		issue_messages.addAll(title_score.getIssueMessages());
		issue_messages.addAll(favicon_score.getIssueMessages());
		issue_messages.addAll(heading_score.getIssueMessages());
		
		int points_earned = 0;
		int max_points = 0;
		for(UXIssueMessage issue_msg : issue_messages) {
			points_earned += issue_msg.getPoints();
			max_points += issue_msg.getMaxPoints();
		}
		
		String why_it_matters = "The favicon is a small detail with a big impact on engagement. When users leave your site to look at another tab that they have open, the favicon allos them to easily identify the tab that belongs to your service.";
		String description = "";
		//page_state = page_state_service.findById(page_state.getId()).get();
		Audit audit = new Audit(AuditCategory.INFORMATION_ARCHITECTURE,
								 AuditSubcategory.SEO,
								 AuditName.TITLES,
								 points_earned,
								 issue_messages,
								 AuditLevel.PAGE,
								 max_points,
								 page_state.getUrl(),
								 why_it_matters,
								 description,
								 true);

		Objects.requireNonNull(audit, "Postcondition failed: audit must not be null");
		return audit_service.save(audit);
	}

	/**
	 * Generates a score for headers found on the page
	 * 
	 * @param page_state
	 * @return
	 */
	private Score scoreHeadings(PageState page_state) {
		Objects.requireNonNull(page_state, "page_state must not be null");

		int points_achieved = 0;
		int max_points = 0;
		Set<UXIssueMessage> issue_messages = new HashSet<>();
		
		//generate score for ordered and unordered lists and their headers
		// TODO :: INCOMPLETE SCORING OF ORDERED LIST HEADERS 
		Score list_score = scoreOrderedListHeaders(page_state);
		points_achieved += list_score.getPointsAchieved();
		max_points += list_score.getMaxPossiblePoints();
		issue_messages.addAll(list_score.getIssueMessages());
		
		//score text elements and their headers
		// TODO :: INCOMPLETE SCORING OF TEXT HEADER ELEMENTS
		Score text_block_header_score = scoreTextElementHeaders(page_state);
		points_achieved += text_block_header_score.getPointsAchieved();
		max_points += text_block_header_score.getMaxPossiblePoints();
		issue_messages.addAll(text_block_header_score.getIssueMessages());	
		
		return new Score(points_achieved, max_points, issue_messages);
	}

	/**
	 * INCOMPLETE: PLEASE FINISH ME
	 * 
	 * @param page_state
	 * @return
	 */
	private Score scoreOrderedListHeaders(PageState page_state) {
		Objects.requireNonNull(page_state, "page_state must not be null");
		int score = 0;
		int max_points = 0;
		
		Document html_doc = Jsoup.parse(page_state.getSrc());
		//review element tree top down to identify elements that own text.
		Elements body_elem = html_doc.getElementsByTag("body");
		List<Element> jsoup_elements = body_elem.get(0).children();
		for(Element element : jsoup_elements) {
			//ignore header tags (h1,h2,h3,h4,h5,h6)
			if(ElementStateUtils.isHeader(element.tagName()) || !element.ownText().isEmpty()) {
				continue;
			}
			
			//extract ordered lists
			//does element own text?
			if(ElementStateUtils.isList(element.tagName())) {
				
				//check if element has header element sibling preceding it
			}			
		}
		
		return new Score(score, max_points, new HashSet<>());
	}

	/**
	 * Generates score based on if text elements have an associated header
	 * 
	 * @param page_state
	 * @return
	 */
	private Score scoreTextElementHeaders(PageState page_state) {
		Objects.requireNonNull(page_state, "page_state must not be null");
		
		int score = 0;
		int max_points = 0;
		
		Document html_doc = Jsoup.parse(page_state.getSrc());
		
		//review element tree top down to identify elements that own text.
		Elements body_elem = html_doc.getElementsByTag("body");
		List<Element> jsoup_elements = body_elem.get(0).children();
		while(!jsoup_elements.isEmpty()) {
			Element element = jsoup_elements.remove(0);
			//ignore header tags (h1,h2,h3,h4,h5,h6)
			if(ElementStateUtils.isHeader(element.tagName()) || ElementStateUtils.isList(element.tagName())) {
				continue;
			}
			
			//does element own text?
			if(!element.ownText().isEmpty()) {
				jsoup_elements.addAll(element.children());
			}
			else if(!element.text().isEmpty() ){
				//check if element has header element sibling preceding it
				int element_idx = element.elementSiblingIndex();
				Elements sibling_elements = element.siblingElements();
				for(Element sibling : sibling_elements) {
					if(ElementStateUtils.isHeader(sibling.tagName())) {
						//check if sibling has a lower index
						int sibling_idx = sibling.siblingIndex();
						if(sibling_idx < element_idx) {
							score += 3;
						}
						else {
							score += 1;
						}
						max_points += 3;
						log.warn("header found for text as previous sibling :: " + score + " / " + max_points);
						break;
					}
				}
			}
		}
		
		
		return new Score(score, max_points, new HashSet<>());
	}

	/**
	 * Generates score based on if favicon is present
	 * 
	 * @param page_state
	 * @return
	 * 
	 * @pre page_state != null
	 */
	private Score scoreFavicon(PageState page_state) {
		Objects.requireNonNull(page_state, "page_state must not be null");
		String ada_compliance = "There are no accessibility guidelines for favicon, but favicon plays a significant role in helping users identify the tab that your website was loaded into.";

		int points = 0;
		int max_points = 1;
		Set<UXIssueMessage> issue_messages = new HashSet<>();
		Set<String> labels = new HashSet<>();
		labels.add("information_architecture");
		labels.add("branding");
		labels.add("seo");
		
		//score title of page state
		if(hasFavicon(page_state.getSrc())) {
			points += 1;
			String title = "Favicon is present and accounted for";
			String description = "Well done! This page has a favicon defined which helps improve recognition of your brand by showing the icon in the browser tab. When users open another tab they'll be able to easily identify which tab is your website.";
			String recommendation = "";
			
			UXIssueMessage favicon_issue = new UXIssueMessage(Priority.NONE,
															description,
															ObservationType.PAGE_STATE,
															AuditCategory.INFORMATION_ARCHITECTURE,
															ada_compliance,
															labels,
															"",
															title,
															1,
															1,
															recommendation);
			
			favicon_issue = issue_message_service.save(favicon_issue);
			//issue_message_service.addPage(favicon_issue.getId(), page_state.getId());
			issue_messages.add(favicon_issue);
		}
		else {
			
			Set<String> categories = new HashSet<>();
			categories.add(AuditCategory.AESTHETICS.toString());
			String title = "favicon is missing";
			String description = "Your page doesn't have a favicon defined. This results in browser tabs not displaying your logo which can reduce recognition of your website when users leave your site temporarily.";
			String recommendation = "Create an icon that is 16x16 for your brand logo and include it as your favicon by inclding the following code in your head tag <link rel=\"shortcut icon\" href=\"your_favicon.ico\" type=\"image/x-icon\"> . Don't forget to put the location of your favicon in place of the href value";
			
			UXIssueMessage favicon_issue = new UXIssueMessage(Priority.HIGH,
																description, 
																ObservationType.PAGE_STATE,
																AuditCategory.INFORMATION_ARCHITECTURE,
																ada_compliance,
																labels,
																"",
																title,
																0,
																1,
																recommendation);
			
			favicon_issue = issue_message_service.save(favicon_issue);
			//issue_message_service.addPage(favicon_issue.getId(), page_state.getId());
			issue_messages.add(favicon_issue);
			points += 0;			
		}
		
		return new Score(points, max_points, issue_messages);
	}

	/**
	 * Checks if a {@link PageState} has a favicon defined.
	 *
	 * @pre {@code page_src != null}
	 * @post returns {@code true} if a link element with rel containing "icon" is found
	 * @param page_src the page HTML source
	 * @return true if a favicon link is present
	 */
	public static boolean hasFavicon(String page_src) {
		Objects.requireNonNull(page_src, "page_src must not be null");
		
		Document doc = Jsoup.parse(page_src);
		Elements link_elements = doc.getElementsByTag("link");
		for(Element element: link_elements) {
			if((element.attr("rel").contains("icon"))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Generate a score for page titles across all pages in this domain
	 * @param domain
	 * @return
	 */
	private Score scorePageTitles(PageState page_state) {
		Objects.requireNonNull(page_state, "page_state must not be null");
		
		Set<UXIssueMessage> issue_messages = new HashSet<>();
		int points = 0;
		int max_points = 1;
		String title = BrowserUtils.getTitle(page_state);
		
		Set<String> labels = new HashSet<>();
		labels.add("seo");
		labels.add("information_architecture");

		//score title of page state
		if( title != null && !title.isEmpty()) {
			points += 1;
			String issue_title = "Page has a title";
			String description = "Well done! This page has a title defined";
			String ada_compliance = "";
			String recommendation = "";

			
			Set<String> categories = new HashSet<>();
			categories.add(AuditCategory.AESTHETICS.toString());

			UXIssueMessage title_issue = new UXIssueMessage(Priority.NONE,
															description, 
															ObservationType.PAGE_STATE,
															AuditCategory.INFORMATION_ARCHITECTURE,
															ada_compliance,
															labels,
															"",
															issue_title,
															1,
															1,
															recommendation);
			
			title_issue = issue_message_service.save(title_issue);
			//issue_message_service.addPage(title_issue.getId(), page_state.getId());
			issue_messages.add(title_issue);
		}
		else {
			String issue_title = "Page is missing a title";
			String description = "This page doesn't have a title defined";
			String why_it_matters = "Making sure each of your pages has a title is incredibly important for SEO. The title isn't just used to display as the page name in the browser. Search engines also use this information as part of their evaluation.";
			String ada_compliance = "";
			String recommendation = "Add a title to the header tag in the html. eg. <title>Page title here</title>";

			
			Set<String> categories = new HashSet<>();
			categories.add(AuditCategory.AESTHETICS.toString());

			UXIssueMessage title_issue = new UXIssueMessage(Priority.HIGH,
															description, 
															ObservationType.PAGE_STATE,
															AuditCategory.INFORMATION_ARCHITECTURE,
															ada_compliance,
															labels,
															"",
															issue_title,
															0,
															1,
															recommendation);
			
			title_issue = issue_message_service.save(title_issue);
			//issue_message_service.addPage(title_issue.getId(), page_state.getId());
			issue_messages.add(title_issue);
		}
		
		return new Score(points, max_points, issue_messages);
	}
}