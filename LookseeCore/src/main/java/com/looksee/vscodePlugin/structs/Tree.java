package com.looksee.vscodePlugin.structs;

/**
 * Tree class
 * @param <E> the type of the elements in the tree
 */
public class Tree<E> {
	/**
	 * The root node of the tree
	 */
	TreeNode<E> root_node;
	
	/**
	 * Constructor for {@link Tree}
	 * @param root the root node of the tree
	 *
	 * precondition: root != null
	 */
	public Tree(TreeNode<E> root){
		assert root != null : "root must not be null";
		this.root_node = root;
	}
}
