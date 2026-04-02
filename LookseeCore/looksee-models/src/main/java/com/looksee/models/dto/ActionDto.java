package com.looksee.models.dto;

import com.looksee.models.ActionOLD;
import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object for {@link ActionOLD}
 */
@Getter
@Setter
public class ActionDto {

	private String key;
	private String value;
	private String name;
	
	/**
	 * Constructs an {@link ActionDto} object from an {@link ActionOLD} object
	 *
	 * @param action {@link ActionOLD} object to construct the {@link ActionDto} from
	 *
	 * precondition: action != null
	 */
	public ActionDto(ActionOLD action){
		assert action != null;

		setKey(action.getKey());
		setValue(action.getValue());
		setName(action.getName());
	}
}
