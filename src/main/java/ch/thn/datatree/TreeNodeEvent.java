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
package ch.thn.datatree;

import java.util.EventObject;

import ch.thn.datatree.core.CollectionTreeNodeInterface;

/**
 * 
 * 
 *
 * @author Thomas Naeff (github.com/thnaeff)
 *
 * @param <V>
 * @param <N>
 */
public class TreeNodeEvent<N extends CollectionTreeNodeInterface<?, N>> extends EventObject {
	private static final long serialVersionUID = -7895991400838880597L;

	private N node = null;
	private N parent = null;

	private int nodeIndex = 0;
	private Object oldValue = null;

	/**
	 * 
	 * 
	 * @param source
	 * @param node
	 * @param parent The parent node of the <code>node</code> parameter. If the node has been 
	 * removed from a tree, this parent is the former parent.
	 * @param nodeIndex The index among its siblings of <code>node</code> parameter. If the node 
	 * has been removed from a tree, this index is the former index.
	 * @param oldValue
	 */
	public TreeNodeEvent(N source, N node, N parent, int nodeIndex, Object oldValue) {
		super(source);
		this.node = node;
		this.parent = parent;
		this.nodeIndex = nodeIndex;
		this.oldValue = oldValue;
	}
	
	/**
	 * The node which caused the event. E.g. the node to which a child node has been added to 
	 * or the node of which a child node has been removed from.
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public N getSourceNode() {
		return (N) getSource();
	}

	/**
	 * Returns the affected node, e.g. the node which has been removed, added, ...
	 * 
	 * @return
	 */
	public N getNode() {
		return node;
	}

	/**
	 * Returns the parent node of the affected node (the parent node of the node which is returned 
	 * when calling {@link #getNode()}). For a node which is in a tree, this is just its parent node. 
	 * For a node which has been removed from a tree, this is its former parent node.
	 * 
	 * @return
	 */
	public N getParentNode() {
		return parent;
	}
	
	/**
	 * The index among its siblings of the affected node (the index of the node which is returned 
	 * when calling {@link #getNode()}). For a node which is in a tree, this is just its node index. 
	 * For a node which has been removed from a tree, this is its former node index.
	 * 
	 * @return
	 */
	public int getNodeIndex() {
		return nodeIndex;
	}
	
	/**
	 * For value change events, this returns the value before the change.
	 * 
	 * @return
	 */
	public Object getOldValue() {
		return oldValue;
	}


}
