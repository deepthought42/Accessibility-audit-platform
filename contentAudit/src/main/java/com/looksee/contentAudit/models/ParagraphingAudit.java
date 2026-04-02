package com.looksee.contentAudit.models;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.cloud.language.v1.Sentence;
import com.looksee.gcp.CloudNLPUtils;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.Score;
import com.looksee.models.audit.interfaces.IExecutablePageStateAudit;
import com.looksee.models.audit.messages.SentenceIssueMessage;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.models.enums.Priority;
import com.looksee.services.AuditService;
import com.looksee.services.UXIssueMessageService;
import com.looksee.utils.BrowserUtils;

import lombok.NoArgsConstructor;

/**
 * Responsible for executing an audit on the text content on a page to ensure
 * that the text is easy to read and understand.
 *
 * <p>This audit evaluates the paragraphing of text content on a web page to
 * ensure it meets readability standards. It checks for proper sentence length
 * and spacing between paragraphs to enhance user experience.</p>
 *
 * <p>The audit supports WCAG Level A compliance for success criterion 3.1.1
 * by ensuring that text content meets readability standards and is presented
 * in a way that is easy to understand.</p>
 *
 * WCAG Level - AAA
 * WCAG Success Criterion - https://www.w3.org/TR/UNDERSTANDING-WCAG20/meaning-supplements.html
 */
@Component
@NoArgsConstructor
public class ParagraphingAudit implements IExecutablePageStateAudit {
	private static Logger log = LoggerFactory.getLogger(ParagraphingAudit.class);
	
	@Autowired
	private AuditService audit_service;
	
	@Autowired
	private UXIssueMessageService issue_message_service;
	
	/**
	 * Executes a paragraphing audit on a web page to assess sentence length
	 * compliance with EU and US governmental standards to ensure that the
	 * text is easy to read and understand.
	 * 
	 * <p><strong>Preconditions:</strong></p>
	 * <ul>
	 *   <li>{@code page_state} must not be null</li>
	 *   <li>{@code page_state.getElements()} must contain valid ElementState objects</li>
	 *   <li>{@code audit_record} must be a valid audit record for tracking</li>
	 *   <li>{@code design_system} must be provided (though unused in this implementation)</li>
	 * </ul>
	 * 
	 * <p><strong>Process:</strong></p>
	 * <ul>
	 *   <li>Retrieves all text elements from the page using BrowserUtils.getTextElements()</li>
	 *   <li>For each text element, extracts owned text content and splits into paragraphs by newline characters</li>
	 *   <li>Filters out paragraphs with fewer than 3 words</li>
	 *   <li>Adds periods to paragraphs that don't contain sentence-ending punctuation</li>
	 *   <li>Uses Google Cloud NLP (CloudNLPUtils.extractSentences()) to parse paragraphs into individual sentences</li>
	 *   <li>Evaluates each sentence against the 25-word maximum length standard used in EU and US governmental documentation</li>
	 *   <li>Creates SentenceIssueMessage objects for sentences that exceed the limit or meet the standard</li>
	 *   <li>Calculates overall score based on points earned vs maximum possible points</li>
	 * </ul>
	 * 
	 * <p><strong>Postconditions:</strong></p>
	 * <ul>
	 *   <li>Returns a non-null Audit object with CONTENT category, WRITTEN_CONTENT subcategory, and PARAGRAPHING audit name</li>
	 *   <li>The audit contains calculated points earned and maximum points based on sentence length compliance</li>
	 *   <li>All SentenceIssueMessage objects are persisted to the database via UXIssueMessageService</li>
	 *   <li>The audit is saved to the database via AuditService</li>
	 *   <li>All issue messages are associated with the audit record</li>
	 * </ul>
	 * 
	 * <p><strong>Scoring:</strong></p>
	 * <ul>
	 *   <li>Sentences with 25 words or fewer: 1 point earned, 1 point maximum</li>
	 *   <li>Sentences with more than 25 words: 0 points earned, 1 point maximum</li>
	 *   <li>Overall score is the sum of all sentence scores across all text elements</li>
	 * </ul>
	 * 
	 * @param page_state The page state containing elements to audit, must not be null
	 * @param audit_record The audit record for tracking this audit execution
	 * @param design_system The design system context (unused in this implementation)
	 * @return A completed Audit object with paragraphing compliance results
	 * @throws NullPointerException if {@code page_state} is null
	 * @throws RuntimeException if CloudNLPUtils.extractSentences() fails for any paragraph
	 */
	@Override
	public Audit execute(PageState page_state,
						AuditRecord audit_record,
						DesignSystem design_system) {
		// Preconditions
		Objects.requireNonNull(page_state, "page_state must not be null");

		Set<UXIssueMessage> issue_messages = new HashSet<>();
		
		List<ElementState> element_list = BrowserUtils.getTextElements(page_state.getElements());
		
		for(ElementState element : element_list) {
			String text_block = element.getOwnedText();
			if(text_block == null || text_block.isBlank()) {
				continue;
			}
			
			//    parse text block into paragraph chunks(multiple paragraphs can exist in a text block)
			String[] paragraphs = text_block.split("\n");
			for(String paragraph : paragraphs) {
				paragraph = paragraph.trim();
				if(paragraph.isEmpty() || paragraph.split("\\s+").length < 3) {
					continue;
				}
				else if(!paragraph.contains(".")) {
					paragraph = paragraph + ".";
				}
				try {
					List<Sentence> sentences = CloudNLPUtils.extractSentences(paragraph);
					Score score = calculateSentenceScore(sentences, element);

					issue_messages.addAll(score.getIssueMessages());
				} catch (Exception e) {
					log.warn("error getting sentences from text :: {}", paragraph, e);
				}

			}
			// validate that spacing between paragraphs is at least 2x the font size within the paragraphs
		}
		
		String why_it_matters = "The way users experience content has changed in the mobile phone era." + 
				" Attention spans are shorter, and users skim through most information." + 
				" Presenting information in small, easy to digest chunks makes their" + 
				" experience easy and convenient. ";


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
							AuditName.PARAGRAPHING,
							points_earned,
							null,
							AuditLevel.PAGE,
							max_points,
							page_state.getUrl(),
							why_it_matters,
							description,
							false);

		audit = audit_service.save(audit);
		audit_service.addAllIssues(audit.getId(), issue_messages);

		// Postcondition: audit must be non-null and persisted
		assert audit != null : "audit must not be null after save";

		return audit;
	}


	/**
	 * Reviews list of sentences and gives a score based on how many of those
	 * sentences have 25 words or less. This is considered the maximum sentence
	 * length allowed in EU government documentation.
	 *
	 * <p><strong>Preconditions:</strong></p>
	 * <ul>
	 *   <li>{@code sentences} must not be null</li>
	 *   <li>{@code element} must not be null</li>
	 * </ul>
	 *
	 * <p><strong>Postconditions:</strong></p>
	 * <ul>
	 *   <li>Returns a non-null {@link Score} object</li>
	 *   <li>Points earned does not exceed max points</li>
	 *   <li>Each sentence contributes exactly 1 to max points</li>
	 *   <li>All generated {@link SentenceIssueMessage} objects are persisted</li>
	 * </ul>
	 *
	 * @param sentences The list of sentences to evaluate, must not be null
	 * @param element The element containing the sentences, must not be null
	 * @return A non-null Score with points earned, max points, and associated issue messages
	 * @throws NullPointerException if {@code sentences} or {@code element} is null
	 */
	public Score calculateSentenceScore(List<Sentence> sentences, ElementState element) {
		Objects.requireNonNull(sentences, "sentences must not be null");
		Objects.requireNonNull(element, "element must not be null");
		int points_earned = 0;
		int max_points = 0;
		Set<UXIssueMessage> issue_messages = new HashSet<>();
		Set<String> labels = new HashSet<>();
		labels.add("written content");
		labels.add("paragraphs");
		labels.add("readability");
		
		String ada_compliance = "There are no ADA compliance requirements for this category.";
		
		for(Sentence sentence : sentences) {
			String sentenceText = sentence != null && sentence.getText() != null ? sentence.getText().getContent() : null;
			String[] words = sentenceText == null || sentenceText.isBlank() ? new String[0] : sentenceText.trim().split("\\s+");
			
			if(words.length > 25) {

				String recommendation = "Try reducing the size of the sentence or breaking it up into multiple sentences";
				String title = "Sentence is too long";
				String description = "The sentence  \"" + sentence.getText().getContent() + "\" has more than 25 words which can make it difficult for users to understand";

				SentenceIssueMessage issue_message = new SentenceIssueMessage(
																Priority.MEDIUM,
																description,
																recommendation,
																element,
																AuditCategory.CONTENT,
																labels,
																ada_compliance,
																title,
																0,
																1,
																words.length);
				
				issue_message = (SentenceIssueMessage) issue_message_service.save(issue_message);
				issue_messages.add(issue_message);

				max_points += 1;
			}
			else {
				points_earned += 1;
				max_points += 1;
				String recommendation = "";
				String title = "Sentence meets EU and US governmental standards for sentence length";
				String description = "The sentence  \"" + sentence.getText().getContent() + "\" has less than 25 words which is the standard for governmental documentation in the European Union(EU) and the United States(US)";

				SentenceIssueMessage issue_message = new SentenceIssueMessage(
																Priority.NONE,
																description,
																recommendation,
																element,
																AuditCategory.CONTENT,
																labels,
																ada_compliance,
																title,
																1,
																1,
																words.length);

				issue_message = (SentenceIssueMessage) issue_message_service.save(issue_message);
				issue_messages.add(issue_message);
			}
		}

		// Invariant: points earned cannot exceed max points
		assert points_earned <= max_points : "points_earned (" + points_earned + ") exceeds max_points (" + max_points + ")";

		return new Score(points_earned, max_points, issue_messages);
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
