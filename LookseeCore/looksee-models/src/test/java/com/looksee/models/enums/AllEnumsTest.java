package com.looksee.models.enums;

import static org.junit.jupiter.api.Assertions.*;

import com.looksee.browsing.enums.BrowserEnvironment;
import com.looksee.browsing.enums.BrowserType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive tests for all enum classes and their create/toString/values methods.
 */
class AllEnumsTest {

    // ===== Action =====
    @Test
    void actionCreateValid() {
        assertEquals(Action.CLICK, Action.create("click"));
        assertEquals(Action.DOUBLE_CLICK, Action.create("doubleClick"));
        assertEquals(Action.HOVER, Action.create("hover"));
        assertEquals(Action.CLICK_AND_HOLD, Action.create("clickAndHold"));
        assertEquals(Action.CONTEXT_CLICK, Action.create("contextClick"));
        assertEquals(Action.RELEASE, Action.create("release"));
        assertEquals(Action.SEND_KEYS, Action.create("sendKeys"));
        assertEquals(Action.MOUSE_OVER, Action.create("mouseover"));
        assertEquals(Action.UNKNOWN, Action.create("unknown"));
    }

    @Test
    void actionCreateCaseInsensitive() {
        assertEquals(Action.CLICK, Action.create("CLICK"));
        assertEquals(Action.CLICK, Action.create("Click"));
    }

    @Test
    void actionCreateNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> Action.create(null));
    }

    @Test
    void actionCreateInvalidThrows() {
        assertThrows(IllegalArgumentException.class, () -> Action.create("invalid_xyz"));
    }

    @Test
    void actionToString() {
        assertEquals("click", Action.CLICK.toString());
        assertEquals("unknown", Action.UNKNOWN.toString());
    }

    @Test
    void actionValues() {
        assertEquals(9, Action.values().length);
    }

    // ===== AlertChoice =====
    @Test
    void alertChoiceCreateValid() {
        assertEquals(AlertChoice.DISMISS, AlertChoice.create("dismiss"));
        assertEquals(AlertChoice.ACCEPT, AlertChoice.create("accept"));
    }

    @Test
    void alertChoiceCreateNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> AlertChoice.create(null));
    }

    @Test
    void alertChoiceCreateInvalidThrows() {
        assertThrows(IllegalArgumentException.class, () -> AlertChoice.create("invalid"));
    }

    @Test
    void alertChoiceToString() {
        assertEquals("dismiss", AlertChoice.DISMISS.toString());
        assertEquals("accept", AlertChoice.ACCEPT.toString());
    }

    // ===== AnimationType =====
    @Test
    void animationTypeCreateValid() {
        assertEquals(AnimationType.CAROUSEL, AnimationType.create("CAROUSEL"));
        assertEquals(AnimationType.LOADING, AnimationType.create("LOADING"));
        assertEquals(AnimationType.CONTINUOUS, AnimationType.create("CONTINUOUS"));
    }

    @Test
    void animationTypeCreateCaseInsensitive() {
        assertEquals(AnimationType.CAROUSEL, AnimationType.create("carousel"));
    }

    @Test
    void animationTypeCreateNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> AnimationType.create(null));
    }

    @Test
    void animationTypeCreateInvalidThrows() {
        assertThrows(IllegalArgumentException.class, () -> AnimationType.create("invalid"));
    }

    // ===== AuditCategory =====
    @Test
    void auditCategoryCreateValid() {
        assertEquals(AuditCategory.CONTENT, AuditCategory.create("CONTENT"));
        assertEquals(AuditCategory.INFORMATION_ARCHITECTURE, AuditCategory.create("INFORMATION_ARCHITECTURE"));
        assertEquals(AuditCategory.AESTHETICS, AuditCategory.create("AESTHETICS"));
        assertEquals(AuditCategory.ACCESSIBILITY, AuditCategory.create("ACCESSIBILITY"));
    }

    @Test
    void auditCategoryValues() {
        assertEquals(4, AuditCategory.values().length);
    }

    // ===== AuditLevel =====
    @Test
    void auditLevelCreateValid() {
        assertEquals(AuditLevel.PAGE, AuditLevel.create("PAGE"));
        assertEquals(AuditLevel.DOMAIN, AuditLevel.create("DOMAIN"));
        assertEquals(AuditLevel.UNKNOWN, AuditLevel.create("UNKNOWN"));
    }

    @Test
    void auditLevelCreateNullReturnsUnknown() {
        assertEquals(AuditLevel.UNKNOWN, AuditLevel.create(null));
    }

    // ===== AuditName =====
    @Test
    void auditNameCreateValid() {
        assertEquals(AuditName.COLOR_PALETTE, AuditName.create("Color_Palette"));
        assertEquals(AuditName.TEXT_BACKGROUND_CONTRAST, AuditName.create("Text_Background_Contrast"));
        assertEquals(AuditName.ALT_TEXT, AuditName.create("Alt_Text"));
        assertEquals(AuditName.UNKNOWN, AuditName.create("Unknown"));
        assertEquals(AuditName.ENCRYPTED, AuditName.create("ENCRYPTED"));
    }

    @Test
    void auditNameCreateCaseInsensitive() {
        assertEquals(AuditName.LINKS, AuditName.create("links"));
        assertEquals(AuditName.FONT, AuditName.create("font"));
    }

    // ===== AuditPhase =====
    @Test
    void auditPhaseCreateValid() {
        assertEquals(AuditPhase.PAGE_DATA_EXTRACTION, AuditPhase.create("PAGE_DATA_EXTRACTION"));
        assertEquals(AuditPhase.START_AUDIT, AuditPhase.create("INIT_AUDIT"));
        assertEquals(AuditPhase.AUDIT_COMPLETE, AuditPhase.create("AUDIT_COMPLETE"));
        assertEquals(AuditPhase.UNKNOWN, AuditPhase.create("UNKNOWN"));
    }

    @Test
    void auditPhaseCreateNullReturnsUnknown() {
        assertEquals(AuditPhase.UNKNOWN, AuditPhase.create(null));
    }

    // ===== AuditStage =====
    @Test
    void auditStageCreateValid() {
        assertEquals(AuditStage.PRERENDER, AuditStage.create("prerender"));
        assertEquals(AuditStage.RENDERED, AuditStage.create("rendered"));
        assertEquals(AuditStage.UNKNOWN, AuditStage.create("unknown"));
    }

    @Test
    void auditStageCreateNullReturnsUnknown() {
        assertEquals(AuditStage.UNKNOWN, AuditStage.create(null));
    }

    // ===== AuditStatus =====
    @Test
    void auditStatusCreateValid() {
        assertEquals(AuditStatus.STARTED, AuditStatus.create("STARTED"));
        assertEquals(AuditStatus.STOPPED, AuditStatus.create("STOPPED"));
        assertEquals(AuditStatus.COMPLETE, AuditStatus.create("COMPLETE"));
    }

    @Test
    void auditStatusCreateCaseInsensitive() {
        assertEquals(AuditStatus.STARTED, AuditStatus.create("started"));
    }

    // ===== AuditSubcategory =====
    @Test
    void auditSubcategoryCreateValid() {
        assertEquals(AuditSubcategory.WRITTEN_CONTENT, AuditSubcategory.create("Written_Content"));
        assertEquals(AuditSubcategory.IMAGERY, AuditSubcategory.create("Imagery"));
        assertEquals(AuditSubcategory.TYPOGRAPHY, AuditSubcategory.create("Typography"));
        assertEquals(AuditSubcategory.COLOR_MANAGEMENT, AuditSubcategory.create("Color_Management"));
        assertEquals(AuditSubcategory.SECURITY, AuditSubcategory.create("Security"));
        assertEquals(AuditSubcategory.NAVIGATION, AuditSubcategory.create("Navigation"));
    }

    @Test
    void auditSubcategoryValues() {
        assertEquals(17, AuditSubcategory.values().length);
    }

    // ===== AuditType =====
    @Test
    void auditTypeCreateValid() {
        assertEquals(AuditType.TABLE, AuditType.create("table"));
        assertEquals(AuditType.FILMSTRIP, AuditType.create("filmstrip"));
        assertEquals(AuditType.OPPORTUNITY, AuditType.create("opportunity"));
        assertEquals(AuditType.NODE, AuditType.create("node"));
        assertEquals(AuditType.DEBUG_DATA, AuditType.create("debugdata"));
        assertEquals(AuditType.UNKNOWN, AuditType.create("unknown"));
    }

    @Test
    void auditTypeCreateNullReturnsUnknown() {
        assertEquals(AuditType.UNKNOWN, AuditType.create(null));
    }

    // ===== AudienceProficiency =====
    @Test
    void audienceProficiencyCreateValid() {
        assertEquals(AudienceProficiency.GENERAL, AudienceProficiency.create("GENERAL"));
        assertEquals(AudienceProficiency.KNOWLEDGEABLE, AudienceProficiency.create("KNOWLEDGEABLE"));
        assertEquals(AudienceProficiency.EXPERT, AudienceProficiency.create("EXPERT"));
        assertEquals(AudienceProficiency.UNKNOWN, AudienceProficiency.create("UNKNOWN"));
    }

    @Test
    void audienceProficiencyCreateNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> AudienceProficiency.create(null));
    }

    // ===== BrowserEnvironment =====
    @Test
    void browserEnvironmentCreateValid() {
        assertEquals(BrowserEnvironment.TEST, BrowserEnvironment.create("test"));
        assertEquals(BrowserEnvironment.DISCOVERY, BrowserEnvironment.create("discovery"));
    }

    @Test
    void browserEnvironmentCreateNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> BrowserEnvironment.create(null));
    }

    // ===== BrowserType =====
    @Test
    void browserTypeCreateValid() {
        assertEquals(BrowserType.CHROME, BrowserType.create("chrome"));
        assertEquals(BrowserType.FIREFOX, BrowserType.create("firefox"));
        assertEquals(BrowserType.SAFARI, BrowserType.create("safari"));
        assertEquals(BrowserType.IE, BrowserType.create("ie"));
    }

    @Test
    void browserTypeCreateNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> BrowserType.create(null));
    }

    // ===== BugType =====
    @Test
    void bugTypeCreateValid() {
        assertEquals(BugType.MISSING_FIELD, BugType.create("MISSING FIELD"));
        assertEquals(BugType.ACCESSIBILITY, BugType.create("ACCESSIBILITY"));
        assertEquals(BugType.PERFORMANCE, BugType.create("PERFORMANCE"));
        assertEquals(BugType.SEO, BugType.create("SEO"));
        assertEquals(BugType.BEST_PRACTICES, BugType.create("BEST_PRACTICES"));
    }

    @Test
    void bugTypeCreateNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> BugType.create(null));
    }

    // ===== CaptchaResult =====
    @Test
    void captchaResultCreateValid() {
        assertEquals(CaptchaResult.CAPTCHA_BLOCKING, CaptchaResult.create("CAPTCHA_BLOCKING"));
        assertEquals(CaptchaResult.CAPTCHA_MATCHED, CaptchaResult.create("CAPTCHA_MATCHED"));
        assertEquals(CaptchaResult.CAPTCHA_NEEDED, CaptchaResult.create("CAPTCHA_NEEDED"));
        assertEquals(CaptchaResult.CAPTCHA_NOT_NEEDED, CaptchaResult.create("CAPTCHA_NOT_NEEDED"));
        assertEquals(CaptchaResult.CAPTCHA_UNMATCHED, CaptchaResult.create("CAPTCHA_UNMATCHED"));
        assertEquals(CaptchaResult.CAPTCHA_UNSET, CaptchaResult.create("CAPTCHA_UNSET"));
    }

    @Test
    void captchaResultCreateNullReturnsUnset() {
        assertEquals(CaptchaResult.CAPTCHA_UNSET, CaptchaResult.create(null));
    }

    // ===== ColorScheme =====
    @Test
    void colorSchemeCreateValid() {
        assertEquals(ColorScheme.COMPLEMENTARY, ColorScheme.create("complementary"));
        assertEquals(ColorScheme.MONOCHROMATIC, ColorScheme.create("monochromatic"));
        assertEquals(ColorScheme.ANALOGOUS, ColorScheme.create("analogous"));
        assertEquals(ColorScheme.TRIADIC, ColorScheme.create("triadic"));
        assertEquals(ColorScheme.TETRADIC, ColorScheme.create("tetradic"));
        assertEquals(ColorScheme.UNKNOWN, ColorScheme.create("unknown"));
        assertEquals(ColorScheme.GRAYSCALE, ColorScheme.create("grayscale"));
        assertEquals(ColorScheme.SPLIT_COMPLIMENTARY, ColorScheme.create("split_complimentary"));
    }

    @Test
    void colorSchemeCreateNullReturnsUnknown() {
        assertEquals(ColorScheme.UNKNOWN, ColorScheme.create(null));
    }

    // ===== CrawlAction =====
    @Test
    void crawlActionCreateValid() {
        assertEquals(CrawlAction.START, CrawlAction.create("start"));
        assertEquals(CrawlAction.STOP, CrawlAction.create("stop"));
    }

    @Test
    void crawlActionCreateNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> CrawlAction.create(null));
    }

    // ===== DiscoveryAction =====
    @Test
    void discoveryActionCreateValid() {
        assertEquals(DiscoveryAction.START, DiscoveryAction.create("start"));
        assertEquals(DiscoveryAction.STOP, DiscoveryAction.create("stop"));
    }

    @Test
    void discoveryActionCreateNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> DiscoveryAction.create(null));
    }

    // ===== DomainAction =====
    @Test
    void domainActionCreateValid() {
        assertEquals(DomainAction.CREATE, DomainAction.create("create"));
        assertEquals(DomainAction.DELETE, DomainAction.create("delete"));
    }

    @Test
    void domainActionCreateNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> DomainAction.create(null));
    }

    // ===== ElementClassification =====
    @Test
    void elementClassificationCreateValid() {
        assertEquals(ElementClassification.TEMPLATE, ElementClassification.create("TEMPLATE"));
        assertEquals(ElementClassification.LEAF, ElementClassification.create("LEAF"));
        assertEquals(ElementClassification.SLIDER, ElementClassification.create("SLIDER"));
        assertEquals(ElementClassification.ANCESTOR, ElementClassification.create("ANCESTOR"));
        assertEquals(ElementClassification.UNKNOWN, ElementClassification.create("UNKNOWN"));
    }

    @Test
    void elementClassificationCreateNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> ElementClassification.create(null));
    }

    // ===== ExecutionStatus =====
    @Test
    void executionStatusCreateValid() {
        assertEquals(ExecutionStatus.RUNNING, ExecutionStatus.create("running"));
        assertEquals(ExecutionStatus.STOPPED, ExecutionStatus.create("stopped"));
        assertEquals(ExecutionStatus.COMPLETE, ExecutionStatus.create("complete"));
        assertEquals(ExecutionStatus.IN_PROGRESS, ExecutionStatus.create("in_progress"));
        assertEquals(ExecutionStatus.ERROR, ExecutionStatus.create("error"));
        assertEquals(ExecutionStatus.RUNNING_AUDITS, ExecutionStatus.create("running audits"));
        assertEquals(ExecutionStatus.BUILDING_PAGE, ExecutionStatus.create("building page"));
        assertEquals(ExecutionStatus.EXTRACTING_ELEMENTS, ExecutionStatus.create("extracting_elements"));
        assertEquals(ExecutionStatus.EXCEEDED_SUBSCRIPTION, ExecutionStatus.create("exceeded_subscription"));
        assertEquals(ExecutionStatus.UNKNOWN, ExecutionStatus.create("unknown"));
    }

    @Test
    void executionStatusCreateNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> ExecutionStatus.create(null));
    }

    @Test
    void executionStatusValues() {
        assertEquals(10, ExecutionStatus.values().length);
    }

    // ===== FormFactor =====
    @Test
    void formFactorCreateValid() {
        assertEquals(FormFactor.UNKNOWN_FORM_FACTOR, FormFactor.create("UNKNOWN_FORM_FACTOR"));
        assertEquals(FormFactor.DESKTOP, FormFactor.create("desktop"));
        assertEquals(FormFactor.MOBILE, FormFactor.create("mobile"));
        assertEquals(FormFactor.NONE, FormFactor.create("none"));
    }

    @Test
    void formFactorCreateNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> FormFactor.create(null));
    }

    // ===== FormStatus =====
    @Test
    void formStatusCreateValid() {
        assertEquals(FormStatus.DISCOVERED, FormStatus.create("discovered"));
        assertEquals(FormStatus.ACTION_REQUIRED, FormStatus.create("action_required"));
        assertEquals(FormStatus.CLASSIFIED, FormStatus.create("classified"));
    }

    @Test
    void formStatusCreateNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> FormStatus.create(null));
    }

    // ===== FormType =====
    @Test
    void formTypeCreateValid() {
        assertEquals(FormType.LOGIN, FormType.create("LOGIN"));
        assertEquals(FormType.REGISTRATION, FormType.create("REGISTRATION"));
        assertEquals(FormType.CONTACT_COMPANY, FormType.create("CONTACT_COMPANY"));
        assertEquals(FormType.SUBSCRIBE, FormType.create("SUBSCRIBE"));
        assertEquals(FormType.LEAD, FormType.create("LEAD"));
        assertEquals(FormType.SEARCH, FormType.create("SEARCH"));
        assertEquals(FormType.PASSWORD_RESET, FormType.create("PASSWORD_RESET"));
        assertEquals(FormType.PAYMENT, FormType.create("PAYMENT"));
        assertEquals(FormType.UNKNOWN, FormType.create("UNKNOWN"));
    }

    @Test
    void formTypeCreateNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> FormType.create(null));
    }

    // ===== InsightType =====
    @Test
    void insightTypeCreateValid() {
        assertEquals(InsightType.PERFORMANCE, InsightType.create("PERFORMANCE"));
        assertEquals(InsightType.ACCESSIBILITY, InsightType.create("ACCESSIBILITY"));
        assertEquals(InsightType.SEO, InsightType.create("SEO"));
        assertEquals(InsightType.PWA, InsightType.create("PWA"));
        assertEquals(InsightType.SECURITY, InsightType.create("SECURITY"));
        assertEquals(InsightType.UNKNOWN, InsightType.create("UNKNOWN"));
    }

    @Test
    void insightTypeCreateNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> InsightType.create(null));
    }

    // ===== ItemType =====
    @Test
    void itemTypeCreateValid() {
        assertEquals(ItemType.TEXT, ItemType.create("text"));
        assertEquals(ItemType.BYTES, ItemType.create("bytes"));
        assertEquals(ItemType.NUMERIC, ItemType.create("numeric"));
        assertEquals(ItemType.URL, ItemType.create("url"));
    }

    @Test
    void itemTypeCreateNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> ItemType.create(null));
    }

    // ===== JourneyStatus =====
    @Test
    void journeyStatusCreateValid() {
        assertEquals(JourneyStatus.CANDIDATE, JourneyStatus.create("CANDIDATE"));
        assertEquals(JourneyStatus.REVIEWING, JourneyStatus.create("REVIEWING"));
        assertEquals(JourneyStatus.DISCARDED, JourneyStatus.create("DISCARDED"));
        assertEquals(JourneyStatus.VERIFIED, JourneyStatus.create("VERIFIED"));
        assertEquals(JourneyStatus.ERROR, JourneyStatus.create("ERROR"));
    }

    @Test
    void journeyStatusCreateNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> JourneyStatus.create(null));
    }

    // ===== ObservationType =====
    @Test
    void observationTypeCreateValid() {
        assertEquals(ObservationType.ELEMENT, ObservationType.create("Element"));
        assertEquals(ObservationType.TYPOGRAPHY, ObservationType.create("Typography"));
        assertEquals(ObservationType.COLOR_PALETTE, ObservationType.create("Color_Palette"));
        assertEquals(ObservationType.COLOR_CONTRAST, ObservationType.create("Color_Contrast"));
        assertEquals(ObservationType.SECURITY, ObservationType.create("Security"));
        assertEquals(ObservationType.SEO, ObservationType.create("SEO"));
        assertEquals(ObservationType.UNKNOWN, ObservationType.create("Unknown"));
    }

    @Test
    void observationTypeCreateNullReturnsUnknown() {
        assertEquals(ObservationType.UNKNOWN, ObservationType.create(null));
    }

    // ===== PathStatus =====
    @Test
    void pathStatusCreateValid() {
        assertEquals(PathStatus.READY, PathStatus.create("ready"));
        assertEquals(PathStatus.EXPANDED, PathStatus.create("expanded"));
        assertEquals(PathStatus.EXAMINED, PathStatus.create("examined"));
    }

    @Test
    void pathStatusCreateNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> PathStatus.create(null));
    }

    // ===== Priority =====
    @Test
    void priorityCreateValid() {
        assertEquals(Priority.HIGH, Priority.create("high"));
        assertEquals(Priority.MEDIUM, Priority.create("medium"));
        assertEquals(Priority.LOW, Priority.create("low"));
        assertEquals(Priority.NONE, Priority.create("none"));
    }

    @Test
    void priorityCreateCaseInsensitive() {
        assertEquals(Priority.HIGH, Priority.create("HIGH"));
    }

    // ===== StepType =====
    @Test
    void stepTypeCreateValid() {
        assertEquals(StepType.UNKNOWN, StepType.create("unknown"));
        assertEquals(StepType.SIMPLE, StepType.create("SIMPLE"));
        assertEquals(StepType.REDIRECT, StepType.create("REDIRECT"));
        assertEquals(StepType.LOGIN, StepType.create("LOGIN"));
        assertEquals(StepType.LANDING, StepType.create("LANDING"));
    }

    @Test
    void stepTypeCreateNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> StepType.create(null));
    }

    // ===== SubscriptionPlan =====
    @Test
    void subscriptionPlanCreateValid() {
        assertEquals(SubscriptionPlan.FREE, SubscriptionPlan.create("FREE"));
        assertEquals(SubscriptionPlan.ENTERPRISE, SubscriptionPlan.create("ENTERPRISE"));
        assertEquals(SubscriptionPlan.STARTUP, SubscriptionPlan.create("STARTUP"));
        assertEquals(SubscriptionPlan.COMPANY_PRO, SubscriptionPlan.create("PRO"));
        assertEquals(SubscriptionPlan.COMPANY_PREMIUM, SubscriptionPlan.create("PREMIUM"));
        assertEquals(SubscriptionPlan.AGENCY_PRO, SubscriptionPlan.create("AGENCY_PRO"));
        assertEquals(SubscriptionPlan.AGENCY_PREMIUM, SubscriptionPlan.create("AGENCY_PREMIUM"));
        assertEquals(SubscriptionPlan.UNLIMITED, SubscriptionPlan.create("UNLIMITED"));
    }

    // ===== TemplateType =====
    @Test
    void templateTypeCreateValid() {
        assertEquals(TemplateType.UNKNOWN, TemplateType.create("unknown"));
        assertEquals(TemplateType.ATOM, TemplateType.create("atom"));
        assertEquals(TemplateType.MOLECULE, TemplateType.create("molecule"));
        assertEquals(TemplateType.ORGANISM, TemplateType.create("organism"));
        assertEquals(TemplateType.TEMPLATE, TemplateType.create("template"));
    }

    @Test
    void templateTypeCreateNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> TemplateType.create(null));
    }

    // ===== TestStatus =====
    @Test
    void testStatusCreateValid() {
        assertEquals(TestStatus.PASSING, TestStatus.create("PASSING"));
        assertEquals(TestStatus.FAILING, TestStatus.create("FAILING"));
        assertEquals(TestStatus.UNVERIFIED, TestStatus.create("UNVERIFIED"));
        assertEquals(TestStatus.RUNNING, TestStatus.create("RUNNING"));
    }

    @Test
    void testStatusCreateNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> TestStatus.create(null));
    }

    // ===== ToneOfVoice =====
    @Test
    void toneOfVoiceCreateValid() {
        assertEquals(ToneOfVoice.CONFIDENT, ToneOfVoice.create("confident"));
        assertEquals(ToneOfVoice.NEUTRAL, ToneOfVoice.create("neutral"));
        assertEquals(ToneOfVoice.JOYFUL, ToneOfVoice.create("joyful"));
        assertEquals(ToneOfVoice.OPTIMISTIC, ToneOfVoice.create("optimistic"));
        assertEquals(ToneOfVoice.FRIENDLY, ToneOfVoice.create("friendly"));
        assertEquals(ToneOfVoice.URGENT, ToneOfVoice.create("urgent"));
        assertEquals(ToneOfVoice.ANALYTICAl, ToneOfVoice.create("analytical"));
        assertEquals(ToneOfVoice.RESPECTFUL, ToneOfVoice.create("respectful"));
        assertEquals(ToneOfVoice.UNKNOWN, ToneOfVoice.create("unknown"));
    }

    @Test
    void toneOfVoiceCreateNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> ToneOfVoice.create(null));
    }

    // ===== WCAGComplianceLevel =====
    @Test
    void wcagComplianceLevelCreateValid() {
        assertEquals(WCAGComplianceLevel.A, WCAGComplianceLevel.create("A"));
        assertEquals(WCAGComplianceLevel.AA, WCAGComplianceLevel.create("AA"));
        assertEquals(WCAGComplianceLevel.AAA, WCAGComplianceLevel.create("AAA"));
        assertEquals(WCAGComplianceLevel.UNKNOWN, WCAGComplianceLevel.create("UNKNOWN"));
    }

    @Test
    void wcagComplianceLevelCreateNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> WCAGComplianceLevel.create(null));
    }

    @Test
    void wcagComplianceLevelToString() {
        assertEquals("A", WCAGComplianceLevel.A.toString());
        assertEquals("AA", WCAGComplianceLevel.AA.toString());
    }

    // ===== Comprehensive toString tests for all enums =====
    @Test
    void allEnumsToStringNotNull() {
        for (Action v : Action.values()) assertNotNull(v.toString());
        for (AlertChoice v : AlertChoice.values()) assertNotNull(v.toString());
        for (AnimationType v : AnimationType.values()) assertNotNull(v.toString());
        for (AuditCategory v : AuditCategory.values()) assertNotNull(v.toString());
        for (AuditLevel v : AuditLevel.values()) assertNotNull(v.toString());
        for (AuditName v : AuditName.values()) assertNotNull(v.toString());
        for (AuditPhase v : AuditPhase.values()) assertNotNull(v.toString());
        for (AuditStage v : AuditStage.values()) assertNotNull(v.toString());
        for (AuditStatus v : AuditStatus.values()) assertNotNull(v.toString());
        for (AuditSubcategory v : AuditSubcategory.values()) assertNotNull(v.toString());
        for (AuditType v : AuditType.values()) assertNotNull(v.toString());
        for (AudienceProficiency v : AudienceProficiency.values()) assertNotNull(v.toString());
        for (BrowserEnvironment v : BrowserEnvironment.values()) assertNotNull(v.toString());
        for (BrowserType v : BrowserType.values()) assertNotNull(v.toString());
        for (BugType v : BugType.values()) assertNotNull(v.toString());
        for (CaptchaResult v : CaptchaResult.values()) assertNotNull(v.toString());
        for (ColorScheme v : ColorScheme.values()) assertNotNull(v.toString());
        for (CrawlAction v : CrawlAction.values()) assertNotNull(v.toString());
        for (DiscoveryAction v : DiscoveryAction.values()) assertNotNull(v.toString());
        for (DomainAction v : DomainAction.values()) assertNotNull(v.toString());
        for (ElementClassification v : ElementClassification.values()) assertNotNull(v.toString());
        for (ExecutionStatus v : ExecutionStatus.values()) assertNotNull(v.toString());
        for (FormFactor v : FormFactor.values()) assertNotNull(v.toString());
        for (FormStatus v : FormStatus.values()) assertNotNull(v.toString());
        for (FormType v : FormType.values()) assertNotNull(v.toString());
        for (InsightType v : InsightType.values()) assertNotNull(v.toString());
        for (ItemType v : ItemType.values()) assertNotNull(v.toString());
        for (JourneyStatus v : JourneyStatus.values()) assertNotNull(v.toString());
        for (ObservationType v : ObservationType.values()) assertNotNull(v.toString());
        for (PathStatus v : PathStatus.values()) assertNotNull(v.toString());
        for (Priority v : Priority.values()) assertNotNull(v.toString());
        for (StepType v : StepType.values()) assertNotNull(v.toString());
        for (SubscriptionPlan v : SubscriptionPlan.values()) assertNotNull(v.toString());
        for (TemplateType v : TemplateType.values()) assertNotNull(v.toString());
        for (TestStatus v : TestStatus.values()) assertNotNull(v.toString());
        for (ToneOfVoice v : ToneOfVoice.values()) assertNotNull(v.toString());
        for (WCAGComplianceLevel v : WCAGComplianceLevel.values()) assertNotNull(v.toString());
    }

    // ===== Round-trip test: create(toString()) for all enums =====
    @Test
    void allEnumsRoundTrip() {
        for (Action v : Action.values()) assertEquals(v, Action.create(v.toString()));
        for (AlertChoice v : AlertChoice.values()) assertEquals(v, AlertChoice.create(v.toString()));
        for (AnimationType v : AnimationType.values()) assertEquals(v, AnimationType.create(v.toString()));
        for (AuditCategory v : AuditCategory.values()) assertEquals(v, AuditCategory.create(v.toString()));
        for (AuditLevel v : AuditLevel.values()) assertEquals(v, AuditLevel.create(v.toString()));
        for (AuditName v : AuditName.values()) assertEquals(v, AuditName.create(v.toString()));
        for (AuditPhase v : AuditPhase.values()) assertEquals(v, AuditPhase.create(v.toString()));
        for (AuditStage v : AuditStage.values()) assertEquals(v, AuditStage.create(v.toString()));
        for (AuditStatus v : AuditStatus.values()) assertEquals(v, AuditStatus.create(v.toString()));
        for (AuditSubcategory v : AuditSubcategory.values()) assertEquals(v, AuditSubcategory.create(v.toString()));
        for (AuditType v : AuditType.values()) assertEquals(v, AuditType.create(v.toString()));
        for (AudienceProficiency v : AudienceProficiency.values()) assertEquals(v, AudienceProficiency.create(v.toString()));
        for (BrowserEnvironment v : BrowserEnvironment.values()) assertEquals(v, BrowserEnvironment.create(v.toString()));
        for (BrowserType v : BrowserType.values()) assertEquals(v, BrowserType.create(v.toString()));
        for (BugType v : BugType.values()) assertEquals(v, BugType.create(v.toString()));
        for (CaptchaResult v : CaptchaResult.values()) assertEquals(v, CaptchaResult.create(v.toString()));
        for (ColorScheme v : ColorScheme.values()) assertEquals(v, ColorScheme.create(v.toString()));
        for (CrawlAction v : CrawlAction.values()) assertEquals(v, CrawlAction.create(v.toString()));
        for (DiscoveryAction v : DiscoveryAction.values()) assertEquals(v, DiscoveryAction.create(v.toString()));
        for (DomainAction v : DomainAction.values()) assertEquals(v, DomainAction.create(v.toString()));
        for (ElementClassification v : ElementClassification.values()) assertEquals(v, ElementClassification.create(v.toString()));
        for (ExecutionStatus v : ExecutionStatus.values()) assertEquals(v, ExecutionStatus.create(v.toString()));
        for (FormFactor v : FormFactor.values()) assertEquals(v, FormFactor.create(v.toString()));
        for (FormStatus v : FormStatus.values()) assertEquals(v, FormStatus.create(v.toString()));
        for (FormType v : FormType.values()) assertEquals(v, FormType.create(v.toString()));
        for (InsightType v : InsightType.values()) assertEquals(v, InsightType.create(v.toString()));
        for (ItemType v : ItemType.values()) assertEquals(v, ItemType.create(v.toString()));
        for (JourneyStatus v : JourneyStatus.values()) assertEquals(v, JourneyStatus.create(v.toString()));
        for (ObservationType v : ObservationType.values()) assertEquals(v, ObservationType.create(v.toString()));
        for (PathStatus v : PathStatus.values()) assertEquals(v, PathStatus.create(v.toString()));
        for (Priority v : Priority.values()) assertEquals(v, Priority.create(v.toString()));
        for (StepType v : StepType.values()) assertEquals(v, StepType.create(v.toString()));
        for (SubscriptionPlan v : SubscriptionPlan.values()) assertEquals(v, SubscriptionPlan.create(v.toString()));
        for (TemplateType v : TemplateType.values()) assertEquals(v, TemplateType.create(v.toString()));
        for (TestStatus v : TestStatus.values()) assertEquals(v, TestStatus.create(v.toString()));
        for (ToneOfVoice v : ToneOfVoice.values()) assertEquals(v, ToneOfVoice.create(v.toString()));
        for (WCAGComplianceLevel v : WCAGComplianceLevel.values()) assertEquals(v, WCAGComplianceLevel.create(v.toString()));
    }

    // ===== getShortName tests =====
    @Test
    void allEnumsGetShortNameNotNull() {
        for (Action v : Action.values()) assertNotNull(v.getShortName());
        for (AlertChoice v : AlertChoice.values()) assertNotNull(v.getShortName());
        for (AnimationType v : AnimationType.values()) assertNotNull(v.getShortName());
        for (AuditLevel v : AuditLevel.values()) assertNotNull(v.getShortName());
        for (AuditStage v : AuditStage.values()) assertNotNull(v.getShortName());
        for (AuditStatus v : AuditStatus.values()) assertNotNull(v.getShortName());
        for (AuditType v : AuditType.values()) assertNotNull(v.getShortName());
        for (BrowserType v : BrowserType.values()) assertNotNull(v.getShortName());
        for (ColorScheme v : ColorScheme.values()) assertNotNull(v.getShortName());
        for (ExecutionStatus v : ExecutionStatus.values()) assertNotNull(v.getShortName());
        for (FormType v : FormType.values()) assertNotNull(v.getShortName());
        for (JourneyStatus v : JourneyStatus.values()) assertNotNull(v.getShortName());
        for (TemplateType v : TemplateType.values()) assertNotNull(v.getShortName());
        for (TestStatus v : TestStatus.values()) assertNotNull(v.getShortName());
        for (ToneOfVoice v : ToneOfVoice.values()) assertNotNull(v.getShortName());
        for (WCAGComplianceLevel v : WCAGComplianceLevel.values()) assertNotNull(v.getShortName());
    }
}
