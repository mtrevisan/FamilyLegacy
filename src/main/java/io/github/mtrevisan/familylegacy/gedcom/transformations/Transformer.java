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
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
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
		multimediaCitationTo(event, destinationEvent, destination);
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

	void headerTo(final GedcomNode parent, final GedcomNode destinationNode){
		final GedcomNode header = parent.getChildrenWithTag("HEAD").get(0);
		final GedcomNode source = traverse(header, "SOUR");
		final GedcomNode date = traverse(header, "DATE");
		final GedcomNode time = traverse(date, "TIME");
		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		JavaHelper.addValueIfNotNull(sj, date);
		JavaHelper.addValueIfNotNull(sj, time);
		final String language = traverse(source, "LANG")
			.getValue();
		final Locale locale = (language != null? new Locale(language): Locale.forLanguageTag("en-US"));
		final GedcomNode destinationHeader = create("HEADER")
			.addChild(create("PROTOCOL")
				.withValue("FLEF")
				.addChildValue("NAME", "Family LEgacy Format")
				.addChildValue("VERSION", "0.0.4")
			)
			.addChild(create("SOURCE")
				.withValue(source.getValue())
				.addChildValue("NAME", traverse(source, "NAME").getValue())
				.addChildValue("VERSION", traverse(source, "VERS").getValue())
				.addChildValue("CORPORATE", traverse(source, "CORP").getValue())
			)
			.addChildValue("DATE", (sj.length() > 0? sj.toString(): null))
			.addChildValue("DEFAULT_LOCALE", locale.toLanguageTag())
			.addChildValue("COPYRIGHT", traverse(header, "COPR").getValue())
			.addChildReference("SUBMITTER", traverse(header, "SUBM").getXRef())
			.addChildValue("NOTE", traverse(header, "NOTE").getValue());

		destinationNode.addChild(destinationHeader);
	}

	void multimediaCitationTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> multimedias = parent.getChildrenWithTag("OBJE");
		for(final GedcomNode multimedia : multimedias){
			String documentXRef = multimedia.getXRef();
			if(documentXRef == null){
				final GedcomNode destinationMultimedia = create("MULTIMEDIA")
					.addChildValue("TITLE", traverse(multimedia, "TITL").getValue())
					.addChild(createWithValue("FILE", traverseAsList(multimedia, "FILE").get(0).getValue()))
					.addChildValue("MEDIA_TYPE", traverse(multimedia, "FORM.MEDI").getValue());
				documentXRef = destination.addMultimedia(destinationMultimedia);
			}
			//TODO remember to manage _PREF

			destinationNode.addChild(createWithReference("MULTIMEDIA", documentXRef)
				.addChildValue("CUTOUT", traverse(multimedia, "_CUTD").getValue())
			);
		}
	}

	void placeAddressStructureTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final GedcomNode address = traverse(parent, "ADDR");
		final GedcomNode place = traverse(parent, "PLAC");
		if(!address.isEmpty() || !place.isEmpty()){
			final GedcomNode map = traverse(place, "MAP");
			final GedcomNode destinationPlace = createWithValue("PLACE", place.getValue())
				.addChildValue("ADDRESS", extractAddressValue(address))
				.addChildValue("CITY", traverse(address, "CITY").getValue())
				.addChildValue("STATE", traverse(address, "STAE").getValue())
				.addChildValue("COUNTRY", traverse(address, "CTRY").getValue())
				.addChild(create("MAP")
					.addChildValue("LATITUDE", traverse(map, "LATI").getValue())
					.addChildValue("LONGITUDE", traverse(map, "LONG").getValue())
				);
			noteCitationTo(place, destinationPlace, destination);

			final String destinationPlaceID = destination.addPlace(destinationPlace);
			destinationNode.addChildReference("PLACE", destinationPlaceID);
		}
	}

	private String extractAddressValue(final GedcomNode address){
		final StringJoiner sj = new StringJoiner(" - ");
		JavaHelper.addValueIfNotNull(sj, address.getValue());
		for(final GedcomNode child : address.getChildren())
			if(ADDRESS_TAGS.contains(child.getTag()))
				JavaHelper.addValueIfNotNull(sj, child.getValue());
		return (sj.length() > 0? sj.toString(): null);
	}

	void contactStructureTo(final GedcomNode parent, final GedcomNode destinationNode){
		final GedcomNode destinationContact = create("CONTACT");
		final List<GedcomNode> phones = parent.getChildrenWithTag("PHON");
		for(final GedcomNode phone : phones)
			destinationContact.addChildValue("PHONE", phone.getValue());
		final List<GedcomNode> emails = parent.getChildrenWithTag("EMAIL");
		for(final GedcomNode email : emails)
			destinationContact.addChildValue("EMAIL", email.getValue());
		final List<GedcomNode> faxes = parent.getChildrenWithTag("FAX");
		for(final GedcomNode fax : faxes)
			destinationContact.addChild(createWithValue("PHONE", fax.getValue())
				.addChildValue("TYPE", "fax"));
		final List<GedcomNode> urls = parent.getChildrenWithTag("WWW");
		for(final GedcomNode url : urls)
			destinationContact.addChildValue("URL", url.getValue());
		destinationNode.addChild(destinationContact);
	}

	//FIXME
//	n SOUR @<XREF:SOUR>@    {1:1}	/* An xref ID of a source record. */
//  +1 PAGE <WHERE_WITHIN_SOURCE>    {0:1}	/* Specific location with in the information referenced. The data in this field should be in the form of a label and value pair (eg. 'Film: 1234567, Frame: 344, Line: 28'). */
//  +1 EVEN <EVENT_TYPE_CITED_FROM>    {0:1}	/* A code that indicates the type of event which was responsible for the source entry being recorded. For example, if the entry was created to record a birth of a child, then the type would be BIRT regardless of the assertions made from that record, such as the mother's name or mother's birth date. This will allow a prioritized best view choice and a determination of the certainty associated with the source used in asserting the cited fact. */
//    +2 ROLE <ROLE_IN_EVENT>    {0:1}	/* Indicates what role this person played in the event that is being cited in this context. */
//  +1 DATA    {0:1}
//    +2 DATE <ENTRY_RECORDING_DATE>    {0:1}	/* A date_value() object giving the date that this event data was entered into the original source document. */
//    +2 TEXT <TEXT_FROM_SOURCE>    {0:M}	/* A verbatim copy of any description contained within the source. This indicates notes or text that are actually contained in the source document, not the submitter's opinion about the source. */
//  +1 <<MULTIMEDIA_LINK>>    {0:M}	/* A list of MULTIMEDIA_LINK() objects. */
//  +1 <<NOTE_STRUCTURE>>    {0:M}	/* A list of NOTE_STRUCTURE() objects. */
//  +1 QUAY <CERTAINTY_ASSESSMENT>    {0:1}	/* A quantitative evaluation of the credibility of a piece of information, based upon its supporting evidence. Some systems use this feature to rank multiple conflicting opinions for display of most likely information first. It is not intended to eliminate the receiver's need to evaluate the evidence for themselves. 0 = unreliable/estimated data 1 = Questionable reliability of evidence 2 = Secondary evidence, data officially recorded sometime after event 3 = Direct and primary evidence used, or by dominance of the evidence. */
//|	/* Systems not using source records. */
//	n SOUR <SOURCE_DESCRIPTION>    {1:1}	/* A free form text block used to describe the source from which information was obtained. This text block is used by those systems which cannot use a pointer to a source record. It must contain a descriptive title, who created the work, where and when it was created, and where the source data stored. */
//  +1 TEXT <TEXT_FROM_SOURCE>    {0:M}	/* A verbatim copy of any description contained within the source. This indicates notes or text that are actually contained in the source document, not the submitter's opinion about the source. */
//  +1 <<MULTIMEDIA_LINK>>    {0:M}	/* A list of MULTIMEDIA_LINK() objects. */
//  +1 <<NOTE_STRUCTURE>>    {0:M}	/* A list of NOTE_STRUCTURE() objects. */
//  +1 QUAY <CERTAINTY_ASSESSMENT>    {0:1}	/* A quantitative evaluation of the credibility of a piece of information, based upon its supporting evidence. Some systems use this feature to rank multiple conflicting opinions for display of most likely information first. It is not intended to eliminate the receiver's need to evaluate the evidence for themselves. 0 = unreliable/estimated data 1 = Questionable reliability of evidence 2 = Secondary evidence, data officially recorded sometime after event 3 = Direct and primary evidence used, or by dominance of the evidence. */

//	n SOURCE @<XREF:SOURCE>@    {1:1}	/* An xref ID of a source record. */
//  +1 LOCATION <WHERE_WITHIN_SOURCE>    {0:1}	/* Specific location with in the information referenced. The data in this field should be in the form of a label and value pair (eg. 'Film: 1234567, Frame: 344, Line: 28'). */
//  +1 ROLE <ROLE_IN_EVENT>    {0:1}	/* Indicates what role this person or family played in the event that is being cited in this context. Known values are: CHILD, FATHER, HUSBAND, MOTHER, WIFE, SPOUSE, etc. */
//  +1 NOTE @<XREF:NOTE>@    {0:M}	/* An xref ID of a note record. */
//  +1 CREDIBILITY <CREDIBILITY_ASSESSMENT>    {0:1}	/* A quantitative evaluation of the credibility of a piece of information, based upon its supporting evidence. Some systems use this feature to rank multiple conflicting opinions for display of most likely information first. It is not intended to eliminate the receiver's need to evaluate the evidence for themselves. 0 = unreliable/estimated data 1 = Questionable reliability of evidence 2 = Secondary evidence, data officially recorded sometime after event 3 = Direct and primary evidence used, or by dominance of the evidence. */
	void sourceCitationTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> sourceCitations = parent.getChildrenWithTag("SOUR");
		for(final GedcomNode sourceCitation : sourceCitations){
			String sourceCitationXRef = sourceCitation.getXRef();
			if(sourceCitationXRef == null){
				//create source:
				final GedcomNode destinationSource = create("SOURCE")
					.addChildValue("TITLE", sourceCitation.getValue());

				final List<GedcomNode> extracts = traverseAsList(sourceCitation, "TEXT");
				sourceCitationXRef = sourceMultimediaCitationTo(parent, destinationSource, destination);
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
				sourceMultimediaCitationTo(parent, destinationSource, destination);
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

	private String sourceMultimediaCitationTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		String sourceXRef = null;
		final List<GedcomNode> multimedias = parent.getChildrenWithTag("OBJE");
		for(final GedcomNode multimedia : multimedias){
			if(multimedia.getXRef() == null){
				final GedcomNode destinationSource = create("SOURCE")
					.addChildValue("TITLE", traverse(multimedia, "TITL").getValue());
				final List<GedcomNode> files = traverseAsList(multimedia, "FILE");
				for(final GedcomNode file : files)
					destinationSource.addChild(createWithValue("FILE", file.getValue()));
				destinationSource.addChildValue("MEDIA_TYPE", traverse(multimedia, "FORM.MEDI").getValue());
				sourceXRef = destination.addSource(destinationSource);
			}
			else if(sourceXRef == null)
				sourceXRef = multimedia.getXRef();
		}
		return sourceXRef;
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
			final GedcomNode destinationFamilySpouse = create("FAMILY_SPOUSE")
				.withXRef(link.getXRef());
			noteCitationTo(link, destinationFamilySpouse, destination);

			destinationNode.addChild(destinationFamilySpouse);
		}
	}

	void childToFamilyLinkTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> links = parent.getChildrenWithTag("FAMC");
		for(final GedcomNode link : links){
			final String pedigree = traverse(link, "PEDI").getValue();
			final GedcomNode destinationFamilyChild = createWithReference("FAMILY_CHILD", link.getXRef())
				.addChild(create("PEDIGREE")
					.addChildValue("PARENT1", pedigree)
					.addChildValue("PARENT2", pedigree)
				)
				.addChildValue("CERTAINTY", traverse(link, "STAT").getValue());
			noteCitationTo(link, destinationFamilyChild, destination);

			destinationNode.addChild(destinationFamilyChild);
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

	void headerFrom(final GedcomNode parent, final GedcomNode destinationNode){
		final GedcomNode header = parent.getChildrenWithTag("HEADER").get(0);
		final GedcomNode source = traverse(header, "SOURCE");
		final String date = traverse(header, "DATE")
			.getValue();
		final String language = traverse(source, "DEFAULT_LOCALE")
			.getValue();
		final Locale locale = Locale.forLanguageTag(language != null? language: "en-US");
		final GedcomNode destinationHeader = create("HEAD")
			.addChild(create("SOUR")
				.withValue(source.getValue())
				.addChildValue("VERS", traverse(source, "VERSION")
					.getValue())
				.addChildValue("NAME", traverse(source, "NAME")
					.getValue())
				.addChildValue("CORP", traverse(source, "CORPORATE")
					.getValue())
			)
			.addChildValue("DATE", date)
			.addChildReference("SUBM", traverse(source, "SUBMITTER")
				.getXRef())
			.addChildValue("COPR", traverse(source, "COPYRIGHT")
				.getValue())
			.addChild(create("GEDC")
				.addChildValue("VERS", "5.5.1")
				.addChildValue("FORM", "LINEAGE-LINKED")
			)
			.addChildValue("CHAR", "UTF-8")
			.addChildValue("LANG", locale.getDisplayLanguage(Locale.ENGLISH))
			.addChildValue("NOTE", traverse(source, "NOTE")
				.getValue());

		destinationNode.addChild(destinationHeader);
	}

	void multimediaCitationFrom(final GedcomNode parent, final GedcomNode destinationNode, final Flef origin){
		final List<GedcomNode> multimedias = parent.getChildrenWithTag("MULTIMEDIA");
		for(final GedcomNode multimedia : multimedias){
			final String multimediaXRef = multimedia.getXRef();
			final String cutout = traverse(multimedia, "CUTOUT").getValue();
			if(cutout == null)
				destinationNode.addChildReference("OBJE", multimediaXRef);
			else{
				final GedcomNode originMultimedia = origin.getMultimedia(multimediaXRef);
				final GedcomNode file = traverse(originMultimedia, "FILE");
				destinationNode.addChild(create("OBJE")
					.addChildValue("TITL", traverse(originMultimedia, "TITLE").getValue())
					.addChildValue("FILE", file.getValue())
					.addChild(create("FORM")
						.addChildValue("TITL", traverse(file, "MEDIA_TYPE").getValue())
					)
					.addChildValue("_CUTD", cutout));
			}
		}
	}

	void addressStructureFrom(final GedcomNode parent, final GedcomNode destinationNode, final Flef origin){
		final GedcomNode place = traverse(parent, "PLACE");
		if(!place.isEmpty()){
			final GedcomNode placeRecord = origin.getPlace(place.getXRef());
			final GedcomNode address = traverse(placeRecord, "ADDRESS");
			destinationNode.addChild(create("ADDR")
				.withValue(address.getValue())
				.addChildValue("CITY", traverse(address, "CITY").getValue())
				.addChildValue("STAE", traverse(address, "STATE").getValue())
				.addChildValue("CTRY", traverse(address, "COUNTRY").getValue()));
		}
	}

	void placeStructureFrom(final GedcomNode parent, final GedcomNode destinationNode, final Flef origin){
		final GedcomNode place = traverse(parent, "PLACE");
		if(!place.isEmpty()){
			final GedcomNode placeRecord = origin.getPlace(place.getXRef());
			final GedcomNode map = traverse(placeRecord, "MAP");
			final GedcomNode destinationPlace = create("PLAC")
				.withValue(traverse(placeRecord, "NAME").getValue())
				.addChild(create("MAP")
					.addChildValue("LATI", traverse(map, "LATITUDE").getValue())
					.addChildValue("LONG", traverse(map, "LONGITUDE").getValue())
				);
			noteCitationFrom(placeRecord, destinationPlace);
			destinationNode.addChild(destinationPlace);
		}
	}

	void contactStructureFrom(final GedcomNode parent, final GedcomNode destinationNode){
		final GedcomNode contact = traverse(parent, "CONTACT");
		final List<GedcomNode> phones = contact.getChildrenWithTag("PHONE");
		for(final GedcomNode phone : phones)
			if(!"fax".equals(traverse(phone, "TYPE").getValue()))
				destinationNode.addChildValue("PHONE", phone.getValue());
		final List<GedcomNode> emails = contact.getChildrenWithTag("EMAIL");
		for(final GedcomNode email : emails)
			destinationNode.addChildValue("EMAIL", email.getValue());
		for(final GedcomNode phone : phones)
			if("fax".equals(traverse(phone, "TYPE").getValue()))
				destinationNode.addChildValue("FAX", phone.getValue());
		final List<GedcomNode> urls = contact.getChildrenWithTag("URL");
		for(final GedcomNode url : urls)
			destinationNode.addChildValue("WWW", url.getValue());
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

	void childToFamilyLinkFrom(final GedcomNode individual, final GedcomNode destinationNode){
		final List<GedcomNode> childToFamilyLinks = individual.getChildrenWithTag("FAMILY_CHILD");
		for(final GedcomNode childToFamilyLink : childToFamilyLinks){
			final GedcomNode pedigree = traverse(childToFamilyLink, "PEDIGREE");
			final String pedigreeParent1 = traverse(pedigree, "PARENT1").getValue();
			final String pedigreeParent2 = traverse(pedigree, "PARENT2").getValue();
			@SuppressWarnings({"StringEquality", "ObjectAllocationInLoop"})
			final String pedigreeValue = (pedigreeParent1 == pedigreeParent2 || pedigreeParent1.equals(pedigreeParent2)?
				pedigreeParent1: "PARENT1: " + pedigreeParent1 + ", PARENT2: " + pedigreeParent2);
			final GedcomNode destinationFamilyChild = create("FAMC")
				.addChildValue("PEDI", pedigreeValue)
				.addChildValue("STAT", traverse(childToFamilyLink, "CERTAINTY").getValue());
			noteCitationFrom(childToFamilyLink, destinationFamilyChild);
			destinationNode.addChild(destinationFamilyChild);
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
