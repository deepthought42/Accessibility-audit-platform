package com.looksee.audits.performance;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Network request detail for a page audit
 */
@Getter
@Setter
@NoArgsConstructor
public class NetworkRequestDetail extends AuditDetail {

	private Integer transferSize;
	private String url;
	private Integer statusCode;
	private String resourceType;
	private String mimeType;
	private Integer resourceSize;
	private Double endTime;
	private Double startTime;
	
	/**
	 * Constructor for {@link NetworkRequestDetail}
	 * @param transferSize the transfer size
	 * @param url the url
	 * @param statusCode the status code
	 * @param resourceType the resource type
	 * @param mimeType the mime type
	 * @param resourceSize the resource size
	 * @param endTime the end time
	 * @param startTime the start time
	 *
	 * precondition: transferSize != null
	 * precondition: url != null
	 * precondition: statusCode != null
	 * precondition: resourceType != null
	 * precondition: mimeType != null
	 * precondition: resourceSize != null
	 * precondition: endTime != null
	 * precondition: startTime != null
	 */
	public NetworkRequestDetail(Integer transferSize,
								String url,
								Integer statusCode,
								String resourceType,
								String mimeType,
								Integer resourceSize,
								Double endTime,
								Double startTime) {
		assert transferSize != null : "transferSize must not be null";
		assert url != null : "url must not be null";
		assert statusCode != null : "statusCode must not be null";
		assert resourceType != null : "resourceType must not be null";
		assert mimeType != null : "mimeType must not be null";
		assert resourceSize != null : "resourceSize must not be null";
		assert endTime != null : "endTime must not be null";
		assert startTime != null : "startTime must not be null";

		setTransferSize(transferSize);
		setUrl(url);
		setStatusCode(statusCode);
		setResourceType(resourceType);
		setMimeType(mimeType);
		setResourceSize(resourceSize);
		setEndTime(endTime);
		setStartTime(startTime);
	}

	@Override
	public String generateKey() {
		return "networkrequestdetail"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(url + transferSize.toString() + statusCode.toString() + resourceType.toString() + mimeType.toString() + resourceSize.toString() + endTime.toString() + startTime.toString());
	}
}
