package com.looksee.services;

import com.looksee.models.dto.UXIssueReportDto;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Service for generating reports
 */
public class ReportService {

	/**
	 * Generates an Excel spreadsheet for a list of audit messages
	 * @param audit_messages the list of audit messages
	 * @param url the url of the page
	 * @return the Excel spreadsheet
	 * @throws FileNotFoundException if the file is not found
	 * @throws IOException if an error occurs while writing the file
	 *
	 * precondition: audit_messages != null
	 * precondition: url != null
	 */
	public static XSSFWorkbook generateExcelSpreadsheet(
			List<UXIssueReportDto> audit_messages,
			URL url
	) throws FileNotFoundException, IOException {
		assert audit_messages != null;
		assert url != null;
		
		XSSFWorkbook workbook = new XSSFWorkbook();

		String workbook_name = url.getHost();
        Sheet sheet = workbook.createSheet(workbook_name);
        int rowCount = 0;
        
        //write header
        XSSFCellStyle header_cell_style = workbook.createCellStyle();
        header_cell_style.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        header_cell_style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row header_row = sheet.createRow(rowCount);
        header_row.setRowStyle(header_cell_style);

        //write object to cells in order of category, issue, why it matters,
        //recommendations, ada compliance, and priority
        Cell category_header_cell = header_row.createCell(0);
        category_header_cell.setCellValue("Category");
        
        Cell description_header_cell = header_row.createCell(1);
        description_header_cell.setCellValue("Description");
        
        Cell why_it_matters_header_cell = header_row.createCell(2);
        why_it_matters_header_cell.setCellValue("Why it matters");
        
        Cell recommendation_header_cell = header_row.createCell(3);
        recommendation_header_cell.setCellValue("Recommendation");
        
        Cell wcag_header_cell = header_row.createCell(4);
        wcag_header_cell.setCellValue("WCAG/ADA compliance");
        
        Cell priority_header_cell = header_row.createCell(5);
        priority_header_cell.setCellValue("Priority");
        
        Cell selector_header_cell = header_row.createCell(6);
        selector_header_cell.setCellValue("Element CSS Selector");
        
        sheet.setColumnWidth(0, 30 * 256);
        sheet.setColumnWidth(1, 10 * 256);
        sheet.setColumnWidth(2, 40 * 256);
        sheet.setColumnWidth(3, 30 * 256);
        sheet.setColumnWidth(4, 30 * 256);
        sheet.setColumnWidth(5, 30 * 256);
        sheet.setColumnWidth(6, 10 * 256);
        sheet.setColumnWidth(7, 40 * 256);

        
        XSSFCellStyle wrapped_text_cell_style = workbook.createCellStyle();
        wrapped_text_cell_style.setWrapText(true);

        for(UXIssueReportDto msg : audit_messages) {
            Row row = sheet.createRow(++rowCount);
            row.setRowStyle(wrapped_text_cell_style);
            row.setHeight((short)2000);

            //write object to cells in order of category, issue, why it matters, recommendations, ada compliance, and priority
            Cell category_cell = row.createCell(0);
            category_cell.setCellValue(msg.getCategory().toString());
            
            Cell description_cell = row.createCell(1);
            description_cell.setCellValue(msg.getDescription());
            
            Cell why_it_matters_cell = row.createCell(2);
            why_it_matters_cell.setCellValue(msg.getWhyItMatters());
            
            Cell recommendation_cell = row.createCell(3);
            recommendation_cell.setCellValue(msg.getRecommendation());
            
            Cell wcag_cell = row.createCell(4);
            wcag_cell.setCellValue(msg.getWcagCompliance());
            
            Cell priority_cell = row.createCell(5);
            priority_cell.setCellValue(msg.getPriority().toString());
            
            Cell css_selector_cell = row.createCell(6);
            css_selector_cell.setCellValue("CSS selector goes here");
        }
        
        return workbook;
	}
	
	/**
	 * Generates an Excel spreadsheet for a list of UX issues
	 * @param ux_issues the list of UX issues
	 * @param url the url of the page
	 * @return the Excel spreadsheet
	 * @throws FileNotFoundException if the file is not found
	 * @throws IOException if an error occurs while writing the file
	 *
	 * precondition: ux_issues != null
	 * precondition: url != null
	 */
	public static XSSFWorkbook generateDomainExcelSpreadsheet(
			List<UXIssueReportDto> ux_issues,
			URL url
	) throws FileNotFoundException, IOException {
		assert ux_issues != null;
		assert url != null;
		
		XSSFWorkbook workbook = new XSSFWorkbook();

		String workbook_name = url.getHost();
        Sheet sheet = workbook.createSheet(workbook_name);
        int rowCount = 0;
        
        //write header
        XSSFCellStyle header_cell_style = workbook.createCellStyle();
        header_cell_style.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        header_cell_style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row header_row = sheet.createRow(rowCount);
        header_row.setRowStyle(header_cell_style);
        //write object to cells in order of category, issue, why it matters, recommendations, ada compliance, and priority
        Cell url_header_cell = header_row.createCell(0);
        url_header_cell.setCellValue("Page URL");
        
        Cell category_header_cell = header_row.createCell(1);
        category_header_cell.setCellValue("Category");
        
        Cell description_header_cell = header_row.createCell(2);
        description_header_cell.setCellValue("Description");
        
        Cell why_it_matters_header_cell = header_row.createCell(3);
        why_it_matters_header_cell.setCellValue("Why it matters");
        
        Cell recommendation_header_cell = header_row.createCell(4);
        recommendation_header_cell.setCellValue("Recommendation");
        
        Cell wcag_header_cell = header_row.createCell(5);
        wcag_header_cell.setCellValue("WCAG/ADA compliance");
        
        Cell priority_header_cell = header_row.createCell(6);
        priority_header_cell.setCellValue("Priority");
        
        Cell selector_header_cell = header_row.createCell(7);
        selector_header_cell.setCellValue("Element CSS Selector");
        
        sheet.setColumnWidth(0, 30 * 256);
        sheet.setColumnWidth(1, 10 * 256);
        sheet.setColumnWidth(2, 40 * 256);
        sheet.setColumnWidth(3, 30 * 256);
        sheet.setColumnWidth(4, 30 * 256);
        sheet.setColumnWidth(5, 30 * 256);
        sheet.setColumnWidth(6, 10 * 256);
        sheet.setColumnWidth(7, 40 * 256);

        
        XSSFCellStyle wrapped_text_cell_style = workbook.createCellStyle();
        wrapped_text_cell_style.setWrapText(true);

        for(UXIssueReportDto msg : ux_issues) {
            Row row = sheet.createRow(++rowCount);
            row.setRowStyle(wrapped_text_cell_style);
            row.setHeight((short)2000);
        
            Cell url_cell = row.createCell(0);
            url_cell.setCellValue(msg.getPageUrl());
            
            //write object to cells in order of category, issue, why it matters,
            //recommendations, ada compliance, and priority
            Cell category_cell = row.createCell(1);
            category_cell.setCellValue(msg.getCategory().toString());
            
            Cell description_cell = row.createCell(2);
            description_cell.setCellValue(msg.getDescription());
            
            Cell why_it_matters_cell = row.createCell(3);
            why_it_matters_cell.setCellValue(msg.getWhyItMatters());
            
            Cell recommendation_cell = row.createCell(4);
            recommendation_cell.setCellValue(msg.getRecommendation());
            
            Cell wcag_cell = row.createCell(5);
            wcag_cell.setCellValue(msg.getWcagCompliance());
            
            Cell priority_cell = row.createCell(6);
            priority_cell.setCellValue(msg.getPriority().toString());
            
            Cell css_selector_cell = row.createCell(7);
            css_selector_cell.setCellValue(msg.getElementSelector());
        }
        
        return workbook;
	}
}
