package audit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.looksee.api.EntryPoint;
import com.looksee.models.audit.aesthetics.MarginAudit;

public class MarginAuditTests {

	@Test
	public void evaluateSpacingMultipleOf8Test() {
		String divisible_by_8 = "8px";
		
		Assert.assertTrue(MarginAudit.isMultipleOf8(divisible_by_8));
		
		divisible_by_8 = "16px";
		
		Assert.assertTrue(MarginAudit.isMultipleOf8(divisible_by_8));
		
		divisible_by_8 = "4px";
		
		Assert.assertTrue(MarginAudit.isMultipleOf8(divisible_by_8));
		
		divisible_by_8 = "6px";
		
		Assert.assertFalse(MarginAudit.isMultipleOf8(divisible_by_8));
		
		divisible_by_8 = "16.4px";
		
		Assert.assertTrue(MarginAudit.isMultipleOf8(divisible_by_8));
	}
}
