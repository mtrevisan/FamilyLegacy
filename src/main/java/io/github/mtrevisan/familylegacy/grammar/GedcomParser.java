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
import io.github.mtrevisan.familylegacy.grammar.exceptions.GedcomParseException;
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


public class GedcomParser{

	private static final Logger LOGGER = LoggerFactory.getLogger(GedcomParser.class);

	private static final String GEDCOM_EXTENSION = "ged";

	private static final String CUSTOM_TAGS_EXTENSION_KEY = "fl.custom_tags";


	private final GedcomNode root = new GedcomNode();
	private final Deque<GedcomNode> nodeStack = new ArrayDeque<>();
	private final Deque<GedcomStoreLine> storeLineStack = new ArrayDeque<>();


	public static void main(String[] args){
		try{
			GedcomStore store = GedcomStore.create("/gedg/gedcomobjects_5.5.1.gedg");
//			GedcomNode node = parse("/ged/Case001-AddressStructure.ged", store);
			GedcomNode node = parse("/ged/complex.ged", store);
			System.out.println(node);
		}
		catch(GedcomGrammarParseException | GedcomParseException e){
			e.printStackTrace();
		}
	}

	/**
	 * Parses the given GEDCOM file.
	 *
	 * @param gedcomFile	The GEDCOM file.
	 */
	public static GedcomNode parse(final String gedcomFile, final GedcomStore store) throws GedcomParseException{
		if(!gedcomFile.endsWith(GEDCOM_EXTENSION))
			throw GedcomParseException.create("Invalid GEDCOM file: only files with extension {} are supported", GEDCOM_EXTENSION);

		try(final InputStream is = GedcomParser.class.getResourceAsStream(gedcomFile)){
			return parse(is, store);
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
	public static GedcomNode parse(final InputStream is, final GedcomStore store) throws GedcomParseException{
		final GedcomParser parser = new GedcomParser();
		return parser.parseGedcom(is, store);
	}

	private GedcomNode parseGedcom(final InputStream is, final GedcomStore store) throws GedcomParseException{
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
				//if level is > previousLevel+1, ignore it until it comes back down
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

				startElement(child, store);

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

		root.setObject(new HashMap<>());
	}

	@SuppressWarnings({"unchecked", "ConstantConditions"})
	private void startElement(final GedcomNode child, final GedcomStore store) throws NoSuchMethodException{
		final GedcomNode parent = nodeStack.peek();
		final GedcomStoreLine parentStoreLine = (!storeLineStack.isEmpty()? storeLineStack.peek(): null);

		parent.addChild(child);

		final String id = child.getID();
		final String tag = child.getTag();
		final String xref = child.getXRef();
		final String value = child.getValue();
		Object parentObject = parent.getObject();
		if(parentObject == null){
			parentObject = new HashMap<>();
			parent.setObject(parentObject);
		}

		final GedcomStoreLine storeLine = (parentStoreLine != null?
			parentStoreLine.getChildBlock().getStoreLine(tag):
			//extract GEDCOM base structure
			store.getStoreStructures("HEAD").get(0).getStoreBlock().getStoreLine("HEAD"));
		if(storeLine != null){
			final Set<String> valueNames = storeLine.getValueNames();
			for(final String valueName : valueNames)
				((Map<String, Object>)parentObject).put(valueName.toLowerCase(), value);
		}
		else if(value != null && tag.charAt(0) == '_'){
			//unexpected tag:
			final GedcomNode obj = new GedcomNode(id, tag, xref);
			obj.setValue(value);
			if(parentObject instanceof Map)
				((Collection<Object>)((Map<String, Object>)parentObject).computeIfAbsent(CUSTOM_TAGS_EXTENSION_KEY,
						k -> new ArrayList<GedcomNode>()))
					.add(obj);
			else if(parentObject instanceof FieldRef && ((FieldRef)parentObject).getTarget() instanceof Map){
				final Map<String, Object> extensionContainer = (Map<String, Object>)((FieldRef)parentObject).getTarget();
				((Collection<Object>)extensionContainer.computeIfAbsent(CUSTOM_TAGS_EXTENSION_KEY, k -> new ArrayList<GedcomNode>()))
					.add(obj);
			}
			else
				LOGGER.error("Dropped tag {}", tag);
		}

		//set value:
		if(value != null && !value.isEmpty()){
			Object obj = child.getObject();
			if(obj == null){
				obj = new HashMap<>();
				child.setObject(obj);
			}
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
						obj.getClass().getSimpleName(), tag);
				else{
					//if the method does not exists, it's programmer error
					LOGGER.error("Setter for value '{}' does not exists for tag {}, field is {}", value, tag, fieldName);

					throw e;
				}
			}
		}

		nodeStack.push(child);
		//NOTE: re-enqueue `parentStoreLine` if a custom tag is encountered (and therefore `storeLine` is null)
		storeLineStack.push(storeLine != null? storeLine: parentStoreLine);
	}

	private void endElement(){
		nodeStack.pop();
		storeLineStack.pop();
	}

}
