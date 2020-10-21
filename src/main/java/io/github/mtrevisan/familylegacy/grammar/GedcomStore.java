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
package io.github.mtrevisan.familylegacy.grammar;

import io.github.mtrevisan.familylegacy.grammar.exceptions.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.services.RegexHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


/**
 * Parse a lineage-linked grammar file and to retrieve the parsed structures from it.
 *
 * @see <a href="https://github.com/daleathan/GedcomStore">GedcomStore</a>
 */
class GedcomStore{

	private static final Logger LOGGER = LoggerFactory.getLogger(GedcomStore.class);

	private static final String GEDCOM_FILENAME_EXTENSION = "gedg";

	private static final String COMMENT_START = "/*";
	private static final Pattern STRUCTURE_NAME_PATTERN = RegexHelper.pattern("[A-Z_]+\\s?:=");
	/** Matches any or-item (the | sign), with or without leading and trailing spaces */
	private static final Pattern OR_PATTERN = RegexHelper.pattern("[ ]*\\|[ ]*");
	/** Matches the [ bracket, with or without trailing spaces */
	private static final Pattern BRACKET_OPEN = RegexHelper.pattern("\\[[ ]*");
	/** Matches the ] bracket, with or without leading spaces */
	private static final Pattern BRACKET_CLOSE = RegexHelper.pattern("[ ]*\\]");
	private static final Pattern SUB_BLOCK_DIVIDER = Pattern.compile("^[\\[\\]|]");
	private static final Pattern ID_PATTERN = Pattern.compile("[A-Z]+([_:]*[A-Z])+");


	private String gedcomVersion;
	private String gedcomSource;
	private final Collection<String> gedcomDescription = new ArrayList<>();

	/** All structures in an ordered list in their parsed order. */
	private final List<GedcomStoreStructure> structures = new ArrayList<>();
	/**
	 * This map contains all the available structure names and links them to the structures.
	 * <p>If multiple variations of a structure are available, the variation can only be determined by the line ID of one of the
	 * top-lines of the first block (a top-line is a line with the index "n" in the lineage-linked grammar). The sub-map of this map
	 * holds those line ID's of all the top-lines. However, the same line ID can occur in multiple variations, thus a list is used to
	 * link multiple store structures to one line ID if necessary.<br>
	 * </p>
	 * &lt;Structure name &lt;Line ID &lt;List of structures&gt;&gt;&gt;
	 */
	private final Map<String, Map<String, List<GedcomStoreStructure>>> idToVariationsLinks = new HashMap<>();
	private final Map<String, List<GedcomStoreStructure>> variationsLinksToId = new HashMap<>();
	/**
	 * This map holds a list for each structure.
	 * <p>The list contains all the variations for that structure.<br>
	 * </p>
	 * &lt;Structure name &lt;List of structures&gt;&gt;
	 */
	private final Map<String, List<GedcomStoreStructure>> variations = new HashMap<>();


	/**
	 * Parses the given lineage-linked grammar file and adds all the structures to this store.
	 *
	 * @param grammarFile	The grammar file.
	 */
	public static GedcomStore create(final String grammarFile) throws GedcomGrammarParseException{
		if(!grammarFile.endsWith("." + GEDCOM_FILENAME_EXTENSION))
			throw GedcomGrammarParseException.create("Invalid GEDCOM grammar file: only files with extension {} are supported",
				GEDCOM_FILENAME_EXTENSION);

		try(final InputStream is = GedcomStore.class.getResourceAsStream(grammarFile)){
			return create(is);
		}
		catch(final IOException e){
			throw GedcomGrammarParseException.create("File {} not found!", grammarFile);
		}
	}

	/**
	 * Parses the given lineage-linked grammar file and adds all the structures to this store.
	 *
	 * @param is	The grammar file.
	 */
	public static GedcomStore create(final InputStream is) throws GedcomGrammarParseException{
		final GedcomStore store = new GedcomStore();
		store.parse(is);
		return store;
	}

	private void parse(final InputStream is) throws GedcomGrammarParseException{
		LOGGER.info("Parsing GEDCOM grammar objects...");

		int lineCount = 0;
		boolean processFileHeader = true;
		boolean descriptionFound = false;
		try(final BufferedReader br = new BufferedReader(new InputStreamReader(is))){
			final List<String> block = new ArrayList<>();

			String line;
			while((line = br.readLine()) != null){
				lineCount ++;

				//remove all leading and trailing extra stuff (spaces, tags, newlines, line feeds), and remove any excessive spaces
				line = line.trim();

				//skip empty lines and comment lines
				if(line.isEmpty() || line.startsWith(COMMENT_START))
					continue;

				//as long as the first structure has not yet appeared and the current line is not the start of a structure, process the
				//file header lines
				if(processFileHeader){
					if(!RegexHelper.matches(line, STRUCTURE_NAME_PATTERN)){
						final String[] components = StringUtils.split(line, '=');
						if(FileHeaderKeywords.GEDCOM_VERSION.value.equals(components[0]))
							gedcomVersion = components[1];
						else if(FileHeaderKeywords.GEDCOM_SOURCE.value.equals(components[0]))
							gedcomSource = components[1];
						else if(FileHeaderKeywords.GEDCOM_DESCRIPTION.value.equals(components[0])){
							if(components.length > 1 && !components[1].isEmpty())
								gedcomDescription.add(components[1]);

							descriptionFound = true;
						}
						//this assumes the next line of a description is a continuation line of the description
						else if(descriptionFound)
							gedcomDescription.add(line);
						else
							throw GedcomGrammarParseException.create("Unrecognized line '{}' at index {}", line, lineCount);

						continue;
					}
					else{
						if(gedcomVersion == null || gedcomSource == null || gedcomDescription.isEmpty())
							throw GedcomGrammarParseException.create("Invalid gedcom grammar file format. "
								+ "The file needs a header with the following keywords: {}", Arrays.toString(FileHeaderKeywords.values()));

						LOGGER.trace("Gedcom version: {}", gedcomVersion);
						LOGGER.trace("Source of gedcom grammar: {}", gedcomSource);
						for(final String description : gedcomDescription)
							LOGGER.trace(description);

						processFileHeader = false;
					}
				}

				//no spaces around OR-signs
				line = RegexHelper.replaceAll(line, OR_PATTERN, "|");
				//no spaces around open brackets
				line = RegexHelper.replaceAll(line, BRACKET_OPEN, "[");
				//no spaces around closing brackets
				line = RegexHelper.replaceAll(line, BRACKET_CLOSE, "]");

				if(RegexHelper.matches(line, STRUCTURE_NAME_PATTERN)){
					//a new structure starts:
					if(!block.isEmpty()){
						//process current block...
						parseBlock(block);

						//... and reset the block after processing
						block.clear();
					}

					//add current line to block
					block.add(line);
				}
				else if(!block.isEmpty())
					//only add the current line to the block if the start of a structure has been found and the block already contains one
					//line (the line with the structure name)
					block.add(line);
			}

			if(!block.isEmpty())
				//and process the last block
				parseBlock(block);
		}
		catch(final IOException e){
			throw GedcomGrammarParseException.create("Failed to read line {}", lineCount);
		}

		LOGGER.info("Adding objects done ({} objects parsed)", structures.size());
	}

	/**
	 * Parses a block, starting from the block name (like FAMILY_EVENT_DETAIL etc.) to the last line, just before a new block name begins.
	 * <p>A block contains the block name on the first line, and might contain multiple block variations.</p>
	 */
	private void parseBlock(final List<String> block) throws GedcomGrammarParseException{
		//the first line is the structure name
		final String structureName = RegexHelper.getFirstMatching(block.get(0), ID_PATTERN);

		LOGGER.trace("=== {} ===", structureName);

		//the second line defines if the block has variations or not
		if(block.get(1).startsWith("[")){
			//it has variations:
			int lastDivider = 2;
			for(int i = 2; i < block.size(); i ++){
				final String line = block.get(i);

				//process all sub-blocks one by one
				if(RegexHelper.contains(line, SUB_BLOCK_DIVIDER)){
					parseSubBlock(structureName, block.subList(lastDivider, i));
					lastDivider = i + 1;
				}
			}
		}
		else
			//no variations: process the whole block without the structure-ID
			parseSubBlock(structureName, block.subList(1, block.size()));
	}

	/**
	 * Processes a sub-block, which only contains gedcom lines (without structure name and without variations).
	 */
	@SuppressWarnings("ObjectAllocationInLoop")
	private void parseSubBlock(final String structureName, final List<String> subBlockView) throws GedcomGrammarParseException{
		//parse the sub block and build the new structure
		final GedcomStoreStructure storeStructure = GedcomStoreStructure.create(structureName, new ArrayList<>(subBlockView));

		//create a simple list of all the available structures
		structures.add(storeStructure);

		//link all the line ID's of the first block to their structure:
		if(!idToVariationsLinks.containsKey(structureName))
			//add a new structure
			idToVariationsLinks.put(structureName, new HashMap<>());
		final List<String> ids = storeStructure.getStoreBlock().getAllLineIDs();
		for(final String id : ids)
			idToVariationsLinks.get(structureName).computeIfAbsent(id, k -> new ArrayList<>())
				.add(storeStructure);

		for(final GedcomStoreLine gLine : storeStructure.getStoreBlock().getStoreLines())
			if(gLine.hasTags())
				for(final String tag : gLine.getTagNames())
					variationsLinksToId.computeIfAbsent(tag, k -> new ArrayList<>())
						.add(storeStructure);

		//create the list of all the variations:
		variations.computeIfAbsent(structureName, k -> new ArrayList<>())
			.add(storeStructure);
	}

	/**
	 * Returns a map which contains all the variations for the structure with the given structure name.
	 */
	public List<GedcomStoreStructure> getVariations(final String structureName){
		return variations.get(structureName);
	}

	/**
	 * Checks if a structure with the given name is available.
	 */
	public boolean hasStructure(final String structureName){
		return idToVariationsLinks.containsKey(structureName);
	}

	public List<GedcomStoreStructure> getStoreStructures(final String tagName){
		return variationsLinksToId.get(tagName);
	}

	/**
	 * Returns a map with all the tags which are available to access the variations of the structure with the given structure name.
	 */
	public List<String> getVariationTags(final String structureName){
		return new ArrayList<>(idToVariationsLinks.get(structureName).keySet());
	}

}
