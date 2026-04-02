package com.looksee.browsing;

import java.util.ArrayList;
import java.util.List;

public class ElementNode<T> {
	private List<ElementNode<T>> children = new ArrayList<ElementNode<T>>();
	private ElementNode<T> parent = null;
	private T data = null;

	public ElementNode(T data) {
        this.data = data;
    }

    public ElementNode(T data, ElementNode<T> parent) {
        this.data = data;
        this.parent = parent;
    }

    public List<ElementNode<T>> getChildren() {
        return children;
    }

    public void setParent(ElementNode<T> parent) {
        parent.addChild(this);
        this.parent = parent;
    }

    public void addChild(T data) {
        ElementNode<T> child = new ElementNode<T>(data);
        child.setParent(this);
        this.children.add(child);
    }

    public void addChild(ElementNode<T> child) {
        child.setParent(this);
        this.children.add(child);
    }

    public T getData() {
        return this.data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public boolean isRoot() {
        return (this.parent == null);
    }

    public boolean isLeaf() {
        return this.children.size() == 0;
    }

    public void removeParent() {
        this.parent = null;
    }
}
