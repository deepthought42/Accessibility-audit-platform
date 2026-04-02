package com.looksee.journeyMapCleanup.models.repository;


import java.util.Set;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.journeyMapCleanup.models.journeys.DomainMap;


@Repository
public interface DomainMapRepository extends Neo4jRepository<DomainMap, Long>{

	@Query("MATCH (map:DomainMap{key:$key}) RETURN map LIMIT 1")
	public DomainMap findByKey(@Param("key") String domain_map_key);

	@Query("MATCH (d:Domain) with d WHERE id(d)=$domain_id MATCH (d)-[:CONTAINS]->(map:DomainMap) RETURN map")
	public DomainMap findByDomainId(@Param("domain_id") long domain_id);

	@Query("MATCH (dm:DomainMap) MATCH (journey:Journey) WHERE id(dm)=$domain_map_id AND id(journey)=$journey_id MERGE (dm)-[:CONTAINS]->(journey) RETURN dm LIMIT 1")
	public DomainMap addJourneyToDomainMap(@Param("journey_id") long journey_id, @Param("domain_map_id") long domain_map_id);

	@Query("MATCH (ar:DomainAuditRecord) with ar WHERE id(ar)=$audit_record_id MATCH (ar)-[:CONTAINS]->(map:DomainMap) RETURN map")
	public DomainMap findByDomainAuditId(@Param("audit_record_id") long audit_record_id);

	@Query("MATCH (dm:DomainMap) WHERE id(dm)=$map_id MATCH (page:PageState) WHERE id(page)=$page_state_id MERGE (dm)-[h:HAS]->(page) RETURN dm")
	public void addPageToDomainMap(@Param("map_id") long map_id, @Param("page_state_id") long page_state_id);

	@Query("MATCH (dm:DomainMap) WHERE duration.inSeconds(dm.createdAt, datetime()).days <= $days RETURN dm")
    public Set<DomainMap> getAllMapsWithinLastDay(@Param("days") int number_of_days);
}
