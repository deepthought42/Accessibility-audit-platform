package com.looksee.visualDesignAudit.audit;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.looksee.models.Domain;
import com.looksee.models.Element;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.Score;
import com.looksee.models.audit.interfaces.IExecutablePageStateAudit;
import com.looksee.models.audit.messages.ElementStateIssueMessage;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.models.enums.Priority;
import com.looksee.services.PageStateService;



/**
 * Responsible for executing an audit on the padding consistency across a {@link Domain} as part of the information architecture audit category
 */
@Component
public class PaddingAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(PaddingAudit.class);
	
	private static final String[] SIZE_UNITS = {"px", "pt", "%", "em", "rem", "ex", "vh", "vw", "vmax", "vmin", "mm", "cm", "in", "pc"};
	
	@Autowired
	private PageStateService page_state_service;

	
	
	public PaddingAudit() {	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * Identifies colors used on page, the color scheme type used, and the ultimately the score for how the colors used conform to scheme
	 *  
	 * @throws MalformedURLException 
	 * @throws URISyntaxException 
	 */
	@Override
	public Audit execute(PageState page, AuditRecord audit_record, DesignSystem design_system) {
		assert page != null;

		
		Set<UXIssueMessage> issue_messages = new HashSet<>();
		Map<ElementState, List<String>> elements_padding_map = new HashMap<>();
		
		//get all pages
	
		//for each page state get elements
		//PageState page_state = page_service.getMostRecentPageState(page.getKey());
		
		List<ElementState> elements = page_state_service.getElementStates(page.getKey());
		log.warn("page state elements for domain audit :: "+elements.size());
		for(ElementState element : elements) {
			//TODO put element padding evaluation logic here
			String padding_value = "";
			List<String> paddings = new ArrayList<>();

			if( element.getRenderedCssValues().containsKey("padding-top")) {
				padding_value = element.getRenderedCssValues().get("padding-top");
				paddings.addAll(Arrays.asList(padding_value.split(" ")));

			}
			
			if( element.getRenderedCssValues().containsKey("padding-bottom")) {
				padding_value = element.getRenderedCssValues().get("padding-bottom");
				paddings.addAll(Arrays.asList(padding_value.split(" ")));

			}
			
			if( element.getRenderedCssValues().containsKey("padding-right")) {
				padding_value = element.getRenderedCssValues().get("padding-right");
				paddings.addAll(Arrays.asList(padding_value.split(" ")));
			}
			
			if( element.getRenderedCssValues().containsKey("padding-left")) {
				padding_value = element.getRenderedCssValues().get("padding-left");
				paddings.addAll(Arrays.asList(padding_value.split(" ")));
			}	
			
			elements_padding_map.put(element, paddings);
		}
		
		
//		Score spacing_score = evaluateSpacingConsistency(elements_padding_map);
		Score spacing_score = evaluateSpacingMultipleOf8(elements_padding_map);

		//Score unit_score = evaluateUnits(elements_padding_map);

		issue_messages.addAll(spacing_score.getIssueMessages());
//		observations.addAll(unit_score.getObservations());
		
		int points = spacing_score.getPointsAchieved();
		int max_points = spacing_score.getMaxPossiblePoints();
		
		//calculate score for question "Is padding used as padding?" NOTE: The expected calculation expects that paddings are not used as padding
		log.warn("PADDING SCORE  :::   "+ (spacing_score.getPointsAchieved()) + " / " + (spacing_score.getMaxPossiblePoints() ) );	

		
		String why_it_matters = "Keeping your use of paddings to a miminum, and when you use them making sure you"
				+ " the padding values are a multiple of 8 dpi ensures your site is more responsive. Not all users"
				+ " have screens that are the same size as those used by the design team, but all monitor sizes"
				+ " are multiple of 8.";
		
		String description = "Evaluation of how whitespace is used";
		
    	Set<String> labels = new HashSet<>();
    	labels.add(AuditSubcategory.WHITESPACE.getShortName());
    	
    	Set<String> categories = new HashSet<>();
    	categories.add(AuditCategory.AESTHETICS.toString());
		

		return new Audit(AuditCategory.AESTHETICS,
						 AuditSubcategory.WHITESPACE,
						 AuditName.PADDING,
						 points,
						 issue_messages,
						 AuditLevel.PAGE,
						 max_points,
						 page.getUrl(),
						 why_it_matters,
						 description,
						 false);
	}

	private Score evaluateSpacingAdherenceToBaseValue(Map<Element, List<String>> elements_padding_map) {
		//extract baseline padding values
		
		//if no baseline exists then score 0 out of 3
		//check if baseline is a multiple of 4 or 8
		
		
		//if multiple of 8 give 3 out of 3
		//if multiple of 4 give 2 out of 3
		//else give score of 1 out of 3
		
		//check if other padding values are a multiple of the baseline
		
		return new Score(0, 0, new HashSet<>());
	}
	
	/**
	 * Generates {@link Score score} for spacing consistency across elements
	 * 
	 * @param elements_padding_map
	 * 
	 * @return {@link Score score}
	 * 
	 * @pre elements_padding_map != null
	 */
	public Score evaluateSpacingConsistency(Map<ElementState, List<String>> elements_padding_map) {
		assert elements_padding_map != null;
		
		int points_earned = 0;
		int max_points = 0;
		Set<UXIssueMessage> issue_messages = new HashSet<>();
		
		Map<String, List<Double>> gcd_map = new HashMap<>();
		Map<String, List<Double>> units = new HashMap<>();
		for(ElementState element : elements_padding_map.keySet()) {
			//START UNIT SCORE HERE
			units.putAll(sortSizeUnits(elements_padding_map.get(element)));
		}
		
		//extract multiples for paddings
		//most common multiples, the highest multiples that can be found to satisfy the list of unique padding values
		for(String unit : units.keySet()) {
			//scale units values by 100 and make unique
			List<Double> distinct_list =  sortAndMakeDistinct(units.get(unit));
			
			if(distinct_list.size() == 1) {
				gcd_map.put(unit, distinct_list);
				continue;
			}
			
			List<Double> gcd_list = new ArrayList<>();
			for(int idx = 0; idx < distinct_list.size()-1; idx++) {
				for(int idx2 = idx+1; idx2 < distinct_list.size(); idx2++) {
					gcd_list.add(findGCD(distinct_list.get(idx), distinct_list.get(idx2)));
				}
			}
			
			gcd_list.remove(new Double(1));
			//reduce gcd again.
			gcd_map.put(unit, gcd_list);
		}			
		
		log.warn("GCD MAP VALUES ::   "+gcd_map);
		//reduce gcd_list until no value is divisible by any other
		//rank gcd list based on frequency values that are multiples of gcd
		//generate score for each element padding based on gcd divisibility
		
		//COMPUTE SCORE FOR PADDING BASED ON GCD VALUES
		Map<String, List<Double>> unit_gcd_lists = new HashMap<>();
		for(String unit : gcd_map.keySet()) {
			List<Double> most_common_gcd_values = new ArrayList<>();
			if(gcd_map.get(unit).size() == 1) {
				log.warn("unit : "+unit+"  has only 1 gcd!!");
				points_earned += 3;
				most_common_gcd_values.addAll(gcd_map.get(unit));
			}
			else {
				List<Double> padding_list = units.get(unit);
				List<Double> gcd_values = gcd_map.get(unit);
				do {
					Map<Double, List<Double>> gcd_match_lists = new HashMap<>();
					
					//find highest gcd values that define the set
					for(double gcd : gcd_values) {
						gcd_match_lists.put(gcd, new ArrayList<Double>());
						for(double value : padding_list) {
							if(value % gcd == 0 && gcd != 1){
								gcd_match_lists.get(gcd).add(value);
							}
						}
					}
					
					//identify gcd with most matches
					int largest_gcd_count = 0;
					double largest_gcd = 0;
					for(Double gcd : gcd_match_lists.keySet()) {
						if(gcd_match_lists.get(gcd).size() >= largest_gcd_count ) {
							largest_gcd_count = gcd_match_lists.get(gcd).size();
							
							if(gcd > largest_gcd) {
								largest_gcd = gcd;
							}
						}
					}
					
					//remove gcd value from input gcd list
					gcd_values.remove(largest_gcd);
					
					if(largest_gcd_count > 0) {						
						//add the largest gcd to the list of most applicable gcd values
						most_common_gcd_values.add(largest_gcd);
					}
					
					//remove gcd matches from vertical padding list
					List<Double> largest_gcd_matches = gcd_match_lists.get(largest_gcd);
					if(largest_gcd_matches != null) {
						padding_list.removeAll(largest_gcd_matches);
					}
					
				}while(!padding_list.isEmpty() && !gcd_values.isEmpty());
				
				if(most_common_gcd_values.size() == 2) {
					points_earned += 2;
				}
				else {
					points_earned += 1;
				}
			}
			unit_gcd_lists.put(unit, most_common_gcd_values);
			max_points += 3;
		}

		return new Score(points_earned, max_points, issue_messages);
	}

	/**
	 * Generates {@link Score score} for spacing consistency across elements
	 * 
	 * @param elements_margin_map
	 * 
	 * @return {@link Score score}
	 * 
	 * @pre elements_margin_map != null
	 */
	private Score evaluateSpacingMultipleOf8(Map<ElementState, List<String>> elements_margins) {
		assert elements_margins != null;
		
		int points_earned = 0;
		int max_points = 0;
		String ada_compliance = "There are no ADA requirements for use of padding";

		Set<UXIssueMessage> issue_messages = new HashSet<>();
		Set<String> labels = new HashSet<>();
		labels.add("whitespace");
		
		for(ElementState element : elements_margins.keySet()) {
			for(String size_str : elements_margins.get(element)) {
				if(isMultipleOf8(size_str)) {
					points_earned += 1;
					String title = "All margins for element are multiple of 8.";
					String description = "All margins for element are multiple of 8.";
					
					ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
							Priority.MEDIUM,
							description,
							"For best responsiveness make sure margin values are a multiple of 8.",
							element,
							AuditCategory.AESTHETICS,
							labels,
							"",
							title,
							1,
							1);
					issue_messages.add(issue_message);
				}
				//else create observation that element is unlikely to scale gracefully
				else {
					String title = "At least one margin value isn't a multiple of 8.";
					String description = "At least one margin value isn't a multiple of 8.";

					ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
							Priority.MEDIUM,
							description,
							"For best responsiveness make sure margin values are a multiple of 8.",
							element,
							AuditCategory.AESTHETICS,
							labels,
							"",
							title,
							0,
							1);
					issue_messages.add(issue_message);
				}
				max_points++;
			}
		}
		
		
		
		return new Score(points_earned, max_points, issue_messages);
	}
	
	public static boolean isMultipleOf8(String size_str) {
		double size = Double.parseDouble(cleanSizeUnits(size_str));
		if(size == 0.0) {
			return true;
		}
		//check if size is a multiple of 8
		int remainder = 0;
		if(size > 8) {
			remainder = (int)size % 8;
		}
		else {
			remainder = 8 % (int)size;
		}
		//if multiple of 8 then note as well done
		if(remainder <=1 ) {
			return true;
		}
		
		return false;
	}

	
	/**
	 * Generates {@link Score score} based on which units (ie, %, em, rem, px, pt, etc.) are used for vertical(top,bottom) padding
	 * 
	 * @param vertical_padding_values
	 * 
	 * @return
	 */
	private Score evaluateUnits(Map<ElementState, List<String>> element_padding_map) {
		assert element_padding_map != null;
		
		int points_earned = 0;
		int max_vertical_score = 0;
		Set<UXIssueMessage> issue_messages = new HashSet<>();
		String ada_compliance = "There are no ADA requirements for use of padding";
		Set<String> labels = new HashSet<>();
		labels.add("whitespace");
		labels.add("responsiveness");
		
		for(ElementState element : element_padding_map.keySet()) {
			for(String padding_value : element_padding_map.get(element)) {
				//determine unit measure
				String unit = extractMeasureUnit(padding_value);
				
				points_earned += scoreMeasureUnit(unit);
				max_vertical_score += 3;
				
				if(points_earned < 2) {
					String title = "At least one margin value isn't a multiple of 8.";
					String description = "At least one margin value isn't a multiple of 8.";
					
					ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
							Priority.MEDIUM,
							description,
							"For best responsiveness make sure margin values are a multiple of 8.",
							element,
							AuditCategory.AESTHETICS,
							labels,
							ada_compliance,
							title,
							0,
							1);
					issue_messages.add(issue_message);
				}
				else {
					String title = "All margins for element are multiple of 8";
					String description = "All margins for element are multiple of 8.";
					
					ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
							Priority.MEDIUM,
							description,
							"For best responsiveness make sure margin values are a multiple of 8.",
							element,
							AuditCategory.AESTHETICS,
							labels,
							"",
							title,
							0,
							1);
					issue_messages.add(issue_message);
				}
			}
		}
		
		return new Score(points_earned, max_vertical_score, issue_messages);
	}
	
	
	private String extractMeasureUnit(String padding_value) {
		if(padding_value.contains("rem")) {
			return "rem";
		}
		else if( padding_value.contains("em")) {
			return "em";
		}
		else if( padding_value.contains("%") ){
			return "%";
		}
		else if(padding_value.contains("vh")) {
			return "vh";
		}
		else if(padding_value.contains("vw") ) {
			return "vw";
		}
		else if(padding_value.contains("vmin")) {
			return "vmin";
		}
		else if(padding_value.contains("vmax")) {
			return "vmax";
		}
		else if(padding_value.contains("px")) {
			return "px";
		}
		else if(padding_value.contains("ex") ) {
			return "ex";
		}
		else if(padding_value.contains("pt")) {
			return "pt";
		}
		else if(padding_value.contains("cm")) {
			return "cm";
		}
		else if(padding_value.contains("mm")) {
			return "mm";
		}
		else if(padding_value.contains("in")) {
			return "in";
		}
		else if(padding_value.contains("pc")) {
			return "pc";
		}
		
		return "";
	}
	
	private int scoreMeasureUnit(String unit) {
		if(unit.contains("rem") || unit.contains("em") || unit.contains("%") ){
			return 3;
		}
		else if(unit.contains("vh") || unit.contains("vw") || unit.contains("vmin") || unit.contains("vmax")) {
			return 2;
		}
		else if(unit.contains("px") || unit.contains("ex") || unit.contains("pt") ) {
			return 1;
		}
		else if(unit.contains("cm") || unit.contains("mm") || unit.contains("in") || unit.contains("pc")) {
			return 0;
		}
		return 3;
	}

	/**
	 * Sort units into buckets by mapping unit type to padding sizes
	 * 
	 * @param padding_set
	 * @return
	 */
	private Map<String, List<Double>> sortSizeUnits(List<String> padding_set) {
		Map<String, List<Double>> sorted_paddings = new HashMap<>();
		
		for(String padding_value : padding_set) {
			if(padding_value == null 
					|| "0".equals(padding_value.trim()) 
					|| padding_value.contains("auto")) {
				continue;
			}
			
			for(String unit : SIZE_UNITS) {
				if(padding_value != null && padding_value.contains(unit)) {
					List<Double> values = new ArrayList<>();

					if(sorted_paddings.containsKey(unit)) {
						values = sorted_paddings.get(unit);
					}
					
					String value = cleanSizeUnits(padding_value);
					values.add(Double.parseDouble(value));
					sorted_paddings.put(unit, values);
				}
			}
		}
		return sorted_paddings;
	}
	
	private static List<Double> sortAndMakeDistinct(List<Double> from){
		return from.stream().filter(n -> n != 0).map(s -> s*100).distinct().sorted().collect(Collectors.toList());
	}
	
	private static String cleanSizeUnits(String value){
		return value.replace("!important", "")
					.replaceAll("px", "")
					.replaceAll("%", "")
					.replaceAll("em", "")
					.replaceAll("rem", "")
					.replaceAll("pt", "")
					.replaceAll("ex", "")
					.replaceAll("vw", "")
					.replaceAll("vh", "")
					.replaceAll("cm", "")
					.replaceAll("mm", "")
					.replaceAll("in", "")
					.replaceAll("pc", "");
					
	}
	
	/* * Java method to find GCD of two number using Euclid's method * @return GDC of two numbers in Java */ 
	private static double findGCD(double number1, double number2) { 
		//base case 
		if(number2 == 0){ 
			return number1; 
		} 
		return findGCD(number2, number1%number2);
	}
}