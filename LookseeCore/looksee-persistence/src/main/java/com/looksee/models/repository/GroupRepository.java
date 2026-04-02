package com.looksee.models.repository;

import com.looksee.models.Group;
import java.util.Set;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for {@link Group}s
 */
public interface GroupRepository extends Neo4jRepository<Group, Long> {

	/**
	 * Finds a {@link Group} by its key
	 *
	 * @param key the key
	 * @return the group
	 */
	public Group findByKey(@Param("key") String key);

	/**
	 * Gets the groups for a test
	 *
	 * @param key the key
	 * @return the groups
	 */
	@Query("MATCH (:Test{key:$key})-[:HAS_GROUP]-(g) RETURN g")
	public Set<Group> getGroups(@Param("key") String key);
	
}
