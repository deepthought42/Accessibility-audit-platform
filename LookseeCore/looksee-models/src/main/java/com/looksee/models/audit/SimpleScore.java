package com.looksee.models.audit;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Simple score object for an audit.
 *
 * <p><b>Invariants:</b>
 * <ul>
 *   <li>datePerformed is never null when constructed via the parameterized constructor.</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
public class SimpleScore {

	private LocalDateTime datePerformed;
	private double score;
	
	/**
	 * Constructor for {@link SimpleScore}
	 * @param datePerformed the date the score was performed
	 * @param score the score
	 *
	 * precondition: datePerformed != null
	 */
	public SimpleScore(LocalDateTime datePerformed, double score) {
		assert datePerformed != null;
		setDatePerformed(datePerformed);
		setScore(score);
	}
}
