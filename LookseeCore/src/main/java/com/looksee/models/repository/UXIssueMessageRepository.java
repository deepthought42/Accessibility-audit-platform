package com.looksee.models.repository;

import com.looksee.models.ElementState;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.models.enums.AuditName;
import java.util.Set;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Spring Data Neo4j to handle interactions with
 * {@link UXIssueMessage} objects
 */
@Repository
public interface UXIssueMessageRepository extends Neo4jRepository<UXIssueMessage, Long>  {
	
	/**
	 * Finds a UX issue message by its key
	 *
	 * @param key the key of the UX issue message
	 * @return the UX issue message
	 */
	public UXIssueMessage findByKey(@Param("key") String key);

	/**
	 * Finds an element for a UX issue message
	 *
	 * @param id the ID of the UX issue message
	 * @return the element
	 */
	@Query("MATCH (uim:UXIssueMessage)-[:FOR]->(e:ElementState) WHERE id(uim)=$id RETURN e")
	public ElementState getElement(@Param("id") long id);

	/**
	 * Finds a good example for a UX issue message
	 *
	 * @param issue_id the ID of the UX issue message
	 * @return the element
	 */
	@Query("MATCH (uim:UXIssueMessage)-[:EXAMPLE]->(e:ElementState) WHERE id(uim)=$issue_id RETURN e")
	public ElementState getGoodExample(@Param("issue_id") long issue_id);

	/**
	 * Adds an element to a UX issue message
	 *
	 * @param issue_id the ID of the UX issue message
	 * @param element_id the ID of the element
	 */
	@Query("MATCH (uim:UXIssueMessage) WITH uim MATCH (e:ElementState) WHERE id(uim)=$issue_id AND id(e)=$element_id MERGE (uim)-[r:FOR]->(e) RETURN r")
	public void addElement(@Param("issue_id") long issue_id, @Param("element_id") long element_id);

	/**
	 * Adds a page to a UX issue message
	 *
	 * @param issue_id the ID of the UX issue message
	 * @param page_id the ID of the page
	 */
	@Query("MATCH (uim:UXIssueMessage) WITH uim MATCH (e:PageState) WHERE id(uim)=$issue_id AND id(e)=$page_id MERGE (uim)-[r:FOR]->(e) RETURN r")
	public void addPage(@Param("issue_id") long issue_id, @Param("page_id") long page_id);

	/**
	 * Finds all UX issue messages for a page audit record
	 *
	 * @param audit_record_id the ID of the page audit record
	 * @return the UX issue messages
	 */
	@Query("MATCH (audit_record:PageAuditRecord)-[]-(audit:Audit)  MATCH (audit)-[:HAS]-(issue:UXIssueMessage) WHERE id(audit_record)=$audit_record_id RETURN issue")
	public Set<UXIssueMessage> getIssues(@Param("audit_record_id") long audit_record_id);

	/**
	 * Finds all UX issue messages for an element
	 *
	 * @param name the name of the audit
	 * @param element_id the ID of the element
	 * @return the UX issue messages
	 */
	@Query("MATCH w=(a:Audit)-[]->(ux:UXIssueMessage) MATCH y=(ux:UXIssueMessage)-[:FOR]->(e:ElementState) WHERE id(e)=$element_id AND a.name=$name RETURN ux")
	public Set<UXIssueMessage> findByNameForElement(@Param("name") AuditName name, @Param("element_id") long element_id);

	/**
	 * Finds the number of UX issue messages for an element
	 *
	 * @param audit_name the name of the audit
	 * @param element_id the ID of the element
	 * @return the number of UX issue messages
	 */
	@Query("MATCH w=(a:Audit)-[]->(ux:UXIssueMessage) MATCH y=(ux:UXIssueMessage)-[:FOR]->(e:ElementState) WHERE id(e)=$element_id AND a.name=$name RETURN COUNT(ux)")
	public int getNumberOfUXIssuesForElement(@Param("name") AuditName audit_name, @Param("element_id") long element_id);
}
