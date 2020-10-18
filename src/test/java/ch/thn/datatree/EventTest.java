package ch.thn.datatree;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ch.thn.datatree.core.CollectionTreeNodeInterface;



/**
 * 
 * 
 * @author Thomas Naeff (github.com/thnaeff)
 *
 */
public class EventTest {
	
	private ListTreeNodeListener listTreeNodeListener = new ListTreeNodeListener();
	private SetTreeNodeListener setTreeNodeListener = new SetTreeNodeListener();
	private KeyListTreeNodeListener keyListTreeNodeListener = new KeyListTreeNodeListener();
	private KeySetTreeNodeListener keySetTreeNodeListener = new KeySetTreeNodeListener();
	
	
	
	@Test
	public void testAddEvent() throws Exception {
		
		listTreeNodeListener.affectedNodes.clear();
		setTreeNodeListener.affectedNodes.clear();
		keyListTreeNodeListener.affectedNodes.clear();
		keySetTreeNodeListener.affectedNodes.clear();
				
		ListTreeNode<String> listNode = new ListTreeNode<String>("Tree");
		SetTreeNode<String> setNode = new SetTreeNode<String>("Tree");
		KeyListTreeNode<String, String> keyListNode = new KeyListTreeNode<String, String>("", "Tree");
		KeySetTreeNode<String, String> keySetNode = new KeySetTreeNode<String, String>("", "Tree");
		
		listNode.addTreeNodeListener(listTreeNodeListener);
		setNode.addTreeNodeListener(setTreeNodeListener);
		keyListNode.addTreeNodeListener(keyListTreeNodeListener);
		keySetNode.addTreeNodeListener(keySetTreeNodeListener);
		
		//Building the tree adds new nodes (at the end only though)
		TreeTest.buildValueTree(listNode);
		TreeTest.buildValueTree(setNode);
		TreeTest.buildKeyValueTree(keyListNode);
		TreeTest.buildKeyValueTree(keySetNode);
		
		
		checkAllAdded(listNode, listTreeNodeListener.affectedNodes);
		checkAllAdded(setNode, setTreeNodeListener.affectedNodes);
		checkAllAdded(keyListNode, keyListTreeNodeListener.affectedNodes);
		checkAllAdded(keySetNode, keySetTreeNodeListener.affectedNodes);
		
	}
	
	@Test
	public void testRemoveEvent() throws Exception {
		
		//TODO
		
	}
	
	
	/**
	 * 
	 * 
	 * @param node
	 * @param nodes
	 */
	private <N extends CollectionTreeNodeInterface> void checkAllAdded(N node, List<N> nodes) {
		
		List<CollectionTreeNodeInterface> treeNodes = new ArrayList<CollectionTreeNodeInterface>();
		
		TreeIterator iterator = node.iterator();
		
		//Skip tree head, because it has not been added
		iterator.next();
		
		while (iterator.hasNext()) {
			CollectionTreeNodeInterface n = iterator.next();
			treeNodes.add(n);
			
			try {
				assertTrue(n.toString(), nodes.contains(n));
			} catch (Exception e) {
				//Continue collecting so that they can be compared visually
				while (iterator.hasNext()) {
					treeNodes.add(iterator.next());
				}
				
				System.out.println("Failed: ");
				System.out.println("Actual  : " + treeNodes);
				System.out.println("Affected: " + nodes);
			}
		}
		
		
		
	}

	
	
	/*******************************************************************************************************
	 * 
	 * @author Thomas Naeff (github.com/thnaeff)
	 *
	 */
	private class ListTreeNodeListener implements TreeNodeListener<ListTreeNode<String>> {
		public List<ListTreeNode<String>> affectedNodes = new ArrayList<ListTreeNode<String>>();
		@Override
		public void childNodeRemoved(TreeNodeEvent<ListTreeNode<String>> e) {
			System.out.println(this.getClass().getSimpleName() + "> " + e.getSourceNode() + " says: childNodeRemoved=" + e.getNode() + ", parent=" + e.getParentNode() + ", parent=" + e.getParentNode() + ", index=" + e.getNodeIndex());
			affectedNodes.add(e.getNode());
			e.getSourceNode().removeTreeNodeListener(listTreeNodeListener);
		}
		@Override
		public void childNodeAdded(TreeNodeEvent<ListTreeNode<String>> e) {
			System.out.println(this.getClass().getSimpleName() + "> " + e.getSourceNode() + " says: childNodeAdded=" + e.getNode() + ", parent=" + e.getParentNode() + ", index=" + e.getNodeIndex());
			affectedNodes.add(e.getNode());
			e.getNode().addTreeNodeListener(listTreeNodeListener);
		}
		@Override
		public void nodeValueChanged(TreeNodeEvent<ListTreeNode<String>> e) {
			System.out.println(this.getClass().getSimpleName() + "> " + e.getSourceNode() + " says: nodeValueChanged=" + e.getNode() + ", parent=" + e.getParentNode() + ", index=" + e.getNodeIndex());
			affectedNodes.add(e.getNode());
		}
		@Override
		public void addedToTree(TreeNodeEvent<ListTreeNode<String>> e) {
			System.out.println(this.getClass().getSimpleName() + "> " + e.getSourceNode() + " says: addedToTree=" + e.getNode() + ", parent=" + e.getParentNode() + ", index=" + e.getNodeIndex());
			affectedNodes.add(e.getNode());
		}
		@Override
		public void removedFromTree(TreeNodeEvent<ListTreeNode<String>> e) {
			System.out.println(this.getClass().getSimpleName() + "> " + e.getSourceNode() + " says: removedFromTree=" + e.getNode() + ", parent=" + e.getParentNode() + ", index=" + e.getNodeIndex());
			affectedNodes.add(e.getNode());
		}
	}
	
	
	/*******************************************************************************************************
	 * 
	 * @author Thomas Naeff (github.com/thnaeff)
	 *
	 */
	private class SetTreeNodeListener implements TreeNodeListener<SetTreeNode<String>> {
		public List<SetTreeNode<String>> affectedNodes = new ArrayList<SetTreeNode<String>>();
		@Override
		public void childNodeRemoved(TreeNodeEvent<SetTreeNode<String>> e) {
			System.out.println(this.getClass().getSimpleName() + "> " + e.getSourceNode() + " says: childNodeRemoved=" + e.getNode() + ", parent=" + e.getParentNode() + ", index=" + e.getNodeIndex());
			affectedNodes.add(e.getNode());
			e.getSourceNode().removeTreeNodeListener(setTreeNodeListener);
		}
		@Override
		public void childNodeAdded(TreeNodeEvent<SetTreeNode<String>> e) {
			System.out.println(this.getClass().getSimpleName() + "> " + e.getSourceNode() + " says: childNodeAdded=" + e.getNode() + ", parent=" + e.getParentNode() + ", index=" + e.getNodeIndex());
			affectedNodes.add(e.getNode());
			e.getNode().addTreeNodeListener(setTreeNodeListener);
		}
		@Override
		public void nodeValueChanged(TreeNodeEvent<SetTreeNode<String>> e) {
			System.out.println(this.getClass().getSimpleName() + "> " + e.getSourceNode() + " says: nodeValueChanged=" + e.getNode() + ", parent=" + e.getParentNode() + ", index=" + e.getNodeIndex());
			affectedNodes.add(e.getNode());
		}
		@Override
		public void addedToTree(TreeNodeEvent<SetTreeNode<String>> e) {
			System.out.println(this.getClass().getSimpleName() + "> " + e.getSourceNode() + " says: addedToTree=" + e.getNode() + ", parent=" + e.getParentNode() + ", index=" + e.getNodeIndex());
			affectedNodes.add(e.getNode());
		}
		@Override
		public void removedFromTree(TreeNodeEvent<SetTreeNode<String>> e) {
			System.out.println(this.getClass().getSimpleName() + "> " + e.getSourceNode() + " says: removedFromTree=" + e.getNode() + ", parent=" + e.getParentNode() + ", index=" + e.getNodeIndex());
			affectedNodes.add(e.getNode());
		}
	}
	
	/*******************************************************************************************************
	 * 
	 * @author Thomas Naeff (github.com/thnaeff)
	 *
	 */
	private class KeyListTreeNodeListener implements TreeNodeListener<KeyListTreeNode<String, String>> {
		public List<KeyListTreeNode<String, String>> affectedNodes = new ArrayList<KeyListTreeNode<String, String>>();
		@Override
		public void childNodeRemoved(TreeNodeEvent<KeyListTreeNode<String, String>> e) {
			System.out.println(this.getClass().getSimpleName() + "> " + e.getSourceNode() + " says: childNodeRemoved=" + e.getNode() + ", parent=" + e.getParentNode() + ", index=" + e.getNodeIndex());
			affectedNodes.add(e.getNode());
			e.getSourceNode().removeTreeNodeListener(keyListTreeNodeListener);
		}
		@Override
		public void childNodeAdded(TreeNodeEvent<KeyListTreeNode<String, String>> e) {
			System.out.println(this.getClass().getSimpleName() + "> " + e.getSourceNode() + " says: childNodeAdded=" + e.getNode() + ", parent=" + e.getParentNode() + ", index=" + e.getNodeIndex());
			affectedNodes.add(e.getNode());
			e.getNode().addTreeNodeListener(keyListTreeNodeListener);
		}
		@Override
		public void nodeValueChanged(TreeNodeEvent<KeyListTreeNode<String, String>> e) {
			System.out.println(this.getClass().getSimpleName() + "> " + e.getSourceNode() + " says: nodeValueChanged=" + e.getNode() + ", parent=" + e.getParentNode() + ", index=" + e.getNodeIndex());
			affectedNodes.add(e.getNode());
		}
		@Override
		public void addedToTree(TreeNodeEvent<KeyListTreeNode<String, String>> e) {
			System.out.println(this.getClass().getSimpleName() + "> " + e.getSourceNode() + " says: addedToTree=" + e.getNode() + ", parent=" + e.getParentNode() + ", index=" + e.getNodeIndex());
			affectedNodes.add(e.getNode());
		}
		@Override
		public void removedFromTree(TreeNodeEvent<KeyListTreeNode<String, String>> e) {
			System.out.println(this.getClass().getSimpleName() + "> " + e.getSourceNode() + " says: removedFromTree=" + e.getNode() + ", parent=" + e.getParentNode() + ", index=" + e.getNodeIndex());
			affectedNodes.add(e.getNode());
		}
	}
	
	/*******************************************************************************************************
	 * 
	 * @author Thomas Naeff (github.com/thnaeff)
	 *
	 */
	private class KeySetTreeNodeListener implements TreeNodeListener<KeySetTreeNode<String, String>> {
		public List<KeySetTreeNode<String, String>> affectedNodes = new ArrayList<KeySetTreeNode<String, String>>();
		@Override
		public void childNodeRemoved(TreeNodeEvent<KeySetTreeNode<String, String>> e) {
			System.out.println(this.getClass().getSimpleName() + "> " + e.getSourceNode() + " says: childNodeRemoved=" + e.getNode() + ", parent=" + e.getParentNode() + ", index=" + e.getNodeIndex());
			affectedNodes.add(e.getNode());
			e.getSourceNode().removeTreeNodeListener(keySetTreeNodeListener);
		}
		@Override
		public void childNodeAdded(TreeNodeEvent<KeySetTreeNode<String, String>> e) {
			System.out.println(this.getClass().getSimpleName() + "> " + e.getSourceNode() + " says: childNodeAdded=" + e.getNode() + ", parent=" + e.getParentNode() + ", index=" + e.getNodeIndex());
			affectedNodes.add(e.getNode());
			e.getNode().addTreeNodeListener(keySetTreeNodeListener);
		}
		@Override
		public void nodeValueChanged(TreeNodeEvent<KeySetTreeNode<String, String>> e) {
			System.out.println(this.getClass().getSimpleName() + "> " + e.getSourceNode() + " says: nodeValueChanged=" + e.getNode() + ", parent=" + e.getParentNode() + ", index=" + e.getNodeIndex());
			affectedNodes.add(e.getNode());
		}
		@Override
		public void addedToTree(TreeNodeEvent<KeySetTreeNode<String, String>> e) {
			System.out.println(this.getClass().getSimpleName() + "> " + e.getSourceNode() + " says: addedToTree=" + e.getNode() + ", parent=" + e.getParentNode() + ", index=" + e.getNodeIndex());
			affectedNodes.add(e.getNode());
		}
		@Override
		public void removedFromTree(TreeNodeEvent<KeySetTreeNode<String, String>> e) {
			System.out.println(this.getClass().getSimpleName() + "> " + e.getSourceNode() + " says: removedFromTree=" + e.getNode() + ", parent=" + e.getParentNode() + ", index=" + e.getNodeIndex());
			affectedNodes.add(e.getNode());
		}
	}
	

}
