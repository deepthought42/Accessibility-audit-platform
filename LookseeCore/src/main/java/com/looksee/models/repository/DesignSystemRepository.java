package com.looksee.models.repository;

import com.looksee.models.designsystem.DesignSystem;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Spring Data Neo4j to handle interactions with
 * {@link DesignSystem} objects
 */
@Repository
@Retry(name = "neoforj")
public interface DesignSystemRepository extends Neo4jRepository<DesignSystem, Long> {
	
	/**
	 * Updates the audience proficiency of a design system
	 *
	 * @param domain_id the ID of the design system
	 * @param audience_proficiency the new audience proficiency
	 * @return the updated design system
	 */
	@Query("MATCH (setting:DesignSystem) WHERE id(setting)=$id SET setting.audienceProficiency=$audience_proficiency RETURN setting")
	public DesignSystem updateExpertiseSetting(@Param("id") long domain_id, @Param("audience_proficiency") String audience_proficiency);

	/**
	 * Retrieves a design system associated with an audit record
	 *
	 * @param audit_record_id the ID of the audit record
	 * @return the design system associated with the audit record
	 */
	@Query("MATCH (audit_record:AuditRecord) WITH audit_record MATCH (audit_record)-[:DETECTED]->(design_system:DesignSystem) WHERE id(audit_record)=$audit_record_id RETURN design_system")
	public Optional<DesignSystem> getDesignSystem(@Param("audit_record_id") long audit_record_id);

	/**
	 * Retrieves a design system associated with a domain
	 *
	 * @param domain_id the ID of the domain
	 * @return the design system associated with the domain
	 */
	@Query("MATCH (domain:Domain)-[:USES]->(ds:DesignSystem) WHERE id(domain)=$domain_id RETURN ds LIMIT 1")
	public Optional<DesignSystem> getDesignSystemForDomain(@Param("domain_id") long domain_id);

	/**
	 * Adds a design system to a domain
	 *
	 * @param domain_id the ID of the domain
	 * @param design_system_id the ID of the design system
	 * @return the design system that was added to the domain
	 */
	@Query("MATCH (d:Domain) WITH d MATCH (design:DesignSystem) WHERE id(d)=$domain_id AND id(design)=$design_system_id MERGE (d)-[:USES]->(design) RETURN design")
	public DesignSystem addDesignSystem(@Param("domain_id") long domain_id, @Param("design_system_id") long design_system_id);

	/**
	 * Updates the audience proficiency of a design system for a domain
	 *
	 * @param domain_id the ID of the domain
	 * @param audience_proficiency the new audience proficiency
	 * @return the updated design system
	 */
	@Query("MATCH (d:Domain)-[:USES]->(setting:DesignSystem) WHERE id(d)=$domain_id SET setting.audience_proficiency=$audience_proficiency RETURN setting")
	public DesignSystem updateExpertiseSettingForDomain(@Param("domain_id") long domain_id, @Param("audience_proficiency") String audience_proficiency);
	
	/**
	 * Updates the WCAG compliance level of a design system for a domain
	 *
	 * @param domain_id the ID of the domain
	 * @param wcag_level the new WCAG compliance level
	 * @return the updated design system
	 */
	@Query("MATCH (d:Domain)-[]->(setting:DesignSystem) WHERE id(d)=$domain_id SET setting.wcag_compliance_level=$wcag_level RETURN setting")
	public DesignSystem updateWcagSettings(@Param("domain_id") long domain_id, @Param("wcag_level") String wcag_level);

	/**
	 * Updates the allowed image characteristics of a design system for a domain
	 *
	 * @param domain_id the ID of the domain
	 * @param allowed_image_characteristics the new allowed image characteristics
	 * @return the updated design system
	 */
	@Query("MATCH (d:Domain)-[]->(setting:DesignSystem) WHERE id(d)=$domain_id SET setting.allowed_image_characteristics=$image_characteristics RETURN setting")
	public DesignSystem updateAllowedImageCharacteristics(@Param("domain_id") long domain_id, @Param("image_characteristics") List<String> allowed_image_characteristics);
}
