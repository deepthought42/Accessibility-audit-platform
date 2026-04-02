package com.looksee.services;

import com.looksee.models.audit.recommend.Recommendation;
import com.looksee.models.repository.RecommendationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for {@link Recommendation} objects
 */
@Service
public class RecommendationService {
	
	@Autowired
	private RecommendationRepository recommendation_repo;
	
	/**
	 * Saves a recommendation
	 * @param rec the recommendation to save
	 * @return the saved recommendation
	 *
	 * precondition: rec != null
	 */
	public Recommendation save(Recommendation rec) {
		assert rec != null;

		return recommendation_repo.save(rec);
	}
}
