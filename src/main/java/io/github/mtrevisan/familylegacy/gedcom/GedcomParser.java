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
import java.util.Collections;
import java.util.Deque;
import java.util.List;


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
			final GedcomParser parser = new GedcomParser(grammar);
			return parser.parseGedcom(is);
		}
		catch(final IOException e){
			throw GedcomParseException.create("File {} not found!", gedcomFile);
		}
	}

	private GedcomParser(final GedcomGrammar grammar){
		this.grammar = grammar;
	}

	private GedcomNode parseGedcom(final InputStream is) throws GedcomParseException{
		LOGGER.info("Parsing GEDCOM file...");

		int lineCount = 0;
		try(final BufferedReader br = GedcomHelper.getBufferedReader(is)){
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

			if(!nodeStack.isEmpty() || nodeStack.size() != grammarBlockOrLineStack.size())
				throw GedcomParseException.create("Badly formatted GEDCOM, tags are not properly closed");

			LOGGER.info("Parsing done");

			return root;
		}
		catch(final GedcomParseException e){
			throw GedcomParseException.create(e.getMessage() + " on line {}", lineCount);
		}
		catch(final Exception e){
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

		GedcomGrammarLine addedGrammarLine = selectAddedGrammarLine(child, parentGrammarBlockOrLine);
		if(addedGrammarLine == null){
			//consider as custom tag
			if(parentGrammarBlockOrLine instanceof GedcomGrammarLine)
				addedGrammarLine = GedcomGrammarLineCustom.getInstance();
			else
				throw GedcomParseException.create("Cannot have custom tag {} here, inside block of {}", child.getTag(),
					parentGrammarBlockOrLine.toString());
		}

		nodeStack.push(child);
		grammarBlockOrLineStack.push(addedGrammarLine);
	}

	private GedcomGrammarLine selectAddedGrammarLine(final GedcomNode child, final Object parentGrammarBlockOrLine){
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
					return grammarLine;

				if(grammarLine.getStructureName() != null){
					final GedcomGrammarLine addedGrammarLine = selectAddedGrammarLine(child, grammarLine.getStructureName());
					if(addedGrammarLine != null)
						return addedGrammarLine;
				}
			}
		}
		return null;
	}

	private GedcomGrammarLine selectAddedGrammarLine(final GedcomNode child, final String grammarLineStructureName){
		final List<GedcomGrammarStructure> variations = grammar.getVariations(grammarLineStructureName);
		for(final GedcomGrammarStructure variation : variations)
			for(final GedcomGrammarLine gLine : variation.getGrammarBlock().getGrammarLines()){
				if(gLine.hasTag(child.getTag()))
					return gLine;

				if(gLine.getStructureName() != null){
					final GedcomGrammarLine line = selectAddedGrammarLine(child, gLine.getStructureName());
					if(line != null)
						return line;
				}
			}
		return null;
	}

	private void endElement() throws GedcomParseException{
		final GedcomNode child = nodeStack.pop();
		final Object grammarBlockOrLine = grammarBlockOrLineStack.pop();

		validate(child, grammarBlockOrLine);
	}

	private void validate(final GedcomNode child, final Object grammarBlockOrLine) throws GedcomParseException{
		if(grammarBlockOrLine instanceof GedcomGrammarLine){
			final GedcomGrammarLine grammarLine = (GedcomGrammarLine)grammarBlockOrLine;
			final int min = grammarLine.getMin();
			if(min > 1)
				throw GedcomParseException.create("Minimum constraint violated, should have been at least {}, was 1", min);
			final int max = grammarLine.getMax();
			if(max != -1 && max < 1)
				throw GedcomParseException.create("Maximum constraint violated, should have been at most {}, was 1", max);

			//TODO validate children
		}
		else{
			//TODO
			System.out.println();
		}
	}

}
