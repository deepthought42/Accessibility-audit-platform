package com.looksee.contentAudit.models;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.looksee.models.audit.Score;

public class ParagraphingAuditUnitTest {

	@Test
	public void calculateParagraphScoreRewardsFiveOrFewerSentences() {
		assertEquals(1, ParagraphingAudit.calculateParagraphScore(1).getPointsAchieved());
		assertEquals(1, ParagraphingAudit.calculateParagraphScore(5).getPointsAchieved());
	}

	@Test
	public void calculateParagraphScorePenalizesMoreThanFiveSentences() {
		assertEquals(0, ParagraphingAudit.calculateParagraphScore(6).getPointsAchieved());
		assertEquals(0, ParagraphingAudit.calculateParagraphScore(10).getPointsAchieved());
	}

	@Test
	public void calculateParagraphScoreReturnsOneForZeroSentences() {
		Score score = ParagraphingAudit.calculateParagraphScore(0);
		assertEquals(1, score.getPointsAchieved());
		assertEquals(1, score.getMaxPossiblePoints());
	}

	@Test
	public void calculateParagraphScoreReturnsZeroForManySentences() {
		Score score = ParagraphingAudit.calculateParagraphScore(100);
		assertEquals(0, score.getPointsAchieved());
		assertEquals(1, score.getMaxPossiblePoints());
	}

	@Test
	public void calculateParagraphScoreBoundaryAtFive() {
		Score atFive = ParagraphingAudit.calculateParagraphScore(5);
		assertEquals(1, atFive.getPointsAchieved());
		assertEquals(1, atFive.getMaxPossiblePoints());

		Score atSix = ParagraphingAudit.calculateParagraphScore(6);
		assertEquals(0, atSix.getPointsAchieved());
		assertEquals(1, atSix.getMaxPossiblePoints());
	}

	@Test
	public void calculateParagraphScoreMaxPointsAlwaysOne() {
		assertEquals(1, ParagraphingAudit.calculateParagraphScore(0).getMaxPossiblePoints());
		assertEquals(1, ParagraphingAudit.calculateParagraphScore(3).getMaxPossiblePoints());
		assertEquals(1, ParagraphingAudit.calculateParagraphScore(5).getMaxPossiblePoints());
		assertEquals(1, ParagraphingAudit.calculateParagraphScore(6).getMaxPossiblePoints());
		assertEquals(1, ParagraphingAudit.calculateParagraphScore(50).getMaxPossiblePoints());
	}
}
