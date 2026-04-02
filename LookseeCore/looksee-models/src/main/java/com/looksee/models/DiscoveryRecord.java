package com.looksee.models;

import com.looksee.models.enums.ExecutionStatus;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Record detailing a "Discovery" ran by an account.
 *
 * <p><b>Class Invariants:</b>
 * <ul>
 *   <li>invariant: startTime is not null after parameterized construction</li>
 *   <li>invariant: browserName is not null after parameterized construction</li>
 *   <li>invariant: domainUrl is not null after parameterized construction</li>
 *   <li>invariant: expandedUrls list is never null after parameterized construction</li>
 *   <li>invariant: expandedPathKeys list is never null after parameterized construction</li>
 *   <li>invariant: totalPathCount >= 0</li>
 *   <li>invariant: testCount >= 0</li>
 * </ul>
 */
@Node
@Getter
@Setter
@NoArgsConstructor
public class DiscoveryRecord extends LookseeObject {
	
	private Date startTime;
	private String browserName;
	private String domainUrl;
	private Date lastPathRanAt;
	private ExecutionStatus status;
	private int totalPathCount;
	private int examinedPathCount;
	private int testCount;
	private List<String> expandedUrls;
	private List<String> expandedPathKeys;

	/**
	 * Creates a new discovery record
	 *
	 * @param started_timestamp the timestamp when the discovery started
	 * @param browser_name the name of the browser used for the discovery
	 * @param domain_url the url of the domain being discovered
	 * @param test_cnt the number of tests to run
	 * @param total_cnt the total number of paths to discover
	 * @param examined_cnt the number of paths that have been examined
	 * @param status the status of the discovery
	 */
	public DiscoveryRecord(Date started_timestamp, String browser_name, String domain_url,
							int test_cnt, int total_cnt, int examined_cnt,
							ExecutionStatus status){
		assert started_timestamp != null;
		assert browser_name != null;
		assert domain_url != null;
		assert test_cnt > -1;
		assert total_cnt > -1;

		setExpandedPathKeys(new ArrayList<String>());
		setExpandedUrls(new ArrayList<String>());
		setStartTime(started_timestamp);
		setBrowserName(browser_name);
		setDomainUrl(domain_url);
		setLastPathRanAt(new Date());
		setTotalPathCount(total_cnt);
		setExaminedPathCount(examined_cnt);
		setTestCount(test_cnt);
		setStatus(status);
		setKey(generateKey());
	}

	/**
	 * Generates a key for the discovery record
	 *
	 * @return the key for the discovery record
	 */
	public String generateKey() {
		return getDomainUrl()+":"+UUID.randomUUID().toString();
	}

	/**
	 * Adds an expanded path key to the discovery record
	 *
	 * @param expanded_path_key the expanded path key to add
	 * @return true if the expanded path key was added, false otherwise
	 *
	 * precondition: expanded_path_key != null
	 */
	public boolean addExpandedPathKey(String expanded_path_key) {
		assert expanded_path_key != null;
		if(!this.expandedPathKeys.contains(expanded_path_key)){
			return this.expandedPathKeys.add(expanded_path_key);
		}
		return false;
	}
}
