package com.looksee.models.dto;

import com.looksee.models.competitiveanalysis.Competitor;
import com.looksee.models.competitiveanalysis.brand.Brand;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * DTO for {@link Competitor}
 */
@Getter
@Setter
@NoArgsConstructor
public class CompetitorDto {
	private long id;
	private String companyName;
	private String url;
	private String industry;
	private boolean analysisRunning;
	private Brand brand;
	
	/**
	 * Constructor for {@link CompetitorDto}
	 * 
	 * @param id id of the competitor
	 * @param company_name name of the competitor
	 * @param url url of the competitor
	 * @param industry industry of the competitor
	 * @param analysis_running true if analysis is running for the competitor, false otherwise
	 * @param brand {@link Brand} of the competitor
	 * 
	 * precondition: company_name != null
	 * precondition: url != null
	 * precondition: industry != null
	 * precondition: brand != null
	 */
	public CompetitorDto(
			long id,
			String company_name,
			String url,
			String industry,
			boolean analysis_running,
			Brand brand
	){
		assert company_name != null;
		assert url != null;
		assert industry != null;
		assert brand != null;

		setId(id);
		setCompanyName(company_name);
		setUrl(url);
		setIndustry(industry);
		setAnalysisRunning(analysis_running);
		setBrand(brand);
	}
}
