package audits;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.looksee.models.audit.GenericIssue;
import com.looksee.utils.FormValidator;

public class FormValidatorTest {

    private FormValidator validator;

    @BeforeEach
    public void setUp() {
        validator = new FormValidator();
    }

    @Test
    public void testValidateFormStructure_ValidForm() {
        String html = "<form>" +
                "<fieldset><legend>Personal Information</legend>" +
                "<input type='text' name='name'>" +
                "<input type='email' name='email'>" +
                "</fieldset>" +
                "<fieldset><legend>Account Details</legend>" +
                "<input type='password' name='password'>" +
                "<input type='password' name='confirm_password'>" +
                "</fieldset>" +
                "</form>";
        Document doc = Jsoup.parse(html);
        Element form = doc.select("form").first();

        Set<GenericIssue> issues = validator.validateFormStructure(form);
        int sum = issues.stream().map(issue -> issue.getRecommendation().isEmpty() ? 0 : 1).mapToInt(Integer::intValue).sum();
        assertTrue(sum==0);
    }

    @Test
    public void testValidateFormStructure_NoFieldset() {
        String html = "<form>" +
                "<input type='text' name='name'>" +
                "<input type='email' name='email'>" +
                "<input type='password' name='password'>" +
                "<input type='password' name='confirm_password'>" +
                "</form>";
        Document doc = Jsoup.parse(html);
        Element form = doc.select("form").first();

        Set<GenericIssue> issues = validator.validateFormStructure(form);
        int sum = issues.stream().map(issue -> issue.getRecommendation().isEmpty() ? 0 : 1).mapToInt(Integer::intValue).sum();

        assertFalse(sum==0);
    }

    @Test
    public void testValidateFormStructure_PartialFieldset() {
        String html = "<form>" +
                "<fieldset><legend>Personal Information</legend>" +
                "<input type='text' name='name'>" +
                "<input type='email' name='email'>" +
                "</fieldset>" +
                "<input type='password' name='password'>" +
                "<input type='password' name='confirm_password'>" +
                "</form>";
        Document doc = Jsoup.parse(html);
        Element form = doc.select("form").first();

        Set<GenericIssue> issues =  validator.validateFormStructure(form);
        int sum = issues.stream().map(issue -> issue.getRecommendation().isEmpty() ? 0 : 1).mapToInt(Integer::intValue).sum();

        assertFalse(sum==0);
    }

    @Test
    public void testValidateFormStructure_NestedFieldset() {
        String html = "<form>" +
                "<fieldset><legend>Personal Information</legend>" +
                "<fieldset><legend>Name</legend>" +
                "<input type='text' name='first_name'>" +
                "<input type='text' name='last_name'>" +
                "</fieldset>" +
                "<input type='email' name='email'>" +
                "</fieldset>" +
                "</form>";
        Document doc = Jsoup.parse(html);
        Element form = doc.select("form").first();

        Set<GenericIssue> issues = validator.validateFormStructure(form);
        int sum = issues.stream().map(issue -> issue.getRecommendation().isEmpty() ? 0 : 1).mapToInt(Integer::intValue).sum();

        assertTrue(sum==0);
    }

    @Test
    public void testValidateFormStructure_SingleFieldset() {
        String html = "<form>" +
                "<fieldset><legend>Personal Information</legend>" +
                "<input type='text' name='name'>" +
                "<input type='email' name='email'>" +
                "<input type='password' name='password'>" +
                "</fieldset>" +
                "</form>";
        Document doc = Jsoup.parse(html);
        Element form = doc.select("form").first();

        Set<GenericIssue> issues =  validator.validateFormStructure(form);
        int sum = issues.stream().map(issue -> issue.getRecommendation().isEmpty() ? 0 : 1).mapToInt(Integer::intValue).sum();

        assertTrue(sum==0);
    }

    @Test
    public void testValidateFormStructure_MissingFieldset() {
        String html = "<form>" +
                "<fieldset><legend>Personal Information</legend>" +
                "<input type='text' name='name'>" +
                "<input type='email' name='email'>" +
                "</fieldset>" +
                "<input type='password' name='password'>" +
                "</form>";
        Document doc = Jsoup.parse(html);
        Element form = doc.select("form").first();

        Set<GenericIssue> issues = validator.validateFormStructure(form);
        int sum = issues.stream().map(issue -> issue.getRecommendation().isEmpty() ? 0 : 1).mapToInt(Integer::intValue).sum();

        assertFalse(sum==0);
    }
}
