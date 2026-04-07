package com.looksee.llm.safety;

import com.looksee.llm.safety.RedactionReport.Category;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scrubs common PII and secrets out of strings before they are forwarded to a
 * third-party LLM provider. This is intentionally conservative: over-redaction
 * is acceptable (the LLM still sees placeholder tokens and the task succeeds
 * for the vast majority of accessibility use cases), under-redaction is not.
 *
 * <p>This class is stateless and thread-safe. Callers should obtain a singleton
 * from Spring.
 */
public class PiiRedactor {

    private static final Pattern EMAIL = Pattern.compile(
            "[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}");
    private static final Pattern PHONE = Pattern.compile(
            "(?<![\\w-])(?:\\+?\\d{1,3}[\\s.\\-]?)?(?:\\(?\\d{3}\\)?[\\s.\\-]?)\\d{3}[\\s.\\-]?\\d{4}(?![\\w-])");
    private static final Pattern SSN = Pattern.compile("(?<!\\d)\\d{3}-\\d{2}-\\d{4}(?!\\d)");
    private static final Pattern IPV4 = Pattern.compile(
            "(?<![\\d.])(?:\\d{1,3}\\.){3}\\d{1,3}(?![\\d.])");
    private static final Pattern JWT = Pattern.compile(
            "eyJ[A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-]+");
    // Rough shape: sk_... / pk_... / AKIA..., or long hex/base64 strings labeled
    // as keys.
    private static final Pattern API_KEY = Pattern.compile(
            "(?:sk|pk|api[_-]?key|token|secret)[_\\-:=\"' ]+[A-Za-z0-9_\\-]{16,}",
            Pattern.CASE_INSENSITIVE);
    // 13-19 digit runs — Luhn-validated before we commit to redacting.
    private static final Pattern CC_CANDIDATE = Pattern.compile("(?<!\\d)\\d{13,19}(?!\\d)");

    private static final Pattern HTML_PASSWORD_INPUT = Pattern.compile(
            "(<input\\b[^>]*type\\s*=\\s*[\"'](?:password|email|tel)[\"'][^>]*\\bvalue\\s*=\\s*)[\"'][^\"']*[\"']",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CSRF_META = Pattern.compile(
            "<meta\\s+[^>]*name\\s*=\\s*[\"']csrf-token[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COOKIE_HEADER = Pattern.compile(
            "(?i)(?:cookie|set-cookie)\\s*:\\s*[^\\n\\r]+");
    private static final Pattern URL_SECRET = Pattern.compile(
            "([?&](?:access_token|api_key|apikey|token|secret|password|pwd)=)([^&\\s\"'<>]+)",
            Pattern.CASE_INSENSITIVE);

    /**
     * Result of redacting a single payload.
     */
    public record Result(String text, RedactionReport report) {}

    /**
     * Redacts the given payload and returns both the scrubbed text and a
     * report of what was removed. If {@code input} is null, returns an empty
     * result.
     */
    public Result redact(String input) {
        if (input == null || input.isEmpty()) {
            return new Result(input == null ? "" : input, RedactionReport.empty());
        }
        RedactionReport.Builder report = RedactionReport.builder();
        String out = input;

        out = applyRegex(out, EMAIL, Category.EMAIL, "[REDACTED:EMAIL]", report);
        out = applyRegex(out, PHONE, Category.PHONE, "[REDACTED:PHONE]", report);
        out = applyRegex(out, SSN, Category.SSN, "[REDACTED:SSN]", report);
        out = applyRegex(out, JWT, Category.JWT, "[REDACTED:JWT]", report);
        out = applyRegex(out, API_KEY, Category.API_KEY, "[REDACTED:API_KEY]", report);
        out = applyRegex(out, IPV4, Category.IP_ADDRESS, "[REDACTED:IP]", report);

        out = redactLuhnCandidates(out, report);

        out = applyRegexWithBackref(out, HTML_PASSWORD_INPUT, Category.HTML_SENSITIVE_INPUT,
                "$1\"[REDACTED]\"", report);
        out = applyRegex(out, CSRF_META, Category.CSRF_TOKEN, "[REDACTED:CSRF_META]", report);
        out = applyRegex(out, COOKIE_HEADER, Category.COOKIE, "[REDACTED:COOKIE]", report);
        out = applyUrlSecretRedaction(out, report);

        return new Result(out, report.build());
    }

    private String applyRegex(String input, Pattern pattern, Category category,
                              String replacement, RedactionReport.Builder report) {
        Matcher m = pattern.matcher(input);
        int count = 0;
        StringBuilder sb = new StringBuilder();
        String quoted = Matcher.quoteReplacement(replacement);
        while (m.find()) {
            count++;
            m.appendReplacement(sb, quoted);
        }
        m.appendTail(sb);
        report.increment(category, count);
        return sb.toString();
    }

    private String applyRegexWithBackref(String input, Pattern pattern, Category category,
                                         String replacement, RedactionReport.Builder report) {
        Matcher m = pattern.matcher(input);
        int count = 0;
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            count++;
            m.appendReplacement(sb, replacement);
        }
        m.appendTail(sb);
        report.increment(category, count);
        return sb.toString();
    }

    private String applyUrlSecretRedaction(String input, RedactionReport.Builder report) {
        Matcher m = URL_SECRET.matcher(input);
        int count = 0;
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            count++;
            m.appendReplacement(sb, m.group(1) + "[REDACTED]");
        }
        m.appendTail(sb);
        report.increment(Category.URL_SECRET, count);
        return sb.toString();
    }

    private String redactLuhnCandidates(String input, RedactionReport.Builder report) {
        Matcher m = CC_CANDIDATE.matcher(input);
        StringBuilder sb = new StringBuilder();
        int count = 0;
        while (m.find()) {
            String candidate = m.group();
            if (luhnValid(candidate)) {
                count++;
                m.appendReplacement(sb, "[REDACTED:CC]");
            } else {
                m.appendReplacement(sb, Matcher.quoteReplacement(candidate));
            }
        }
        m.appendTail(sb);
        report.increment(Category.CREDIT_CARD, count);
        return sb.toString();
    }

    private boolean luhnValid(String digits) {
        int sum = 0;
        boolean alt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (alt) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alt = !alt;
        }
        return sum % 10 == 0;
    }
}
