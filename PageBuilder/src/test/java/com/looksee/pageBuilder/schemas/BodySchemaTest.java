package com.looksee.pageBuilder.schemas;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BodySchemaTest {

    @Test
    void noArgConstructorCreatesNullMessage() {
        BodySchema body = new BodySchema();
        assertNull(body.getMessage());
    }

    @Test
    void allArgsConstructorSetsMessage() {
        MessageSchema msg = new MessageSchema(null, "dGVzdA==");
        BodySchema body = new BodySchema(msg);
        assertNotNull(body.getMessage());
        assertEquals("dGVzdA==", body.getMessage().getData());
    }

    @Test
    void setterAndGetterWork() {
        BodySchema body = new BodySchema();
        MessageSchema msg = new MessageSchema(null, "abc");
        body.setMessage(msg);
        assertEquals(msg, body.getMessage());
    }

    @Test
    void equalsAndHashCodeForEqualObjects() {
        MessageSchema msg1 = new MessageSchema(null, "data1");
        MessageSchema msg2 = new MessageSchema(null, "data1");
        BodySchema body1 = new BodySchema(msg1);
        BodySchema body2 = new BodySchema(msg2);

        assertEquals(body1, body2);
        assertEquals(body1.hashCode(), body2.hashCode());
    }

    @Test
    void equalsReturnsFalseForDifferentObjects() {
        BodySchema body1 = new BodySchema(new MessageSchema(null, "a"));
        BodySchema body2 = new BodySchema(new MessageSchema(null, "b"));
        assertNotEquals(body1, body2);
    }

    @Test
    void toStringContainsClassName() {
        BodySchema body = new BodySchema(new MessageSchema(null, "x"));
        String str = body.toString();
        assertNotNull(str);
        assertTrue(str.contains("BodySchema"));
    }
}
