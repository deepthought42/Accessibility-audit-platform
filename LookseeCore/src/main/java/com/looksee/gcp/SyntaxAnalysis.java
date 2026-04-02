package com.looksee.gcp;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a syntax analysis of a text.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SyntaxAnalysis {

	/**
	 * The moods of the text.
	 */
	private Map<String, Boolean> moods;

	/**
	 * The voices of the text.
	 */
	private Map<String, Boolean> voices;
}
