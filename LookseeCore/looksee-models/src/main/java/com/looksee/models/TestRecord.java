package com.looksee.models;

import com.looksee.models.enums.TestStatus;
import java.util.Date;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * A {@link Test} record for reflecting an execution of a test
 * indicating whether the execution is aligned with the test and therefore status
 * or mis-aligned with the expectations of the test and therefore failing in
 * which case a {@link PageState} can be saved as a record of what the state of the page
 * was after the test was executed.
 *
 * invariant: ranAt != null
 * invariant: browser != null
 * invariant: status != null
 * invariant: result != null
 * invariant: pathKeys != null
 */
@Node
@Getter
@Setter
@NoArgsConstructor
public class TestRecord extends LookseeObject {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(TestRecord.class);

	private Date ranAt;
	private String browser;
	private TestStatus status;
	private long runTime;
	private List<String> pathKeys;

	@Relationship(type = "HAS_RESULT", direction = Relationship.Direction.OUTGOING)
	private PageState result;
	
	/**
	 * Constructor for {@link TestRecord}
	 *
	 * @param ranAt the date the test was run
	 * @param status the status of the test
	 * @param browser_name the name of the browser
	 * @param result the result of the test
	 * @param runTime the time the test was run
	 * @param pathKeys the keys of the path
	 *
	 * precondition: ranAt != null
	 * precondition: status != null
	 * precondition: browser_name != null
	 * precondition: !browser_name.isEmpty()
	 * precondition: result != null
	 * precondition: runTime > 0
	 * precondition: pathKeys != null
	 * precondition: !pathKeys.isEmpty()
	 */
	public TestRecord(Date ranAt,
					TestStatus status,
					String browser_name,
					PageState result,
					long runTime,
					List<String> pathKeys)
	{
		assert ranAt != null;
		assert status != null;
		assert browser_name != null;
		assert !browser_name.isEmpty();
		assert result != null;
		assert runTime > 0;
		assert pathKeys != null;
		assert !pathKeys.isEmpty();

		setRanAt(ranAt);
		setResult(result);
		setRunTime(runTime);
		setStatus(status);
		setBrowser(browser_name);
		setPathKeys(pathKeys);
		setKey(generateKey());
	}
		
	/**
	 * Generates a key for this object
	 * @return generated key
	 */
	@Override
	public String generateKey() {
		return "testrecord"+getRanAt().hashCode()+getResult().getKey();
	}
}
