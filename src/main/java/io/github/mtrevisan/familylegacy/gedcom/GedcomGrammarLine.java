/**
 * Copyright (c) 2020 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.gedcom;

import io.github.mtrevisan.familylegacy.services.RegexHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * A grammar line contains all the parsed information of one single line.
 * <p>A line could be a tag line or a structure line. A tag line has a tag, a tag ID (in brackets {@code <} and {@code >}) and min/max
 * values, whereas a structure line has a structure ID (in double brackets {@code <<} and {@code >>}) and min/max values. A tag line
 * can also have a value (enclosed in {@code <} and {@code >}) or xref (enclosed in {@code @<} and {@code >@}) field and it can have
 * multiple tag possibilities (enclosed in {@code [} and {@code ]} and separated by {@code |}).<br>
 * <br>
 * The class {@link GedcomGrammarStructure} has more information about the hierarchy of structures, blocks and lines.</p>
 */
class GedcomGrammarLine{

	private static final Logger LOGGER = LoggerFactory.getLogger(GedcomGrammarLine.class);

	private static final Pattern COMMENT_PATTERN = Pattern.compile("/\\*.*\\*/");
	private static final Pattern LEVEL_PATTERN = Pattern.compile("^(n|\\+[1-9]|\\+[1-9][0-9]) ");
	private static final Pattern SPACES_PATTERN = Pattern.compile("[ \t]+");
	private static final Pattern XREF_PATTERN = Pattern.compile("@<.*>@");
	private static final Pattern XREF_TAG_REPLACE = Pattern.compile("[@<|>]");
	private static final Pattern MULTIPLE_X_REFS = Pattern.compile("([\\[|]@?<[A-Z_:]+>@?\\|?)+\\]");
	private static final Pattern MULTIPLE_X_REFS_REPLACE = Pattern.compile("[\\[\\] <>@]");
	private static final Pattern MIN_MAX_PATTERN = Pattern.compile("\\{\\d:[\\d|M]\\*?\\}\\*?");
	private static final Pattern MIN_MAX_REPLACE = Pattern.compile("[{|}*]");
	private static final Pattern STRUCTURE_PATTERN = Pattern.compile("<<.*>>");
	private static final Pattern STRUCTURE_REPLACE = Pattern.compile("[<|>]");
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
	private final Set<String> tagNamesBeforeXRef = new HashSet<>();
	private final Set<String> tagNamesAfterXRef = new HashSet<>();
	private final Set<String> valuePossibilities = new HashSet<>();

	private String structureName;
	private String originalDefinitionLine;

	private GedcomGrammarBlock childBlock;


	/**
	 * Parses the given lineage linked grammar line.
	 */
	public static GedcomGrammarLine parse(String gedcomDefinitionLine){
		final GedcomGrammarLine sl = new GedcomGrammarLine();
		sl.originalDefinitionLine = gedcomDefinitionLine;

		//clean the line from all unnecessary stuff
		gedcomDefinitionLine = RegexHelper.removeAll(gedcomDefinitionLine, COMMENT_PATTERN);
		gedcomDefinitionLine = RegexHelper.replaceAll(gedcomDefinitionLine, LEVEL_PATTERN, "");
		gedcomDefinitionLine = RegexHelper.replaceAll(gedcomDefinitionLine, SPACES_PATTERN, " ");
		gedcomDefinitionLine = gedcomDefinitionLine.trim();

		//split for each space
		final String[] components = StringUtils.split(gedcomDefinitionLine, ' ');
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
				final String[] values = StringUtils.split(RegexHelper.replaceAll(components[i], MULTIPLE_X_REFS_REPLACE, ""), '|');
				Collections.addAll(sl.xrefNames, values);
			}
			else if(RegexHelper.matches(components[i], MIN_MAX_PATTERN)){
				//{MIN:MAX}
				//{MIN:MAX*}
				//{MIN:MAX}*
				final String[] minmax = StringUtils.split(RegexHelper.removeAll(components[i], MIN_MAX_REPLACE), ':');
				sl.min = Integer.parseInt(minmax[0]);
				sl.max = ("M".equals(minmax[1])? -1: Integer.parseInt(minmax[1]));
			}
			else if(RegexHelper.matches(components[i], STRUCTURE_PATTERN))
				//<<STRUCTURE>>
				sl.structureName = RegexHelper.removeAll(components[i], STRUCTURE_REPLACE);
			else if(RegexHelper.matches(components[i], VALUE_PATTERN))
				//<VALUE>
				sl.valueNames.add(RegexHelper.removeAll(components[i], VALUE_REPLACE));
			else if(RegexHelper.matches(components[i], MULTIPLE_VALUES)){
				//Multiple VALUE ([<ABC>|<DEF>|<GHI>...])
				final String[] values = StringUtils.split(RegexHelper.replaceAll(components[i], MULTIPLE_VALUES_REPLACE, ""), '|');
				Collections.addAll(sl.valueNames, values);
			}
			else if(RegexHelper.matches(components[i], TAG_PATTERN)){
				//TAG
				if(sl.xrefNames.isEmpty())
					//tag before xref
					sl.tagNamesBeforeXRef.add(components[i]);
				else
					//tag after xref
					sl.tagNamesAfterXRef.add(components[1]);

				tagIndex = i;
			}
			else if(RegexHelper.contains(components[i], MULTIPLE_TAGS)){
				//Multiple TAG ([ABC|DEF|GHI...])
				final String[] tags = StringUtils.split(RegexHelper.replaceAll(components[i], MULTIPLE_TAGS_REPLACE, ""), '|');
				for(final String tag : tags){
					if(sl.xrefNames.isEmpty())
						sl.tagNamesBeforeXRef.add(tag);
					else
						sl.tagNamesAfterXRef.add(tag);
				}

				tagIndex = i;
			}
			else if(tagIndex != - 1 && i == tagIndex + 1 && RegexHelper.contains(components[i], MULTIPLE_VALUE_POSSIBILITIES)){
				//Value possibilities. They can only appear right after the tag
				//Example: DEAT [Y|<NULL>]
				final String[] possibilities = StringUtils.split(
					RegexHelper.replaceAll(components[i], MULTIPLE_VALUES_REPLACE, ""), '|');
				for(final String possibility : possibilities)
					sl.valuePossibilities.add(!"NULL".equals(possibility.toUpperCase())? possibility: null);
			}
			else
				LOGGER.info("Did not process {} in {} under {}", components[i], sl.getId(), sl.structureName);
		}

		return sl;
	}

	/**
	 * @return	Child block of this grammar line.
	 */
	public GedcomGrammarBlock getChildBlock(){
		return childBlock;
	}

	/**
	 * Sets a child block for this gedcom grammar line.
	 */
	public void setChildBlock(final GedcomGrammarBlock childBlock){
		this.childBlock = childBlock;
	}

	public String getOriginalDefinitionLine(){
		return originalDefinitionLine;
	}

	/**
	 * @return	Minimum number of lines which are required in one block.
	 */
	public int getMin(){
		return min;
	}

	/**
	 * @return	Maximum number of lines which are allowed in one block.
	 */
	public int getMax(){
		return max;
	}

	/**
	 * @return	Whether this is a mandatory line (with a minimum number of lines {@code >= 1}).
	 */
	public boolean isMandatory(){
		return (min >= 1);
	}

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
	 * is a tag line). This ID can be used to identify the grammar line.
	 */
	public String getId(){
		return (structureName != null? structureName: GedcomFormatter.makeOrList(getTagNames()).toString());
	}

	/**
	 * @return	All the possible tag names.
	 */
	public Set<String> getTagNames(){
		return (!tagNamesBeforeXRef.isEmpty()? tagNamesBeforeXRef: tagNamesAfterXRef);
	}

	/**
	 * @return	List of all the xref names on this line.
	 */
	public Set<String> getXRefNames(){
		return xrefNames;
	}

	/**
	 * @return	List of all the value names in this line.
	 */
	public Set<String> getValueNames(){
		return valueNames;
	}

	/**
	 * Returns all values which are possible for this line.
	 */
	public Set<String> getValuePossibilities(){
		return valuePossibilities;
	}

	/**
	 * @return	Whether this line has at least one tag name.
	 */
	public boolean hasTags(){
		return !getTagNames().isEmpty();
	}

	/**
	 * @return	Whether the tag appears before the {@code xref} value on this line.
	 */
	public boolean hasTagBeforeXRef(){
		return !tagNamesBeforeXRef.isEmpty();
	}

	/**
	 * @return	Whether the tag appears after the {@code xref} value on this line.
	 */
	public boolean hasTagAfterXRef(){
		return !tagNamesAfterXRef.isEmpty();
	}

	/**
	 * @return	Whether this line has any xref names.
	 */
	public boolean hasXRefNames(){
		return !xrefNames.isEmpty();
	}

	/**
	 * @return	Whether this line has any value names.
	 */
	public boolean hasValueNames(){
		return (!valueNames.isEmpty() || !valuePossibilities.isEmpty());
	}

	/**
	 * @return	Whether this line has more than one tag name possibilities.
	 */
	public boolean hasMultipleTagNames(){
		return (getTagNames().size() > 1);
	}

	/**
	 * Returns <code>true</code> if this line has a structure name instead of
	 * tag names. If this line has a structure name, it is a structure line and
	 * does not have any value/xref fields.
	 */
	public boolean hasStructureName(){
		return (structureName != null);
	}

	/**
	 * @return	Whether this line has sub-lines (with higher levels than this line) and therefore has a child block which contains
	 * 	all the sub-lines.
	 */
	public boolean hasChildBlock(){
		return (childBlock != null);
	}

	/**
	 * @return	Whether the given tag name is a possible tag name for this line.
	 */
	public boolean hasTag(final String tag){
		return getTagNames().contains(tag);
	}

}
