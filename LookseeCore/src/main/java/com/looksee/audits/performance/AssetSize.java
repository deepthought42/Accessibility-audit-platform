package com.looksee.audits.performance;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Defines detail item for "total-byte-weight" and  in the Google PageSpeed API
 */
@Getter
@Setter
@NoArgsConstructor
public class AssetSize extends AuditDetail {

	private String url;
	private Integer totalBytes;
	
	/**
	 * Constructs an {@link AssetSize} object
	 *
	 * @param url url of the asset
	 * @param total_bytes total bytes of the asset
	 *
	 * precondition: url != null
	 * precondition: total_bytes > 0
	 */
	public AssetSize(String url, Integer total_bytes) {
		assert url != null : "url must not be null";
		assert total_bytes != null : "total_bytes must not be null";

		setUrl(url);
		setTotalBytes(total_bytes);
	}

	@Override
	public String generateKey() {
		return "assetsize"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(url);
	}
}
