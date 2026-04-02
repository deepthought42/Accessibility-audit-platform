package com.crawlerApi.generators.report;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.joda.time.LocalDate;

import com.looksee.models.ColorData;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.models.enums.WCAGComplianceLevel;
import com.looksee.utils.ColorUtils;
import com.looksee.utils.ImageUtils;

public class GeneratePDFReport {

	private PDDocument document;
	private String document_name;
	private PDFont base_font;
	private PDFont light_font;
	private PDFont medium_font;
	private PDFont bold_font;
	
	private final int HEADER_XL = 56;
	private final int HEADER_0 = 40;
	private final int HEADER_1 = 32;
	private final int HEADER_2 = 26;
	private final int HEADER_3 = 22;
	private final int HEADER_4 = 20;
	
	private final int TEXT_SIZE = 12;
	
	private final int LEFT_MARGIN_SM = 80;
	private final int LEFT_MARGIN_LG = 120;
	
	
	public GeneratePDFReport(String domain_url) throws IOException, URISyntaxException {
		 setDocument(new PDDocument());
		 setDocumentName(domain_url+"-"+LocalDate.now()+".pdf");
		 setBaseFont(PDType0Font.load(document, 
				 getClass().getResourceAsStream("/fonts/cera-pro/CeraProBlack.ttf")));
		 setLightFont(PDType0Font.load(document, 
				 getClass().getResourceAsStream("/fonts/cera-pro/CeraProLight.ttf")));
		 setMediumFont(PDType0Font.load(document, 
				 getClass().getResourceAsStream("/fonts/cera-pro/CeraProMedium.ttf")));
		 setBoldFont(PDType0Font.load(document, 
				 getClass().getResourceAsStream("/fonts/cera-pro/CeraProBold.ttf")));
	}
	
	/**
	 * Writes the full document for the PDF report
	 * 
	 * @param needs_improvement
	 * @throws IOException
	 */
	public void writeDocument(List<AuditSubcategory> needs_improvement,
							  String domain_host,
							  int pages_audited,
							  int overall_score,
							  int color_management_score,
							  int color_palette_score,
							  int text_contrast_score,
							  int percentage_of_passing_large_text_items,
							  int percent_failing_large_text_items,
							  int percent_failing_small_text_items,
							  int non_text_contrast_score,
							  int percentage_pages_non_text_issues,
							  int written_content_score,
							  int ease_of_understanding_score,
							  int paragraphing_score,
							  int number_of_pages_paragraphing_issues,
							  int average_words_per_sentence,
							  int visuals_score,
							  int visuals_imagery_score,
							  int percent_custom_images,
							  int stock_image_percentage,
							  WCAGComplianceLevel wcag_company_compliance_level,
							  int information_architecture_score,
							  int branding_score,
							  List<String> palette_colors,
							  String avg_difficulty_string,
							  String avg_grade_level,
							  int non_ada_compliant_pages
	) throws IOException {
		
		this.generateCoverPage(domain_host);
		this.generateTableOfContents();
		this.generateWelcomePage(domain_host, pages_audited);
		this.generateScoringPage(domain_host);
		this.generateAuditCategoryDescriptionPage();
		this.generateScoreOverviewPage(overall_score, domain_host, needs_improvement);
		this.generateScoreBreakdownPage(overall_score, 
										domain_host, 
										color_management_score,
										written_content_score,
										visuals_score, 
										information_architecture_score, 
										branding_score);
		
		this.generateColorManagementCoverPage(color_management_score, domain_host);
		
		/*
		List<String> colors = new ArrayList<>();
		colors.add("255, 0, 80");
		colors.add("35, 31, 32");
		colors.add("57, 183, 255");
		colors.add("249, 191, 7");
		colors.add("35, 216, 164");
		*/
		
		//this.generateColorManagementColorPalettePage(color_palette_score, domain_host, palette_colors);
		//this.generateColorPsychologyDescriptionPage(palette_colors);
		this.generateColorManagementTextColorContrastPage(text_contrast_score, 
														  domain_host, 
														  "", 
														  percentage_of_passing_large_text_items, 
														  percent_failing_large_text_items,
														  percent_failing_small_text_items);
		this.generateTextColorContrastWhyItMattersPage();
		
		this.generateNonTextColorContrastPage(non_text_contrast_score, 
											  domain_host, 
											  percentage_pages_non_text_issues);
		this.generateNonTextColorContrastWhyItMattersPage();
		
		this.generateWrittenContentCoverPage(written_content_score, domain_host);
		this.generateWrittenContentEaseOfUnderstandingPage(ease_of_understanding_score, 
														   domain_host,
														   avg_difficulty_string, 
														   avg_grade_level, 
														   non_ada_compliant_pages);
		this.generateEaseOfReadingWhyItMattersPage();
		this.generateWrittenContentParagraphingPage(paragraphing_score, 
													domain_host, 
													number_of_pages_paragraphing_issues, 
													pages_audited, 
													average_words_per_sentence);
		this.generateParagraphingWhyItMattersPage();
		
		//Visuals
		this.generateVisualsCoverPage(visuals_score, domain_host);
		this.generateVisualsImageryPage(visuals_imagery_score, 
										domain_host, 
										percent_custom_images, 
										stock_image_percentage, 
										non_ada_compliant_pages, 
										wcag_company_compliance_level);
		this.generateImageryWhyItMattersPage();
		
		this.generateAppendixCoverPage();
		//this.writeToDoc();
	}
	

	public PDPage generateCoverPage(String domain_host) throws IOException {
		PDPage page = new PDPage();
		addPage(page);
		PDPageContentStream content_stream = new PDPageContentStream(document, page);
	
		//URL cover_background =new URL(COVERPAGE_BACKGROUND_1_PNG_URL);
		//addImageToPage(document, 0, -30, 1.36f, cover_background, content_stream);

		//URL watermark_logo =new URL(WHITELABELED_LOGO_URL);
		//addImageToPage(document, 240, 550, 0.08f, watermark_logo, content_stream);
		
		//company name/report title
		content_stream.setFont(getMediumFont(), HEADER_1);
		content_stream.setNonStrokingColor(new Color( 255, 0, 80 ));
		content_stream.beginText();
		content_stream.newLineAtOffset(220, 200);
		content_stream.showText(domain_host);
		
		content_stream.newLineAtOffset(-80, -50);
		content_stream.showText( "Website Audit Report" );
		content_stream.endText();
		
		content_stream.beginText();
		int copyrightSymbolCodePoint = 169;
		StringBuilder sb = new StringBuilder( "CrawlerApi " ) ;
		sb.appendCodePoint( copyrightSymbolCodePoint ) ;
		sb.append(" 2022");
		String output = sb.toString() ;
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(250, 50);
		content_stream.setNonStrokingColor(Color.WHITE);
		content_stream.showText( output );

		content_stream.endText();
		content_stream.close();
		
		return page;
	}
	
	public PDPage generateTableOfContents() throws IOException {
		PDPage page = new PDPage();
		addPage(page);
		PDPageContentStream content_stream = new PDPageContentStream(document, page);
		//URL background_url =new URL(COVERPAGE_BACKGROUND_4_PNG_URL);
		//addImageToPage(document, 0, 0, 0.32f, background_url, content_stream);
		
		content_stream.setFont(getBoldFont(), HEADER_2);
		content_stream.beginText();
		content_stream.newLineAtOffset(100, 550);
		content_stream.showText("Contents");

		content_stream.endText();
		
		content_stream.setFont(getMediumFont(), TEXT_SIZE);
		content_stream.beginText();
		content_stream.newLineAtOffset(110, 500);
		content_stream.showText("Introduction");
		
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Score Overview");
		
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Color Management");
		
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Written Content");
		
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Visuals");

		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Appendix");
		content_stream.endText();
		
		//Write page numbers
		content_stream.setFont(getMediumFont(), TEXT_SIZE);
		content_stream.beginText();
		content_stream.newLineAtOffset(450, 500);
		content_stream.showText("3");
		
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("6");
		
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("8");
		
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("16");
		
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("22");
		
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("26");
		
		content_stream.endText();
		content_stream.close();
		
		return page;
	}
	
	public PDPage generateWelcomePage(String domain_host, int pages_audited) throws IOException {
		PDPage page = new PDPage();
		addPage(page);
		PDPageContentStream content_stream = new PDPageContentStream(document, page);
	
		//URL background_url =new URL(WELCOME_PAGE_BACKGROUND_PNG_URL);
		//addImageToPage(document, 0, 0, 1.0f, background_url, content_stream);
		
		content_stream.setNonStrokingColor(Color.WHITE);
		content_stream.addRect(LEFT_MARGIN_SM, 0, 550, 800);
		content_stream.fill();

		content_stream.setFont(getBaseFont(), HEADER_0);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_LG, 550);
		content_stream.showText("Welcome to");
		
		content_stream.setFont(getBaseFont(), HEADER_XL);
		content_stream.newLineAtOffset(0, -70);
		content_stream.showText("CrawlerApi!");
		content_stream.endText();
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_LG, 420);
		content_stream.showText("The CrawlerApi platform has analyzed the experience on your website and");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("generated a report for "+domain_host+". We audited a total of "+pages_audited+" pages from");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("your website to give you an overall score and a comprehensive report");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("on areas you can improve to elevate your experience design!");
		
		content_stream.newLineAtOffset(0, -50);
		content_stream.showText("Experience design comprises two broad aspects: ");
		content_stream.setFont(getBoldFont(), TEXT_SIZE);

		content_stream.newLineAtOffset(280, 0);
		content_stream.showText("Aesthetics & Functionality.");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(-280, -30);
		content_stream.showText("Our audit categories stemmed from these two aspects. We analyzed six");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("different categories taking a microscopic view of each element on your");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("website and how it impacts your overall experience.");
		content_stream.endText();
		
		content_stream.close();
		
		return page;
	}
	
	public PDPage generateScoringPage(String domain_host) throws IOException {
		PDPage page = new PDPage();
		addPage(page);
		PDPageContentStream content_stream = new PDPageContentStream(document, page);
	
		URL background_url =new URL("https://storage.googleapis.com/look-see-inc-assets/report-images/backgrounds/background-path-pattern-light.png");
		addImageToPage(document, 0, 0, 1.0f, background_url, content_stream);
		
		content_stream.setNonStrokingColor(Color.WHITE);
		content_stream.addRect(LEFT_MARGIN_SM, 80, 450, 640);
		content_stream.fill();

		content_stream.setFont(getBoldFont(), HEADER_1);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_LG, 650);
		content_stream.showText("CrawlerApi Scoring");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Our comprehensive CrawlerApi scoring criteria encompasses");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("extensive research, best practices, and ADA (Americans with");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("Disabilities Act) compliance guidelines. The "+domain_host+" website");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("was scored against these criteria for each category, according");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("to the following metric:");
		content_stream.endText();

		
		//add emoji icons and scoring
		//poor score
		URL needs_work_emoji_url =new URL("https://storage.googleapis.com/look-see-inc-assets/icons/C-Sad-Face-128px.png");
		addImageToPage(document, LEFT_MARGIN_LG, 450, 0.3f, needs_work_emoji_url, content_stream);
		
		content_stream.setFont(getBoldFont(), HEADER_3);
		content_stream.setNonStrokingColor(new Color(57, 183, 255));
		content_stream.beginText();
		content_stream.newLineAtOffset(180, 470);
		content_stream.showText("Below 60%");
		
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.setFont(getMediumFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("Your experience 'needs work'.");
		content_stream.endText();
		
		//mid score
		//URL almost_there_emoji_url =new URL(ALMOST_THERE_EMOJI_URL);
		//addImageToPage(document, LEFT_MARGIN_LG, 370, 0.3f, almost_there_emoji_url, content_stream);
		
		content_stream.setFont(getBoldFont(), HEADER_3);
		content_stream.setNonStrokingColor(new Color(249, 191, 7));
		content_stream.beginText();
		content_stream.newLineAtOffset(180, 390);
		content_stream.showText("Between 60% to 80%");
		
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.setFont(getMediumFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("Your experience is 'almost there'.");
		content_stream.endText();
		
		//high score
		//URL delightful_emoji_url =new URL(DELIGHTFUL_EMOJI_URL);
		//addImageToPage(document, LEFT_MARGIN_LG, 290, 0.3f, delightful_emoji_url, content_stream);
		content_stream.setFont(getBoldFont(), HEADER_3);
		content_stream.setNonStrokingColor(new Color(35, 216, 164));
		content_stream.beginText();
		content_stream.newLineAtOffset(180, 310);
		content_stream.showText("Above 80%");
		
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.setFont(getMediumFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("Your experience is 'delightful'.");
		content_stream.endText();
		
		
		//final paragraph
		content_stream.beginText();
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(LEFT_MARGIN_LG, 250);
		content_stream.showText("We have also recommended research-backed, actionable");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("steps to upgrade your score to have an exact game plan for");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("your improvement areas.");
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Let's have a look at our CrawlerApi meter to see how you");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("scored.");
		
		content_stream.endText();
		content_stream.close();
		
		return page;
	}
	
	/**
	 * generates pdf page that describes that various audit categories
	 * 
	 * @return PDF page
	 * 
	 * @throws IOException
	 */
	public PDPage generateAuditCategoryDescriptionPage() throws IOException {
		PDPage page = new PDPage();
		addPage(page);
		PDPageContentStream content_stream = new PDPageContentStream(document, page);
	
		//URL background_url =new URL(AUDIT_CATEGORY_DESCRIPTION_BACKGROUND_PNG_URL);
		//addImageToPage(document, 0, 0, 1.0f, background_url, content_stream);
		
		content_stream.setNonStrokingColor(Color.WHITE);
		content_stream.addRect(LEFT_MARGIN_SM, 80, 450, 640);
		content_stream.fill();

		//color management
		//URL color_management_icon = new URL(COLOR_MANAGEMENT_ICON_URL);
		//addImageToPage(document, LEFT_MARGIN_LG, 650, 0.6f, color_management_icon, content_stream);
		
		content_stream.setFont(getBoldFont(), HEADER_4);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.beginText();
		content_stream.newLineAtOffset(180, 680);
		content_stream.showText("Color Management");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("The color palette, contrast of text and");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("non-text items");
		content_stream.endText();
		
		//branding
		//URL branding_icon = new URL(BRANDING_ICON_URL);
		//addImageToPage(document, LEFT_MARGIN_LG, 550, 0.6f, branding_icon, content_stream);
		
		content_stream.setFont(getBoldFont(), HEADER_4);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.beginText();
		content_stream.newLineAtOffset(180, 580);
		content_stream.showText("Branding");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("The position and placement of your logo");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("of icons and visual hierarchy");
		content_stream.endText();
		
		//visuals
		//URL visuals_icon = new URL(VISUALS_LANDSCAPE_ICON_URL);
		//addImageToPage(document, LEFT_MARGIN_LG, 450, 0.6f, visuals_icon, content_stream);
		
		content_stream.setFont(getBoldFont(), HEADER_4);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.beginText();
		content_stream.newLineAtOffset(180, 480);
		content_stream.showText("Visuals");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("The quality and relevance of images, consistency");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("of icons and visual hierarchy");
		content_stream.endText();
		
		
		//written content
		//URL written_content_icon = new URL(WRITTEN_CONTENT_ICON_URL);
		//addImageToPage(document, LEFT_MARGIN_LG, 350, 0.6f, written_content_icon, content_stream);
		
		content_stream.setFont(getBoldFont(), HEADER_4);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.beginText();
		content_stream.newLineAtOffset(180, 380);
		content_stream.showText("Written Content");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("The readability, grammatical errors and");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("organization of the text");
		content_stream.endText();
		
		//typography
		//URL typography_icon = new URL(TYPOGRAPHY_ICON_URL);
		//addImageToPage(document, LEFT_MARGIN_LG, 250, 0.6f, typography_icon, content_stream);
		
		content_stream.setFont(getBoldFont(), HEADER_4);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.beginText();
		content_stream.newLineAtOffset(180, 280);
		content_stream.showText("Typography");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("The consistency of type used across your website");
		content_stream.endText();
		
		//Information architecture
		//URL info_architecture_icon = new URL(INFO_ARCHITECTURE_ICON_URL);
		//addImageToPage(document, LEFT_MARGIN_LG, 150, 0.6f, info_architecture_icon, content_stream);
		
		content_stream.setFont(getBoldFont(), HEADER_4);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.beginText();
		content_stream.newLineAtOffset(180, 180);
		content_stream.showText("Information Architecture");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("The placement and usage of menus, interactions,");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("CTA buttons and overall layout of the website");
		
		content_stream.endText();
		
		content_stream.close();
		
		return page;
	}
	
	/**
	 * generates pdf page that gives an overview of score and areas to improve
	 * 
	 * @return PDF page
	 * 
	 * @throws IOException
	 */
	public void generateScoreOverviewPage(int overall_score, 
										   String domain_host, 
										   List<AuditSubcategory> improvement_areas
	) throws IOException {
		PDPage page = new PDPage();
		addPage(page);
		PDPageContentStream content_stream = new PDPageContentStream(document, page);
	
		//URL background_url =new URL(BACKGROUND_PATH_PATTERN_LIGHT_URL);
		//addImageToPage(document, 50, 0, 1.0f, background_url, content_stream);
		
		content_stream.setNonStrokingColor(Color.WHITE);
		content_stream.addRect(0, 0, 550, 800);
		content_stream.fill();

		content_stream.setFont(getBoldFont(), HEADER_1);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.beginText();
		content_stream.newLineAtOffset(60, 700);
		content_stream.showText("Your Score Overview");
		
		content_stream.endText();
		
		drawScoreStatusBox(overall_score, content_stream, 40, 480, 450, 170);
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(Color.BLACK);

		content_stream.beginText();
		content_stream.newLineAtOffset(70, 450);
		content_stream.showText("You are a few steps away from reaching CrawlerApi's top tier!");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText(domain_host + " meets certain best practice standards and provides a good");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("experience, but you have the potential to elevate your experience further to");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("make it smooth and engaging.");
		
		content_stream.newLineAtOffset(0, -30);
		content_stream.showText("Here are some improvement areas we identified, that will further");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("enhance your website experience:");
		
		//heading 2 styling
		content_stream.setFont(getBoldFont(), HEADER_2);
		
		content_stream.newLineAtOffset(0, -75);
		content_stream.showText("Areas that need improvement");
		content_stream.endText();

		//iterate over list of improvement areas.
		//If index is odd then draw on left, else draw on right
		for(int i=0; i < improvement_areas.size(); i++) {
			AuditSubcategory category = improvement_areas.get(i);
			
			drawImprovementCategory(content_stream, category, i);
		}

		content_stream.close();
	}
	

	/**
	 * Generates the score breakdown page
	 * 
	 * @param overall_score
	 * @param domain_host
	 * @return
	 * @throws IOException
	 */
	public PDPage generateScoreBreakdownPage(int overall_score, 
											String domain_host,
											int color_management_score,
											int written_content_score,
											int visuals_score,
											int information_architecture_score,
											int branding_score
	) throws IOException {
		PDPage page = new PDPage();
		addPage(page);
		PDPageContentStream content_stream = new PDPageContentStream(document, page);

		//URL background_url =new URL(BACKGROUND_PATH_PATTERN_LIGHT_URL);
		//addImageToPage(document, 50, 0, 1.0f, background_url, content_stream);
		
		content_stream.setNonStrokingColor(Color.WHITE);
		content_stream.addRect(0, 0, 550, 800);
		content_stream.fill();
		
		content_stream.setFont(getBoldFont(), HEADER_2);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.beginText();
		content_stream.newLineAtOffset(40, 700);
		content_stream.showText("Score Breakdown");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -30);
		content_stream.showText("You scored "+overall_score+" on the CrawlerApi Meter!");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText(domain_host + " is almost at a delightful user experience.");
		
		content_stream.newLineAtOffset(0, -30);
		content_stream.showText("Check out the score breakdown and our recommendations on");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("improvement areas that will take you to 100%.");
		content_stream.endText();
		
		drawScoreBreakdownBox(color_management_score, 
							  content_stream, 
							  AuditSubcategory.COLOR_MANAGEMENT, 
							  40, 
							  475, 
							  425, 
							  100);
		
		drawScoreBreakdownBox(written_content_score, 
							  content_stream, 
							  AuditSubcategory.WRITTEN_CONTENT, 
							  40, 
							  375, 
							  425, 
							  100);
		
		drawScoreBreakdownBox(visuals_score, 
							  content_stream, 
							  AuditSubcategory.IMAGERY, 
							  40, 
							  275, 
							  425, 
							  100);
		
		drawScoreBreakdownBox(information_architecture_score, 
							  content_stream, 
							  AuditSubcategory.INFORMATION_ARCHITECTURE, 
							  40, 
							  175, 
							  425, 
							  100);
/* Branding is ready yet so it's commented out for now. Date: 2/22/2022
		drawScoreBreakdownBox(branding_score, 
							  content_stream, 
							  AuditSubcategory.BRANDING, 
							  40, 
							  75, 
							  425, 
							  100);
*/
		content_stream.close();
		
		return page;
	}
	
	/** COLOR MANAGEMENT PAGES **/

	/**
	 * Generates the cover page for the Color Management section
	 *  
	 * @param overall_score integer score between 0 and 100 inclusive
	 * @param domain_host domain url
	 * 
	 * @throws IOException
	 * 
	 * @pre domain_host != null
	 * @pre !domain_host.isEmpty()
	 */
	public void generateColorManagementCoverPage(int overall_score, 
												 String domain_host
	) throws IOException {
		assert domain_host != null;
		assert !domain_host.isEmpty();
		
		PDPage page = new PDPage();
		addPage(page);
		PDPageContentStream content_stream = new PDPageContentStream(document, page);
	
		//URL background_url =new URL(COLOR_MANAGEMENT_COVER_1_PNG_URL);
		//addImageToPage(document, 0, -150, 1.3f, background_url, content_stream);
		
		content_stream.setFont(getBaseFont(), HEADER_XL);
		content_stream.setNonStrokingColor(Color.WHITE);
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_SM, 650);
		content_stream.showText("Color");
		content_stream.newLineAtOffset(0, -60);
		content_stream.showText("Management");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -50);
		content_stream.showText("Here is how "+domain_host+" performed on Color Management");
		content_stream.endText();
		
		drawScoreStatusBox(overall_score, content_stream, 60, 330, 450, 170);
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(Color.WHITE);
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_SM, 300);
		content_stream.showText("The colors used on your website reflect your brand, create");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("the look and feel, and influence the user's emotion and");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("intuitive connection with your website.");
				
		content_stream.newLineAtOffset(0, -50);
		content_stream.showText("Under color management, we analyzed the following");
		content_stream.newLineAtOffset(0, -30);
		content_stream.showText("\u2022");
		content_stream.newLineAtOffset(30, 0);
		content_stream.showText("Color Palette");
		content_stream.newLineAtOffset(-30, -30);
		content_stream.showText("\u2022");
		content_stream.newLineAtOffset(30, 0);
		content_stream.showText("Contrast of Text items");
		content_stream.newLineAtOffset(-30, -30);
		content_stream.showText("\u2022");
		content_stream.newLineAtOffset(30, 0);
		content_stream.showText("Contrast of Non-Text items");
		content_stream.endText();

		content_stream.close();
		
		PDPage page2 = new PDPage();
		addPage(page2);
		PDPageContentStream content_stream2 = new PDPageContentStream(document, page2);
	
		//URL background_url2 =new URL(COLOR_MANAGEMENT_COVER_2_PNG_URL);
		//addImageToPage(document, -550, 0, 1.0f, background_url2, content_stream2);
		content_stream2.close();
	}
	
	
	/**
	 * Generates the color palette audit report pages
	 * @param domain_host
	 * @param primary_colors
	 * @param overall_score
	 * 
	 * @throws IOException
	 */
	public void generateColorManagementColorPalettePage(int score, 
			 									 		String domain_host,
			 									 		List<String> primary_colors
	) throws IOException {
		assert domain_host != null;
		assert !domain_host.isEmpty();
		
		PDPage page = new PDPage();
		addPage(page);
		PDPageContentStream content_stream = new PDPageContentStream(document, page);
	
		//URL background_url =new URL(BACKGROUND_PATH_PATTERN_LIGHT_URL);
		//addImageToPage(document, 0, 0, 1.0f, background_url, content_stream);
		
		content_stream.setNonStrokingColor(Color.WHITE);
		content_stream.addRect(LEFT_MARGIN_SM, 0, 550, 800);
		content_stream.fill();
		
		content_stream.setFont(getMediumFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(new Color(255, 0, 80));
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_LG, 750);
		content_stream.showText("COLOR MANAGEMENT");
		
		
		content_stream.setFont(getBoldFont(), HEADER_1);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Color Palette");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("The main colors comprising the background, font, and elements");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("on "+ domain_host +".");
		content_stream.endText();
		
		drawScoreStatusBox(score, content_stream, 100, 430, 450, 170);
		
		content_stream.setFont(getBoldFont(), HEADER_2);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_LG, 380);
		content_stream.showText("Observation");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("The following colors were identified on the "+ domain_host+" website:");
		content_stream.endText();
		
		
		drawColorPaletteColors(primary_colors, LEFT_MARGIN_LG, 220, content_stream);
		
		content_stream.setFont(getBoldFont(), HEADER_2);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_LG, 150);
		content_stream.showText("ADA compliance");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("There are no ADA compliance guidelines regarding the website color");

		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("palette. However, keeping a cohesive color palette allows you to create");

		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("a webpage easy for everyone to read.");
		content_stream.endText();
		
		content_stream.close();
	}
	
	/**
	 * Generates page that describes the psychology of colors used
	 * 
	 * @param colors
	 * 
	 * @throws IOException
	 */
	public void generateColorPsychologyDescriptionPage(
									List<String> colors
	) throws IOException {
		PDPage page = new PDPage();
		addPage(page);
		PDPageContentStream content_stream = new PDPageContentStream(document, page);
	
		//URL background_url =new URL(BACKGROUND_PATH_PATTERN_LIGHT_URL);
		//addImageToPage(document, 0, 0, 1.0f, background_url, content_stream);
		
		content_stream.setNonStrokingColor(Color.WHITE);
		content_stream.addRect(LEFT_MARGIN_SM, 0, 550, 800);
		content_stream.fill();
		
		content_stream.setFont(getBoldFont(), HEADER_2);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_LG, 700);
		content_stream.showText("Accoring to color theory, this is");
		content_stream.newLineAtOffset(0, -30);
		content_stream.showText("what your colors communicate");
		
		content_stream.endText();
		
		for(int i=0; i < colors.size(); i++) {
			ColorData color_data = new ColorData(colors.get(i));
			drawColorPsychologyDescription(color_data, i, content_stream);
		}
		
		content_stream.close();
		
	}
	
	/**
	 * Generates page that describes the psychology of colors used
	 * 
	 * @param colors
	 * 
	 * @throws IOException
	 */
	public void generateColorPaletteWhyItMattersPage() throws IOException {
		PDPage page = new PDPage();
		addPage(page);
		PDPageContentStream content_stream = new PDPageContentStream(document, page);
	
		//URL background_url =new URL(BACKGROUND_PATH_PATTERN_LIGHT_URL);
		//addImageToPage(document, 0, 0, 1.0f, background_url, content_stream);
		
		content_stream.setNonStrokingColor(Color.WHITE);
		content_stream.addRect(LEFT_MARGIN_SM, 0, 550, 800);
		content_stream.fill();
		
		content_stream.setFont(getMediumFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(new Color(255, 0, 80));
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_LG, 750);
		content_stream.showText("MEANING");
		
		content_stream.setFont(getBoldFont(), HEADER_2);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Why is this important for your UX?");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Studies have found that it takes 90 seconds for a customer to form an");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("opinion on a product. 62–90% of that interaction is determined by the");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("color of the product alone.");
		
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Color impacts how a user feels when they interact with your website; it is");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("key to their experience. The right usage of colors can brighten a website");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("and communicates the tone of your brand. Furthermore, using your brand");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("colors consistently makes the website appear cohesive and collected,");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("while creating a sense of familiarity for the user.");
		
		content_stream.setFont(getBoldFont(), HEADER_2);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -80);
		content_stream.showText("Recommendations");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("We recommend increasing the usage of your brand colors throughout the");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("website. Also consider including some bright colors to make the page");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("visually appealing and break the blandness of gray, black, and white.");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("Consider increasing the use of crimson to accentuate you website.");
		
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("The color palette rule of thumb for websites is 60%/30%/10%, hence your");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("primary color should have 60% usage, secondary color 30% and an accent");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("color 10% on your website. Thats why it's also recommended to use a");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("triadic color palette – up to 3 colors. Anything less at times, gives you less");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("room for accentuating important information, while more can be");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("overwhelming and incohesive.");
		content_stream.endText();
		
		content_stream.close();
	}
	
	
	
	
	/**
	 * Generates the color palette audit report pages
	 * @param domain_host
	 * @param primary_colors
	 * @param overall_score
	 * 
	 * @throws IOException
	 */
	public void generateColorManagementTextColorContrastPage(int score, 
			 									 			String domain_host,
			 									 			String example_image_url,
			 									 			int percentage_of_passing_large_text_items,
			 									 			int percent_failing_large_text_items,
			 									 			int percent_failing_small_text_items
	) throws IOException {
		assert domain_host != null;
		assert !domain_host.isEmpty();
		
		PDPage page = new PDPage();
		addPage(page);
		PDPageContentStream content_stream = new PDPageContentStream(document, page);
	
		//URL background_url =new URL(BACKGROUND_PATTERN_PNG_URL);
		//addImageToPage(document, 0, 0, 1.0f, background_url, content_stream);
		
		content_stream.setNonStrokingColor(Color.WHITE);
		content_stream.addRect(LEFT_MARGIN_SM, 0, 550, 800);
		content_stream.fill();
		
		content_stream.setFont(getMediumFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(new Color(255, 0, 80));
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_LG, 750);
		content_stream.showText("COLOR MANAGEMENT");
		
		
		content_stream.setFont(getBoldFont(), HEADER_1);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Contrast of Text Items");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Color contrast between background and text color.");
		content_stream.endText();
		
		drawScoreStatusBox(score, content_stream, 100, 430, 450, 170);
		
		content_stream.setFont(getBoldFont(), HEADER_2);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_LG, 380);
		content_stream.showText("Observation");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Large text items include headings and any content above 18 px.");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText(percentage_of_passing_large_text_items+"% of large text items on the " + domain_host + " website have a good");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("contrast ratio, making it clear and easy to read. "+percent_failing_small_text_items + "% of small");

		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("text on your website has a low contrast ratio with the background");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("color, creating low visibility and legibility.");
		
		content_stream.setFont(getBoldFont(), HEADER_2);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -60);
		content_stream.showText("ADA compliance");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText(percent_failing_small_text_items + "% of small text on your website doesn't meet the minimum ");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("required contrast ratio of 4.5:1.");
		content_stream.newLineAtOffset(0, -30);
		content_stream.showText(percent_failing_large_text_items + "% of large text on your website doesn't meet the minimum");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText(" required contrast ratio of 3:1.");

		content_stream.endText();
		
		content_stream.close();
	}
	
	/**
	 * Generates page that describes the psychology of colors used
	 * 
	 * @param colors
	 * 
	 * @throws IOException
	 */
	public void generateTextColorContrastWhyItMattersPage() throws IOException {
		PDPage page = new PDPage();
		addPage(page);
		PDPageContentStream content_stream = new PDPageContentStream(document, page);
	
		//URL background_url =new URL(BACKGROUND_PATH_PATTERN_LIGHT_URL);
		//addImageToPage(document, 0, 0, 1.0f, background_url, content_stream);
		
		content_stream.setNonStrokingColor(Color.WHITE);
		content_stream.addRect(LEFT_MARGIN_SM, 0, 550, 800);
		content_stream.fill();
		
		content_stream.setFont(getMediumFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(new Color(255, 0, 80));
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_LG, 700);
		content_stream.showText("MEANING");
		
		content_stream.setFont(getBoldFont(), HEADER_2);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Why is this important for your UX?");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Color, just like the overall design, goes beyond aesthetics. It impacts the");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("usability and functionality of your website, deciding what information");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("stands out to the user.");
		
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("A good contrast ratio makes your content easy to read and navigate");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("through, creating a comfortable and engaging experience for your user.");
		
		content_stream.setFont(getBoldFont(), HEADER_2);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -80);
		content_stream.showText("Recommendations");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Improve the contrast score by changing the low contrast colors to meet");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("minimum contrast requirements.");
		
		content_stream.setFont(getMediumFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(new Color(255, 0, 80));
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Small Text");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -30);
		content_stream.showText("Contrast of 4.5 – 7 with the background for WCAG level AA compliance");
		
		content_stream.setFont(getMediumFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(new Color(255, 0, 80));
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Large Text");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -30);
		content_stream.showText("Contrast of 3 – 4.5 with the background.");
		content_stream.newLineAtOffset(0, -15);
		content_stream.showText("Black on white or vice versa is not recommended if not balanced with an accent");
		content_stream.newLineAtOffset(0, -15);
		content_stream.showText("color or neutral colors like grey, since they can be too harsh.");
		
		content_stream.endText();
		
		content_stream.close();
	}
	
	
	/**
	 * Generates the color palette audit report pages
	 * @param domain_host
	 * @param primary_colors
	 * @param overall_score
	 * 
	 * @throws IOException
	 */
	public void generateNonTextColorContrastPage(int score, 
 									 			 String domain_host,
 									 			 int percent_pages_failing_non_text_items
	) throws IOException {
		assert domain_host != null;
		assert !domain_host.isEmpty();
		
		PDPage page = new PDPage();
		addPage(page);
		PDPageContentStream content_stream = new PDPageContentStream(document, page);
	
		//URL background_url =new URL(BACKGROUND_PATH_PATTERN_LIGHT_URL);
		//addImageToPage(document, 0, 0, 1.0f, background_url, content_stream);
		
		content_stream.setNonStrokingColor(Color.WHITE);
		content_stream.addRect(LEFT_MARGIN_SM, 0, 550, 800);
		content_stream.fill();
		
		content_stream.setFont(getMediumFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(new Color(255, 0, 80));
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_LG, 750);
		content_stream.showText("COLOR MANAGEMENT");
		
		
		content_stream.setFont(getBoldFont(), HEADER_1);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Contrast of Non-Text Items");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Color contrast between background and non-text elements");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("(buttons & icons).");
		content_stream.endText();
		
		drawScoreStatusBox(score, content_stream, 100, 430, 450, 170);
		
		content_stream.setFont(getBoldFont(), HEADER_2);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_LG, 380);
		content_stream.showText("Observation");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("There are only 2 icons on your website, on the Home page. They have a");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("high contrast ratio against the background color.");
		
		content_stream.setFont(getBoldFont(), HEADER_2);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -60);
		content_stream.showText("ADA compliance");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText(percent_pages_failing_non_text_items + "% of pages on your website have non-text elements");
		content_stream.showText("that don't meet the minimum");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("required contrast ratio of 3:1.");
		
		content_stream.endText();
		
		content_stream.close();
	}
	
	/**
	 * Generates page that describes the psychology of colors used
	 * 
	 * @param colors
	 * 
	 * @throws IOException
	 */
	public void generateNonTextColorContrastWhyItMattersPage() throws IOException {
		PDPage page = new PDPage();
		addPage(page);
		PDPageContentStream content_stream = new PDPageContentStream(document, page);
	
		//URL background_url =new URL(BACKGROUND_PATH_PATTERN_LIGHT_URL);
		//addImageToPage(document, 0, 0, 1.0f, background_url, content_stream);
		
		content_stream.setNonStrokingColor(Color.WHITE);
		content_stream.addRect(LEFT_MARGIN_SM, 0, 550, 800);
		content_stream.fill();
		
		content_stream.setFont(getMediumFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(new Color(255, 0, 80));
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_LG, 700);
		content_stream.showText("MEANING");
		
		content_stream.setFont(getBoldFont(), HEADER_2);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Why is this important for your UX?");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Icons are an easily recognizable, fun element, and a great way to");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("communicate with your user beyond just using text. Icons should be");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("familiar and captivating.");
		
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Bright colors have higher conversion rates, so it is important for your");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("button to have a high contrast score to create an eye-catching effect");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("and be evidently clickable.");
		
		content_stream.setFont(getBoldFont(), HEADER_2);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -80);
		content_stream.showText("Recommendations");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Change the CTA color buttons to high contrast colors, to captivate the user's");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("interest.");
		
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Graphical objects & user interface components should have a color contrast");
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("of at least 3:1 with the background colors.");
		
		content_stream.endText();
		
		content_stream.close();
	}
	
	/******************************************************************
	 * 
	 *  COLOR MANAGEMENT PAGES 
	 *  
	 *****************************************************************/

	/**
	 * Generates the cover page for the Color Management section
	 *  
	 * @param overall_score integer score between 0 and 100 inclusive
	 * @param domain_host domain url
	 * 
	 * @throws IOException
	 * 
	 * @pre domain_host != null
	 * @pre !domain_host.isEmpty()
	 */
	public void generateWrittenContentCoverPage(int overall_score, 
												 String domain_host
	) throws IOException {
		assert domain_host != null;
		assert !domain_host.isEmpty();
		
		PDPage page = new PDPage();
		addPage(page);
		PDPageContentStream content_stream = new PDPageContentStream(document, page);
	
		//URL background_url =new URL(WRITTEN_CONTENT_COVER_BACKGROUND_PNG_URL);
		//addImageToPage(document, 0, -150, 1.3f, background_url, content_stream);
		
		content_stream.setFont(getBaseFont(), HEADER_XL);
		content_stream.setNonStrokingColor(Color.WHITE);
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_SM, 650);
		content_stream.showText("Written");
		content_stream.newLineAtOffset(0, -60);
		content_stream.showText("Content");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -50);
		content_stream.showText("Here is how "+domain_host+" performed on Written Content:");
		content_stream.endText();
		
		drawScoreStatusBox(overall_score, content_stream, 60, 330, 450, 170);
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(Color.WHITE);
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_SM, 300);
		content_stream.showText("The information and experiences that are directed towards an");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("end-user or audience. Written content is information expressed");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("through a textual medium (speech/writing).");
				
		content_stream.newLineAtOffset(0, -50);
		content_stream.showText("Under written content, we analyzed the following");
		content_stream.newLineAtOffset(0, -30);
		content_stream.showText("\u2022");
		content_stream.newLineAtOffset(30, 0);
		content_stream.showText("Content");
		content_stream.newLineAtOffset(-30, -30);
		content_stream.showText("\u2022");
		content_stream.newLineAtOffset(30, 0);
		content_stream.showText("Ease of Understanding");
		content_stream.newLineAtOffset(-30, -30);
		content_stream.showText("\u2022");
		content_stream.newLineAtOffset(30, 0);
		content_stream.showText("Paragraphing");
		content_stream.endText();

		content_stream.close();
		
		PDPage page2 = new PDPage();
		addPage(page2);
		PDPageContentStream content_stream2 = new PDPageContentStream(document, page2);
	
		//URL background_url2 =new URL(WRITTEN_CONTENT_COVER_BACKGROUND_PNG_URL);
		//addImageToPage(document, -550, 0, 1.0f, background_url2, content_stream2);
		content_stream2.close();
	}
	
	/**
	 * 
	 * @param score
	 * @param domain_host
	 * @param average_reading_complexity (easy, moderate, complex)
	 * @param average_grade_level
	 * @param number_of_non_ada_compliant_pages
	 * @throws IOException
	 */
	public void generateWrittenContentEaseOfUnderstandingPage(int score, 
 									 			 			  String domain_host,
 									 			 			  String avg_difficulty_string,
 									 			 			  String average_grade_level,
 									 			 			  int number_of_non_ada_compliant_pages
	) throws IOException {
		assert domain_host != null;
		assert !domain_host.isEmpty();
		
		PDPage page = new PDPage();
		addPage(page);
		PDPageContentStream content_stream = new PDPageContentStream(document, page);
	
		//URL background_url =new URL(BACKGROUND_PATH_PATTERN_LIGHT_URL);
		//addImageToPage(document, 0, 0, 1.0f, background_url, content_stream);
		
		content_stream.setNonStrokingColor(Color.WHITE);
		content_stream.addRect(LEFT_MARGIN_SM, 0, 550, 800);
		content_stream.fill();
		
		content_stream.setFont(getMediumFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(new Color(255, 0, 80));
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_LG, 750);
		content_stream.showText("WRITTEN CONTENT");
		
		
		content_stream.setFont(getBoldFont(), HEADER_1);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Ease of");
		content_stream.newLineAtOffset(0, -30);
		content_stream.showText("Understanding");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("The reading level of the content on the " + domain_host + " website.");
		content_stream.endText();
		
		drawScoreStatusBox(score, content_stream, 100, 430, 450, 170);
		
		content_stream.setFont(getBoldFont(), HEADER_2);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_LG, 380);
		content_stream.showText("Observation");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("The main navigation pages on your website are "+avg_difficulty_string+" to understand.");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("with an average reading level of "+average_grade_level);
		
		content_stream.setFont(getBoldFont(), HEADER_2);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -60);
		content_stream.showText("ADA compliance");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("We found " + number_of_non_ada_compliant_pages + " page that do not meet accessibility standards");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("as they are not easily understandable for mass audiences");
		content_stream.newLineAtOffset(0, -30);
		content_stream.showText("ADA Compliance 3.1.5 requires text to not require reading ability more");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("advanced than the lower secondary education level, or have simpler");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("version of the content available.");
		
		content_stream.endText();
		
		content_stream.close();
	}
	
	
	/**
	 * Generates page that describes the psychology of colors used
	 * 
	 * @param colors
	 * 
	 * @throws IOException
	 */
	public void generateEaseOfReadingWhyItMattersPage() throws IOException {
		PDPage page = new PDPage();
		addPage(page);
		PDPageContentStream content_stream = new PDPageContentStream(document, page);
	
		//URL background_url =new URL(BACKGROUND_PATH_PATTERN_LIGHT_URL);
		//addImageToPage(document, 0, 0, 1.0f, background_url, content_stream);
		
		content_stream.setNonStrokingColor(Color.WHITE);
		content_stream.addRect(LEFT_MARGIN_SM, 0, 550, 800);
		content_stream.fill();
		
		content_stream.setFont(getMediumFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(new Color(255, 0, 80));
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_LG, 700);
		content_stream.showText("MEANING");
		
		content_stream.setFont(getBoldFont(), HEADER_2);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Why is this important for your UX?");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Simple content makes reading comfortable for the user; the use of");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("complex vocabulary can disrupt their flow of reading. Popular novels");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("aimed at American adults tend to be written in the 7th to 9th-grade range,");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("which mirrors the reading capability of the average American adult.");
		
		content_stream.setFont(getBoldFont(), HEADER_2);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -80);
		content_stream.showText("Recommendations");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Assess your target audience and adjust your text on the identified pages");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("accordingly. Text should be at a lower secondary education level by U.S.");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("Educational standards.");
		
		content_stream.endText();
		
		content_stream.close();
	}
	
	
	/**
	 * Generates the observation and ADA compliance page for written content paragraphing category
	 * 
	 * @param score
	 * @param domain_host
	 * @param number_of_pages_with_issues
	 * @param total_pages
	 * @param average_words_per_sentence
	 * @throws IOException
	 */
	public void generateWrittenContentParagraphingPage(int score, 
							 			 			  String domain_host,
							 			 			  int number_of_pages_with_issues,
							 			 			  int total_pages,
							 			 			  int average_words_per_sentence
	) throws IOException {
		assert domain_host != null;
		assert !domain_host.isEmpty();
		int percent_pages_with_issues = (int)(((double)number_of_pages_with_issues / (double)total_pages)*100);
		
		PDPage page = new PDPage();
		addPage(page);
		PDPageContentStream content_stream = new PDPageContentStream(document, page);
	
		//URL background_url =new URL(BACKGROUND_PATH_PATTERN_LIGHT_URL);
		//addImageToPage(document, 0, 0, 1.0f, background_url, content_stream);
		
		content_stream.setNonStrokingColor(Color.WHITE);
		content_stream.addRect(LEFT_MARGIN_SM, 0, 550, 800);
		content_stream.fill();
		
		content_stream.setFont(getMediumFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(new Color(255, 0, 80));
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_LG, 750);
		content_stream.showText("WRITTEN CONTENT");
		
		
		content_stream.setFont(getBoldFont(), HEADER_1);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Paragraphing");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("The way your textual content is structured and organized.");
		content_stream.endText();
		
		drawScoreStatusBox(score, content_stream, 100, 430, 450, 170);
		
		content_stream.setFont(getBoldFont(), HEADER_2);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_LG, 380);
		content_stream.showText("Observation");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText(percent_pages_with_issues+"% of the pages ("+number_of_pages_with_issues+"/"+total_pages+") contain lengthy and verbose sentences with over");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText(average_words_per_sentence+" words per sentence on average.");
		
		content_stream.setFont(getBoldFont(), HEADER_2);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -60);
		content_stream.showText("ADA compliance");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Even though there are no ADA compliance requirements specifically for");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("this category, reading level needs to be taken into consideration when");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("writing content and paragraphing.");
		
		content_stream.endText();
		
		content_stream.close();
	}
	
	
	/**
	 * Generates page that describes the structure of paragraphs in written content
	 * 
	 * @param colors
	 * 
	 * @throws IOException
	 */
	public void generateParagraphingWhyItMattersPage() throws IOException {
		PDPage page = new PDPage();
		addPage(page);
		PDPageContentStream content_stream = new PDPageContentStream(document, page);
	
		//URL background_url =new URL(BACKGROUND_PATH_PATTERN_LIGHT_URL);
		//addImageToPage(document, 0, 0, 1.0f, background_url, content_stream);
		
		content_stream.setNonStrokingColor(Color.WHITE);
		content_stream.addRect(LEFT_MARGIN_SM, 0, 550, 800);
		content_stream.fill();
		
		content_stream.setFont(getMediumFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(new Color(255, 0, 80));
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_LG, 700);
		content_stream.showText("MEANING");
		
		content_stream.setFont(getBoldFont(), HEADER_2);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Why is this important for your UX?");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("The way users experience content has changed in the mobile phone era.");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("Attention spans are shorter, and users skim through most information.");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("Presenting information in small, easy to digest chunks makes their");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("experience easy and convenient.");
		
		content_stream.setFont(getBoldFont(), HEADER_2);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -80);
		content_stream.showText("Recommendations");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("No more than 25 words per sentence to comply with US and EU standards");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("5 sentences per paragraph is recommended.");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("Smaller chunks of info are easier to read and understand.");
		
		content_stream.endText();
		
		content_stream.close();
	}
	
	
	/**
	 * Generates the observation and ADA compliance page for written content paragraphing category
	 * 
	 * @param score
	 * @param domain_host
	 * @param number_of_pages_with_issues
	 * @param total_pages
	 * @param average_words_per_sentence
	 * @throws IOException
	 */
	public void generateWrittenContentSEOPage(int score, 
												String domain_host,
												int number_of_pages_with_issues,
												int total_pages,
												int average_words_per_sentence
	) throws IOException {
		assert domain_host != null;
		assert !domain_host.isEmpty();
		int percent_pages_with_issues = (int)(((double)number_of_pages_with_issues / (double)total_pages)*100);
		
		PDPage page = new PDPage();
		addPage(page);
		PDPageContentStream content_stream = new PDPageContentStream(document, page);
	
		//URL background_url =new URL(BACKGROUND_PATH_PATTERN_LIGHT_URL);
		//addImageToPage(document, 0, 0, 1.0f, background_url, content_stream);
		
		content_stream.setNonStrokingColor(Color.WHITE);
		content_stream.addRect(LEFT_MARGIN_SM, 0, 550, 800);
		content_stream.fill();
		
		content_stream.setFont(getMediumFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(new Color(255, 0, 80));
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_LG, 750);
		content_stream.showText("WRITTEN CONTENT");
		
		content_stream.setFont(getBoldFont(), HEADER_1);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("SEO");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("A checklist of key SEO elements that should be");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("incorporated into the Modulate website to make a user");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("more likely to find and click on your website.");
		content_stream.endText();
		
		drawScoreStatusBox(score, content_stream, 100, 380, 450, 170);
		
		content_stream.setFont(getBoldFont(), HEADER_2);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_LG, 330);
		content_stream.showText("Observation");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -50);
		content_stream.showText("We scored your website on 4 SEO elements:");
		
		content_stream.newLineAtOffset(0, -30);
		content_stream.showText("\u2022");
		content_stream.newLineAtOffset(30, 0);
		content_stream.showText("Meta Title");
		
		content_stream.newLineAtOffset(-30, -30);
		content_stream.showText("\u2022");
		content_stream.newLineAtOffset(30, 0);
		content_stream.showText("Meta Description");
		
		content_stream.newLineAtOffset(-30, -30);
		content_stream.showText("\u2022");
		content_stream.newLineAtOffset(30, 0);
		content_stream.showText("Heading Tags");
		
		content_stream.newLineAtOffset(-30, -30);
		content_stream.showText("\u2022");
		content_stream.newLineAtOffset(30, 0);
		content_stream.showText("SiteMap");
		
		content_stream.newLineAtOffset(-30, -30);
		content_stream.showText("\u2022");
		content_stream.newLineAtOffset(30, 0);
		content_stream.showText("Alternative text");
		
		content_stream.showText(percent_pages_with_issues+"% of the pages ("+number_of_pages_with_issues+"/"+total_pages+") contain lengthy and verbose sentences with over");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText(average_words_per_sentence+" words per sentence on average.");
		
		content_stream.setFont(getBoldFont(), HEADER_2);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -60);
		content_stream.showText("ADA compliance");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Even though there are no ADA compliance requirements specifically for");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("this category, reading level needs to be taken into consideration when");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("writing content and paragraphing.");
		
		content_stream.endText();
		
		content_stream.close();
	}
	
	
	/**
	 * Generates page that describes the structure of paragraphs in written content
	 * 
	 * @param colors
	 * 
	 * @throws IOException
	 */
	public void generateSEOWhyItMattersPage() throws IOException {
		PDPage page = new PDPage();
		addPage(page);
		PDPageContentStream content_stream = new PDPageContentStream(document, page);
	
		//URL background_url =new URL(BACKGROUND_PATH_PATTERN_LIGHT_URL);
		//addImageToPage(document, 0, 0, 1.0f, background_url, content_stream);
		
		content_stream.setNonStrokingColor(Color.WHITE);
		content_stream.addRect(LEFT_MARGIN_SM, 0, 550, 800);
		content_stream.fill();
		
		content_stream.setFont(getMediumFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(new Color(255, 0, 80));
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_LG, 700);
		content_stream.showText("MEANING");
		
		content_stream.setFont(getBoldFont(), HEADER_2);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Why is this important for your UX?");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("The meta-title and meta-description provide the user with a concise,");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("easy to digest summary of what your page is about though search");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("engines. A user is more likely to click on your website and visit your page");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("if they are attracted by a title and description that is clear, concise, and");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("not overwhelming. An SEO-friendly URL improves your website's search");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("ability.");
		
		content_stream.setFont(getBoldFont(), HEADER_2);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -80);
		content_stream.showText("Recommendations");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("We recommend ensuring all pages have meta-descriptions.");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("Most search engines only show descriptions up to 160 characters so we");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("suggest keeping them within that length. Also consider creating SEO");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("suggest keeping them within that length.");
		content_stream.newLineAtOffset(0, -20);
		
		content_stream.endText();
		
		content_stream.close();
	}
	
	
	/*****************************************************************
	 * 
	 * VISUALS
	 * 
	 *****************************************************************/
	
	/**
	 * Generates the cover page for the Visuals section
	 *  
	 * @param overall_score integer score between 0 and 100 inclusive
	 * @param domain_host domain url
	 * 
	 * @throws IOException
	 * 
	 * @pre domain_host != null
	 * @pre !domain_host.isEmpty()
	 */
	public void generateVisualsCoverPage(int overall_score, 
										 String domain_host
	) throws IOException {
		assert domain_host != null;
		assert !domain_host.isEmpty();
		
		PDPage page = new PDPage();
		addPage(page);
		PDPageContentStream content_stream = new PDPageContentStream(document, page);
	
		//URL background_url =new URL(COVERPAGE_BACKGROUND_PNG_URL);
		//addImageToPage(document, 0, -150, 1.3f, background_url, content_stream);
		
		content_stream.setFont(getBaseFont(), 56);
		content_stream.setNonStrokingColor(Color.WHITE);
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_SM, 650);
		content_stream.showText("Visuals");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -50);
		content_stream.showText("Here is how "+domain_host+" performed on Visuals");
		content_stream.endText();
		
		drawScoreStatusBox(overall_score, content_stream, 60, 330, 450, 170);
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(Color.WHITE);
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_SM, 300);
		content_stream.showText("Visual appeal is what meets the eye. The overall visual balance of a");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("design.");
				
		content_stream.newLineAtOffset(0, -50);
		content_stream.showText("We analyzed the following");
		content_stream.newLineAtOffset(-30, -30);
		content_stream.showText("\u2022");
		content_stream.newLineAtOffset(30, 0);
		content_stream.showText("Imagery");
		content_stream.newLineAtOffset(-30, -30);
		content_stream.showText("\u2022");
		content_stream.newLineAtOffset(30, 0);
		content_stream.showText("Videos");
		content_stream.endText();

		content_stream.close();
		
		PDPage page2 = new PDPage();
		addPage(page2);
		PDPageContentStream content_stream2 = new PDPageContentStream(document, page2);
	
		//URL background_url2 =new URL(VISUALS_BACKGROUND_1_PNG_URL);
		//addImageToPage(document, -550, 0, 1.0f, background_url2, content_stream2);
		content_stream2.close();
	}
	
	/**
	 * 
	 * @param score
	 * @param domain_host
	 * @param average_reading_complexity (easy, moderate, complex)
	 * @param average_grade_level
	 * @param number_of_non_ada_compliant_pages
	 * @throws IOException
	 */
	public void generateVisualsImageryPage(int score, 
				 			 			   String domain_host,
				 			 			   int percent_custom_images,
				 			 			   int stock_image_percentage,
				 			 			   int page_count_wcag_compliance_issues,
				 			 			   WCAGComplianceLevel company_compliance
	) throws IOException {
		assert domain_host != null;
		assert !domain_host.isEmpty();
		
		PDPage page = new PDPage();
		addPage(page);
		PDPageContentStream content_stream = new PDPageContentStream(document, page);
	
		//URL background_url =new URL(BACKGROUND_PNG_URL);
		//addImageToPage(document, 0, 0, 1.0f, background_url, content_stream);
		
		content_stream.setNonStrokingColor(Color.WHITE);
		content_stream.addRect(LEFT_MARGIN_SM, 0, 550, 800);
		content_stream.fill();
		
		content_stream.setFont(getMediumFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(new Color(255, 0, 80));
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_LG, 750);
		content_stream.showText("VISUAL");
		
		
		content_stream.setFont(getBoldFont(), HEADER_1);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Imagery");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("The type of images, graphics, and illustrations used on your website.");
		content_stream.endText();
		
		drawScoreStatusBox(score, content_stream, 100, 450, 450, 170);
		
		content_stream.setFont(getBoldFont(), HEADER_2);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_LG, 410);
		content_stream.showText("Observation");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText(percent_custom_images+"% of the images used are your own custom photography. This is the");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("industry practice customizing the look and feel of your website allowing");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("users to connect on a personal level.");
		
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText(stock_image_percentage+" of images are stock images. Using stock imagery as-is");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("is not an ideal practice. It is recommended that when using them, some");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("customization should be done to personalize the user experience.");
		
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("The main reason behind a low score is due to the missing alt text to all");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("images. Alt text enhances your website with respect to SEO elements");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("and part of the best practices.");
		
		content_stream.setFont(getBoldFont(), HEADER_2);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -60);
		content_stream.showText("ADA compliance");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		if(page_count_wcag_compliance_issues > 0) {
			content_stream.newLineAtOffset(0, -40);
			content_stream.showText("Your website does not meet the level "+company_compliance+" ADA compliance requirement for");
			content_stream.newLineAtOffset(0, -20);
			content_stream.showText("'Alt' text for images present on the website.");
		}
		else {
			content_stream.newLineAtOffset(0, -40);
			content_stream.showText("Your website meets the level "+company_compliance+" ADA compliance requirement for");
			content_stream.newLineAtOffset(0, -20);
			content_stream.showText("'Alt' text for images present on the website.");
		}
		
		content_stream.endText();
		
		content_stream.close();
	}
	
	/**
	 * Generates page that describes the structure of paragraphs in written content
	 * 
	 * @param colors
	 * 
	 * @throws IOException
	 */
	public void generateImageryWhyItMattersPage() throws IOException {
		PDPage page = new PDPage();
		addPage(page);
		PDPageContentStream content_stream = new PDPageContentStream(document, page);
	
		//URL background_url =new URL(BACKGROUND_PNG_URL);
		//addImageToPage(document, 0, 0, 1.0f, background_url, content_stream);
		
		content_stream.setNonStrokingColor(Color.WHITE);
		content_stream.addRect(LEFT_MARGIN_SM, 0, 550, 800);
		content_stream.fill();
		
		content_stream.setFont(getMediumFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(new Color(255, 0, 80));
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_LG, 700);
		content_stream.showText("MEANING");
		
		content_stream.setFont(getBoldFont(), HEADER_2);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Why is this important for your UX?");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Images create a more immersive experience for the user, giving context");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("to the text and allowing the user to visualize what is being presented. The");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("brain processes images faster than text, so images are very effective at");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("communicating a message. Furthermore, images have the power to elicit");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("an emotional response from the user, creating a stronger connection.");
		
		content_stream.setFont(getBoldFont(), HEADER_2);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -80);
		content_stream.showText("Recommendations");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("From a performance perspective an image should be no larger than the");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("maximum size it is rendered at on screen. For example, an icon could");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("use an image that is 1500×1500 pixels, but only renders in a space of");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("100×100 pixels.");
		
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("Other recommendations include customizing any stock photography if");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("not custom photography you may intend to use on the website.");
		
		content_stream.newLineAtOffset(0, -40);
		content_stream.showText("You should ensure that you add relevant 'Alt' text for SEO and ADA");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("compliance purposes.");
		
		content_stream.endText();
		
		content_stream.close();
	}
	
	/********************************************************
	 * 
	 * Information Architecture
	 * 
	 *********************************************************/
	
	/**
	 * Generates the cover page for the Visuals section
	 *  
	 * @param overall_score integer score between 0 and 100 inclusive
	 * @param domain_host domain url
	 * 
	 * @throws IOException
	 * 
	 * @pre domain_host != null
	 * @pre !domain_host.isEmpty()
	 */
	public void generateInformationArchitectureCoverPage(int overall_score, 
										 				 String domain_host
	) throws IOException {
		assert domain_host != null;
		assert !domain_host.isEmpty();
		
		PDPage page = new PDPage();
		addPage(page);
		PDPageContentStream content_stream = new PDPageContentStream(document, page);
	
		//URL background_url =new URL(VISUALS_BACKGROUND_1_PNG_URL);
		//addImageToPage(document, 0, -150, 1.3f, background_url, content_stream);
		
		content_stream.setFont(getBaseFont(), HEADER_XL);
		content_stream.setNonStrokingColor(Color.WHITE);
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_SM, 650);
		content_stream.showText("Information");
		content_stream.newLineAtOffset( 0, HEADER_2);
		content_stream.showText("Architecture");
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(0, -50);
		content_stream.showText("Here is how "+domain_host+" performed on Information Architecture");
		content_stream.endText();
		
		drawScoreStatusBox(overall_score, content_stream, 60, 330, 450, 170);
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(Color.WHITE);
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_SM, 300);
		content_stream.showText("The effectiveness and functionality of the structure and different");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("interactive elements on your website such as menu, links and");
		content_stream.newLineAtOffset(0, -20);
		content_stream.showText("buttons, as well as the overall structure of the website.");
				
		content_stream.newLineAtOffset(0, -50);
		content_stream.showText("Under information architecture, we analyzed the following");
		content_stream.newLineAtOffset(-30, -30);
		content_stream.showText("\u2022");
		content_stream.newLineAtOffset(30, 0);
		content_stream.showText("Menu");
		content_stream.newLineAtOffset(-30, -30);
		content_stream.showText("\u2022");
		content_stream.newLineAtOffset(30, 0);
		content_stream.showText("Interactions");
		content_stream.newLineAtOffset(-30, -30);
		content_stream.showText("\u2022");
		content_stream.newLineAtOffset(30, 0);
		content_stream.showText("CTA buttons");
		content_stream.newLineAtOffset(-30, -30);
		content_stream.showText("\u2022");
		content_stream.newLineAtOffset(30, 0);
		content_stream.showText("Layout");
		content_stream.endText();

		content_stream.close();
		
		PDPage page2 = new PDPage();
		addPage(page2);
		PDPageContentStream content_stream2 = new PDPageContentStream(document, page2);
	
		//URL background_url2 =new URL(VISUALS_BACKGROUND_1_PNG_URL);
		//addImageToPage(document, -550, 0, 1.0f, background_url2, content_stream2);
		content_stream2.close();
	}
	
	
	/******************************************************
	 * 
	 * Appendix
	 * 
	 ******************************************************/
	
	/**
	 * Generates the cover page for the Visuals section
	 *  
	 * @param overall_score integer score between 0 and 100 inclusive
	 * @param domain_host domain url
	 * 
	 * @throws IOException
	 * 
	 * @pre domain_host != null
	 * @pre !domain_host.isEmpty()
	 */
	public void generateAppendixCoverPage() throws IOException {
		/*
		PDPage page = new PDPage();
		addPage(page);
		PDPageContentStream content_stream = new PDPageContentStream(document, page);
	
		URL background_url =new URL("https://storage.googleapis.com/look-see-inc-assets/report-images/backgrounds/appendix-background-1.png");
		addImageToPage(document, -250, -150, 1.2f, background_url, content_stream);
		
		content_stream.setFont(getBaseFont(), 56);
		content_stream.setNonStrokingColor(Color.WHITE);
		content_stream.beginText();
		content_stream.newLineAtOffset(LEFT_MARGIN_SM, 350);
		content_stream.showText("Appendix");
		
		content_stream.endText();
		
		content_stream.close();
		*/
		PDPage page2 = new PDPage();
		addPage(page2);
		PDPageContentStream content_stream2 = new PDPageContentStream(document, page2);
	
		//URL background_url2 =new URL(APPENDIX_BACKGROUND_1_PNG_URL);
		//addImageToPage(document, -550, 0, 1.0f, background_url2, content_stream2);
		content_stream2.close();
	}
	
	/******************************************************
	 * 
	 * DRAW METHODS FOR INDIVIDUAL COMPONENTS
	 *  
	 *******************************************************/

	/**
	 * 
	 * @param color_data
	 * @param index
	 * @param content_stream
	 * @throws IOException
	 */
	private void drawColorPsychologyDescription(ColorData color_data,
												int index,
												PDPageContentStream content_stream
	) throws IOException {
		int y_offset = 600 - (index * 125);
		
		if(ColorUtils.isBlack(color_data)) {
			//draw 50x50 square with the color in it
			drawColorBox(color_data, LEFT_MARGIN_LG, y_offset, 30, 30, content_stream);
			content_stream.setFont(getBoldFont(), HEADER_4);
			content_stream.setNonStrokingColor(Color.BLACK);
			content_stream.beginText();
			content_stream.newLineAtOffset(180, y_offset+10);
			content_stream.showText("Black");
			
			content_stream.setFont(getLightFont(), TEXT_SIZE);
			content_stream.newLineAtOffset(-40, -40);
			content_stream.showText("Formal, dignified, sophisticated, and mysterious. Black often has an intense energy.");
			content_stream.newLineAtOffset(0, -15);
			content_stream.showText("Be careful how you use this color, as it can also be create feelings of aloofness and");
			content_stream.newLineAtOffset(0, -15);
			content_stream.showText("depression. To avoid this, pair black with bright and vibrant colors.");
			
			content_stream.endText();
		}
		else if(ColorUtils.isWhite(color_data)) {
			//draw 50x50 square with the color in it
			drawColorBox(color_data, LEFT_MARGIN_LG, y_offset, 30, 30, content_stream);
			content_stream.setFont(getBoldFont(), HEADER_4);
			content_stream.setNonStrokingColor(Color.BLACK);
			content_stream.beginText();
			content_stream.newLineAtOffset(180, y_offset+10);
			content_stream.showText("White");
			
			content_stream.setFont(getLightFont(), TEXT_SIZE);
			content_stream.newLineAtOffset(-40, -40);
			content_stream.showText("Represents purity and innocence and often evokes a sense of space. Some of the positivity");
			content_stream.newLineAtOffset(0, -15);
			content_stream.showText("positive meanings that white can convey include cleanliness, freshness, and simplicity. ");
			
			content_stream.endText();
		}
		else if(ColorUtils.isRed(color_data)) {
			//draw 50x50 square with the color in it
			drawColorBox(color_data, LEFT_MARGIN_LG, y_offset, 30, 30, content_stream);
			content_stream.setFont(getBoldFont(), HEADER_4);
			content_stream.setNonStrokingColor(Color.BLACK);
			content_stream.beginText();
			content_stream.newLineAtOffset(180, y_offset+10);
			content_stream.showText("Red");
			
			content_stream.setFont(getLightFont(), TEXT_SIZE);
			content_stream.newLineAtOffset(-40, -40);
			content_stream.showText("Associated with strong emotions, such as love, passion, and anger. It's the");
			content_stream.newLineAtOffset(0, -15);
			content_stream.showText("universal color to signify strength, power, courage, and danger. Red is vibrant,");
			content_stream.newLineAtOffset(0, -15);
			content_stream.showText("stimulating and exciting with a strong link to sexuality and increased appetites.");
			content_stream.endText();
		}
		else if(ColorUtils.isOrange(color_data)) {
			//draw 50x50 square with the color in it
			drawColorBox(color_data, LEFT_MARGIN_LG, y_offset, 30, 30, content_stream);
			content_stream.setFont(getBoldFont(), HEADER_4);
			content_stream.setNonStrokingColor(Color.BLACK);
			content_stream.beginText();
			content_stream.newLineAtOffset(180, y_offset+10);
			content_stream.showText("Orange");
			
			content_stream.setFont(getLightFont(), TEXT_SIZE);
			content_stream.newLineAtOffset(-40, -40);
			content_stream.showText("The color of motivation, bringing a positive attitude and general zest");
			content_stream.newLineAtOffset(0, -15);
			content_stream.showText("for life. In general, orange is ideal for bringing comfort in difficult times");
			content_stream.newLineAtOffset(0, -15);
			content_stream.showText("and creating a sense of fun or freedom in your images.");
			content_stream.endText();
		}
		else if(ColorUtils.isGold(color_data)) {
			//draw 50x50 square with the color in it
			drawColorBox(color_data, LEFT_MARGIN_LG, y_offset, 30, 30, content_stream);
			content_stream.setFont(getBoldFont(), HEADER_4);
			content_stream.setNonStrokingColor(Color.BLACK);
			content_stream.beginText();
			content_stream.newLineAtOffset(180, y_offset+10);
			content_stream.showText("Gold");
			
			content_stream.setFont(getLightFont(), TEXT_SIZE);
			content_stream.newLineAtOffset(-40, -40);
			content_stream.showText("The color of success, achievement and triumph. Associated with abundance,");
			content_stream.newLineAtOffset(0, -15);
			content_stream.showText("prosperity, luxury and quality, prestige and sophistication, value and elegance.");
			content_stream.newLineAtOffset(0, -15);
			content_stream.showText("The psychology of this color implies affluence, material wealth and extravagance.");
			content_stream.endText();
		}
		else if(ColorUtils.isYellow(color_data)) {
			//draw 50x50 square with the color in it
			drawColorBox(color_data, LEFT_MARGIN_LG, y_offset, 30, 30, content_stream);
			content_stream.setFont(getBoldFont(), HEADER_4);
			content_stream.setNonStrokingColor(Color.BLACK);
			content_stream.beginText();
			content_stream.newLineAtOffset(180, y_offset+10);
			content_stream.showText("Yellow");
			   
			content_stream.setFont(getLightFont(), TEXT_SIZE);
			content_stream.newLineAtOffset(-40, -40);
			content_stream.showText("Associated with the intellect, logic; it has the ability to improve analytical");
			content_stream.newLineAtOffset(0, -15);
			content_stream.showText("thinking. It is also linked with cheerfulness, happiness, and optimism, and inspires");
			content_stream.newLineAtOffset(0, -15);
			content_stream.showText("hope and enthusiasm. Yellow fosters positive way of thinking, and a thirst for knowledge.");

			content_stream.endText();
		}
		else if(ColorUtils.isGreen(color_data)) {
			//draw 50x50 square with the color in it
			drawColorBox(color_data, LEFT_MARGIN_LG, y_offset, 30, 30, content_stream);
			content_stream.setFont(getBoldFont(), HEADER_4);
			content_stream.setNonStrokingColor(Color.BLACK);
			content_stream.beginText();
			content_stream.newLineAtOffset(180, y_offset+10);
			content_stream.showText("Green");
			
			content_stream.setFont(getLightFont(), TEXT_SIZE);
			content_stream.newLineAtOffset(-40, -40);
			content_stream.showText("Symbolizes harmony, tranquility, and peace. As a soothing, relaxing color, it ");
			content_stream.newLineAtOffset(0, -15);
			content_stream.showText("enhances stability and endurance. It is most often associated with growth and");
			content_stream.newLineAtOffset(0, -15);
			content_stream.showText("renewal, and it promotes optimism, hopefulness, and balance.");

			content_stream.endText();
		}
		else if(ColorUtils.isCyan(color_data)) {
			//draw 50x50 square with the color in it
			drawColorBox(color_data, LEFT_MARGIN_LG, y_offset, 30, 30, content_stream);
			content_stream.setFont(getBoldFont(), HEADER_4);
			content_stream.setNonStrokingColor(Color.BLACK);
			content_stream.beginText();
			content_stream.newLineAtOffset(180, y_offset+10);
			content_stream.showText("Cyan");
			
			content_stream.setFont(getLightFont(), TEXT_SIZE);
			content_stream.newLineAtOffset(-40, -40);
			content_stream.showText("Represents natural and forestial environments and is regarded as the most restful");
			content_stream.newLineAtOffset(0, -15);
			content_stream.showText("and relaxing color for the human eye. Green symbolizes harmony, tranquility, and peace.");
			content_stream.newLineAtOffset(0, -15);
			content_stream.showText("As a soothing, relaxing color, it enhances stability and endurance. It is most often");
			content_stream.newLineAtOffset(0, -15);
			content_stream.showText("associated with growth and renewal, and it promotes optimism, hopefulness, and balance.");

			content_stream.endText();
		}
		else if(ColorUtils.isBlue(color_data)) {
			//draw 50x50 square with the color in it
			drawColorBox(color_data, LEFT_MARGIN_LG, y_offset, 30, 30, content_stream);
			content_stream.setFont(getBoldFont(), HEADER_4);
			content_stream.setNonStrokingColor(Color.BLACK);
			content_stream.beginText();
			content_stream.newLineAtOffset(180, y_offset+10);
			content_stream.showText("Blue");
			
			content_stream.setFont(getLightFont(), TEXT_SIZE);
			content_stream.newLineAtOffset(-40, -40);
			content_stream.showText("The color of trust. Evokes feelings of calm, serenity, loyalty, and integrity. Blue is ");
			content_stream.newLineAtOffset(0, -15);
			content_stream.showText("often described as peaceful, secure and orderly.");

			content_stream.endText();
		}
		else if(ColorUtils.isViolet(color_data)) {
			//draw 50x50 square with the color in it
			drawColorBox(color_data, LEFT_MARGIN_LG, y_offset, 30, 30, content_stream);
			content_stream.setFont(getBoldFont(), HEADER_4);
			content_stream.setNonStrokingColor(Color.BLACK);
			content_stream.beginText();
			content_stream.newLineAtOffset(180, y_offset+10);
			content_stream.showText("Violet");
			
			content_stream.setFont(getLightFont(), TEXT_SIZE);
			content_stream.newLineAtOffset(-40, -40);
			content_stream.showText("The color of spirituality and luxury. This color inpsires reflection and self awareness.");
			content_stream.newLineAtOffset(0, -15);
			content_stream.showText("It is often associated with royalty, quality, and luxury.");


			content_stream.endText();
		}
		else if(ColorUtils.isPurple(color_data)) {
			//draw 50x50 square with the color in it
			drawColorBox(color_data, LEFT_MARGIN_LG, y_offset, 30, 30, content_stream);
			content_stream.setFont(getBoldFont(), HEADER_4);
			content_stream.setNonStrokingColor(Color.BLACK);
			content_stream.beginText();
			content_stream.newLineAtOffset(180, y_offset+10);
			content_stream.showText("Purple");
			
			content_stream.setFont(getLightFont(), TEXT_SIZE);
			content_stream.newLineAtOffset(-40, -40);
			content_stream.showText("The color of spirituality and luxury. This color inpsires reflection and self awareness.");
			content_stream.newLineAtOffset(0, -15);
			content_stream.showText("It is often associated with royalty, quality, and luxury.");

			content_stream.endText();
		}
		else if(ColorUtils.isMagenta(color_data)) {
			//draw 50x50 square with the color in it
			drawColorBox(color_data, LEFT_MARGIN_LG, y_offset, 30, 30, content_stream);
			content_stream.setFont(getBoldFont(), HEADER_4);
			content_stream.setNonStrokingColor(Color.BLACK);
			content_stream.beginText();
			content_stream.newLineAtOffset(180, y_offset+10);
			content_stream.showText("Magenta");
			
			content_stream.setFont(getLightFont(), TEXT_SIZE);
			content_stream.newLineAtOffset(-40, -40);
			content_stream.showText("Represents universal harmony and emotional balance and it promotes compassion, kindness ");
			content_stream.newLineAtOffset(0, -15);
			content_stream.showText("and cooperation. Magenta is a color of cheerfulness, happiness, contentment and appreciation.");
			content_stream.endText();
		}
	}

	/**
	 * Draws boxes with color palette colors as background and hex values as labels
	 * 
	 * @param primary_colors {@link List} of rgb values
	 * @param x_offset x offset of the color palette
	 * @param y_offset y offset of the color palette
	 * @param content_stream {@link PDPageContentStream} to draw the color palette on
	 * 
	 * @throws IOException thrown if there is an error drawing the color palette
	 */
	private void drawColorPaletteColors(List<String> primary_colors, 
										int x_offset, 
										int y_offset, 
									    PDPageContentStream content_stream
	) throws IOException {
		for(int i=0; i< primary_colors.size(); i++) {
			int new_x_offset = x_offset +(i*90);
			drawColorBoxWithLabel(primary_colors.get(i), new_x_offset, y_offset, content_stream);
		}
	}

	/**
	 * Draws a single square and fills it with the rgb value provided as well
	 *   as the hex value of the color
	 *   
	 * @param rgb rgb value of the color
	 * @param x_offset x offset of the color
	 * @param y_offset y offset of the color
	 * @param content_stream {@link PDPageContentStream} to draw the color on
	 * 
	 * @throws IOException thrown if there is an error drawing the color
	 */
	private void drawColorBoxWithLabel(String rgb,
										int x_offset,
										int y_offset,
										PDPageContentStream content_stream
    ) throws IOException {
		ColorData color_data = new ColorData(rgb);
		content_stream.setNonStrokingColor(new Color(color_data.getRed(), 
													 color_data.getGreen(), 
													 color_data.getBlue()));
		content_stream.addRect(x_offset, y_offset, 80, 80);
		content_stream.fill();
		
		content_stream.setFont(getLightFont(), TEXT_SIZE);
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.beginText();
		content_stream.newLineAtOffset(x_offset+20, y_offset-20);
		content_stream.showText(color_data.getHex());
		
		content_stream.endText();
	}
	
	/**
	 * Draws a single square and fills it with the rgb value provided as well
	 *   as the hex value of the color
	 *   
	 * @param color_data {@link ColorData} to draw the color on
	 * @param x_offset x offset of the color
	 * @param y_offset y offset of the color
	 * @param width width of the color
	 * @param height height of the color
	 * @param content_stream {@link PDPageContentStream} to draw the color on
	 * 
	 * @throws IOException thrown if there is an error drawing the color
	 */
	private void drawColorBox(ColorData color_data, 
							  int x_offset, 
							  int y_offset,
							  int width,
							  int height,
							  PDPageContentStream content_stream
    ) throws IOException {
		content_stream.setNonStrokingColor(new Color(color_data.getRed(), 
													 color_data.getGreen(), 
													 color_data.getBlue()));
		content_stream.addRect(x_offset, y_offset, width, height);
		content_stream.fill();
	}

	/**
	 * Creates items with icon and category name for areas of improvement
	 * 
	 * @param content_stream {@link PDPageContentStream} to draw the category on
	 * @param category {@link AuditSubcategory} to draw the category on
	 * @param index index of the category
	 * 
	 * @throws IOException thrown if there is an error drawing the category
	 */
	private void drawImprovementCategory(PDPageContentStream content_stream, 
										AuditSubcategory category, 
										int index
	) throws IOException {
		assert category != null;
		
		int x = 0;
		if(index %2 == 0) {
			x = 80;
		}
		else {
			x = 300;
		}

		int y = 200 - ((int)(Math.round((index+1)/2.0)-1)*70);

		
		if(AuditSubcategory.COLOR_MANAGEMENT.equals(category)) {
			//URL color_management_icon = new URL(COLOR_MANAGEMENT_ICON_PNG_URL);
			//addImageToPage(document, x, y, 0.4f, color_management_icon, content_stream);
			
			content_stream.setFont(getMediumFont(), TEXT_SIZE);
			content_stream.setNonStrokingColor(Color.BLACK);
			content_stream.beginText();
			content_stream.newLineAtOffset(x+40, y+10);
			content_stream.showText("Color Palette");
			content_stream.endText();
		}
		else if(AuditSubcategory.TYPOGRAPHY.equals(category)) {
			//URL typography_icon = new URL(TYPOGRAPHY_ICON_PNG_URL);
			//addImageToPage(document, x, y, 0.4f, typography_icon, content_stream);
			
			content_stream.setFont(getMediumFont(), TEXT_SIZE);
			content_stream.setNonStrokingColor(Color.BLACK);
			content_stream.beginText();
			content_stream.newLineAtOffset(x+40, y+10);
			content_stream.showText("Typeface consistency");
			content_stream.endText();
		}
		else if(AuditSubcategory.IMAGERY.equals(category)) {
			//URL visuals_icon = new URL("https://storage.googleapis.com/look-see-inc-assets/icons/landscape-icon.png");
			//addImageToPage(document, x, y, 0.4f, visuals_icon, content_stream);
			
			content_stream.setFont(getMediumFont(), TEXT_SIZE);
			content_stream.setNonStrokingColor(Color.BLACK);
			content_stream.beginText();
			content_stream.newLineAtOffset(x+40, y+10);
			content_stream.showText("Imagery");
			content_stream.endText();
		}
		else if(AuditSubcategory.WRITTEN_CONTENT.equals(category)) {
			//URL written_content_icon = new URL(WRITTEN_CONTENT_ICON_PNG_URL);
			//addImageToPage(document, x, y, 0.4f, written_content_icon, content_stream);
			
			content_stream.setFont(getMediumFont(), TEXT_SIZE);
			content_stream.setNonStrokingColor(Color.BLACK);
			content_stream.beginText();
			content_stream.newLineAtOffset(x+40, y+10);
			content_stream.showText("Color Palette");
			content_stream.endText();
		}
		else if(AuditSubcategory.BRANDING.equals(category)) {
			//URL branding_icon = new URL(BRANDING_ICON_PNG_URL);
			//addImageToPage(document, x, y, 0.4f, branding_icon, content_stream);
			
			content_stream.setFont(getMediumFont(), TEXT_SIZE);
			content_stream.setNonStrokingColor(Color.BLACK);
			content_stream.beginText();
			content_stream.newLineAtOffset(x+40, y+10);
			content_stream.showText("Branding");
			content_stream.endText();
		}
		else if(AuditSubcategory.INFORMATION_ARCHITECTURE.equals(category)) {
			//URL info_architecture_icon = new URL(INFORMATION_ARCHITECTURE_ICON_PNG_URL);
			//addImageToPage(document, x, y, 0.4f, info_architecture_icon, content_stream);
			
			content_stream.setFont(getMediumFont(), TEXT_SIZE);
			content_stream.setNonStrokingColor(Color.BLACK);
			content_stream.beginText();
			content_stream.newLineAtOffset(x+40, y+10);
			content_stream.showText("Information Architecture");
			content_stream.endText();
		}
		
	}
	
	/** 
	 * Creates score breakdown box with category score details
	 * 
	 * @param score
	 * @param content_stream
	 * @param category
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @throws IOException
	 */
	private void drawScoreBreakdownBox(int score, 
									   PDPageContentStream content_stream,
									   AuditSubcategory category,
									   int x, 
									   int y, 
									   int width, 
									   int height
    ) throws IOException {
		//create overall score box with rounded edges, drop shadow and progress style bar
		//URL score_overview_card = new URL(SCORE_OVERVIEW_CARD_PNG_URL);
		//addImageToPage(document, x, y, 1.0f, score_overview_card, content_stream);
		
		content_stream.setFont(getMediumFont(), HEADER_1);
		content_stream.setNonStrokingColor(getScoreColor(score));
		content_stream.beginText();
		content_stream.newLineAtOffset(x+20, y+25);
		content_stream.showText(score+"%");
		
		writeCategoryInfoToBreakdownBox(content_stream, category);
		
		content_stream.endText();
	}

	/**
	 * Fills content for score breakdown box based on category and score
	 * 
	 * @param content_stream
	 * @param category
	 * @throws IOException
	 */
	private void writeCategoryInfoToBreakdownBox(
								PDPageContentStream content_stream, 
								AuditSubcategory category
	) throws IOException {
		if(AuditSubcategory.COLOR_MANAGEMENT.equals(category)) {
			content_stream.setFont(getMediumFont(), TEXT_SIZE);
			content_stream.setNonStrokingColor(Color.BLACK);
			content_stream.newLineAtOffset( 100, 15);
			content_stream.showText("Color");
			content_stream.newLineAtOffset( 0, -15);
			content_stream.showText("Management");
			
			content_stream.setFont(getLightFont(), TEXT_SIZE);
			content_stream.newLineAtOffset( 100, HEADER_2);
			content_stream.showText("This score indicates that you have room for");
			content_stream.newLineAtOffset( 0, -15);
			content_stream.showText("improvement on the color scheme and color");
			content_stream.newLineAtOffset( 0, -15);
			content_stream.showText("contrast on your website. Read page 11 for");
			content_stream.newLineAtOffset( 0, -15);
			content_stream.showText("details");
		}
		else if(AuditSubcategory.WRITTEN_CONTENT.equals(category)) {
			content_stream.setFont(getMediumFont(), TEXT_SIZE);
			content_stream.setNonStrokingColor(Color.BLACK);
			content_stream.newLineAtOffset( 100, 15);
			content_stream.showText("Written");
			content_stream.newLineAtOffset( 0, -15);
			content_stream.showText("Content");
			
			content_stream.setFont(getLightFont(), TEXT_SIZE);
			content_stream.newLineAtOffset( 100, HEADER_2);
			content_stream.showText("Your content is well-written and organized,");
			content_stream.newLineAtOffset( 0, -15);
			content_stream.showText("but can be difficult to understand for the");
			content_stream.newLineAtOffset( 0, -15);
			content_stream.showText("general audience. Read page 21 for more on");
			content_stream.newLineAtOffset( 0, -15);
			content_stream.showText("this.");
		}
		else if(AuditSubcategory.BRANDING.equals(category)) {
			content_stream.setFont(getMediumFont(), TEXT_SIZE);
			content_stream.setNonStrokingColor(Color.BLACK);
			content_stream.newLineAtOffset( 100, 7);
			content_stream.showText("Branding");
			
			content_stream.setFont(getLightFont(), TEXT_SIZE);
			content_stream.newLineAtOffset( 100, 7);
			content_stream.showText("Optimize your logo placement for the best");
			content_stream.newLineAtOffset( 0, -15);
			content_stream.showText("branding practices. More details on page 31.");
		}
		else if(AuditSubcategory.TYPOGRAPHY.equals(category)) {
			content_stream.setFont(getMediumFont(), TEXT_SIZE);
			content_stream.setNonStrokingColor(Color.BLACK);
			content_stream.newLineAtOffset( 100, 10);
			content_stream.showText("Typography");
			
			content_stream.setFont(getLightFont(), TEXT_SIZE);
			content_stream.newLineAtOffset( 100, HEADER_2);
			content_stream.showText("This is your biggest improvement area.");
			content_stream.newLineAtOffset( 0, -15);
			content_stream.showText("Maintain consistency in font styling and");
			content_stream.newLineAtOffset( 0, -15);
			content_stream.showText("make other adjustments to improve this");
			content_stream.newLineAtOffset( 0, -15);
			content_stream.showText("element of experience. More on page 37.");
		}
		else if(AuditSubcategory.IMAGERY.equals(category)) {
			content_stream.setFont(getMediumFont(), TEXT_SIZE);
			content_stream.setNonStrokingColor(Color.BLACK);
			content_stream.newLineAtOffset( 100, 7);
			content_stream.showText("Visuals");
			
			content_stream.setFont(getLightFont(), TEXT_SIZE);
			content_stream.newLineAtOffset( 100, 13);
			content_stream.showText("This area follows good practices, but does");
			content_stream.newLineAtOffset( 0, -15);
			content_stream.showText("not comply with accessibility guidelines.");
			content_stream.newLineAtOffset( 0, -15);
			content_stream.showText("Learn how to change this on page 43.");
		}
		else if(AuditSubcategory.INFORMATION_ARCHITECTURE.equals(category)) {
			content_stream.setFont(getMediumFont(), TEXT_SIZE);
			content_stream.setNonStrokingColor(Color.BLACK);
			content_stream.newLineAtOffset( 100, 15);
			content_stream.showText("Information");
			content_stream.newLineAtOffset( 0, -15);
			content_stream.showText("Architecture");
			
			content_stream.setFont(getLightFont(), TEXT_SIZE);
			content_stream.newLineAtOffset( 100, HEADER_2);
			content_stream.showText("A few adjustments around your CTA");
			content_stream.newLineAtOffset( 0, -15);
			content_stream.showText("buttons, Interactions, and Layout can");
			content_stream.newLineAtOffset( 0, -15);
			content_stream.showText("perfect your score in this category! Learn");
			content_stream.newLineAtOffset( 0, -15);
			content_stream.showText("more on page 51.");
		}
	}

	private void drawScoreStatusBox(int score, 
									PDPageContentStream content_stream, 
									int x, 
									int y, 
									int width, 
									int height
	) throws IOException {
		//create overall score box with rounded edges, drop shadow and progress style bar
		//URL score_overview_card = new URL(SCORE_OVERVIEW_CARD_PNG_URL);
		//addImageToPage(document, x, y, 1.0f, score_overview_card, content_stream);
		
		URL emoji = getScoreEmojiImage(score);
		addImageToPage(document, x+40, y+LEFT_MARGIN_LG, 0.4f, emoji, content_stream);
		
		content_stream.beginText();
		content_stream.setNonStrokingColor(Color.BLACK);
		content_stream.setFont(getBoldFont(), TEXT_SIZE);
		content_stream.newLineAtOffset(x+LEFT_MARGIN_LG, y+150);
		content_stream.showText(getScoreText(score));
		
		content_stream.setNonStrokingColor(getScoreColor(score));
		content_stream.setFont(getBoldFont(), HEADER_2);
		content_stream.newLineAtOffset(0, -30);
		content_stream.showText(getScoreCategoryText(score));
		content_stream.endText();
		
		drawProgressBar(score, content_stream, x+40, y+90, width-50, 10);
		
		
	}

	/**
	 * Generates a progress bar that reflects the score and uses appropriate colors
	 * 
	 * @param overall_score
	 * @param content_stream
	 * @throws IOException 
	 */
	private void drawProgressBar(int score, 
								PDPageContentStream content_stream, 
								int x, 
								int y, 
								int width, 
								int height
	) throws IOException {
		content_stream.setNonStrokingColor(new Color(211, 211, 211));
		content_stream.addRect(x, y, width, height);
		content_stream.fill();
		
		content_stream.setNonStrokingColor(getScoreColor(score));
		content_stream.addRect(x, y, (int)(width*(score/100.0)), height);
		content_stream.fill();
		
		//URL up_caret =new URL(UP_CARET_PNG_URL);
		//addImageToPage(document, x+(int)(width*(score/100.0))-15, y-30, 0.25f, up_caret, content_stream);
		
		content_stream.setFont(getBaseFont(), HEADER_2);
		content_stream.beginText();
		
		int offset = 0;
		if(score < 10) {
			offset = 10;
		}
		else {
			offset = 15;
		}
		
		content_stream.newLineAtOffset(x+(int)(width*(score/100.0))-offset, y-50);
		content_stream.showText(score+"%");
		content_stream.endText();
	}

	private String getScoreCategoryText(int overall_score) {
		if(overall_score >= 80.0) {
			return "Delightful";
		}
		else if(overall_score >= 60.0 && overall_score < 80.0) {
			return "Almost There";
		}
		else {
			return "Needs Work";
		}
	}

	private Color getScoreColor(int overall_score) {
		if(overall_score >= 80.0) {
			return new Color(35, 216, 164);
		}
		else if(overall_score >= 60.0 && overall_score < 80.0) {
			return new Color(249, 191, 7);
		}
		else {
			return new Color(57, 183, 255);
		}
	}

	private String getScoreText(int overall_score) {
		if(overall_score >= 80.0) {
			return "80% to 100%";
		}
		else if(overall_score >= 60.0 && overall_score < 80.0) {
			return "60% to 80%";
		}
		else {
			return "Less than 60%";
		}
	}

	private URL getScoreEmojiImage(int overall_score) throws MalformedURLException {
		if(overall_score >= 80.0) {
			//return new URL("https://storage.googleapis.com/look-see-inc-assets/icons/C-Happy-Face-128px.png");
			//return new URL(HAPPY_FACE_PNG_URL);
			return new URL("");
		}
		else if(overall_score >= 60.0 && overall_score < 80.0) {
			//return new URL("https://storage.googleapis.com/look-see-inc-assets/icons/C-Average-Face-128px-2.png");
			//return new URL(AVERAGE_FACE_PNG_URL);
			return new URL("");
		}
		else {
			//return new URL("https://storage.googleapis.com/look-see-inc-assets/icons/C-Sad-Face-128px.png");
			//return new URL(SAD_FACE_PNG_URL);
			return new URL("");
		}
	}

	private void addPage(PDPage page) {
		document.addPage(page);
	}
	
	public static void addImageToPage(PDDocument document, 
										int x, 
										int y, 
										float scale, 
										URL image_url, 
										PDPageContentStream contentStream
	) throws IOException {
	    BufferedImage tmp_image = ImageUtils.readImageFromURL(image_url);
	    BufferedImage image = new BufferedImage(tmp_image.getWidth(), tmp_image.getHeight(),
	            BufferedImage.TYPE_4BYTE_ABGR);
	    image.createGraphics().drawRenderedImage(tmp_image, null);
	    byte[] byte_array = ImageUtils.toByteArray(image, "png");
	    
	    PDImageXObject ximage = PDImageXObject.createFromByteArray(document, byte_array, "pdf background image");
	    contentStream.drawImage(ximage, x, y, ximage.getWidth() * scale, ximage.getHeight() * scale);
	}
	
	/**
	 * Writes document to file and closes the PDF writer stream
	 * 
	 * @throws IOException
	 */
	public void writeToDoc() throws IOException {
		document.save(document_name);
		document.close();
	}
	
	//GETTERS AND SETTERS
	public PDDocument getDocument() {
		return document;
	}

	public void setDocument(PDDocument document) {
		this.document = document;
	}

	public String getDocumentName() {
		return document_name;
	}

	public void setDocumentName(String doc_name) {
		this.document_name = doc_name;
	}

	public PDFont getBaseFont() {
		return base_font;
	}

	public void setBaseFont(PDFont pdfFont) {
		this.base_font = pdfFont;
	}

	public PDFont getBoldFont() {
		return bold_font;
	}

	public void setBoldFont(PDFont bold_font) {
		this.bold_font = bold_font;
	}

	public PDFont getLightFont() {
		return light_font;
	}

	public void setLightFont(PDFont light_font) {
		this.light_font = light_font;
	}

	public PDFont getMediumFont() {
		return medium_font;
	}

	public void setMediumFont(PDFont medium_font) {
		this.medium_font = medium_font;
	}

	public void write(ByteArrayOutputStream outputStream) throws IOException {
		document.save(outputStream);
		document.close();
	}
	
}
