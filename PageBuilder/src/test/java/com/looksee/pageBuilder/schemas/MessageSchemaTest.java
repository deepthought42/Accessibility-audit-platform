package com.looksee.pageBuilder.schemas;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MessageSchemaTest {

    @Test
    void noArgConstructorCreatesNullData() {
        MessageSchema msg = new MessageSchema();
        assertNull(msg.getData());
    }

    @Test
    void allArgsConstructorSetsData() {
        MessageSchema msg = new MessageSchema("dGVzdA==");
        assertEquals("dGVzdA==", msg.getData());
    }

    @Test
    void setterAndGetterWork() {
        MessageSchema msg = new MessageSchema();
        msg.setData("newData");
        assertEquals("newData", msg.getData());
    }

    @Test
    void equalsAndHashCodeForEqualObjects() {
        MessageSchema msg1 = new MessageSchema("same");
        MessageSchema msg2 = new MessageSchema("same");
        assertEquals(msg1, msg2);
        assertEquals(msg1.hashCode(), msg2.hashCode());
    }

    @Test
    void equalsReturnsFalseForDifferentData() {
        MessageSchema msg1 = new MessageSchema("a");
        MessageSchema msg2 = new MessageSchema("b");
        assertNotEquals(msg1, msg2);
    }

    @Test
    void toStringContainsClassName() {
        MessageSchema msg = new MessageSchema("test");
        String str = msg.toString();
        assertNotNull(str);
        assertTrue(str.contains("MessageSchema"));
    }
}
