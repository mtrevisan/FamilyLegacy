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

import io.github.mtrevisan.familylegacy.services.RegexHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * A store line contains all the parsed information of one single line.
 * <p>A line could be a tag line or a structure line. A tag line has a tag, a tag ID (in brackets < and >) and min/max values, whereas
 * a structure line has a structure ID (in double brackets << and >>) and min/max values. A tag line can also have a value (enclosed
 * in < and >) or xref (enclosed in @< and >@) field and it can have multiple tag possibilities (enclosed in [ and ] and separated by
 * |).<br>
 * <br>
 * The class {@link GedcomStoreStructure} has more information about the hierarchy of structures, blocks and lines.</p>
 */
class GedcomStoreLine{

	private static final Logger LOGGER = LoggerFactory.getLogger(GedcomStoreLine.class);

	private static final Pattern COMMENT_PATTERN = Pattern.compile("/\\*.*\\*/");
	private static final Pattern LEVEL_PATTERN = Pattern.compile("^(n|\\+[1-9]|\\+[1-9][0-9]) ");
	private static final Pattern SPACES_PATTERN = Pattern.compile("[ \t]+");
	private static final Pattern XREF_PATTERN = Pattern.compile("@<.*>@");
	private static final Pattern XREF_TAG_REPLACE = Pattern.compile("[@<|>@]");
	private static final Pattern MULTIPLE_X_REFS = Pattern.compile("([\\[|]@?<[A-Z_:]+>@?\\|?)+\\]");
	private static final Pattern MULTIPLE_X_REFS_REPLACE = Pattern.compile("[\\[\\] <>@]");
	private static final Pattern MIN_MAX_PATTERN = Pattern.compile("\\{\\d:[\\d|M]\\*?\\}\\*?");
	private static final Pattern MIN_MAX_REPLACE = Pattern.compile("[{|}|*]");
	private static final Pattern STRUCTURE_PATTERN = Pattern.compile("<<.*>>");
	private static final Pattern STRUCTURE_REPLACE = Pattern.compile("[<<|>>]");
	private static final Pattern VALUE_PATTERN = Pattern.compile("<.*>");
	private static final Pattern VALUE_REPLACE = Pattern.compile("[<|>]");
	/** Matches [&lt;EVENT_DESCRIPTOR&gt;|&lt;NULL&gt;] etc */
	private static final Pattern MULTIPLE_VALUES = Pattern.compile("([\\[|]<[A-Z_]+>\\|?)+\\]");
	private static final Pattern MULTIPLE_VALUES_REPLACE = Pattern.compile("[\\[\\] <>]");
	private static final Pattern TAG_PATTERN = Pattern.compile("[A-Z]+[1-9]*");
	/** Matches multiple tags ([ABC|DEF|GHI|JKL]) */
	private static final Pattern MULTIPLE_TAGS = Pattern.compile("([\\[|]?[A-Z]+\\|?)+\\]");
	private static final Pattern MULTIPLE_TAGS_REPLACE = Pattern.compile("[\\[\\] ]");
	private static final Pattern MULTIPLE_VALUE_POSSIBILITIES = Pattern.compile("([\\[|]<?[A-Z_]+>?\\|?)+\\]");


	private int min;
	private int max;

	private final Set<String> xrefNames = new HashSet<>();
	private final Set<String> valueNames = new HashSet<>();
	private final Set<String> tagNames1 = new HashSet<>();
	private final Set<String> tagNames2 = new HashSet<>();
	private final Set<String> valuePossibilities = new HashSet<>();

	private String structureName;
	private String originalGedcomDefinitionLine;

	private GedcomStoreBlock childBlock;


	/**
	 * Sets a child block for this gedcom store line.
	 */
	protected void setChildBlock(final GedcomStoreBlock childBlock){
		this.childBlock = childBlock;
	}

	/**
	 * Parses the given lineage linked grammar line.
	 */
	protected static GedcomStoreLine parse(String gedcomDefinitionLine){
		final GedcomStoreLine sl = new GedcomStoreLine();
		sl.originalGedcomDefinitionLine = gedcomDefinitionLine;

		//clean the line from all unnecessary stuff
		gedcomDefinitionLine = RegexHelper.removeAll(gedcomDefinitionLine, COMMENT_PATTERN);
		gedcomDefinitionLine = RegexHelper.replaceAll(gedcomDefinitionLine, LEVEL_PATTERN, "");
		gedcomDefinitionLine = RegexHelper.replaceAll(gedcomDefinitionLine, SPACES_PATTERN, " ");
		gedcomDefinitionLine = gedcomDefinitionLine.trim();

		//split for each space
		final String[] components = gedcomDefinitionLine.split(" ");
		//check if line could be valid
		if(components.length < 1 || components.length > 4){
			LOGGER.error("[ERROR] Failed to parse line '{}'. Number of items not in the range.", gedcomDefinitionLine);
			return null;
		}

		int tagIndex = - 1;
		for(int i = 0; i < components.length; i ++){
			if(RegexHelper.matches(components[i], XREF_PATTERN))
				//@<XREF:TAG>
				sl.xrefNames.add(RegexHelper.removeAll(components[i], XREF_TAG_REPLACE));
			else if(components[i].contains("@") && RegexHelper.matches(components[i], MULTIPLE_X_REFS)){
				//Multiple XREF ([@<XREF>@|@<XREF>@|<NULL>...])
				//At least one @ has to be present
				final String[] values = RegexHelper.replaceAll(components[i], MULTIPLE_X_REFS_REPLACE, "").split("\\|");
				Collections.addAll(sl.xrefNames, values);
			}
			else if(RegexHelper.matches(components[i], MIN_MAX_PATTERN)){
				//{MIN:MAX}
				//{MIN:MAX*}
				//{MIN:MAX}*
				final String[] minmax = RegexHelper.removeAll(components[i], MIN_MAX_REPLACE).split(":");
				sl.min = Integer.parseInt(minmax[0]);
				if(!minmax[1].equals("M"))
					sl.max = Integer.parseInt(minmax[1]);
			}
			else if(RegexHelper.matches(components[i], STRUCTURE_PATTERN))
				//<<STRUCTURE>>
				sl.structureName = RegexHelper.removeAll(components[i], STRUCTURE_REPLACE);
			else if(RegexHelper.matches(components[i], VALUE_PATTERN))
				//<VALUE>
				sl.valueNames.add(RegexHelper.removeAll(components[i], VALUE_REPLACE));
			else if(RegexHelper.matches(components[i], MULTIPLE_VALUES)){
				//Multiple VALUE ([<ABC>|<DEF>|<GHI>...])
				final String[] values = RegexHelper.replaceAll(components[i], MULTIPLE_VALUES_REPLACE, "").split("\\|");
				Collections.addAll(sl.valueNames, values);
			}
			else if(RegexHelper.matches(components[i], TAG_PATTERN)){
				//TAG
				if(sl.xrefNames.isEmpty())
					//tag before xref
					sl.tagNames1.add(components[i]);
				else
					//tag after xref
					sl.tagNames2.add(components[1]);

				tagIndex = i;
			}
			else if(RegexHelper.contains(components[i], MULTIPLE_TAGS)){
				//Multiple TAG ([ABC|DEF|GHI...])
				final String[] tags = RegexHelper.replaceAll(components[i], MULTIPLE_TAGS_REPLACE, "").split("\\|");
				for(final String tag : tags){
					if(sl.xrefNames.size() == 0)
						//Tag before xref
						sl.tagNames1.add(tag);
					else
						//Tag after xref
						sl.tagNames2.add(tag);
				}

				tagIndex = i;
			}
			else if(tagIndex != - 1 && i == tagIndex + 1 && RegexHelper.contains(components[i], MULTIPLE_VALUE_POSSIBILITIES)){
				//Value possibilities. They can only appear right after the tag
				//Example: DEAT [Y|<NULL>]
				final String[] possibilities = RegexHelper.replaceAll(components[i], MULTIPLE_VALUES_REPLACE, "")
					.split("\\|");
				for(String possibility : possibilities)
					sl.valuePossibilities.add(!possibility.toUpperCase().equals("NULL")? possibility: null);
			}
			else
				LOGGER.info("Did not process {} in {} under "
					//FIXME
					/*+ storeStructure.getStructureName()*/, components[i], sl.getId());
		}

		//FIXME
//		LOGGER.trace("  parsed: {}", GedcomStorePrinter.preparePrint(this));

		return sl;
	}

	/**
	 * Returns the position of this store line in the block.
	 */
//	public int getPos(final GedcomStoreBlock parentBlock){
//		return parentBlock.getStoreLines().indexOf(this);
//	}

//	/**
//	 * Returns the child block of this store line.
//	 */
//	public StoreBlock getChildBlock(){
//		return childBlock;
//	}

//	/**
//	 * Returns the store structure if there is one. <code>NULL</code> is returned
//	 * if there is no store structure or if multiple variations are available.<br>
//	 * <b>Note: </b>Only if this is a structure line the store structure can be retrieved.
//	 */
//	public StoreStructure getStoreStructure(final Store store){
//		if(structureName == null)
//			return null;
//
//		final List<StoreStructure> storeStructures = store.getVariations(structureName);
//		if(storeStructures == null || storeStructures.size() > 1 || storeStructures.size() == 0)
//			//No variations or multiple variations available
//			return null;
//
//		//Only one variation available
//		return storeStructures.get(0);
//	}

//	/**
//	 * Returns <code>true</code> if this store line has multiple variations.<br>
//	 * <b>Note: </b>Only a structure line can have variations.
//	 */
//	public boolean hasVariations(final Store store){
//		if(structureName == null)
//			return false;
//
//		return store.getVariations(structureName).size() > 1;
//	}

//	protected String getOriginalGedcomDefinitionLine(){
//		return originalGedcomDefinitionLine;
//	}

//	/**
//	 * Creates a new instance of a {@link GedcomLine}<br>
//	 * The returned line can be a {@link GedcomStructureLine} if a structure name
//	 * is set, or a {@link GedcomTagLine} if no structure name is set and the
//	 * given tag is valid.<br>
//	 * <br>
//	 * This method can be used if only one tag name for this line exists. If there
//	 * are multiple tag names, null is returned.
//	 *
//	 * @param parentLine The line which should be the parent of the returned
//	 * line
//	 * @return
//	 */
//	public GedcomLine getLineInstance(GedcomLine parentLine, int copyMode) {
//
//		if (structureName != null) {
//			return new GedcomStructureLine(this, parentLine, copyMode);
//		} else {
//			LinkedHashSet<String> tagNames = getTagNames();
//
//			if (tagNames.size() != 1) {
//				return null;
//			}
//
//			return new GedcomTagLine(this, parentLine,
//					tagNames.toArray(new String[tagNames.size()])[0], copyMode);
//		}
//	}
//
//	/**
//	 * Creates a new instance of a {@link GedcomLine}<br>
//	 * The returned line can be a {@link GedcomStructureLine} if a structure name
//	 * is set, or a {@link GedcomTagLine} if no structure name is set and the
//	 * given tag is valid.<br>
//	 * <br>
//	 * This method has to be used if multiple variations for this line exists.
//	 *
//	 * @param parentLine The line which should be the parent of the returned
//	 * line
//	 * @param tag
//	 * @return
//	 */
//	public GedcomLine getLineInstance(GedcomLine parentLine, String tag, int copyMode) {
//
//		if (structureName != null) {
//			return new GedcomStructureLine(this, parentLine, tag, copyMode);
//		} else {
//			if (!hasTag(tag)) {
//				return null;
//			}
//
//			return new GedcomTagLine(this, parentLine, tag, copyMode);
//		}
//	}
//
//	/**
//	 * Creates a new instance of a {@link GedcomLine}<br>
//	 * The returned line can be a {@link GedcomStructureLine} if a structure name
//	 * is set, or a {@link GedcomTagLine} if no structure name is set and the
//	 * given tag is valid.<br>
//	 * <br>
//	 * This method has to be used if multiple variations for this line exists.
//	 *
//	 * @param parentLine The line which should be the parent of the returned
//	 * line
//	 * @param tag
//	 * @param withXRef
//	 * @param withValue
//	 * @return
//	 */
//	public GedcomLine getLineInstance(GedcomLine parentLine, String tag,
//			boolean withXRef, boolean withValue, int copyMode) {
//
//		if (structureName != null) {
//			return new GedcomStructureLine(this, parentLine, tag, withXRef, withValue, copyMode);
//		} else {
//			if (!hasTag(tag)) {
//				return null;
//			}
//
//			return new GedcomTagLine(this, parentLine, tag, copyMode);
//		}
//	}

	/**
	 * Returns the minimum number of lines which are required in one block.
	 */
	public int getMin(){
		return min;
	}

//	/**
//	 * Returns the maximum number of lines which are allowed in one block.
//	 */
//	public int getMax(){
//		return max;
//	}

//	/**
//	 * Returns <code>true</code> if this is a mandatory line (with a minimum number of lines >= 1).
//	 */
//	public boolean isMandatory(){
//		return (min >= 1);
//	}

	/**
	 * Returns the structure name if there is one. If there is a structure name
	 * (the return value is != <code>NULL</code>), it means that this line is
	 * a structure line.
	 */
	public String getStructureName(){
		return structureName;
	}

	/**
	 * Returns the ID of this line. The ID is either the structure name (if
	 * this is a structure line) or a list of the possible tag names (if it
	 * is a tag line). This ID can be used to identify the store line.
	 */
	public String getId(){
		return (structureName != null? structureName: GedcomFormatter.makeOrList(getTagNames(), "", "").toString());
	}

	/**
	 * Returns all the possible tag names.
	 */
	public Set<String> getTagNames(){
		return (!tagNames1.isEmpty()? tagNames1: tagNames2);
	}

	/**
	 * Returns a list of all the xref names on this line.
	 */
//	public Set<String> getXRefNames(){
//		return xrefNames;
//	}

	/**
	 * Returns a list of all the value names in this line.
	 */
//	public Set<String> getValueNames(){
//		return valueNames;
//	}

	/**
	 * Returns all values which are possible for this line.
	 */
//	public Set<String> getValuePossibilities(){
//		return valuePossibilities;
//	}

	/**
	 * Returns <code>true</code> if this line has at least one tag name.
	 */
//	public boolean hasTags(){
//		return !getTagNames().isEmpty();
//	}

	/**
	 * Returns <code>true</code> if the tag appears before the xref value on this line.
	 */
//	public boolean hasTagBeforeXRef(){
//		return !tagNames1.isEmpty();
//	}

	/**
	 * Returns <code>true</code> if the tag appears after the xref value on this line.
	 */
//	public boolean hasTagAfterXRef(){
//		return !tagNames2.isEmpty();
//	}

	/**
	 * Returns <code>true</code> if this line has any xref names.
	 */
//	public boolean hasXRefNames(){
//		return !xrefNames.isEmpty();
//	}

	/**
	 * Returns <code>true</code> if this line has any value names.
	 */
//	public boolean hasValueNames(){
//		return (!valueNames.isEmpty() || !valuePossibilities.isEmpty());
//	}

//	/**
//	 * Returns <code>true</code> if this line has more than one tag name possibilities.
//	 */
//	public boolean hasMultipleTagNames(){
//		return (getTagNames().size() > 1);
//	}

	/**
	 * Returns <code>true</code> if this line has a structure name instead of
	 * tag names. If this line has a structure name, it is a structure line and
	 * does not have any value/xref fields.
	 */
	public boolean hasStructureName(){
		return (structureName != null);
	}

	/**
	 * Returns the level of this line.
	 */
//	public int getLevel(final GedcomStoreBlock parentBlock, final GedcomStoreLine parentStoreLine){
//		return parentBlock.getLevel(parentStoreLine);
//	}

//	/**
//	 * Returns true if this line has sub-lines (with higher levels than this line)
//	 * and therefore has a child block which contains all the sub-lines.
//	 */
//	public boolean hasChildBlock(){
//		return (childBlock != null);
//	}

//	/**
//	 * Returns <code>true</code> if the given tag name is a possible tag name for this line.
//	 */
//	public boolean hasTag(final String tag){
//		return getTagNames().contains(tag);
//	}

//	@Override
//	public String toString(){
//		return GedcomStorePrinter.preparePrint(this).toString();
//	}

}
