package com.looksee.utils;

import com.looksee.models.CIEColorSpace;
import com.looksee.models.ColorData;
import com.looksee.models.PaletteColor;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.AuditScore;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.audit.Score;
import com.looksee.models.audit.messages.ColorPaletteIssueMessage;
import com.looksee.models.audit.messages.ReadingComplexityIssueMessage;
import com.looksee.models.audit.messages.SentenceIssueMessage;
import com.looksee.models.audit.messages.StockImageIssueMessage;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.models.enums.ColorScheme;
import com.looksee.models.enums.Priority;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.SetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for auditing
 */
@NoArgsConstructor
public class AuditUtils {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(AuditUtils.class.getName());

	/**
	 * Calculate the score for a set of {@link Audit}
	 * @param audits the set of {@link Audit} to calculate the score for
	 * @return the score
	 *
	 * precondition: audits != null
	 */
	public static double calculateScore(Set<Audit> audits) {
		assert audits != null;
		
		List<Audit> filtered_audits = audits.parallelStream()
											.filter((s) -> (s.getTotalPossiblePoints() > 0))
											.collect(Collectors.toList());
		
		double scores_total = filtered_audits.parallelStream()
											.mapToDouble(x -> x.getPoints() / (double)x.getTotalPossiblePoints())
											.sum();

		if(filtered_audits.isEmpty()) {
			return -1.0;
		}
		double final_score = (scores_total / (double)filtered_audits.size())*100;
		return final_score;
	}
	
	/**
	 * Reviews set of {@link Audit} and generates audits scores for content,
	 *   information architecture, aesthetics, interactivity and accessibility
	 *
	 * @param audits the set of {@link Audit} to extract the score from
	 * @return the {@link AuditScore}
	 *
	 * precondition: audits != null
	 */
	public static AuditScore extractAuditScore(Set<Audit> audits) {
		assert audits != null;

		double content_score = 0;
		int content_count = 0;
		
		double info_architecture_score = 0;
		int info_architecture_count = 0;
		
		double aesthetic_score = 0;
		int aesthetic_count = 0;
		
		double interactivity_score = 0;
		
		for(Audit audit: audits) {
			if(audit.getTotalPossiblePoints() == 0) {
				continue;
			}
			
			if(AuditCategory.CONTENT.equals(audit.getCategory())) {
				content_score += (audit.getPoints()/(double)audit.getTotalPossiblePoints());
				content_count++;
			}
			else if(AuditCategory.INFORMATION_ARCHITECTURE.equals(audit.getCategory())) {
				info_architecture_score += (audit.getPoints()/(double)audit.getTotalPossiblePoints());
				info_architecture_count++;
			}
			else if(AuditCategory.AESTHETICS.equals(audit.getCategory())) {
				aesthetic_score += (audit.getPoints()/(double)audit.getTotalPossiblePoints());
				aesthetic_count++;
			}
		}
		
		if(content_count > 0) {
			content_score = ( content_score / (double)content_count ) * 100;
		}
		else {
			content_score = -1;
		}
		
		if(info_architecture_count > 0) {
			info_architecture_score = ( info_architecture_score / (double)info_architecture_count ) * 100;
		}
		else {
			info_architecture_score = -1;
		}
		
		if(aesthetic_count > 0) {
			aesthetic_score = ( aesthetic_score / (double)aesthetic_count ) * 100;
		}
		else {
			aesthetic_score = -1;
		}
		
		double readability = extractLabelScore(audits, "readability");
		double spelling_grammar = extractLabelScore(audits, "spelling");
		double image_quality = extractLabelScore(audits, "images");
		double alt_text = extractLabelScore(audits, "alt_text");
		double links = extractLabelScore(audits, "links");
		double metadata = extractLabelScore(audits, "metadata");
		double seo = extractLabelScore(audits, "seo");
		double security = extractLabelScore(audits, "security");
		double color_contrast = extractLabelScore(audits, "color contrast");
		double text_contrast = AuditUtils.calculateScoreByName(audits, AuditName.TEXT_BACKGROUND_CONTRAST);
		double non_text_contrast = AuditUtils.calculateScoreByName(audits, AuditName.NON_TEXT_BACKGROUND_CONTRAST);
		double whitespace = extractLabelScore(audits, "whitespace");
		double accessibility = extractLabelScore(audits, "accessibility");
		
		return new AuditScore(content_score,
								readability,
								spelling_grammar,
								image_quality,
								alt_text,
								info_architecture_score,
								links,
								metadata,
								seo,
								security,
								aesthetic_score,
								color_contrast,
								whitespace,
								interactivity_score,
								accessibility,
								text_contrast,
								non_text_contrast);
		
	}

	/**
	 * Extract the score for a label
	 * @param audits the set of {@link Audit} to extract the score from
	 * @param label the label to extract the score from
	 * @return the score
	 *
	 * precondition: audits != null
	 * precondition: label != null
	 */
	private static double extractLabelScore(Set<Audit> audits, String label) {
		assert audits != null;
		assert label != null;
		
		double score = 0.0;
		int count = 0;
		for(Audit audit: audits) {
			for(UXIssueMessage msg: audit.getMessages()) {
				if(msg.getLabels().contains(label)) {
					count++;
					score += (msg.getPoints() / (double)msg.getMaxPoints());
				}
			}
		}
		
		if(count <= 0) {
			return 0.0;
		}
		
		return score / (double)count;
	}

	/**
	 * Check if a page audit is complete
	 * @param audit_record the {@link AuditRecord} to check
	 * @return true if the page audit is complete, false otherwise
	 * 
	 * precondition: audit_record != null
	 */
	public static boolean isPageAuditComplete(AuditRecord audit_record) {
		assert audit_record != null;
		
		return audit_record.getAestheticAuditProgress() >= 1 
			&& audit_record.getContentAuditProgress() >= 1
			&& audit_record.getInfoArchitectureAuditProgress() >= 1
			&& audit_record.getDataExtractionProgress() >= 1;
	}

	/**
	 * Get the experience rating for a page audit
	 * @param audit_record the {@link PageAuditRecord} to get the experience rating for
	 * @return the experience rating
	 *
	 * precondition: audit_record != null
	 */
	public static String getExperienceRating(PageAuditRecord audit_record) {
		assert audit_record != null;
		
		double score = audit_record.getAestheticAuditProgress();
		score += audit_record.getContentAuditProgress();
		score += audit_record.getInfoArchitectureAuditProgress();
		
		double final_score = score / 3;
		if(final_score >= 80) {
			return "delightful";
		}
		else if(final_score <80.0 && final_score >= 60) {
			return "almost there";
		}
		else {
			return "needs work";
		}
	}
	
	/**
	 * Check if the aesthetics audit is complete
	 * @param audits the set of {@link Audit} to check
	 * @return true if the aesthetics audit is complete, false otherwise
	 *
	 * precondition: audits != null
	 */
	public static boolean isAestheticsAuditComplete(Set<Audit> audits) {
		assert audits != null;
		
		return audits.size() == 2;
	}

	/**
	 * Check if the content audit is complete
	 * @param audits the set of {@link Audit} to check
	 * @return true if the content audit is complete, false otherwise
	 *
	 * precondition: audits != null
	 */
	public static boolean isContentAuditComplete(Set<Audit> audits) {
		assert audits != null;
		
		return audits.size() == 3;
	}
	
	/**
	 * Check if the information architecture audit is complete
	 * @param audits the set of {@link Audit} to check
	 * @return true if the information architecture audit is complete, false otherwise
	 *
	 * precondition: audits != null
	 */
	public static boolean isInformationArchitectureAuditComplete(Set<Audit> audits) {
		assert audits != null;
		
		return audits.size() == 3;
	}

	/**
	 * Calculate the score for all audits that have the given subcategory
	 *
	 * @param audits the set of {@link Audit} to calculate the score for
	 * @param subcategory the subcategory to calculate the score for
	 *
	 * @return the score
	 *
	 * precondition: audits != null
	 * precondition: subcategory != null
	 */
	public static double calculateSubcategoryScore(Set<Audit> audits, AuditSubcategory subcategory) {
		assert audits != null;
		assert subcategory != null;
		
		
		List<Audit> filtered_audits = audits.parallelStream()
				.filter((s) -> (s.getTotalPossiblePoints() > 0 && s.getSubcategory().equals(subcategory)))
				.collect(Collectors.toList());

		double scores_total = filtered_audits.parallelStream()
				.mapToDouble(x -> (x.getPoints() / (double)x.getTotalPossiblePoints()))
				.sum();

		if(filtered_audits.isEmpty()) {
			return -1.0;
		}
		
		double category_score = (scores_total / (double)filtered_audits.size())*100;
		
		return category_score;
	}

	/**
	 * Calculate the score for a category
	 * @param audits the set of {@link Audit} to calculate the score for
	 * @param category the category to calculate the score for
	 * @return the score
	 *
	 * precondition: audits != null
	 * precondition: category != null
	 */
	public static double calculateScoreByCategory(Set<Audit> audits, AuditCategory category) {
		assert audits != null;
		assert category != null;
			
		List<Audit> filtered_audits = audits.parallelStream()
							.filter((s) -> (s.getTotalPossiblePoints() > 0 && s.getCategory().equals(category)))
							.collect(Collectors.toList());
		
		
		double scores_total = filtered_audits.parallelStream()
					.mapToDouble(x -> (x.getPoints() / (double)x.getTotalPossiblePoints()))
					.sum();
		
		if(filtered_audits.isEmpty()) {
			return -1.0;
		}
		
		double category_score = (scores_total / (double)filtered_audits.size())*100;
		return category_score;
	}

	/**
	 * Calculates percentage score based on audits with the given name
	 *
	 * @param audits the set of {@link Audit} to calculate the score for
	 * @param name the name of the audit to calculate the score for
	 * @return the score
	 *
	 * precondition: audits != null
	 * precondition: name != null
	 */
	public static double calculateScoreByName(Set<Audit> audits, AuditName name) {
		assert audits != null;
		assert name != null;
		
		//int audit_cnt = 0;
	
		List<Audit> filtered_audits = audits.parallelStream()
					.filter((s) -> (s.getTotalPossiblePoints() > 0 && name.equals(s.getName())))
					.collect(Collectors.toList());
		
		double scores_total = filtered_audits.parallelStream()
					.mapToDouble(x -> { return x.getPoints() / (double)x.getTotalPossiblePoints(); })
					.sum();

		if(filtered_audits.isEmpty()) {
			return -1.0;
		}
		
		double category_score = (scores_total / (double)filtered_audits.size())*100;
		
		return category_score;
	}

	/**
	 * Calculates percentage of failing large text items
	 * 
	 * @param audits the set of {@link Audit} to calculate the percentage of passing large text items for
	 * @return the percentage of passing large text items
	 *
	 * precondition: audits != null
	 */
	public static double getPercentPassingLargeTextItems(Set<Audit> audits) {
		assert audits != null;
		
		int count_large_text_items = 0;
		int failing_large_text_items = 0;
		
		for(Audit audit: audits) {
			//get audit issue messages
			for(UXIssueMessage msg : audit.getMessages()){
				if(msg.getTitle().contains("Large text")) {
					count_large_text_items++;
					if(msg.getPoints() == msg.getMaxPoints()) {
						failing_large_text_items++;
					}
				}
			}
		}
		
		return count_large_text_items / (double)failing_large_text_items;
	}

	/**
	 * Calculate the percentage of failing small text items
	 * @param audits the set of {@link Audit} to calculate the percentage of failing small text items for
	 * @return the percentage of failing small text items
	 *
	 * precondition: audits != null
	 */
	public static double getPercentFailingSmallTextItems(Set<Audit> audits) {
		assert audits != null;
		
		int count_text_items = 0;
		int failing_text_items = 0;
		
		for(Audit audit: audits) {
			//get audit issue messages
			for(UXIssueMessage msg : audit.getMessages()){
				if(msg.getDescription().contains("Text has")) {
					count_text_items++;
					if(msg.getPoints() == msg.getMaxPoints()) {
						failing_text_items++;
					}
				}
			}
		}
		
		return count_text_items / (double)failing_text_items;
	}

	/**
	 * Retrieves count of pages that have non text contrast issue
	 * 
	 * @param page_audits the set of {@link PageAuditRecord} to get the count of pages with non text contrast issue for
	 * @param subcategory the subcategory to get the count of pages with non text contrast issue for
	 * @return the count of pages with non text contrast issue
	 *
	 * precondition: page_audits != null
	 * precondition: subcategory != null
	 */
	public static int getCountPagesWithSubcategoryIssues(Set<PageAuditRecord> page_audits,
														AuditSubcategory subcategory) {
		assert page_audits != null;
		assert subcategory != null;
		
		int count_failing_pages = 0;
		for(PageAuditRecord page_audit : page_audits) {
			for(Audit audit: page_audit.getAudits()) {
				if(subcategory.equals( audit.getSubcategory() ) 
						&& audit.getPoints() < audit.getTotalPossiblePoints()) {
					count_failing_pages++;
					break;
				}
			}
		}
		
		return count_failing_pages;
	}

	/**
	 * Retrieves count of pages that have non text contrast issue
	 * 
	 * @param page_audits the set of {@link PageAuditRecord} to get the count of pages with non text contrast issue for
	 * @param audit_name the audit name to get the count of pages with non text contrast issue for
	 * @return the count of pages with non text contrast issue
	 *
	 * precondition: page_audits != null
	 * precondition: audit_name != null
	 */
	public static int getCountPagesWithIssuesByAuditName(Set<PageAuditRecord> page_audits,
														AuditName audit_name) {
		assert page_audits != null;
		assert audit_name != null;
		
		int count_failing_pages = 0;
		
		for(PageAuditRecord page_audit : page_audits) {
			for(Audit audit: page_audit.getAudits()) {
				if(audit_name.equals( audit.getName() ) 
						&& audit.getPoints() < audit.getTotalPossiblePoints()) {
					count_failing_pages++;
					break;
				}
			}
		}
		
		return count_failing_pages;
	}

	/**
	 * Retrieves count of pages that have WCAG compliance issues
	 *
	 * @param page_audits the set of {@link PageAuditRecord} to get the count of pages with WCAG compliance issues for
	 * @return the count of pages with WCAG compliance issues
	 *
	 * precondition: page_audits != null
	 */
	public static int getCountOfPagesWithWcagComplianceIssues(Set<PageAuditRecord> page_audits) {
		assert page_audits != null;
		
		int pages_with_issues = 0;
		
		for(PageAuditRecord audit_record : page_audits) {
			boolean has_issue = false;
			for(Audit audit: audit_record.getAudits()) {
				for(UXIssueMessage issue : audit.getMessages()) {
					if(issue.getLabels().contains("wcag") && issue.getPoints() < issue.getMaxPoints()) {
						pages_with_issues++;
						has_issue = true;
						break;
					}
				}
				if(has_issue) {
					break;
				}
			}
		}
		return pages_with_issues;
	}

	/**
	 * Calculates the average words per sentence
	 * @param audits the set of {@link Audit} to calculate the average words per sentence for
	 * @return the average words per sentence
	 *
	 * precondition: audits != null
	 */
	public static double calculateAverageWordsPerSentence(Set<Audit> audits) {
		assert audits != null;
		
		int issue_count = 0;
		int word_count = 0;
		
		for(Audit audit: audits) {
			for(UXIssueMessage issue_msg : audit.getMessages()) {
				if(issue_msg instanceof SentenceIssueMessage) {
					issue_count++;
					word_count += ((SentenceIssueMessage) issue_msg).getWordCount();
				}
			}
		}
		
		return word_count / (double)issue_count;
	}

	/**
	 * Calculates the percentage of stock images
	 * @param audits the set of {@link Audit} to calculate the percentage of stock images for
	 * @return the percentage of stock images
	 *
	 * precondition: audits != null
	 */
	public static double calculatePercentStockImages(Set<Audit> audits) {
		assert audits != null;
		
		int image_count = 0;
		int stock_image_count = 0;
		
		for(Audit audit: audits) {
			for(UXIssueMessage issue_msg : audit.getMessages()) {
				if(issue_msg instanceof StockImageIssueMessage) {
					image_count++;
					if(((StockImageIssueMessage) issue_msg).isStockImage()) {
						stock_image_count++;
					}
				}
			}
		}
		
		return image_count / (double)stock_image_count;
	}

	/**
	 * Calculates the average reading complexity
	 * @param audits the set of {@link Audit} to calculate the average reading complexity for
	 * @return the average reading complexity
	 *
	 * precondition: audits != null
	 */
	public static double calculateAverageReadingComplexity(Set<Audit> audits) {
		assert audits != null;
		
		int reading_complexity_issues = 0;
		double reading_complexity_total = 0;
		
		for(Audit audit: audits) {
			for(UXIssueMessage issue_msg : audit.getMessages()) {
				if(issue_msg instanceof ReadingComplexityIssueMessage) {
					reading_complexity_issues++;
					reading_complexity_total += ((ReadingComplexityIssueMessage) issue_msg).getEaseOfReadingScore();
				}
			}
		}
		
		return reading_complexity_issues / reading_complexity_total;
	}

	/**
	 * Calculates the {@link Audit} progress based on audits completed and the total pages
	 * @param category the category to calculate the progress for
	 * @param page_count the total number of pages
	 * @param audit_list the list of audits to calculate the progress for
	 * @param audit_labels the labels to calculate the progress for
	 * @return the progress
	 *
	 * precondition: category != null
	 * precondition: page_count >= 0
	 * precondition: audit_list != null
	 * precondition: audit_labels != null
	 */
	public static double calculateProgress(AuditCategory category,
										int page_count,
										Set<Audit> audit_list,
										Set<AuditName> audit_labels) {
		assert category != null;
		assert page_count >= 0;
		assert audit_list != null;
		assert audit_labels != null;
				
		Map<AuditName, Integer> audit_count_map = new HashMap<>();
		Set<AuditName> category_audit_labels = getAuditLabels(category, audit_labels);
		
		List<Audit> filtered_audits = audit_list.stream()
												.filter(audit -> category.equals(audit.getCategory()))
												.filter(audit -> category_audit_labels.contains(audit.getName()))
												.collect(Collectors.toList());
		
		
		for(Audit audit : filtered_audits) {
			if(audit_count_map.containsKey(audit.getName())) {
				audit_count_map.put(audit.getName(), audit_count_map.get(audit.getName())+1);
			}
			else {
				audit_count_map.put(audit.getName(), 1);
			}
		}

		double total_count = audit_count_map.values().stream().mapToDouble(i -> i).sum();
		log.warn("total count value = "+total_count);
		total_count = total_count / (double)category_audit_labels.size();
		return total_count / (double)page_count;
	}

	/**
	 * Checks if all expected audit names exist within the Audit
	 * 
	 * @param audits the set of {@link Audit} to check
	 * @param audit_list the set of {@link AuditName} to check
	 * @return true if all expected audit names exist within the Audit, false otherwise
	 *
	 * precondition: audits != null
	 * precondition: audit_list != null
	 */
	public static boolean isPageAuditComplete( Set<Audit> audits,
											Set<AuditName> audit_list) {
		assert audits != null;
		assert audit_list != null;
		
		for(Audit audit : audits) {
			audit_list.remove(audit.getName());
		}
		
		return audit_list.isEmpty();
	}

	/**
	 * Retrieves the intersection between the full set of Audit Labels for a category compared with the desired set of audit labels.
	 * If the category is unknown, then the desired set of audit labels is returned without an intersection operation performed
	 * 
	 * @param category the category to get the audit labels for
	 * @param audit_labels the set of {@link AuditName} to get the audit labels for
	 * @return the intersection between the full set of Audit Labels for a category compared with the desired set of audit labels
	 *
	 * precondition: category != null
	 * precondition: audit_labels != null
	 */
	public static Set<AuditName> getAuditLabels(AuditCategory category, Set<AuditName> audit_labels) {
		assert category != null;
		assert audit_labels != null;
		
		Set<AuditName> aesthetic_audit_labels = new HashSet<>();
		aesthetic_audit_labels.add(AuditName.TEXT_BACKGROUND_CONTRAST);
		aesthetic_audit_labels.add(AuditName.NON_TEXT_BACKGROUND_CONTRAST);
		
		Set<AuditName> content_audit_labels = new HashSet<>();
		content_audit_labels.add(AuditName.ALT_TEXT);
		content_audit_labels.add(AuditName.READING_COMPLEXITY);
		content_audit_labels.add(AuditName.PARAGRAPHING);
		content_audit_labels.add(AuditName.IMAGE_COPYRIGHT);
		content_audit_labels.add(AuditName.IMAGE_POLICY);

		Set<AuditName> info_architecture_audit_labels = new HashSet<>();
		info_architecture_audit_labels.add(AuditName.LINKS);
		info_architecture_audit_labels.add(AuditName.TITLES);
		info_architecture_audit_labels.add(AuditName.ENCRYPTED);
		info_architecture_audit_labels.add(AuditName.METADATA);
		
		if(AuditCategory.AESTHETICS.equals(category)) {
			//retrieve labels that are in both the aesthetic audit labels set and the list of audit labels passed in
			return SetUtils.intersection(aesthetic_audit_labels, audit_labels);
		}
		else if(AuditCategory.CONTENT.equals(category)) {
			//retrieve labels that are in both the content audit labels set and the list of audit labels passed in
			return SetUtils.intersection(content_audit_labels, audit_labels);

		}
		else if(AuditCategory.INFORMATION_ARCHITECTURE.equals(category)) {
			//retrieve labels that are in both the aesthetic audit labels set and the list of audit labels passed in
			return SetUtils.intersection(info_architecture_audit_labels, audit_labels);
		}
		
		return audit_labels;
	}

	/**
	 * Scores site color palette based on the color scheme it most resembles
	 * @param palette the {@link List} of {@link PaletteColor}s
	 * @param scheme the {@link ColorScheme}
	 * @return the {@link Score}
	 *
	 * precondition: palette != null
	 * precondition: scheme != null
	 */
	public static Score getPaletteScore(List<PaletteColor> palette, ColorScheme scheme) {
		assert palette != null;
		assert scheme != null;
		
		//if palette has exactly 1 color set and that color set has more than 1 color, then monochromatic
		int score = 0;
		int max_points = 3;
				
		if(ColorScheme.GRAYSCALE.equals(scheme)) {
			score = 3;
		}
		//check if monochromatic
		else if(ColorScheme.MONOCHROMATIC.equals(scheme)) {
			score = getMonochromaticScore(palette);
		}
		//check if complimentary
		else if( ColorScheme.COMPLEMENTARY.equals(scheme)) {
			score = getComplementaryScore(palette);
		}
		//analogous and triadic both have 3 colors
		else if(ColorScheme.ANALOGOUS.equals(scheme)) {
			score = 3;
		}
		else if(ColorScheme.SPLIT_COMPLIMENTARY.equals(scheme)) {
			log.debug("Color scheme is SPLIT COMPLIMENTARY!!!");
			score = 3;
		}
		else if(ColorScheme.TRIADIC.equals(scheme)) {
			//check if triadic
			//if hues are nearly equal in differences then return triadic
			log.debug("Color scheme is TRIADIC!!!");
			score = 3;
		}
		else if(ColorScheme.TETRADIC.equals(scheme)) {
			log.debug("Color scheme is Tetradic!!!");

			//check if outer points are equal in distances
			score = 2;
		}
		else {
			//unknown color scheme
			score = 0;
		}
		
		return new Score(score, max_points, new HashSet<>());
	}
	
	/**
	 * Scores site color palette based on the design system color palette and the colors found on the page
	 * 
	 * @param palette {@link List} of colors that define the accepted {@link DesignSystem} palette
	 * @param colors {@link List} of colors that were found on the page
	 * 
	 * @return {@link Score}
	 * 
	 * precondition: palette != null
	 * precondition: colors != null
	 */
	public static Score getPaletteScore(List<String> palette, List<ColorData> colors) {
		assert palette != null;
		assert colors != null;
		
		//if palette has exactly 1 color set and that color set has more than 1 color, then monochromatic
		int score = 0;
		int max_points = 0;
		
		Map<String, Boolean> non_compliant_colors = ColorPaletteUtils.retrieveNonCompliantColors(palette, colors);
		
		//if all colors match up with a palette color in hue then score is 1/1
		//otherwise score is based on the number of colors that deviate from the design system
		Set<String> labels = new HashSet<>();
		labels.add("brand");
		labels.add("color");
		
		Set<String> categories = new HashSet<>();
		categories.add(AuditCategory.AESTHETICS.toString());
		
		Set<UXIssueMessage> messages = new HashSet<>();
		if(!non_compliant_colors.isEmpty()) {
			String title = "Page colors don't conform to the brand";
			String description = "Colors were found that aren't in the design system";
			String recommendation = "You shouldn't use colors that aren't part of brand's design system";
			String ada_compliance = "There are no ADA compliance guidelines regarding the website color" + 
									" palette. However, keeping a cohesive color palette allows you to create" + 
									" a webpage that is easy for everyone to read and interact with ";
			
			UXIssueMessage palette_issue_message = new ColorPaletteIssueMessage(
																	Priority.HIGH,
																	description,
																	recommendation,
																	non_compliant_colors.keySet(),
																	palette,
																	AuditCategory.AESTHETICS,
																	labels,
																	ada_compliance,
																	title,
																	0,
																	1);
			
			messages.add(palette_issue_message);
		}
		else {
			String title = "Page colors are on brand!";
			String description = "All colors on the page are part of the design system";
			String recommendation = "";
			String ada_compliance = "";
			
			//for(String color : non_compliant_colors.keySet()) {
			UXIssueMessage palette_success_message = new ColorPaletteIssueMessage(
																	Priority.HIGH,
																	description,
																	recommendation,
																	non_compliant_colors.keySet(),
																	palette,
																	AuditCategory.AESTHETICS,
																	labels,
																	ada_compliance,
																	title,
																	1,
																	1);
			
			messages.add(palette_success_message);
		}
		return new Score(score, max_points, messages);
	}

	/**
	 * Determines the {@link ColorScheme} of a {@link Collection} of {@link PaletteColor}s
	 * NOTE:: we consider black and white as one color and the shades of gray as shades of 1 extreme meaning that grayscale is 1 color(gray) with many shades.
	 *
	 * @param palette the {@link Collection} of {@link PaletteColor}s
	 * @return the {@link ColorScheme}
	 *
	 * precondition: palette != null
	 */
	public static ColorScheme getColorScheme(Collection<PaletteColor> palette) {
		assert palette != null;
		
		//if palette has exactly 1 color set and that color set has more than 1 color, then monochromatic
		if(palette.isEmpty()) {
			return ColorScheme.GRAYSCALE;
		}
		//check if monochromatic
		else if(palette.size() == 1 && palette.iterator().next().getTintsShadesTones().size() > 1) {
			log.debug("COLOR IS MONOCHROMATIC!!!!!!!");
			return ColorScheme.MONOCHROMATIC;
		}
		
		//check if complimentary
		else if( palette.size() == 2 ) {
			return ColorScheme.COMPLEMENTARY;
		}
		//analogous and triadic both have 3 colors
		else if(palette.size() == 3) {
			//check if analogous
			//if difference in hue is less than 0.40 for min and max hues then return analogous
			double min_hue = 1.0;
			double max_hue = 0.0;
			for(PaletteColor palette_color : palette) {
				ColorData color = new ColorData(palette_color.getPrimaryColor());
				if(color.getHue() > max_hue) {
					max_hue = color.getHue();
				}
				if(color.getHue() < min_hue) {
					min_hue = color.getHue();
				}
			}
			
			if((max_hue-min_hue) < 0.16) {
				log.debug("Color scheme is ANALOGOUS");
				return ColorScheme.ANALOGOUS;
			}
			else {
				//if all hues are roughly the same distance apart, then TRIADIC
				if(areEquidistantColors(palette)) {
					return ColorScheme.TRIADIC;
				}
				else {
					return ColorScheme.SPLIT_COMPLIMENTARY;
				}
			}
		}
		else if(palette.size() == 4) {
			log.debug("Color scheme is Tetradic!!!");
			//check if hues are equal in differences
			return ColorScheme.TETRADIC;
		}
		else {
			return ColorScheme.UNKNOWN;
		}
	}
	
	/**
	 * Checks if all colors are equidistant on the color wheel
	 *
	 * @param colors the {@link Collection} of {@link PaletteColor}s
	 * @return true if all colors are equidistant on the color wheel, otherwise false
	 *
	 * precondition: colors != null
	 */
	private static boolean areEquidistantColors(Collection<PaletteColor> colors) {
		assert colors != null;
		
		List<PaletteColor> color_list = new ArrayList<>(colors);
		List<Double> distances = new ArrayList<>();
		for(int a=0; a < color_list.size()-1; a++) {
			ColorData color_a = new ColorData(color_list.get(a).getPrimaryColor());
			for(int b=a+1; b < color_list.size(); b++) {
				ColorData color_b = new ColorData(color_list.get(b).getPrimaryColor());

				distances.add(
						Math.sqrt( Math.pow((color_b.getHue() - color_a.getHue()), 2) 
								+ Math.pow((color_b.getSaturation() - color_a.getSaturation()), 2) 
								+ Math.pow((color_b.getBrightness() - color_a.getBrightness()), 2)));
			}
		}
		
		for(int a=0; a < distances.size()-1; a++) {
			for(int b=a+1; b < distances.size(); b++) {
				if( Math.abs(distances.get(a) - distances.get(b)) > .05 ){
					return false;
				}
			}
		}
		
		return true;
	}

	/**
	 * Calculates a score for how well a palette adheres to a complimentary color palette
	 * @param palette the {@link List} of {@link PaletteColor}s
	 * @return the score
	 *
	 * precondition: palette != null
	 */
	private static int getComplementaryScore(List<PaletteColor> palette) {
		assert palette != null;
		
		//complimentary colors should add up to 255, 255, 255 with a margin of error of 2%
		double total_red = 0;
		double total_green = 0;
		double total_blue = 0;
		
		//if both color sets have only 1
		for(PaletteColor color : palette) {
			ColorData color_data = new ColorData(color.getPrimaryColor());
			total_red+= color_data.getRed();
			total_green += color_data.getGreen();
			total_blue += color_data.getBlue();
		}
		
		int red_score = getComplimentaryColorScore(total_red);
		int blue_score = getComplimentaryColorScore(total_blue);
		int green_score = getComplimentaryColorScore(total_green);

		return (red_score + blue_score + green_score)/ 3;
	}

	/**
	 * Calculates a score for a color based on how well it adheres to a complimentary color palette
	 * @param color_val the value of the color
	 * @return the score
	 */
	private static int getComplimentaryColorScore(double color_val) {
		//test if each color is within a margin of error that is acceptable for complimentary colors
		int score = 0;
		if(color_val > 250 && color_val < 260) {
			score = 3;
		}
		else if(color_val > 245 && color_val < 265) {
			score = 2;
		}
		else if(color_val > 230 && color_val < 280) {
			score = 1;
		}
		return score;
	}

	/**
	 * Scores palette based on how well it adheres to a monochromatic color set
	 * @param palette the {@link List} of {@link PaletteColor}s
	 * @return the score
	 *
	 * precondition: palette != null
	 */
	private static int getMonochromaticScore(List<PaletteColor> palette) {
		assert palette != null;
		
		int tint_shade_tone_size = palette.get(0).getTintsShadesTones().size();
		int score = 0;
		if(tint_shade_tone_size == 2) {
			score = 3;
		}
		else if(tint_shade_tone_size <= 1) {
			score = 1;
		}
		else if(tint_shade_tone_size >= 3) {
			score = 2;
		}
		return score;
	}


	/**
	 * Extracts set of {@link PaletteColor colors} that define a palette based on a set of rgb strings
	 *
	 * @param colors the {@link List} of {@link ColorData}s
	 * @return the {@link List} of {@link PaletteColor}s
	 * 
	 * precondition: colors != null
	 */
	public static List<PaletteColor> extractPalette(List<ColorData> colors) {
		assert colors != null;
		
		//Group colors
		Set<Set<ColorData>> color_sets = groupColors(colors);
		Set<ColorData> primary_colors = identifyPrimaryColors(color_sets);
		List<PaletteColor> palette_colors = new ArrayList<>();
		
		for(ColorData color : primary_colors) {
			PaletteColor palette_color = new PaletteColor(color.rgb(), 
															color.getUsagePercent(), 
															new HashMap<>());
			palette_colors.add(palette_color);
		}
		
		return palette_colors;
	}
	
	/**
	 * Extracts set of {@link PaletteColor colors} that define a palette based on a set of rgb strings
	 *
	 * @param colors the {@link List} of {@link ColorData}s
	 * @return the {@link List} of {@link PaletteColor}s
	 *
	 * precondition: colors != null
	 */
	public static List<PaletteColor> extractColors(List<ColorData> colors) {
		assert colors != null;
		
		List<PaletteColor> palette_colors = new ArrayList<>();
		for(ColorData color : colors) {
			PaletteColor palette_color = new PaletteColor(color.rgb(),
															color.getUsagePercent(),
															new HashMap<>());
			palette_colors.add(palette_color);
		}
		
		return palette_colors;
	}
	
	/**
	 * Identifies the primary colors of a {@link Set} of {@link Set} of {@link ColorData}s
	 * @param color_sets the {@link Set} of {@link Set} of {@link ColorData}s
	 * @return the {@link Set} of {@link ColorData}s
	 *
	 * precondition: color_sets != null
	 */
	private static Set<ColorData> identifyPrimaryColors(Set<Set<ColorData>> color_sets) {
		assert color_sets != null;
		Set<ColorData> primary_colors = new HashSet<>();
		for(Set<ColorData> color_set : color_sets) {
			List<ColorData> color_list = new ArrayList<>(color_set);
			color_list.sort((o1, o2) -> Double.compare(o2.getUsagePercent(), o1.getUsagePercent()));

			primary_colors.add(color_list.get(0));
		}
		return primary_colors;
	}

	/**
	 * Identifies the primary colors of a {@link List} of {@link ColorData}s
	 * @param colors the {@link List} of {@link ColorData}s
	 * @return the {@link Set} of {@link ColorData}s
	 *
	 * precondition: colors != null
	 */
	public static Set<ColorData> identifyColorSet(List<ColorData> colors) {
		assert colors != null;
		
		log.warn("identifying primary colors ....  "+colors.size());
		ColorData largest_color = null;
		Set<ColorData> primary_colors = new HashSet<>();
		while(!colors.isEmpty()) {
			double percent = -5.0;

			for(ColorData color : colors) {
				if(percent < color.getUsagePercent()) {
					percent = color.getUsagePercent();
					largest_color = color;
				}
			}
			if(largest_color == null) {
				continue;
			}
			log.warn("identified largest color  ::   "+largest_color);

			primary_colors.add(largest_color);
			
			Set<ColorData> similar_colors = new HashSet<>();
			//remove any similar colors to primary color
			for(ColorData color : colors) {
				if(!color.equals(largest_color) && isSimilar(color, largest_color)) {
					similar_colors.add(color);
				}
			}
			
			colors.remove(largest_color);

			//remove similar colors from color set
			for(ColorData color : similar_colors) {
				colors.remove(color);
			}
		}
		return primary_colors;
	}

	/**
	 * Converts a map representing primary and secondary colors within a
	 * palette from using {@link ColorData} to {@link String}
	 *
	 * @param palette the {@link Map} of {@link ColorData} to {@link Set} of {@link ColorData}
	 * @return the {@link Map} of {@link String} to {@link Set} of {@link String}
	 *
	 * precondition: palette != null
	 */
	public static Map<String, Set<String>> convertPaletteToStringRepresentation(Map<ColorData, Set<ColorData>> palette) {
		assert palette != null;
		
		Map<String, Set<String>> stringified_map = new HashMap<>();
		for(ColorData primary : palette.keySet()) {
			Set<String> secondary_colors = new HashSet<>();
			for(ColorData secondary : palette.get(primary)) {
				if(secondary == null) {
					continue;
				}
				secondary_colors.add(secondary.rgb());
			}
			stringified_map.put(primary.rgb(), secondary_colors);
		}
		return stringified_map;
	}
	
	/**
	 * Groups {@link ColorData}s into sets of similar colors
	 * @param colors the {@link List} of {@link ColorData}s
	 * @return the {@link Set} of {@link Set} of {@link ColorData}s
	 *
	 * precondition: colors != null
	 */
	public static Set<Set<ColorData>> groupColors(List<ColorData> colors) {
		assert colors != null;
		
		Set<Set<ColorData>> color_sets = new HashSet<>();
		while(!colors.isEmpty()) {
			//initialize set for all similar colors
			Set<ColorData> similar_colors = new HashSet<>();
			
			//identify most frequent color
			ColorData most_frequent_color = colors.parallelStream().max(Comparator.comparing( ColorData::getUsagePercent)).get();
			similar_colors.add(most_frequent_color);
			//identify all similar colors and remove them from the colors set
			for(int idx=0; idx < colors.size(); idx++) {
				ColorData color = colors.get(idx);
				if(color.equals(most_frequent_color)) {
					continue;
				}
				//add similar colors to similar colors set
				if(isSimilarHue(most_frequent_color, color)) {	
					similar_colors.add( color );
				}
			}
			
			//remove similar colors from colors list
			colors.removeAll(similar_colors);
			color_sets.add(similar_colors);
		}
		
		return color_sets;
	}

	/**
	 * Checks if 2 colors are similar
	 * @param color1 the first {@link ColorData}
	 * @param color2 the second {@link ColorData}
	 * @return true if the colors are similar, otherwise false
	 *
	 * precondition: color1 != null
	 * precondition: color2 != null
	 */
	public static boolean isSimilar(ColorData color1, ColorData color2) {
		assert color1 != null;
		assert color2 != null;

		CIEColorSpace cie_color1 = color1.RGBtoXYZ().XYZtoCIE();
		CIEColorSpace cie_color2 = color2.RGBtoXYZ().XYZtoCIE();

		double l_square = Math.pow(Math.abs(cie_color1.l-cie_color2.l), 2);
		double a_square = Math.pow(Math.abs(cie_color1.a-cie_color2.a), 2);
		double b_square = Math.pow(Math.abs(cie_color1.b-cie_color2.b), 2);

		double diff = Math.sqrt( l_square + a_square + b_square);
		return (1/diff) >= 0.1;
	}

	/**
	 *	Checks if 2 colors are within 5 degrees
	 *
	 * @param color1 the first {@link ColorData}
	 * @param color2 the second {@link ColorData}
	 *
	 * @return true if the difference between the 2 hues is less 5 degrees, otherwise false
	 *
	 * precondition: color1 != null
	 * precondition: color2 != null
	 */
	public static boolean isSimilarHue(ColorData color1, ColorData color2) {
		assert color1 != null;
		assert color2 != null;
		
		if(isGrayScale(color1) && isGrayScale(color2)) {
			return true;
		}
		else if((isGrayScale(color1) && !isGrayScale(color2))
			|| (!isGrayScale(color1) && isGrayScale(color2)))
		{
			return false;
		}

		double hue_diff = Math.abs(color1.getHue() - color2.getHue());
		return hue_diff <= 10;
	}

	/**
	 * Checks if a {@link ColorData} is grayscale
	 * @param color the {@link ColorData}
	 * @return true if the color is grayscale, otherwise false
	 *
	 * precondition: color != null
	 */
	public static boolean isGrayScale(ColorData color) {
		assert color != null;
		
		return ((color.getSaturation() < 15 && color.getBrightness() > 25)
				|| (color.getBrightness() < 25));
	}

	/**
	 * Gets the maximum value of a {@link ColorData}
	 * @param color the {@link ColorData}
	 * @return the maximum value
	 *
	 * <b>Preconditions:</b>
	 * <ul>
	 *   <li>color != null</li>
	 * </ul>
	 */
	public static int getMax(ColorData color) {
		assert color != null;
		
		if(color.getRed() >= color.getBlue()
				&& color.getRed() >= color.getGreen()) {
			return color.getRed();
		}
		else if(color.getBlue() >= color.getRed()
				&& color.getBlue() >= color.getGreen()) {
			return color.getBlue();
		}
		
		return color.getGreen();
	}

	/**
	 * Gets the minimum value of a {@link ColorData}
	 * @param color the {@link ColorData}
	 * @return the minimum value
	 *
	 * <b>Preconditions:</b>
	 * <ul>
	 *   <li>color != null</li>
	 * </ul>
	 */
	public static int getMin(ColorData color) {
		assert color != null;

		if(color.getRed() <= color.getBlue()
				&& color.getRed() <= color.getGreen()) {
			return color.getRed();
		}
		else if(color.getBlue() <= color.getRed()
				&& color.getBlue() <= color.getGreen()) {
			return color.getBlue();
		}
		
		return color.getGreen();
	}

	/**
	 * Retrieves count of pages that have non text contrast issue
	 * 
	 * @param page_audits the list of {@link AuditRecord}s
	 * @param subcategory the subcategory of the audit
	 * @return the count of pages that have issues by subcategory
	 * 
	 * precondition: page_audits != null
	 * precondition: subcategory != null
	 */
	public static int getCountPagesWithSubcategoryIssues(List<AuditRecord> page_audits,
														AuditSubcategory subcategory) {
		assert page_audits != null;
		assert subcategory != null;
		
		int count_failing_pages = 0;
		for(AuditRecord page_audit : page_audits) {
			PageAuditRecord page_audit_record = (PageAuditRecord)page_audit;
			for(Audit audit: page_audit_record.getAudits()) {
				if(subcategory.equals( audit.getSubcategory() ) 
						&& audit.getPoints() < audit.getTotalPossiblePoints()) {
					count_failing_pages++;
					break;
				}
			}
		}
		
		return count_failing_pages;
	}

	/**
	 * Retrieves count of pages that have non text contrast issue
	 * 
	 * @param page_audits the list of {@link AuditRecord}s
	 * @param audit_name the name of the audit
	 * @return the count of pages that have issues by audit name
	 * 
	 * precondition: page_audits != null
	 * precondition: audit_name != null
	 */
	public static int getCountPagesWithIssuesByAuditName(List<AuditRecord> page_audits, AuditName audit_name) {
		assert page_audits != null;
		assert audit_name != null;
		
		int count_failing_pages = 0;
		
		for(AuditRecord page_audit : page_audits) {
			PageAuditRecord page_audit_record = (PageAuditRecord)page_audit;

			for(Audit audit: page_audit_record.getAudits()) {
				if(audit_name.equals( audit.getName() ) 
						&& audit.getPoints() < audit.getTotalPossiblePoints()) {
					count_failing_pages++;
					break;
				}
			}
		}
		
		return count_failing_pages;
	}

	/**
	 * Retrieves the count of pages that have WCAG compliance issues
	 * 
	 * @param page_audits the list of {@link AuditRecord}s
	 * @return the count of pages that have WCAG compliance issues
	 *
	 * precondition: page_audits != null
	 */
	public static int getCountOfPagesWithWcagComplianceIssues(List<AuditRecord> page_audits) {
		assert page_audits != null;
		
		int pages_with_issues = 0;
		
		for(AuditRecord audit_record : page_audits) {
			PageAuditRecord page_audit_record = (PageAuditRecord)audit_record;

			boolean has_issue = false;
			for(Audit audit: page_audit_record.getAudits()) {
				for(UXIssueMessage issue : audit.getMessages()) {
					if(issue.getLabels().contains("wcag") && issue.getPoints() < issue.getMaxPoints()) {
						pages_with_issues++;
						has_issue = true;
						break;
					}
				}
				if(has_issue) {
					break;
				}
			}
		}
		return pages_with_issues;
	}

	/**
	 * Calculates the overall score of based on audits that are based on WCAG standards
	 * The list of audits that are based on WCAG are those with the subcategories 
	 * TEXT_CONTRAST, NON-TEXT CONTRAST, LINKS, ALT_TEXT, PARAGRAPHING, READING_COMPLEXITY, TITLES
	 * 
	 * @param audits List of {@link Audit audits}
	 * @return overall score
	 */
	public static double calculateAccessibilityScore(Set<Audit> audits) {
		assert(audits != null);
		
		List<String> audit_names = new ArrayList<>();
		audit_names.add(AuditName.TEXT_BACKGROUND_CONTRAST.toString());
		audit_names.add(AuditName.NON_TEXT_BACKGROUND_CONTRAST.toString());
		audit_names.add(AuditName.LINKS.toString());
		audit_names.add(AuditName.ALT_TEXT.toString());
		audit_names.add(AuditName.PARAGRAPHING.toString());
		audit_names.add(AuditName.READING_COMPLEXITY.toString());
		audit_names.add(AuditName.TITLES.toString());

		//filter audits based on subcategory
		List<Audit> list1 = audits.parallelStream()
										.filter(audit -> audit_names.contains(audit.getName().toString()))
										.filter(audit -> audit.getTotalPossiblePoints() != 0)
										.collect(Collectors.toList());

		DoubleSummaryStatistics score_stats = list1.parallelStream()
													.mapToDouble(audit -> (audit.getPoints()/(double)audit.getTotalPossiblePoints())*100)
													.summaryStatistics();

		return score_stats.getAverage();
	}
}