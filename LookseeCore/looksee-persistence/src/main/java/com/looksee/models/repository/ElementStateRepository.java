package com.looksee.models.repository;

import com.looksee.models.Domain;
import com.looksee.models.Element;
import com.looksee.models.ElementState;
import com.looksee.models.rules.Rule;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Spring Data Neo4j to handle interactions with
 * {@link ElementState} objects
 */
@Repository
@Retry(name = "neoforj")
public interface ElementStateRepository extends Neo4jRepository<ElementState, Long> {
	
	/**
	 * Finds an element state by its key
	 *
	 * @param key the key of the element state
	 * @return the element state
	 */
	@Query("MATCH (e:ElementState{key:$key}) RETURN e LIMIT 1")
	public ElementState findByKey(@Param("key") String key);

	/**
	 * Removes a rule from an element state
	 *
	 * @param user_id the ID of the user
	 * @param element_key the key of the element state
	 * @param key the key of the rule
	 */
	@Query("MATCH (:Account{user_id:$user_id})-[*]->(e:ElementState{key:$element_key}) MATCH (e)-[hr:HAS]->(:Rule{key:$key}) DELETE hr")
	public void removeRule(@Param("user_id") String user_id, @Param("element_key") String element_key, @Param("key") String key);

	/**
	 * Finds all rules for an element state
	 *
	 * @param user_id the ID of the user
	 * @param element_key the key of the element state
	 * @return the rules
	 */
	@Query("MATCH (:Account{user_id:$user_id})-[*]->(e:ElementState{key:$element_key}) MATCH (e)-[hr:HAS]->(r) RETURN r")
	public Set<Rule> getRules(@Param("user_id") String user_id, @Param("element_key") String element_key);

	/**
	 * Adds a rule to an element state
	 *
	 * @param username the username of the user
	 * @param element_key the key of the element state
	 * @param rule_key the key of the rule
	 * @return the rule
	 */
	@Query("MATCH (:Account{username:$username})-[*]->(e:ElementState{key:$element_key}),(r:Rule{key:$rule_key}) MERGE element=(e)-[hr:HAS]->(r) RETURN r")
	public Rule addRuleToFormElement(@Param("username") String username, @Param("element_key") String element_key, @Param("rule_key") String rule_key);

	/**
	 * Finds a rule for an element state
	 *
	 * @param username the username of the user
	 * @param element_key the key of the element state
	 * @param rule_key the key of the rule
	 * @return the rule
	 */
	@Query("MATCH (:Account{username:$username})-[*]->(e:ElementState{key:$element_key}) MATCH (e)-[:HAS]->(r:Rule{key:$rule_key}) RETURN r LIMIT 1")
	public Rule getElementRule(@Param("username") String username, @Param("element_key") String element_key, @Param("rule_key") String rule_key);

	/**
	 * Finds an element state by its outer HTML
	 *
	 * @param account_id the ID of the account
	 * @param snippet the outer HTML of the element state
	 * @return the element state
	 */
	@Query("MATCH (account:Account)-[*]->(e:ElementState{outer_html:$outer_html}) WHERE id(account)=$account_id RETURN e LIMIT 1")
	public ElementState findByOuterHtml(@Param("account_id") long account_id, @Param("outer_html") String snippet);

	/**
	 * Clears all bug messages for an element state
	 *
	 * @param account_id the ID of the account
	 * @param element_key the key of the element state
	 */
	@Query("MATCH (account:Account)-[*]->(es:ElementState{key:$element_key}) Match (es)-[:HAS]->(b:BugMessage) WHERE id(account)=$account_id DETACH DELETE b")
	public void clearBugMessages(@Param("account_id") long account_id, @Param("element_key") String element_key);

	/**
	 * Finds all child elements for a user
	 *
	 * @param user_id the ID of the user
	 * @param element_key the key of the element state
	 * @return the child elements
	 */
	@Query("MATCH (:Account{user_id:$user_id})-[]-(d:Domain) MATCH (d)-[]->(page:PageVersion) MATCH (page)-[*]->(e:ElementState{key:$element_key}) MATCH (e)-[:HAS_CHILD]->(es:ElementState) RETURN es")
	public List<ElementState> getChildElementsForUser(@Param("user_id") String user_id, @Param("element_key") String element_key);

	/**
	 * Finds all child elements for an element state
	 *
	 * @param element_key the key of the element state
	 * @return the child elements
	 */
	@Query("MATCH (e:ElementState{key:$element_key})-[:HAS_CHILD]->(es:ElementState) RETURN es")
	public List<ElementState> getChildElements(@Param("element_key") String element_key);

	/**
	 * Finds a child element for a parent element
	 *
	 * @param parent_key the key of the parent element
	 * @param child_key the key of the child element
	 * @return the child element
	 */
	@Query("MATCH (e:ElementState{key:$parent_key})-[:HAS_CHILD]->(es:ElementState{key:$child_key}) RETURN es")
	public List<ElementState> getChildElementForParent(@Param("parent_key") String parent_key, @Param("child_key") String child_key);

	/**
	 * Finds a parent element for a child element
	 *
	 * @param user_id the ID of the user
	 * @param url the URL of the domain
	 * @param page_state_key the key of the page state
	 * @param element_state_key the key of the element state
	 * @return the parent element
	 */
	@Query("MATCH (:Account{user_id:$user_id})-[]->(d:Domain{url:$url}) MATCH (d)-[*]->(p:PageState{key:$page_state_key}) MATCH (p)-[]->(parent_elem:ElementState) MATCH (parent_elem)-[:HAS]->(e:ElementState{key:$element_state_key}) RETURN parent_elem LIMIT 1")
	public ElementState getParentElement(@Param("user_id") String user_id, @Param("url") Domain url, @Param("page_state_key") String page_state_key, @Param("element_state_key") String element_state_key);

	/**
	 * Finds a parent element for a child element
	 *
	 * @param page_state_key the key of the page state
	 * @param element_state_key the key of the element state
	 * @return the parent element
	 */
	@Query("MATCH (p:PageState{key:$page_state_key})-[*]->(parent_elem:ElementState) MATCH (parent_elem)-[:HAS_CHILD]->(e:ElementState{key:$element_state_key}) RETURN parent_elem LIMIT 1")
	public ElementState getParentElement(@Param("page_state_key") String page_state_key, @Param("element_state_key") String element_state_key);

	/**
	 * Adds a child element to a parent element
	 *
	 * @param parent_key the key of the parent element
	 * @param child_key the key of the child element
	 */
	@Query("MATCH (parent:ElementState{key:$parent_key}) WITH parent MATCH (child:ElementState{key:$child_key}) MERGE (parent)-[:HAS_CHILD]->(child) RETURN parent")
	public void addChildElement(@Param("parent_key") String parent_key, @Param("child_key") String child_key);

	/**
	 * Finds a parent element for a child element
	 *
	 * @param page_state_key the key of the page state
	 * @param element_state_key the key of the element state
	 * @return the parent element
	 */
	@Query("MATCH (p:PageState{key:$page_state_key})-[*]->(parent_elem:ElementState) MATCH (parent_elem)-[:HAS_CHILD]->(e:ElementState{key:$element_state_key}) RETURN parent_elem LIMIT 1")
	public ElementState findByPageStateAndChild(@Param("page_state_key") String page_state_key, @Param("element_state_key") String element_state_key);

	/**
	 * Finds an element state by its page state and xpath
	 *
	 * @param page_state_key the key of the page state
	 * @param xpath the xpath of the element state
	 * @return the element state
	 */
	@Query("MATCH (p:PageState{key:$page_state_key})-[*]->(element:ElementState{xpath:$xpath}) RETURN element LIMIT 1")
	public ElementState findByPageStateAndXpath(@Param("page_state_key") String page_state_key, @Param("xpath") String xpath);

	/**
	 * Finds all existing keys for a page state
	 *
	 * @param page_state_id the ID of the page state
	 * @return the existing keys
	 */
	@Query("MATCH (p:PageState)-[]->(e:ElementState) WHERE id(p)=$page_state_id RETURN e.key")
	public List<String> getAllExistingKeys(@Param("page_state_id") long page_state_id);
	
	/**
	 * Finds all element states by their keys
	 *
	 * @param element_keys the keys of the element states
	 * @return the element states
	 */
	@Query("MATCH (e:ElementState) WHERE e.key IN $element_keys RETURN e")
	public List<ElementState> getElements(@Param("element_keys") Set<String> element_keys);
	
	/**
	 * Finds all link element states for a page state
	 *
	 * @param page_state_id the ID of the page state
	 * @return the link element states
	 */
	@Query("MATCH (p:PageState)-[:HAS]->(e:ElementState{name:'a'}) WHERE id(p)=$page_state_id RETURN DISTINCT e")
	public List<ElementState> getLinkElementStates(@Param("page_state_id") long page_state_id);
	
	/**
	 * Finds an element state by its page ID and element ID
	 *
	 * @param page_id the ID of the page
	 * @param element_id the ID of the element
	 * @return the element state
	 */
	@Query("MATCH (p:PageState)-[:HAS]->(element:ElementState) WHERE id(p)=$page_id AND id(element)=$element_id RETURN element ORDER BY p.created_at DESC LIMIT 1")
	public Optional<ElementState> getElementState(@Param("page_id") long page_id, @Param("element_id") long element_id);

	/**
	 * Finds all element states for a page state
	 *
	 * @param page_key the key of the page state
	 * @return the element states
	 */
	@Query("MATCH (p:PageState{key:$page_key})-[:HAS]->(e:ElementState) RETURN DISTINCT e")
	public List<ElementState> getElementStates(@Param("page_key") String page_key);
	
	/**
	 * Finds all element states for a page state
	 *
	 * @param page_state_id the ID of the page state
	 * @return the element states
	 */
	@Query("MATCH (p:PageState)-[:HAS]->(e:ElementState) WHERE id(p)=$page_state_id RETURN DISTINCT e")
	public List<ElementState> getElementStates(@Param("page_state_id") long page_state_id);

	/**
	 * Finds all visible leaf elements for a page state
	 *
	 * @param page_state_key the key of the page state
	 * @return the visible leaf elements
	 */
	@Query("MATCH (p:PageState{key:$page_state_key})-[:HAS]->(e:ElementState{classification:'LEAF'}) where e.visible=true RETURN e")
	public List<ElementState> getVisibleLeafElements(@Param("page_state_key") String page_state_key);

	/**
	 * Finds an element state by its page state and key
	 *
	 * @param page_state_id the ID of the page state
	 * @param element_key the key of the element state
	 * @return the element state
	 */
	@Query("MATCH (p:PageState)-[:HAS]->(e:ElementState{key:$element_key}) WHERE id(p)=$page_state_id RETURN e")
	public ElementState findByPageStateAndKey(@Param("page_state_id") long page_state_id, @Param("element_key") String element_key);
	
	/**
	 * Adds an element to a page state
	 *
	 * @param page_id the ID of the page
	 * @param element_id the ID of the element
	 * @return the element state
	 */
	@Query("MATCH (p:PageState) WITH p MATCH (element:ElementState) WHERE id(p)=$page_id AND id(element)=$element_id MERGE (p)-[:HAS]->(element) RETURN element LIMIT 1")
	public ElementState addElement(@Param("page_id") long page_id, @Param("element_id") long element_id);

	/**
	 * Finds an element state by its domain audit ID and key
	 *
	 * @param domain_audit_id the ID of the domain audit
	 * @param element_key the key of the element state
	 * @return the element state
	 */
	@Query("MATCH p=(d:DomainAuditRecord)-[]->(n:PageState) WHERE id(d)=$domain_audit_id MATCH a=(n)-[]-(e:ElementState{key:$element_key}) RETURN e LIMIT 1")
	public ElementState findByDomainAuditAndKey(@Param("domain_audit_id") long domain_audit_id, @Param("element_key") String element_key);

	/**
	 * Retrieves all element states associated with a specific form within a domain and page state.
	 * The query traverses from Account through Domain, Page, PageState to find the form and its elements.
	 *
	 * @param user_id The unique identifier of the user account
	 * @param url The domain URL to search within
	 * @param form_key The unique key identifying the form
	 * @return List of ElementState objects associated with the form
	 */
	@Query("MATCH (:Account{user_id:$user_id})-[]->(d:Domain{url:$url}) MATCH (d)-[]->(p:Page) MATCH (p)-[]->(ps:PageState) MATCH (ps)-[]->(ps:PageState) MATCH (ps)-[]->(f:Form{key:$form_key}) Match (f)-[:HAS]->(e:ElementState) RETURN e")
	public List<ElementState> getElementStates(@Param("user_id") String user_id, @Param("url") String url, @Param("form_key") String form_key);

	/**
	 * Retrieves the submit element associated with a specific form within a domain and page state.
	 *
	 * @param user_id The unique identifier of the user account
	 * @param url The domain URL to search within
	 * @param form_key The unique key identifying the form
	 * @return The ElementState representing the submit element of the form
	 */
	@Query("MATCH (:Account{user_id:$user_id})-[]->(d:Domain{url:$url}) MATCH (d)-[]->(p:Page) MATCH (p)-[]->(ps:PageState) MATCH (ps)-[]->(f:Form{key:$form_key}) Match (f)-[:HAS_SUBMIT]->(e) RETURN e")
	public ElementState getSubmitElement(@Param("user_id") String user_id, @Param("url") String url, @Param("form_key") String form_key);
	
	/**
	 * Retrieves the form element that defines a specific form within a domain and page state.
	 *
	 * @param user_id The unique identifier of the user account
	 * @param url The domain URL to search within
	 * @param form_key The unique key identifying the form
	 * @return The ElementState representing the form element
	 */
	@Query("MATCH (:Account{user_id:$user_id})-[]->(d:Domain{url:$url}) MATCH (d)-[]->(p:Page) MATCH (p)-[]->(ps:PageState) MATCH (ps)-[]->(f:Form{key:$form_key}) Match (f)-[:DEFINED_BY]->(e) RETURN e")
	public ElementState getFormElement(@Param("user_id") String user_id, @Param("url") String url, @Param("form_key") String form_key);

	/**
	 * Associates an element state with a step by creating a HAS relationship.
	 *
	 * @param id The ID of the step
	 * @param element_state_id The ID of the element state to associate
	 * @return The associated ElementState
	 */
	@Query("MATCH (s:Step) WITH s MATCH (p:ElementState) WHERE id(s)=$step_id AND id(p)=$element_state_id MERGE (s)-[:HAS]->(p) RETURN p")
	public ElementState addElementState(@Param("step_id") long id, @Param("element_state_id") long element_state_id);

	/**
	 * Retrieves the element state associated with a specific element interaction step.
	 *
	 * @param step_key The unique key identifying the element interaction step
	 * @return The ElementState associated with the step
	 */
	@Query("MATCH (:ElementInteractionStep{key:$step_key})-[:HAS]->(e:ElementState) RETURN e")
	public ElementState getElementStateForStep(@Param("step_key") String step_key);

	/**
	 * Associates a username input element with a step by creating a USERNAME_INPUT relationship.
	 *
	 * @param id The ID of the step
	 * @param element_id The ID of the element state to associate as username input
	 * @return The associated ElementState
	 */
	@Query("MATCH (s:Step) WITH s MATCH (e:ElementState) WHERE id(s)=$step_id AND id(e)=$element_id MERGE (s)-[:USERNAME_INPUT]->(e) RETURN e")
	public ElementState addUsernameElement(@Param("step_id") long id, @Param("element_id") long element_id);
	
	/**
	 * Retrieves the username input element associated with a login step.
	 *
	 * @param id The ID of the login step
	 * @return The ElementState representing the username input element
	 */
	@Query("MATCH (s:LoginStep)-[:USERNAME_INPUT]->(e:ElementState) WHERE id(s)=$step_id RETURN e")
	public ElementState getUsernameElement(@Param("step_id") long id);
	
	/**
	 * Associates a password input element with a step by creating a PASSWORD_INPUT relationship.
	 *
	 * @param id The ID of the step
	 * @param element_id The ID of the element state to associate as password input
	 * @return The associated ElementState
	 */
	@Query("MATCH (s:Step) WITH s MATCH (e:ElementState) WHERE id(s)=$step_id AND id(e)=$element_id MERGE (s)-[:PASSWORD_INPUT]->(e) RETURN e")
	public ElementState addPasswordElement(@Param("step_id") long id, @Param("element_id") long element_id);
	
	/**
	 * Retrieves the password input element associated with a login step.
	 *
	 * @param id The ID of the login step
	 * @return The ElementState representing the password input element
	 */
	@Query("MATCH (s:LoginStep)-[:PASSWORD_INPUT]->(e:ElementState) WHERE id(s)=$step_id RETURN e")
	public ElementState getPasswordElement(@Param("step_id") long id);
	
	/**
	 * Associates a submit element with a step by creating a SUBMIT relationship.
	 *
	 * @param id The ID of the step
	 * @param element_id The ID of the element state to associate as submit element
	 * @return The associated ElementState
	 */
	@Query("MATCH (s:Step) WITH s MATCH (e:ElementState) WHERE id(s)=$step_id AND id(e)=$element_id MERGE (s)-[:SUBMIT]->(e) RETURN e")
	public ElementState addSubmitElement(@Param("step_id") long id, @Param("element_id") long element_id);

	/**
	 * Retrieves the submit element associated with a login step.
	 *
	 * @param id The ID of the login step
	 * @return The ElementState representing the submit element
	 */
	@Query("MATCH (s:LoginStep)-[:SUBMIT]->(e:ElementState) WHERE id(s)=$step_id RETURN e")
	public ElementState getSubmitElement(@Param("step_id") long id);
	
	/**
	 * Retrieves the element state associated with a simple step.
	 * @deprecated This method is deprecated and should not be used in new code
	 *
	 * @param step_key The unique key identifying the simple step
	 * @return The ElementState associated with the step
	 */
	@Deprecated
	@Query("MATCH (:SimpleStep{key:$step_key})-[:HAS]->(e:ElementState) RETURN e")
	public ElementState getElementState(@Param("step_key") String step_key);

	/**
	 * Retrieves the element state associated with a UX issue message.
	 *
	 * @param id The ID of the UX issue message
	 * @return The ElementState associated with the UX issue message
	 */
	@Query("MATCH (uim:UXIssueMessage)-[:FOR]->(e:ElementState) WHERE id(uim)=$id RETURN e")
	public ElementState getElement(@Param("id") long id);

	/**
	 * Retrieves the good example element state associated with a UX issue message.
	 *
	 * @param issue_id The ID of the UX issue message
	 * @return The ElementState representing the good example
	 */
	@Query("MATCH (uim:UXIssueMessage)-[:EXAMPLE]->(e:ElementState) WHERE id(uim)=$id RETURN e")
	public ElementState getGoodExample(@Param("id") long issue_id);

	/**
	 * Retrieves all element states and their relationships for a specific domain and user.
	 *
	 * @param url The domain URL to search within
	 * @param username The username of the account
	 * @return Set of Element objects containing element states and their relationships
	 */
	@Query("MATCH (:Account{username:$username})-[:HAS_DOMAIN]-(d:Domain{url:$url}) MATCH (d)-[]->(t:Test) MATCH (t)-[]->(e:ElementState) OPTIONAL MATCH b=(e)-->() RETURN b")
	public Set<Element> getElementStates(@Param("url") String url, @Param("username") String username);

	/**
	 * Counts the number of distinct element states associated with a page state.
	 *
	 * @param page_id The ID of the page state
	 * @return The count of distinct element states
	 */
	@Query("MATCH (p:PageState)-[:HAS]->(e:ElementState) WHERE id(p)=$page_state_id RETURN DISTINCT COUNT(e)")
	public int getElementStateCount(@Param("page_state_id") long page_id);

	/**
	 * Retrieves all visible leaf elements for a specific page state.
	 *
	 * @param page_state_id The ID of the page state
	 * @return List of ElementState objects representing the visible leaf elements
	 */
	@Query("MATCH (p:PageState) WHERE id(p)=$page_state_id MATCH (p)-[:HAS]->(e:ElementState) where e.visible=true AND e.classification'LEAF' RETURN e")
	public List<ElementState> getVisibleLeafElements(@Param("page_state_id") long page_state_id);

	/**
	 * Retrieves an element state by its domain map ID and key.
	 *
	 * @param domain_map_id The ID of the domain map
	 * @param key The key of the element state
	 * @return The ElementState object if found, null otherwise
	 */
	@Query("MATCH p=(d:DomainMap)-[*3]->(n:PageState)-[]->(e:ElementState{key:$key}) WHERE id(d)=$domain_map_id RETURN e LIMIT 1")
	public ElementState findByDomainMapAndKey(@Param("domain_map_id") long domain_map_id, @Param("key") String key);

	/**
	 * Retrieves an element state by its page state ID and CSS selector.
	 *
	 * @param page_state_id The ID of the page state
	 * @param cssSelector The CSS selector of the element state
	 * @return The ElementState object if found, null otherwise
	 */
	@Query("MATCH (p:PageState)-[:HAS]->(e:ElementState{cssSelector:$selector}) WHERE id(p)=$page_state_id RETURN DISTINCT e")
    public ElementState findByPageAndCssSelector(@Param("page_state_id") long page_state_id, @Param("selector") String cssSelector);
}
