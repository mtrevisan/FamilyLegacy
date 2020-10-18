package ch.thn.datatree;

import ch.thn.datatree.core.GenericKeyListTreeNode;



/**
 * The benefit of having a tree implementation which extends the generic tree class instead of the 
 * implementation (e.g. GenericKeyListTreeNode instead of KeyListTreeNode) is that it can be copied 
 * with the utility class returning the same tree type and getting children of a node returns a 
 * collection of nodes of the same type rather than nodes of the extended tree implementation type.
 * 
 * If MyOwnTree would extend KeyListTreeNode instead of GenericKeyListTreeNode, a call to getChildNodes 
 * for example would return a list of KeyListTreeNodes. When extending GenericKeyListTreeNode, the child 
 * node type can be specified as MyOwnTree and a call to getChildNodes returns a list of MyOwnTree nodes.
 * 
 * 
 * @author Thomas Naeff (github.com/thnaeff)
 *
 */
public class MyOwnTree extends GenericKeyListTreeNode<String, String, MyOwnTree> {

	public MyOwnTree(String key, String value) {
		super(key, value);
	}

	@Override
	public MyOwnTree nodeFactory(String value) {
		return new MyOwnTree(null, value);
	}

	@Override
	public MyOwnTree nodeFactory(MyOwnTree node) {
		return new MyOwnTree(node.getNodeKey(), node.getNodeValue());
	}

	@Override
	public MyOwnTree nodeFactory(String key, String value) {
		return new MyOwnTree(key, value);
	}
	
	@Override
	protected MyOwnTree internalGetThis() {
		return this;
	}
	
	

}
