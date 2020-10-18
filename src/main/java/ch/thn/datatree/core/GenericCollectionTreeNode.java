/**
 *    Copyright 2015 Thomas Naeff (github.com/thnaeff)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package ch.thn.datatree.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import ch.thn.datatree.TreeIterator;
import ch.thn.datatree.TreeNodeEvent;
import ch.thn.datatree.TreeNodeListener;

/**
 * 
 * 
 *
 * @author Thomas Naeff (github.com/thnaeff)
 *
 * @param <V>
 * @param <N>
 * @param <C>
 */
public abstract class GenericCollectionTreeNode<V, N extends GenericCollectionTreeNode<V, N, C>, C extends Collection<N>>
implements CollectionTreeNodeInterface<V, N>, Iterable<N>  {

	private C children = null;
	private N parent = null;
	private V value = null;

	private Set<TreeNodeListener<N>> listeners = null;
	
	protected enum TreeEventType {
		/**
		 * 
		 */
		CHILD_ADDED, 
		
		/**
		 * 
		 */
		CHILD_REMOVED, 
		
		/**
		 * 
		 */
		VALUE_CHANGED, 
		
		/**
		 * 
		 */
		ADDED, 
		
		/**
		 * 
		 */
		REMOVED;
	}

	/**
	 * 
	 * 
	 * @param childrenCollection
	 * @param value
	 */
	public GenericCollectionTreeNode(C childrenCollection, V value) {
		this.value = value;
		this.children = childrenCollection;

		listeners = new HashSet<TreeNodeListener<N>>();
	}

	/**
	 * <i><b>For internal use only!</b></i>
	 * 
	 * @return
	 */
	protected abstract N internalGetThis();

	/**
	 * 
	 * 
	 * @return
	 */
	protected C internalGetChildren() {
		return children;
	}
	
	/**
	 * A dummy method for {@link TreeNodeListener}s
	 * 
	 * @return
	 */
	protected int getNodeIndex() {
		return -1;
	}

	/**
	 * 
	 * 
	 * @param childrenCollection
	 */
	protected void internalSetChildrenCollection(C childrenCollection) {
		this.children = childrenCollection;
	}

	/**
	 * <i><b>For internal use only!</b></i>
	 * 
	 * @param parent
	 * @param notify
	 */
	protected void internalSetParentNode(N parent, boolean notify) {
		this.parent = parent;
	}


	@Override
	public void addTreeNodeListener(TreeNodeListener<N> l) {
		listeners.add(l);
	}

	@Override
	public void removeTreeNodeListener(TreeNodeListener<N> l) {
		listeners.remove(l);
	}
	
	/**
	 * Fire event listeners
	 * 
	 * @param eventType
	 * @param node
	 * @param parent
	 * @param nodeIndex
	 * @param oldValue
	 */
	protected void fireNodeEvent(TreeEventType eventType, 
			N node, N parent, int nodeIndex, Object oldValue) {
		
		TreeNodeEvent<N> e = new TreeNodeEvent<N>(internalGetThis(), node, parent, nodeIndex, oldValue);
		
		for (TreeNodeListener<N> l : listeners) {
			switch (eventType) {
			case CHILD_ADDED:
				l.childNodeAdded(e);
				//Also send a notification from the view of the added node
				node.fireNodeEvent(TreeEventType.ADDED, node, internalGetThis(), nodeIndex, null);
				break;
			case CHILD_REMOVED:
				l.childNodeRemoved(e);
				//Also send a notification from the view of the removed node
				node.fireNodeEvent(TreeEventType.REMOVED, node, internalGetThis(), nodeIndex, null);
				break;
			case VALUE_CHANGED:
				l.nodeValueChanged(e);
				break;
			case ADDED:
				l.addedToTree(e);
				break;
			case REMOVED:
				l.removedFromTree(e);
				break;
			default:
				break;
			}
		}
		
	}


	@Override
	public N addChildNode(N node) {
		return internalAddChildNode(node, true);
	}

	/**
	 * 
	 * 
	 * @param node
	 * @param notify
	 * @return
	 */
	protected N internalAddChildNode(N node, boolean notify) {
		if (node.getParentNode() != null) {
			throw new TreeNodeError("The node already has a parent node set (which " +
					"means it is from another tree)");
		}

		if (children.add(node)) {
			node.internalSetParentNode(internalGetThis(), true);

			if (notify) {
				fireNodeEvent(TreeEventType.CHILD_ADDED, node, internalGetThis(), node.getNodeIndex(), null);
			}

			return node;
		}

		return null;
	}

	@Override
	public N addChildNodeCopy(N node) {
		return addChildNode(node.nodeFactory(node));
	}

	@Override
	public boolean addChildNodes(Collection<N> nodes) {
		boolean ret = false;

		for (N node : nodes) {
			addChildNode(node);
			ret = true;
		}

		return ret;
	}

	@Override
	public void removeChildNodes() {
		internalRemoveChildNodes(true);
	}

	/**
	 * 
	 * 
	 * @param notify
	 */
	protected void internalRemoveChildNodes(boolean notify) {
		//The children might or might not be in an ordered list. Create an
		//ordered list here in whatever order the children are.
		ArrayList<N> tempChildren = new ArrayList<N>(children);

		//Remove children in reverse order with the last one first.
		//This is important because when removing
		//the first child, the indexes of all the following children change.
		for (int i = tempChildren.size() - 1; i >= 0; i--) {
			internalRemoveChildNode(tempChildren.get(i), notify);
		}
	}

	@Override
	public int getChildNodesCount() {
		return children.size();
	}

	@Override
	public N getParentNode() {
		return parent;
	}

	@Override
	public N getHeadNode() {
		if (parent == null) {
			return internalGetThis();
		}

		return parent.getHeadNode();
	}

	@Override
	public V getNodeValue() {
		return value;
	}

	@Override
	public void setNodeValue(V value) {
		Object oldValue = this.value;
		this.value = value;
		fireNodeEvent(TreeEventType.VALUE_CHANGED, internalGetThis(), this.getParentNode(), this.getNodeIndex(), oldValue);
	}

	@Override
	public N addChildNode(V value) {
		return addChildNode(nodeFactory(value));
	}

	@Override
	public boolean removeNode() {
		if (isRootNode()) {
			return false;
		}

		return getParentNode().removeChildNode(internalGetThis());
	}

	@Override
	public boolean removeChildNode(N node) {
		return internalRemoveChildNode(node, true);
	}

	/**
	 * 
	 * @param node
	 * @param notify
	 * @return
	 */
	protected boolean internalRemoveChildNode(N node, boolean notify) {
		int oldIndex = node.getNodeIndex();
		node.internalSetParentNode(null, true);

		boolean ret = children.remove(node);

		if (notify) {
			fireNodeEvent(TreeEventType.CHILD_REMOVED, node, internalGetThis(), oldIndex, null);
		}

		return ret;
	}


	@Override
	public N replaceNode(V value) {
		return replaceNode(nodeFactory(value));
	}

	@Override
	public N replaceNode(N newNode) {
		if (isRootNode()) {
			throw new TreeNodeError("Root node can not be replaced");
		}
		
		if (newNode.getParentNode() != null) {
			throw new TreeNodeError("The node already has a parent node set (which " +
					"means it is from another tree)");
		}

		Collection<N> tempChildren = new ArrayList<N>(children);
		
		//Step 1: Transfer child nodes from current node to replacement node

		//Remove all child nodes
		internalRemoveChildNodes(true);

		//Transfer all child nodes to the new node
		for (N childNode : tempChildren) {
			//Add all child nodes to new parent 
			newNode.internalAddChildNode(childNode, true);
		}

		//Step 2: Switch nodes
		
		if (getParentNode() != null) {
			getParentNode().switchChildNodes(internalGetThis(), newNode);
		}
		
		return newNode;
	}

	/**
	 * Replaces the old node with the new node.
	 * 
	 * @param oldNode
	 * @param newNode
	 */
	protected void switchChildNodes(N oldNode, N newNode) {
		if (newNode.getParentNode() != null) {
			throw new TreeNodeError("The node already has a parent node set (which " +
					"means it is from another tree)");
		}
		
		ArrayList<N> tempChildren = new ArrayList<N>(children);
		int index = tempChildren.indexOf(oldNode);

		//Use existing method to have proper notification
		internalRemoveChildNode(oldNode, true);
		tempChildren.remove(index);
		tempChildren.add(index, newNode);

		internalRemoveChildNodes(false);

		for (N node : tempChildren) {
			internalAddChildNode(node, false);
		}
		
		fireNodeEvent(TreeEventType.CHILD_ADDED, newNode, internalGetThis(), newNode.getNodeIndex(), null);

	}

	@Override
	public N getRootNode() {
		if (parent == null) {
			return internalGetThis();
		}

		return parent.getRootNode();
	}

	@Override
	public boolean isRootNode() {
		return parent == null;
	}

	@Override
	public boolean isLeafNode() {
		return getChildNodesCount() == 0;
	}

	@Override
	public int getNodeDepth() {
		if (parent == null) {
			return 0;
		}

		return parent.getNodeDepth() + 1;
	}

	@Override
	public TreeIterator<N> iterator() {
		return new TreeIterator<N>(internalGetThis());
	}

	@Override
	public TreeIterator<N> iterator(boolean subtreeOnly) {
		throw new UnsupportedOperationException("This tree implementation does not support the iteration over the subtree only");
	}

	@Override
	public String toString() {
		return value == null ? null : value.toString();
	}


}
