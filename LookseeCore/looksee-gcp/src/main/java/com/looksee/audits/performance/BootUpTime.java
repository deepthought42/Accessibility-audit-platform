package com.looksee.audits.performance;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Defines detail item for "bootup-time" in the Google PageSpeed API 
 */
@Getter
@Setter
@NoArgsConstructor
public class BootUpTime extends AuditDetail {
	
	private String url;
	private Double totalTime;
	private Double scriptLoadTime;
	private Double scriptParseTime;
	
	/**
	 * Constructs a {@link BootUpTime} object
	 *
	 * @param url the url of the bootup time
	 * @param total_time the total time of the bootup time
	 * @param script_load_time the script load time of the bootup time
	 * @param script_parse_time the script parse time of the bootup time
	 *
	 * precondition: url != null
	 */
	public BootUpTime(String url, double total_time, double script_load_time, double script_parse_time) {
		assert url != null : "url must not be null";

		setUrl(url);
		setTotalTime(total_time);
		setScriptLoadTime(script_load_time);
		setScriptParseTime(script_parse_time);
	}

	@Override
	public String generateKey() {
		return "bootuptime"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(url);
	}
}
