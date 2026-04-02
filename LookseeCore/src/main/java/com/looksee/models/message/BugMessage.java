package com.looksee.models.message;

import java.util.Date;

import com.looksee.models.LookseeObject;
import com.looksee.models.enums.BugType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * A BugMessage is a message that is used to report a bug
 *
 * invariant: message != null after parameterized construction
 * invariant: bugType is always a valid {@link BugType} string or null
 */
@NoArgsConstructor
@Getter
@Setter
public class BugMessage extends LookseeObject{
	/**
	 * The message of the bug
	 */
	private String message;

	/**
	 * The type of the bug
	 */
	private String bugType;

	/**
	 * The date the bug was identified
	 */
	private Date dateIdentified;
	
	/**
	 * Creates a new BugMessage
	 * @param message the message of the bug
	 * @param type the type of the bug
	 * @param date the date the bug was identified
	 *
	 * precondition: message != null
	 * precondition: type != null
	 * precondition: date != null
	 */
	public BugMessage(
		String message,
		BugType type,
		Date date
	) {
		assert message != null;
		assert type != null;
		assert date != null;

		setMessage(message);
		setBugType(type);
		setDateIdentified(date);
	}

	/**
	 * Checks if the bug message is equal to another object
	 * @param o the object to check
	 * @return true if the bug message is equal to the object, false otherwise
	 */
	public boolean equals(Object o) {
		if (this == o) return true;
        if (!(o instanceof BugMessage)) return false;
        
        BugMessage that = (BugMessage)o;
		return this.getMessage().equals(that.getMessage());
	}
	
	/*******************************
	 * GETTERS/SETTERS
	 *******************************/

	/**
	 * Returns the type of the bug
	 * @return the type of the bug
	 */
	public BugType getBugType() {
		return BugType.create(bugType);
	}

	/**
	 * Sets the type of the bug
	 * @param bugType the type of the bug
	 *
	 * precondition: bugType != null
	 */
	public void setBugType(BugType bugType) {
		assert bugType != null;

		this.bugType = bugType.toString();
	}

	/**
	 * Generates a key for the bug message
	 * @return the key for the bug message
	 */
	@Override
	public String generateKey() {
		return "bugmessage:"+getBugType()+getMessage();
	}
}
