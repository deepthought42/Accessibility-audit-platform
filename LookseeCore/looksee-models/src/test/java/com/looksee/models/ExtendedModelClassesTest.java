package com.looksee.models;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.looksee.models.enums.*;

/**
 * Unit tests for additional model/entity classes.
 */
class ExtendedModelClassesTest {

    // ===== XYZColorSpace =====
    @Test
    void xyzColorSpaceDefaultConstructor() {
        XYZColorSpace xyz = new XYZColorSpace();
        assertEquals(0.0, xyz.getX());
        assertEquals(903.3, xyz.getK());
    }

    @Test
    void xyzColorSpaceParameterizedConstructor() {
        XYZColorSpace xyz = new XYZColorSpace(0.5, 0.6, 0.7);
        assertEquals(0.5, xyz.getX());
        assertEquals(0.6, xyz.getY());
        assertEquals(0.7, xyz.getZ());
    }

    @Test
    void xyzColorSpaceToCIE() {
        XYZColorSpace xyz = new XYZColorSpace(0.5, 0.6, 0.7);
        CIEColorSpace cie = xyz.XYZtoCIE();
        assertNotNull(cie);
    }

    // ===== SimpleElement =====
    @Test
    void simpleElementDefaultConstructor() {
        SimpleElement elem = new SimpleElement();
        assertNull(elem.getKey());
    }

    @Test
    void simpleElementParameterizedConstructor() {
        SimpleElement elem = new SimpleElement("key1", "http://img.png", 10, 20, 100, 200, ".cls", "text", false, false);
        assertEquals("key1", elem.getKey());
        assertEquals("http://img.png", elem.getScreenshotUrl());
        assertEquals(10, elem.getXLocation());
        assertEquals(20, elem.getYLocation());
        assertEquals(100, elem.getWidth());
        assertEquals(200, elem.getHeight());
        assertEquals(".cls", elem.getCssSelector());
        assertEquals("text", elem.getText());
        assertFalse(elem.isImageFlagged());
        assertFalse(elem.isAdultContent());
    }

    // ===== SimplePage =====
    @Test
    void simplePageDefaultConstructor() {
        SimplePage page = new SimplePage();
        assertNull(page.getUrl());
    }

    @Test
    void simplePageParameterizedConstructor() {
        SimplePage page = new SimplePage("https://example.com", "ss.png", "fp.png", 1920, 3000, "<html></html>", "key1", 42L);
        assertEquals("https://example.com", page.getUrl());
        assertEquals("ss.png", page.getScreenshotUrl());
        assertEquals("fp.png", page.getFullPageScreenshotUrl());
        assertEquals(1920, page.getWidth());
        assertEquals(3000, page.getHeight());
        assertEquals("<html></html>", page.getHtmlSource());
        assertEquals("key1", page.getKey());
        assertEquals(42L, page.getId());
    }

    // ===== Label =====
    @Test
    void labelDefaultConstructor() {
        Label label = new Label();
        assertEquals("", label.getDescription());
        assertEquals(0.0F, label.getScore());
    }

    @Test
    void labelParameterizedConstructor() {
        Label label = new Label("test label", 0.95f);
        assertEquals("test label", label.getDescription());
        assertEquals(0.95f, label.getScore());
        assertEquals("label::test label", label.getKey());
    }

    @Test
    void labelGenerateKey() {
        Label label = new Label("desc", 0.5f);
        assertEquals("label::desc", label.generateKey());
    }

    // ===== LatLng =====
    @Test
    void latLngDefaultConstructor() {
        LatLng latLng = new LatLng();
        assertEquals(0.0, latLng.getLatitude());
        assertEquals(0.0, latLng.getLongitude());
    }

    @Test
    void latLngParameterizedConstructor() {
        LatLng latLng = new LatLng(40.7128, -74.0060);
        assertEquals(40.7128, latLng.getLatitude());
        assertEquals(-74.0060, latLng.getLongitude());
        assertNotNull(latLng.getKey());
    }

    @Test
    void latLngGenerateKey() {
        LatLng latLng = new LatLng(40.7128, -74.0060);
        assertTrue(latLng.generateKey().startsWith("latlng::"));
    }

    // ===== PaletteColor =====
    @Test
    void paletteColorDefaultConstructor() {
        PaletteColor pc = new PaletteColor();
        assertNull(pc.getPrimaryColor());
    }

    @Test
    void paletteColorParameterizedConstructor() {
        Map<String, String> tst = new HashMap<>();
        tst.put("shade1", "#333333");
        PaletteColor pc = new PaletteColor("#FF0000", 25.0, tst);
        assertEquals("#FF0000", pc.getPrimaryColor());
        assertEquals(25.0, pc.getPrimaryColorPercent());
        assertEquals(1, pc.getTintsShadesTones().size());
    }

    @Test
    void paletteColorAddTintsShadesTones() {
        PaletteColor pc = new PaletteColor();
        Map<String, String> tst = new HashMap<>();
        tst.put("tint1", "#FFCCCC");
        pc.addTintsShadesTones(tst);
        assertEquals(1, pc.getTintsShadesTones().size());
    }

    // ===== TestUser =====
    @Test
    void testUserDefaultConstructor() {
        TestUser user = new TestUser();
        assertNull(user.getUsername());
    }

    @Test
    void testUserParameterizedConstructor() {
        TestUser user = new TestUser("admin", "password123");
        assertEquals("admin", user.getUsername());
        assertEquals("password123", user.getPassword());
        assertEquals("useradminpassword123", user.getKey());
    }

    @Test
    void testUserGenerateKey() {
        TestUser user = new TestUser("user", "pass");
        assertEquals("useruserpass", user.generateKey());
    }

    // ===== AccountUsage =====
    @Test
    void accountUsageDefaultConstructor() {
        AccountUsage usage = new AccountUsage();
        assertEquals(0, usage.getDiscoveryLimit());
    }

    @Test
    void accountUsageParameterizedConstructor() {
        AccountUsage usage = new AccountUsage(100, 50, 200, 75);
        assertEquals(100, usage.getDiscoveryLimit());
        assertEquals(50, usage.getDiscoveriesUsed());
        assertEquals(200, usage.getTestLimit());
        assertEquals(75, usage.getTestsUsed());
    }

    // ===== BrowserPassingStatuses =====
    @Test
    void browserPassingStatusesSetAndGet() {
        BrowserPassingStatuses bps = new BrowserPassingStatuses();
        Map<String, Boolean> statuses = new HashMap<>();
        statuses.put("chrome", true);
        statuses.put("firefox", false);
        bps.setBrowserPassingStatuses(statuses);
        assertEquals(2, bps.getBrowserPassingStatuses().size());
        assertTrue(bps.getBrowserPassingStatuses().get("chrome"));
    }

    // ===== ElementTemplates =====
    @Test
    void elementTemplatesDefaultConstructor() {
        ElementTemplates et = new ElementTemplates();
        assertNull(et.getElements());
        assertNull(et.getTemplates());
    }

    @Test
    void elementTemplatesSetters() {
        ElementTemplates et = new ElementTemplates();
        et.setElements(new ArrayList<>());
        et.setTemplates(new ArrayList<>());
        assertNotNull(et.getElements());
        assertNotNull(et.getTemplates());
    }

    // ===== ImageSafeSearchAnnotation =====
    @Test
    void imageSafeSearchAnnotationDefaultConstructor() {
        ImageSafeSearchAnnotation ann = new ImageSafeSearchAnnotation();
        assertEquals("", ann.getAdult());
        assertEquals("", ann.getMedical());
        assertEquals("", ann.getSpoof());
        assertEquals("", ann.getViolence());
        assertEquals("", ann.getRacy());
    }

    @Test
    void imageSafeSearchAnnotationParameterizedConstructor() {
        ImageSafeSearchAnnotation ann = new ImageSafeSearchAnnotation("UNLIKELY", "VERY_UNLIKELY", "POSSIBLE", "UNLIKELY", "LIKELY");
        assertEquals("UNLIKELY", ann.getSpoof());
        assertEquals("VERY_UNLIKELY", ann.getMedical());
        assertEquals("POSSIBLE", ann.getAdult());
        assertEquals("UNLIKELY", ann.getViolence());
        assertEquals("LIKELY", ann.getRacy());
    }

    @Test
    void imageSafeSearchAnnotationGenerateKey() {
        ImageSafeSearchAnnotation ann = new ImageSafeSearchAnnotation();
        String key = ann.generateKey();
        assertTrue(key.startsWith("imagesearchannotation::"));
    }

    // ===== ImageSearchAnnotation =====
    @Test
    void imageSearchAnnotationDefaultConstructor() {
        ImageSearchAnnotation ann = new ImageSearchAnnotation();
        assertEquals(0.0F, ann.getScore());
        assertNotNull(ann.getBestGuessLabel());
        assertNotNull(ann.getFullMatchingImages());
        assertNotNull(ann.getSimilarImages());
    }

    @Test
    void imageSearchAnnotationParameterizedConstructor() {
        Set<String> labels = new HashSet<>(Arrays.asList("cat"));
        Set<String> matching = new HashSet<>(Arrays.asList("http://img1.com"));
        Set<String> similar = new HashSet<>(Arrays.asList("http://img2.com"));
        ImageSearchAnnotation ann = new ImageSearchAnnotation(labels, matching, similar);
        assertEquals(1, ann.getBestGuessLabel().size());
        assertEquals(1, ann.getFullMatchingImages().size());
    }

    @Test
    void imageSearchAnnotationGenerateKey() {
        ImageSearchAnnotation ann = new ImageSearchAnnotation();
        assertTrue(ann.generateKey().startsWith("imagesearchannotation::"));
    }

    // ===== ImageFaceAnnotation =====
    @Test
    void imageFaceAnnotationDefaultConstructor() {
        ImageFaceAnnotation ann = new ImageFaceAnnotation();
        assertEquals("", ann.getLocale());
        assertEquals(0.0F, ann.getScore());
    }

    // ===== ImageLandmarkInfo =====
    @Test
    void imageLandmarkInfoDefaultConstructor() {
        ImageLandmarkInfo info = new ImageLandmarkInfo();
        assertEquals("", info.getDescription());
        assertEquals(0.0, info.getScore());
        assertNotNull(info.getLatLngSet());
    }

    @Test
    void imageLandmarkInfoParameterizedConstructor() {
        Set<LatLng> latlngs = new HashSet<>();
        latlngs.add(new LatLng(40.0, -74.0));
        ImageLandmarkInfo info = new ImageLandmarkInfo(latlngs, "Statue of Liberty", 0.99);
        assertEquals("Statue of Liberty", info.getDescription());
        assertEquals(0.99, info.getScore());
        assertEquals(1, info.getLatLngSet().size());
    }

    @Test
    void imageLandmarkInfoGenerateKey() {
        ImageLandmarkInfo info = new ImageLandmarkInfo();
        assertTrue(info.generateKey().startsWith("landmarkinfo::"));
    }

    // ===== Template =====
    @Test
    void templateDefaultConstructor() {
        Template template = new Template();
        assertEquals("", template.getTemplate());
        assertNotNull(template.getElements());
        assertNotNull(template.getKey());
    }

    @Test
    void templateParameterizedConstructor() {
        Template template = new Template(TemplateType.ATOM, "<div>{{content}}</div>");
        assertEquals("<div>{{content}}</div>", template.getTemplate());
        assertNotNull(template.getKey());
    }

    // ===== DiscoveryRecord =====
    @Test
    void discoveryRecordDefaultConstructor() {
        DiscoveryRecord record = new DiscoveryRecord();
        assertNull(record.getBrowserName());
    }

    @Test
    void discoveryRecordParameterizedConstructor() {
        DiscoveryRecord record = new DiscoveryRecord(
            new Date(), "chrome", "https://example.com",
            5, 10, 3, ExecutionStatus.RUNNING
        );
        assertEquals("chrome", record.getBrowserName());
        assertEquals("https://example.com", record.getDomainUrl());
        assertEquals(ExecutionStatus.RUNNING, record.getStatus());
        assertEquals(5, record.getTestCount());
    }

    // ===== Logo =====
    @Test
    void logoDefaultConstructor() {
        Logo logo = new Logo();
        assertEquals("", logo.getDescription());
        assertEquals("", logo.getLocale());
        assertEquals(0.0F, logo.getScore());
    }
}
