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
package io.github.mtrevisan.familylegacy.grammar;

import io.github.mtrevisan.familylegacy.grammar.exceptions.GedcomParseException;
import io.github.mtrevisan.familylegacy.services.RegexHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * A store block contains all the lines of the same level.
 * <p>Furthermore, it has a list of mandatory lines and a list which links all the line ID's (tag or structure names) to their line
 * objects. Each block also links back to its parent line and to the structure it is located in.<br>
 * <br>
 * The class {@link GedcomStoreStructure} has more information about the hierarchy of structures, blocks and lines.</p>
 */
class GedcomStoreBlock{

	private static final Pattern LEVEL_PATTERN = RegexHelper.pattern("^(n|\\+[1-9]|\\+[1-9][0-9]) ");


	/** All the lines of this block which are defined in the lineage-linked grammar in their parsing order. */
	private final List<GedcomStoreLine> storeLines = new ArrayList<>();
	/** A sublist of the {@link #storeLines} which only contains the mandatory lines. */
	private final List<GedcomStoreLine> mandatoryLines = new ArrayList<>();
	/**
	 * The line ID's (tag or structure names) linked to their lines.
	 * <p>If a line has multiple tag possibilities (like [ANUL|CENS|DIV|DIVF]), the line appears  multiple times, once for every tag.</p>
	 */
	private final Map<String, GedcomStoreLine> idToLineLinks = new HashMap<>();
	/** The line which this block is located under. */
//	private final StoreLine parentStoreLine;


	/**
	 * Parses the given block with all the lines.
	 */
	protected boolean parse(final List<String> block) throws GedcomParseException{
		/* Example:
		 * n TAG something
		 *   +1 TAG something
		 *   +1 TAG something
		 *
		 * n [TAG1 | TAG2] something
		 *
		 * n <<STRUCTURE_NAME>>
		 *
		 * ...
		 */

		if(block.isEmpty())
			//nothing to do, just continue
			return true;

		//gets the line index which is at the very beginning of a line
		//(the line index is either "n" or "+NUMBER", where NUMBER is 0-99)
		final String lineIndex = RegexHelper.getFirstMatching(block.get(0), LEVEL_PATTERN);
		if(lineIndex == null)
			//this error should already be captured by GedcomStore.parsingErrorCheck
			throw new GedcomParseException("On line '" + block.get(0) + "'. The format of the line index is not valid. "
				+ "A index can either be 'n' or '+' followed by a positive number 1-99.");

		final List<String> subBlock = new ArrayList<>();
		GedcomStoreLine lastStoreLine = null;
		for(final String line : block){
			if(line.startsWith(lineIndex)){
				//it is a line for this block on the same level

				//if there are sub block lines (lines with a higher level), process them first
				if(!subBlock.isEmpty() && !parseSubBlock(subBlock, lastStoreLine))
					return false;

				final GedcomStoreLine storeLine = GedcomStoreLine.parse(line);
				//FIXME manages null storeLine
				addLine(storeLine);

				lastStoreLine = storeLine;
			}
			else
				//it is a line of the sub block of the last line
				subBlock.add(line);
		}

		//process the last sub block
		return (subBlock.isEmpty() || parseSubBlock(subBlock, lastStoreLine));
	}

	/**
	 * Process a sub block.
	 */
	private boolean parseSubBlock(final List<String> subBlock, final GedcomStoreLine parentStoreLine) throws GedcomParseException{
		GedcomStoreBlock storeSubBlock = new GedcomStoreBlock();
		if(!storeSubBlock.parse(subBlock))
			return false;

		subBlock.clear();

		//add the new sub block as a child to its parent line
		parentStoreLine.setChildBlock(storeSubBlock);

		return true;
	}

	/**
	 * Adds a new store line to this block
	 */
	private void addLine(final GedcomStoreLine newLine){
		storeLines.add(newLine);

		if(newLine.getMin() > 0)
			mandatoryLines.add(newLine);

		if(newLine.hasStructureName()){
			//Link the structure name to the new line
			final String id = newLine.getStructureName();

			idToLineLinks.put(id, newLine);
		}
		else{
			//Link each tag to the new line
			final Set<String> allTags = newLine.getTagNames();
			for(final String tag : allTags)
				idToLineLinks.put(tag, newLine);
		}
	}

//	/**
//	 * Searches through the gedcom grammar structure and returns the path
//	 * to the child line with the given tag.
//	 * For example the INDIVIDUAL_RECORD structure:<br />
//	 * <pre><code>
//	 * 0 INDI
//	 *   1 SEX
//	 *   ...
//	 *   1 CHAN
//	 *     2 DATE
//	 *       3 TIME
//	 * </code></pre>
//	 * CHAN is a child line of INDI. However, it is defined in the gedcom grammar
//	 * as follows with a structure in between:<br />
//	 * <pre><code>
//	 * 0 INDI
//	 *   +1 SEX
//	 *   ...
//	 *   CHANGE_DATE
//	 *   +1 CHAN
//	 *     +2 DATE
//	 *       +3 TIME
//	 * </code></pre>
//	 *
//	 * This means that if this method is executed on the INDI block for example
//	 * with the parameter tag="CHAN", it returns [CHANGE_DATE, CHAN]. It only works
//	 * with immediate child lines, thus it is not possible to execute it for "DATE"
//	 * on the INDI block since it would be impossible to determine if the CHAN
//	 * DATE is needed or some other DATE tag in another structure.
//	 */
//	public List<String> getPathToStoreLine(final Store store, final String tagOrStructureName){
//		return getPathToStoreLine(store, tagOrStructureName, null, false, false, false);
//	}

//	/**
//	 * Searches through the gedcom grammar structure and returns the path
//	 * to the child line with the given tag.
//	 * For example the INDIVIDUAL_RECORD structure:<br />
//	 * <pre><code>
//	 * 0 INDI
//	 *   1 SEX
//	 *   ...
//	 *   1 CHAN
//	 *     2 DATE
//	 *       3 TIME
//	 * </code></pre>
//	 * CHAN is a child line of INDI. However, it is defined in the gedcom grammar
//	 * as follows with a structure in between:<br />
//	 * <pre><code>
//	 * 0 INDI
//	 *   +1 SEX
//	 *   ...
//	 *   CHANGE_DATE
//	 *   +1 CHAN
//	 *     +2 DATE
//	 *       +3 TIME
//	 * </code></pre>
//	 *
//	 * This means that if this method is executed on the INDI block for example
//	 * with the parameter tag="CHAN", it returns [CHANGE_DATE, CHAN]. It only works
//	 * with immediate child lines, thus it is not possible to execute it for "DATE"
//	 * on the INDI block since it would be impossible to determine if the CHAN
//	 * DATE is needed or some other DATE tag in another structure.
//	 */
//	public List<String> getPathToStoreLine(final Store store, final String tagOrStructureName, final String tag){
//		return getPathToStoreLine(store, tagOrStructureName, tag, false, false, false);
//	}

//	/**
//	 * Searches through the gedcom grammar structure and returns the path
//	 * to the child line with the given tag.
//	 * For example the INDIVIDUAL_RECORD structure:<br />
//	 * <pre><code>
//	 * 0 INDI
//	 *   1 SEX
//	 *   ...
//	 *   1 CHAN
//	 *     2 DATE
//	 *       3 TIME
//	 * </code></pre>
//	 * CHAN is a child line of INDI. However, it is defined in the gedcom grammar
//	 * as follows with a structure in between:<br />
//	 * <pre><code>
//	 * 0 INDI
//	 *   +1 SEX
//	 *   ...
//	 *   CHANGE_DATE
//	 *   +1 CHAN
//	 *     +2 DATE
//	 *       +3 TIME
//	 * </code></pre>
//	 *
//	 * This means that if this method is executed on the INDI block for example
//	 * with the parameter tag="CHAN", it returns [CHANGE_DATE, CHAN]. It only works
//	 * with immediate child lines, thus it is not possible to execute it for "DATE"
//	 * on the INDI block since it would be impossible to determine if the CHAN
//	 * DATE is needed or some other DATE tag in another structure.
//	 */
//	public List<String> getPathToStoreLine(final Store store, final String tagOrStructureName, final String tag, final boolean withXRef, final boolean withValue){
//		return getPathToStoreLine(store, tagOrStructureName, tag, true, withXRef, withValue);
//	}

//	/**
//	 * Searches through the gedcom grammar structure and returns the path
//	 * to the child line with the given tag.
//	 * For example the INDIVIDUAL_RECORD structure:<br />
//	 * <pre><code>
//	 * 0 INDI
//	 *   1 SEX
//	 *   ...
//	 *   1 CHAN
//	 *     2 DATE
//	 *       3 TIME
//	 * </code></pre>
//	 * CHAN is a child line of INDI. However, it is defined in the gedcom grammar
//	 * as follows with a structure in between:<br />
//	 * <pre><code>
//	 * 0 INDI
//	 *   +1 SEX
//	 *   ...
//	 *   CHANGE_DATE
//	 *   +1 CHAN
//	 *     +2 DATE
//	 *       +3 TIME
//	 * </code></pre>
//	 *
//	 * This means that if this method is executed on the INDI block for example
//	 * with the parameter tag="CHAN", it returns [CHANGE_DATE, CHAN]. It only works
//	 * with immediate child lines, thus it is not possible to execute it for "DATE"
//	 * on the INDI block since it would be impossible to determine if the CHAN
//	 * DATE is needed or some other DATE tag in another structure.
//	 */
//	public List<String> getPathToStoreLine(final Store store, final String tagOrStructureName, final boolean withXRef, final boolean withValue){
//		return getPathToStoreLine(store, tagOrStructureName, null, true, withXRef, withValue);
//	}

//	/**
//	 * Searches through the gedcom grammar structure and returns the path
//	 * to the child line with the given tag.
//	 * For example the INDIVIDUAL_RECORD structure:<br />
//	 * <pre><code>
//	 * 0 INDI
//	 *   1 SEX
//	 *   ...
//	 *   1 CHAN
//	 *     2 DATE
//	 *       3 TIME
//	 * </code></pre>
//	 * CHAN is a child line of INDI. However, it is defined in the gedcom grammar
//	 * as follows with a structure in between:<br />
//	 * <pre><code>
//	 * 0 INDI
//	 *   +1 SEX
//	 *   ...
//	 *   CHANGE_DATE
//	 *   +1 CHAN
//	 *     +2 DATE
//	 *       +3 TIME
//	 * </code></pre>
//	 *
//	 * This means that if this method is executed on the INDI block for example
//	 * with the parameter tag="CHAN", it returns [CHANGE_DATE, CHAN]. It only works
//	 * with immediate child lines, thus it is not possible to execute it for "DATE"
//	 * on the INDI block since it would be impossible to determine if the CHAN
//	 * DATE is needed or some other DATE tag in another structure.
//	 */
//	private List<String> getPathToStoreLine(final Store store, final String tagOrStructureName, String tag,
//			final boolean lookForXRefAndValueVariation, final boolean withXRef, boolean withValue){
//		final List<String> path = new LinkedList<>();
//		if(tag == null)
//			tag = tagOrStructureName;
//
//		if(hasStoreLine(tagOrStructureName)){
//			String variation = "";
//
//			if(store.hasStructure(tagOrStructureName)){
//				//It is a structure
//				if(store.getVariationTags(tagOrStructureName).contains(tag)){
//					//The structure has the given tag variation
//					variation = ";" + tag;
//
//					if(lookForXRefAndValueVariation)
//						variation = variation + ";" + withXRef + ";" + withValue;
//				}
//			}
//			path.add(tagOrStructureName + variation);
//			return path;
//		}
//		else{
//			for(StoreLine storeLine : storeLines){
//				//Only check structure lines
//				if(!storeLine.hasStructureName())
//					continue;
//				else if(!store.hasStructure(storeLine.getStructureName()))
//					continue;
//
//				StoreStructure structure;
//				String variation = "";
//
//				try{
//					if(store.structureHasVariations(storeLine.getStructureName())){
//						structure = store.getGedcomStructure(storeLine.getStructureName(), tag, lookForXRefAndValueVariation, withXRef, withValue);
//						variation = ";" + tagOrStructureName;
//
//						if(lookForXRefAndValueVariation)
//							variation = variation + ";" + withXRef + ";" + withValue;
//					}
//					else
//						structure = store.getGedcomStructure(storeLine.getStructureName(), null, false, false, false);
//				}
//				catch(final GedcomAccessError e){
//					//structure and/or variation does not exist
//					continue;
//				}
//
//				if(structure != null){
//					if(structure.getStoreBlock().hasStoreLine(tagOrStructureName)){
//						//found it!
//						path.add(storeLine.getStructureName() + variation);
//						path.add(tagOrStructureName);
//						return path;
//					}
//					else{
//						List<String> path2 = structure.getStoreBlock().getPathToStoreLine(store, tagOrStructureName, tag, lookForXRefAndValueVariation, withXRef, withValue);
//						if(path2 == null)
//							//not found in the path
//							continue;
//
//						path.add(storeLine.getStructureName() + variation);
//						path.addAll(path2);
//						return path;
//					}
//				}
//				//else: not found in this path
//			}
//
//			return null;
//		}
//	}

	/**
	 * Returns the line from this block which has the given tag or structure name.
	 */
//	public GedcomStoreLine getStoreLine(final String tagOrStructureName){
//		return idToLineLinks.get(tagOrStructureName);
//	}

	/**
	 * Returns a list of all the store lines which are in this store block.
	 */
//	public List<GedcomStoreLine> getStoreLines(){
//		return storeLines;
//	}

	/**
	 * Returns the line ID's (tag or structure names) of all the lines in this store block.
	 */
	public List<String> getAllLineIDs(){
		return new ArrayList<>(idToLineLinks.keySet());
	}

//	/**
//	 * Returns a list of all the mandatory lines in this block.
//	 */
//	public List<StoreLine> getMandatoryLines(){
//		return mandatoryLines;
//	}


	/**
	 * Returns the level of this block. The level of this block is one higher than the parent line of this block.
	 */
//	public int getLevel(final GedcomStoreLine parentStoreLine){
//		return (parentStoreLine != null? parentStoreLine.getLevel(this, parentStoreLine) + 1: 0);
//	}

//	/**
//	 * Returns <code>true</code> if this block has one or more child lines.
//	 */
//	public boolean hasChildLines(){
//		return !storeLines.isEmpty();
//	}

//	/**
//	 * Returns <code>true</code> if this block has one or more mandatory lines.
//	 */
//	public boolean hasMandatoryLines(){
//		return !mandatoryLines.isEmpty();
//	}

//	/**
//	 * Returns <code>true</code> if this block has a line with the given line ID (tag or structure name).
//	 */
//	public boolean hasStoreLine(final String lineId){
//		return idToLineLinks.containsKey(lineId);
//	}

//	@Override
//	public String toString(){
//		return GedcomStorePrinter.preparePrint(this, 1, false).toString();
//	}

}
