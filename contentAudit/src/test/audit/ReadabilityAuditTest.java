package audit;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import io.whelk.flesch.kincaid.ReadabilityCalculator;

public class ReadabilityAuditTest {
	
	@Test
	public void easeOfReadingTest() {
		String sentence_1 = "By using our website you consent to the use of cookies.";
		
		double ease_of_reading_score = ReadabilityCalculator.calculateReadingEase(sentence_1);
		
		assertTrue(ease_of_reading_score == 64.9245454545455);
		
		//comes out at difficult to read
		String sentence_2 = "Our clients are corporations, startups, not-for-profits and positive impact organizations. Our team has worked with hundreds of clients on thousands of high-impact initiatives.";
		ease_of_reading_score = ReadabilityCalculator.calculateReadingEase(sentence_2);
		
		assertTrue(ease_of_reading_score == 30.947692307692336);

		
		String sentence_3 = "Being agile is about more than just moving fast. It’s about setting the right foundation for things to come and being responsive in the face of new learnings and market changes. We believe in properly planning your initiative, then sprinting through discrete agile cycles for optimal time to value.";
		ease_of_reading_score = ReadabilityCalculator.calculateReadingEase(sentence_3);
		
		assertTrue(ease_of_reading_score == 62.493401360544254);

		
		sentence_3 = "Being agile is about more than just moving fast. It’s about setting the right foundation for things to come and being responsive in the face of new learnings and market changes. We believe in properly planning your initiative, then sprinting through discrete agile cycles for optimal time to value. This sentence is here to help test if making a sentence longer improves it's score.";
		ease_of_reading_score = ReadabilityCalculator.calculateReadingEase(sentence_3);
		
		assertTrue(ease_of_reading_score == 66.69509615384617);

	}
}
