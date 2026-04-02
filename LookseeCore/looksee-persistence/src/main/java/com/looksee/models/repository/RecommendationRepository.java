package com.looksee.models.repository;

import com.looksee.models.audit.recommend.Recommendation;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link Recommendation} objects
 */
@Repository
@Retry(name = "neoforj")
public interface RecommendationRepository extends Neo4jRepository<Recommendation, Long> {

}
