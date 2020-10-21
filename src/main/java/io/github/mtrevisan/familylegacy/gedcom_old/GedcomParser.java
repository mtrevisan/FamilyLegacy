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
package io.github.mtrevisan.familylegacy.gedcom_old;

import io.github.mtrevisan.familylegacy.gedcom_old.exceptions.GedcomParseException;
import io.github.mtrevisan.familylegacy.gedcom_old.models.ExtensionContainer;
import io.github.mtrevisan.familylegacy.gedcom_old.models.Gedcom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;


public class GedcomParser{

	private static final Logger LOGGER = LoggerFactory.getLogger(GedcomParser.class);

	private static final String GEDCOM_EXTENSION = "ged";


	private final GedcomNode root = new GedcomNode();
	private final Deque<GedcomNode> nodeStack = new ArrayDeque<>();


	/**
	 * Parses the given GEDCOM file.
	 *
	 * @param gedcomFile	The GEDCOM file.
	 */
	public static GedcomNode parse(final String gedcomFile) throws GedcomParseException{
		if(!gedcomFile.endsWith(GEDCOM_EXTENSION))
			throw GedcomParseException.create("Invalid GEDCOM file: only files with extension {} are supported", GEDCOM_EXTENSION);

		try(final InputStream is = GedcomParser.class.getResourceAsStream(gedcomFile)){
			return parse(is);
		}
		catch(final IOException e){
			throw GedcomParseException.create("File {} not found!", gedcomFile);
		}
	}

	/**
	 * Parses the given GEDCOM file.
	 *
	 * @param is	The GEDCOM file.
	 */
	public static GedcomNode parse(final InputStream is) throws GedcomParseException{
		final GedcomParser parser = new GedcomParser();
		return parser.parseGedcom(is);
	}

	private GedcomNode parseGedcom(final InputStream is) throws GedcomParseException{
		LOGGER.info("Parsing GEDCOM file...");

		startDocument();

		int lineCount = 0;
		try(final BufferedReader br = GedcomHelper.getBufferedReader(is)){
			String line;
			int currentLevel;
			int previousLevel = -1;
			while((line = br.readLine()) != null){
				lineCount ++;

				line = line.trim();
				//skip empty lines
				if(line.isEmpty())
					continue;

				//parse the line into five fields: level, ID, tag, xref, value
				final GedcomNode child = GedcomNode.parse(line);
				if(child == null)
					throw GedcomParseException.create("Line {} does not appear to be standard appending content to the last tag started: {}",
						lineCount, line);

				currentLevel = child.getLevel();
				//if level is > prevlevel+1, ignore it until it comes back down
				if(currentLevel > previousLevel + 1)
					throw GedcomParseException.create("Level > prevLevel+1 @ {}", lineCount);
				if(currentLevel < 0)
					throw GedcomParseException.create("Level < 0 @ {}", lineCount);
				final String tag = child.getTag();
				if(tag == null || tag.isEmpty())
					throw GedcomParseException.create("Tag not found @ {}", lineCount);

				//close pending levels
				while(currentLevel <= previousLevel){
					endElement();

					previousLevel --;
				}

				startElement(child);

				previousLevel = currentLevel;
			}

			endElement();
		}
		catch(final IOException | NoSuchMethodException e){
			throw GedcomParseException.create("Failed to read line {}", lineCount);
		}

		LOGGER.info("Parsing done");

		return root;
	}

	private void startDocument(){
		nodeStack.clear();
		nodeStack.push(root);

		root.setObject(new Gedcom());
	}

	private void startElement(final GedcomNode child) throws NoSuchMethodException{
		final GedcomNode parent = nodeStack.peek();

		child.setParent(parent);
		parent.addChild(child);

		final String id = child.getID();
		final String tag = child.getTag();
		final String xref = child.getXRef();
		final Object parentObject = parent.getObject();

		//TODO refactor
		final GedcomTag t = GedcomTag.from(tag);
		if(t != null)
			t.createFrom(child);
		else{
			//unexpected tag
			final GedcomNode obj = new GedcomNode(id, tag, xref);
			if(parentObject instanceof ExtensionContainer)
				addExtension((ExtensionContainer)parentObject, obj);
			else if(parentObject instanceof GedcomNode)
				((GedcomNode)parentObject).addChild(obj);
			else if(parentObject instanceof FieldRef && ((FieldRef)parentObject).getTarget() instanceof ExtensionContainer){
				obj.setParent(nodeStack.peek());
				final ExtensionContainer ec = (ExtensionContainer)((FieldRef)parentObject).getTarget();
				addExtension(ec, obj);
			}
			else
				LOGGER.error("Dropped tag {}", tag);
		}

		//set value:
		final String value = child.getValue();
		if(value != null && !value.isEmpty()){
			final Object obj = child.getObject();
			FieldRef fieldRef = null;
			try{
				if(obj instanceof GedcomNode)
					((GedcomNode)obj).appendValue(value);
				else if(obj instanceof FieldRef){
					fieldRef = (FieldRef)obj;
					fieldRef.appendValue(value);
				}
				else{
					fieldRef = new FieldRef(obj, "Value");
					fieldRef.setValue(value);
				}
			}
			catch(final Exception e){
				if("Value".equals(fieldRef.getFieldName()))
					//this object doesn't have a value field, so drop it
					LOGGER.error("Value '{}' not stored for field '{}', parent '{}', and tag {}", value, fieldRef.getFieldName(),
						(obj != null? obj.getClass().getSimpleName(): null), tag);
				else{
					//if the method does not exists, it's programmer error
					LOGGER.error("Setter for value '{}' does not exists for tag {}, field is {}", value, tag, fieldRef.getClassFieldName());

					throw e;
				}
			}
		}

		nodeStack.push(child);
	}

	private void addExtension(final ExtensionContainer ec, final GedcomNode tag){
		@SuppressWarnings("unchecked")
		Collection<GedcomNode> extensions = (List<GedcomNode>)ec.getExtension(ExtensionContainer.MORE_TAGS_EXTENSION_KEY);
		if(extensions == null){
			extensions = new ArrayList<>();
			ec.putExtension(ExtensionContainer.MORE_TAGS_EXTENSION_KEY, extensions);
		}
		extensions.add(tag);

		LOGGER.warn("Tag added as extension: {}", tag.getTag());
	}

	private void endElement(){
		nodeStack.pop();
	}

}
