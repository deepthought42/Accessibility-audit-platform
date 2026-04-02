package audit;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.looksee.models.audit.ParagraphingAudit;

public class ParagraphAuditTest {
	
	@Test
	public void paragraphScoreTest() {
		assertTrue(ParagraphingAudit.calculateParagraphScore(1).getPointsAchieved() == 1);
		
		assertTrue(ParagraphingAudit.calculateParagraphScore(3).getPointsAchieved() == 1);

		assertTrue(ParagraphingAudit.calculateParagraphScore(4).getPointsAchieved() == 1);

		assertTrue(ParagraphingAudit.calculateParagraphScore(5).getPointsAchieved() == 1);

		assertTrue(ParagraphingAudit.calculateParagraphScore(6).getPointsAchieved() == 0);
	}
}
