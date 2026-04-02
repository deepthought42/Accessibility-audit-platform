package com.looksee.frontEndBroadcaster.models;

import java.util.Date;
import java.util.List;

import org.aspectj.weaver.ast.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import com.looksee.frontEndBroadcaster.models.enums.TestStatus;

/**
 * A {@link Test} record for reflecting an execution of a test 
 * indicating whether the execution is aligned with the test and therefore status
 * or mis-aligned with the expectations of the test and therefore failing in 
 * which case a {@link PageState} can be saved as a record of what the state of the page
 * was after the test was executed.
 *
 */
@Node
public class TestRecord implements Persistable {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(TestRecord.class);

	@GeneratedValue
    @Id
	private Long id;
	
	private String key;
	private Date ran_at;
	private String browser;
	private TestStatus status;
	private long run_time_length;
	private List<String> path_keys;

	@Relationship(type = "HAS_RESULT", direction = Relationship.Direction.OUTGOING)
	private PageState result;
	
	//Empty constructor for spring
	public TestRecord(){}
	
	public TestRecord(Date ran_at, TestStatus status, String browser_name, PageState result, long run_time, List<String> path_keys){
		setRanAt(ran_at);
		setResult(result);
		setRunTime(run_time);
		setStatus(status);
		setBrowser(browser_name);
		setPathKeys(path_keys);
		setKey(generateKey());
	}
	
	/**
	 * @return {@link Date} when test was ran
	 */
	public Date getRanAt(){
		return ran_at;
	}
	
	/**
	 * @return {@link Date} when test was ran
	 */
	public void setRanAt(Date date){
		this.ran_at = date;
	}
	
	public PageState getResult() {
		return this.result;
	}

	public void setResult(PageState page) {
		this.result = page;
	}
	
	/**
	 * @return whether or not the test passes compared to expected {@link Test test} path
	 */
	public TestStatus getStatus(){
		return this.status;
	}
	
	/**
	 * @return whether or not the test passes compared to expected {@link Test test} path
	 */
	public void setStatus(TestStatus status){
		this.status = status;
	}
	
	public String getKey() {
		return this.key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public void setRunTime(long pathCrawlRunTime) {
		this.run_time_length = pathCrawlRunTime;
	}
	
	public long getRunTime() {
		return this.run_time_length;
	}

	public String getBrowser() {
		return browser;
	}

	public void setBrowser(String browser) {
		this.browser = browser;
	}
		
	/**
	 * Generates a key for this object
	 * @return generated key
	 */
	@Override
	public String generateKey() {
		return "testrecord"+getRanAt().hashCode()+getResult().getKey();
	}

	public List<String> getPathKeys() {
		return path_keys;
	}

	public void setPathKeys(List<String> path_keys) {
		this.path_keys = path_keys;
	}
}
