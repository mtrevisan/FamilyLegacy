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

import java.util.EventListener;

import ch.thn.datatree.core.CollectionTreeNodeInterface;


/**
 * 
 * 
 *
 * @author Thomas Naeff (github.com/thnaeff)
 *
 * @param <K>
 * @param <V>
 */
public interface TreeNodeListener<N extends CollectionTreeNodeInterface<?, N>> extends EventListener {


	/**
	 * Called when a child node has been removed.
	 * 
	 * @param e A {@link TreeNodeEvent}. The source is the node to which the child node has been 
	 * added to. 
	 */
	public void childNodeRemoved(TreeNodeEvent<N> e);

	/**
	 * Called when one or more child nodes have been added
	 * 
	 * @param e
	 */
	public void childNodeAdded(TreeNodeEvent<N> e);

	/**
	 * Called when the value of a node changed
	 * 
	 * @param e
	 */
	public void nodeValueChanged(TreeNodeEvent<N> e);

	/**
	 * Called when a node is added to a tree
	 * 
	 * @param e
	 */
	public void addedToTree(TreeNodeEvent<N> e);
	
	/**
	 * Called when a node is removed from a tree
	 * 
	 * @param e
	 */
	public void removedFromTree(TreeNodeEvent<N> e);

}
