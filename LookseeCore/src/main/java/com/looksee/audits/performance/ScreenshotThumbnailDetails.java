package com.looksee.audits.performance;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Screenshot thumbnail details
 */
@Getter
@Setter
@NoArgsConstructor
public class ScreenshotThumbnailDetails extends AuditDetail {

	private Double timing;
	private Long timestamp;
	private String data;
	
	/**
	 * Constructor for {@link ScreenshotThumbnailDetails}
	 *
	 * @param timing the timing
	 * @param timestamp the timestamp
	 * @param data the data
	 *
	 * precondition: timing != null
	 * precondition: timestamp != null
	 * precondition: data != null
	 */
	public ScreenshotThumbnailDetails(Double timing, Long timestamp, String data) {
		assert timing != null : "timing must not be null";
		assert timestamp != null : "timestamp must not be null";
		assert data != null : "data must not be null";

		setTiming(timing);
		setTimestamp(timestamp);
		setData(data);
	}

	@Override
	public String generateKey() {
		return "screenshotthumbnaildetails"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(timing.toString() + timestamp.toString() + data);
	}
}
