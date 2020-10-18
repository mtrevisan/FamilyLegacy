package ch.thn.datatree;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;

import ch.thn.datatree.printer.TreeNodePlainTextPrinter;
import ch.thn.datatree.printer.generic.PlainTextTreePrinter;

public class TreeTest2 {

	
	@Test
	public void testUtilTreeCopy() throws Exception {
		System.out.println("\n ================= Tree copy =====================\n");
		
		KeyListTreeNode<String, String> keyListNodeReference = new KeyListTreeNode<String, String>("", "Tree");
		
		TreeTest.buildValueTree(keyListNodeReference);
		
		KeyListTreeNode<String, String> keyListNodeCopy = DataTreeUtil.copyTree(keyListNodeReference);
		
		TreeNodePlainTextPrinter printer = new TreeNodePlainTextPrinter();
		
		StringBuilder keyListNodeReferenceOut = printer.print(keyListNodeReference);
		StringBuilder keyListNodeCopyOut = printer.print(keyListNodeCopy);
		
		
		System.out.println("-- Key list tree reference -------------------------------------");
		System.out.println(keyListNodeReferenceOut);
		System.out.println("-- Key list tree copy -------------------------------------");
		System.out.println(keyListNodeCopyOut);
		
		//Check that the printed copy matches the printed reference
		assertThat(keyListNodeCopyOut.toString(), is(equalTo(keyListNodeReferenceOut.toString())));
		
	}
	
	@Test
	public void testCustomTreeCopy() throws Exception {
		
		System.out.println("\n ================= Tree copy custom tree =====================\n");

		
		MyOwnTree keyListNodeReference = new MyOwnTree("", "Tree");
		
		TreeTest.buildValueTree(keyListNodeReference);
		
		//See javadoc of MyOwnTree for information about the advantages of creating a simple tree implementation 
		//which extends the generic tree type rather than extending a tree implementation
		MyOwnTree keyListNodeCopy = DataTreeUtil.copyTree(keyListNodeReference);
		
		TreeNodePlainTextPrinter printer = new TreeNodePlainTextPrinter();
		
		StringBuilder keyListNodeReferenceOut = printer.print(keyListNodeReference);
		StringBuilder keyListNodeCopyOut = printer.print(keyListNodeCopy);
		
		
		System.out.println("-- Key list tree reference -------------------------------------");
		System.out.println(keyListNodeReferenceOut);
		System.out.println("-- Key list tree copy -------------------------------------");
		System.out.println(keyListNodeCopyOut);
		
		//Check that the printed copy matches the printed reference
		assertThat(keyListNodeCopyOut.toString(), is(equalTo(keyListNodeReferenceOut.toString())));
		
	}
	
	
	@Test
	public void multiKeyTest() throws Exception {
		
		System.out.println("\n ================= multiple keys =====================\n");
		
		KeyListTreeNode<String, String> keyListNode = new KeyListTreeNode<String, String>("", "Tree");
		KeySetTreeNode<String, String> keySetNode = new KeySetTreeNode<String, String>("", "Tree");
		
		//Same key multiple times
		
		keyListNode.addChildNode("key1", "Value1");
		keyListNode.addChildNode("key2", "Value2");
		//Key-value pair is added twice, because this tree is backed by a list
		keyListNode.addChildNode("key2", "Value2");
		
		
		keySetNode.addChildNode("key1", "Value1");
		keySetNode.addChildNode("key2", "Value2");
		//Same key-value combination. Because this tree is backed by a set, 
		//the default comparator will prevent it from being added
		keySetNode.addChildNode("key2", "Value2");
		
		
		
		TreeNodePlainTextPrinter printer = new TreeNodePlainTextPrinter();
		
		StringBuilder keyListNodeOut = printer.print(keyListNode);
		StringBuilder keySetNodeOut = printer.print(keySetNode);
		
		
		String referenceList = "Tree" + PlainTextTreePrinter.LINE_SEPARATOR 
				+ "├─ Value1" + PlainTextTreePrinter.LINE_SEPARATOR 
				+ "├─ Value2" + PlainTextTreePrinter.LINE_SEPARATOR
				+ "└─ Value2" + PlainTextTreePrinter.LINE_SEPARATOR;
		
		String referenceSet = "Tree" + PlainTextTreePrinter.LINE_SEPARATOR 
				+ "├─ Value1" + PlainTextTreePrinter.LINE_SEPARATOR 
				+ "└─ Value2" + PlainTextTreePrinter.LINE_SEPARATOR;
		
		
		System.out.println("-- Reference tree list -------------------------------------");
		System.out.println(referenceList);
		System.out.println("-- Reference tree set -------------------------------------");
		System.out.println(referenceSet);
		System.out.println("--- Key list tree ------------------------------------");
		System.out.println(keyListNodeOut);
		System.out.println("--- Key set tree ------------------------------------");
		System.out.println(keySetNodeOut);
		
		
		//Check that all the printed trees match the reference
		assertThat(keyListNodeOut.toString(), is(equalTo(referenceList)));
		assertThat(keySetNodeOut.toString(), is(equalTo(referenceSet)));
		
	}
	
}
