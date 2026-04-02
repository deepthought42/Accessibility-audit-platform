package com.looksee.vscodePlugin;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

import com.looksee.models.Domain;
import com.looksee.vscodePlugin.structs.*;

/**
 * Unit tests for VS Code plugin classes.
 */
class VscodePluginTest {

    // ===== Message =====
    @Test
    void messageConstructor() {
        Domain domain = new Domain();
        Message<String> msg = new Message<>("acc123", "data", new HashMap<>(), domain);
        assertEquals("acc123", msg.getAccountKey());
        assertEquals("data", msg.getData());
        assertNotNull(msg.getOptions());
        assertNotNull(msg.getDomain());
    }

    @Test
    void messageClone() {
        Domain domain = new Domain();
        Message<String> msg = new Message<>("acc", "data", new HashMap<>(), domain);
        Message<String> clone = msg.clone();
        assertEquals(msg.getAccountKey(), clone.getAccountKey());
        assertEquals(msg.getData(), clone.getData());
        assertNotSame(msg, clone);
    }

    // ===== Tree =====
    @Test
    void treeConstructor() {
        TreeNode<String> root = new TreeNode<>("root");
        Tree<String> tree = new Tree<>(root);
        assertNotNull(tree);
    }

    // ===== TreeNode =====
    @Test
    void treeNodeConstructor() {
        TreeNode<String> node = new TreeNode<>("data");
        assertEquals("data", node.getRoot());
        assertNotNull(node.getChildNodes());
        assertTrue(node.getChildNodes().isEmpty());
    }

    @Test
    void treeNodeAddChildNode() {
        TreeNode<String> parent = new TreeNode<>("parent");
        TreeNode<String> child = new TreeNode<>("child");
        assertTrue(parent.addChildNode(child));
        assertEquals(1, parent.getChildNodes().size());
    }

    @Test
    void treeNodeAddChildNodes() {
        TreeNode<String> parent = new TreeNode<>("parent");
        java.util.List<TreeNode<String>> children = new java.util.ArrayList<>();
        children.add(new TreeNode<>("c1"));
        children.add(new TreeNode<>("c2"));
        parent.addChildNodes(children);
        assertEquals(2, parent.getChildNodes().size());
    }

    // ===== SessionTestTracker =====
    @Test
    void sessionTestTrackerSingleton() {
        SessionTestTracker tracker1 = SessionTestTracker.getInstance();
        SessionTestTracker tracker2 = SessionTestTracker.getInstance();
        assertSame(tracker1, tracker2);
    }

    @Test
    void sessionTestTrackerAddAndGetSession() {
        SessionTestTracker tracker = SessionTestTracker.getInstance();
        tracker.addSessionSequences("session1");
        TestMapper mapper = tracker.getSequencesForSession("session1");
        assertNotNull(mapper);
    }

    @Test
    void sessionTestTrackerGetNonExistentSession() {
        SessionTestTracker tracker = SessionTestTracker.getInstance();
        TestMapper mapper = tracker.getSequencesForSession("nonexistent_" + System.nanoTime());
        assertNull(mapper);
    }

    // ===== TestMapper =====
    @Test
    void testMapperDefaultConstructor() {
        TestMapper mapper = new TestMapper();
        assertNotNull(mapper);
        assertNotNull(mapper.getTestHash());
        assertTrue(mapper.getTestHash().isEmpty());
    }
}
