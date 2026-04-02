package com.looksee.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.looksee.models.enums.TestStatus;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * Defines the path of a test, the result and the expected values to determine if a test was
 * successful or not.
 *
 * invariant: name != null
 * invariant: status != null
 * invariant: pathKeys != null
 * invariant: records != null
 * invariant: groups != null
 * invariant: pathObjects != null
 */
@Getter
@Setter
@NoArgsConstructor
@Node
public class Test extends LookseeObject {
    @SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(Test.class);
	
	private String name;
	private TestStatus status;
	private boolean isUseful = false;
	private boolean spansMultipleDomains = false;
	private boolean isRunning;
	private boolean archived;
	private Date lastRunTime;
	private long runTimeLength;
	private List<String> pathKeys;

	@CompositeProperty
	private Map<String, String> browserPassingStatuses = new HashMap<>();
	
	@Relationship(type = "HAS_TEST_RECORD")
	private List<TestRecord> records;
	
	@Relationship(type = "HAS_GROUP")
	private Set<Group> groups = new HashSet<>();

	@JsonIgnore
	@Relationship(type = "HAS_PATH_OBJECT")
	private List<LookseeObject> pathObjects = new ArrayList<>();
	
	@Relationship(type = "HAS_RESULT")
	private PageState result;
	
	/**
	 * Constructs a test object
	 * 
	 * @param path_keys the keys of the path (must not be null or empty)
	 * @param path_objects the objects of the path (must not be null or empty)
	 * @param result the result of the test
	 * @param name the name of the test
	 * @param is_running whether the test is running
	 * @param spansMultipleDomains whether the test spans multiple domains
	 * @throws MalformedURLException if the URL is malformed
	 * 
	 * precondition: path_keys != null
	 * precondition: !path_keys.isEmpty()
	 * precondition: path_objects != null
	 * precondition: !path_objects.isEmpty()
	 * precondition: result != null
	 * precondition: name != null
	 * precondition: !name.isEmpty()
	 * precondition: is_running != null
	 * precondition: spansMultipleDomains != null
	 * @throws IllegalArgumentException if path_keys is null or empty
	 * @throws IllegalArgumentException if path_objects is null or empty
	 */
	public Test(List<String> path_keys,
				List<LookseeObject> path_objects,
				PageState result,
				String name,
				boolean is_running,
				boolean spansMultipleDomains) throws MalformedURLException{
		assert path_keys != null;
		assert !path_keys.isEmpty();
		assert path_objects != null;
		assert !path_objects.isEmpty();
		assert result != null;
		assert name != null;
		assert !name.isEmpty();

		setPathKeys(path_keys);
		setPathObjects(path_objects);
		setResult(result);
		setRecords(new ArrayList<TestRecord>());
		setStatus(TestStatus.UNVERIFIED);
		setSpansMultipleDomains(spansMultipleDomains);
		setLastRunTime(new Date());
		setName(name);
		setBrowserPassingStatuses(new HashMap<String, String>());
		setRunning(is_running);
		setArchived(false);
		setKey(generateKey());
		setRunTimeLength(0L);
	}
	
	/**
	 * Checks if a {@code TestRecord} snapshot of a {@code Test} is passing or not
	 * 
	 * @param expected_page the expected page state
	 * @param new_result_page the new result page state
	 * @param last_test_passing_status the last test passing status
	 * @return the test status
	 */
	public static TestStatus isTestPassing(PageState expected_page,
											PageState new_result_page,
											TestStatus last_test_passing_status){
		assert expected_page != null;
		assert new_result_page != null;
		assert last_test_passing_status != null;
		
		if(last_test_passing_status.equals(TestStatus.FAILING) && expected_page.equals(new_result_page)){
			return TestStatus.FAILING; 
		}
		else if(last_test_passing_status.equals(TestStatus.FAILING) && !expected_page.equals(new_result_page)){
			return TestStatus.UNVERIFIED;
		}
		else if(last_test_passing_status.equals(TestStatus.PASSING) && expected_page.equals(new_result_page)){
			return TestStatus.PASSING;
		}
		else if(last_test_passing_status.equals(TestStatus.PASSING) && !expected_page.equals(new_result_page)){
			return TestStatus.FAILING;
		}
		
		return last_test_passing_status;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object o){
		if(o instanceof Test){
			Test test = (Test)o;
			
			return test.getKey().equals(this.getKey());
		}
		
		return false;
	}
	
	/**
	 * Adds a path key to the test
	 *
	 * @param key the path key to add
	 * @return true if the path key was added, false otherwise
	 *
	 * precondition: key != null
	 */
	public boolean addPathKey(String key) {
		assert key != null;
		return this.pathKeys.add(key);
	}
	
	/**
	 * Adds a record to the test
	 *
	 * @param record the record to add
	 *
	 * precondition: record != null
	 */
	public void addRecord(TestRecord record){
		assert record != null;
		this.records.add(record);
	}
	
	/**
	 * Adds a group to the test
	 *
	 * @param group the group to add
	 *
	 * precondition: group != null
	 */
	public void addGroup(Group group){
		assert group != null;
		this.groups.add(group);
	}
	
	/**
	 * Removes a group from the test
	 *
	 * @param group the group to remove
	 *
	 * precondition: group != null
	 */
	public void removeGroup(Group group) {
		assert group != null;
		//remove edge between test and group
		this.groups.remove(group);
	}
	
	/**
	 * Adds a path object to the test
	 *
	 * @param path_obj the path object to add
	 *
	 * precondition: path_obj != null
	 */
	public void addPathObject(LookseeObject path_obj) {
		assert path_obj != null;
		this.pathObjects.add(path_obj);
	}

	/**
	 * Gets the path objects of the test
	 *
	 * @return the path objects
	 */
	@JsonIgnore
	public List<LookseeObject> getPathObjects() {
		return this.pathObjects;
	}

	/**
	 * Sets the path objects of the test
	 *
	 * @param path_objects the path objects to set
	 */
	@JsonIgnore
	public void setPathObjects(List<LookseeObject> path_objects) {
		this.pathObjects = path_objects;
	}
	
	/**
	 * Sets the browser passing status for a test
	 *
	 * @param browser_name name of browser (ie 'chrome', 'firefox') (must not be null)
	 * @param status boolean indicating passing or failing
	 * @throws IllegalArgumentException if browser_name is null
	 *
	 * precondition: browser_name != null
	 * precondition: status != null
	 */
	public void setBrowserStatus(String browser_name, String status){
		assert browser_name != null;
		assert status != null;
		getBrowserPassingStatuses().put(browser_name, status);
	}
	
	/**
	 * Gets the first page state in the test
	 *
	 * @return the first page state
	 */
	public PageState firstPage() {
		for(String key : this.getPathKeys()){
			if(key.contains("pagestate")){
				for(LookseeObject path_obj: this.getPathObjects()){
					if(path_obj.getKey().equals(key) && path_obj instanceof PageState){
						return (PageState)path_obj;
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Generates a key using both path and result in order to guarantee uniqueness of key as well 
	 * as easy identity of {@link Test} when generated in the wild via discovery
	 * 
	 * @return the key
	 */
	@Override
	public String generateKey() {
		String path_key =  String.join("", getPathKeys());
		path_key += getResult().getKey();
		
		return "test"+org.apache.commons.codec.digest.DigestUtils.sha512Hex(path_key);
	}
	
	/**
	 * Clone {@link Test} object
	 * 
	 * @param test the test to clone
	 * @return the cloned test
	 * @throws MalformedURLException if the URL is malformed
	 *
	 * precondition: test != null
	 */
	public static Test clone(Test test) throws MalformedURLException{
		assert test != null;
		
		Test clone_test = new Test(new ArrayList<String>(test.getPathKeys()),
									new ArrayList<LookseeObject>(test.getPathObjects()),
									test.getResult(), 
									test.getName(),
									test.isRunning(),
									test.isSpansMultipleDomains());

		clone_test.setBrowserPassingStatuses(test.getBrowserPassingStatuses());
		clone_test.setGroups(new HashSet<>(test.getGroups()));
		clone_test.setLastRunTime(test.getLastRunTime());
		clone_test.setStatus(test.getStatus());
		clone_test.setRunTimeLength(test.getRunTimeLength());
		
		return clone_test;
	}
	
	/**
	 * Generates a name for the test
	 *
	 * @return the test name
	 * @throws MalformedURLException if the URL is malformed
	 */
	public String generateTestName() throws MalformedURLException {
		String test_name = "";
		int page_state_idx = 0;
		int element_action_cnt = 0;
		for(LookseeObject obj : this.pathObjects){
			if(obj instanceof PageState && page_state_idx < 1){
				String path = (new URL(((PageState)obj).getUrl())).getPath().trim();
				path = path.replace("/", " ");
				path = path.trim();
				if("/".equals(path) || path.isEmpty()){
					path = "home";
				}
				test_name +=  path + " page ";
				page_state_idx++;
			}
			else if(obj instanceof Element){
				if(element_action_cnt > 0){
					test_name += "> ";
				}
				
				Element element = (Element)obj;
				String tag_name = element.getName();
				
				if(element.getAttribute("id") != null){
					tag_name = element.getAttribute("id");
				}
				else{
					if("a".equals(tag_name)){
						tag_name = "link";
					}
				}
				test_name += tag_name + " ";
				element_action_cnt++;
			}
			else if(obj instanceof TestAction){
				TestAction action = ((TestAction)obj);
				test_name += action.getName() + " ";
				if(action.getValue() != null ){
					test_name += action.getValue() + " ";
				}
			}
		}
		
		return test_name.trim();
	}
}
