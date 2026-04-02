package com.looksee.audit.informationArchitecture.audits;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
import com.looksee.utils.ContentUtils;

import io.whelk.flesch.kincaid.ReadabilityCalculator;


/**
 * Audits page metadata for SEO quality, checking title length, meta description presence
 * and readability, and meta refresh tag usage.
 *
 * <p><b>Class invariant:</b> All {@code @Autowired} dependencies ({@code audit_service},
 * {@code issue_message_service}) are non-null after Spring construction.</p>
 */
@Component
public class MetadataAudit implements IExecutablePageStateAudit {
	
	@Autowired
	private AuditService audit_service;
	
	@Autowired
	private UXIssueMessageService issue_message_service;
	
	/**
	 * {@inheritDoc}
	 *
	 * Evaluates page metadata including title, description, and refresh tags.
	 *
	 * @pre {@code page_state != null}
	 * @post returned {@code Audit} is non-null and persisted
	 */
	@Override
	public Audit execute(PageState page_state,
						 AuditRecord audit_record,
						 DesignSystem design_system)
	{
		Objects.requireNonNull(page_state, "page_state must not be null");
		Set<UXIssueMessage> issue_messages = new HashSet<>();
		//List<PageVersion> pages = domain_service.getPages(domain.getHost());

		Score title_score = scoreTitle(page_state);
		Score description_score = scoreDescription(page_state);
		Score refresh_score = scoreRefreshes(page_state);
		// Score keyword_score = scoreKeywords(page_state);   TODO: uncomment once keywords are part of dataset
		
		issue_messages.addAll(title_score.getIssueMessages());
		issue_messages.addAll(description_score.getIssueMessages());
		issue_messages.addAll(refresh_score.getIssueMessages());
		//issue_messages.addAll(keyword_score.getIssueMessages()); TODO: uncomment once keywords are part of dataset
			
		int points_earned = 0;
		int max_points = 0;
		for(UXIssueMessage issue_msg : issue_messages) {
			points_earned += issue_msg.getPoints();
			max_points += issue_msg.getMaxPoints();
		}
		
		String why_it_matters = "Metadata tells search engines what your web page has to offer. By using metadata correctly, you can boost your relevancy in search results. Metadata provides search engines with the most important information about your web pages, including titles and descriptions.";
		String description = "";

		Audit audit = new Audit(AuditCategory.INFORMATION_ARCHITECTURE,
								 AuditSubcategory.SEO,
								 AuditName.METADATA,
								 points_earned,
								 issue_messages,
								 AuditLevel.PAGE,
								 max_points,
								 page_state.getUrl(),
								 why_it_matters,
								 description,
								 false);

		Objects.requireNonNull(audit, "Postcondition failed: audit must not be null");
		return audit_service.save(audit);
	}

	/**
	 * Checks if title matches SEO requirements
	 * 
	 * 		1. Between 50-60 characters long
	 * 		2. contains Brand name
	 * 		3. no duplicates across pages (full-site)
	 * 		4. accurately describes content of page
	 * 		5. doesn't keyword stuff
	 * 		6. H1 headline doesn't match title tag content
	 * 	
	 * Resources: 
	 * 
	 * 		1)  https://www.w3.org/TR/UNDERSTANDING-WCAG20/navigation-mechanisms-title.html
	 * 		2)  
	 * 
	 * @param page_state
	 * @return
	 */
	private Score scoreTitle(PageState page_state) {
		Objects.requireNonNull(page_state, "page_state must not be null");

		int points_achieved = 0;
		int max_points = 1;
		Set<UXIssueMessage> issue_messages = new HashSet<>();
		
		//extract title from page_state
		String page_title = page_state.getTitle();
		
		Set<String> labels = new HashSet<>();
		labels.add("information_architecture");
		labels.add("metadata");
		labels.add("seo");
		
		// check 1. Between 50-60 characters long
		if(page_title.length() > 50 || page_title.length() < 60) {
			points_achieved++;
			
			String recommendation = "";
			String title = "Page title is set correctly";
			String description = "Well done! You have a title that is between 50 and 60 characters long, which is considered the optimal length range for search engine optimization";
			String wcag_compliance = "There are no WCAG requirements for SEO techniques";

			ObservationType type = ObservationType.SEO;
			AuditCategory category = AuditCategory.INFORMATION_ARCHITECTURE;

			String why_it_matters = "When your title is too short or too long then users aren't able to easily identify what to expect from a page.";
			
			UXIssueMessage ux_issue = new UXIssueMessage(Priority.NONE,
														description,
														type,
														category,
														wcag_compliance,
														labels,
														why_it_matters,
														title,
														1,
														1,
														recommendation);
			
			issue_messages.add(issue_message_service.save(ux_issue));

		}
		else {
			String recommendation = "";
			String title = "";
			
			if(page_title.length() < 50) {
				recommendation = "Make your title longer so that it has between 50 and 60 characters";
				title = "Title is too short";
			}
			else if(page_title.length() > 60) {
				recommendation = "Make your title shorter so that it has between 50 and 60 characters";
				title = "Title is too long";
			}
			
			String description = "For best results you should aim to keep your description between 50 and 60 characters long. Google typically displays the first 50–60 characters of a title tag, however there isn't an exact character limit, because characters can vary in width and Google's display titles max out (currently) at 600 pixels.";
			
			ObservationType type = ObservationType.SEO;
			AuditCategory category = AuditCategory.INFORMATION_ARCHITECTURE;
			
			String wcag_compliance = "There is no WCAG requirement for SEO techniques";


			String why_it_matters = "When your title is too short or too long then users aren't able to easily identify what to expect from a page.";
			
			UXIssueMessage ux_issue = new UXIssueMessage(Priority.MEDIUM,
														description,
														type,
														category,
														wcag_compliance,
														labels,
														why_it_matters,
														title,
														0,
														1,
														recommendation );
			
			issue_messages.add(issue_message_service.save(ux_issue));

		}
		max_points++;
		
		return new Score(points_achieved, max_points, issue_messages);
	}

	/**
	 * 
	 * @param page_title
	 * @param page_content
	 * 
	 * @pre page_title != null
	 * @pre page title is not empty
	 * @pre page_content != null
	 * @pre page content is not empty
	 * 
	 * 
	 * @return true if title desribes content of page
	 */
	private boolean describesContentOfPage(String page_title, String page_content) {
		Objects.requireNonNull(page_title, "page_title must not be null");
		if (page_title.isEmpty()) { throw new IllegalArgumentException("page_title must not be empty"); }
		Objects.requireNonNull(page_content, "page_content must not be null");
		if (page_content.isEmpty()) { throw new IllegalArgumentException("page_content must not be empty"); }
		
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * Generates a score for headers found on the page
	 * 
	 * @param page_state
	 * @return
	 */
	private Score scoreKeywords(PageState page_state) {
		Objects.requireNonNull(page_state, "page_state must not be null");

		int points_achieved = 0;
		int max_points = 0;
		Set<UXIssueMessage> issue_messages = new HashSet<>();
		
		//extract keywords from metadata tags for keywords
		//add keywords to page_state
		//if meta tag with name="keywords" exists then add 1 to score
		//if keywords is not empty then add 1 to score
		//if keywords does NOT contains duplicates then add 1 to score
		
		return new Score(points_achieved, max_points, issue_messages);
	}

	/**
	 * INCOMPLETE: PLEASE FINISH ME
	 * 
	 * @param page_state
	 * @return
	 */
	private Score scoreDescription(PageState page_state) {
		Objects.requireNonNull(page_state, "page_state must not be null");
		int score = 0;
		int max_points = 0;
		
		int description_count = 0;
		Set<UXIssueMessage> issue_messages = new HashSet<>();

		Document html_doc = Jsoup.parse(page_state.getSrc());
		//review element tree top down to identify elements that own text.
		Elements meta_elements = html_doc.getElementsByTag("meta");
		//List<Element> jsoup_elements = body_elem.get(0).children();
		Set<String> labels = new HashSet<>();
		labels.add("information_architecture");
		labels.add("metadata");
		labels.add("seo");
		
		for(Element element : meta_elements) {
			//if element has attribute name="description" then add 1 to score
			if(element.attr("name").contentEquals("description")) {
				score++;
				description_count++;
				//if element with type description contains text then add 1 to score
				if(!element.attr("content").isEmpty()) {
					score++;

					String meta_description = element.attr("content");
					
					//if element with type description contains text with length < 150 add 1 to score ( Google recommended description length - https://moz.com/learn/seo/meta-description)
					if(meta_description.length() < 150 && meta_description.length() > 120) {
						score++;
						String recommendation = "Well done! Your page contains a description meta tag with between 120 and 150 characters, which is the suggested maximum length for optimal SEO results.";
						Priority priority = Priority.NONE;
						String description = "The meta description \"" + meta_description + "\" is too short";
						ObservationType type = ObservationType.SEO;
						AuditCategory category = AuditCategory.INFORMATION_ARCHITECTURE;
						String wcag_compliance = "There are no WCAG requirements for this";
						String why_it_matters = "Search engines show the meta description to users when your page shows up in search results. Meta descriptions that are longer than 160 characters get cut off by Search engines and won't be shown to the user";
						String title= "Meta description is too long";
						
						UXIssueMessage issue_msg = new UXIssueMessage(priority,
																	description,
																	type,
																	category,
																	wcag_compliance,
																	labels,
																	why_it_matters,
																	title,
																	1,
																	1,
																	recommendation);
						
						issue_messages.add(issue_message_service.save(issue_msg));
					}
					else if (meta_description.length() > 150) {
						String recommendation = "Try to be more concise with your meta description and make sure the description is no longer than 150 characters";
						Priority priority = Priority.MEDIUM;
						String description = "The meta description \"" + meta_description + "\" is too long";
						ObservationType type = ObservationType.SEO;
						AuditCategory category = AuditCategory.INFORMATION_ARCHITECTURE;
						String wcag_compliance = "There are no WCAG requirements for this";
						String why_it_matters = "Search engines show the meta description to users when your page shows up in search results. Meta descriptions that are longer than 160 characters get cut off by Search engines and won't be shown to the user";
						String title= "Meta description is too long";
						
						UXIssueMessage issue_msg = new UXIssueMessage(priority,
																	description,
																	type,
																	category,
																	wcag_compliance,
																	labels,
																	why_it_matters,
																	title,
																	0,
																	1,
																	recommendation);
						
						issue_messages.add(issue_message_service.save(issue_msg));
					}
					else {
						String recommendation = "Add some more context to your description, so that it's easy for a user to understand if they will find what they are looking for on the page";
						Priority priority = Priority.MEDIUM;
						String description = "The meta description \"" + meta_description + "\" is too short";
						ObservationType type = ObservationType.SEO;
						AuditCategory category = AuditCategory.INFORMATION_ARCHITECTURE;
						String wcag_compliance = "There are no WCAG requirements for this";
						String why_it_matters = "Search engines show the meta description to users when your page shows up in search results. Meta descriptions that are too short often lack enough information to know if the content within a search result is going to be helpful.";
						String title= "Meta description is too short";
						
						UXIssueMessage issue_msg = new UXIssueMessage(priority,
																	description,
																	type,
																	category,
																	wcag_compliance,
																	labels,
																	why_it_matters,
																	title,
																	0,
																	1,
																	recommendation);

						issue_messages.add(issue_message_service.save(issue_msg));
					}
				
					//if element with type description contains text that is between 5-8th grade reading level then add 1 to score
					double ease_of_reading_score = ReadabilityCalculator.calculateReadingEase(meta_description);
					if(ease_of_reading_score >= 70.0) {
						score++;
						String recommendation = "";
						String description = "The text \"" + meta_description + 
											"\" is written at the "+ ContentUtils.getReadingGradeLevel(ease_of_reading_score) + 
											", which is considered " + ContentUtils.getReadingDifficultyRating(ease_of_reading_score) + " to read";
						ObservationType type = ObservationType.SEO;
						AuditCategory category = AuditCategory.INFORMATION_ARCHITECTURE;
						String wcag_compliance = "There are no WCAG requirements for this";
						String why_it_matters = "When users are reviewing search results they don't read everything. Instead they scan the results."
												+ "When your meta description is too difficult to read, it makes it difficult to understand if your page can help them at a quick glance."
												+ "This often results on the user choosing another option that with a better meta description";
						String title= "Meta description is " + ContentUtils.getReadingGradeLevel(ease_of_reading_score) + " reading level ";
						
						UXIssueMessage issue_msg = new UXIssueMessage(Priority.NONE, 
																	  description, 
																	  type, 
																	  category, 
																	  wcag_compliance, 
																	  labels, 
																	  why_it_matters, 
																	  title, 
																	  1, 
																	  1, 
																	  recommendation);
						
						issue_messages.add(issue_message_service.save(issue_msg));
					}
					else {
						String recommendation = "Simplify the language in your meta description so that it is within the 5-7 grade reading level";
						Priority priority = Priority.MEDIUM;
						String description = "The text \"" + meta_description + 
											"\" is written at the "+ ContentUtils.getReadingGradeLevel(ease_of_reading_score) + 
											 ", which is considered " + ContentUtils.getReadingDifficultyRating(ease_of_reading_score) + " to read";
						ObservationType type = ObservationType.SEO;
						AuditCategory category = AuditCategory.INFORMATION_ARCHITECTURE;
						String wcag_compliance = "There are no WCAG requirements for this";
						String why_it_matters = "When users are reviewing search results they don't read everything. Instead they scan the results."
												+ "When your meta description is too difficult to read, it makes it difficult to understand if your page can help them at a quick glance."
												+ "This often results on the user choosing another option that with a better meta description";
						String title= "Meta description is " + ContentUtils.getReadingDifficultyRating(ease_of_reading_score) + " to read";
						
						UXIssueMessage issue_msg = new UXIssueMessage(priority,
																	description,
																	type,
																	category,
																	wcag_compliance,
																	labels,
																	why_it_matters,
																	title,
																	0,
																	1,
																	recommendation);
						
						issue_messages.add(issue_message_service.save(issue_msg));
					}
				}
				else {
					String recommendation = "Add a description to the meta html element. For best results your description should be between 120 and 160 characters in length";
					Priority priority = Priority.MEDIUM;
					String description = "The meta description tag on your page is empty";
					ObservationType type = ObservationType.SEO;
					AuditCategory category = AuditCategory.INFORMATION_ARCHITECTURE;
					String wcag_compliance = "There are no WCAG requirements for this";
					String why_it_matters = "Search engines show the meta description to users when your page shows up in search results. Having a description helps users understand if they can find what they are looking for on any given webpage.";
					String title= "Meta description is empty";
					
					UXIssueMessage issue_msg = new UXIssueMessage(priority,
																description,
																type,
																category,
																wcag_compliance,
																labels,
																why_it_matters,
																title,
																0,
																1,
																recommendation);
					
					issue_messages.add(issue_message_service.save(issue_msg));
				}
			}
			max_points += 5;
		}

		if(description_count == 0) {
			max_points = 1;
			String recommendation = "Add a meta html element with a description of the purpose of your page. Example : <meta name='description'>your description here</meta>";
			Priority priority = Priority.MEDIUM;
			String description = "Meta html tags allow you to provide a description that can be used by search engines to help users easily understand what they can accomplish with each page";
			ObservationType type = ObservationType.SEO;
			AuditCategory category = AuditCategory.INFORMATION_ARCHITECTURE;
			String wcag_compliance = "There are no WCAG requirements for this";
			String why_it_matters = "Search engines show the meta description to users when your page shows up in search results. Having a description helps users understand if they can find what they are looking for on any given webpage.";
			String title= "Meta description not found";
			
			UXIssueMessage issue_msg = new UXIssueMessage(priority,
														description,
														type,
														category,
														wcag_compliance,
														labels,
														why_it_matters,
														title,
														0,
														2,
														recommendation);
			
			issue_messages.add(issue_message_service.save(issue_msg));
		}
		if(description_count > 1) {
			score = score / description_count;
			
			String recommendation = "Remove extraneous meta description tags";
			Priority priority = Priority.LOW;
			String description = description_count + " meta description tags were found.";
			ObservationType type = ObservationType.SEO;
			AuditCategory category = AuditCategory.INFORMATION_ARCHITECTURE;
			String wcag_compliance = "There are no WCAG requirements for this";
			String why_it_matters = "Search engines will only show one meta description to users. Having more than 1 meta description doesn't help, and may actually hurt your search ranking";
			String title= "Too many meta descriptions found";
			
			UXIssueMessage issue_msg = new UXIssueMessage(priority,
														description,
														type,
														category,
														wcag_compliance,
														labels,
														why_it_matters,
														title,
														1,
														2,
														recommendation);
			
			issue_messages.add(issue_message_service.save(issue_msg));
		}
		
		return new Score(score, max_points, issue_messages);
	}
	
	private Score scoreRefreshes(PageState page_state) {
		Objects.requireNonNull(page_state, "page_state must not be null");
		
		int score = 0;
		int max_points = 1;
		Set<UXIssueMessage> issue_messages = new HashSet<>();

		Document html_doc = Jsoup.parse(page_state.getSrc());
		//review element tree top down to identify elements that own text.
		Elements meta_elements = html_doc.getElementsByTag("meta");
		//List<Element> jsoup_elements = body_elem.get(0).children();
		int refresh_element_count = 0;
		for(Element element : meta_elements) {
			//if element has attribute name="refresh" then add 1 refresh_element_count
			if(element.hasAttr("name") && element.attr("name").contentEquals("refresh")) {
				meta_elements.add(element);
			}
		}
		
		Set<String> labels = new HashSet<>();
		labels.add("information_architecture");
		labels.add("metadata");
		labels.add("seo");
		
		if(refresh_element_count == 0) {
			score += 1;
			String recommendation = "";
			String description = "Great job on not using a meta tag with name=\"refresh\"!";
			ObservationType type = ObservationType.SEO;
			AuditCategory category = AuditCategory.INFORMATION_ARCHITECTURE;
			String wcag_compliance = "There are no WCAG requirements for this";
			String why_it_matters = "Meta html tags with name=\"refresh\" are discouraged because consistent page refreshes can be disruptive to the experience as well as making a page highly difficult to interact with for people that rely on assistive technologies";
			String title= "Meta refresh tag found";
			
			UXIssueMessage issue_msg = new UXIssueMessage(Priority.NONE,
														description,
														type,
														category,
														wcag_compliance,
														labels,
														why_it_matters,
														title,
														1,
														1,
														recommendation);
			
			issue_messages.add(issue_message_service.save(issue_msg));
		}
		else {
			String recommendation = "Remove the meta tag with the attribute name='refresh'";
			String description = "Meta tag with name=\"refresh\" was found";
			ObservationType type = ObservationType.SEO;
			AuditCategory category = AuditCategory.INFORMATION_ARCHITECTURE;
			String wcag_compliance = "There are no WCAG requirements for this";
			String why_it_matters = "Meta html tags with name=\"refresh\" are discouraged because consistent page refreshes can be disruptive to the experience as well as making a page highly difficult to interact with for people that rely on assistive technologies";
			String title= "Meta refresh tag found";
			
			UXIssueMessage issue_msg = new UXIssueMessage(Priority.HIGH,
														description,
														type,
														category,
														wcag_compliance,
														labels,
														why_it_matters,
														title,
														0,
														1,
														recommendation);
			
			issue_messages.add(issue_message_service.save(issue_msg));
		}
		
		return new Score(score, max_points, issue_messages);
	}
}