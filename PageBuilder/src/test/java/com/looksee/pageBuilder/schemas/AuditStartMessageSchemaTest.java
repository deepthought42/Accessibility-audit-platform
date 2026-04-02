package com.looksee.pageBuilder.schemas;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AuditStartMessageSchemaTest {

    @Test
    void noArgConstructorCreatesNullFields() {
        AuditStartMessageSchema schema = new AuditStartMessageSchema();
        assertNull(schema.getUrl());
        assertNull(schema.getType());
        assertNull(schema.getAccountId());
        assertNull(schema.getAuditId());
    }

    @Test
    void allArgsConstructorSetsAllFields() {
        AuditStartMessageSchema schema = new AuditStartMessageSchema(
                "https://example.com", "PAGE", "123", "456");

        assertEquals("https://example.com", schema.getUrl());
        assertEquals("PAGE", schema.getType());
        assertEquals("123", schema.getAccountId());
        assertEquals("456", schema.getAuditId());
    }

    @Test
    void settersAndGettersWork() {
        AuditStartMessageSchema schema = new AuditStartMessageSchema();
        schema.setUrl("https://test.com");
        schema.setType("DOMAIN");
        schema.setAccountId("42");
        schema.setAuditId("99");

        assertEquals("https://test.com", schema.getUrl());
        assertEquals("DOMAIN", schema.getType());
        assertEquals("42", schema.getAccountId());
        assertEquals("99", schema.getAuditId());
    }

    @Test
    void equalsAndHashCodeForEqualObjects() {
        AuditStartMessageSchema s1 = new AuditStartMessageSchema("u", "t", "a", "i");
        AuditStartMessageSchema s2 = new AuditStartMessageSchema("u", "t", "a", "i");
        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
    }

    @Test
    void equalsReturnsFalseForDifferentObjects() {
        AuditStartMessageSchema s1 = new AuditStartMessageSchema("u1", "t", "a", "i");
        AuditStartMessageSchema s2 = new AuditStartMessageSchema("u2", "t", "a", "i");
        assertNotEquals(s1, s2);
    }

    @Test
    void toStringContainsClassName() {
        AuditStartMessageSchema schema = new AuditStartMessageSchema("u", "t", "a", "i");
        String str = schema.toString();
        assertNotNull(str);
        assertTrue(str.contains("AuditStartMessageSchema"));
    }

    @Test
    void equalsReturnsFalseForDifferentType() {
        AuditStartMessageSchema s1 = new AuditStartMessageSchema("u", "PAGE", "a", "i");
        AuditStartMessageSchema s2 = new AuditStartMessageSchema("u", "DOMAIN", "a", "i");
        assertNotEquals(s1, s2);
    }

    @Test
    void equalsReturnsFalseForDifferentAccountId() {
        AuditStartMessageSchema s1 = new AuditStartMessageSchema("u", "t", "1", "i");
        AuditStartMessageSchema s2 = new AuditStartMessageSchema("u", "t", "2", "i");
        assertNotEquals(s1, s2);
    }

    @Test
    void equalsReturnsFalseForDifferentAuditId() {
        AuditStartMessageSchema s1 = new AuditStartMessageSchema("u", "t", "a", "1");
        AuditStartMessageSchema s2 = new AuditStartMessageSchema("u", "t", "a", "2");
        assertNotEquals(s1, s2);
    }
}
