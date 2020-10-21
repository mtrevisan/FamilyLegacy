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

import io.github.mtrevisan.familylegacy.services.JavaHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


class GedcomParser{

	private static final Logger LOGGER = LoggerFactory.getLogger(GedcomParser.class);

	private static final String GEDCOM_EXTENSION = "ged";

	private static final String CUSTOM_TAGS_EXTENSION_KEY = "fl.custom_tags";


	private final GedcomNode root = GedcomNode.createEmpty();
	private final Deque<GedcomNode> nodeStack = new ArrayDeque<>();
	private final Deque<GedcomGrammarLine> grammarLineStack = new ArrayDeque<>();


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
			startDocument();

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

				startElement(child, grammar);

				previousLevel = currentLevel;
			}

			endElement();

			LOGGER.info("Parsing done");

			return root;
		}
		catch(final Exception e){
			throw GedcomParseException.create("Failed to read line {}", lineCount);
		}
	}

	private void startDocument(){
		nodeStack.clear();
		nodeStack.push(root);
	}

	@SuppressWarnings("ConstantConditions")
	private void startElement(final GedcomNode child, final GedcomGrammar grammar) throws NoSuchMethodException{
		final GedcomNode parent = nodeStack.peek();
		final GedcomGrammarLine parentGrammarLine = (!grammarLineStack.isEmpty()? grammarLineStack.peek(): null);

		parent.addChild(child);

		final GedcomGrammarLine grammarLine = (parentGrammarLine != null?
			parentGrammarLine.getChildBlock().getGrammarLine(child.getTag()):
			//extract GEDCOM base structure
			grammar.getGrammarStructures("HEAD").get(0).getGrammarBlock().getGrammarLine("HEAD"));
		storeParameter(child, parent, grammarLine);

		setValue(child);

		nodeStack.push(child);
		//NOTE: re-enqueue `parentGrammarLine` if a custom tag is encountered (and therefore `grammarLine` is null)
		grammarLineStack.push(grammarLine != null? grammarLine: parentGrammarLine);
	}

	@SuppressWarnings("unchecked")
	private void storeParameter(final GedcomNode child, final GedcomNode parent, final GedcomGrammarLine grammarLine){
		final String value = child.getValue();
		if(value != null){
			if(grammarLine != null){
				final Set<String> valueNames = grammarLine.getValueNames();
				if(!valueNames.isEmpty()){
					final Object parentObject = JavaHelper.nonNullOrDefault(parent.getObject(), new HashMap<>());
					for(final String valueName : valueNames)
						((Map<String, Object>)parentObject).put(valueName.toLowerCase(), value);
					parent.setObject(parentObject);
				}
			}
			else if(child.getTag().charAt(0) == '_'){
				final Object parentObject = JavaHelper.nonNullOrDefault(parent.getObject(), new HashMap<>());
				if(handleUnexpectedTag(child, parentObject))
					parent.setObject(parentObject);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private boolean handleUnexpectedTag(final GedcomNode child, final Object parentObject){
		boolean added = false;
		final String tag = child.getTag();
		if(parentObject instanceof Map)
			added = addCustomTags((Map<String, Object>)parentObject, child);
		else if(parentObject instanceof FieldRef && ((FieldRef) parentObject).getTarget() instanceof Map){
			final Map<String, Object> extensionContainer = (Map<String, Object>)((FieldRef)parentObject).getTarget();
			added = addCustomTags(extensionContainer, child);
		}
		else
			LOGGER.error("Dropped tag {}", tag);
		return added;
	}

	@SuppressWarnings("unchecked")
	public boolean addCustomTags(final Map<String, Object> map, final GedcomNode value){
		return ((Collection<Object>)map.computeIfAbsent(CUSTOM_TAGS_EXTENSION_KEY, k -> new ArrayList<GedcomNode>()))
			.add(value);
	}

	@SuppressWarnings("unchecked")
	public static Object getCustomTags(final GedcomNode node){
		return (node.getObject() instanceof Map? ((Map<String, Object>)node.getObject()).get(CUSTOM_TAGS_EXTENSION_KEY): null);
	}

	private void setValue(final GedcomNode child) throws NoSuchMethodException{
		final String value = child.getValue();
		if(value != null){
			final Object obj = child.getObject();
			FieldRef fieldRef = null;
			try{
				if(obj instanceof GedcomNode)
					((GedcomNode)obj).appendValue(value);
				else if(obj instanceof FieldRef){
					fieldRef = (FieldRef)obj;
					fieldRef.appendValue(value);
				}
				//otherwise do nothing, the `value` is already valued
			}
			catch(final Exception e){
				final String fieldName = (fieldRef != null? fieldRef.getFieldName(): null);
				if("value".equals(fieldName))
					//this object doesn't have a value field, so drop it
					LOGGER.error("Value '{}' not stored for field '{}', parent '{}', and tag {}", value, fieldName,
						obj.getClass().getSimpleName(), child.getTag());
				else{
					//if the method does not exists, it's programmer error
					LOGGER.error("Setter for value '{}' does not exists for tag {}, field is {}", value, child.getTag(), fieldName);

					throw e;
				}
			}
		}
	}

	private void endElement(){
		nodeStack.pop();
		grammarLineStack.pop();
	}

}
