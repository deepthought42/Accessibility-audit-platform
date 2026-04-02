package utils;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.junit.Test;

import io.whelk.flesch.kincaid.ReadabilityCalculator;

public class ContentUtilsTest {
	
	@Test
	public void testExtractArticle() throws IOException {
		//marketing page url
		String url = "https://www.look-see.com";
		
		//CRUX library to extract article data
		//String all_page_text = Jsoup.connect(url).get().text();

        
		String content = 
			      "A rich man's wife became sick, and when she felt that her end was drawing near, " +
			      "she called her only daughter to her bedside and said, \"Dear child, remain pious " +
			      "and good, and then our dear God will always protect you, and I will look down on " +
			      "you from heaven and be near you.\" With this she closed her eyes and died. " +
			      "The girl went out to her mother's grave every day and wept, and she remained pious " +
			      "and good. When winter came the snow spread a white cloth over the grave, and when " +
			      "the spring sun had removed it again, the man took himself another wife. This wife " +
			      "brought two daughters into the house with her. They were beautiful, with fair faces, " +
			      "but evil and dark hearts. Times soon grew very bad for the poor stepchild.";

	      double result = ReadabilityCalculator.calculateReadingEase(content);
	      assertTrue( result == 80.13934306569344 );
	      System.out.println(result);  // 93.55913669064749

	      
	      content = 
	    	      "A rich man's wife became sick, and when she felt that her end was drawing near, " +
	    	      "she called her only daughter to her bedside and said, \"Dear child, remain pious " +
	    	      "and good, and then our dear God will always protect you, and I will look down on " +
	    	      "you from heaven and be near you.\" With this she closed her eyes and died. " +
	    	      "The girl went out to her mother's grave every day and wept, and she remained pious " +
	    	      "and good. When winter came the snow spread a white cloth over the grave, and when " +
	    	      "the spring sun had removed it again, the man took himself another wife. This wife " +
	    	      "brought two daughters into the house with her. They were beautiful, with fair faces, " +
	    	      "but evil and dark hearts. Times soon grew very bad for the poor stepchild.";

	    	      double result1 = ReadabilityCalculator.calculateGradeLevel(content);

	    	      System.out.println(result1);  // 5.142774922918807
	    	      assertTrue(result1 == 6.943587069864442);
		
		
	}
	
}
