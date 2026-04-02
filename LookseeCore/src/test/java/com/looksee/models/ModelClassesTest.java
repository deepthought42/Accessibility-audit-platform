package com.looksee.models;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.looksee.models.enums.AnimationType;

/**
 * Unit tests for model/entity classes.
 */
class ModelClassesTest {

    // ===== Account =====
    @Test
    void accountDefaultConstructor() {
        Account account = new Account();
        assertNull(account.getUserId());
        assertNull(account.getEmail());
    }

    @Test
    void accountParameterizedConstructor() {
        Account account = new Account("user1", "test@example.com", "cust_token", "sub_token");
        assertEquals("user1", account.getUserId());
        assertEquals("test@example.com", account.getEmail());
        assertEquals("cust_token", account.getCustomerToken());
        assertEquals("sub_token", account.getSubscriptionToken());
        assertNotNull(account.getOnboardedSteps());
        assertTrue(account.getOnboardedSteps().isEmpty());
        assertEquals("", account.getName());
    }

    @Test
    void accountSetOnboardedStepsNull() {
        Account account = new Account();
        account.setOnboardedSteps(null);
        assertNotNull(account.getOnboardedSteps());
        assertTrue(account.getOnboardedSteps().isEmpty());
    }

    @Test
    void accountAddOnboardingStep() {
        Account account = new Account("u", "e", "c", "s");
        account.addOnboardingStep("step1");
        account.addOnboardingStep("step1"); // duplicate
        assertEquals(1, account.getOnboardedSteps().size());
        account.addOnboardingStep("step2");
        assertEquals(2, account.getOnboardedSteps().size());
    }

    @Test
    void accountAddDomain() {
        Account account = new Account("u", "e", "c", "s");
        Domain domain = new Domain("https", "example.com", "/", "logo.png");
        account.addDomain(domain);
        assertEquals(1, account.getDomains().size());
    }

    @Test
    void accountGenerateKey() {
        Account account = new Account("u", "e", "c", "s");
        String key = account.generateKey();
        assertNotNull(key);
        assertFalse(key.isEmpty());
    }

    @Test
    void accountCreatedAtNotNull() {
        Account account = new Account("u", "e", "c", "s");
        assertNotNull(account.getCreatedAt());
    }

    // ===== Domain =====
    @Test
    void domainDefaultConstructor() {
        Domain domain = new Domain();
        assertNotNull(domain.getPages());
        assertNotNull(domain.getAuditRecords());
        assertNotNull(domain.getSitemap());
    }

    @Test
    void domainParameterizedConstructor() {
        Domain domain = new Domain("https", "example.com", "/path", "logo.png");
        assertEquals("example.com", domain.getUrl());
        assertEquals("example.com/path", domain.getEntrypointUrl());
        assertEquals("logo.png", domain.getLogoUrl());
        assertNotNull(domain.getKey());
    }

    @Test
    void domainGenerateKey() {
        Domain domain = new Domain();
        domain.setUrl("example.com");
        String key = domain.generateKey();
        assertTrue(key.startsWith("domain"));
    }

    @Test
    void domainEquals() {
        Domain d1 = new Domain("https", "example.com", "/", "");
        Domain d2 = new Domain("https", "example.com", "/path", "");
        assertTrue(d1.equals(d2)); // same URL
        Domain d3 = new Domain("https", "other.com", "/", "");
        assertFalse(d1.equals(d3));
        assertFalse(d1.equals("not a domain"));
    }

    // ===== ColorData =====
    @Test
    void colorDataFromRgbString() {
        ColorData color = new ColorData("rgb(255, 0, 0)");
        assertEquals(255, color.getRed());
        assertEquals(0, color.getGreen());
        assertEquals(0, color.getBlue());
    }

    @Test
    void colorDataFromRgbaString() {
        ColorData color = new ColorData("rgba(128, 64, 32, 0.5)");
        assertEquals(128, color.getRed());
        assertEquals(64, color.getGreen());
        assertEquals(32, color.getBlue());
        assertEquals(0.5, color.getTransparency(), 0.01);
    }

    @Test
    void colorDataFromHex() {
        ColorData color = new ColorData("#FF0000");
        assertEquals(255, color.getRed());
        assertEquals(0, color.getGreen());
        assertEquals(0, color.getBlue());
    }

    @Test
    void colorDataFromRgbValues() {
        ColorData color = new ColorData(100, 150, 200);
        assertEquals(100, color.getRed());
        assertEquals(150, color.getGreen());
        assertEquals(200, color.getBlue());
        assertEquals(1.0, color.getTransparency(), 0.01);
    }

    @Test
    void colorDataRgbString() {
        ColorData color = new ColorData(10, 20, 30);
        assertEquals("10,20,30", color.rgb());
    }

    @Test
    void colorDataToString() {
        ColorData color = new ColorData(10, 20, 30);
        assertEquals("10,20,30", color.toString());
    }

    @Test
    void colorDataEquals() {
        ColorData c1 = new ColorData(100, 100, 100);
        ColorData c2 = new ColorData(100, 100, 100);
        ColorData c3 = new ColorData(200, 100, 100);
        assertTrue(c1.equals(c2));
        assertFalse(c1.equals(c3));
        assertTrue(c1.equals(c1));
        assertFalse(c1.equals("not a color"));
    }

    @Test
    void colorDataClone() {
        ColorData original = new ColorData(50, 100, 150);
        ColorData clone = original.clone();
        assertEquals(original.getRed(), clone.getRed());
        assertEquals(original.getGreen(), clone.getGreen());
        assertEquals(original.getBlue(), clone.getBlue());
        assertNotSame(original, clone);
    }

    @Test
    void colorDataGenerateKey() {
        ColorData color = new ColorData(10, 20, 30);
        assertEquals("10,20,30", color.generateKey());
    }

    @Test
    void colorDataHsb() {
        ColorData color = new ColorData(255, 0, 0);
        String hsb = color.hsb();
        assertNotNull(hsb);
        assertTrue(hsb.contains(","));
    }

    @Test
    void colorDataGetHex() {
        ColorData color = new ColorData(255, 0, 128);
        String hex = color.getHex();
        assertTrue(hex.startsWith("#"));
        assertEquals("#ff0080", hex);
    }

    @Test
    void colorDataGetHexPadding() {
        ColorData color = new ColorData(0, 0, 0);
        assertEquals("#000000", color.getHex());
    }

    @Test
    void colorDataComputeContrast() {
        ColorData white = new ColorData(255, 255, 255);
        ColorData black = new ColorData(0, 0, 0);
        double contrast = ColorData.computeContrast(white, black);
        assertTrue(contrast > 20.0); // should be ~21:1
    }

    @Test
    void colorDataComputeContrastSameColor() {
        ColorData c = new ColorData(128, 128, 128);
        double contrast = ColorData.computeContrast(c, c);
        assertEquals(1.0, contrast, 0.01);
    }

    @Test
    void colorDataIsSimilarHue() {
        ColorData c1 = new ColorData(255, 0, 0);
        ColorData c2 = new ColorData(250, 0, 0);
        assertTrue(c1.isSimilarHue(c2));
    }

    @Test
    void colorDataYtoLstar() {
        assertEquals(0.0, ColorData.YtoLstar(0.0), 0.1);
        double lstar = ColorData.YtoLstar(0.5);
        assertTrue(lstar > 0);
    }

    @Test
    void colorDataRGBtoXYZ() {
        ColorData color = new ColorData(128, 128, 128);
        XYZColorSpace xyz = color.RGBtoXYZ();
        assertNotNull(xyz);
    }

    @Test
    void colorDataAlphaBlend() {
        ColorData fg = new ColorData("rgba(255, 0, 0, 0.5)");
        ColorData bg = new ColorData(0, 0, 255);
        fg.alphaBlend(bg);
        // After blending, red should be mixed with blue
        assertTrue(fg.getRed() > 0);
        assertTrue(fg.getBlue() > 0);
    }

    @Test
    void colorDataLuminosity() {
        ColorData white = new ColorData(255, 255, 255);
        ColorData black = new ColorData(0, 0, 0);
        assertTrue(white.getLuminosity() > black.getLuminosity());
    }

    @Test
    void colorDataFromUsageStat() {
        ColorUsageStat stat = new ColorUsageStat(100f, 150f, 200f, 0.5, 0.8f);
        ColorData color = new ColorData(stat);
        assertEquals(100, color.getRed());
        assertEquals(150, color.getGreen());
        assertEquals(200, color.getBlue());
        assertEquals(0.5, color.getUsagePercent(), 0.01);
    }

    // ===== CIEColorSpace =====
    @Test
    void cieColorSpaceConstructor() {
        CIEColorSpace cie = new CIEColorSpace(50.0, 10.0, -20.0);
        assertEquals(50.0, cie.l);
        assertEquals(10.0, cie.a);
        assertEquals(-20.0, cie.b);
    }

    // ===== ColorUsageStat =====
    @Test
    void colorUsageStatDefaultConstructor() {
        ColorUsageStat stat = new ColorUsageStat();
        assertEquals(0, stat.getRed());
    }

    @Test
    void colorUsageStatParameterizedConstructor() {
        ColorUsageStat stat = new ColorUsageStat(100f, 150f, 200f, 0.75, 0.9f);
        assertEquals(100f, stat.getRed());
        assertEquals(150f, stat.getGreen());
        assertEquals(200f, stat.getBlue());
        assertEquals(0.75, stat.getPixelPercent(), 0.01);
        assertEquals(0.9f, stat.getScore());
    }

    @Test
    void colorUsageStatGetRGB() {
        ColorUsageStat stat = new ColorUsageStat(10f, 20f, 30f, 0.5, 0.0f);
        assertEquals("10,20,30", stat.getRGB());
    }

    // ===== Screenshot =====
    @Test
    void screenshotDefaultConstructor() {
        Screenshot screenshot = new Screenshot();
        assertNull(screenshot.getBrowserName());
    }

    @Test
    void screenshotParameterizedConstructor() {
        Screenshot screenshot = new Screenshot("http://url.com/img.png", "chrome", "abc123", 1920, 1080);
        assertEquals("http://url.com/img.png", screenshot.getScreenshotUrl());
        assertEquals("chrome", screenshot.getBrowserName());
        assertEquals("abc123", screenshot.getChecksum());
        assertEquals(1920, screenshot.getWidth());
        assertEquals(1080, screenshot.getHeight());
        assertEquals("screenshotabc123", screenshot.getKey());
    }

    @Test
    void screenshotGenerateKey() {
        Screenshot screenshot = new Screenshot("url", "chrome", "checksum123", 100, 100);
        assertEquals("screenshotchecksum123", screenshot.generateKey());
    }

    // ===== HeaderNode =====
    @Test
    void headerNodeDefaultConstructor() {
        HeaderNode node = new HeaderNode();
        assertNull(node.getTag());
    }

    @Test
    void headerNodeTwoArgConstructor() {
        HeaderNode node = new HeaderNode("h1", "Title");
        assertEquals("h1", node.getTag());
        assertEquals("Title", node.getText());
        assertNotNull(node.getChildren());
        assertTrue(node.getChildren().isEmpty());
    }

    @Test
    void headerNodeAllArgsConstructor() {
        List<HeaderNode> children = new ArrayList<>();
        children.add(new HeaderNode("h2", "Subtitle"));
        HeaderNode node = new HeaderNode("h1", "Title", children);
        assertEquals(1, node.getChildren().size());
    }

    @Test
    void headerNodeAddChild() {
        HeaderNode parent = new HeaderNode("h1", "Title");
        HeaderNode child = new HeaderNode("h2", "Subtitle");
        parent.addChild(child);
        assertEquals(1, parent.getChildren().size());
        assertEquals("h2", parent.getChildren().get(0).getTag());
    }

    // ===== Group =====
    @Test
    void groupSingleArgConstructor() {
        Group group = new Group("testGroup");
        assertEquals("Testgroup", group.getName()); // first letter uppercase, rest lowercase
        assertEquals("", group.getDescription());
        assertNotNull(group.getKey());
    }

    @Test
    void groupTwoArgConstructor() {
        Group group = new Group("test", "A test group");
        assertEquals("Test", group.getName());
        assertEquals("A test group", group.getDescription());
    }

    @Test
    void groupGenerateKey() {
        Group group = new Group("TestGroup");
        assertEquals("group:testgroup", group.generateKey());
    }

    @Test
    void groupSetNameFormatsCorrectly() {
        Group group = new Group("TEST");
        assertEquals("Test", group.getName());
    }

    // ===== Animation =====
    @Test
    void animationDefaultConstructor() {
        Animation anim = new Animation();
        assertNull(anim.getImageUrls());
    }

    @Test
    void animationParameterizedConstructor() {
        List<String> urls = Arrays.asList("url1", "url2");
        List<String> checksums = Arrays.asList("cs1", "cs2");
        Animation anim = new Animation(urls, checksums, AnimationType.CAROUSEL);
        assertEquals(2, anim.getImageUrls().size());
        assertEquals(AnimationType.CAROUSEL, anim.getAnimationType());
        assertNotNull(anim.getKey());
        assertTrue(anim.getKey().startsWith("animation:"));
    }

    @Test
    void animationGenerateKeyDeterministic() {
        List<String> urls = Arrays.asList("url1", "url2");
        Animation a1 = new Animation(urls, Arrays.asList("cs1"), AnimationType.LOADING);
        Animation a2 = new Animation(new ArrayList<>(urls), Arrays.asList("cs1"), AnimationType.LOADING);
        assertEquals(a1.generateKey(), a2.generateKey());
    }

    // ===== LookseeObject (through Account) =====
    @Test
    void lookseeObjectKeyConstructor() {
        Account account = new Account();
        account.setKey("test-key");
        assertEquals("test-key", account.getKey());
        assertEquals("test-key", account.toString());
    }

    @Test
    void lookseeObjectCreatedAt() {
        Account account = new Account("u", "e", "c", "s");
        assertNotNull(account.getCreatedAt());
    }

    @Test
    void lookseeObjectIdDefaultNull() {
        Account account = new Account();
        assertNull(account.getId());
    }
}
