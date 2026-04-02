package com.looksee.visualDesignAudit.audit;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.looksee.models.ColorData;
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
import com.looksee.services.AuditService;
import com.looksee.services.UXIssueMessageService;
import com.looksee.utils.ColorPaletteUtils;



/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class ColorPaletteAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(ColorPaletteAudit.class);
	
	@Autowired
	private AuditService audit_service;
	
	@Autowired
	private UXIssueMessageService ux_issue_service;
		
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
		
		String why_it_matters = "Studies have found that it takes 90 seconds for a customer to form an" + 
				" opinion on a product. 62–90% of that interaction is determined by the" + 
				" color of the product alone." + 
				" Color impacts how a user feels when they interact with your website; it is" + 
				" key to their experience. The right usage of colors can brighten a website" + 
				" and communicates the tone of your brand. Furthermore, using your brand" + 
				" colors consistently makes the website appear cohesive and collected," + 
				" while creating a sense of familiarity for the user.";
		String audit_description = "Reviews the palette defined in the design system settings against the colors"
				+ " on a webpage to ensure that the webpage doesn't use colors that aren't part of the design system.";

		/*
		Map<ColorUsageStat, Boolean> gray_colors = new HashMap<ColorUsageStat, Boolean>();
		Map<ColorUsageStat, Boolean> filtered_colors = new HashMap<>();
		//discard any colors that are transparent
		for(ColorUsageStat color: color_usage_list) {
			String rgb_color_str = "rgb("+color.getRed()+","+color.getGreen()+","+color.getBlue()+")";
			//convert rgb to hsl, store all as Color object
			color_map.put(rgb_color_str, new ColorData(rgb_color_str));
			if( Math.abs(color.getRed() - color.getGreen()) < 4
					&& Math.abs(color.getRed() - color.getBlue()) < 4
					&& Math.abs(color.getBlue() - color.getGreen()) < 4) {
				gray_colors.put(color, Boolean.TRUE);
			}
			else {
				filtered_colors.put(color, Boolean.TRUE);
			}
		}
		gray_colors.remove(null);
		filtered_colors.remove(null);
		 */
		/*
		for(ColorUsageStat color : color_usage_list) {
			if(color.getPixelPercent() >= 0.025) {
				colors.add(new ColorData(color));
			}
		}
		*/
		
		//List<PaletteColor> palette_colors = ColorPaletteUtils.extractPalette(colors);		
		
		List<ColorData> colors = new ArrayList<ColorData>();
		for(String color : audit_record.getColors()) {
			colors.add(new ColorData(color));
		}

		//generate palette, identify color scheme and score how well palette conforms to color scheme
		List<String> palette_colors = new ArrayList<>();
		palette_colors.addAll( design_system.getColorPalette() );

		log.warn("calculating palette score...");
		Score score = ColorPaletteUtils.getPaletteScore(palette_colors, colors);
		
		Set<UXIssueMessage> issue_messages = new HashSet<>();
		for( UXIssueMessage issue_msg : score.getIssueMessages()) {
			issue_messages.add(ux_issue_service.save(issue_msg));
		}
		
		//score colors found against scheme
		//observations.add(observation_service.save(observation));
		//score colors found against scheme
		//setGrayColors(new ArrayList<>(gray_colors));
		
		Audit audit = new Audit(AuditCategory.AESTHETICS,
								 AuditSubcategory.COLOR_MANAGEMENT,
								 AuditName.COLOR_PALETTE,
								 score.getPointsAchieved(),
								 issue_messages,
								 AuditLevel.PAGE,
								 score.getMaxPossiblePoints(),
								 page_state.getUrl(),
								 why_it_matters,
								 audit_description,
								 false);
		
		audit_service.save(audit);
		audit_service.addAllIssues(audit.getId(), issue_messages);
		return audit;
	}
	
	
}