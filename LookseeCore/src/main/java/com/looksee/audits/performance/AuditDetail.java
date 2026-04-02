package com.looksee.audits.performance;

import com.looksee.models.LookseeObject;
import java.util.Date;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Abstract class for audit details
 */
@Getter
@Setter
@NoArgsConstructor
@Node
public abstract class AuditDetail extends LookseeObject {

	private Date createdDate = new Date();

	@Override
	public String generateKey() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'generateKey'");
	}
}
