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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


final class GedcomParser{

	private static final Logger LOGGER = LoggerFactory.getLogger(GedcomParser.class);

	private static final String GEDCOM_EXTENSION = "ged";

	private final GedcomGrammar grammar;

	private final GedcomNode root = GedcomNode.createEmpty();
	private final Deque<GedcomNode> nodeStack = new ArrayDeque<>();
	/** Stacks of {@link GedcomGrammarBlock} or {@link GedcomGrammarLine} objects. */
	private final Deque<Object> grammarBlockOrLineStack = new ArrayDeque<>();


	/**
	 * Parses the given GEDCOM file.
	 *
	 * @param gedcomFile	The GEDCOM file.
	 */
	public static GedcomNode parse(final String gedcomFile, final GedcomGrammar grammar) throws GedcomParseException{
		if(!gedcomFile.endsWith(GEDCOM_EXTENSION))
			throw GedcomParseException.create("Invalid GEDCOM file: only files with extension {} are supported", GEDCOM_EXTENSION);

		try(final InputStream is = GedcomParser.class.getResourceAsStream(gedcomFile)){
			if(is == null)
				throw new IllegalArgumentException();

			final GedcomParser parser = new GedcomParser(grammar);
			return parser.parseGedcom(is);
		}
		catch(final IllegalArgumentException | IOException e){
			throw GedcomParseException.create("GEDCOM file '{}' not found!", gedcomFile);
		}
	}

	private GedcomParser(final GedcomGrammar grammar){
		this.grammar = grammar;
	}

	private GedcomNode parseGedcom(final InputStream is) throws GedcomParseException{
		LOGGER.info("Parsing GEDCOM file...");

		int lineCount = -1;
		try(final BufferedReader br = GedcomHelper.getBufferedReader(is)){
			lineCount = 0;
			startDocument();

			String line;
			int currentLevel;
			int previousLevel = -1;
			while((line = br.readLine()) != null){
				lineCount ++;

				//skip empty lines
				if(line.charAt(0) == ' ' || line.charAt(0) == '\t' || line.trim().isEmpty())
					throw GedcomParseException.create("GEDCOM file cannot contain an empty line, or a line starting with space, at line {}",
						lineCount);

				//parse the line into five fields: level, ID, tag, xref, value
				final GedcomNode child = GedcomNode.parse(line);
				if(child == null)
					throw GedcomParseException.create("Line {} does not appear to be a standard appending content to the last tag started: {}",
						lineCount, line);

				currentLevel = child.getLevel();
				//if `currentLevel` is greater than `previousLevel+1`, ignore it until it comes back down
				if(currentLevel > previousLevel + 1)
					throw GedcomParseException.create("Current-level > previous-level + 1 at line {}", lineCount);
				if(currentLevel < 0)
					throw GedcomParseException.create("Current-level < 0 at line {}", lineCount);
				if(child.getTag() == null)
					throw GedcomParseException.create("Tag not found at line {}", lineCount);

				//close pending levels
				while(currentLevel <= previousLevel){
					endElement();

					previousLevel --;
				}

				startElement(child);

				previousLevel = currentLevel;
			}

			endElement();
			//end document
			endElement();

			//TODO check referential integrity

			LOGGER.info("Parsing done");

			return root;
		}
		catch(final IllegalArgumentException e){
			throw e;
		}
		catch(final GedcomParseException e){
			throw GedcomParseException.create(e.getMessage() + " on line {}", lineCount);
		}
		catch(final Exception e){
			if(lineCount < 0)
				throw GedcomParseException.create("Failed to read file", e);
			throw GedcomParseException.create("Failed to read line {}", lineCount);
		}
	}

	private void startDocument(){
		nodeStack.clear();
		nodeStack.push(root);

		grammarBlockOrLineStack.push(grammar.getRootStructure().getGrammarBlock());
	}

	@SuppressWarnings("ConstantConditions")
	private void startElement(final GedcomNode child) throws GedcomParseException{
		final GedcomNode parent = nodeStack.peek();
		final Object parentGrammarBlockOrLine = grammarBlockOrLineStack.peek();

		parent.addChild(child);

		GedcomGrammarLine addedGrammarLine = selectAddedGrammarLine(child, parentGrammarBlockOrLine)[1];
		if(addedGrammarLine == null){
			//consider as custom tag
			child.setCustom();
			if(parentGrammarBlockOrLine instanceof GedcomGrammarLine)
				addedGrammarLine = GedcomGrammarLineCustom.getInstance();
			else
				throw GedcomParseException.create("Cannot have custom tag {} here, inside block of {}", child.getTag(),
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
				if(gLine.hasTag(child.getTag()))
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
		if(grammarBlockOrLine instanceof GedcomGrammarLine){
			final GedcomGrammarLine grammarLine = (GedcomGrammarLine)grammarBlockOrLine;

			//validate min-max constraints:
			final String tag = node.getTag();
			checkConstraints(tag, 1, grammarLine);
			if(!grammarLine.getValuePossibilities().isEmpty() && !grammarLine.getValuePossibilities().contains(node.getValue()))
				throw GedcomParseException.create("Value violated on tag {}, should have been one of {}, was {}", tag,
					grammarLine.getValuePossibilities().toString(), node.getValue());


			//validate children:
			final Map<String, List<String>> childrenValueBucket = bucketByTag(node);
			for(final Map.Entry<String, List<String>> entry : childrenValueBucket.entrySet()){
				final GedcomGrammarBlock childBlock = grammarLine.getChildBlock();
				if(childBlock == null)
					throw GedcomParseException.create("Children of parent tag does not exists");

				final String entryTag = entry.getKey();
				final int entryCount = entry.getValue().size();
				final List<String> entryValues = childrenValueBucket.get(entryTag);

				final List<GedcomNode> childrenWithTag = node.getChildrenWithTag(entryTag);
				for(final GedcomNode childWithTag : childrenWithTag){
					final GedcomGrammarLine[] addedGrammarLine = selectAddedGrammarLine(childWithTag, childBlock);

					checkConstraints(entryTag, entryCount, (addedGrammarLine[0] != null? addedGrammarLine[0]: grammarLine));
					if(addedGrammarLine[1] != null && !addedGrammarLine[1].getValuePossibilities().isEmpty()
							&& !addedGrammarLine[1].getValuePossibilities().containsAll(entryValues))
						throw GedcomParseException.create("Value violated on tag {}, should have been one of {}, was {}", tag,
							addedGrammarLine[1].getValuePossibilities().toString(), entryValues.toString());
				}
			}
		}
		else{
			//validate children of root:
			if(!nodeStack.isEmpty())
				throw GedcomParseException.create("Badly formatted GEDCOM, tags are not properly closed");

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
						throw GedcomParseException.create("Value violated on tag {}, should have been one of {}, was {}", entryTag,
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
			throw GedcomParseException.create("Minimum constraint violated on tag {}, should have been at least {}, was {}", tag,
				min, count);
		final int max = grammarLine.getMax();
		if(max != -1 && max < count)
			throw GedcomParseException.create("Maximum constraint violated on tag {}, should have been at most {}, was {}", tag,
				max, count);
	}

}
