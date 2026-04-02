package com.looksee.models;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a header node in the DOM tree.
 *
 * invariant: tag != null
 * invariant: text != null
 * invariant: children != null
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HeaderNode {
    private String tag;
    private String text;
    private List<HeaderNode> children;

    /**
     * Constructs a new header node with the given tag and text
     * @param tag the tag of the header node
     * @param text the text of the header node
     *
     * precondition: tag != null
     * precondition: text != null
     */
    public HeaderNode(String tag, String text) {
        assert tag != null;
        assert text != null;
        this.tag = tag;
        this.text = text;
        this.children = new ArrayList<>();
    }

    /**
     * Adds a child to the header node
     * @param child the child to add
     *
     * precondition: child != null
     */
    public void addChild(HeaderNode child) {
        assert child != null;
        this.children.add(child);
    }
}
