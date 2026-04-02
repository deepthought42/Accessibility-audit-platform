package com.looksee.models.repository;

import com.looksee.models.message.BugMessage;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data interface for interacting with {@link BugMessage} objects in database
 */
@Repository
public interface BugMessageRepository extends Neo4jRepository<BugMessage, Long> {

	/**
	 * Finds a bug message by its message
	 *
	 * @param message the message
	 * @return the bug message
	 */
	@Query("MATCH (bm:BugMessage{message:$message}) RETURN bm LIMIT 1")
	public BugMessage findByMessage(@Param("message") String message);
}
