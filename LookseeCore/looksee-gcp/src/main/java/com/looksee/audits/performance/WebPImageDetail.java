package com.looksee.audits.performance;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * WebP image detail for performance audit
 */
@Getter
@Setter
@NoArgsConstructor
public class WebPImageDetail extends AuditDetail {

	private int wastedBytes;
	private String url;
	private boolean fromProtocol;
	private boolean crossOrigin;
	private int totalBytes;
	
	/**
	 * Constructor for {@link WebPImageDetail}
	 *
	 * @param wastedBytes the wasted bytes
	 * @param url the url
	 * @param fromProtocol the from protocol
	 * @param isCrossOrigin the is cross origin
	 * @param totalBytes the total bytes
	 *
	 * precondition: url != null
	 */
	public WebPImageDetail(int wastedBytes, String url, boolean fromProtocol, boolean isCrossOrigin, int totalBytes) {
		assert url != null : "url must not be null";

		setWastedBytes(wastedBytes);
		setUrl(url);
		setFromProtocol(fromProtocol);
		setCrossOrigin(isCrossOrigin);
		setTotalBytes(totalBytes);
	}

	@Override
	public String generateKey() {
		return "webpimagedetail"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(wastedBytes + url + fromProtocol + crossOrigin + totalBytes);
	}
}
