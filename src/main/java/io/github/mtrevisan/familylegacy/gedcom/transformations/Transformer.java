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
import io.github.mtrevisan.familylegacy.gedcom.parsers.calendars.CalendarParserBuilder;
import io.github.mtrevisan.familylegacy.services.JavaHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;


public final class Transformer extends TransformerHelper{

	private static final Map<String, String> FAM_TO_FAMILY = new HashMap<>();
	private static final Map<String, String> FAMILY_TO_FAM = new HashMap<>();
	private static final String CUSTOM_EVENT_TAG = "@EVENT@";
	static{
		FAM_TO_FAMILY.put("ANUL", "ANNULMENT");
		FAM_TO_FAMILY.put("CENS", "CENSUS");
		FAM_TO_FAMILY.put("DIV", "DIVORCE");
		FAM_TO_FAMILY.put("DIVF", "DIVORCE_FILED");
		FAM_TO_FAMILY.put("ENGA", "ENGAGEMENT");
		FAM_TO_FAMILY.put("MARB", "MARRIAGE_BANN");
		FAM_TO_FAMILY.put("MARC", "MARRIAGE_CONTRACT");
		FAM_TO_FAMILY.put("MARR", "MARRIAGE");
		FAM_TO_FAMILY.put("MARL", "MARRIAGE_LICENCE");
		FAM_TO_FAMILY.put("MARS", "MARRIAGE_SETTLEMENT");
		FAM_TO_FAMILY.put("RESI", "RESIDENCE");
		FAM_TO_FAMILY.put("EVEN", CUSTOM_EVENT_TAG);

		//fill up inverse relation
		for(final Map.Entry<String, String> entry : FAM_TO_FAMILY.entrySet()){
			final String value = entry.getValue();
			if(!CUSTOM_EVENT_TAG.equals(value))
				FAMILY_TO_FAM.put(value, entry.getKey());
		}
	}

	private static final Collection<String> ADDRESS_TAGS = new HashSet<>(Arrays.asList("CONT", "ADR1", "ADR2", "ADR3"));


	public Transformer(final Protocol protocol){
		super(protocol);
	}


	/*
		transfer FAM[EVEN] to FAMILY[EVENT]
		create EVENT{CHILDREN_COUNT}
			EVENT{CHILDREN_COUNT}.DESCRIPTION.value = FAM.NCHI.value
		transfer FAM.NOTE to FAMILY.NOTE
		transfer SOUR,OBJE xref to SOURCE
		FAMILY.RESTRICTION.value = FAM.RESN.value
	*/
	void familyRecordTo(final GedcomNode parent, final Gedcom origin, final Flef destination){
		final GedcomNode destinationFamily = create("FAMILY");
		final List<GedcomNode> families = traverseAsList(parent, "FAM");
		for(final GedcomNode family : families){
			destinationFamily.withID(family.getID())
				.addChildValue("SPOUSE1", traverse(family, "HUSB").getXRef())
				.addChildValue("SPOUSE2", traverse(family, "WIFE").getXRef());
			final List<GedcomNode> children = traverseAsList(family, "CHIL");
			for(final GedcomNode child : children)
				destinationFamily.addChildReference("CHILD", child.getXRef());
			//scan events one by one, maintaining order
			final List<GedcomNode> nodeChildren = family.getChildren();
			for(final GedcomNode nodeChild : nodeChildren){
				final String tagFrom = nodeChild.getTag();
				final String valueTo = FAM_TO_FAMILY.get(tagFrom);
				if(valueTo != null){
					final GedcomNode destinationEvent = createEventTo(valueTo, nodeChild, origin, destination);
					destinationFamily.addChild(destinationEvent);
				}
			}
			noteCitationTo(family, destinationFamily, destination);
			sourceCitationTo(family, destinationFamily, origin, destination);
			final GedcomNode destinationFamilyReference = create("SOURCE");
			multimediaCitationTo(family, destinationFamily, destinationFamilyReference, origin, destination, "TEXT");
			destinationFamily.addChildValue("CREDIBILITY", traverse(parent, "RESN").getValue());

			destination.addFamily(destinationFamily);
		}
	}

	/*
	TYPE.value = value
	DESCRIPTION.value = TYPE.value
	DATE.value = DATE.value
	transfer PLAC,ADDR to PLACE
	AGENCY.value = AGNC.value
	CAUSE.value = CAUS.value
	transfer NOTE to NOTE
	transfer SOUR,OBJE xref to SOURCE
	FAMILY_CHILD.value = FAMC.value
	FAMILY_CHILD.ADOPTED_BY.value = FAMC.ADOP.value
	*/
	private GedcomNode createEventTo(final String valueTo, final GedcomNode event, final Gedcom origin, final Flef destination){
		final String value = traverse(event, "DATE").getValue();
		final GedcomNode destinationEvent = create("EVENT")
			.addChildValue("TYPE", (CUSTOM_EVENT_TAG.equals(valueTo)? event.getValue(): valueTo))
			.addChildValue("DESCRIPTION", traverse(event, "TYPE").getValue());
		final GedcomNode familyChild = traverse(event, "FAMC");
		if(!familyChild.isEmpty())
			//EVEN BIRT
			destinationEvent.addChild(create("FAMILY_CHILD")
				.withXRef(familyChild.getXRef())
				//EVEN ADOP
				.addChildValue("ADOPTED_BY", traverse(familyChild, "ADOP").getValue())
			);
		destinationEvent.addChild(create("DATE")
			.withValue(CalendarParserBuilder.removeCalendarType(value))
			.addChildValue("CALENDAR", CalendarParserBuilder.getCalendarType(value))
		);
		placeAddressStructureTo(event, destinationEvent, destination);
		destinationEvent.addChildValue("AGENCY", traverse(event, "AGNC").getValue())
			.addChildValue("CAUSE", traverse(event, "CAUS").getValue());
		noteCitationTo(event, destinationEvent, destination);
		sourceCitationTo(event, destinationEvent, origin, destination);
		final GedcomNode destinationSourceReference = create("SOURCE");
		multimediaCitationTo(event, destinationEvent, destinationSourceReference, origin, destination, "MEDI");
		destinationEvent.addChildValue("RESTRICTION", traverse(event, "RESN").getValue());

		return destinationEvent;
	}

	/*
	for-each HEAD value create HEADER
		HEADER.PROTOCOL.value = "FLEF"
		HEADER.PROTOCOL.NAME.value = "Family LEgacy Format"
		HEADER.PROTOCOL.VERSION.value = "0.04"
		HEADER.SOURCE.value = HEAD.SOUR.value
		HEADER.SOURCE.NAME.value = HEAD.SOUR.NAME.value
		HEADER.SOURCE.VERSION.value = HEAD.SOUR.VERS.value
		HEADER.SOURCE.CORPORATE.value = HEAD.SOUR.CORP.value
		HEADER.DATE.value = HEAD.DATE.value + " " + HEAD.DATE.TIME.value
		HEADER.DEFAULT_LOCALE.value = HEAD.LANG.value
		HEADER.COPYRIGHT.value = HEAD.COPR.value
		HEADER.SUBMITTER.xref = HEAD.SUBM.xref
		HEADER.NOTE.value = HEAD.NOTE.value
	*/
	void headerTo(final GedcomNode parent, final GedcomNode destinationNode){
		final GedcomNode header = parent.getChildrenWithTag("HEAD").get(0);
		final GedcomNode source = traverse(header, "SOUR");
		final GedcomNode date = traverse(header, "DATE");
		final GedcomNode time = traverse(date, "TIME");
		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		JavaHelper.addValueIfNotNull(sj, date);
		JavaHelper.addValueIfNotNull(sj, time);
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
			.addChildValue("COPYRIGHT", traverse(header, "COPR").getValue())
			.addChildReference("SUBMITTER", traverse(header, "SUBM").getXRef())
			.addChildValue("NOTE", traverse(header, "NOTE").getValue());

		destinationNode.addChild(destinationHeader);
	}

	/*
		for-each SOUR.OBJE xref
			load OBJE[rec] from SOUR.OBJE.xref
			for-each OBJE[rec].FILE
				pick-each: SOURCE.FILE.value = OBJE[rec].FILE.value
				pick-each: SOURCE.FILE.DESCRIPTION.value = OBJE[rec].FILE.TITL.value
				pick-one: SOURCE.MEDIA_TYPE.value = OBJE[rec].FORM.TYPE.value
			transfer OBJE[rec].NOTE to SOURCE.NOTE
		for-each SOUR.OBJE
			pick-each: SOURCE.FILE.value = SOUR.OBJE.FILE.value
			pick-each: SOURCE.FILE.DESCRIPTION.value = SOUR.OBJE.TITL.value
			pick-each: SOURCE[ref].CUTOUT = SOUR.OBJE._CUTD.value
			pick-one: SOURCE.MEDIA_TYPE.value = SOUR.OBJE.FORM.TYPE.value
			remember which one has the _PREF tag
	*/
	void multimediaCitationTo(final GedcomNode parent, final GedcomNode destinationNode, final GedcomNode destinationSourceReference,
			final Gedcom origin, final Flef destination, final String extractionsTag){
		final List<GedcomNode> objects = traverseAsList(parent, "OBJE");
		for(final GedcomNode object : objects){
			final List<GedcomNode> files;
			final String multimediaXRef = object.getXRef();
			if(multimediaXRef != null){
				//load object by xref
				final GedcomNode multimediaRecord = origin.getObject(multimediaXRef);
				//FIXME source with ID and object with ID: what to do?
				if(destinationNode.getID() == null)
					destinationNode.withXRef(multimediaXRef);
				files = traverseAsList(multimediaRecord, "FILE");
				String mediaType = null;
				for(int i = 0; mediaType == null && i < files.size(); i ++)
					mediaType = traverse(files.get(i), "FORM.TYPE").getValue();
				destinationNode.addChildValue("MEDIA_TYPE", mediaType);
				for(final GedcomNode file : files)
					destinationNode.addChild(createWithValue("FILE", file.getValue())
						.addChildValue("DESCRIPTION", traverse(file, "TITL").getValue())
					);
				noteCitationTo(multimediaRecord, destinationNode, destination);
			}
			else{
				files = traverseAsList(object, "FILE");
				destinationNode.addChildValue("MEDIA_TYPE", traverse(object, "FORM.TYPE").getValue());
				final String title = traverse(object, "TITL").getValue();
				for(final GedcomNode file : files)
					destinationNode.addChild(createWithValue("FILE", file.getValue())
						.addChildValue("DESCRIPTION", title)
					);
				if(files.size() == 1){
					destinationSourceReference.addChildValue("CUTOUT", traverse(object, "_CUTD").getValue());
					//TODO remember which one has the _PREF tag
//					final boolean preferred = ("Y".equals(traverse(object, "_PREF").getValue()));
				}

				destination.addSource(destinationNode);
				destinationSourceReference.withXRef(destinationNode.getID());
			}
		}

			final List<GedcomNode> extracts = traverseAsList(parent, extractionsTag);
			assignExtractionsTo(extracts, destinationNode);
			noteCitationTo(parent, destinationNode, destination);
			destinationSourceReference.addChildValue("CREDIBILITY", traverse(parent, "QUAY").getValue());
	}

	/*
		for-each SOUR.TEXT
			pick-each: SOURCE.FILE.EXTRACT.value = SOUR.DATA.TEXT.value
	*/
	private void assignExtractionsTo(final List<GedcomNode> extracts, final GedcomNode destinationSource){
		final List<GedcomNode> destinationFiles = traverseAsList(destinationSource, "FILE");
		if(extracts.size() > destinationFiles.size()){
			//collect all extractions and assign to first
			final StringJoiner sj = new StringJoiner("\n");
			for(int index = 0; index < extracts.size(); index ++)
				sj.add("EXTRACT " + index)
					.add(extracts.get(index).getValue());
			destinationFiles.get(0)
				.addChildValue("EXTRACT", sj.toString());
		}
		else
			//otherwise distribute extractions
			for(int index = 0; index < extracts.size(); index ++)
				destinationFiles.get(index)
					.addChildValue("EXTRACT", extracts.get(index).getValue());
	}

	/*
	for-each OBJE id create SOURCE
		SOURCE.id = OBJE.id
		for-each OBJE.FILE create SOURCE.FILE
			pick-each: SOURCE.FILE.DESCRIPTION.value = OBJE.FILE.TITL.value
			pick-one: SOURCE.MEDIA_TYPE.value = OBJE.FILE.FORM.TYPE.value
		transfer OBJE.NOTE to SOURCE.NOTE
	*/
	void multimediaRecordTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final GedcomNode destinationSource = create("SOURCE");
		final List<GedcomNode> objects = traverseAsList(parent, "OBJE");
		for(final GedcomNode object : objects){
			final List<GedcomNode> files = traverseAsList(object, "FILE");
			String mediaType = null;
			for(int i = 0; mediaType == null && i < files.size(); i ++)
				mediaType = traverse(files.get(i), "FORM.TYPE").getValue();
			destinationSource.addChildValue("MEDIA_TYPE", mediaType);
			for(final GedcomNode file : files)
				destinationSource.addChild(createWithValue("FILE", file.getValue())
					.addChildValue("DESCRIPTION", traverse(file, "TITL").getValue())
				);
			noteCitationTo(object, destinationSource, destination);
		}

		final List<GedcomNode> extracts = traverseAsList(parent, "TEXT");
		assignExtractionsTo(extracts, destinationSource);
		noteCitationTo(parent, destinationSource, destination);
		destinationSource.addChildValue("CREDIBILITY", traverse(parent, "QUAY").getValue());

		final String sourceID = destination.addSource(destinationSource);
		destinationNode.addChildReference("SOURCE", sourceID);
	}

	/*
	for-each REPO id create REPOSITORY
		REPOSITORY.id = REPO.id
		REPOSITORY.NAME.value = REPO.NAME.value
		transfer REPO.ADDR to REPOSITORY.PLACE
		transfer REPO.NOTE to REPOSITORY.NOTE
	*/
	void repositoryRecordTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> repositories = parent.getChildrenWithTag("REPO");
		for(final GedcomNode repository : repositories){
			final GedcomNode destinationRepository = createWithID("REPOSITORY", repository.getID())
				.addChildValue("NAME", traverse(repository, "NAME").getValue());
			placeAddressStructureTo(parent, destinationRepository, destination);
			noteCitationTo(parent, destinationRepository, destination);

			destinationNode.addChild(destinationRepository);
		}
	}

	/*
	for-each PLAC create PLACE
		PLACE.NAME.value = PLAC.value
		PLACE.MAP.LATITUDE.value = PLAC.MAP.LATI.value
		PLACE.MAP.LONGITUDE.value = PLAC.MAP.LONG.value
		transfer PLAC.NOTE to PLACE.NOTE
	*/
	void placeAddressStructureTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final GedcomNode address = traverse(parent, "ADDR");
		final GedcomNode place = traverse(parent, "PLAC");
		if(!address.isEmpty() || !place.isEmpty()){
			final GedcomNode map = traverse(place, "MAP");
			final GedcomNode destinationPlace = create("PLACE")
				.addChildValue("NAME", place.getValue())
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

	/*
	for-each [PHON|EMAIL|FAX|WWW] create CONTACT
		for-each PHONE value create PHONE
			PHONE.value = PHON.value
		for-each EMAIL value create EMAIL
			EMAIL.value = EMAIL.value
		for-each FAX value create PHONE
			PHONE.value = FAX.value
			PHONE.TYPE.value = "fax"
		for-each WWW value create URL
			URL.value = WWW.value
	*/
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

	/*
	for-each SOUR xref create SOURCE, SOURCE[ref]
		SOURCE.xref = SOUR.xref
		SOURCE.LOCATION.value = SOUR.PAGE.value
		SOURCE.ROLE.value = SOUR.EVEN.ROLE.value
		load SOURCE[rec] from SOUR.xref
		SOURCE[rec].EVENT = SOUR.EVEN.value
		SOURCE[rec].DATE.value = SOUR.DATA.DATE.value
		for-each SOUR.OBJE xref
			load OBJE[rec] from SOUR.OBJE.xref
			for-each OBJE[rec].FILE
				pick-each: SOURCE.FILE.value = OBJE[rec].FILE.value
				pick-each: SOURCE.FILE.DESCRIPTION.value = OBJE[rec].TITL.value
				pick-one: SOURCE.MEDIA_TYPE.value = OBJE[rec].FORM.TYPE.value
			transfer OBJE[rec].NOTE to SOURCE.NOTE
		for-each SOUR.OBJE
			pick-each: SOURCE.FILE.value = SOUR.OBJE.FILE.value
			pick-each: SOURCE.FILE.DESCRIPTION.value = SOUR.OBJE.TITL.value
			pick-each: SOURCE[ref].CUTOUT = SOUR.OBJE._CUTD.value
			pick-one: SOURCE.MEDIA_TYPE.value = SOUR.OBJE.FORM.TYPE.value
			remember which one has the _PREF tag
		for-each SOUR.DATA.TEXT
			pick-each: SOURCE.FILE.EXTRACT.value = SOUR.DATA.TEXT.value
		transfer SOUR.NOTE to SOURCE.NOTE
		SOURCE[ref].CREDIBILITY.value = SOUR.QUAY.value
	for-each SOUR value create SOURCE, SOURCE[ref]
		SOURCE.TITLE = SOUR.value
		transfer SOUR.NOTE to SOURCE[ref].NOTE
		for-each SOUR.OBJE xref
			load OBJE[rec] from SOUR.OBJE.xref
			for-each OBJE[rec].FILE
				pick-each: SOURCE.FILE.value = OBJE[rec].FILE.value
				pick-each: SOURCE.FILE.DESCRIPTION.value = OBJE[rec].FILE.TITL.value
				pick-one: SOURCE.MEDIA_TYPE.value = OBJE[rec].FORM.TYPE.value
			transfer OBJE[rec].NOTE to SOURCE.NOTE
		for-each SOUR.OBJE
			pick-each: SOURCE.FILE.value = SOUR.OBJE.FILE.value
			pick-each: SOURCE.FILE.DESCRIPTION.value = SOUR.OBJE.TITL.value
			pick-each: SOURCE[ref].CUTOUT = SOUR.OBJE._CUTD.value
			pick-one: SOURCE.MEDIA_TYPE.value = SOUR.OBJE.FORM.TYPE.value
			remember which one has the _PREF tag
		for-each SOUR.TEXT
			pick-each: SOURCE.FILE.EXTRACT.value = SOUR.DATA.TEXT.value
		transfer SOUR.NOTE to SOURCE.NOTE
		SOURCE[ref].CREDIBILITY.value = SOUR.QUAY.value
	*/
	void sourceCitationTo(final GedcomNode parent, final GedcomNode destinationNode, final Gedcom origin, final Flef destination){
		final List<GedcomNode> sourceCitations = parent.getChildrenWithTag("SOUR");
		for(final GedcomNode sourceCitation : sourceCitations){
			final GedcomNode destinationSource = create("SOURCE");
			final GedcomNode destinationSourceReference = create("SOURCE");
			String sourceCitationXRef = sourceCitation.getXRef();
			if(sourceCitationXRef == null){
				destinationSource.addChildValue("TITLE", sourceCitation.getValue());
				multimediaCitationTo(sourceCitation, destinationSource, destinationSourceReference, origin, destination,
					"TEXT");

				//this is in fact a record, not a citation
				destinationSource.withID(destinationSource.getXRef());
				destinationSource.clearXRef();
			}
			else{
				destinationSource.withID(sourceCitationXRef)
					.addChildValue("LOCATION", traverse(sourceCitation, "PAGE").getValue())
					.addChildValue("ROLE", traverse(sourceCitation, "EVENT.ROLE").getValue());
				//load source by xref
				final GedcomNode sourceRecord = origin.getSource(sourceCitationXRef);
				final String sourceCitationValue = traverse(sourceCitation, "EVENT").getValue();
				if(sourceCitationValue != null){
					GedcomNode event = traverse(sourceRecord, "EVENT");
					if(event.isEmpty()){
						event = createWithValue("EVENT", sourceCitationValue);
						sourceRecord.addChild(event);
					}
					else
						event.withValue(event.getValue() + "," + sourceCitationValue);
				}
				final GedcomNode sourceCitationDate = traverse(sourceRecord, "DATA.DATE");
				if(sourceCitationDate != null){
					final GedcomNode date = traverse(sourceRecord, "DATE");
					if(date.isEmpty()){
						final String value = sourceCitationDate.getValue();
						sourceRecord.addChild(create("DATE")
							.withValue(CalendarParserBuilder.removeCalendarType(value))
							.addChildValue("CALENDAR", CalendarParserBuilder.getCalendarType(value))
						);
					}
					else
						date.withValue(date.getValue() + "," + sourceCitationDate);
				}

				multimediaCitationTo(sourceCitation, destinationSource, destinationSourceReference, origin, destination,
					"DATA.TEXT");
			}

			sourceCitationXRef = destination.addSource(destinationSource);

			//add source citation:
			destinationNode.addChild(destinationSourceReference
				.withXRef(sourceCitationXRef));
		}
	}

	/*
	for-each SOUR id create SOURCE
		SOURCE.id = SOUR.id
		SOURCE.EVENT.value = SOUR.DATA.EVEN.value
		SOURCE.TITLE.value = SOUR.TITL.value
		SOURCE.DATE.value = SOUR.DATA.EVEN.DATE.value
		SOURCE.AUTHOR.value = SOUR.AUTH.value
		SOURCE.PUBLICATION_FACTS.value = SOUR.PUBL.value
		transfer SOUR.REPO to SOURCE.REPOSITORY
		for-each SOUR.OBJE xref
			load OBJE[rec] from SOUR.OBJE.xref
			for-each OBJE[rec].FILE
				pick-each: SOURCE.FILE.value = OBJE[rec].FILE.value
				pick-each: SOURCE.FILE.DESCRIPTION.value = OBJE[rec].TITL.value
				pick-one: SOURCE.MEDIA_TYPE.value = OBJE[rec].FORM.MEDI.value
			transfer OBJE[rec].NOTE to SOURCE.NOTE
		for-each SOUR.OBJE
			pick-each: SOURCE.FILE.value = SOUR.OBJE.FILE.value
			pick-each: SOURCE.FILE.DESCRIPTION.value = SOUR.OBJE.TITL.value
			pick-each: SOURCE[ref].CUTOUT = SOUR.OBJE._CUTD.value
			pick-one: SOURCE.MEDIA_TYPE.value = SOUR.OBJE.FORM.MEDI.value
			remember which one has the _PREF tag
		for-each SOUR.TEXT
			pick-each: SOURCE.FILE.EXTRACT.value = SOUR.DATA.TEXT.value
		transfer SOUR.NOTE to SOURCE.NOTE
	*/
	void sourceRecordTo(final GedcomNode parent, final GedcomNode destinationNode, final Gedcom origin, final Flef destination){
		final List<GedcomNode> sources = parent.getChildrenWithTag("SOUR");
		for(final GedcomNode source : sources){
			final String value = traverse(source, "DATA.EVEN.DATE").getValue();
			final GedcomNode destinationSource = createWithIDValue("SOURCE", source.getID(), source.getValue())
				.addChildValue("EVENT", traverse(source, "DATA.EVEN").getValue())
				.addChildValue("TITLE", traverse(source, "TITL").getValue())
				.addChild(create("DATE")
					.withValue(CalendarParserBuilder.removeCalendarType(value))
					.addChildValue("CALENDAR", CalendarParserBuilder.getCalendarType(value))
				)
				.addChildValue("AUTHOR", traverse(source, "AUTH").getValue())
				.addChildValue("PUBLICATION_FACTS", traverse(source, "PUBL").getValue());
			sourceRepositoryCitationTo(parent, destinationNode, destination);
			final GedcomNode destinationSourceReference = create("SOURCE");
			multimediaCitationTo(source, destinationSource, destinationSourceReference, origin, destination, "TEXT");

			final String sourceID = destination.addSource(destinationSource);

			//add source citation:
			destinationNode.addChild(destinationSourceReference
				.withXRef(sourceID));
		}
	}

	/*
	for-each REPO xref|NULL create REPOSITORY
		REPOSITORY.xref = REPO.xref
		REPOSITORY.LOCATION.value = REPO.CALN.value
		transfer REPO.NOTE to REPOSITORY.NOTE
	*/
	void sourceRepositoryCitationTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> citations = parent.getChildrenWithTag("REPO");
		for(final GedcomNode citation : citations){
			String citationXRef = citation.getXRef();
			final GedcomNode repositoryCitation = create("REPOSITORY")
				.addChildValue("LOCATION", traverse(citation, "CALN").getValue());
			noteCitationTo(citation, repositoryCitation, destination);
			if(citationXRef == null)
				citationXRef = destination.addRepository(create("REPOSITORY")
					.addChildValue("NAME", "-- repository --")
				);
			repositoryCitation.withXRef(citationXRef);

			destinationNode.addChild(repositoryCitation);
		}
	}

	/*
	for-each FAMS xref create FAMILY_SPOUSE
		FAMILY_SPOUSE.xref = FAMS.xref
		transfer FAMS.NOTE to FAMILY_SPOUSE.NOTE
	*/
	void spouseToFamilyLinkTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> links = parent.getChildrenWithTag("FAMS");
		for(final GedcomNode link : links){
			final GedcomNode destinationFamilySpouse = create("FAMILY_SPOUSE")
				.withXRef(link.getXRef());
			noteCitationTo(link, destinationFamilySpouse, destination);

			destinationNode.addChild(destinationFamilySpouse);
		}
	}

	/*
	for-each FAMC xref create FAMILY_CHILD
		FAMILY_CHILD.xref = FAMC.xref
		FAMILY_CHILD.PEDIGREE.PARENT1.value = FAMC.PEDI.value
		FAMILY_CHILD.PEDIGREE.PARENT2.value = FAMC.PEDI.value
		FAMILY_CHILD.CERTAINTY.value = FAMC.STAT.value
		transfer FAMC.NOTE to FAMILY_CHILD.NOTE
	*/
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

	/*
	for-each NOTE xref create NOTE
		NOTE.xref = NOTE.xref
	*/
	void noteCitationTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> notes = parent.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes){
			String noteXRef = note.getXRef();
			if(noteXRef == null)
				noteXRef = destination.addNote(createWithValue("NOTE", note.getValue()));

			destinationNode.addChildReference("NOTE", noteXRef);
		}
	}

	/*
	for-each NOTE value create NOTE
		NOTE.value = NOTE.value
	*/
	void noteRecordTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> notes = parent.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes){
			final String noteID = destination.addNote(createWithIDValue("NOTE", note.getID(), note.getValue()));

			destinationNode.addChildReference("NOTE", noteID);
		}
	}


	/*
	for-each FAMILY id create FAM
		FAM.id = FAMILY.id
		FAM.HUSB.xref = FAMILY.SPOUSE1.xref
		FAM.WIFE.xref = FAMILY.SPOUSE2.xref
		for-each CHILD create FAM.CHIL
			FAM.CHIL.xref = CHILD.xref
		transfer FAMILY[EVENT] to FAM[EVEN]
		FAM.NCHI.value = EVENT{CHILDREN_COUNT}.DESCRIPTION.value
		transfer FAMILY.NOTE to FAM.NOTE
		transfer SOURCE xref to SOUR,OBJE
		FAM.RESN.value = FAMILY.RESTRICTION.value
	*/
	void familyRecordFrom(final GedcomNode parent, final Flef origin, final Gedcom destination){
		final GedcomNode destinationFamily = create("FAM");
		final List<GedcomNode> families = traverseAsList(parent, "FAMILY");
		for(final GedcomNode family : families){
			destinationFamily.withID(family.getID())
				.addChildValue("HUSB", traverse(family, "SPOUSE1").getXRef())
				.addChildValue("WIFE", traverse(family, "SPOUSE2").getXRef());
			final List<GedcomNode> children = traverseAsList(family, "CHILD");
			for(final GedcomNode child : children)
				destinationFamily.addChildReference("CHIL", child.getXRef());
			final List<GedcomNode> events = family.getChildrenWithTag("EVENT");
			//scan events one by one, maintaining order
			for(final GedcomNode event : events){
				final String tagTo = FAMILY_TO_FAM.get(traverse(event, "TYPE").getValue());
				final GedcomNode destinationEvent = createEventFrom(tagTo, event, origin);
				destinationFamily.addChild(destinationEvent);
			}
			noteCitationFrom(family, destinationFamily);
			sourceCitationFrom(family, destinationFamily, origin);
			multimediaRecordFrom(family, destinationFamily);
			sourceCitationFrom(family, destinationFamily, origin);
		}
		destinationFamily.addChildValue("CREDIBILITY", traverse(parent, "RESN").getValue());

		destination.addFamily(destinationFamily);
	}

	/*
	value = TYPE.value
	TYPE.value = DESCRIPTION.value
	DATE.value = DATE.value
	transfer PLACE to PLAC,ADDR
	AGNC.value = AGENCY.value
	CAUS.value = CAUSE.value
	transfer NOTE to NOTE
	transfer SOURCE to SOUR,OBJE
	FAMC.value = FAMILY_CHILD.value
	FAMC.ADOP.value = FAMILY_CHILD.ADOPTED_BY.value
	*/
	private GedcomNode createEventFrom(final String tagTo, final GedcomNode event, final Flef origin){
		final GedcomNode destinationEvent = (tagTo != null? create(tagTo):
				createWithValue("EVEN", traverse(event, "TYPE").getValue()))
			.addChildValue("TYPE", traverse(event, "DESCRIPTION").getValue())
			.addChildValue("DATE", traverse(event, "DATE").getValue());
		placeStructureFrom(event, destinationEvent, origin);
		addressStructureFrom(event, destinationEvent, origin);
		destinationEvent.addChildValue("AGNC", traverse(event, "AGENCY").getValue())
			.addChildValue("CAUS", traverse(event, "CAUSE").getValue());
		//EVENT BIRTH
		final GedcomNode familyChild = traverse(event, "FAMILY_CHILD");
		if(!familyChild.isEmpty())
			destinationEvent.addChild(createWithReference("FAMC", familyChild.getXRef())
				//EVENT ADOPTION
				.addChildValue("ADOP", traverse(familyChild, "ADOPTED_BY").getValue())
			);
		noteCitationFrom(event, destinationEvent);
		sourceCitationFrom(event, destinationEvent, origin);
		destinationEvent.addChildValue("RESN", traverse(event, "RESTRICTION").getValue());
		return destinationEvent;
	}

	void sourceCitationFrom(final GedcomNode parent, final GedcomNode destinationNode, final Flef origin){
		final List<GedcomNode> sourceCitations = parent.getChildrenWithTag("SOURCE");
		for(final GedcomNode sourceCitation : sourceCitations){
			final String sourceCitationXRef = sourceCitation.getXRef();
			final GedcomNode sourceRecord = origin.getSource(sourceCitationXRef);
			//create source:
			final GedcomNode destinationSource = createWithReference("SOUR", sourceCitationXRef)
				.addChildValue("PAGE", traverse(sourceCitation, "LOCATION").getValue())
				.addChild(create("EVEN")
					.addChildValue("ROLE", traverse(sourceCitation, "ROLE").getValue())
				)
				.addChild(create("DATA")
					.addChildValue("DATE", traverse(sourceRecord, "DATE").getValue())
					.addChildValue("TEXT", traverse(sourceCitation, "FILE.EXTRACT").getValue())
				);
			final List<GedcomNode> files = sourceRecord.getChildrenWithTag("FILE");
			final String mediaType = traverse(sourceRecord, "MEDIA_TYPE").getValue();
			for(final GedcomNode file : files){
				destinationSource.addChild(create("OBJE"))
					.addChildValue("FILE", file.getValue())
					.addChildValue("TITL", traverse(file, "DESCRIPTION").getValue())
					.addChildValue("_CUTD", traverse(sourceCitation, "CUTOUT").getValue())
					.addChild(create("FORM")
						.addChildValue("MEDI", mediaType)
					);
				//TODO restore the one that has the _PREF tag
			}
			destinationSource.addChildValue("QUAY", traverse(sourceCitation, "CREDIBILITY").getValue());
			noteCitationFrom(sourceCitation, destinationSource);

			destinationNode.addChild(destinationSource);
		}
	}

	/*
	for-each SOURCE id create SOUR
		SOUR.id = SOURCE.id
		SOUR.DATA.EVEN.value = SOURCE.EVENT.value
		SOUR.TITL.value = SOURCE.TITLE.value
		SOUR.DATA.EVEN.DATE.value = SOURCE.DATE.value
		SOUR.AUTH.value = SOURCE.AUTHOR.value
		SOUR.PUBL.value = SOURCE.PUBLICATION_FACTS.value
		transfer SOURCE.REPOSITORY to SOUR.REPO
		transfer SOURCE.NOTE to SOUR.OBJE
		for-each SOURCE.FILE xref
			load SOURCE[rec] from SOURCE.xref
			for-each SOURCE[rec].FILE
				pick-each: OBJE[rec].FILE.value = SOURCE.FILE.value
				pick-each: OBJE[rec].TITL.value = SOURCE.FILE.DESCRIPTION.value
				pick-one: OBJE[rec].FORM.TYPE.value = SOURCE.MEDIA_TYPE.value
			transfer SOURCE.NOTE = OBJE[rec].NOTE
		for-each SOURCE.FILE
			pick-each: SOUR.OBJE.FILE.value = SOURCE.FILE.value
			pick-each: SOUR.OBJE.TITL.value = SOURCE.FILE.DESCRIPTION.value
			pick-each: SOUR.OBJE._CUTD.value = SOURCE[ref].CUTOUT
			pick-each: SOUR.DATA.TEXT.value = SOURCE.FILE.EXTRACT.value
			pick-one: SOUR.OBJE.FORM.MEDI.value = SOURCE.MEDIA_TYPE.value
			remember which one has the PREFERRED tag
		transfer SOURCE.NOTE to SOUR.NOTE
	*/
	void sourceRecordFrom(final GedcomNode parent, final GedcomNode destinationNode){
		final List<GedcomNode> sources = parent.getChildrenWithTag("SOURCE");
		for(final GedcomNode source : sources){
			//create source:
			final GedcomNode destinationSource = createWithID("SOUR", source.getID())
				.addChildValue("PAGE", traverse(source, "LOCATION").getValue())
				.addChild(create("EVEN")
					.addChildValue("ROLE", traverse(source, "ROLE").getValue())
				)
				.addChild(create("DATA")
					.addChildValue("DATE", traverse(source, "DATE").getValue())
					.addChildValue("TEXT", traverse(source, "FILE.EXTRACT").getValue())
				);
			final List<GedcomNode> files = source.getChildrenWithTag("FILE");
			final String mediaType = traverse(source, "MEDIA_TYPE").getValue();
			for(final GedcomNode file : files){
				destinationSource.addChild(create("OBJE"))
					.addChildValue("FILE", file.getValue())
					.addChildValue("TITL", traverse(file, "DESCRIPTION").getValue())
					.addChildValue("_CUTD", traverse(source, "CUTOUT").getValue())
					.addChild(create("FORM")
						.addChildValue("MEDI", mediaType)
					);
				//TODO restore the one that has the _PREF tag
			}
			destinationSource.addChildValue("QUAY", traverse(source, "CREDIBILITY").getValue());
			noteCitationFrom(source, destinationSource);

			destinationNode.addChild(destinationSource);
		}
	}

	/*
	for-each HEADER value create HEAD
		HEAD.SOUR.value = HEADER.SOURCE.value
		HEAD.SOUR.VERS.value = HEADER.SOURCE.VERSION.value
		HEAD.SOUR.NAME.value = HEADER.SOURCE.NAME.value
		HEAD.SOUR.CORP.value = HEADER.SOURCE.CORPORATE.value
		HEAD.DATE.value = HEADER.DATE.value
		HEAD.SUBM.xref = HEADER.SUBMITTER.xref
		HEAD.COPR.value = HEADER.COPYRIGHT.value
		HEAD.GEDC.VERS.value = "5.5.1"
		HEAD.GEDC.FORM.value = "LINEAGE-LINKED"
		HEAD.CHAR.value = "UTF-8"
		HEAD.LANG.value = HEADER.DEFAULT_LOCALE.value
		HEAD.NOTE.value = HEADER.NOTE.value
	*/
	void headerFrom(final GedcomNode parent, final GedcomNode destinationNode){
		final GedcomNode header = parent.getChildrenWithTag("HEADER").get(0);
		final GedcomNode source = traverse(header, "SOURCE");
		final String date = traverse(header, "DATE")
			.getValue();
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
			.addChildValue("NOTE", traverse(source, "NOTE")
				.getValue());

		destinationNode.addChild(destinationHeader);
	}

	/*
	for-each SOURCE id create OBJE
		OBJE.id = SOURCE.id
		for-each SOURCE.FILE create OBJE.FILE
			pick-each: OBJE.FILE.TITL.value = SOURCE.FILE.DESCRIPTION.value
			pick-each: OBJE.FILE.FORM.MEDI.value = SOURCE.MEDIA_TYPE.value
		transfer SOURCE.NOTE to OBJE.NOTE
	*/
	void multimediaRecordFrom(final GedcomNode parent, final GedcomNode destinationNode){
		final GedcomNode destinationObject = create("OBJE");
		final List<GedcomNode> sources = traverseAsList(parent, "SOURCE");
		for(final GedcomNode source : sources){
			final List<GedcomNode> files = traverseAsList(source, "FILE");
			final String mediaType = traverse(source, "MEDIA_TYPE").getValue();
			for(final GedcomNode file : files)
				destinationObject.addChild(createWithValue("FILE", file.getValue())
					.addChildValue("TITL", traverse(file, "DESCRIPTION").getValue())
					.addChild(create("FORM")
						.addChildValue("MEDI", mediaType)
					)
				);
			noteCitationFrom(source, destinationObject);

			destinationNode.addChild(destinationObject);
		}
	}

	/*
	for-each REPOSITORY id create REPO
		REPO.id = REPOSITORY.id
		REPO.NAME.value = REPOSITORY.NAME.value
		transfer REPOSITORY.PLACE to REPO.ADDR
		transfer REPOSITORY.NOTE to REPO.NOTE
	*/
	void repositoryRecordFrom(final GedcomNode parent, final GedcomNode destinationNode, final Flef origin){
		final List<GedcomNode> repositories = parent.getChildrenWithTag("REPOSITORY");
		for(final GedcomNode repository : repositories){
			final GedcomNode destinationRepository = createWithID("REPO", repository.getID())
				.addChildValue("NAME", traverse(repository, "NAME").getValue());
			placeStructureFrom(parent, destinationRepository, origin);
			noteCitationFrom(parent, destinationRepository);

			destinationNode.addChild(destinationRepository);
		}
	}

	/*
	for-each PLACE create ADDR
		ADDR.value = PLACE.ADDRESS.value
		ADDR.CITY.value = PLACE.ADDRESS.CITY
		ADDR.STAE.value = PLACE.ADDRESS.STATE
		ADDR.CTRY.value = PLACE.ADDRESS.COUNTRY
	*/
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

	/*
	for-each PLACE create PLAC
		PLAC.value = PLACE.NAME.value
		PLAC.MAP.LATI.value = PLACE.MAP.LATITUDE.value
		PLAC.MAP.LONG.value = PLACE.MAP.LONGITUDE.value
		transfer PLACE.NOTE to PLAC.NOTE
	*/
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

	/*
	for-each CONTACT.PHONE whose TYPE != "fax" create PHON
		PHON.value = CONTACT.PHONE.value
	for-each CONTACT.PHONE whose TYPE == "fax" create FAX
		FAX.value = CONTACT.PHONE.value
	for-each CONTACT.EMAIL create EMAIL
		EMAIL.value = CONTACT.EMAIL.value
	for-each CONTACT.URL create WWW
		WWW.value = CONTACT.URL.value
	*/
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

	/*
	for-each REPOSITORY xref create REPO
		REPO.xref = REPOSITORY.xref
		REPO.CALN.value = REPOSITORY.LOCATION.value
		transfer REPOSITORY.NOTE to REPO.NOTE
	*/
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

	/*
	for-each FAMILY_SPOUSE xref create FAMS
		FAMS.xref = FAMILY_SPOUSE.xref
		transfer FAMIY_SPOUSE.NOTE to FAMS.NOTE
	*/
	void spouseToFamilyLinkFrom(final GedcomNode parent, final GedcomNode destinationNode){
		final List<GedcomNode> links = parent.getChildrenWithTag("FAMILY_SPOUSE");
		for(final GedcomNode link : links){
			final GedcomNode familySpouse = create("FAMS")
				.withXRef(link.getXRef());
			noteCitationFrom(link, familySpouse);

			destinationNode.addChild(familySpouse);
		}
	}

	/*
	for-each FAMILY_CHILD xref create FAMC
		FAMC.xref = FAMILY_CHILD.xref
		FAMC.PEDI.value = "PARENT1: " + FAMILY_CHILD.PEDIGREE.PARENT1.value + ", PARENT2: " + FAMILY_CHILD.PEDIGREE.PARENT2.value
		FAMC.STAT.value = FAMILY_CHILD.CERTAINTY.value
		transfer FAMILY_CHILD.NOTE to FAMC.NOTE
	*/
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

	/*
	for-each NOTE xref create NOTE
		NOTE.xref = NOTE.xref
	*/
	void noteCitationFrom(final GedcomNode parent, final GedcomNode destinationNode){
		final List<GedcomNode> notes = parent.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			destinationNode.addChildReference("NOTE", note.getXRef());
	}

	/*
	for-each NOTE value create NOTE
		NOTE.value = NOTE.value
	*/
	void noteRecordFrom(final GedcomNode parent, final GedcomNode destinationNode, final Gedcom destination){
		final List<GedcomNode> notes = parent.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes){
			final String noteID = destination.addNote(createWithIDValue("NOTE", note.getID(), note.getValue()));

			destinationNode.addChildReference("NOTE", noteID);
		}
	}

}
