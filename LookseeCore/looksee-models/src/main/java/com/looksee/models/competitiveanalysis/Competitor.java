package com.looksee.models.competitiveanalysis;

import com.looksee.models.LookseeObject;
import com.looksee.models.competitiveanalysis.brand.Brand;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * Defines the type of package paid for, which domains are registered and which Users belong to the account
 *
 * invariant: companyName != null after parameterized construction
 * invariant: url != null after parameterized construction
 * invariant: industry != null after parameterized construction
 */
@Getter
@Setter
@NoArgsConstructor
@Node
public class Competitor extends LookseeObject{

	private String companyName;
	private String url;
	private String industry;
	
	@Relationship("USES")
	private Brand brand;

	/**
	 * Constructs a new competitor
	 *
	 * @param company_name the name of the company
	 * @param url the URL of the company
	 * @param industry the industry of the company
	 *
	 * precondition: company_name != null
	 * precondition: url != null
	 * precondition: industry != null
	 */
	public Competitor(
			String company_name,
			String url,
			String industry
	){
		super();

		assert company_name != null;
		assert url != null;
		assert industry != null;

		setCompanyName(company_name);
		setUrl(url);
		setIndustry(industry);
	}

	/**
	 * Generates a key for the competitor
	 *
	 * @return the key
	 */
	@Override
	public String generateKey() {
		return "competitor::" + org.apache.commons.codec.digest.DigestUtils.sha256Hex( this.getUrl() );
	}
}
