package com.looksee.journeyErrors.models.repository;

import java.util.Set;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.journeyErrors.models.Label;

import io.github.resilience4j.retry.annotation.Retry;

@Repository
@Retry(name = "neoforj")
public interface LabelRepository extends Neo4jRepository<Label, Long> {
	
	@Query("MATCH (e:Label{key:$key}) RETURN e LIMIT 1")
	public Label findByKey(@Param("key") String key);
	
	@Query("MATCH (audit_record:AuditRecord) WITH audit_record WHERE id(audit_record)=$audit_record_id MATCH (audit_record)-[*]->(element:ImageElementState) MATCH (element)-[]->(label:Label) RETURN label")
	public Set<Label> getLabelsForImageElements(@Param("audit_record_id") long id);
}
