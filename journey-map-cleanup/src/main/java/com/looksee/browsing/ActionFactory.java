package com.looksee.browsing;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.looksee.journeyMapCleanup.models.enums.Action;

/**
 * Constructs {@linkplain Actions} provided by Selenium
 *
 */
public class ActionFactory {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(ActionFactory.class);

	private Actions builder;
	
	public ActionFactory(WebDriver driver){
		builder = new Actions(driver);
	}

	/**
	 * 
	 * @param driver
	 * @param elem
	 * @param action
	 */
	public void execAction(WebElement elem, String input, Action action) throws WebDriverException{
		if(Action.CLICK.equals(action)){
			builder.click(elem);
		}
		else if(Action.CLICK_AND_HOLD.equals(action)){
			builder.clickAndHold(elem);
		}
		//Context click clicks select/options box
		else if(Action.CONTEXT_CLICK.equals(action)){
			builder.contextClick(elem);
		}
		else if(Action.DOUBLE_CLICK.equals(action)){
			builder.doubleClick(elem);
		}
		/*else if("dragAndDrop".equals(action)){
			//builder.dragAndDrop(source, target);
		}
		else if("keyDown".equals(action)){
			//builder.keyDown();
		}
		else if("keyUp".equals(action)){
			//builder.keyUp(theKey);
		}
		*/
		else if(Action.RELEASE.equals(action)){
			builder.release(elem);
		}
		else if(Action.SEND_KEYS.equals(action)){
			//builder.sendKeys(elem, Keys.chord(Keys.CONTROL, Keys.ALT, Keys.DELETE));
			builder.sendKeys(elem, input);
		}
		else if(Action.MOUSE_OVER.equals(action)){
			builder.scrollToElement(elem);
			builder.moveToElement(elem);
		}

		builder.build().perform();
	}
}
