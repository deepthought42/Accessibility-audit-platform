package com.looksee.browsing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.looksee.pageBuilder.models.enums.Action;

import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

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
			builder.moveToElement(elem);
		}

		builder.build().perform();
	}
	
	/**
	 * 
	 * @param driver
	 * @param elem
	 * @param action
	 */
	public void execAction(WebElement elem, String input, String action, Point point) throws WebDriverException{
		if("click".equals(action)){
			builder.moveToElement(elem, point.getX(), point.getY()).click();
		}
		else if("clickAndHold".equals(action)){
			builder.moveToElement(elem, point.getX(), point.getY()).clickAndHold();
		}
		//Context click clicks select/options box
		else if("contextClick".equals(action)){
			builder.moveToElement(elem, point.getX(), point.getY()).contextClick(elem);
		}
		else if("doubleClick".equals(action)){
			builder.moveToElement(elem, point.getX(), point.getY()).doubleClick(elem);
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
		else if("release".equals(action)){
			builder.release(elem);
		}
		else if("sendKeys".equals(action)){
			//builder.sendKeys(elem, Keys.chord(Keys.CONTROL, Keys.ALT, Keys.DELETE));
			builder.sendKeys(elem, input);
		}
		else if("mouseover".equals(action)){
			builder.moveToElement(elem);
		}

		builder.build().perform();
	}
}
