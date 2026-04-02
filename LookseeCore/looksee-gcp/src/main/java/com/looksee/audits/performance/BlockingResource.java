package com.looksee.audits.performance;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a blocking resource
 */
@Getter
@Setter
@NoArgsConstructor
public class BlockingResource extends AuditDetail {

	private String url;
	private Integer totalBytes;
	private Double wastedMs;
	
	/**
	 * Constructs a {@link BlockingResource} object
	 *
	 * @param url url of the blocking resource
	 * @param total_bytes total bytes of the blocking resource
	 * @param wasted_ms wasted milliseconds of the blocking resource
	 *
	 * precondition: url != null
	 * precondition: total_bytes != null
	 * precondition: wasted_ms != null
	 */
	public BlockingResource(String url, Integer total_bytes, Double wasted_ms) {
		assert url != null : "url must not be null";
		assert total_bytes != null : "total_bytes must not be null";
		assert wasted_ms != null : "wasted_ms must not be null";

		setUrl(url);
		setTotalBytes(total_bytes);
		setWastedMs(wasted_ms);
	}

	@Override
	public String generateKey() {
		return "blockingresource"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(url);
	}
}
