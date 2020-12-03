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
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.services.JavaHelper;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;


public final class Transformer extends TransformerHelper{

	private static final Collection<String> ADDRESS_TAGS = new HashSet<>(Arrays.asList("CONT", "ADR1", "ADR2", "ADR3"));


	public Transformer(final Protocol protocol){
		super(protocol);
	}


	//FIXME
	void eventTo(final GedcomNode individual, final GedcomNode destinationNode, final Flef destination,
			final String tagFrom, final String valueTo){
		final List<GedcomNode> events = individual.getChildrenWithTag(tagFrom);
		for(final GedcomNode event : events){
			final GedcomNode destinationEvent = createEventTo(valueTo, event, destination);
			destinationNode.addChild(destinationEvent);
		}
	}

	//FIXME
	void documentTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> documents = parent.getChildrenWithTag("OBJE");
		for(final GedcomNode document : documents){
			String documentXRef = document.getXRef();
			if(documentXRef == null){
				final GedcomNode destinationDocument = create("SOURCE")
					.addChildValue("TITLE", traverse(document, "TITL")
						.getValue());
				final String documentMediaType = traverse(document, "FORM.MEDI")
					.getValue();
				if(documentMediaType != null)
					destinationDocument.addChild(create("FILE")
						.withValue(traverse(document, "FILE")
							.getValue())
						.addChildValue("MEDIA_TYPE", documentMediaType)
					);

				documentXRef = destination.addSource(destinationDocument);
			}

			destinationNode.addChildReference("SOURCE", documentXRef);
		}
	}

	//FIXME
	void sourceCitationTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> sourceCitations = parent.getChildrenWithTag("SOUR");
		for(final GedcomNode sourceCitation : sourceCitations){
			String sourceCitationXRef = sourceCitation.getXRef();
			if(sourceCitationXRef == null){
				//create source:
				final GedcomNode destinationSource = create("SOURCE")
					.addChildValue("TITLE", sourceCitation.getValue());

				final List<GedcomNode> extracts = traverseAsList(sourceCitation, "TEXT");
				documentTo(sourceCitation, destinationSource, destination);
				assignExtractionsTo(extracts, destinationSource);
				noteCitationTo(sourceCitation, destinationSource, destination);

				sourceCitationXRef = destination.addSource(destinationSource);

				//add source citation:
				destinationNode.addChild(createWithReference("SOURCE", sourceCitationXRef)
					.addChildValue("CREDIBILITY", traverse(sourceCitation, "QUAY").getValue()));
			}
			else{
				//retrieve source:
				final GedcomNode destinationSource = destination.getSource(sourceCitationXRef)
					.addChildValue("EVENT", traverse(sourceCitation, "EVEN").getValue())
					.addChildValue("DATE", traverse(sourceCitation, "DATA.DATE").getValue());
				final List<GedcomNode> extracts = traverseAsList(sourceCitation, "DATA.TEXT");
				documentTo(sourceCitation, destinationSource, destination);
				assignExtractionsTo(extracts, destinationSource);
				noteCitationTo(sourceCitation, destinationSource, destination);

				//add source citation:
				final GedcomNode destinationSourceCitation = create("SOURCE")
					.withXRef(sourceCitationXRef)
					.addChildValue("LOCATION", traverse(sourceCitation, "PAGE").getValue())
					.addChildValue("ROLE", traverse(sourceCitation, "EVEN.ROLE").getValue())
					.addChildValue("CREDIBILITY", traverse(sourceCitation, "QUAY").getValue());
				destinationNode.addChild(destinationSourceCitation);
			}
		}
	}

	//FIXME
	private void assignExtractionsTo(final List<GedcomNode> extracts, final GedcomNode destinationSource){
		final List<GedcomNode> destinationSources = traverseAsList(destinationSource, "SOURCE");
		if(extracts.size() > destinationSources.size()){
			//collect all extractions and assign to first
			final StringJoiner sj = new StringJoiner("\n");
			for(int index = 0; index < extracts.size(); index ++)
				sj.add("EXTRACT " + index)
					.add(extracts.get(index).getValue());
			destinationSources.get(0)
				.withValue(sj.toString());
		}
		else
			//otherwise distribute extractions
			for(int index = 0; index < extracts.size(); index ++)
				destinationSources.get(index)
					.withValue(extracts.get(index).getValue());
	}

	//FIXME
	void addressStructureTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final GedcomNode address = traverse(parent, "ADDR");
		final String addressValue = extractAddressValue(address);

		final GedcomNode destinationPlace = create("PLACE")
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

	//FIXME
	private GedcomNode createEventTo(final String valueTo, final GedcomNode event, final Flef destination){
		final GedcomNode destinationEvent = create("EVENT")
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
		noteCitationTo(event, destinationEvent, destination);
		sourceCitationTo(event, destinationEvent, destination);
		documentTo(event, destinationEvent, destination);
		final GedcomNode familyChild = traverse(event, "FAMC");
		destinationEvent.addChildValue("RESTRICTION", traverse(event, "RESN")
			.getValue())
			.addChild(create("FAMILY_CHILD")
				.withXRef(familyChild.getXRef())
				.addChildValue("ADOPTED_BY", traverse(familyChild, "ADOP")
					.getValue())
			);
		return destinationEvent;
	}

	//FIXME
	void placeAddressStructureTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final GedcomNode address = traverse(parent, "ADDR");
		final String addressValue = extractAddressValue(address);

		final GedcomNode place = traverse(parent, "PLAC");
		if(!address.isEmpty() || !place.isEmpty()){
			final GedcomNode map = traverse(place, "MAP");
			final GedcomNode destinationPlace = create("PLACE")
				.withValue(place.getValue())
				.addChildValue("ADDRESS", addressValue)
				.addChildValue("CITY", traverse(address, "CITY")
					.getValue())
				.addChildValue("STATE", traverse(address, "STAE")
					.getValue())
				.addChildValue("COUNTRY", traverse(address, "CTRY")
					.getValue())
				.addChild(create("MAP")
					.addChildValue("LATITUDE", traverse(map, "LATI")
						.getValue())
					.addChildValue("LONGITUDE", traverse(map, "LONG")
						.getValue())
				);
			noteCitationTo(place, destinationPlace, destination);
			final String destinationPlaceID = destination.addPlace(destinationPlace);
			destinationNode.addChildReference("PLACE", destinationPlaceID);
		}
	}

	//FIXME
	private String extractAddressValue(final GedcomNode address){
		final StringJoiner sj = new StringJoiner(" - ");
		final String wholeAddress = address.getValue();
		JavaHelper.addValueIfNotNull(sj, wholeAddress);
		for(final GedcomNode child : address.getChildren())
			if(ADDRESS_TAGS.contains(child.getTag())){
				final String value = child.getValue();
				JavaHelper.addValueIfNotNull(sj, value);
			}
		return (sj.length() > 0? sj.toString(): null);
	}

	void sourceRepositoryCitationTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> citations = parent.getChildrenWithTag("REPO");
		for(final GedcomNode citation : citations){
			final GedcomNode repositoryCitation = create("REPOSITORY")
				.withXRef(citation.getXRef())
				.addChildValue("LOCATION", traverse(citation, "CALN").getValue());
			noteCitationTo(citation, repositoryCitation, destination);

			destinationNode.addChild(repositoryCitation);
		}
	}

	void spouseToFamilyLinkTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> links = parent.getChildrenWithTag("FAMS");
		for(final GedcomNode link : links){
			final GedcomNode familySpouse = create("FAMILY_SPOUSE")
				.withXRef(link.getXRef());
			noteCitationTo(link, familySpouse, destination);

			destinationNode.addChild(familySpouse);
		}
	}

	void noteCitationTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> notes = parent.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes){
			String noteXRef = note.getXRef();
			if(noteXRef == null)
				noteXRef = destination.addNote(createWithValue("NOTE", note.getValue()));

			destinationNode.addChildReference("NOTE", noteXRef);
		}
	}

	void noteRecordTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> notes = parent.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes){
			final String noteID = destination.addNote(createWithIDValue("NOTE", note.getID(), note.getValue()));

			destinationNode.addChildReference("NOTE", noteID);
		}
	}


	//FIXME
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

	//FIXME
	void documentFrom(final GedcomNode parent, final GedcomNode destinationNode){
		final List<GedcomNode> files = parent.getChildrenWithTag("FILE");
		for(final GedcomNode file : files){
			final String mediaType = traverse(file, "MEDIA_TYPE")
				.getValue();
			final GedcomNode destinationObject = create("OBJE")
				.addChild(create("FORM")
					.addChildValue("MEDI", mediaType)
				)
				.addChildValue("FILE", file.getValue());
			destinationNode.addChild(destinationObject);
		}
	}

	//FIXME
	void sourceCitationFrom(final GedcomNode parent, final GedcomNode destinationNode){
		final List<GedcomNode> sourceCitations = parent.getChildrenWithTag("SOURCE");
		for(final GedcomNode sourceCitation : sourceCitations){
			//create source:
			final GedcomNode destinationSource = create("SOUR")
				.withXRef(sourceCitation.getXRef())
				.addChildValue("PAGE", traverse(sourceCitation, "LOCATION")
					.getValue())
				.addChild(create("EVEN")
					.addChildValue("ROLE", traverse(sourceCitation, "ROLE")
						.getValue())
				)
				.addChildValue("QUAY", traverse(sourceCitation, "CREDIBILITY")
					.getValue());
			noteCitationFrom(sourceCitation, destinationSource);
			destinationNode.addChild(destinationSource);
		}
	}

	//FIXME
	private GedcomNode createEventFrom(final String tagTo, final GedcomNode event, final Flef origin){
		final GedcomNode destinationEvent = create(tagTo)
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
			.addChild(create("FAMC")
				.withXRef(familyChild.getXRef())
				.addChildValue("ADOP", traverse(familyChild, "ADOPTED_BY")
					.getValue())
			);
		noteCitationFrom(event, destinationEvent);
		sourceCitationFrom(event, destinationEvent);
		return destinationEvent;
	}

	//FIXME
	void addressStructureFrom(final GedcomNode parent, final GedcomNode destinationNode, final Flef origin){
		final GedcomNode place = traverse(parent, "PLACE");
		if(!place.isEmpty()){
			final GedcomNode placeRecord = origin.getPlace(place.getXRef());
			final GedcomNode address = traverse(placeRecord, "ADDRESS");
			destinationNode.addChild(create("ADDR")
				.withValue(placeRecord.getValue())
				.addChildValue("CITY", traverse(address, "CITY")
					.getValue())
				.addChildValue("STAE", traverse(address, "STATE")
					.getValue())
				.addChildValue("CTRY", traverse(address, "COUNTRY")
					.getValue()));
		}
	}

	//FIXME
	void placeStructureFrom(final GedcomNode parent, final GedcomNode destinationNode, final Flef origin){
		final GedcomNode place = traverse(parent, "PLACE");
		if(!place.isEmpty()){
			final GedcomNode placeRecord = origin.getPlace(place.getXRef());
			final GedcomNode map = traverse(placeRecord, "MAP");
			final GedcomNode destinationPlace = create("PLAC")
				.withValue(traverse(placeRecord, "NAME")
					.getValue())
				.addChild(create("MAP")
					.addChildValue("LATI", traverse(map, "LATI")
						.getValue())
					.addChildValue("LONG", traverse(map, "LONG")
						.getValue())
				);
			noteCitationFrom(place, destinationPlace);
			destinationNode.addChild(destinationPlace);
		}
	}

	void sourceRepositoryCitationFrom(final GedcomNode parent, final GedcomNode destinationNode){
		final List<GedcomNode> citations = parent.getChildrenWithTag("REPOSITORY");
		for(final GedcomNode citation : citations){
			final GedcomNode repositoryCitation = create("REPO")
				.withXRef(citation.getXRef())
				.addChildValue("CALN", traverse(citation, "LOCATION").getValue());
			noteCitationFrom(citation, repositoryCitation);

			destinationNode.addChild(repositoryCitation);
		}
	}

	void spouseToFamilyLinkFrom(final GedcomNode parent, final GedcomNode destinationNode){
		final List<GedcomNode> links = parent.getChildrenWithTag("FAMILY_SPOUSE");
		for(final GedcomNode link : links){
			final GedcomNode familySpouse = create("FAMS")
				.withXRef(link.getXRef());
			noteCitationFrom(link, familySpouse);

			destinationNode.addChild(familySpouse);
		}
	}

	void noteCitationFrom(final GedcomNode parent, final GedcomNode destinationNode){
		final List<GedcomNode> notes = parent.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			destinationNode.addChildReference("NOTE", note.getXRef());
	}

	void noteRecordFrom(final GedcomNode parent, final GedcomNode destinationNode, final Gedcom destination){
		final List<GedcomNode> notes = parent.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes){
			final String noteID = destination.addNote(createWithIDValue("NOTE", note.getID(), note.getValue()));

			destinationNode.addChildReference("NOTE", noteID);
		}
	}

}
