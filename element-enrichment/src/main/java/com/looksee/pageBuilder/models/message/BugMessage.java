package com.looksee.pageBuilder.models.message;

import java.util.Date;

import com.looksee.pageBuilder.models.enums.BugType;
import com.looksee.pageBuilder.models.LookseeObject;


/**
 * 
 */
public class BugMessage extends LookseeObject{
	private String message;
	private String bug_type;
	private Date date_identified;
	
	public BugMessage() {}
	
	public BugMessage(
		String message,
		BugType type,
		Date date
	) {
		setMessage(message);
		setBugType(type);
		setDateIdentified(date);
	}
	
	public boolean equals(Object o) {
		if (this == o) return true;
        if (!(o instanceof BugMessage)) return false;
        
        BugMessage that = (BugMessage)o;
		return this.getMessage().equals(that.getMessage());
	}
	
	/*******************************
	 * GETTERS/SETTERS
	 *******************************/
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public BugType getBugType() {
		return BugType.create(bug_type);
	}
	public void setBugType(BugType bug_type) {
		this.bug_type = bug_type.toString();
	}
	public Date getDateIdentified() {
		return date_identified;
	}
	public void setDateIdentified(Date date_identified) {
		this.date_identified = date_identified;
	}

	@Override
	public String generateKey() {
		return "bugmessage:"+getBugType()+getMessage();
	}
}
