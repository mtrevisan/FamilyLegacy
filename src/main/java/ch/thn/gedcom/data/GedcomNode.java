/**
 * Copyright 2013 Thomas Naeff (github.com/thnaeff)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.thn.gedcom.data;

import ch.thn.datatree.onoff.core.GenericOnOffKeySetTreeNode;
import ch.thn.datatree.onoff.core.OnOffTreeNodeModifier;
import ch.thn.gedcom.GedcomFormatter;
import ch.thn.gedcom.store.GedcomStoreBlock;
import ch.thn.gedcom.store.GedcomStoreLine;
import ch.thn.gedcom.store.GedcomStoreStructure;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;


/**
 *
 *
 *
 * @author Thomas Naeff (github.com/thnaeff)
 *
 */
public class GedcomNode extends GenericOnOffKeySetTreeNode<NodeKey, GedcomLine, GedcomNode>{

	/** The delimiter for multiple step values used in {@link #followPath(String...)} **/
	public static final String PATH_OPTION_DELIMITER = ";";

	/** Create all the available lines automatically */
	public static final int ADD_ALL = 0;
	/** Only create mandatory lines automatically */
	public static final int ADD_MANDATORY = 1;
	/** Do not create any lines automatically */
	public static final int ADD_NONE = 2;

	private static final NodeKeyComparator nodeKeyComparator = new NodeKeyComparator();
	private static final NodeValueComparator nodeValueComparator = new NodeValueComparator();

	/** The tag->NodeKey links */
	private HashMap<String, NodeKey> nodeKeys = null;

	private NodeKey nullNodeKey = null;

	/**The block with the information about any child lines*/
	private GedcomStoreBlock storeBlock = null;
	/**The line which contains this nodes gedcom grammar information*/
	private GedcomStoreLine storeLine = null;

	private String tagOrStructureName = null;
	private String tag = null;

	private boolean lookForXRefAndValueVariation = false;
	private boolean withXRef = false;
	private boolean withValue = false;

	/**
	 * Creates a new {@link GedcomNode} with the given information. The new node
	 * has to be available in the given store block.
	 *
	 * @param key
	 * @param storeBlock
	 * @param tagOrStructureName
	 * @param tag
	 * @param lookForXRefAndValueVariation
	 * @param withXRef
	 * @param withValue
	 */
	protected GedcomNode(NodeKey key, GedcomStoreBlock storeBlock, String tagOrStructureName, String tag, boolean lookForXRefAndValueVariation, boolean withXRef, boolean withValue){
		this(key, (GedcomLine)null);
		//		this(new NodeKey(tagOrStructureName, storeBlock.getStoreLine(tagOrStructureName)), null);

		this.tagOrStructureName = tagOrStructureName;
		this.tag = tag;
		this.lookForXRefAndValueVariation = lookForXRefAndValueVariation;
		this.withXRef = withXRef;
		this.withValue = withValue;

		if(! storeBlock.hasStoreLine(tagOrStructureName)){
			//Line with that tag or structure name does not exist in the given block
			String s = "";

			if(storeBlock.getParentStoreLine() == null){
				s = "Structure " + storeBlock.getStoreStructure().getStructureName();
			}
			else{
				s = "Store block " + storeBlock.getParentStoreLine().getId();
			}

			throw new GedcomCreationError(s + " does not have a tag " + tagOrStructureName + ". Available tags: " + GedcomFormatter.makeOrList(storeBlock.getAllLineIDs(), null, null));
		}

		storeLine = storeBlock.getStoreLine(tagOrStructureName);

		if(storeLine.hasStructureName()){
			//It is a structure line, thus it does not have a child block but it
			//is only a "link" to the structure
			this.storeBlock = storeBlock.getStoreStructure().getStore().getGedcomStructure(tagOrStructureName, tag, lookForXRefAndValueVariation, withXRef, withValue).getStoreBlock();
		}
		else{
			this.storeBlock = storeLine.getChildBlock();
		}

		if(storeLine.hasStructureName()){
			setNodeValue(new GedcomStructureLine(storeLine, tag));
		}
		else{
			setNodeValue(new GedcomTagLine(storeLine, tagOrStructureName));
		}

	}

	/**
	 * Only called if a GedcomTree is created, which is the head of a gedcom structure
	 *
	 * @param key
	 * @param storeStructure
	 */
	protected GedcomNode(NodeKey key, GedcomStoreStructure storeStructure){
		this(key, (GedcomLine)null);
		//		this(new NodeKey(storeStructure.getStoreBlock().getStoreStructure().getStructureName(), null), null);
		this.storeBlock = storeStructure.getStoreBlock();
		this.tagOrStructureName = storeStructure.getStructureName();
	}

	/**
	 *
	 *
	 * @param comparator
	 * @param key
	 * @param value
	 */
	private GedcomNode(NodeKey key, GedcomLine value){
		super(nodeKeyComparator, nodeValueComparator, key, value);

		nodeKeys = new HashMap<>();

		//A null node key. Since the used map of the tree node is a TreeMultiMap,
		//it does not accept null keys and fails with an NPE when looking up
		//null keys. Thus, this NodeKey is always returned for a null tag or
		//structure name
		nullNodeKey = new NodeKey("", - 1);

	}

	@Override
	public GedcomNode nodeFactory(GedcomLine value){
		return new GedcomNode(null, value);
	}

	@Override
	public GedcomNode nodeFactory(GedcomNode node){
		//!!!
		//This nodeFactory method is called when the OnOff tree is converted
		//into a simple tree (without invisible structures) from OnOffTreeUtil.
		//It should not be called from anywhere else since it adjusts the node key
		//ordering
		//!!!
		node.getNodeKey().setAsSimpleTreeKey(node);
		return new GedcomNode(node.getNodeKey(), node.getNodeValue());
	}

	@Override
	public GedcomNode nodeFactory(NodeKey key, GedcomLine value){
		return new GedcomNode(key, value);
	}

	@Override
	protected GedcomNode internalGetThis(){
		return this;
	}

	@Override
	public GedcomTree getHeadNode(){
		return (GedcomTree)super.getHeadNode();
	}

	/**
	 *
	 *
	 * @param tagOrStructureName
	 * @return
	 */
	private NodeKey getNodeKey(String tagOrStructureName){
		if(tagOrStructureName == null){
			return nullNodeKey;
		}

		if(! nodeKeys.containsKey(tagOrStructureName)){
			NodeKey newNodeKey = new NodeKey(tagOrStructureName, storeBlock.getStoreLine(tagOrStructureName));
			nodeKeys.put(tagOrStructureName, newNodeKey);
			return newNodeKey;
		}

		return nodeKeys.get(tagOrStructureName);
	}

	/**
	 *
	 *
	 * @return
	 */
	public GedcomNode newLine(){
		if(isRootNode()){
			//The head node is the starting point of the tree
			return null;
		}

		//Add child node to the parent node, using the parameters of this node
		if(lookForXRefAndValueVariation){
			return getParentNode().addChildLine(tagOrStructureName, tag, withXRef, withValue);
		}
		else{
			return getParentNode().addChildLine(tagOrStructureName, tag);
		}
	}

	/**
	 *
	 *
	 * @param tagOrStructureName
	 * @return
	 */
	public GedcomNode addChildLine(String tagOrStructureName){
		return addChildLine(tagOrStructureName, null, false, false, false);
	}

	/**
	 *
	 *
	 * @param tagOrStructureName
	 * @param tag
	 * @return
	 */
	public GedcomNode addChildLine(String tagOrStructureName, String tag){
		return addChildLine(tagOrStructureName, tag, false, false, false);
	}

	/**
	 *
	 *
	 * @param tagOrStructureName
	 * @param tag
	 * @param withXRef
	 * @param withValue
	 * @return
	 */
	public GedcomNode addChildLine(String tagOrStructureName, String tag, boolean withXRef, boolean withValue){
		return addChildLine(tagOrStructureName, tag, true, withXRef, withValue);
	}

	/**
	 *
	 *
	 * @param tagOrStructureName
	 * @param tag
	 * @param lookForXRefAndValueVariation
	 * @param withXRef
	 * @param withValue
	 * @return
	 */
	private GedcomNode addChildLine(String tagOrStructureName, String tag, boolean lookForXRefAndValueVariation, boolean withXRef, boolean withValue){

		if(maxNumberOfLinesReached(tagOrStructureName)){
			return null;
		}

		GedcomNode newNode = new GedcomNode(getNodeKey(tagOrStructureName), storeBlock, tagOrStructureName, tag, lookForXRefAndValueVariation, withXRef, withValue);

		addChildNode(newNode);

		return newNode;
	}

	/**
	 * Returns the child node with the given tag or structure name. Since there
	 * can be more than one line with the same tag or structure name, its line number
	 * has to be given.
	 *
	 * @param tagOrStructureName
	 * @param index
	 * @return
	 */
	public GedcomNode getChildLine(String tagOrStructureName, int lineNumber){
		if(! hasChildNodes(getNodeKey(tagOrStructureName)) || getChildNodesCount(getNodeKey(tagOrStructureName)) <= lineNumber){
			return null;
		}

		//Since the child nodes are stored in a set, it has to be iterated to the
		//given line number
		Iterator<GedcomNode> childNodes = getChildNodes(getNodeKey(tagOrStructureName)).iterator();
		int count = 0;
		while(childNodes.hasNext()){
			if(count != lineNumber){
				childNodes.next();
				count++;
			}
			else{
				return childNodes.next();
			}
		}

		//Line number not found
		return null;
	}

	/**
	 * Returns the child node with the given structure name and tag variation
	 *
	 * @param structureName
	 * @param tag
	 * @param lineNumber
	 * @return
	 */
	public GedcomNode getChildLine(String structureName, String tag, int lineNumber){
		return getChildLine(structureName, tag, false, false, false, lineNumber);
	}

	/**
	 * Returns the child node with the given structure name and variation
	 *
	 * @param structureName
	 * @param tag
	 * @param withXRef
	 * @param withValue
	 * @param lineNumber
	 * @return
	 */
	public GedcomNode getChildLine(String structureName, String tag, boolean withXRef, boolean withValue, int lineNumber){
		return getChildLine(structureName, tag, true, withXRef, withValue, lineNumber);
	}

	/**
	 *
	 *
	 * @param structureName
	 * @param tag
	 * @param lookForXRefAndValueVariation
	 * @param withXRef
	 * @param withValue
	 * @param lineNumber
	 * @return
	 */
	private GedcomNode getChildLine(String structureName, String tag, boolean lookForXRefAndValueVariation, boolean withXRef, boolean withValue, int lineNumber){

		if(isLeafNode()){
			//Nothing to do
			return null;
		}

		return searchForNode(getChildNodes(getNodeKey(structureName)).iterator(), structureName, tag, lookForXRefAndValueVariation, withXRef, withValue, lineNumber);
	}

	/**
	 * Searches the matching node in the list of given nodes. It loops through the
	 * given nodes list and checks if they match the structure name, tag and value/xref
	 * variation
	 *
	 * @param nodes
	 * @param structureName
	 * @param tag
	 * @param lookForXRefAndValueVariation
	 * @param withXRef
	 * @param withValue
	 * @param lineNumber Returns the line with the given line number. If <code>lineNumber</code>
	 * is -1, the first line which matches the given parameters will be returned
	 * @return
	 */
	private GedcomNode searchForNode(Iterator<GedcomNode> nodeIterator, String structureName, String tag, boolean lookForXRefAndValueVariation, boolean withXRef, boolean withValue, int lineNumber){
		int lineIndexCount = - 1;

		//Search for the child node which matches the parameters
		while(nodeIterator.hasNext()){
			GedcomNode node = nodeIterator.next();

			if(structureName.equals(node.getTagOrStructureName()) && tag.equals(node.getTag())){

				if(lookForXRefAndValueVariation){
					if(withXRef == node.getWithXRef() && withValue == node.getWithValue()){
						lineIndexCount++;
					}
				}
				else{
					lineIndexCount++;
				}
			}

			if(lineNumber != - 1){
				if(lineIndexCount == lineNumber){
					return node;
				}
			}
			else{
				return node;
			}
		}

		return null;
	}

	/**
	 * Counts the matching nodes in the list of given nodes. It loops through the
	 * given nodes list and checks if they match the structure name, tag and value/xref
	 * variation
	 *
	 * @param nodes
	 * @param structureName
	 * @param tag
	 * @param lookForXRefAndValueVariation
	 * @param withXRef
	 * @param withValue
	 * @return The number of nodes which match the given parameters, or -1 if
	 * no matching node has been found
	 */
	private int countNodes(Iterator<GedcomNode> nodeIterator, String structureName, String tag, boolean lookForXRefAndValueVariation, boolean withXRef, boolean withValue){
		int matchCount = 0;

		//Search for the child node which matches the parameters
		while(nodeIterator.hasNext()){
			GedcomNode node = nodeIterator.next();

			if(structureName.equals(node.getTagOrStructureName()) && tag.equals(node.getTag())){

				if(lookForXRefAndValueVariation){
					if(withXRef == node.getWithXRef() && withValue == node.getWithValue()){
						matchCount++;
					}
				}
				else{
					matchCount++;
				}
			}

		}

		return matchCount;
	}

	/**
	 * Removes all the child lines from the structure of this line
	 *
	 */
	public void removeAllChildLines(){
		removeChildNodes();
	}

	/**
	 * Removes this line from the structure
	 *
	 * @return
	 */
	public boolean removeLine(){
		return removeNode();
	}

	/**
	 * Removes this line from the structure.
	 *
	 * @param branchCleanup If set to <code>true</code>, {@link #branchCleanup()} is
	 * automatically executed on the parent line to remove all empty and unused
	 * parent lines.
	 * @return
	 */
	public boolean removeLine(boolean branchCleanup){
		GedcomNode parent = getParentNode();

		if(parent == null){
			//Nothing to do
			return true;
		}

		boolean ret = removeLine();

		if(branchCleanup){
			parent.branchCleanup();
		}

		return ret;
	}

	/**
	 * Replaces this structure node with the give node
	 *
	 */
	public GedcomNode replace(GedcomNode replacementNode){
		replaceNode(replacementNode);
		return replacementNode;
	}

	/**
	 * Returns true if this line has child lines
	 *
	 * @return
	 */
	public boolean hasChildLines(){
		return ! isLeafNode();
	}

	/**
	 * Returns the number of child lines of this line
	 *
	 * @return
	 */
	public int getNumberOfChildLines(){
		return getChildNodesCount();
	}

	/**
	 * Returns the number of lines which have the given tag or structure name.
	 * If there are different structure variations for one name, they are all
	 * counted together.
	 *
	 * @param tagOrStructureName
	 * @return
	 */
	public int getNumberOfChildLines(String tagOrStructureName){
		return getChildNodesCount(getNodeKey(tagOrStructureName));
	}

	/**
	 * Counts the number of structures with the given tag variation
	 *
	 * @param structureName
	 * @param tag
	 * @return
	 */
	public int getNumberOfChildLines(String structureName, String tag){
		if(tag == null){
			return getNumberOfChildLines(structureName);
		}

		if(isLeafNode()){
			//Nothing to do
			return 0;
		}

		return countNodes(getChildNodes(getNodeKey(structureName)).iterator(), structureName, tag, false, false, false);
	}

	/**
	 * Counts the number of structures with the given tag, xref and value variation
	 *
	 * @param structureName
	 * @param tag
	 * @param withXRef
	 * @param withValue
	 * @return
	 */
	public int getNumberOfChildLines(String structureName, String tag, boolean withXRef, boolean withValue){
		if(tag == null){
			return getNumberOfChildLines(structureName);
		}

		if(isLeafNode()){
			//Nothing to do
			return 0;
		}

		return countNodes(getChildNodes(getNodeKey(structureName)).iterator(), structureName, tag, true, withXRef, withValue);
	}

	/**
	 * Checks if a child line with the given tag or structure name exists
	 *
	 * @param tagOrStructureName
	 * @return
	 */
	public boolean hasChildLine(String tagOrStructureName){
		return hasChildNodes(getNodeKey(tagOrStructureName));
	}

	/**
	 * Checks if a child line with the given structure name and tag variation exists
	 *
	 * @param structureName
	 * @param tag
	 * @return
	 */
	public boolean hasChildLine(String structureName, String tag){
		return getChildLine(structureName, tag, false, false, false, - 1) != null;
	}

	/**
	 * Checks if a child line with the given structure name and tag/xref/value
	 * variation exists
	 *
	 * @param structureName
	 * @param tag
	 * @param withXRef
	 * @param withValue
	 * @return
	 */
	public boolean hasChildLine(String structureName, String tag, boolean withXRef, boolean withValue){
		return getChildLine(structureName, tag, true, withXRef, withValue, - 1) != null;
	}

	/**
	 * Returns the key (tag or structure name) of the parent node
	 *
	 * @return
	 */
	public String getParentLineKey(){
		if(! isRootNode()){
			return getParentNode().getNodeKey().getKey();
		}
		else{
			return null;
		}
	}

	/**
	 * Sets the value of this node
	 *
	 * @param value
	 * @return <code>null</code> if setting the value failed
	 */
	public GedcomNode setTagLineValue(String value){
		if(! getNodeValue().isTagLine()){
			return null;
		}

		getNodeValue().getAsTagLine().setValue(value);
		return this;
	}

	/**
	 * Returns the value of the line at this node
	 *
	 * @return
	 */
	public String getTagLineValue(){
		return getNodeValue().getAsTagLine().getValue();
	}

	/**
	 * Sets the xref of this node
	 *
	 * @param xref
	 * @return
	 */
	public GedcomNode setTagLineXRef(String xref){
		if(! getNodeValue().isTagLine()){
			return null;
		}

		getNodeValue().getAsTagLine().setXRef(xref);
		return this;
	}

	/**
	 * Returns the xref of the line at this node
	 *
	 * @return
	 */
	public String getTagLineXRef(){
		return getNodeValue().getAsTagLine().getXRef();
	}

	/**
	 * Checks if there is a child line with the given value
	 *
	 * @param tagName
	 * @param value
	 * @return
	 */
	public boolean hasLineWithValue(String value){
		Iterator<GedcomNode> iterator = getChildNodes().iterator();

		while(iterator.hasNext()){
			GedcomLine line = iterator.next().getNodeValue();
			if(line.isTagLine() && line.getAsTagLine().getValue() != null && line.getAsTagLine().getValue().equals(value)){
				return true;
			}
		}

		return false;
	}

	/**
	 * Checks if there is a child line with the given xref
	 *
	 * @param tagName
	 * @param xref
	 * @return
	 */
	public boolean hasLineWithXRef(String xref){
		Iterator<GedcomNode> iterator = getChildNodes().iterator();

		while(iterator.hasNext()){
			GedcomLine line = iterator.next().getNodeValue();
			if(line.isTagLine() && line.getAsTagLine().getXRef() != null && line.getAsTagLine().getXRef().equals(xref)){
				return true;
			}
		}

		return false;
	}

	/**
	 * This method checks if a new line with the given tag or structure name
	 * can be added to this block. The method does not do any extended checks, like
	 * if the maximum number of the line is already reached. It only checks if
	 * there is such a line defined for this block in the lineage-linked grammar
	 *
	 * @param tagOrStructureName The tag or structure name to look for
	 * @return True if such a line can be added
	 */
	public boolean canAddLine(String tagOrStructureName){
		//-> multiple variations do not matter, because the min/max values apply
		//for all variations of a structure
		return storeBlock.hasStoreLine(tagOrStructureName);
	}

	/**
	 * This method returns the maximum number of lines allowed for the line
	 * with the given tag or structure name.<br>
	 * Since all the variations have the
	 * same min/max limits, there is no need for specifying the variation and
	 * only the tag or structure name is enough.
	 *
	 * @param tagOrStructureName
	 * @return Returns the maximum number or allowed lines or 0 if there is no
	 * maximum defined. -1 is returned if there is no line with the given tag
	 * or structure line available for this block.
	 */
	public int maxNumberOfLines(String tagOrStructureName){
		//It does not need the tag for multiple variations, since all the
		//variations are the same

		if(! storeBlock.hasStoreLine(tagOrStructureName)){
			return - 1;
		}

		return storeBlock.getStoreLine(tagOrStructureName).getMax();
	}

	/**
	 * This method checks if the maximum number of lines of the line with the given
	 * tag or structure name has been reached already
	 *
	 * @param tagOrStructureName
	 * @return
	 */
	public boolean maxNumberOfLinesReached(String tagOrStructureName){
		if(! hasChildNodes(getNodeKey(tagOrStructureName))){
			return false;
		}

		int lineCount = getChildNodesCount(getNodeKey(tagOrStructureName));
		int max = maxNumberOfLines(tagOrStructureName);

		return max != 0 && lineCount >= max;
	}

	/**
	 * Returns the store block which holds the all the information about the
	 * possible child lines of this node
	 *
	 * @return
	 */
	public GedcomStoreBlock getStoreBlock(){
		return storeBlock;
	}

	/**
	 * Returns the store line of which this line is derived from. The store line
	 * holds all the information about this node. There is no store line if this
	 * node is the head of a gedcom structure.
	 *
	 * @return
	 */
	public GedcomStoreLine getStoreLine(){
		return storeLine;
	}

	/**
	 * Returns the structure object of which this structure is derived from. The
	 * store structure is the head of a gedcom structure tree.
	 *
	 * @return
	 */
	public GedcomStoreStructure getStoreStructure(){
		return storeBlock.getStoreStructure();
	}

	/**
	 * This method checks if the given name is a structure name which is defined
	 * for this block. If this method returns true, it should be possible to
	 * add a structure line with the given name.
	 *
	 * @param name The name to check
	 * @return Returns true if the given name is a structure name, or false if it is not
	 * a structure name or if it does not exist.
	 */
	public boolean nameIsPossibleStructure(String name){
		if(! getStoreBlock().hasStoreLine(name)){
			return false;
		}

		return getStoreBlock().getStoreLine(name).hasStructureName();
	}

	/**
	 * This method checks if the given name is a tag name which is defined
	 * for this block. If this method returns true, it should be possible to
	 * add a tag line with the given name.
	 *
	 * @param name The name to check
	 * @return Returns true if the given name is a tag name, or false if it is not
	 * a tag name or if it does not exist.
	 */
	public boolean nameIsPossibleTag(String name){
		if(! getStoreBlock().hasStoreLine(name)){
			return false;
		}

		return getStoreBlock().getStoreLine(name).hasTags();
	}

	/**
	 * Returns true if the current object can have child lines as defined in the
	 * lineage linked grammar
	 *
	 * @return
	 */
	public boolean canHaveChildren(){
		return getStoreBlock().hasChildLines();
	}

	/**
	 * Returns the minimum number of lines of the type of this line which are
	 * required in one block
	 *
	 * @return
	 */
	public int getMinNumberOfLines(){
		return storeLine.getMin();
	}

	/**
	 * Returns the maximum number of lines of the type of this line which are
	 * allowed in one block. A returned number of 0 indicates that there is
	 * not maximum limit (given as M in the lineage linked grammar).
	 *
	 * @return
	 */
	public int getMaxNumberOfLines(){
		return storeLine.getMax();
	}

	/**
	 * Adds all the lines which are available for this node. However, some
	 * structures which have multiple variations can not be added automatically
	 * because the user has to make the decision which variation should be added.
	 *
	 * @param recursive If this flag is set to <code>true</code>, all child lines
	 * are also added for each added line -> the whole tree is built.
	 */
	public void addAllChildLines(boolean recursive){
		if(storeBlock == null){
			return;
		}

		LinkedList<GedcomStoreLine> allLines = storeBlock.getStoreLines();
		for(GedcomStoreLine line : allLines){
			try{
				if(recursive){
					GedcomNode node = addChildLine(line);

					if(node != null){
						node.addAllChildLines(recursive);
					}
				}
				else{
					addChildLine(line);
				}
			}
			catch(GedcomCreationError e){
				//No warnings when adding all the lines
				//				e.printStackTrace();
			}
		}
	}

	/**
	 * Adds all the lines which are defined as mandatory in the gedcom lineage-linked
	 * grammar for this node. However, some structures which have multiple variations
	 * can not be added automatically because the user has to make the decision
	 * which variation should be added.
	 *
	 * @param recursive If this flag is set to <code>true</code>, all mandatory child lines
	 * are also added for each added line -> the whole tree is built with mandatory lines.
	 */
	public void addMandatoryChildLines(boolean recursive){
		if(storeBlock == null){
			return;
		}

		LinkedList<GedcomStoreLine> mandatoryLines = storeBlock.getMandatoryLines();
		for(GedcomStoreLine line : mandatoryLines){
			try{
				if(recursive){
					GedcomNode node = addChildLine(line);

					if(node != null){
						node.addMandatoryChildLines(recursive);
					}
				}
				else{
					addChildLine(line);
				}
			}
			catch(GedcomCreationError e){
				//No warnings when adding all the lines
				//				e.printStackTrace();
			}
		}
	}

	/**
	 * Tries to add the given store line. A given store line can not be added
	 * if it has multiple variations, because it can not make the decision
	 * which variation to use.
	 *
	 * @param line
	 * @return
	 */
	private GedcomNode addChildLine(GedcomStoreLine line){
		if(line.hasVariations()){
			throw new GedcomCreationError("Can not add child line " + line.getStructureName() + ". " + "Line has multiple variations.");
		}

		if(line.getTagNames().size() > 1){
			throw new GedcomCreationError("Can not add child line " + line.getTagNames() + ". " + "Line has multiple tags available.");
		}

		String tag = null;

		if(line.getTagNames().size() == 1){
			tag = line.getTagNames().iterator().next();
		}

		if(! line.hasStructureName()){
			return addChildLine(tag);
		}
		else{
			boolean lookForXRefAndValueVariation = line.hasVariations();
			return addChildLine(line.getStructureName(), tag, lookForXRefAndValueVariation, line.hasXRefNames(), line.hasValueNames());
		}
	}

	@Override
	public boolean isNodeIgnored(OnOffTreeNodeModifier modifier){
		//Do not print structure lines
		if(getNodeValue() != null && getNodeValue().isStructureLine()){
			return true;
		}

		return super.isNodeIgnored(modifier);
	}

	@Override
	public boolean isNodeHidden(OnOffTreeNodeModifier modifier){
		//Always print the head
		if(isRootNode()){
			return false;
		}

		return skipLinePrint(modifier, this, false, false);
	}

	/**
	 * Checks if the line has to be skipped. A line has to be skipped if value/xref
	 * are required but not set/empty (depending on the given flags). However,
	 * if there is a line in a lower level which has a value/xref, then this
	 * line has to be printed in order to print the line with value on the lower level.
	 *
	 * @param modifier
	 * @param printEmptyLines
	 * @param printLinesWithNoValueSet
	 * @return
	 */
	private boolean skipLinePrint(OnOffTreeNodeModifier modifier, GedcomNode node, boolean printEmptyLines, boolean printLinesWithNoValueSet){
		GedcomLine line = node.getNodeValue();
		boolean skip = false;

		if(line.isTagLine()){
			GedcomTagLine tagLine = line.getAsTagLine();

			//Never skip lines which do not have a value or xref field
			if(! tagLine.requiresValue() && ! tagLine.requiresXRef()){
				return false;
			}

			if(! printEmptyLines){
				//Skip empty lines which actually require a value
				//An empty value has been set
				if((tagLine.isValueSet() || tagLine.isXRefSet()) && tagLine.isEmpty()){
					skip = true;
				}
			}

			if(! printLinesWithNoValueSet){
				//Skip lines which have no value and xref set, but which require a value
				if(! tagLine.isValueSet() && ! tagLine.isXRefSet()){
					skip = true;
				}
			}
		}
		else{
			//Its a structure line
		}

		//Any child lines which shouldn't be skipped? If yes, then this line
		//should not be skipped because otherwise the child line with content
		//will not be printed. If all the child lines can be skipped, this line
		//does not need to be printed if skip has already been set to true

		if(skip && ! node.isLeafNode()){
			Iterator<GedcomNode> iterator = node.getChildNodes().iterator();

			while(iterator.hasNext()){
				GedcomNode childNode = iterator.next();

				if(forceNodeVisible(modifier)){
					//Do not skip, because a lower level node is forced to be printed
					return false;
				}

				if(! skipLinePrint(modifier, childNode, printEmptyLines, printLinesWithNoValueSet)){
					//A tag line found which should not be skipped
					return false;
				}
				else{
					//If it is a structure line, just pass the skip state
					//on to the caller (structure lines are not considered
					//when checking if lines have to be skipped or not)
					if(line.isStructureLine()){
						return true;
					}
				}

			}
		}

		return skip;
	}

	/**
	 * Tries to follow the given path. The path needs to exist in order to follow
	 * it.<br>
	 * <br>
	 * For more information about how to use the path array, read
	 * {@link #followPath(boolean, boolean, boolean, String...)}
	 *
	 * @param path The path to follow.
	 * @return The {@link GedcomNode} of the last object in the path, or <code>null</code> if
	 * following the path did not work.
	 * @see #followPath(boolean, boolean, boolean, String...)
	 */
	public GedcomNode followPath(String... path){
		return followPath(false, false, false, path);
	}

	/**
	 * Tries to follow the given path. If the path does not exist, it tries to
	 * create the path.<br>
	 * <br>
	 * For more information about how to use the path array, read
	 * {@link #followPath(boolean, boolean, boolean, String...)}
	 *
	 * @param path The path to follow.
	 * @return The {@link GedcomNode} of the last object in the path, or <code>null</code> if
	 * following the path did not work.
	 * @see #followPath(boolean, boolean, boolean, String...)
	 */
	public GedcomNode followPathCreate(String... path){
		return followPath(true, false, false, path);
	}

	/**
	 * Tries to follow the given path. If such a path already exists, a new path
	 * is created at the most end possible of the path. If no such path exists,
	 * it just tries to create the path.<br>
	 * <br>
	 * For more information about how to use the path array, read
	 * {@link #followPath(boolean, boolean, boolean, String...)}
	 *
	 * @param path The path to follow.
	 * @return The {@link GedcomNode} of the last object in the path, or <code>null</code> if
	 * following the path did not work.
	 * @see #followPath(boolean, boolean, boolean, String...)
	 */
	public GedcomNode createPathEnd(String... path){
		return followPath(false, true, false, path);
	}

	/**
	 * Tries to create the given path.<br>
	 * <br>
	 * For more information about how to use the path array, read
	 * {@link #followPath(boolean, boolean, boolean, String...)}
	 *
	 * @param path The path to create.
	 * @return The {@link GedcomNode} of the last object in the path, or <code>null</code> if
	 * following the path did not work.
	 * @see #followPath(boolean, boolean, boolean, String...)
	 */
	public GedcomNode createPath(String... path){
		return followPath(false, false, true, path);
	}

	/**
	 * Follows the path given with <code>path</code>. Each array position describes
	 * one path step, and each step can contain multiple values describing the
	 * step. The following lines each show one step in the path, (multiple values
	 * are separated by {@value #PATH_OPTION_DELIMITER}):<br>
	 * - "tag or structure name"<br>
	 * - "tag or structure name;line number"<br>
	 * - "structure name;tag"<br>
	 * - "structure name;tag;line number"<br>
	 * - "structure name;tag;with xref;with value;line number"<br>
	 * - "structure name;tag;with xref;with value"<br>
	 * ("with xref" and "with value" have to be given as "true" or "false")<br>
	 * <br>
	 * If multiple step values are given, they have to be separated with the
	 * {@link #PATH_OPTION_DELIMITER}. Multiple step values are needed if the
	 * next path step can not be identified with one step value only. A tag line
	 * for example can be added multiple times, thus when accessing that line, the
	 * tag and the line number have to be given. Also, some structures exist in
	 * different variations (with/without xref, with/without value, ...) and might
	 * have to be accessed with multiple values for one path step.<br>
	 * If a path can not be followed, this method throws an {@link GedcomPathAccessError}
	 * with an error text and the path which caused the error. The error text might
	 * give a hint to what has gone wrong.
	 *
	 * @param createNewIfNotExisting If set to <code>true</code>, it tries to
	 * create the path if it does not exist yet
	 * @param createNewEnd If set to <code>true</code>, it tries to create a new
	 * path at the very most end possible. This means that if no such path exists
	 * yet, the path is created and if such a path already exists, a new one is
	 * created.
	 * @param createPath Create the whole given path.
	 * @param path The path to follow.
	 * @return The {@link GedcomNode} of the last object in the path, or <code>null</code> if
	 * following the path did not work.
	 * @throws GedcomPathAccessError If a path piece can not be accessed because it
	 * does not exist
	 * @throws GedcomCreationError If new path pieces have to be created but they
	 * can not be created (because of invalid structure/tag names, or there is no
	 * node which can have another line added).
	 */
	private GedcomNode followPath(boolean createNewIfNotExisting, boolean createNewEnd, boolean createPath, String... path){

		if(path == null || path.length == 0){
			//Nothing to do
			return this;
		}

		GedcomNode currentNode = this;
		GedcomNode lastNodeWithSplitPossibility = null;

		int lastIndexWithSplitPossibility = - 1;
		int pathIndex = 0;

		for(; pathIndex < path.length; pathIndex++){
			PathStepPieces pp = new PathStepPieces();
			if(path[pathIndex] == null || path[pathIndex].length() == 0 || ! pp.parse(path[pathIndex])){
				//Nothing to do
				continue;
			}

			if((createNewEnd || createNewIfNotExisting) && ! currentNode.maxNumberOfLinesReached(pp.tagOrStructureName)){
				lastNodeWithSplitPossibility = currentNode;
				lastIndexWithSplitPossibility = pathIndex;
			}

			GedcomNode lastNode = currentNode;

			if(createPath){
				if(currentNode.maxNumberOfLinesReached(pp.tagOrStructureName)){
					throw new GedcomPathCreationError(path, pathIndex, "Can not add another path '" + path[pathIndex] + "' as child of '" + lastNode.getNodeKey() + "'. " + "Maximum number of lines reached.");
				}

				//Create path
				if(pp.tag == null){
					currentNode = currentNode.addChildLine(pp.tagOrStructureName);
				}
				else{
					currentNode = currentNode.addChildLine(pp.tagOrStructureName, pp.tag, pp.lookForXRefAndValueVariation, pp.withXRef, pp.withValue);
				}

				if(currentNode == null){
					throw new GedcomPathCreationError(path, pathIndex, "Can not create path '" + path[pathIndex] + "' as child of '" + lastNode.getNodeKey() + "'.");
				}
			}
			else{
				//Follow path
				if(pp.tag == null){
					currentNode = currentNode.getChildLine(pp.tagOrStructureName, pp.lineNumber);
				}
				else{
					currentNode = currentNode.getChildLine(pp.tagOrStructureName, pp.tag, pp.lookForXRefAndValueVariation, pp.withXRef, pp.withValue, pp.lineNumber);
				}

				if((createNewEnd || createPath || createNewIfNotExisting) && currentNode == null){
					//Failed to follow path
					break;
				}

				//Only show an error message if the path should be created
				if(currentNode == null){
					if(createPath){
						throw new GedcomPathAccessError(path, pathIndex, "Can not access path '" + path[pathIndex] + "' as child of '" + lastNode.getNodeKey() + "'.");
					}
					else{
						return null;
					}
				}
			}

		}

		if(! createPath && (createNewEnd || createNewIfNotExisting && currentNode == null)){
			//createNewEnd: Create a new end without caring if such a path is already
			//there or not.
			//createNewIfNotExisting: Following the path was not possible -> create it

			if(lastNodeWithSplitPossibility == null){
				throw new GedcomPathCreationError(path, lastIndexWithSplitPossibility, "Can not create a new path " + Arrays.toString(path) + " in " + this + ". The maximum number of lines has been reached.");
			}
			else{
				//Create new path, starting at the last possible split point
				String[] newPath = new String[path.length - lastIndexWithSplitPossibility];
				System.arraycopy(path, lastIndexWithSplitPossibility, newPath, 0, newPath.length);
				return lastNodeWithSplitPossibility.createPath(newPath);
			}
		}

		return currentNode;
	}

	/**
	 * Removes the very end node of the given path only.
	 *
	 * @param path
	 * @return
	 */
	public GedcomNode removePathEnd(String... path){
		return removePath(false, path);
	}

	/**
	 * Removes the very end node of the given path and does a branch cleanup (removes
	 * all unused parent branch parts). The cleanup travels the tree branch upwards
	 * and removes all empty branch parts until a node with children or a tag line
	 * node with value/xref is found.
	 *
	 * @param path
	 * @return
	 */
	public GedcomNode removePath(String... path){
		return removePath(true, path);
	}

	/**
	 * Removes the node which is at the very end of the given path.
	 *
	 * @param branchCleanup If set to <code>true</code>, after removing the path
	 * end node it travels the tree branch upwards and removes all empty branch
	 * parts until a node with children or a tag line node with value/xref is found.
	 * @param path The path to remove the end of
	 * @return The node which has been removed. If any empty parents are removed,
	 * it returns the top most removed parent.
	 */
	private GedcomNode removePath(boolean branchCleanup, String... path){

		GedcomNode node = followPath(path);

		if(node == null){
			return null;
		}

		if(! branchCleanup){
			//Remove the very end node only
			if(! node.removeLine()){
				return null;
			}

			return node;
		}
		else{
			//Get the parent node before removing the node because removing a
			//node clears its parent node
			GedcomNode parentNode = node.getParentNode();

			//Remove the very end node
			if(! node.removeLine()){
				return null;
			}

			return branchCleanup(parentNode);
		}

	}

	/**
	 * This only works on a end node of a branch. <code>null</code> is returned
	 * if the given node has child nodes.
	 * It travels upwards and removes all empty branch parts until a node with
	 * children or a tag line node with value/xref is found.
	 *
	 * @param node The node from which the cleanup should start
	 * @return If any empty parents are removed, it returns the top most removed parent.
	 */
	private GedcomNode branchCleanup(GedcomNode node){
		if(getNumberOfChildLines() > 1){
			return null;
		}

		GedcomNode parentNode = node;
		boolean firstCheck = true;

		while(parentNode != null){

			if(parentNode.getNumberOfChildLines() > 1 || parentNode.getNodeValue().isTagLine() && (parentNode.getNodeValue().getAsTagLine().requiresValue() || parentNode.getNodeValue().getAsTagLine().requiresXRef())){
				//Node with children or a tag line node found -> do not continue

				if(firstCheck){
					return null;
				}

				break;
			}

			firstCheck = false;
			node = parentNode;
			parentNode = parentNode.getParentNode();
		}

		if(! node.removeLine()){
			return null;
		}

		return node;
	}

	/**
	 * This only works on a end node of a branch. <code>null</code> is returned
	 * if the node on which it is executed has child nodes.
	 * It travels upwards and removes all empty branch parts until a node with
	 * children or a tag line node with value/xref is found.
	 *
	 * @return If any empty parents are removed, it returns the top most removed parent.
	 */
	public GedcomNode branchCleanup(){
		return branchCleanup(this);
	}

	/**
	 *
	 *
	 * @return tagOrStructureName
	 */
	protected String getTagOrStructureName(){
		return tagOrStructureName;
	}

	/**
	 *
	 *
	 * @return tag
	 */
	protected String getTag(){
		return tag;
	}

	/**
	 *
	 *
	 * @return lookForXRefAndValueVariation
	 */
	protected boolean getLookForXRefAndValueVariation(){
		return lookForXRefAndValueVariation;
	}

	/**
	 *
	 *
	 * @return withXRef
	 */
	protected boolean getWithXRef(){
		return withXRef;
	}

	/**
	 *
	 *
	 * @return withValue
	 */
	protected boolean getWithValue(){
		return withValue;
	}

	@Override
	public String toString(){
		if(getNodeValue() == null){
			if(tag != null){
				return tagOrStructureName + " (" + tag + ")";
			}
			else{
				return tagOrStructureName;
			}
		}
		else{
			return getNodeValue().toString();
		}
	}


	/**************************************************************************
	 * A class to parse one piece of the gedcom path and split it in its pieces
	 *
	 *
	 * @author Thomas Naeff (github.com/thnaeff)
	 *
	 */
	public class PathStepPieces{
		public String tagOrStructureName = null;
		public String tag = null;
		public boolean lookForXRefAndValueVariation = false;
		public boolean withXRef = false;
		public boolean withValue = false;
		public int lineNumber = 0;

		/**
		 * Parses the given path piece. The path piece has to use the delimiter
		 * from {@link PATH_OPTION_DELIMITER}
		 *
		 * @param pathPiece
		 * @return
		 */
		public boolean parse(String pathPiece){
			String[] pathPieceParts = pathPiece.split(PATH_OPTION_DELIMITER);

			if(pathPieceParts.length == 0){
				return false;
			}

			tagOrStructureName = pathPieceParts[0];

			//There is more than just a structure name or tag
			if(pathPieceParts.length > 1){
				try{
					lineNumber = Integer.parseInt(pathPieceParts[1]);
					//No variation
					return true;
				}
				catch(NumberFormatException e){
					//If it is not a line number, it must be a tag for the
					//variation
					tag = pathPieceParts[1];
				}

				//Continues if there is more than just the structure name and
				//variation tag
				if(pathPieceParts.length > 2){
					lookForXRefAndValueVariation = true;

					int lineNumberIncrease = 0;
					try{
						lineNumber = Integer.parseInt(pathPieceParts[2]);
						lineNumberIncrease = 1;
					}
					catch(NumberFormatException e){

					}

					if(pathPieceParts.length > 2 + lineNumberIncrease){
						withXRef = Boolean.parseBoolean(pathPieceParts[2 + lineNumberIncrease]);
					}

					if(pathPieceParts.length > 3 + lineNumberIncrease){
						withValue = Boolean.parseBoolean(pathPieceParts[3 + lineNumberIncrease]);
					}

					if(pathPieceParts.length > 4 + lineNumberIncrease){
						lineNumber = Integer.parseInt(pathPieceParts[4 + lineNumberIncrease]);
					}
				}
			}

			return true;
		}

		@Override
		public String toString(){
			return tagOrStructureName + ", " + tag + ", " + lookForXRefAndValueVariation + ", " + withXRef + ", " + withValue + ", " + lineNumber;
		}

	}


}
