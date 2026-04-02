package com.looksee.frontEndBroadcaster.models;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.api.client.util.DateTime;
import com.looksee.frontEndBroadcaster.models.enums.TestStatus;

/**
 * Defines the path of a test, the result and the expected values to determine if a test was 
 * successful or not
 *
 */
@Node
public class Test implements Persistable {
    @SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(Test.class);
	
    @GeneratedValue
    @Id
	private Long id;
	
	private String key; 
	private String name;
	private TestStatus status;
	private boolean isUseful = false;
	private boolean spansMultipleDomains = false;
	private boolean is_running;
	private boolean archived;
	private Date last_run_time;
	private long run_time_length;
	private List<String> path_keys;

	@CompositeProperty
	private Map<String, String> browser_passing_statuses = new HashMap<>();
	
	@Relationship(type = "HAS_TEST_RECORD")
	private List<TestRecord> records = new ArrayList<>();
	
	@Relationship(type = "HAS_GROUP")
	private Set<Group> groups = new HashSet<>();

	@JsonIgnore
	@Relationship(type = "HAS_PATH_OBJECT")
	private List<LookseeObject> path_objects = new ArrayList<>();
	
	@Relationship(type = "HAS_RESULT")
	private PageState result;
	
	public Test(){}
	
	/**
	 * Constructs a test object
	 * 
	 * @param path that will be used to determine what the expected path should be
 	 * @param result
	 * @param domain
	 * @throws MalformedURLException 
	 * 
	 * @pre path_keys != null
	 * @pre !path_keys.isEmpty()
	 * @pre path_objects != null
	 * @pre !path_objects.isEmpty()
	 */
	public Test(List<String> path_keys, List<LookseeObject> path_objects, PageState result, boolean spansMultipleDomains) throws MalformedURLException{
		assert path_keys != null;
		assert !path_keys.isEmpty();
		assert path_objects != null;
		assert !path_objects.isEmpty();
		
		setPathKeys(path_keys);
		setPathObjects(path_objects);
		setResult(result);
		setRecords(new ArrayList<TestRecord>());
		setStatus(TestStatus.UNVERIFIED);
		setSpansMultipleDomains(spansMultipleDomains);
		setLastRunTimestamp(new Date());
		setName(generateTestName());
		setBrowserStatuses(new HashMap<String, String>());
		setArchived(false);
		setIsRunning(false);
		setKey(generateKey());
		setRunTime(0L);
	}
	
	/**
	 * Constructs a test object
	 * 
	 * @param path that will be used to determine what the expected path should be
 	 * @param result
	 * @param domain
	 * @throws MalformedURLException 
	 * 
	 * @pre path_keys != null
	 * @pre !path_keys.isEmpty()
	 * @pre path_objects != null
	 * @pre !path_objects.isEmpty()
	 */
	public Test(List<String> path_keys, List<LookseeObject> path_objects, PageState result, String name, boolean is_running, boolean spansMultipleDomains) throws MalformedURLException{
		assert path_keys != null;
		assert !path_keys.isEmpty();
		assert path_objects != null;
		assert !path_objects.isEmpty();
		
		setPathKeys(path_keys);
		setPathObjects(path_objects);
		setResult(result);
		setRecords(new ArrayList<TestRecord>());
		setStatus(TestStatus.UNVERIFIED);
		setSpansMultipleDomains(spansMultipleDomains);
		setLastRunTimestamp(new Date());
		setName(name);
		setBrowserStatuses(new HashMap<String, String>());
		setIsRunning(is_running);
		setArchived(false);
		setKey(generateKey());
		setRunTime(0L);
	}
	
	/**
	 * Checks if a {@code TestRecord} snapshot of a {@code Test} is passing or not
	 * 
	 * @param record
	 * @return
	 */
	public static TestStatus isTestPassing(PageState expected_page, PageState new_result_page, TestStatus last_test_passing_status){
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

	public TestStatus getStatus(){
		return this.status;
	}
	
	public void setStatus(TestStatus status){
		this.status = status;
	}
	
	public String getKey(){
		return this.key;
	}
	
	public void setKey(String key){
		this.key = key;
	}
	
	public String getName(){
		return this.name;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public List<String> getPathKeys(){
		return this.path_keys;
	}
	
	public void setPathKeys(List<String> path_keys){
		this.path_keys = path_keys;
	}
	
	public boolean addPathKey(String key) {
		return this.path_keys.add(key);
	}
	
	public void addRecord(TestRecord record){
		this.records.add(record);
	}
	
	public List<TestRecord> getRecords(){
		return this.records;
	}
	
	public void setRecords(List<TestRecord> records){
		this.records = records;
	}
	
	/**
	 * @return result of running the test. Can be either null or have a {@link PageState} set
	 */
	public PageState getResult(){
		return this.result;
	}
	
	/**
	 * @param result_page expected {@link PageState} state after running through path
	 */
	public void setResult(PageState result_page){
		this.result = result_page;
	}

	public boolean isUseful() {
		return isUseful;
	}

	public void setUseful(boolean isUseful) {
		this.isUseful = isUseful;
	}

	public boolean getSpansMultipleDomains() {
		return spansMultipleDomains;
	}

	public void setSpansMultipleDomains(boolean spansMultipleDomains) {
		this.spansMultipleDomains = spansMultipleDomains;
	}

	public Set<Group> getGroups() {
		return groups;
	}

	public void setGroups(Set<Group> groups) {
		this.groups = groups;
	}
	
	public void addGroup(Group group){
		this.groups.add(group);
	}
	
	public void removeGroup(Group group) {
		//remove edge between test and group
		this.groups.remove(group);
	}
	
	/**
	 * @return date timestamp of when test was last ran
	 */
	public Date getLastRunTimestamp(){
		return this.last_run_time;
	}
	
	/**
	 * sets date timestamp of when test was last ran
	 * 
	 * @param timestamp of last run as a {@link DateTime}
	 */
	public void setLastRunTimestamp(Date timestamp){
		this.last_run_time = timestamp;
	}

	public void setRunTime(long pathCrawlRunTime) {
		this.run_time_length = pathCrawlRunTime;
		
	}
	
	public long getRunTime() {
		return this.run_time_length;
	}

	public boolean isRunning() {
		return is_running;
	}

	public void setIsRunning(boolean is_running) {
		this.is_running = is_running;
	}

	public Map<String, String> getBrowserStatuses() {
		return browser_passing_statuses;
	}

	public void setBrowserStatuses(Map<String, String> browser_passing_statuses) {
		this.browser_passing_statuses = browser_passing_statuses;
	}

	public void addPathObject(LookseeObject path_obj) {
		this.path_objects.add(path_obj);
	}

	@JsonIgnore
	public List<LookseeObject> getPathObjects() {
		return this.path_objects;
	}

	@JsonIgnore
	public void setPathObjects(List<LookseeObject> path_objects) {
		this.path_objects = path_objects;
	}
	
	/**
	 * 
	 * @param browser_name name of browser (ie 'chrome', 'firefox')
	 * @param status boolean indicating passing or failing
	 * 
	 * @pre browser_name != null
	 */
	public void setBrowserStatus(String browser_name, String status){
		assert browser_name != null;
		getBrowserStatuses().put(browser_name, status);
	}
	
	/**
	 * 
	 * @return
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
	 * @return
	 */
	public String generateKey() {
		String path_key =  String.join("", getPathKeys());
		path_key += getResult().getKey();
		
		return "test"+org.apache.commons.codec.digest.DigestUtils.sha512Hex(path_key);
	}
	
	/**
	 * Clone {@link Test} object
	 * 
	 * @param path
	 * @return
	 * @throws MalformedURLException 
	 */
	public static Test clone(Test test) throws MalformedURLException{
		Test clone_test = new Test(new ArrayList<String>(test.getPathKeys()),
									   new ArrayList<LookseeObject>(test.getPathObjects()),
									   test.getResult(), 
									   test.getSpansMultipleDomains());

		clone_test.setBrowserStatuses(test.getBrowserStatuses());
		clone_test.setGroups(new HashSet<>(test.getGroups()));
		clone_test.setLastRunTimestamp(test.getLastRunTimestamp());
		clone_test.setStatus(test.getStatus());
		clone_test.setRunTime(test.getRunTime());
		
		return clone_test;
	}

	public boolean isArchived() {
		return archived;
	}

	public void setArchived(boolean is_archived) {
		this.archived = is_archived;
	}
	
	public String generateTestName() throws MalformedURLException {
		 String test_name = "";
			int page_state_idx = 0;
			int element_action_cnt = 0;
			for(LookseeObject obj : this.path_objects){
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
			}
			
			return test_name.trim();
	}
}
