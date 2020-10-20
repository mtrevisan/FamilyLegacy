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

import io.github.mtrevisan.familylegacy.grammar.exceptions.GedcomGrammarParseException;
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
	protected boolean parse(final List<String> block) throws GedcomGrammarParseException{
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
			throw GedcomGrammarParseException.create("On line '{}'. The format of the line index is not valid. "
				+ "A index can either be 'n' or '+' followed by a positive number 1-99.", block.get(0));

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
	private boolean parseSubBlock(final List<String> subBlock, final GedcomStoreLine parentStoreLine) throws GedcomGrammarParseException{
		final GedcomStoreBlock storeSubBlock = new GedcomStoreBlock();
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

		if(newLine.hasStructureName()){
			//link the structure name to the new line
			final String id = newLine.getStructureName();

			idToLineLinks.put(id, newLine);
		}
		else{
			//link each tag to the new line
			final Set<String> allTags = newLine.getTagNames();
			for(final String tag : allTags)
				idToLineLinks.put(tag, newLine);
		}
	}

	/**
	 * Returns the line from this block which has the given tag or structure name.
	 */
	public GedcomStoreLine getStoreLine(final String tagOrStructureName){
		return idToLineLinks.get(tagOrStructureName);
	}

	/**
	 * Returns a list of all the store lines which are in this store block.
	 */
	public List<GedcomStoreLine> getStoreLines(){
		return storeLines;
	}

	/**
	 * Returns the line ID's (tag or structure names) of all the lines in this store block.
	 */
	public List<String> getAllLineIDs(){
		return new ArrayList<>(idToLineLinks.keySet());
	}

	/**
	 * Returns a list of all the mandatory lines in this block.
	 */
	public List<GedcomStoreLine> getMandatoryLines(){
		/** A sublist of the {@link #storeLines} which only contains the mandatory lines. */
		final List<GedcomStoreLine> mandatoryLines = new ArrayList<>(storeLines);
		for(final GedcomStoreLine line : storeLines)
			if(line.getMin() > 0)
				mandatoryLines.add(line);
		return mandatoryLines;
	}

	/**
	 * Returns the level of this block. The level of this block is one higher than the parent line of this block.
	 */
	public int getLevel(final GedcomStoreLine parentStoreLine){
		return (parentStoreLine != null? parentStoreLine.getLevel(this, parentStoreLine) + 1: 0);
	}

	/**
	 * Returns <code>true</code> if this block has one or more child lines.
	 */
	public boolean hasChildLines(){
		return !storeLines.isEmpty();
	}

	/**
	 * Returns <code>true</code> if this block has one or more mandatory lines.
	 */
	public boolean hasMandatoryLines(){
		for(final GedcomStoreLine line : storeLines)
			if(line.getMin() > 0)
				return true;
		return false;
	}

	/**
	 * Returns <code>true</code> if this block has a line with the given line ID (tag or structure name).
	 */
	public boolean hasStoreLine(final String lineId){
		return idToLineLinks.containsKey(lineId);
	}

}
