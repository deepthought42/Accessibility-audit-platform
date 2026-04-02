package com.looksee.services;

import com.looksee.models.message.BugMessage;
import com.looksee.models.repository.BugMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for {@link BugMessage}s
 */
@Service
public class BugMessageService {

	@Autowired
	private BugMessageRepository bug_message_repo;
	
	/**
	 * Objects are expected to be immutable as of 3/14/19. When this method is
	 * ran, if a {@link BugMessage} already exists with a given message then it
	 * will be loaded from the database, otherwise it will be saved
	 *
	 * @param bug_message {@link BugMessage}
	 * @return the saved {@link BugMessage}
	 *
	 * precondition: bug_message != null
	 */
	public BugMessage save(BugMessage bug_message){
		assert bug_message != null;
		BugMessage bug_message_record = bug_message_repo.findByMessage(bug_message.getMessage());
		if(bug_message_record == null){
			return bug_message_repo.save(bug_message);
		}
		return bug_message_record;
	}
}
