package com.looksee.services;

import com.looksee.models.ActionOLD;
import com.looksee.models.repository.ActionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for interacting with {@link ActionOLD} objects
 */
@Service
public class ActionService {

	@Autowired
	private ActionRepository action_repo;
	
	/**
	 * Saves an {@link ActionOLD} object to the database
	 * 
	 * @param action {@link ActionOLD}
	 * @return {@link ActionOLD}
	 * 
	 * precondition: action != null
	 */
	public ActionOLD save(ActionOLD action){
		assert action != null;

		ActionOLD action_record = action_repo.findByKey(action.getKey());
		if(action_record == null){
			action_record = action_repo.save(action);
		}
		return action_record;
	}

	/**
	 * Retrieve data from database
	 *
	 * @param key key of the action to find
	 * @return {@link ActionOLD}
	 *
	 * precondition: key != null
	 * precondition: !key.isEmpty()
	 */
	public ActionOLD findByKey(String key) {
		assert key != null;
		assert !key.isEmpty();

		return action_repo.findByKey(key);
	}
}
