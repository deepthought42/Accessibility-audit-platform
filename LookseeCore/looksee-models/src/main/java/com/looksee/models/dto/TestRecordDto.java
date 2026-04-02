package com.looksee.models.dto;

import com.looksee.models.Test;
import com.looksee.models.TestRecord;
import com.looksee.models.enums.TestStatus;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for {@link TestRecord}
 */
@Getter
@Setter
public class TestRecordDto {
	private String key;
	private Date ranAt;
	private String browser;
	private TestStatus status;
	private long runTimeLength;
	private String testKey;
	private String resultKey;
	
	/**
	 * Constructor for {@link TestRecordDto}
	 * @param record the {@link TestRecord} to convert
	 * @param test_key the key of the {@link Test}
	 *
	 * precondition: record != null
	 * precondition: test_key != null
	 */
	public TestRecordDto(TestRecord record, String test_key){
		assert record != null;
		assert test_key != null;

		setKey(record.getKey());
		setRanAt(record.getRanAt());
		setBrowser(record.getBrowser());
		setStatus(record.getStatus());
		setRunTimeLength(record.getRunTime());
		setResultKey(record.getResult().getKey());
		setTestKey(test_key);
	}
}
