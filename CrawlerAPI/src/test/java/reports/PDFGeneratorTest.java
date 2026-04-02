package reports;

public class PDFGeneratorTest {
	/*

	@Test
	public void tableOfContentsPageTest() throws IOException, URISyntaxException {
		GeneratePDFReport pdf_report = new GeneratePDFReport("look-see");
		
		pdf_report.generateTableOfContents();
		pdf_report.writeToDoc();
	}
	
	@Test
	public void coverPageTest() throws IOException, URISyntaxException {
		GeneratePDFReport pdf_report = new GeneratePDFReport("look-see");
		
		pdf_report.generateCoverPage("Acme.com");
		pdf_report.writeToDoc();
	}
	@Test
	public void welcomePageTest() throws IOException, URISyntaxException {
		GeneratePDFReport pdf_report = new GeneratePDFReport("look-see");
		
		pdf_report.generateWelcomePage("Acme.com", 50);
		pdf_report.writeToDoc();
	}
	
	@Test
	public void scoringPageTest() throws IOException, URISyntaxException {
		GeneratePDFReport pdf_report = new GeneratePDFReport("look-see");
		
		pdf_report.generateScoringPage("Acme.com");
		pdf_report.writeToDoc();
	}
	
	@Test
	public void scoreOverviewTest() throws IOException, URISyntaxException {
		GeneratePDFReport pdf_report = new GeneratePDFReport("look-see");
		
		pdf_report.generateScoreOverviewPage(77, "Acme.com", new ArrayList<>());
		pdf_report.writeToDoc();
	}
	
	@Test
	public void scoreBreakdownTest() throws IOException, URISyntaxException {
		GeneratePDFReport pdf_report = new GeneratePDFReport("look-see");
		
		pdf_report.generateScoreBreakdownPage(77, "Acme.com", 80, 48, 61, 94, 77);
		pdf_report.writeToDoc();
	}
	
	@Test
	public void colorManagementCoverTest() throws IOException, URISyntaxException {
		GeneratePDFReport pdf_report = new GeneratePDFReport("look-see");
		
		pdf_report.generateColorManagementCoverPage(77, "Acme.com");
		pdf_report.writeToDoc();
	}
	
	@Test
	public void colorManagementColorPaletteReportTest() throws IOException, URISyntaxException {
		GeneratePDFReport pdf_report = new GeneratePDFReport("look-see");
		
		List<String> colors = new ArrayList<>();
		colors.add("255, 0, 80");
		colors.add("35, 31, 32");
		colors.add("57, 183, 255");
		colors.add("249, 191, 7");
		colors.add("35, 216, 164");
		
		pdf_report.generateColorManagementColorPalettePage(77, "Acme.com", colors);
		pdf_report.writeToDoc();
	}
	
	
	@Test
	public void colorManagementColorPalettePsychologyPageTest() throws IOException, URISyntaxException {
		GeneratePDFReport pdf_report = new GeneratePDFReport("look-see");
		
		List<String> colors = new ArrayList<>();
		colors.add("255, 0, 80");
		colors.add("35, 31, 32");
		colors.add("57, 183, 255");
		colors.add("249, 191, 7");
		colors.add("35, 216, 164");
		
		pdf_report.generateColorPsychologyDescriptionPage(colors);
		pdf_report.writeToDoc();
	}
	
	@Test
	public void colorManagementColorPaletteWhyItMattersPageTest() throws IOException, URISyntaxException {
		GeneratePDFReport pdf_report = new GeneratePDFReport("look-see");
		
		pdf_report.generateColorPaletteWhyItMattersPage();
		pdf_report.writeToDoc();
	}
	
	@Test
	public void colorManagementTextContrastDescriptionTest() throws IOException, URISyntaxException {
		GeneratePDFReport pdf_report = new GeneratePDFReport("look-see");
		
		pdf_report.generateColorManagementTextColorContrastPage(69, "acme.com", "", 84, 8, 22);
		pdf_report.generateTextColorContrastWhyItMattersPage();
		pdf_report.writeToDoc();
	}
	
	
	
	@Test
	public void generateFullReportTest() throws IOException, URISyntaxException {
		GeneratePDFReport pdf_report = new GeneratePDFReport("look-see");
		
		List<AuditSubcategory> needs_improvement = new ArrayList<>();
		needs_improvement.add(AuditSubcategory.COLOR_MANAGEMENT);
		needs_improvement.add(AuditSubcategory.TYPOGRAPHY);
		needs_improvement.add(AuditSubcategory.IMAGERY);
		needs_improvement.add(AuditSubcategory.WRITTEN_CONTENT);
		
		pdf_report.generateCoverPage("Acme.com");
		pdf_report.generateTableOfContents();
		pdf_report.generateWelcomePage("Acme.com", 50);
		pdf_report.generateScoringPage("Acme.com");
		pdf_report.generateAuditCategoryDescriptionPage();
		pdf_report.generateScoreOverviewPage(77, "Acme.com", needs_improvement);
		pdf_report.generateScoreBreakdownPage(77, "Acme.com", 80, 48, 61, 94, 77);
		pdf_report.generateColorManagementCoverPage(80, "Acme.com");
		
		List<String> colors = new ArrayList<>();
		colors.add("255, 0, 80");
		colors.add("35, 31, 32");
		colors.add("57, 183, 255");
		colors.add("249, 191, 7");
		colors.add("35, 216, 164");
		
		pdf_report.generateColorManagementColorPalettePage(98, "Acme.com", colors);
		pdf_report.generateColorPsychologyDescriptionPage(colors);
		pdf_report.generateColorManagementTextColorContrastPage(69, "acme.com", "", 84, 8, 22);
		pdf_report.generateTextColorContrastWhyItMattersPage();
		
		pdf_report.generateNonTextColorContrastPage(67, "Acme.com", 23);
		pdf_report.generateNonTextColorContrastWhyItMattersPage();
		
		pdf_report.generateWrittenContentCoverPage(82, "Acme.com");
		pdf_report.generateWrittenContentEaseOfUnderstandingPage(59, "Acme.com", "easy", "college" ,13);
		pdf_report.generateEaseOfReadingWhyItMattersPage();
		pdf_report.generateWrittenContentParagraphingPage(80, "Acme.com", 11, 15, 15);
		pdf_report.generateParagraphingWhyItMattersPage();
		
		
		//Visuals
		pdf_report.generateVisualsCoverPage(68, "Acme.com");
		pdf_report.generateVisualsImageryPage(68, "Acme.com", 67, 12, 5, WCAGComplianceLevel.AA);
		pdf_report.generateImageryWhyItMattersPage();
		
		pdf_report.generateAppendixCoverPage();
		pdf_report.writeToDoc();
	}
		*/
}
