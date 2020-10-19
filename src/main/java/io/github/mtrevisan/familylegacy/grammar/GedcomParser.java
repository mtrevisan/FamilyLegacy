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

import io.github.mtrevisan.familylegacy.grammar.exceptions.GedcomParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class GedcomParser{

	private static final Logger LOGGER = LoggerFactory.getLogger(GedcomParser.class);

	private static final String GEDCOM_EXTENSION = "ged";


	public static void main(String[] args){
		try{
			GedcomParser store = GedcomParser.parse("/Case001-AddressStructure.ged");
			System.out.println(store);
		}
		catch(GedcomParseException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Parses the given GEDCOM file.
	 *
	 * @param gedcomFile	The GEDCOM file.
	 */
	public static GedcomParser parse(final String gedcomFile) throws GedcomParseException{
		if(!gedcomFile.endsWith(GEDCOM_EXTENSION))
			throw new GedcomParseException("Invalid GEDCOM file: only files with extension " + GEDCOM_EXTENSION + " are supported");

		try(final InputStream is = GedcomParser.class.getResourceAsStream(gedcomFile)){
			return parse(is);
		}
		catch(final IOException e){
			throw new GedcomParseException("File " + gedcomFile + " not found!");
		}
	}

	/**
	 * Parses the given GEDCOM file.
	 *
	 * @param is	The GEDCOM file.
	 */
	public static GedcomParser parse(final InputStream is) throws GedcomParseException{
		final GedcomParser parser = new GedcomParser();
		parser.parseGedcom(is);
		return parser;
	}

	private void parseGedcom(final InputStream is) throws GedcomParseException{
		LOGGER.info("Parsing GEDCOM file...");

		int lineCount = 0;
		final List<GedcomLine> gedcomLines = new ArrayList<>();
		try(final BufferedReader br = GedcomHelper.getBufferedReader(is)){
			String line;
			int currentLevel;
			int previousLevel = -1;
			while((line = br.readLine()) != null){
				line = line.trim();
				lineCount ++;

				//skip empty lines
				if(line.isEmpty())
					continue;

				//parse the line into five fields: level, ID, tag, xref, value
				final GedcomLine gLine = GedcomLine.parse(line);
				if(gLine == null)
					throw new GedcomParseException("Line does not appear to be standard @ " + lineCount
						+ " appending content to the last tag started: " + line);

				currentLevel = gLine.getLevel();
				final String tag = gLine.getTag();

				//if level is > prevlevel+1, ignore it until it comes back down
				if(currentLevel > previousLevel + 1)
					throw new GedcomParseException("Level > prevLevel+1 @ " + lineCount);
				if(currentLevel < 0)
					throw new GedcomParseException("Level < 0 @ " + lineCount);
				if(tag == null || tag.isEmpty())
					throw new GedcomParseException("Tag not found @ " + lineCount);

				gedcomLines.add(gLine);

				previousLevel = currentLevel;
			}
		}
		catch(final IOException e){
			throw new GedcomParseException("Failed to read line " + lineCount);
		}

		LOGGER.info("Parsing done");
	}

}
