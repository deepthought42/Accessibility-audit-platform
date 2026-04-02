package com.looksee.pageBuilder.models.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.models.rules.Rule;
import com.looksee.pageBuilder.models.Domain;
import com.looksee.pageBuilder.models.ElementState;

import io.github.resilience4j.retry.annotation.Retry;

@Repository
@Retry(name = "neoforj")
public interface ElementStateRepository extends Neo4jRepository<ElementState, Long> {
	
	@Query("MATCH (e:ElementState{key:$key}) RETURN e LIMIT 1")
	public ElementState findByKey(@Param("key") String key);

	@Query("MATCH (:Account{user_id:$user_id})-[*]->(e:ElementState{key:$element_key}) MATCH (e)-[hr:HAS]->(:Rule{key:$key}) DELETE hr")
	public void removeRule(@Param("user_id") String user_id, @Param("element_key") String element_key, @Param("key") String key);

	@Query("MATCH (:Account{user_id:$user_id})-[*]->(e:ElementState{key:$element_key}) MATCH (e)-[hr:HAS]->(r) RETURN r")
	public Set<Rule> getRules(@Param("user_id") String user_id, @Param("element_key") String element_key);

	@Query("MATCH (:Account{username:$username})-[*]->(e:ElementState{key:$element_key}),(r:Rule{key:$rule_key}) MERGE element=(e)-[hr:HAS]->(r) RETURN r")
	public Rule addRuleToFormElement(@Param("username") String username, @Param("element_key") String element_key, @Param("rule_key") String rule_key);

	@Query("MATCH (:Account{username:$username})-[*]->(e:ElementState{key:$element_key}) MATCH (e)-[:HAS]->(r:Rule{key:$rule_key}) RETURN r LIMIT 1")
	public Rule getElementRule(@Param("username") String username, @Param("element_key") String element_key, @Param("rule_key") String rule_key);

	@Query("MATCH (account:Account)-[*]->(e:ElementState{outer_html:$outer_html}) WHERE id(account)=$account_id RETURN e LIMIT 1")
	public ElementState findByOuterHtml(@Param("account_id") long account_id, @Param("outer_html") String snippet);

	@Query("MATCH (account:Account)-[*]->(es:ElementState{key:$element_key}) Match (es)-[:HAS]->(b:BugMessage) WHERE id(account)=$account_id DETACH DELETE b")
	public void clearBugMessages(@Param("account_id") long account_id, @Param("element_key") String element_key);

	@Query("MATCH (:Account{user_id:$user_id})-[]-(d:Domain) MATCH (d)-[]->(page:PageVersion) MATCH (page)-[*]->(e:ElementState{key:$element_key}) MATCH (e)-[:HAS_CHILD]->(es:ElementState) RETURN es")
	public List<ElementState> getChildElementsForUser(@Param("user_id") String user_id, @Param("element_key") String element_key);

	@Query("MATCH (e:ElementState{key:$element_key})-[:HAS_CHILD]->(es:ElementState) RETURN es")
	public List<ElementState> getChildElements(@Param("element_key") String element_key);

	@Query("MATCH (e:ElementState{key:$parent_key})-[:HAS_CHILD]->(es:ElementState{key:$child_key}) RETURN es")
	public List<ElementState> getChildElementForParent(@Param("parent_key") String parent_key, @Param("child_key") String child_key);

	@Query("MATCH (:Account{user_id:$user_id})-[]->(d:Domain{url:$url}) MATCH (d)-[*]->(p:PageState{key:$page_state_key}) MATCH (p)-[]->(parent_elem:ElementState) MATCH (parent_elem)-[:HAS]->(e:ElementState{key:$element_state_key}) RETURN parent_elem LIMIT 1")
	public ElementState getParentElement(@Param("user_id") String user_id, @Param("url") Domain url, @Param("page_state_key") String page_state_key, @Param("element_state_key") String element_state_key);

	@Query("MATCH (p:PageState{key:$page_state_key})-[*]->(parent_elem:ElementState) MATCH (parent_elem)-[:HAS_CHILD]->(e:ElementState{key:$element_state_key}) RETURN parent_elem LIMIT 1")
	public ElementState getParentElement(@Param("page_state_key") String page_state_key, @Param("element_state_key") String element_state_key);

	@Query("MATCH (parent:ElementState{key:$parent_key}) WITH parent MATCH (child:ElementState{key:$child_key}) MERGE (parent)-[:HAS_CHILD]->(child) RETURN parent")
	public void addChildElement(@Param("parent_key") String parent_key, @Param("child_key") String child_key);

	@Query("MATCH (p:PageState{key:$page_state_key})-[*]->(parent_elem:ElementState) MATCH (parent_elem)-[:HAS_CHILD]->(e:ElementState{key:$element_state_key}) RETURN parent_elem LIMIT 1")
	public ElementState findByPageStateAndChild(@Param("page_state_key") String page_state_key, @Param("element_state_key") String element_state_key);

	@Query("MATCH (p:PageState{key:$page_state_key})-[*]->(element:ElementState{xpath:$xpath}) RETURN element LIMIT 1")
	public ElementState findByPageStateAndXpath(@Param("page_state_key") String page_state_key, @Param("xpath") String xpath);

	@Query("MATCH (p:PageState)-[]->(e:ElementState) WHERE id(p)=$page_state_id RETURN e.key")
	public List<String> getAllExistingKeys(@Param("page_state_id") long page_state_id);
	
	@Query("MATCH (e:ElementState) WHERE e.key IN $element_keys RETURN e")
	public List<ElementState> getElements(@Param("element_keys")  Set<String> existing_keys);
	
	@Query("MATCH (p:PageState)-[:HAS]->(e:ElementState{name:'a'}) WHERE id(p)=$page_state_id RETURN DISTINCT e")
	public List<ElementState> getLinkElementStates(@Param("page_state_id") long page_state_id);
	
	@Query("MATCH (p:PageState)-[:HAS]->(element:ElementState) WHERE id(p)=$page_id AND id(element)=$element_id RETURN element ORDER BY p.created_at DESC LIMIT 1")
	public Optional<ElementState> getElementState(@Param("page_id") long page_id, @Param("element_id") long element_id);

	@Query("MATCH (p:PageState{key:$page_key})-[:HAS]->(e:ElementState) RETURN DISTINCT e")
	public List<ElementState> getElementStates(@Param("page_key") String key);
	
	@Query("MATCH (p:PageState)-[:HAS]->(e:ElementState) WHERE id(p)=$page_state_id RETURN DISTINCT e")
	public List<ElementState> getElementStates(@Param("page_state_id") long page_state_id);

	@Query("MATCH (p:PageState{key:$page_state_key})-[:HAS]->(e:ElementState{classification:'LEAF'}) where e.visible=true RETURN e")
	public List<ElementState> getVisibleLeafElements(@Param("page_state_key") String page_state_key);

	@Query("MATCH (p:PageState)-[:HAS]->(e:ElementState{key:$element_key}) WHERE id(p)=$page_state_id RETURN e")
	public ElementState findByPageStateAndKey(@Param("page_state_id") long page_state_id, @Param("element_key") String element_key);
	
	@Query("MATCH (p:PageState) WITH p MATCH (element:ElementState) WHERE id(p)=$page_id AND id(element)=$element_id MERGE (p)-[:HAS]->(element) RETURN element LIMIT 1")
	public ElementState addElement(@Param("page_id") long page_id, @Param("element_id") long element_id);

	@Query("MATCH p=(d:DomainAuditRecord)-[]->(n:PageState) WHERE id(d)=$domain_audit_id MATCH a=(n)-[]-(e:ElementState{key:$key}) RETURN e LIMIT 1")
	public ElementState findByDomainAuditAndKey(@Param("domain_audit_id") long domain_audit_id, @Param("key") String element_key);

	@Query("MATCH (page:PageState)-[]->(e:ElementState) WHERE id(page)=$page_id RETURN e")
    public List<ElementState> findAllForPage(@Param("page_id") long page_id);
}
