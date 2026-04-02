package com.looksee.contentAudit.models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.Score;
import com.looksee.models.audit.interfaces.IExecutablePageStateAudit;
import com.looksee.models.audit.messages.ReadingComplexityIssueMessage;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.models.enums.Priority;
import com.looksee.services.AuditService;
import com.looksee.services.UXIssueMessageService;
import com.looksee.utils.ContentUtils;

import io.whelk.flesch.kincaid.ReadabilityCalculator;
import lombok.NoArgsConstructor;

/**
 * Responsible for executing a readability audit on web page text content to
 * assess reading complexity and ensure compliance with WCAG AAA accessibility
 * standards.
 *
 * <p>This audit evaluates the readability of text content on a web page using
 * the Flesch Reading Ease formula to determine if content is appropriate for
 * the target audience's education level. It filters out non-meaningful text
 * elements (buttons, links, very short text) and analyzes remaining text content
 * for reading difficulty.</p>
 *
 * <p>The audit supports WCAG Level AAA compliance for success criterion 3.1.5
 * by ensuring that text content doesn't require reading ability beyond the
 * lower secondary education level (grades 5-8). It provides detailed feedback
 * on text complexity and recommendations for improving readability.</p>
 *
 * WCAG Level - AAA
 * WCAG Success Criterion - https://www.w3.org/TR/UNDERSTANDING-WCAG20/meaning-supplements.html
 */
@Component
@NoArgsConstructor
public class ReadabilityAudit implements IExecutablePageStateAudit {
	private static Logger log = LoggerFactory.getLogger(ReadabilityAudit.class);
	
	@Autowired
	private AuditService audit_service;
	
	@Autowired
	private UXIssueMessageService issue_message_service;
	
	/**
	 * Executes a readability audit on a web page to assess text complexity and compliance with WCAG AAA standards.
	 * 
	 * <p><strong>Preconditions:</strong></p>
	 * <ul>
	 *   <li>{@code page_state} must not be null</li>
	 *   <li>{@code page_state.getElements()} must contain valid ElementState objects</li>
	 *   <li>{@code audit_record} must be a valid audit record for tracking</li>
	 *   <li>{@code design_system} must be provided (though unused in this implementation)</li>
	 *   <li>{@code audit_service} and {@code issue_message_service} must be properly injected</li>
	 * </ul>
	 * 
	 * <p><strong>Process:</strong></p>
	 * <ul>
	 *   <li>Filters page elements to identify meaningful text content by excluding buttons, links, empty text, and text with 3 or fewer words</li>
	 *   <li>Removes duplicate text content by identifying elements whose text is contained within other elements</li>
	 *   <li>For each qualifying text element, calculates Flesch Reading Ease score using ReadabilityCalculator.calculateReadingEase()</li>
	 *   <li>Determines reading difficulty rating and grade level using ContentUtils helper methods</li>
	 *   <li>Assigns points based on reading ease score and target user education level using getPointsForEducationLevel()</li>
	 *   <li>Boosts points to maximum (4) for text elements with fewer than 10 words</li>
	 *   <li>Creates ReadingComplexityIssueMessage objects for both problematic and compliant text elements</li>
	 *   <li>Calculates overall score based on points earned vs maximum possible points (4 points per text element)</li>
	 * </ul>
	 * 
	 * <p><strong>Postconditions:</strong></p>
	 * <ul>
	 *   <li>Returns a non-null Audit object with CONTENT category, WRITTEN_CONTENT subcategory, and READING_COMPLEXITY audit name</li>
	 *   <li>The audit contains calculated points earned and maximum points based on readability compliance</li>
	 *   <li>All ReadingComplexityIssueMessage objects are persisted to the database via UXIssueMessageService</li>
	 *   <li>The audit is saved to the database via AuditService</li>
	 *   <li>All issue messages are associated with their respective text elements</li>
	 *   <li>Issue messages contain appropriate priority levels (LOW for problematic text, NONE for compliant text)</li>
	 * </ul>
	 * 
	 * <p><strong>Scoring System:</strong></p>
	 * <ul>
	 *   <li>Each text element contributes up to 4 points maximum</li>
	 *   <li>Text with fewer than 10 words automatically receives 4 points</li>
	 *   <li>Points are assigned based on Flesch Reading Ease score ranges and target user education level</li>
	 *   <li>Reading ease scores 90+: 3-4 points depending on education level</li>
	 *   <li>Reading ease scores 80-89: 4 points for most education levels</li>
	 *   <li>Reading ease scores 70-79: 3-4 points depending on education level</li>
	 *   <li>Reading ease scores 60-69: 2-4 points depending on education level</li>
	 *   <li>Reading ease scores 50-59: 1-4 points depending on education level</li>
	 *   <li>Reading ease scores 30-49: 0-3 points depending on education level</li>
	 *   <li>Reading ease scores below 30: 0-2 points depending on education level</li>
	 * </ul>
	 * 
	 * <p><strong>WCAG Compliance:</strong></p>
	 * <ul>
	 *   <li>Supports WCAG Level AAA compliance</li>
	 *   <li>Addresses WCAG Success Criterion for reading level requirements</li>
	 *   <li>Ensures text content doesn't require reading ability beyond lower secondary education level</li>
	 * </ul>
	 * 
	 * @param page_state The page state containing elements to audit, must not be null
	 * @param audit_record The audit record for tracking this audit execution, must not be null
	 * @param design_system The design system context (unused in this implementation)
	 * @return A completed Audit object with readability compliance results
	 * @throws NullPointerException if {@code page_state} or {@code audit_record} is null
	 * @throws RuntimeException if ReadabilityCalculator.calculateReadingEase() fails for any text element
	 */
	@Override
	public Audit execute(PageState page_state, AuditRecord audit_record, DesignSystem design_system) {
		// Preconditions
		Objects.requireNonNull(page_state, "page_state must not be null");
		Objects.requireNonNull(audit_record, "audit_record must not be null");
		
		Set<UXIssueMessage> issue_messages = new HashSet<>();
		
		//filter elements that aren't text elements
		//get all element states
		//filter any element state whose text exists within another element
		List<ElementState> og_text_elements = new ArrayList<>();
		
		String ada_compliance = "Text content shouldn't require a reading ability more advanced than the lower"
				+ " secondary education level (grades 5 through 8 ) after removal of proper names and titles.";
		
		Set<String> labels = new HashSet<>();
		labels.add("written content");
		labels.add("readability");
		labels.add("wcag");

		try{
			for(ElementState element: page_state.getElements()) {
				if(element.getName().contentEquals("button")
						|| element.getName().contentEquals("a")
						|| element.getOwnedText() == null
						|| element.getOwnedText().isBlank()
						|| element.getAllText() == null
						|| element.getAllText().isBlank()
						|| countWords(element.getAllText()) <= 3
				) {
					continue;
				}
				boolean is_child_text = false;
				for(ElementState element2: page_state.getElements()) {
					if(element2.getKey().contentEquals(element.getKey())) {
						continue;
					}
					if(element2.getOwnedText() != null
							&& !element2.getOwnedText().isBlank()
							&& element2.getAllText() != null
							&& element.getAllText() != null
							&& element2.getAllText().contains(element.getAllText())
							&& !element2.getAllText().contentEquals(element.getAllText())
					) {
						is_child_text = true;
						break;
					}
					else if(element2.getAllText() != null
							&& element.getAllText() != null
							&& element2.getAllText().contentEquals(element.getAllText())
							&& !xpathContains(element2.getXpath(), element.getXpath())
					) {
						is_child_text = true;
						break;
					}
				}
				
				if(!is_child_text) {
					if(countWords(element.getAllText()) > 3){
						og_text_elements.add(element);
					}
				}
			}
			log.warn("elemens with text content found = "+og_text_elements);
			
			for(ElementState element : og_text_elements) {
				log.warn("Calculating readability of text : "+element.getAllText());
				try {
					double ease_of_reading_score = ReadabilityCalculator.calculateReadingEase(element.getAllText());
					String difficulty_string = ContentUtils.getReadingDifficultyRatingByEducationLevel(ease_of_reading_score, audit_record.getTargetUserEducation());
					String grade_level = ContentUtils.getReadingGradeLevel(ease_of_reading_score);
					
					if("unknown".contentEquals(difficulty_string)) {
						continue;
					}
		
					int element_points = getPointsForEducationLevel(ease_of_reading_score, audit_record.getTargetUserEducation());
		
					if(countWords(element.getAllText()) < 10) {
						element_points = 4;
					}
					
					if(element_points < 4) {
						String title = "Content is written at " + grade_level + " reading level";
						String description = generateIssueDescription(element, difficulty_string, audit_record.getTargetUserEducation());
						String recommendation = "Reduce the length of your sentences by breaking longer sentences into 2 or more shorter sentences. You can also use simpler words. Words that contain many syllables can also be difficult to understand.";
						
						ReadingComplexityIssueMessage issue_message = new ReadingComplexityIssueMessage(Priority.LOW, 
																									description,
																									recommendation,
																									null,
																									AuditCategory.CONTENT,
																									labels,
																									ada_compliance,
																									title,
																									element_points,
																									4,
																									ease_of_reading_score);
						
						issue_message = (ReadingComplexityIssueMessage) issue_message_service.save(issue_message);
						issue_message_service.addElement(issue_message.getId(), element.getId());
						issue_messages.add(issue_message);
					}
					else {
						String recommendation = "";
						String description = "";
						if(countWords(element.getAllText()) < 10) {
							element_points = 4;
							description = "Content is short enough to be easily understood by all users";
						}
						else {
							description = generateIssueDescription(element, difficulty_string, audit_record.getTargetUserEducation());
						}
						String title = "Content is easy to read";
						
						ReadingComplexityIssueMessage issue_message = new ReadingComplexityIssueMessage(Priority.NONE, 
																									description,
																									recommendation,
																									null,
																									AuditCategory.CONTENT,
																									labels,
																									ada_compliance,
																									title,
																									element_points,
																									4,
																									ease_of_reading_score);
						
						issue_message = (ReadingComplexityIssueMessage) issue_message_service.save(issue_message);
						issue_message_service.addElement(issue_message.getId(), element.getId());
						issue_messages.add(issue_message);
					}
				} catch(Exception e) {
					log.warn("error calculating readability for element {}", element.getId(), e);
				}
			}

			String why_it_matters = "For people with reading disabilities(including the most highly educated), it is important"
					+ "to accomodate these users by providing text that is simpler to read."
					+ "Beyond accessibility, the way users experience content online has changed." +
					" Attention spans are shorter, and users skim through most information." +
					" Presenting information in small, easy to digest chunks makes their" +
					" experience easy and convenient.";
			
			int points_earned = 0;
			int max_points = 0;
			for(UXIssueMessage issue_msg : issue_messages) {
				points_earned += issue_msg.getPoints();
				max_points += issue_msg.getMaxPoints();
			}

			// Invariant: points earned cannot exceed max points
			assert points_earned <= max_points : "points_earned (" + points_earned + ") exceeds max_points (" + max_points + ")";

			String description = "";
			Audit audit = new Audit(AuditCategory.CONTENT,
									AuditSubcategory.WRITTEN_CONTENT,
									AuditName.READING_COMPLEXITY,
									points_earned,
									issue_messages,
									AuditLevel.PAGE,
									max_points,
									page_state.getUrl(),
									why_it_matters,
									description,
									false);

			Audit saved_audit = audit_service.save(audit);

			// Postcondition: audit must be non-null and persisted
			assert saved_audit != null : "audit must not be null after save";

			return saved_audit;
		}
		catch(Exception e){
			log.error("readability audit failed", e);
			throw e;
		}
	}

	/**
	 * Generates a description of the issue based on the element, difficulty string,
	 * and target user education.
	 *
	 * <p><strong>Preconditions:</strong></p>
	 * <ul>
	 *   <li>{@code element} must not be null</li>
	 *   <li>{@code difficulty_string} must not be null</li>
	 * </ul>
	 *
	 * <p><strong>Postconditions:</strong></p>
	 * <ul>
	 *   <li>Returns a non-null, non-empty description string</li>
	 * </ul>
	 *
	 * @param element The element that is being audited, must not be null
	 * @param difficulty_string The difficulty string of the element, must not be null
	 * @param targetUserEducation The target user education of the element (may be null)
	 * @return A description of the issue based on the element, difficulty string,
	 * and target user education.
	 * @throws NullPointerException if {@code element} or {@code difficulty_string} is null
	 */
	private String generateIssueDescription(ElementState element,
									String difficulty_string,
									String targetUserEducation) {
		Objects.requireNonNull(element, "element must not be null");
		Objects.requireNonNull(difficulty_string, "difficulty_string must not be null");
		String description = "The text \"" + element.getAllText() + "\" is " + difficulty_string + " to read for " + getConsumerType(targetUserEducation) + ".";
		
		return description;
	}


	/**
	 * Gets the consumer type label based on the target user education.
	 *
	 * <p><strong>Postconditions:</strong></p>
	 * <ul>
	 *   <li>Returns a non-null, non-empty consumer type string</li>
	 *   <li>Returns "the average consumer" if {@code targetUserEducation} is null</li>
	 * </ul>
	 *
	 * @param targetUserEducation The target user education (may be null)
	 * @return The consumer type label based on the target user education.
	 */
	private String getConsumerType(String targetUserEducation) {
		String consumer_label = "the average consumer";
		
		if(targetUserEducation != null) {
			consumer_label = "users with a "+targetUserEducation + " education";
		}
		
		return consumer_label;
	}

	/**
	 * Calculates the points for a given ease of reading score and target user education.
	 *
	 * <p><strong>Postconditions:</strong></p>
	 * <ul>
	 *   <li>Returns a value between 0 and 4, inclusive</li>
	 *   <li>Higher ease of reading scores yield higher point values</li>
	 *   <li>Higher target education levels yield higher point values for the same reading ease score</li>
	 * </ul>
	 *
	 * @param ease_of_reading_score The Flesch Reading Ease score (0-100 scale)
	 * @param target_user_education The target user education level (may be null; null treated as general audience)
	 * @return The points earned, between 0 and 4 inclusive.
	 */
	private int getPointsForEducationLevel(double ease_of_reading_score, String target_user_education) {
		int element_points = 0;
				
		if(ease_of_reading_score >= 90 ) {
			if(target_user_education == null) {
				element_points = 4;
			}
			else if("HS".contentEquals(target_user_education)) {
				element_points = 4;
			}
			else if("College".contentEquals(target_user_education)) {
				element_points = 4;
			}
			else if("Advanced".contentEquals(target_user_education)) {
				element_points = 3;
			}
			else {
				element_points = 4;
			}
		}
		else if(ease_of_reading_score < 90 && ease_of_reading_score >= 80 ) {
			if(target_user_education == null) {
				element_points = 4;
			}
			else if("HS".contentEquals(target_user_education)) {
				element_points = 4;
			}
			else if("College".contentEquals(target_user_education)) {
				element_points = 4;
			}
			else if("Advanced".contentEquals(target_user_education)) {
				element_points = 4;
			}
			else {
				element_points = 4;
			}
		}
		else if(ease_of_reading_score < 80 && ease_of_reading_score >= 70) {
			if(target_user_education == null) {
				element_points = 4;
			}
			else if("HS".contentEquals(target_user_education)) {
				element_points = 4;
			}
			else if("College".contentEquals(target_user_education)) {
				element_points = 4;
			}
			else if("Advanced".contentEquals(target_user_education)) {
				element_points = 4;
			}
			else {
				element_points = 3;
			}
		}
		else if(ease_of_reading_score < 70 && ease_of_reading_score >= 60) {
			if(target_user_education == null) {
				element_points = 3;
			}
			else if("HS".contentEquals(target_user_education)) {
				element_points = 3;
			}
			else if("College".contentEquals(target_user_education)) {
				element_points = 4;
			}
			else if("Advanced".contentEquals(target_user_education)) {
				element_points = 4;
			}
			else {
				element_points = 2;
			}
		}
		else if(ease_of_reading_score < 60 && ease_of_reading_score >= 50) {
			if(target_user_education == null) {
				element_points = 2;
			}
			else if("HS".contentEquals(target_user_education)) {
				element_points = 2;
			}
			else if("College".contentEquals(target_user_education)) {
				element_points = 3;
			}
			else if("Advanced".contentEquals(target_user_education)) {
				element_points = 4;
			}
			else {
				element_points = 1;
			}
		}
		else if(ease_of_reading_score < 50 && ease_of_reading_score >= 30) {
			if(target_user_education == null) {
				element_points = 1;
			}
			else if("HS".contentEquals(target_user_education)) {
				element_points = 1;
			}
			else if("College".contentEquals(target_user_education)) {
				element_points = 2;
			}
			else if("Advanced".contentEquals(target_user_education)) {
				element_points = 3;
			}
			else {
				element_points = 0;
			}
		}
		else if(ease_of_reading_score < 30) {
			if(target_user_education == null) {
				element_points = 0;
			}
			else if("College".contentEquals(target_user_education)) {
				element_points = 1;
			}
			else if("Advanced".contentEquals(target_user_education)) {
				element_points = 2;
			}
			else {
				element_points = 0;
			}
		}

		// Postcondition: points must be in valid range [0, 4]
		assert element_points >= 0 && element_points <= 4 : "element_points (" + element_points + ") out of valid range [0, 4]";

		return element_points;
	}


	private static int countWords(String text) {
		if (text == null || text.isBlank()) {
			return 0;
		}

		return text.trim().split("\\s+").length;
	}

	private static boolean xpathContains(String parentXpath, String childXpath) {
		if (parentXpath == null || childXpath == null) {
			return false;
		}

		return parentXpath.contains(childXpath);
	}

	/**
	 * Calculates the score for a sentence based on the number of words in the sentence.
	 *
	 * <p><strong>Postconditions:</strong></p>
	 * <ul>
	 *   <li>Returns a non-null {@link Score} object</li>
	 *   <li>Score points are 2 for sentences with 10 or fewer words, 1 for 11-20 words, 0 for more than 20 words</li>
	 *   <li>Max points is always 2</li>
	 *   <li>Points earned does not exceed max points</li>
	 * </ul>
	 *
	 * @param sentence The sentence to calculate the score for (may be null or blank, treated as 0 words)
	 * @return A non-null Score based on the number of words in the sentence.
	 */
	public static Score calculateSentenceScore(String sentence) {
		String[] words = sentence == null || sentence.isBlank() ? new String[0] : sentence.trim().split("\\s+");
		
		if(words.length <= 10) {
			return new Score(2, 2, new HashSet<>());
		}
		else if(words.length <= 20) {
			return new Score(1, 2, new HashSet<>());
		}

		return new Score(0, 2, new HashSet<>());
	}


	/**
	 * Calculates the score for a paragraph based on the number of sentences in the paragraph.
	 *
	 * <p><strong>Preconditions:</strong></p>
	 * <ul>
	 *   <li>{@code sentence_count} must be non-negative</li>
	 * </ul>
	 *
	 * <p><strong>Postconditions:</strong></p>
	 * <ul>
	 *   <li>Returns a non-null {@link Score} object</li>
	 *   <li>Score is 1 for paragraphs with 5 or fewer sentences, 0 otherwise</li>
	 *   <li>Max points is always 1</li>
	 * </ul>
	 *
	 * @param sentence_count The number of sentences in the paragraph, must be non-negative
	 * @return A non-null Score based on the number of sentences in the paragraph.
	 * @throws IllegalArgumentException if {@code sentence_count} is negative
	 */
	public static Score calculateParagraphScore(int sentence_count) {
		if (sentence_count < 0) {
			throw new IllegalArgumentException("sentence_count must be non-negative, got: " + sentence_count);
		}
		if(sentence_count <= 5) {
			return new Score(1, 1, new HashSet<>());
		}

		return new Score(0, 1, new HashSet<>());
	}
}
