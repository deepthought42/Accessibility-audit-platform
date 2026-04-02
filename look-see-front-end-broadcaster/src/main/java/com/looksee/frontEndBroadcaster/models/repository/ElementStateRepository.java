package com.looksee.frontEndBroadcaster.models.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.frontEndBroadcaster.models.Domain;
import com.looksee.frontEndBroadcaster.models.ElementState;

@Repository
public interface ElementStateRepository extends Neo4jRepository<ElementState, Long> {
	
	@Query("MATCH (e:ElementState{key:$key}) RETURN e LIMIT 1")
	public ElementState findByKey(@Param("key") String key);

	@Query("MATCH (:Account{user_id:$user_id})-[*]->(e:ElementState{key:$element_key}) MATCH (e)-[hr:HAS]->(:Rule{key:$key}) DELETE hr")
	public void removeRule(@Param("user_id") String user_id, @Param("element_key") String element_key, @Param("key") String key);

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
	
	@Query("MATCH (p:PageState{key:$page_state_key})-[:HAS]->(e:ElementState{classification:'LEAF'}) where e.visible=true RETURN e")
	public List<ElementState> getVisibleLeafElements(@Param("page_state_key") String page_state_key);

	@Query("MATCH (p:PageState)-[:HAS]->(e:ElementState{classification:'LEAF'}) where id(p)=$page_state_id AND e.visible=true RETURN e")
	public List<ElementState> getVisibleLeafElements(@Param("page_state_id") long page_state_id);

	@Query("MATCH (p:PageState) WHERE id(p)=$page_id MATCH (element:ElementState) WHERE id(element)=$element_id MERGE (p)-[:HAS]->(element) RETURN element LIMIT 1")
	public ElementState addElement(@Param("page_id") long page_id, @Param("element_id") long element_id);

	@Query("MATCH (p:PageState)-[:HAS]->(element:ElementState) WHERE id(p)=$page_id AND id(element)=$element_id RETURN element ORDER BY p.created_at DESC LIMIT 1")
	public Optional<ElementState> getElementState(@Param("page_id") long page_id, @Param("element_id") long element_id);

	@Query("MATCH (:Account{user_id:$user_id})-[]->(d:Domain{url:$url}) MATCH (d)-[]->(p:Page) MATCH (p)-[]->(ps:PageState) MATCH (ps)-[]->(ps:PageState) MATCH (ps)-[]->(f:Form{key:$form_key}) Match (f)-[:HAS]->(e:ElementState) RETURN e")
	public List<ElementState> getElementStates(@Param("user_id") String user_id, @Param("url") String url, @Param("form_key") String form_key);

	@Query("MATCH (:Account{user_id:$user_id})-[]->(d:Domain{url:$url}) MATCH (d)-[]->(p:Page) MATCH (p)-[]->(ps:PageState) MATCH (ps)-[]->(f:Form{key:$form_key}) Match (f)-[:HAS_SUBMIT]->(e) RETURN e")
	public ElementState getSubmitElement(@Param("user_id") String user_id, @Param("url") String url, @Param("form_key") String form_key);
	
	@Query("MATCH (:Account{user_id:$user_id})-[]->(d:Domain{url:$url}) MATCH (d)-[]->(p:Page) MATCH (p)-[]->(ps:PageState) MATCH (ps)-[]->(f:Form{key:$form_key}) Match (f)-[:DEFINED_BY]->(e) RETURN e")
	public ElementState getFormElement(@Param("user_id") String user_id, @Param("url") String url, @Param("form_key") String form_key);

	@Query("MATCH (p:PageState{key:$page_key})-[:HAS]->(e:ElementState) RETURN DISTINCT e")
	public List<ElementState> getElementStates(@Param("page_key") String key);
	
	@Query("MATCH (p:PageState)-[:HAS]->(e:ElementState) WHERE id(p)=$page_state_id RETURN DISTINCT e")
	public List<ElementState> getElementStates(@Param("page_state_id") long page_state_id);

	@Query("MATCH (p:PageState)-[:HAS]->(e:ElementState{name:'a'}) WHERE id(p)=$page_state_id RETURN DISTINCT e")
	public List<ElementState> getLinkElementStates(@Param("page_state_id") long page_state_id);
	
	@Query("MATCH (s:Step) WITH s MATCH (p:ElementState) WHERE id(s)=$step_id AND id(p)=$element_state_id MERGE (s)-[:HAS]->(p) RETURN p")
	public ElementState addElementState(@Param("step_id") long id, @Param("element_state_id") long element_state_id);

	@Query("MATCH (:ElementInteractionStep{key:$step_key})-[:HAS]->(e:ElementState) RETURN e")
	public ElementState getElementStateForStep(@Param("step_key") String step_key);

	@Query("MATCH (s:Step) WITH s MATCH (e:ElementState) WHERE id(s)=$step_id AND id(e)=$element_id MERGE (s)-[:USERNAME_INPUT]->(e) RETURN e")
	public ElementState addUsernameElement(@Param("step_id") long id, @Param("element_id") long element_id);
	
	@Query("MATCH (s:LoginStep)-[:USERNAME_INPUT]->(e:ElementState) WHERE id(s)=$step_id RETURN e")
	public ElementState getUsernameElement(@Param("step_id") long id);
	
	@Query("MATCH (s:Step) WITH s MATCH (e:ElementState) WHERE id(s)=$step_id AND id(e)=$element_id MERGE (s)-[:PASSWORD_INPUT]->(e) RETURN e")
	public ElementState addPasswordElement(@Param("step_id") long id, @Param("element_id") long element_id);
	
	@Query("MATCH (s:LoginStep)-[:PASSWORD_INPUT]->(e:ElementState) WHERE id(s)=$step_id RETURN e")
	public ElementState getPasswordElement(@Param("step_id") long id);
	
	@Query("MATCH (s:Step) WITH s MATCH (e:ElementState) WHERE id(s)=$step_id AND id(e)=$element_id MERGE (s)-[:SUBMIT]->(e) RETURN e")
	public ElementState addSubmitElement(@Param("step_id") long id, @Param("element_id") long element_id);

	@Query("MATCH (s:LoginStep)-[:SUBMIT]->(e:ElementState) WHERE id(s)=$step_id RETURN e")
	public ElementState getSubmitElement(@Param("step_id") long id);
	
	@Deprecated
	@Query("MATCH (:SimpleStep{key:$step_key})-[:HAS]->(e:ElementState) RETURN e")
	public ElementState getElementState(@Param("step_key") String step_key);

	@Query("MATCH (uim:UXIssueMessage)-[:FOR]->(e:ElementState) WHERE id(uim)=$id RETURN e")
	public ElementState getElement(@Param("id") long id);

	@Query("MATCH (uim:UXIssueMessage)-[:EXAMPLE]->(e:ElementState) WHERE id(uim)=$id RETURN e")
	public ElementState getGoodExample(@Param("id") long issue_id);

	@Query("MATCH (p:PageState)-[]->(e:ElementState) WHERE id(p)=$page_state_id RETURN COUNT(e)")
	public long getElementStateCount(@Param("page_state_id") long page_state_id);

	@Query("MATCH p=(d:DomainAuditRecord)-[]->(n:PageState) WHERE id(d)=$domain_audit_id MATCH a=(n)-[]->(e:ElementState{key:$key}) RETURN e LIMIT 1")
	public ElementState findByDomainAuditAndKey(@Param("domain_audit_id") long domain_audit_id, @Param("key") String element_key);

	@Query("MATCH p=(d:DomainMap)-[*3]->(n:PageState) WHERE id(d)=$domain_map_id MATCH a=(n)-[]->(e:ElementState{key:$key}) RETURN e LIMIT 1")
	public ElementState findByDomainMapAndKey(@Param("domain_map_id") long domain_map_id, @Param("key") String key);
}
