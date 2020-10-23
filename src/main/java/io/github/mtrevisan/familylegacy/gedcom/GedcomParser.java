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
import java.util.Arrays;
import java.util.Deque;
import java.util.List;


class GedcomParser{

	private static final Logger LOGGER = LoggerFactory.getLogger(GedcomParser.class);

	private static final String GEDCOM_EXTENSION = "ged";


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
			final GedcomParser parser = new GedcomParser();
			return parser.parseGedcom(is, grammar);
		}
		catch(final IOException e){
			throw GedcomParseException.create("File {} not found!", gedcomFile);
		}
	}

	private GedcomNode parseGedcom(final InputStream is, final GedcomGrammar grammar) throws GedcomParseException{
		LOGGER.info("Parsing GEDCOM file...");

		int lineCount = 0;
		try(final BufferedReader br = GedcomHelper.getBufferedReader(is)){
			startDocument(grammar);

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
					endElement(grammar);

					previousLevel --;
				}

				startElement(child, grammar);

				previousLevel = currentLevel;
			}

			endElement(grammar);

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

	private void startDocument(final GedcomGrammar grammar){
		nodeStack.clear();
		nodeStack.push(root);

		grammarBlockOrLineStack.push(grammar.getRootStructure().getGrammarBlock());
	}

	@SuppressWarnings("ConstantConditions")
	private void startElement(final GedcomNode child, final GedcomGrammar grammar) throws NoSuchMethodException, GedcomParseException{
		final GedcomNode parent = nodeStack.peek();
		final Object parentGrammarBlockOrLine = grammarBlockOrLineStack.peek();

		parent.addChild(child);

if("ADDR".equals(child.getTag()))
	System.out.println();
		final List<GedcomGrammarLine> grammarLines = (parentGrammarBlockOrLine instanceof GedcomGrammarBlock?
			((GedcomGrammarBlock)parentGrammarBlockOrLine).getGrammarLines():
			(((GedcomGrammarLine)parentGrammarBlockOrLine).getChildBlock() != null?
				((GedcomGrammarLine)parentGrammarBlockOrLine).getChildBlock().getGrammarLines():
				Arrays.asList((GedcomGrammarLine)parentGrammarBlockOrLine)));
		GedcomGrammarLine addedGrammarLine = null;
		outer:
		for(final GedcomGrammarLine grammarLine : grammarLines){
			if(grammarLine.getTagNames().contains(child.getTag())){
				//tag found
				addedGrammarLine = grammarLine;
				break outer;
			}

			addedGrammarLine = search(child, grammarLine.getStructureName(), grammar);
		}
		if(addedGrammarLine == null)
			throw GedcomParseException.create("Unknown error");
		grammarBlockOrLineStack.push(addedGrammarLine);

//		final GedcomGrammarLine grammarLine = (parentGrammarBlockLine != null?
//			(parentGrammarBlockLine instanceof GedcomGrammarBlock?
//				((GedcomGrammarBlock)parentGrammarBlockLine).getGrammarLine(child.getTag()):
//				((GedcomGrammarLine)parentGrammarBlockLine)):
//			//extract GEDCOM base structure
//			grammar.getGrammarStructures(TAG_HEAD).get(0).getGrammarBlock().getGrammarLine(TAG_HEAD));

		nodeStack.push(child);
		//NOTE: re-enqueue `parentGrammarBlockOrLine` if a custom tag is encountered (and therefore `grammarLine` is null)
//		grammarBlockOrLineStack.push(grammarLine != null? grammarLine: parentGrammarBlockOrLine);
//grammarBlockOrLineStack.push(parentGrammarBlockOrLine);
	}

	private GedcomGrammarLine search(final GedcomNode child, final String grammarLineStructureName, final GedcomGrammar grammar){
		GedcomGrammarLine addedGrammarLine = null;
		final List<GedcomGrammarStructure> variations = grammar.getVariations(grammarLineStructureName);
		outer:
		for(final GedcomGrammarStructure variation : variations)
			for(final GedcomGrammarLine gLine : variation.getGrammarBlock().getGrammarLines()){
				if(gLine.hasTag(child.getTag())){
					//tag found
					addedGrammarLine = gLine;
					break outer;
				}
				else{
					final GedcomGrammarLine line = search(child, gLine.getStructureName(), grammar);
					if(line != null){
						addedGrammarLine = gLine;
						break outer;
					}
				}
			}
		return addedGrammarLine;
	}

	private void endElement(final GedcomGrammar grammar) throws GedcomParseException{
		final GedcomNode child = nodeStack.pop();
		final Object grammarBlockOrLine = grammarBlockOrLineStack.pop();

		validate(child, grammar, grammarBlockOrLine);
	}

	private void validate(final GedcomNode child, final GedcomGrammar grammar, final Object grammarBlockOrLine) throws GedcomParseException{
		//TODO
	}

}
