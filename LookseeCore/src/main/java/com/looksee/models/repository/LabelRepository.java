package com.looksee.models.repository;

import com.looksee.models.Label;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Set;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Spring Data Neo4j to handle interactions with {@link Label} objects
 */
@Repository
@Retry(name = "neoforj")
public interface LabelRepository extends Neo4jRepository<Label, Long> {
	
	/**
	 * Finds a label by key
	 *
	 * @param key the key of the label
	 * @return the label
	 *
	 * precondition: key != null
	 * precondition: !key.isEmpty()
	 */
	@Query("MATCH (e:Label{key:$key}) RETURN e LIMIT 1")
	public Label findByKey(@Param("key") String key);
	
	/**
	 * Retrieves labels for image elements for a given audit record id
	 *
	 * @param audit_record_id the id of the audit record
	 * @return the labels for the image elements
	 *
	 * precondition: audit_record_id > 0
	 */
	@Query("MATCH (audit_record:AuditRecord) WITH audit_record WHERE id(audit_record)=$audit_record_id MATCH (audit_record)-[*]->(element:ImageElementState) MATCH (element)-[]->(label:Label) RETURN label")
	public Set<Label> getLabelsForImageElements(@Param("audit_record_id") long audit_record_id);
}
