package com.looksee.contentAudit.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.Test;

import com.looksee.models.ElementState;
import com.looksee.models.audit.Score;

public class ReadabilityAuditUnitTest {

	@Test
	public void calculateSentenceScoreReturnsFullPointsForShortSentence() {
		Score score = ReadabilityAudit.calculateSentenceScore("This sentence has only a few words.");

		assertEquals(2, score.getPointsAchieved());
		assertEquals(2, score.getMaxPossiblePoints());
		assertTrue(score.getIssueMessages().isEmpty());
	}

	@Test
	public void calculateSentenceScoreReturnsPartialPointsForMediumSentence() {
		Score score = ReadabilityAudit.calculateSentenceScore(
			"This sentence has more than ten words but remains short enough to avoid the longest penalty tier.");

		assertEquals(1, score.getPointsAchieved());
		assertEquals(2, score.getMaxPossiblePoints());
	}

	@Test
	public void calculateSentenceScoreReturnsNoPointsForLongSentence() {
		Score score = ReadabilityAudit.calculateSentenceScore(
			"This sentence intentionally includes enough additional words to ensure it exceeds twenty words and is scored in the lowest readability sentence scoring category.");

		assertEquals(0, score.getPointsAchieved());
		assertEquals(2, score.getMaxPossiblePoints());
	}

	@Test
	public void calculateSentenceScoreReturnsFullPointsForNullInput() {
		Score score = ReadabilityAudit.calculateSentenceScore(null);

		assertEquals(2, score.getPointsAchieved());
		assertEquals(2, score.getMaxPossiblePoints());
	}

	@Test
	public void calculateSentenceScoreReturnsFullPointsForBlankInput() {
		Score score = ReadabilityAudit.calculateSentenceScore("   ");

		assertEquals(2, score.getPointsAchieved());
		assertEquals(2, score.getMaxPossiblePoints());
	}

	@Test
	public void calculateSentenceScoreReturnsFullPointsForExactlyTenWords() {
		Score score = ReadabilityAudit.calculateSentenceScore("one two three four five six seven eight nine ten");

		assertEquals(2, score.getPointsAchieved());
		assertEquals(2, score.getMaxPossiblePoints());
	}

	@Test
	public void calculateSentenceScoreReturnsPartialForElevenWords() {
		Score score = ReadabilityAudit.calculateSentenceScore("one two three four five six seven eight nine ten eleven");

		assertEquals(1, score.getPointsAchieved());
		assertEquals(2, score.getMaxPossiblePoints());
	}

	@Test
	public void calculateSentenceScoreReturnsPartialForExactlyTwentyWords() {
		Score score = ReadabilityAudit.calculateSentenceScore(
			"one two three four five six seven eight nine ten eleven twelve thirteen fourteen fifteen sixteen seventeen eighteen nineteen twenty");

		assertEquals(1, score.getPointsAchieved());
		assertEquals(2, score.getMaxPossiblePoints());
	}

	@Test
	public void calculateSentenceScoreReturnsZeroForTwentyOneWords() {
		Score score = ReadabilityAudit.calculateSentenceScore(
			"one two three four five six seven eight nine ten eleven twelve thirteen fourteen fifteen sixteen seventeen eighteen nineteen twenty twentyone");

		assertEquals(0, score.getPointsAchieved());
		assertEquals(2, score.getMaxPossiblePoints());
	}

	@Test
	public void calculateParagraphScoreReturnsExpectedValues() {
		assertEquals(1, ReadabilityAudit.calculateParagraphScore(5).getPointsAchieved());
		assertEquals(0, ReadabilityAudit.calculateParagraphScore(6).getPointsAchieved());
	}

	@Test
	public void calculateParagraphScoreReturnsOneForZeroSentences() {
		assertEquals(1, ReadabilityAudit.calculateParagraphScore(0).getPointsAchieved());
		assertEquals(1, ReadabilityAudit.calculateParagraphScore(0).getMaxPossiblePoints());
	}

	@Test
	public void calculateParagraphScoreReturnsOneForOneSentence() {
		assertEquals(1, ReadabilityAudit.calculateParagraphScore(1).getPointsAchieved());
	}

	@Test
	public void calculateParagraphScoreReturnsZeroForManySentences() {
		assertEquals(0, ReadabilityAudit.calculateParagraphScore(100).getPointsAchieved());
		assertEquals(1, ReadabilityAudit.calculateParagraphScore(100).getMaxPossiblePoints());
	}

	@Test
	public void privateHelpersHandleNullAndConsumerLabels() throws Exception {
		ReadabilityAudit audit = new ReadabilityAudit();

		Method countWordsMethod = ReadabilityAudit.class.getDeclaredMethod("countWords", String.class);
		countWordsMethod.setAccessible(true);
		assertEquals(0, ((Integer) countWordsMethod.invoke(null, (String) null)).intValue());
		assertEquals(0, ((Integer) countWordsMethod.invoke(null, "   ")).intValue());
		assertEquals(3, ((Integer) countWordsMethod.invoke(null, "alpha beta gamma")).intValue());

		Method xpathContainsMethod = ReadabilityAudit.class.getDeclaredMethod("xpathContains", String.class, String.class);
		xpathContainsMethod.setAccessible(true);
		assertTrue((Boolean) xpathContainsMethod.invoke(null, "/html/body/div", "body"));
		assertFalse((Boolean) xpathContainsMethod.invoke(null, null, "body"));
		assertFalse((Boolean) xpathContainsMethod.invoke(null, "/html/body", null));

		Method consumerTypeMethod = ReadabilityAudit.class.getDeclaredMethod("getConsumerType", String.class);
		consumerTypeMethod.setAccessible(true);
		assertEquals("the average consumer", consumerTypeMethod.invoke(audit, (Object) null));
		assertEquals("users with a College education", consumerTypeMethod.invoke(audit, "College"));
	}

	@Test
	public void generateIssueDescriptionIncludesElementTextAndEducationAudience() throws Exception {
		ReadabilityAudit audit = new ReadabilityAudit();
		ElementState element = mock(ElementState.class);
		when(element.getAllText()).thenReturn("Complex sample content");

		Method method = ReadabilityAudit.class.getDeclaredMethod("generateIssueDescription", ElementState.class, String.class, String.class);
		method.setAccessible(true);

		String description = (String) method.invoke(audit, element, "difficult", "HS");

		assertTrue(description.contains("Complex sample content"));
		assertTrue(description.contains("difficult"));
		assertTrue(description.contains("users with a HS education"));
	}

	@Test
	public void generateIssueDescriptionWithNullEducation() throws Exception {
		ReadabilityAudit audit = new ReadabilityAudit();
		ElementState element = mock(ElementState.class);
		when(element.getAllText()).thenReturn("Some text");

		Method method = ReadabilityAudit.class.getDeclaredMethod("generateIssueDescription", ElementState.class, String.class, String.class);
		method.setAccessible(true);

		String description = (String) method.invoke(audit, new Object[] { element, "easy", null });

		assertTrue(description.contains("Some text"));
		assertTrue(description.contains("easy"));
		assertTrue(description.contains("the average consumer"));
	}

	@Test
	public void getPointsForEducationLevelUsesExpectedBands() throws Exception {
		ReadabilityAudit audit = new ReadabilityAudit();
		Method method = ReadabilityAudit.class.getDeclaredMethod("getPointsForEducationLevel", double.class, String.class);
		method.setAccessible(true);

		// 90+ band
		assertEquals(4, ((Integer) method.invoke(audit, 95.0d, null)).intValue());
		assertEquals(4, ((Integer) method.invoke(audit, 95.0d, "HS")).intValue());
		assertEquals(4, ((Integer) method.invoke(audit, 95.0d, "College")).intValue());
		assertEquals(3, ((Integer) method.invoke(audit, 95.0d, "Advanced")).intValue());
		assertEquals(4, ((Integer) method.invoke(audit, 90.0d, "Other")).intValue());

		// 80-89 band
		assertEquals(4, ((Integer) method.invoke(audit, 85.0d, null)).intValue());
		assertEquals(4, ((Integer) method.invoke(audit, 85.0d, "HS")).intValue());
		assertEquals(4, ((Integer) method.invoke(audit, 85.0d, "College")).intValue());
		assertEquals(4, ((Integer) method.invoke(audit, 85.0d, "Advanced")).intValue());
		assertEquals(4, ((Integer) method.invoke(audit, 85.0d, "Other")).intValue());

		// 70-79 band
		assertEquals(4, ((Integer) method.invoke(audit, 75.0d, null)).intValue());
		assertEquals(4, ((Integer) method.invoke(audit, 75.0d, "HS")).intValue());
		assertEquals(4, ((Integer) method.invoke(audit, 75.0d, "College")).intValue());
		assertEquals(4, ((Integer) method.invoke(audit, 75.0d, "Advanced")).intValue());
		assertEquals(3, ((Integer) method.invoke(audit, 75.0d, "Other")).intValue());

		// 60-69 band
		assertEquals(3, ((Integer) method.invoke(audit, 65.0d, null)).intValue());
		assertEquals(3, ((Integer) method.invoke(audit, 65.0d, "HS")).intValue());
		assertEquals(4, ((Integer) method.invoke(audit, 65.0d, "College")).intValue());
		assertEquals(4, ((Integer) method.invoke(audit, 65.0d, "Advanced")).intValue());
		assertEquals(2, ((Integer) method.invoke(audit, 65.0d, "Other")).intValue());

		// 50-59 band
		assertEquals(2, ((Integer) method.invoke(audit, 55.0d, null)).intValue());
		assertEquals(2, ((Integer) method.invoke(audit, 55.0d, "HS")).intValue());
		assertEquals(3, ((Integer) method.invoke(audit, 55.0d, "College")).intValue());
		assertEquals(4, ((Integer) method.invoke(audit, 55.0d, "Advanced")).intValue());
		assertEquals(1, ((Integer) method.invoke(audit, 55.0d, "Other")).intValue());

		// 30-49 band
		assertEquals(1, ((Integer) method.invoke(audit, 45.0d, null)).intValue());
		assertEquals(1, ((Integer) method.invoke(audit, 45.0d, "HS")).intValue());
		assertEquals(2, ((Integer) method.invoke(audit, 45.0d, "College")).intValue());
		assertEquals(3, ((Integer) method.invoke(audit, 45.0d, "Advanced")).intValue());
		assertEquals(0, ((Integer) method.invoke(audit, 45.0d, "Other")).intValue());

		// Below 30 band
		assertEquals(0, ((Integer) method.invoke(audit, 20.0d, null)).intValue());
		assertEquals(0, ((Integer) method.invoke(audit, 20.0d, "HS")).intValue());
		assertEquals(1, ((Integer) method.invoke(audit, 20.0d, "College")).intValue());
		assertEquals(2, ((Integer) method.invoke(audit, 20.0d, "Advanced")).intValue());
		assertEquals(0, ((Integer) method.invoke(audit, 20.0d, "Other")).intValue());
	}

	@Test
	public void countWordsHandlesVariousInputs() throws Exception {
		Method countWordsMethod = ReadabilityAudit.class.getDeclaredMethod("countWords", String.class);
		countWordsMethod.setAccessible(true);

		assertEquals(0, ((Integer) countWordsMethod.invoke(null, (String) null)).intValue());
		assertEquals(0, ((Integer) countWordsMethod.invoke(null, "")).intValue());
		assertEquals(0, ((Integer) countWordsMethod.invoke(null, "   ")).intValue());
		assertEquals(1, ((Integer) countWordsMethod.invoke(null, "hello")).intValue());
		assertEquals(5, ((Integer) countWordsMethod.invoke(null, "The quick brown fox jumps")).intValue());
		assertEquals(3, ((Integer) countWordsMethod.invoke(null, "  multiple   spaces   here  ")).intValue());
	}

	@Test
	public void xpathContainsHandlesNullInputs() throws Exception {
		Method xpathContainsMethod = ReadabilityAudit.class.getDeclaredMethod("xpathContains", String.class, String.class);
		xpathContainsMethod.setAccessible(true);

		assertFalse((Boolean) xpathContainsMethod.invoke(null, null, null));
		assertFalse((Boolean) xpathContainsMethod.invoke(null, null, "/html"));
		assertFalse((Boolean) xpathContainsMethod.invoke(null, "/html/body", null));
		assertTrue((Boolean) xpathContainsMethod.invoke(null, "/html/body/div", "/html/body"));
		assertFalse((Boolean) xpathContainsMethod.invoke(null, "/html/body", "/html/body/div"));
	}
}
