package com.looksee.pageBuilder.gcp;

import java.util.Map;

public class SyntaxAnalysis {

	private Map<String, Boolean> moods;
	private Map<String, Boolean> voices;
	
	public Map<String, Boolean> getMoods() {
		return moods;
	}
	public void setMoods(Map<String, Boolean> moods) {
		this.moods = moods;
	}
	public Map<String, Boolean> getVoices() {
		return voices;
	}
	public void setVoices(Map<String, Boolean> voices) {
		this.voices = voices;
	}
}
