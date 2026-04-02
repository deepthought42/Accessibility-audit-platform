package com.looksee.models.message;

import com.looksee.models.Account;
import com.looksee.models.competitiveanalysis.Competitor;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a message for a {@link Competitor}
 */
@Getter
@Setter
public class CompetitorMessage extends Message {

	private Competitor competitor;
	
	/**
	 * Constructs a {@link CompetitorMessage}
	 *
	 * @param competitor the {@link Competitor}
	 *
	 * precondition: competitor != null
	 */
	public CompetitorMessage(Competitor competitor) {
		super();

		assert competitor != null : "competitor must not be null";
	}

	/**
	 * Constructs a {@link CompetitorMessage}
	 *
	 * @param competitor_id the id of the {@link Competitor}
	 * @param account_id the id of the {@link Account}
	 * @param competitor the {@link Competitor}
	 *
	 * precondition: competitor != null
	 */
	public CompetitorMessage(long competitor_id, long account_id, Competitor competitor) {
		super(account_id);

		assert competitor != null : "competitor must not be null";

		setCompetitor(competitor);
	}
}
