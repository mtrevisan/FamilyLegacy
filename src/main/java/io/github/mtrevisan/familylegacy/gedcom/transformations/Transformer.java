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
package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNodeBuilder;
import io.github.mtrevisan.familylegacy.services.RegexHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class Transformer{

	private static final Collection<String> ADDRESS_TAGS = new HashSet<>(Arrays.asList("CONT", "ADR1", "ADR2", "ADR3"));

	//tag, or tag{value}, tag[index], or tag{value}[index], or tag#id, or tag#id{value}
	private static final String PARAM_TAG = "tag";
	private static final String PARAM_VALUE = "value";
	private static final String PARAM_INDEX = "index";
	private static final String PARAM_ID = "id";
	private static final String PATH_COMPONENT = "[^#{}\\[\\])]+";
	private static final String PATH_COMPONENT_OPTIONAL = "[^#{}\\[\\])]*";
	private static final String PATH_COMPONENT_TAG = "(?<" + PARAM_TAG + ">" + PATH_COMPONENT + ")";
	private static final String PATH_COMPONENT_VALUE = "\\{(?<" + PARAM_VALUE + ">" + PATH_COMPONENT + ")\\}";
	private static final String PATH_COMPONENT_INDEX = "\\[(?<" + PARAM_INDEX + ">" + PATH_COMPONENT_OPTIONAL + ")\\]";
	private static final String PATH_COMPONENT_ID = "#(?<" + PARAM_ID + ">" + PATH_COMPONENT + ")";
	private static final Pattern PATH_COMPONENTS = RegexHelper.pattern(
		PATH_COMPONENT_TAG + "?(?:" + PATH_COMPONENT_ID + ")?(?:" + PATH_COMPONENT_VALUE + ")?(?:" + PATH_COMPONENT_INDEX + ")?"
	);


	private final Protocol protocol;


	public Transformer(final Protocol protocol){
		this.protocol = protocol;
	}


	public GedcomNode createEmpty(){
		return GedcomNodeBuilder.createEmpty(protocol);
	}

	public GedcomNode create(final String tag){
		return GedcomNodeBuilder.create(protocol, tag);
	}

	public GedcomNode create(final String tag, final String id, final String value){
		return GedcomNodeBuilder.create(protocol, tag, id, value);
	}

	String joinIfNotNull(final String separator, final String... components){
		final StringJoiner sj = new StringJoiner(separator);
		for(final String component : components)
			if(component != null)
				sj.add(component);
		return (sj.length() > 0? sj.toString(): null);
	}

	/**
	 * @param origin	Origin node from which to start the traversal.
	 * @param path	The path to follow from the origin in the form `tag#id{value}[index]` or `(tag1|tag2)#id{value}[index]` and separated by dots.
	 * @return	The final node.
	 */
	public GedcomNode traverse(final GedcomNode origin, final String path){
		return (GedcomNode)traverseInner(origin, path);
	}

	/**
	 * @param origin	Origin node from which to start the traversal.
	 * @param path	The path to follow from the origin in the form `tag#id{value}[]` or `(tag1|tag2)#id{value}[]` and separated by dots.
	 * 	<p>The void array MUST BE last in the sequence.</p>
	 * @return	The final node list.
	 */
	@SuppressWarnings("unchecked")
	public List<GedcomNode> traverseAsList(final GedcomNode origin, String path){
		if(path.charAt(path.length() - 1) == ']' && path.charAt(path.length() - 2) != '[')
			throw new IllegalArgumentException("The array indication `[]` must be last in the path, was " + path);
		else if(path.charAt(path.length() - 1) != ']' && path.charAt(path.length() - 2) != '[')
			path = path + "[]";
		return (List<GedcomNode>)traverseInner(origin, path);
	}

	private Object traverseInner(final GedcomNode origin, final String path){
		Object pointer = origin;
		final String[] components = StringUtils.split(path, '.');
		for(final String component : components){
			if(pointer instanceof List)
				throw new IllegalArgumentException("Only the last step of the path can produce an array, was " + path);

			final Matcher m = RegexHelper.matcher(component, PATH_COMPONENTS);
			if(m.find()){
				final String tag = m.group(PARAM_TAG);
				final String value = m.group(PARAM_VALUE);
				final String index = m.group(PARAM_INDEX);
				final String id = m.group(PARAM_ID);

				final List<GedcomNode> nodes = new ArrayList<>(((GedcomNode)pointer).getChildren());
				if(tag != null){
					final String[] tags = (tag.charAt(0) == '(' && tag.charAt(tag.length() - 1) == ')'?
						StringUtils.split(tag.substring(1, tag.length() - 1), '|'): new String[]{tag});
					Arrays.sort(tags);
					final Iterator<GedcomNode> itr = nodes.iterator();
					while(itr.hasNext())
						if(Arrays.binarySearch(tags, itr.next().getTag()) < 0)
							itr.remove();
				}
				if(value != null){
					final Iterator<GedcomNode> itr = nodes.iterator();
					while(itr.hasNext())
						if(!value.equals(itr.next().getValue()))
							itr.remove();
				}
				if(id != null){
					final Iterator<GedcomNode> itr = nodes.iterator();
					while(itr.hasNext())
						if(!id.equals(itr.next().getID()))
							itr.remove();
				}
				if(index == null){
					final int size = nodes.size();
					if(size > 1)
						throw new IllegalArgumentException("More than one node is selected from path " + path);
					else if(size == 1)
						pointer = nodes.get(0);
					else{
						pointer = GedcomNodeBuilder.createEmpty(protocol);
						break;
					}
				}
				else if(index.length() == 0){
					pointer = nodes;
				}
				else{
					pointer = nodes.get(Integer.parseInt(index));
				}
			}
			else
				throw new IllegalArgumentException("Illegal path " + path);
		}
		return pointer;
	}


	void eventTo(final GedcomNode individual, final GedcomNode destinationNode, final Flef destination,
			final String tagFrom, final String valueTo){
		final List<GedcomNode> events = individual.getChildrenWithTag(tagFrom);
		for(final GedcomNode event : events){
			final GedcomNode destinationEvent = createEventTo(valueTo, event, destination);
			destinationNode.addChild(destinationEvent);
		}
	}

	void documentTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> documents = parent.getChildrenWithTag("OBJE");
		for(final GedcomNode document : documents){
			String documentID = document.getID();
			if(documentID == null){
				final String documentFormat = traverse(document, "FORM")
					.getValue();
				final String documentMedia = traverse(document, "FORM.MEDI")
					.getValue();

				final GedcomNode destinationDocument = GedcomNodeBuilder.create(protocol, "SOURCE")
					.addChildValue("TITLE", traverse(document, "TITL")
						.getValue());
				if(documentFormat != null || documentMedia != null)
					destinationDocument.addChild(GedcomNodeBuilder.create(protocol, "FILE")
						.withValue(traverse(document, "FILE")
							.getValue())
						.addChildValue("FORMAT", documentFormat)
						.addChildValue("MEDIA", documentMedia)
						.addChildValue("CUT", traverse(document, "_CUTD")
							.getValue())
						.addChildValue("PREFERRED", traverse(document, "_PREF")
							.getValue())
					);
				documentID = destination.addSource(destinationDocument);
			}
			destinationNode.addChildReference("SOURCE", documentID);
		}
	}

	void sourceCitationTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> sourceCitations = parent.getChildrenWithTag("SOUR");
		for(final GedcomNode sourceCitation : sourceCitations){
			String sourceCitationID = sourceCitation.getID();
			if(sourceCitationID == null){
				//create source:
				final String noteID = destination.addNote(GedcomNodeBuilder.create(protocol, "NOTE")
					.withValue(sourceCitation.getValue()));
				final GedcomNode destinationSource = GedcomNodeBuilder.create(protocol, "SOURCE")
					.addChildValue("EXTRACT", traverse(sourceCitation, "TEXT")
						.getValue())
					.addChildReference("NOTE", noteID);
				documentTo(sourceCitation, destinationSource, destination);
				noteTo(sourceCitation, destinationSource, destination);
				sourceCitationID = destination.addSource(destinationSource);

				//add source citation
				destinationNode.addChild(GedcomNodeBuilder.create(protocol, "SOURCE")
					.withID(sourceCitationID)
					.addChildValue("CREDIBILITY", traverse(sourceCitation, "QUAY")
						.getValue()));
			}
			else{
				//create source:
				final String noteID = destination.addNote(GedcomNodeBuilder.create(protocol, "NOTE")
					.withValue(sourceCitation.getValue()));
				final GedcomNode eventNode = traverse(sourceCitation, "EVEN");
				final GedcomNode data = traverse(sourceCitation, "DATA");
				final GedcomNode destinationSource = GedcomNodeBuilder.create(protocol, "SOURCE")
					.addChildValue("EVENT", eventNode.getValue())
					.addChildValue("DATE", traverse(data, "DATE")
						.getValue());
				final List<GedcomNode> texts = data.getChildrenWithTag( "EXTRACT");
				for(final GedcomNode text : texts)
					destinationSource.addChildValue("TEXT", text
						.getValue());
				destinationSource.addChildReference("NOTE", noteID);
				documentTo(sourceCitation, destinationSource, destination);
				noteTo(sourceCitation, destinationSource, destination);
				sourceCitationID = destination.addSource(destinationSource);

				//add source citation
				destinationNode.addChild(GedcomNodeBuilder.create(protocol, "SOURCE")
					.withID(sourceCitationID)
					.addChildValue("PAGE", traverse(sourceCitation, "PAGE")
						.getValue())
					.addChildValue("ROLE", traverse(eventNode, "ROLE")
						.getValue())
					.addChildValue("CREDIBILITY", traverse(sourceCitation, "QUAY")
						.getValue()));
			}
		}
	}

	void addressStructureTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final GedcomNode address = traverse(parent, "ADDR");
		final String addressValue = extractAddressValue(address);

		final GedcomNode destinationPlace = GedcomNodeBuilder.create(protocol, "PLACE")
			.addChildValue("ADDRESS", addressValue)
			.addChildValue("CITY", traverse(address, "CITY")
				.getValue())
			.addChildValue("STATE", traverse(address, "STAE")
				.getValue())
			.addChildValue("COUNTRY", traverse(address, "CTRY")
				.getValue());
		final String destinationPlaceID = destination.addPlace(destinationPlace);
		destinationNode.addChildReference("PLACE", destinationPlaceID);
	}

	private GedcomNode createEventTo(final String valueTo, final GedcomNode event, final Flef destination){
		final GedcomNode destinationEvent = GedcomNodeBuilder.create(protocol, "EVENT")
			.withValue("EVENT".equals(valueTo)? event.getValue(): valueTo)
			.addChildValue("TYPE", traverse(event, "TYPE")
				.getValue())
			.addChildValue("DATE", traverse(event, "DATE")
				.getValue());
		placeAddressStructureTo(event, destinationEvent, destination);
		destinationEvent.addChildValue("AGENCY", traverse(event, "AGNC")
			.getValue())
			.addChildValue("CAUSE", traverse(event, "CAUS")
				.getValue());
		noteTo(event, destinationEvent, destination);
		sourceCitationTo(event, destinationEvent, destination);
		documentTo(event, destinationEvent, destination);
		final GedcomNode familyChild = traverse(event, "FAMC");
		destinationEvent.addChildValue("RESTRICTION", traverse(event, "RESN")
			.getValue())
			.addChild(GedcomNodeBuilder.create(protocol, "FAMILY_CHILD")
				.withID(familyChild.getID())
				.addChildValue("ADOPTED_BY", traverse(familyChild, "ADOP")
					.getValue())
			);
		return destinationEvent;
	}

	void placeAddressStructureTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final GedcomNode address = traverse(parent, "ADDR");
		final String addressValue = extractAddressValue(address);

		final GedcomNode place = traverse(parent, "PLAC");
		if(!address.isEmpty() || !place.isEmpty()){
			final GedcomNode map = traverse(place, "MAP");
			final GedcomNode destinationPlace = GedcomNodeBuilder.create(protocol, "PLACE")
				.withValue(place.getValue())
				.addChildValue("ADDRESS", addressValue)
				.addChildValue("CITY", traverse(address, "CITY")
					.getValue())
				.addChildValue("STATE", traverse(address, "STAE")
					.getValue())
				.addChildValue("COUNTRY", traverse(address, "CTRY")
					.getValue())
				.addChild(GedcomNodeBuilder.create(protocol, "MAP")
					.addChildValue("LATITUDE", traverse(map, "LATI")
						.getValue())
					.addChildValue("LONGITUDE", traverse(map, "LONG")
						.getValue())
				);
			noteTo(place, destinationPlace, destination);
			final String destinationPlaceID = destination.addPlace(destinationPlace);
			destinationNode.addChildReference("PLACE", destinationPlaceID);
		}
	}

	private String extractAddressValue(final GedcomNode address){
		final StringJoiner sj = new StringJoiner(" - ");
		final String wholeAddress = address.getValue();
		if(wholeAddress != null)
			sj.add(wholeAddress);
		for(final GedcomNode child : address.getChildren())
			if(ADDRESS_TAGS.contains(child.getTag())){
				final String value = child.getValue();
				if(value != null)
					sj.add(value);
			}
		return (sj.length() > 0? sj.toString(): null);
	}

	void noteTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> notes = parent.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes){
			String noteID = note.getID();
			if(noteID == null)
				noteID = destination.addNote(GedcomNodeBuilder.create(protocol, "NOTE")
					.withValue(note.getValue()));
			destinationNode.addChildReference("NOTE", noteID);
		}
	}


	void eventFrom(final Iterable<GedcomNode> events, final GedcomNode destinationNode, final Flef origin, final String valueFrom,
			final String tagTo){
		final Iterator<GedcomNode> itr = events.iterator();
		while(itr.hasNext()){
			final GedcomNode event = itr.next();
			if("@EVENT@".equals(valueFrom) || valueFrom.equals(event.getValue())){
				final GedcomNode destinationEvent = createEventFrom(tagTo, event, origin);
				destinationNode.addChild(destinationEvent);

				itr.remove();
			}
		}
	}

	void documentFrom(final GedcomNode parent, final GedcomNode destinationNode){
		final List<GedcomNode> files = parent.getChildrenWithTag("FILE");
		for(final GedcomNode file : files){
			final String format = traverse(file, "FORMAT")
				.getValue();
			final String media = traverse(file, "MEDIA")
				.getValue();
			final GedcomNode destinationObject = GedcomNodeBuilder.create(protocol, "OBJE")
				.addChild(GedcomNodeBuilder.create(protocol, "FORM")
					.withValue(format)
					.addChildValue("MEDI", media)
				)
				.addChildValue("FILE", file.getValue());
			final GedcomNode cut = traverse(file, "CUT");
			if(!cut.isEmpty())
				destinationObject.addChildValue("CUT", "Y")
					.addChildValue("_CUTD", cut.getValue());
			final GedcomNode preferred = traverse(file, "PREFERRED");
			if(!preferred.isEmpty())
				destinationObject.addChildValue("_PREF", preferred.getValue());
			destinationNode.addChild(destinationObject);
		}
	}

	void sourceCitationFrom(final GedcomNode parent, final GedcomNode destinationNode){
		final List<GedcomNode> sourceCitations = parent.getChildrenWithTag("SOURCE");
		for(final GedcomNode sourceCitation : sourceCitations){
			//create source:
			final GedcomNode destinationSource = GedcomNodeBuilder.create(protocol, "SOUR")
				.withID(sourceCitation.getID())
				.addChildValue("PAGE", traverse(sourceCitation, "PAGE")
					.getValue())
				.addChild(GedcomNodeBuilder.create(protocol, "EVEN")
					.addChildValue("ROLE", traverse(sourceCitation, "ROLE")
						.getValue())
				)
				.addChildValue("QUAY", traverse(sourceCitation, "CREDIBILITY")
					.getValue());
			noteFrom(sourceCitation, destinationSource);
			destinationNode.addChild(destinationSource);
		}
	}

	private GedcomNode createEventFrom(final String tagTo, final GedcomNode event, final Flef origin){
		final GedcomNode destinationEvent = GedcomNodeBuilder.create(protocol, tagTo)
			.withValue("EVENT".equals(tagTo)? event.getValue(): null)
			.addChildValue("TYPE", traverse(event, "TYPE")
				.getValue())
			.addChildValue("DATE", traverse(event, "DATE")
				.getValue());
		placeStructureFrom(event, destinationEvent, origin);
		addressStructureFrom(event, destinationEvent, origin);
		destinationEvent.addChildValue("AGNC", traverse(event, "AGENCY")
			.getValue())
			.addChildValue("CAUS", traverse(event, "CAUSE")
				.getValue());
		final GedcomNode familyChild = traverse(event, "FAMILY_CHILD");
		destinationEvent.addChildValue("RESN", traverse(event, "RESTRICTION")
			.getValue())
			.addChild(GedcomNodeBuilder.create(protocol, "FAMC")
				.withID(familyChild.getID())
				.addChildValue("ADOP", traverse(familyChild, "ADOPTED_BY")
					.getValue())
			);
		noteFrom(event, destinationEvent);
		sourceCitationFrom(event, destinationEvent);
		return destinationEvent;
	}

	void addressStructureFrom(final GedcomNode parent, final GedcomNode destinationNode, final Flef origin){
		final GedcomNode place = traverse(parent, "PLACE");
		if(!place.isEmpty()){
			final GedcomNode placeRecord = origin.getPlace(place.getID());
			final GedcomNode address = traverse(placeRecord, "ADDRESS");
			destinationNode.addChild(GedcomNodeBuilder.create(protocol, "ADDR")
				.withValue(placeRecord.getValue())
				.addChildValue("CITY", traverse(address, "CITY")
					.getValue())
				.addChildValue("STAE", traverse(address, "STATE")
					.getValue())
				.addChildValue("CTRY", traverse(address, "COUNTRY")
					.getValue()));
		}
	}

	void placeStructureFrom(final GedcomNode parent, final GedcomNode destinationNode, final Flef origin){
		final GedcomNode place = traverse(parent, "PLACE");
		if(!place.isEmpty()){
			final GedcomNode placeRecord = origin.getPlace(place.getID());
			final GedcomNode map = traverse(placeRecord, "MAP");
			final GedcomNode destinationPlace = GedcomNodeBuilder.create(protocol, "PLAC")
				.withValue(traverse(placeRecord, "NAME")
					.getValue())
				.addChild(GedcomNodeBuilder.create(protocol, "MAP")
					.addChildValue("LATI", traverse(map, "LATI")
						.getValue())
					.addChildValue("LONG", traverse(map, "LONG")
						.getValue())
				);
			noteFrom(place, destinationPlace);
			destinationNode.addChild(destinationPlace);
		}
	}

	void noteFrom(final GedcomNode parent, final GedcomNode destinationNode){
		final List<GedcomNode> notes = parent.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			destinationNode.addChildReference("NOTE", note.getID());
	}

}
