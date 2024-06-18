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
package io.github.mtrevisan.familylegacy.flef.gedcom;

import io.github.mtrevisan.familylegacy.services.TimeWatch;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public final class GedcomFileParser{

	private static final Logger LOGGER = LoggerFactory.getLogger(GedcomFileParser.class);

	private static final String GEDCOM_EXTENSION = "ged";

	private GedcomGrammar grammar;

	private GedcomNode root;
	private final Deque<GedcomNode> nodeStack = new LinkedList<>();
	/** Stacks of {@link GedcomGrammarBlock} or {@link GedcomGrammarLine} objects. */
	private final Deque<Object> grammarBlockOrLineStack = new LinkedList<>();

	private final Map<String, List<GedcomNode>> tables = new HashMap<>();


	public void load(final String grammarFile, final String dataFile) throws GedcomGrammarParseException, GedcomParseException{
		parse(grammarFile);

		populate(dataFile);
	}

	public void parse(final String grammarFile) throws GedcomGrammarParseException{
		grammar = GedcomGrammar.create(grammarFile);
	}

	/**
	 * Parses the given GEDCOM file.
	 *
	 * @param dataFile	The GEDCOM file.
	 */
	public void populate(final String dataFile) throws GedcomParseException{
		if(!dataFile.endsWith(GEDCOM_EXTENSION))
			throw new GedcomParseException("Invalid GEDCOM file: only files with extension {} are supported", GEDCOM_EXTENSION);

		final TimeWatch watch = TimeWatch.start();

		LOGGER.info("Parsing GEDCOM format...");

		try(final InputStream is = new FileInputStream(dataFile)){
			root = parseGedcom(is);


			final List<GedcomNode> children = root.children;
			for(int i = 0, length = children.size(); i < length; i++){
				final GedcomNode element = children.get(i);

				final String tag = element.getTag();
				final List<GedcomNode> tagTables = tables.computeIfAbsent(tag, k -> new ArrayList<>());
				tagTables.add(element);
			}
		}
		catch(final IllegalArgumentException | IOException e){
			throw new GedcomParseException((e.getMessage() == null? "GEDCOM file '{}' not found!": e.getMessage()), dataFile);
		}
		finally{
			watch.stop();
			LOGGER.info("Parsed GEDCOM format in {}", watch.toStringMillis());
		}
	}

	private GedcomNode parseGedcom(final InputStream is) throws GedcomParseException{
		root = GedcomNodeBuilder.createRoot();

		int lineCount = -1;
		try(final BufferedReader br = GedcomHelper.getBufferedReader(is)){
			lineCount = 0;
			startDocument();

			final Collection<String> references = new HashSet<>(0);

			String line;
			int currentLevel;
			int previousLevel = -1;
			while((line = br.readLine()) != null){
				lineCount ++;

				//skip empty lines
				if(Character.isWhitespace(line.charAt(0)) || StringUtils.isBlank(line))
					throw new GedcomParseException("GEDCOM file cannot contain an empty line, or a line starting with space, at line {}",
						lineCount);

				//parse the line into five fields: level, ID, tag, xref, value
				final GedcomNode child = GedcomNodeBuilder.parse(line);
				if(child == null)
					throw new GedcomParseException("Line {} does not appear to be a standard appending content to the last tag started: {}",
						lineCount, line);

				currentLevel = child.getLevel();
				//if `currentLevel` is greater than `previousLevel+1`, ignore it until it comes back down
				if(currentLevel > previousLevel + 1)
					throw new GedcomParseException("current-level > previous-level + 1");
				if(currentLevel < 0)
					throw new GedcomParseException("current-level < 0");
				if(child.getTag() == null)
					throw new GedcomParseException("Tag not found");

				//close pending levels
				while(currentLevel <= previousLevel){
					endElement();

					previousLevel --;
				}

				startElement(child);

				if(child.getXRef() != null)
					//collect reference to another node
					references.add(child.getXRef());

				previousLevel = currentLevel;
			}

			endElement();
			//end document
			endElement();

			//check referential integrity:
			final List<GedcomNode> children = root.getChildren();
			final Collection<String> ids = new HashSet<>(children.size());
			for(final GedcomNode child : children)
				ids.add(child.getID());
			ids.add("#DJULIAN");
			ids.add("#DGREGORIAN");
			ids.add("#DFRENCH R");
			ids.add("#DHEBREW");
			references.removeAll(ids);
			if(!references.isEmpty())
				throw new GedcomParseException("Cannot find object for IDs [{}]", String.join(", ", references))
					.skipAddLineNumber();

			return root;
		}
		catch(final IllegalArgumentException e){
			throw e;
		}
		catch(final GedcomParseException gpe){
			if(!gpe.isSkipAddLineNumber())
				throw new GedcomParseException(gpe.getMessage() + " on line {}", lineCount);
			throw gpe;
		}
		catch(final Exception e){
			if(lineCount < 0)
				throw new GedcomParseException("Failed to read file", e);

			throw new GedcomParseException("Failed to read line {}", lineCount);
		}
	}

	private void startDocument(){
		nodeStack.clear();
		nodeStack.push(root);

		grammarBlockOrLineStack.push(grammar.getRootStructure().getGrammarBlock());
	}

	private void startElement(final GedcomNode child) throws GedcomParseException{
		final GedcomNode parent = nodeStack.peek();
		final Object parentGrammarBlockOrLine = grammarBlockOrLineStack.peek();

		parent.forceAddChild(child);

		GedcomGrammarLine addedGrammarLine = selectAddedGrammarLine(child, parentGrammarBlockOrLine)[1];
		if(addedGrammarLine == null){
			//consider as custom tag
			child.setCustom();
			if(parentGrammarBlockOrLine instanceof GedcomGrammarLine)
				addedGrammarLine = GedcomGrammarLineCustom.create(child);
			else
				throw new GedcomParseException("Cannot have custom tag {} here, inside block of {}", child.getTag(),
					parentGrammarBlockOrLine.toString());
		}

		nodeStack.push(child);
		grammarBlockOrLineStack.push(addedGrammarLine);
	}

	//FIXME ugliness
	private GedcomGrammarLine[] selectAddedGrammarLine(final GedcomNode child, final Object parentGrammarBlockOrLine){
		if(!child.isCustomTag()){
			final List<GedcomGrammarLine> grammarLines;
			if(parentGrammarBlockOrLine instanceof GedcomGrammarLine){
				final GedcomGrammarBlock childBlock = ((GedcomGrammarLine)parentGrammarBlockOrLine).getChildBlock();
				grammarLines = (childBlock != null?
					childBlock.getGrammarLines():
					Collections.singletonList((GedcomGrammarLine)parentGrammarBlockOrLine));
			}
			else
				grammarLines = ((GedcomGrammarBlock)parentGrammarBlockOrLine).getGrammarLines();
			for(final GedcomGrammarLine grammarLine : grammarLines){
				if(grammarLine.getTagNames().contains(child.getTag()))
					return new GedcomGrammarLine[]{grammarLine, grammarLine};

				if(grammarLine.getStructureName() != null){
					final GedcomGrammarLine[] addedGrammarLine = selectAddedGrammarLine(child, grammarLine);
					if(addedGrammarLine[1] != null){
						GedcomGrammarLine parentGrammarLine = addedGrammarLine[0];
						if(parentGrammarLine == null)
							parentGrammarLine = (addedGrammarLine[1].getMin() == 1 && addedGrammarLine[1].getMax() == 1? grammarLine:
								addedGrammarLine[1]);
						return new GedcomGrammarLine[]{parentGrammarLine, addedGrammarLine[1]};
					}
				}
			}
		}
		return new GedcomGrammarLine[2];
	}

	//FIXME ugliness
	private GedcomGrammarLine[] selectAddedGrammarLine(final GedcomNode child, final GedcomGrammarLine grammarLine){
		final String grammarLineStructureName = grammarLine.getStructureName();
		final List<GedcomGrammarStructure> variations = grammar.getVariations(grammarLineStructureName);
		for(final GedcomGrammarStructure variation : variations)
			for(final GedcomGrammarLine gLine : variation.getGrammarBlock().getGrammarLines()){
				final boolean hasID = StringUtils.isNotBlank(child.getID());
				final boolean hasXRef = StringUtils.isNotBlank(child.getXRef());
				if(gLine.hasTag(child.getTag()) && (!hasID && !hasXRef && !gLine.hasXRefNames()
						|| hasID && gLine.hasTagAfterXRef()
						|| hasXRef && gLine.hasTagBeforeXRef()))
					return new GedcomGrammarLine[]{null, gLine};

				if(gLine.getStructureName() != null){
					final GedcomGrammarLine[] line = selectAddedGrammarLine(child, gLine);
					if(line[1] != null)
						return new GedcomGrammarLine[]{(line[0] != null? line[0]: (variations.size() > 1? grammarLine: gLine)), line[1]};
				}
			}
		return new GedcomGrammarLine[2];
	}

	private void endElement() throws GedcomParseException{
		final GedcomNode child = nodeStack.pop();
		final Object grammarBlockOrLine = grammarBlockOrLineStack.pop();

		validate(child, grammarBlockOrLine);
	}

	//FIXME ugliness
	private void validate(final GedcomNode node, final Object grammarBlockOrLine) throws GedcomParseException{
		if(grammarBlockOrLine instanceof final GedcomGrammarLine grammarLine){
			//validate min-max constraints:
			final String tag = node.getTag();
			checkConstraints(tag, 1, grammarLine);
			if(!grammarLine.getValuePossibilities().isEmpty() && !grammarLine.getValuePossibilities().contains(node.getValue()))
				throw new GedcomParseException("Value violated on tag {}, should have been one of {}, was {}", tag,
					grammarLine.getValuePossibilities().toString(), node.getValue());


			//validate children:
			final Map<String, List<String>> childrenValueBucket = bucketByTag(node);
			for(final Map.Entry<String, List<String>> entry : childrenValueBucket.entrySet()){
				final GedcomGrammarBlock childBlock = grammarLine.getChildBlock();
				if(childBlock == null)
					throw new GedcomParseException("Children of parent tag does not exists: {}", grammarBlockOrLine);

				final String entryTag = entry.getKey();
				final int entryCount = entry.getValue().size();
				final List<String> entryValues = childrenValueBucket.get(entryTag);

				final List<GedcomNode> childrenWithTag = node.getChildrenWithTag(entryTag);
				for(final GedcomNode childWithTag : childrenWithTag){
					final GedcomGrammarLine[] addedGrammarLine = selectAddedGrammarLine(childWithTag, childBlock);

					checkConstraints(entryTag, entryCount, (addedGrammarLine[0] != null? addedGrammarLine[0]: grammarLine));
					if(addedGrammarLine[1] != null && !addedGrammarLine[1].getValuePossibilities().isEmpty()
							&& !addedGrammarLine[1].getValuePossibilities().containsAll(entryValues))
						throw new GedcomParseException("Value violated on tag {}, should have been one of {}, was {}", tag,
							addedGrammarLine[1].getValuePossibilities().toString(), entryValues.toString());
				}
			}
		}
		else{
			//validate children of root:
			if(!nodeStack.isEmpty())
				throw new GedcomParseException("Badly formatted GEDCOM, tags are not properly closed");

			final GedcomGrammarBlock grammarBlock = (GedcomGrammarBlock)grammarBlockOrLine;
			final Map<String, List<String>> childrenValueBucket = bucketByTag(node);
			for(final Map.Entry<String, List<String>> entry : childrenValueBucket.entrySet()){
				final String entryTag = entry.getKey();
				final int entryCount = entry.getValue().size();
				final List<String> entryValues = childrenValueBucket.get(entryTag);

				final List<GedcomNode> childrenWithTag = node.getChildrenWithTag(entryTag);
				for(final GedcomNode childWithTag : childrenWithTag){
					final GedcomGrammarLine[] addedGrammarLine = selectAddedGrammarLine(childWithTag, grammarBlock);

					checkConstraints(entryTag, entryCount, addedGrammarLine[0]);
					if(addedGrammarLine[1] != null && !addedGrammarLine[1].getValuePossibilities().isEmpty()
							&& !addedGrammarLine[1].getValuePossibilities().containsAll(entryValues))
						throw new GedcomParseException("Value violated on tag {}, should have been one of {}, was {}", entryTag,
							addedGrammarLine[1].getValuePossibilities().toString(), entryValues.toString());
				}
			}
		}
	}

	/** Bucket children of nodes by tag. */
	private Map<String, List<String>> bucketByTag(final GedcomNode node){
		final Map<String, List<String>> childrenValueBucket = new HashMap<>(node.getChildren().size());
		for(final GedcomNode gedcomNode : node.getChildren())
			//don't count custom tags
			if(!gedcomNode.isCustom())
				childrenValueBucket.computeIfAbsent(gedcomNode.getTag(), k -> new ArrayList<>(1))
					.add(gedcomNode.getValue());
		return childrenValueBucket;
	}

	private void checkConstraints(final String tag, final Integer count, final GedcomGrammarLine grammarLine)
			throws GedcomParseException{
		final int min = grammarLine.getMin();
		if(count < min)
			throw new GedcomParseException("Minimum constraint violated on tag {}, should have been at least {}, was {}", tag,
				min, count);
		final int max = grammarLine.getMax();
		if(max != -1 && max < count)
			throw new GedcomParseException("Maximum constraint violated on tag {}, should have been at most {}, was {}", tag,
				max, count);
	}


	public static void main(final String[] args) throws GedcomGrammarParseException, GedcomParseException{
		final GedcomFileParser parser = new GedcomFileParser();
		parser.parse("/gedg/gedcom_5.5.1.tcgb.gedg");
		parser.populate("src/main/resources/ged/large.ged");

		parser.root.children
			.forEach(System.out::println);
	}

}
