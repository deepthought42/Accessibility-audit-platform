package com.looksee.audits.performance;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.CompositeProperty;

/**
 * Third party summary detail
 */
@Getter
@Setter
@NoArgsConstructor
public class ThirdPartySummaryDetail extends AuditDetail {

	private Integer transferSize;
	private Double blockingTime;
	private Double mainThreadTime;
	
	@CompositeProperty
	private Map<String, String> entity = new HashMap<>();
	
	/**
	 * Constructor for {@link ThirdPartySummaryDetail}
	 *
	 * @param transferSize the transfer size
	 * @param blockingTime the blocking time
	 * @param mainThreadTime the main thread time
	 * @param entity the entity
	 *
	 * precondition: entity != null
	 */
	public ThirdPartySummaryDetail(int transferSize,
									double blockingTime,
									double mainThreadTime,
									Map<String, String> entity) {
		assert entity != null : "entity must not be null";

		setTransferSize(transferSize);
		setBlockingTime(blockingTime);
		setMainThreadTime(mainThreadTime);
		setEntity(entity);
	}

	/**
	 * Generates a key for the third party summary detail
	 * @return the key
	 */
	@Override
	public String generateKey() {
		return "thirdpartysummarydetail"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(transferSize.toString() + blockingTime.toString() + mainThreadTime.toString() + entity.toString());
	}
}
