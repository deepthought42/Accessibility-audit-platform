package com.looksee.pageBuilder.schemas;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MessageSchemaTest {

    @Test
    void noArgConstructorCreatesNullData() {
        MessageSchema msg = new MessageSchema();
        assertNull(msg.getData());
        assertNull(msg.getMessageId());
    }

    @Test
    void allArgsConstructorSetsData() {
        MessageSchema msg = new MessageSchema("msg-123", "dGVzdA==");
        assertEquals("dGVzdA==", msg.getData());
        assertEquals("msg-123", msg.getMessageId());
    }

    @Test
    void setterAndGetterWork() {
        MessageSchema msg = new MessageSchema();
        msg.setData("newData");
        msg.setMessageId("id-456");
        assertEquals("newData", msg.getData());
        assertEquals("id-456", msg.getMessageId());
    }

    @Test
    void equalsAndHashCodeForEqualObjects() {
        MessageSchema msg1 = new MessageSchema(null, "same");
        MessageSchema msg2 = new MessageSchema(null, "same");
        assertEquals(msg1, msg2);
        assertEquals(msg1.hashCode(), msg2.hashCode());
    }

    @Test
    void equalsReturnsFalseForDifferentData() {
        MessageSchema msg1 = new MessageSchema(null, "a");
        MessageSchema msg2 = new MessageSchema(null, "b");
        assertNotEquals(msg1, msg2);
    }

    @Test
    void toStringContainsClassName() {
        MessageSchema msg = new MessageSchema(null, "test");
        String str = msg.toString();
        assertNotNull(str);
        assertTrue(str.contains("MessageSchema"));
    }
}
