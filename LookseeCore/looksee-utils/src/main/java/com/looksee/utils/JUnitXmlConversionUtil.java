package com.looksee.utils;

import com.looksee.models.TestRecord;
import com.looksee.models.enums.TestStatus;
import java.util.Date;
import java.util.List;

/**
 * Utility class for converting test records to JUnit XML format
 */
public class JUnitXmlConversionUtil {

	/**
	 * Converts a list of test records to JUnit XML format
	 *
	 * @param test_record_list {@link List} of {@link TestRecord}s to convert
	 * @param failing_cnt the number of failing tests
	 * @param time_in_sec the time in seconds
	 * @param date the date
	 * @return the JUnit XML string
	 *
	 * precondition: test_record_list != null
	 * precondition: date != null
	 */
	public static String convertToJUnitXml(List<TestRecord> test_record_list, int failing_cnt, long time_in_sec, Date date){
		assert test_record_list != null;
		assert date != null;

		StringBuffer str_buf = new StringBuffer();
		
		str_buf.append("<testsuites id='' name='' tests='" +test_record_list.size()+ "' failures='" + failing_cnt + "' time='" + time_in_sec + "'>\n");
		str_buf.append("<testsuite id='' name='' skipped='' tests='" +test_record_list.size()+ "' failures='" + failing_cnt + "' time='" + time_in_sec + "' timestamp='" + date.toString() + "'>\n");
		
		for(TestRecord record : test_record_list){
			str_buf.append("<testcase id='' name='" + record.getKey()+ "' time='" + record.getRunTime() + "'>\n");
			
			if(record.getStatus().equals(TestStatus.FAILING)){
				str_buf.append("<failure message='' type='WARNING' >\n");
				str_buf.append("ERROR MESSAGE HERE. (THE ABILITY TO TRACK ERROR MESSAGES IS NOT CURRENTLY SUPPORTED)");
				str_buf.append("</failure>\n");
			}
			
			str_buf.append("</testcase>\n");
		}
		
		str_buf.append("</testsuite>\n");
		str_buf.append("</testsuites>\n");
		return str_buf.toString();
	}
}
