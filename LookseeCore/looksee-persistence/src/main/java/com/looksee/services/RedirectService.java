package com.looksee.services;

import com.looksee.models.journeys.Redirect;
import com.looksee.models.repository.DomainRepository;
import com.looksee.models.repository.RedirectRepository;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for {@link Redirect} objects
 */
@Service
public class RedirectService {
	private static Logger log = LoggerFactory.getLogger(RedirectService.class.getName());

	@Autowired
	private RedirectRepository redirect_repo;
	
	@Autowired
	private DomainRepository domain_repo;
	
	/**
	 * Finds a redirect by its key
	 * @param key the key of the redirect
	 * @return the redirect
	 *
	 * precondition: key != null
	 * precondition: !key.isEmpty()
	 */
	public Redirect findByKey(String key){
		assert key != null;
		assert !key.isEmpty();

		return redirect_repo.findByKey(key);
	}
	
	/**
	 * Saves a redirect
	 * @param redirect the redirect to save
	 * @return the saved redirect
	 *
	 * precondition: redirect != null
	 */
	public Redirect save(Redirect redirect){
		assert redirect != null;

		Redirect record = findByKey(redirect.getKey());
		if(record == null){
			log.warn("redirect key   :: "+redirect.getKey());
			log.warn("redirect urls  ::  " + redirect.getUrls());
			log.warn("redirect repo :: " + redirect_repo);
			record = redirect_repo.save(redirect);
		}
		return record;
	}

	/**
	 * Gets the redirects for a domain
	 * @param account_id the ID of the account
	 * @param url the url of the domain
	 * @return the redirects for the domain
	 *
	 * precondition: account_id > 0
	 * precondition: url != null
	 * precondition: !url.isEmpty()
	 */
	public Set<Redirect> getRedirects(long account_id, String url) {
		assert account_id > 0;
		assert url != null;
		assert !url.isEmpty();

		return domain_repo.getRedirects(account_id, url);
	}
}
