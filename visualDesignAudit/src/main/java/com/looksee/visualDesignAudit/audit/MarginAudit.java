package com.looksee.visualDesignAudit.audit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
 * Responsible for executing an audit on the margins used within a page state as part of  the 
 * 	information architecture audit category
 */
@Component
public class MarginAudit implements IExecutablePageStateAudit {
	private static Logger log = LoggerFactory.getLogger(MarginAudit.class);

	private String[] size_units = {"px", "pt", "%", "em", "rem", "ex", "vh", "vw", "vmax", "vmin", "mm", "cm", "in", "pc"};
	
	@Autowired
	private PageStateService page_state_service;

	
	public MarginAudit() {	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * Identifies colors used on page, the color scheme type used, and the ultimately the score for how the colors used conform to scheme
	 *  
	 * @throws MalformedURLException 
	 * @throws URISyntaxException 
	 */
	@Override
	public Audit execute(PageState page_state, AuditRecord audit_record, DesignSystem design_system) {
		assert page_state != null;

		Set<UXIssueMessage> issue_messages = new HashSet<>();
		Map<ElementState, List<String>> elements_margin_map = new HashMap<>(); 

		//get all pages
		//get most recent page state for each page
		
		List<ElementState> elements = page_state_service.getElementStates(page_state.getKey());
		log.warn("page state elements for domain audit :: "+elements.size());
		for(ElementState element : elements) {
			String margin_value = "";
			List<String> margins = new ArrayList<>();

			if( element.getRenderedCssValues().containsKey("margin-top")) {
				margin_value = element.getRenderedCssValues().get("margin-top").trim();
				if(!margin_value.contentEquals("0px")) {
					margins.add(margin_value);
				}
			}

			if( element.getRenderedCssValues().containsKey("margin-bottom")) {
				margin_value = element.getRenderedCssValues().get("margin-bottom").trim();
				if(!margin_value.contentEquals("0px")) {
					margins.add(margin_value);
				}			}
			
			if( element.getRenderedCssValues().containsKey("margin-right")) {
				margin_value = element.getRenderedCssValues().get("margin-right").trim();
				if(!margin_value.contentEquals("0px")) {
					margins.add(margin_value);
				}			}
			
			if( element.getRenderedCssValues().containsKey("margin-left")) {
				margin_value = element.getRenderedCssValues().get("margin-left").trim();
				if(!margin_value.contentEquals("0px")) {
					margins.add(margin_value);
				}			}
			
			elements_margin_map.put(element, margins);
		}

		

			
		log.warn("Element margin map size :: "+elements_margin_map.size());
		// Score spacing_score = evaluateSpacingConsistency(elements_margin_map);     //commented out because this is old greatest common divisor methodology
		Score spacing_score = evaluateSpacingMultipleOf8(elements_margin_map);
		//Score unit_score = evaluateUnits(elements_margin_map);

		Score margin_as_padding_score = scoreMarginAsPadding(elements_margin_map.keySet());
		
		issue_messages.addAll(spacing_score.getIssueMessages());
		//observations.addAll(unit_score.getObservations());
		issue_messages.addAll(margin_as_padding_score.getIssueMessages());
		
		log.warn("spacing score : "+spacing_score.getPointsAchieved() + " / " +spacing_score.getMaxPossiblePoints());
		//log.warn("unit score : "+spacing_score.getPointsAchieved() + " / " +spacing_score.getMaxPossiblePoints());
		log.warn("margin as padding score : "+margin_as_padding_score.getPointsAchieved() + " / " +margin_as_padding_score.getMaxPossiblePoints());

		int points = spacing_score.getPointsAchieved() + margin_as_padding_score.getPointsAchieved();
		int max_points = spacing_score.getMaxPossiblePoints() + margin_as_padding_score.getMaxPossiblePoints();

		return new Audit(AuditCategory.AESTHETICS,
						AuditSubcategory.WHITESPACE,
						AuditName.MARGIN,
						points,
						issue_messages,
						AuditLevel.PAGE,
						max_points,
						page_state.getUrl(),
						"",
						"",
						false);
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
		Set<UXIssueMessage> issue_messages = new HashSet<>();
		
		for(ElementState element : elements_margins.keySet()) {
			for(String size_str : elements_margins.get(element)) {
				if(isMultipleOf8(size_str)) {
					points_earned += 1;
					String title = "Element margins are multiple of 8";
					String description = "All margins for element are a mutliple of 8";
					Set<String> labels = new HashSet<>();
					labels.add("whitespace");
					
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
					Set<String> labels = new HashSet<>();
					labels.add("whitespace");
					
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
		

		String why_it_matters = "Keeping your use of margins to a miminum, and when you use them making sure"
				+ " the margin values are a multiple of 8 dpi ensures your site is more responsive. Not all users"
				+ " have screens that are the same size as those used by the design team, but all monitor sizes"
				+ " are multiple of 8.";
		
		String ada_compliance = "There are no ADA requirements for use of margins";
		Set<String> recommendations = new HashSet<>();
		recommendations.add("For a responsive design we recommend using margin values that are a multiple of 8.");
		
		Set<String> labels = new HashSet<>();
		labels.add("whitespace");
		
		Set<String> categories = new HashSet<>();
		categories.add(AuditCategory.AESTHETICS.toString());
		
		//observations.add(new ElementStateObservation(elements, "Margin values are multiple of 8"));
		
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
	private Score evaluateSpacingAppliedEvenly(Map<ElementState, List<String>> elements_margins) {
		assert elements_margins != null;
		
		int points_earned = 0;
		int max_points = 0;
		Set<UXIssueMessage> element_issues = new HashSet<>();
		
		Set<String> labels = new HashSet<>();
		labels.add("whitespace");
		
		for(ElementState element : elements_margins.keySet()) {
			for(String size_str : elements_margins.get(element)) {
				if(isMultipleOf8(size_str)) {
					points_earned += 1;
					String title = "Has at least one margin value that isn't a multiple of 8.";
					String description = title;
					ElementStateIssueMessage element_issue = new ElementStateIssueMessage(
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
					element_issues.add(element_issue);
				}
				//else create observation that element is unlikely to scale gracefully
				else {
					String title = "Has at least one margin value that isn't a multiple of 8.";
					String description = title;
					ElementStateIssueMessage element_issue = new ElementStateIssueMessage(
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
					element_issues.add(element_issue);
				}
				max_points++;
			}
		}
		

		String why_it_matters = "Keeping your use of margins to a miminum, and when you use them making sure"
				+ " the margin values are a multiple of 8 dpi ensures your site is more responsive. Not all users"
				+ " have screens that are the same size as those used by the design team, but all monitor sizes"
				+ " are multiple of 8.";
		
		String ada_compliance = "There are no ADA requirements for use of margins";
		Set<String> recommendations = new HashSet<>();
		recommendations.add("For a responsive design we recommend using margin values that are a multiple of 8.");
		
		Set<String> categories = new HashSet<>();
		categories.add(AuditCategory.AESTHETICS.toString());
		

		//observations.add(new ElementStateObservation(elements, "Margin values are multiple of 8"));
		
		return new Score(points_earned, max_points, element_issues);
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
	 * @param vertical_margin_values
	 * 
	 * @return
	 */
	private Score evaluateUnits(Map<ElementState, List<String>> element_margin_map) {
		assert element_margin_map != null;
		
		int vertical_score = 0;
		int max_vertical_score = 0;
		Set<UXIssueMessage> element_issues = new HashSet<>();

		Set<String> labels = new HashSet<>();
		labels.add("responsiveness");
		labels.add("whitespace");
		
		for(ElementState element : element_margin_map.keySet()) {
			for(String margin_value : element_margin_map.get(element)) {
				//determine unit measure
				String unit = extractMeasureUnit(margin_value);
				
				vertical_score += scoreMeasureUnit(unit);
				max_vertical_score += 3;
				
				if(vertical_score < 1) {
					String description = "Unscalable margin units";
					String title = "Unscalable margin units";

					ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																	Priority.MEDIUM,
																	description,
																	"Elements with unscalable margin units",
																	element,
																	AuditCategory.AESTHETICS,
																	labels,
																	"",
																	title,
																	0,
																	1);
					element_issues.add(issue_message);
				}
				else {
					String description = "Correct use of scalable margin units";
					String title = "Correct use of scalable margin units";

					ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																	Priority.MEDIUM,
																	description,
																	"Elements with unscalable margin units",
																	element,
																	AuditCategory.AESTHETICS,
																	labels,
																	"",
																	title,
																	1,
																	1);
					element_issues.add(issue_message);
				}
			}
		}
		
		return new Score(vertical_score, max_vertical_score, element_issues);
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
	
	
	/**
	 * Identifies elements that are using margin when they should be using margin
	 * 
	 * @param elements
	 * 
	 * @return 
	 * 
	 * @pre elements != null
	 */
	private Score scoreMarginAsPadding(Set<ElementState> elements) {
		assert elements != null;
		
		int score = 0;
		int max_score = 0;
		
		Set<String> labels = new HashSet<>();
		labels.add("whitespace");
		
		Set<UXIssueMessage> element_issues = new HashSet<>();
		for(ElementState element : elements) {
			if(element == null) {
				log.warn("margin padding audit Element :: "+element);
				continue;
			}

			//identify elements that own text and have margin but not padding set
			if(element.getOwnedText() != null && !element.getOwnedText().trim().isEmpty()) {
				//check if element has margin but not padding set for any direction(top, bottom, left, right)
				boolean margin_used_as_padding = false;
				String margin_top = element.getRenderedCssValues().get("margin-top");
				if(!isSpacingValueZero(margin_top) && isSpacingValueZero(element.getRenderedCssValues().get("padding-top"))) {
					log.warn("margin top : "+margin_top+";      padding-top  :  "+element.getRenderedCssValues().get("padding-top"));
					margin_used_as_padding = true;
				}
				else if(!isSpacingValueZero(element.getRenderedCssValues().get("margin-right")) && isSpacingValueZero(element.getRenderedCssValues().get("padding-right"))) {
					log.warn("margin right : "+element.getRenderedCssValues().get("margin-right")+";      padding-right  :  "+element.getRenderedCssValues().get("padding-right"));

					margin_used_as_padding = true;
				}
				else if(!isSpacingValueZero(element.getRenderedCssValues().get("margin-bottom")) && isSpacingValueZero(element.getRenderedCssValues().get("padding-bottom"))) {
					log.warn("margin bottom : "+element.getRenderedCssValues().get("margin-bottom")+";      padding-bottom  :  "+element.getRenderedCssValues().get("padding-bottom"));

					margin_used_as_padding = true;
				}
				else if(!isSpacingValueZero(element.getRenderedCssValues().get("margin-left")) && isSpacingValueZero(element.getRenderedCssValues().get("padding-left"))) {
					log.warn("margin left : "+element.getRenderedCssValues().get("margin-left")+";      padding-left  :  "+element.getRenderedCssValues().get("padding-left"));

					margin_used_as_padding = true;
				}
				else {
					score += 3;
				}
				
				if(margin_used_as_padding) {
					String title = "Margin used as padding";
					String description = "Margin used as padding";

					ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																	Priority.MEDIUM,
																	description,
																	"Elements that appear to use margin as padding",
																	element,
																	AuditCategory.AESTHETICS,
																	labels,
																	"",
																	title,
																	0,
																	1);
					element_issues.add(issue_message);
				}
				else {
					score += 1;
					String title = "Margin used as padding";
					String description = "Margin used as padding";

					ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																	Priority.MEDIUM,
																	description,
																	"Elements that appear to use margin as padding",
																	element,
																	AuditCategory.AESTHETICS,
																	labels,
																	"",
																	title,
																	1,
																	1);
					element_issues.add(issue_message);
				}
				max_score += 3;
			}
		}
		if(!element_issues.isEmpty()) {

			String why_it_matters = "Keeping your use of margins to a miminum, and when you use them making sure"
					+ " the margin values are a multiple of 8 dpi ensures your site is more responsive. Not all users"
					+ " have screens that are the same size as those used by the design team, but all monitor sizes"
					+ " are multiple of 8.";
			
			String ada_compliance = "There are no ADA requirements for use of margins";
			
			Set<String> categories = new HashSet<>();
			categories.add(AuditCategory.AESTHETICS.toString());
		}
		return new Score(score, max_score, element_issues);
	}
	
	private boolean isSpacingValueZero(String spacing) {
		spacing = cleanSizeUnits(spacing);
		return spacing == null || ( !spacing.isEmpty() || !spacing.equals("0") || !spacing.equals("auto"));
	}

	/**
	 * TODO
	 * 
	 * @param elements
	 * @return
	 */
	private int scoreNonCollapsingMargins(List<Element> elements) {
		for(Element element : elements) {
			//identify situations of margin collapse by finding elements that are 
				//positioned vertically where 1 is above the other and both have margins
				//element is empty and has margins set
				//first and last child margin collapse when their parent has margin set
			
			
		}
		
		return 0;
		
	}

	public static String URLReader(URL url) throws IOException {
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        
        log.warn("Content encoding for URL connection ::  " + con.getContentEncoding());
        if(con.getContentEncoding() != null && con.getContentEncoding().equalsIgnoreCase("gzip")) {
        	return readGzipStream(con.getInputStream());
        }
        else {
        	return readStream(con.getInputStream());
        }
	}
	
	private static String readGzipStream(InputStream inputStream) {
		 StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream( inputStream )));) {
            String nextLine = "";
            while ((nextLine = reader.readLine()) != null) {
                sb.append(nextLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
	}

	private static String readStream(InputStream in) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in));) {
            String nextLine = "";
            while ((nextLine = reader.readLine()) != null) {
                sb.append(nextLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }


	/**
	 * Sort units into buckets by mapping unit type to margin sizes
	 * 
	 * @param margin_set
	 * @return
	 */
	private Map<String, List<Double>> sortSizeUnits(List<String> margin_set) {
		Map<String, List<Double>> sorted_margins = new HashMap<>();
		//replace all px values with em for values that contain decimals
		
		for(String margin_value : margin_set) {
			if(margin_value == null 
					|| "0".equals(margin_value.trim()) 
					|| margin_value.contains("auto")) {
				continue;
			}
			
			for(String unit : size_units) {
				if(margin_value != null && margin_value.contains(unit)) {
					List<Double> values = new ArrayList<Double>();

					if(sorted_margins.containsKey(unit)) {
						values = sorted_margins.get(unit);
					}
					
					String value = cleanSizeUnits(margin_value);
					
					//values = cleanSizeUnits(values);
					//List<Double> converted_values = convertList(values, s -> Double.parseDouble(s));
					values.add(Double.parseDouble(value));
					sorted_margins.put(unit, values);
				}
			}
		}
		return sorted_margins;
	}
	
	public static List<Double> sortAndMakeDistinct(List<Double> from){
		return from.stream().filter(n -> n != 0.0).distinct().sorted().collect(Collectors.toList());
	}
	
	public static List<String> cleanSizeUnits(List<String> from){
		return from.stream()
				.map(line -> line.replaceAll("px", ""))
				.map(line -> line.replaceAll("%", ""))
				.map(line -> line.replaceAll("em", ""))
				.map(line -> line.replaceAll("rem", ""))
				.map(line -> line.replaceAll("pt", ""))
				.map(line -> line.replaceAll("ex", ""))
				.map(line -> line.replaceAll("vm", ""))
				.map(line -> line.replaceAll("vh", ""))
				.map(line -> line.replaceAll("cm", ""))
				.map(line -> line.replaceAll("mm", ""))
				.map(line -> line.replaceAll("in", ""))
				.map(line -> line.replaceAll("pc", ""))
				.map(line -> line.indexOf(".") > -1 ? line.substring(0, line.indexOf(".")) : line)
				.collect(Collectors.toList());
	}
	
	public static String cleanSizeUnits(String value){
		return value.replaceAll("px", "")
					.replaceAll("%", "")
					.replaceAll("em", "")
					.replaceAll("rem", "")
					.replaceAll("pt", "")
					.replaceAll("ex", "")
					.replaceAll("vm", "")
					.replaceAll("vh", "")
					.replaceAll("cm", "")
					.replaceAll("mm", "")
					.replaceAll("in", "")
					.replaceAll("pc", "")
					.replaceAll("auto", "")
					.replaceAll("!important", "")
					.trim();
	}
	
	public static List<Integer> removeZeroValues(List<Integer> from){
		return from.stream().filter(n -> n != 0).collect(Collectors.toList());
	}
	
	//for lists
	public static <T, U> List<U> convertList(List<T> from, Function<T, U> func) {
	    return from.stream().map(func).collect(Collectors.toList());
	}
	
	/* * Java method to find GCD of two number using Euclid's method * @return GDC of two numbers in Java */ 
	private static double findGCD(double number1, double number2) { 
		//base case 
		if(number2 == 0){ 
			return number1; 
		} 
		return findGCD(number2, number1%number2);
	}
	
	private int scoreMeasureUnit(String unit) {
		if(unit.contains("rem") || unit.contains("em") || unit.contains("%") ){
			return 2;
		}
		else if(unit.contains("vh") || unit.contains("vw") || unit.contains("vmin") || unit.contains("vmax")) {
			return 1;
		}
		else if(unit.contains("px") || unit.contains("ex") || unit.contains("pt") || unit.contains("cm") || unit.contains("mm") || unit.contains("in") || unit.contains("pc")) {
			return 0;
		}
		
		return 2;
	}
	
	public static List<Double> deflateGCD(List<Double> gcd_list){
		return gcd_list.stream().map(s -> s/100.0).distinct().sorted().collect(Collectors.toList());
	}
}