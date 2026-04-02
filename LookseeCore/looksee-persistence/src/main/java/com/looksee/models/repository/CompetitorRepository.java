package com.looksee.models.repository;

import com.looksee.models.competitiveanalysis.Competitor;
import com.looksee.models.competitiveanalysis.brand.Brand;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Spring Data Neo4j to handle interactions with {@link Competitor} objects
 */
@Repository
@Retry(name = "neoforj")
public interface CompetitorRepository extends Neo4jRepository<Competitor, Long> {

	/**
	 * Adds a brand to a competitor.
	 *
	 * @param competitor_id the ID of the competitor
	 * @param brand_id the ID of the brand
	 */
	@Query("MATCH (c:Competitor), (brand:Brand) WHERE id(c)=$competitor_id AND id(brand)=$brand_id MERGE (c)-[r:USES]->(brand) return brand")
	public void addBrand(@Param("competitor_id") long competitor_id, @Param("brand_id") long brand_id);

	/**
	 * Retrieves the most recently created brand associated with a competitor.
	 *
	 * @param competitor_id the ID of the competitor
	 * @return the most recent brand used by the competitor, ordered by creation date
	 */
	@Query("MATCH (c:Competitor)-[r:USES]->(brand:Brand) WHERE id(c)=$competitor_id RETURN brand ORDER BY brand.created_at DESC LIMIT 1")
	public Brand getMostRecentBrand(@Param("competitor_id") long competitor_id);
	
	/**
	 * Retrieves all competitors associated with a domain.
	 *
	 * @param domain_id the ID of the domain
	 * @return list of all competitors associated with the domain
	 */
	@Query("MATCH (domain:Domain)-[]->(c:Competitor) WHERE id(domain)=$domain_id RETURN c")
	public List<Competitor> getCompetitors(@Param("domain_id") long domain_id);

	/**
	 * Adds a competitor relationship to a domain if it doesn't already exist.
	 *
	 * @param domain_id the ID of the domain
	 * @param competitor_id the ID of the competitor to add
	 * @return the competitor that was added to the domain
	 */
	@Query("MATCH (d:Domain) WITH d MATCH (competitor:Competitor) WHERE id(d)=$domain_id AND id(competitor)=$competitor_id MERGE (d)-[:COMPETES_WITH]->(competitor) RETURN competitor")
	public Competitor addCompetitor(@Param("domain_id") long domain_id, @Param("competitor_id") long competitor_id);
}
