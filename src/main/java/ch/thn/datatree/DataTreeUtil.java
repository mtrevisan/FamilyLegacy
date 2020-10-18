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

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 *
 * @author Thomas Naeff (github.com/thnaeff)
 *
 */
public class DataTreeUtil {

	/**
	 * Creates a comparator which compares the toString output of parameters of type K
	 *
	 * @return
	 */
	public static <K> Comparator<K> getDefaultKeyComparator() {
		return new Comparator<K>() {

			@Override
			public int compare(K o1, K o2) {
				if (o1 == null && o2 != null) { return 1; }
				if (o1 != null && o2 == null) { return -1; }
				if (o1 == o2) { return 0; }

				return o1.toString().compareTo(o2.toString());
			}
		};
	}

	/**
	 * Creates a comparator which compares the toString output of the tree node
	 * value of type N
	 *
	 * @return
	 */
	public static <N extends GenericKeySetTreeNode<?, ?, N>> Comparator<N> getDefaultValueComparator() {
		return new Comparator<N>() {

			@Override
			public int compare(N o1, N o2) {
				if (o1 == null && o2 != null) { return 1; }
				if (o1 != null && o2 == null) { return -1; }
				if (o1 == o2) { return 0; }

				if (o1.getNodeValue() == null && o2.getNodeValue() != null) { return 1; }
				if (o1.getNodeValue() != null && o2.getNodeValue() == null) { return -1; }
				if (o1.getNodeValue() == o2.getNodeValue()) { return 0; }

				return o1.getNodeValue().toString().compareTo(o2.getNodeValue().toString());
			}
		};
	}


	/**
	 * Makes a copy of the whole tree, starting at the given node
	 *
	 * @param node
	 * @return
	 */
	public static <N extends CollectionTreeNodeInterface<?, N>> N copyTree(N node) {
		return copyTree(node, null);
	}

	/**
	 * Makes a copy of the whole tree, starting at the given node.
	 * If the creation of node instances has to be controlled, a node factory can be provided for that purpose.
	 *
	 * @param node
	 * @param nodeFactory
	 * @return
	 */
	public static <N extends CollectionTreeNodeInterface<?, N>> N copyTree(N node,
			DataTreeCopyNodeFactory<N> nodeFactory) {
		N newNode = node.nodeFactory(node);
		DataTreeUtil.copyTreeChildNodes(newNode, node, nodeFactory);
		return newNode;
	}

	/**
	 *
	 *
	 * @param targetNode
	 * @param sourceNode
	 */
	private static <N extends CollectionTreeNodeInterface<?, N>> void copyTreeChildNodes(N targetNode,
			N sourceNode, DataTreeCopyNodeFactory<N> nodeFactory) {

		if (sourceNode.isLeafNode()) {
			//Skip if there are no child nodes
			return;
		}

		for (N sourceChildNode : sourceNode.getChildNodes()) {
			//Add a new child node with the data of the source child node
			N newTargetChildNode = null;
			if (nodeFactory == null) {
				newTargetChildNode = targetNode.addChildNodeCopy(sourceChildNode);
			} else {
				newTargetChildNode = nodeFactory.newInstanceCopy(sourceChildNode);
				targetNode.addChildNode(newTargetChildNode);
			}

			//Add all children of the source child node to the new child node
			DataTreeUtil.copyTreeChildNodes(newTargetChildNode, sourceChildNode, nodeFactory);

		}

	}

	/**
	 * Walks two trees in parallel and (optional) builds the intersect between the two trees.
	 * The given {@link TreeIntersectProcessor} does the comparison between two nodes
	 * and decides if they are equal or not. The processor also creates the instances
	 * for the resulting intersect tree or simply can be used to do anything else with
	 * the two matching nodes.
	 *
	 * @param masterTree
	 * @param slaveNode The tree to compare against
	 * @param processor The processor which defined equality and creates nodes of the
	 * possible resulting tree
	 * @param preventDuplicates If set to <code>true</code>, once a slave node is considered
	 * equal once it is not compared to the master node again. If set to <code>false</code>,
	 * all slave nodes are compared to each master node.
	 * @return
	 */
	public static <N1 extends CollectionTreeNodeInterface<?, N1>,
					N2 extends CollectionTreeNodeInterface<?, N2>,
					N3 extends CollectionTreeNodeInterface<?, N3>>
			N3 intersect(N1 masterTree, N2 slaveTree,
			TreeIntersectProcessor<N1, N2, N3> processor, boolean preventDuplicates) {

		if (! processor.equals(masterTree, slaveTree)) {
			//The head nodes are already not equal.
			return null;
		}

		N3 intersectTree = processor.intersectNode(masterTree, slaveTree);


		intersectChildren(masterTree, slaveTree, processor,
				intersectTree, preventDuplicates);

		return intersectTree;
	}

	/**
	 *
	 *
	 * @param masterNode
	 * @param slaveNode
	 * @param processor
	 * @param intersectTree
	 * @param preventDuplicates
	 */
	private static <K,
					N1 extends CollectionTreeNodeInterface<?, N1>,
					N2 extends CollectionTreeNodeInterface<?, N2>,
					N3 extends CollectionTreeNodeInterface<?, N3>>
		void intersectChildren(
			N1 masterNode, N2 slaveNode, TreeIntersectProcessor<N1, N2, N3> processor,
			N3 intersectTree, boolean preventDuplicates) {

		Collection<N1> masterChildNodes = masterNode.getChildNodes();
		Collection<N2> slaveChildNodes = slaveNode.getChildNodes();

		//Do not keep a list of added nodes if duplicates should not be ignored
		Set<N2> copiedSlaveNodes = (preventDuplicates ? new HashSet<N2>() : null);

		//Compare each master node to each child node
		for (N1 masterChildNode : masterChildNodes) {
			for (N2 slaveChildNode : slaveChildNodes) {
				//Make sure a node of the slave tree is only considered once
				if (copiedSlaveNodes != null && copiedSlaveNodes.contains(slaveChildNode)) {
					continue;
				}

				if (processor.equals(masterChildNode, slaveChildNode)) {

					if (copiedSlaveNodes != null) {
						copiedSlaveNodes.add(slaveChildNode);
					}

					N3 intersectChildTree = null;
					//The resulting intersect tree can be null if the intersectNode method does not
					//return a tree node (e.g. the master and/or slave tree is only modified).
					if (intersectTree != null) {
						intersectChildTree = intersectTree.addChildNode(processor.intersectNode(masterChildNode, slaveChildNode));
					}

					intersectChildren(masterChildNode, slaveChildNode, processor,
							intersectChildTree, preventDuplicates);

				}
			}
		}

	}

	/**
	 * Goes through the whole tree and looks for the highest node level
	 *
	 * @param node
	 * @return
	 */
	public static <N extends CollectionTreeNodeInterface<?, N>> int highestNodeLevel(N node) {

		if (node.isLeafNode()) {
			return node.getNodeDepth();
		}

		//It is probably faster to iterate through the tree like this than
		//using node.iterator() which goes step by step and has to search for the next node
		Iterator<N> iterator = node.getChildNodes().iterator();

		int highest = 0;
		int current = 0;

		while (iterator.hasNext()) {
			N childNode = iterator.next();

			current = highestNodeLevel(childNode);
			if (current > highest) {
				highest = current;
			}

		}

		return highest;
	}

	/**
	 * Jumps to the very last leaf node of the branch of the given node.
	 *
	 * @param node
	 * @return
	 */
	public static <	V extends Object,
					N extends ListTreeNodeInterface<V, N>>
					ListTreeNodeInterface<V, N> getLastLeafNode(ListTreeNodeInterface<V, N> node) {

		if (node.isLeafNode()) {
			return node;
		}

		//Improved functionality with a list tree node (compared to the general getLastLeafNode() method)
		N lastChildNode = node.getLastChildNode();

		while (! lastChildNode.isLeafNode()) {
			lastChildNode = lastChildNode.getLastChildNode();
		}

		return lastChildNode;
	}

	/**
	 * Jumps to the very last leaf node of the branch of the given node.
	 *
	 *
	 * @param node
	 * @return
	 */
	public static <	V extends Object,
					N extends CollectionTreeNodeInterface<V, N>>
					CollectionTreeNodeInterface<V, N> getLastLeafNode(CollectionTreeNodeInterface<V, N> node) {

		if (node.isLeafNode()) {
			return node;
		}

		N lastChildNode = getLastSibling(node.iterator());

		while (! lastChildNode.isLeafNode()) {
			lastChildNode = getLastSibling(lastChildNode.iterator());
		}

		return lastChildNode;
	}

	/**
	 * Iterates over the given iterator until the last element is reached
	 *
	 * @param iterator
	 * @return
	 */
	private static <N extends CollectionTreeNodeInterface<?, N>> N getLastSibling(Iterator<N> iterator) {
		N lastSibling = null;
		while (iterator.hasNext()) {
			lastSibling = iterator.next();
		}

		return lastSibling;
	}



	/**********************************************************************************************************
	 *
	 *
	 * @author Thomas Naeff (github.com/thnaeff)
	 *
	 * @param <N>
	 */
	public interface DataTreeCopyNodeFactory<N> {

		/**
		 * Creates a new instance of a tree node and copies needed data
		 *
		 * @param node
		 * @return
		 */
		public N newInstanceCopy(N node);

	}

	/***********************************************************************************************************
	 * This class controls how the intersect is built. It has a comparator which defines how a node is considered
	 * equal in the master and the slave tree, and it has a method which creates node instances for the resulting
	 * intersect tree built from a matching master and slave node.
	 *
	 *
	 * @author Thomas Naeff (github.com/thnaeff)
	 *
	 * @param <N1>
	 * @param <N2>
	 */
	public interface TreeIntersectProcessor<N1 extends CollectionTreeNodeInterface<?, N1>,
											N2 extends CollectionTreeNodeInterface<?, N2>,
											N3 extends CollectionTreeNodeInterface<?, N3>> {

		/**
		 * Compares the master node to the slave node. If they are equal, the tree walking continues
		 * with this node. If they are not equal, the tree walking stops at this node and continues
		 * with its parent/sibling node.
		 */
		public boolean equals(N1 masterNode, N2 slaveNode);

		/**
		 * Gets called for all nodes which match in both trees. The implementation of this
		 * method builds the node for the resulting intersect tree, and/or modifies visited
		 * nodes.
		 *
		 * @param masterNode
		 * @param slaveNode
		 * @return
		 */
		public N3 intersectNode(N1 masterNode, N2 slaveNode);


	}

}
