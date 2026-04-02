package com.looksee.models.repository;

import com.looksee.models.Form;
import java.util.Set;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for {@link Form}s
 */
public interface FormRepository extends Neo4jRepository<Form, Long> {

	/**
	 * Finds a form by key for a user and domain
	 *
	 * @param account_id the ID of the account
	 * @param url the URL of the page
	 * @param form_key the key of the form
	 * @return the form
	 */
	@Deprecated
	@Query("MATCH (account:Account)-[]->(d:Domain{url:$url}) MATCH (d)-[]->(p:Page) MATCH (p)-[]->(ps:PageState) MATCH (ps)-[]->(f:Form{key:$form_key}) Match form=(f)-[]->() WHERE id(account)=$account_id RETURN form LIMIT 1")
	public Form findByKeyForUserAndDomain(@Param("account_id") long account_id, @Param("url") String url, @Param("form_key") String form_key);
	
	/**
	 * Finds a form by key
	 *
	 * @param url the URL of the page
	 * @param form_key the key of the form
	 * @return the form
	 */
	@Query("MATCH (p:Page{url:$page_url})-[]->(ps:PageState) MATCH (ps)-[]->(f:Form{key:$form_key}) Match form=(f)-[]->() RETURN form LIMIT 1")
	public Form findByKey(@Param("page_url") String url, @Param("form_key") String form_key);
	
	/**
	 * Clears bug messages for a form
	 *
	 * @param user_id the ID of the user
	 * @param form_key the key of the form
	 * @return the form
	 */
	@Query("MATCH (:Account{user_id:$user_id})-[]->(d:Domain) MATCH (d)-[]->(p:Page) MATCH (p)-[]->(ps:PageState) MATCH (ps)-[]->(f:Form{key:$form_key}) Match (f)-[hbm:HAS]->(b:BugMessage) DELETE hbm,b")
	public Form clearBugMessages(@Param("user_id") String user_id, @Param("form_key") String form_key);
	
	/**
	 * Finds all forms for a domain
	 *
	 * @param account_id the ID of the account
	 * @param url the URL of the domain
	 * @return the forms
	 */
	@Query("MATCH (account:Account)-[:HAS_DOMAIN]->(d:Domain{url:$url}) MATCH (d)-[]->(p:Page) MATCH (p)-[]->(ps:PageState) MATCH (ps)-[]->(f:Form) MATCH a=(f)-[:DEFINED_BY]->() MATCH b=(f)-[:HAS]->(e) OPTIONAL MATCH c=(e)-->() WHERE id(account)=$account_id return a,b,c")
	public Set<Form> getForms(@Param("account_id") long account_id, @Param("url") String url);
}
