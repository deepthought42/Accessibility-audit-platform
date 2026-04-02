package com.looksee.services;


import com.looksee.models.Animation;
import com.looksee.models.repository.AnimationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for interacting with {@link Animation} objects
 */
@Service
public class AnimationService {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(AnimationService.class.getName());

	@Autowired
	private AnimationRepository animation_repo;
	
	/**
	 * Finds an {@link Animation} by key
	 *
	 * @param key key of the animation to find
	 * @return {@link Animation}
	 *
	 * precondition: key != null
	 * precondition: !key.isEmpty()
	 */
	public Animation findByKey(String key){
		assert key != null;
		assert !key.isEmpty();

		return animation_repo.findByKey(key);
	}
	
	/**
	 * Saves an {@link Animation} object to the database
	 * 
	 * @param animation {@link Animation} to save
	 * @return {@link Animation}
	 * 
	 * precondition: animation != null
	 */
	public Animation save(Animation animation){
		assert animation != null;

		Animation record = findByKey(animation.getKey());
		if(record == null){
			record = animation_repo.save(animation);
		}
		return record;
	}
}
