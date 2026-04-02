package com.looksee.pageBuilder.models;

public class Label extends LookseeObject{
	private String description;
	private float score;
	
	public Label() {
		setDescription("");
		setScore(0.0F);
	}
	
	public Label(String description, float score) {
		setDescription(description);
		setScore(score);
		setKey(generateKey());
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public float getScore() {
		return score;
	}

	public void setScore(float score) {
		this.score = score;
	}

	@Override
	public String generateKey() {
		return "label::"+getDescription();
	}
}
