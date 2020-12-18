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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;


public final class Transformer extends TransformerHelper{

	private static final Map<String, String> FAM_TO_FAMILY = new HashMap<>();
	private static final Map<String, String> FAMILY_TO_FAM = new HashMap<>();
	private static final String CUSTOM_EVENT_TAG = "@event@";
	static{
		//Life related:
		FAM_TO_FAMILY.put("BIRT", "BIRTH");
		FAM_TO_FAMILY.put("DSCR", "CHARACTERISTIC");
		FAM_TO_FAMILY.put("DEAT", "DEATH");
		FAM_TO_FAMILY.put("BURI", "BURIAL");
		FAM_TO_FAMILY.put("CREM", "CREMATION");

		//Family related:
		FAM_TO_FAMILY.put("NCHI", "CHILDREN_COUNT");
		FAM_TO_FAMILY.put("NMR", "MARRIAGES_COUNT");
		FAM_TO_FAMILY.put("ADOP", "ADOPTION");
		FAM_TO_FAMILY.put("ENGA", "ENGAGEMENT");
		FAM_TO_FAMILY.put("MARB", "MARRIAGE_BANN");
		FAM_TO_FAMILY.put("MARC", "MARRIAGE_CONTRACT");
		FAM_TO_FAMILY.put("MARL", "MARRIAGE_LICENCE");
		FAM_TO_FAMILY.put("MARS", "MARRIAGE_SETTLEMENT");
		FAM_TO_FAMILY.put("MARR", "MARRIAGE");
		FAM_TO_FAMILY.put("DIVF", "DIVORCE_FILED");
		FAM_TO_FAMILY.put("DIV", "DIVORCE");
		FAM_TO_FAMILY.put("ANUL", "ANNULMENT");

		//Achievements related:
		FAM_TO_FAMILY.put("RESI", "RESIDENCE");
		FAM_TO_FAMILY.put("EDUC", "EDUCATION");
		FAM_TO_FAMILY.put("GRAD", "GRADUATION");
		FAM_TO_FAMILY.put("OCCU", "OCCUPATION");
		FAM_TO_FAMILY.put("RETI", "RETIREMENT");

		//National/government related:
		FAM_TO_FAMILY.put("CAST", "CASTE");
		FAM_TO_FAMILY.put("NATI", "NATIONALITY");
		FAM_TO_FAMILY.put("EMIG", "EMIGRATION");
		FAM_TO_FAMILY.put("IMMI", "IMMIGRATION");
		FAM_TO_FAMILY.put("NATU", "NATURALIZATION");
		FAM_TO_FAMILY.put("CENS", "CENSUS");
		FAM_TO_FAMILY.put("SSN", "SSN");

		//Possessions and titles related:
		FAM_TO_FAMILY.put("PROP", "POSSESSION");
		FAM_TO_FAMILY.put("TITL", "TITLE");
		FAM_TO_FAMILY.put("WILL", "WILL");
		FAM_TO_FAMILY.put("PROB", "PROBATE");

		//Religious and social related:
		FAM_TO_FAMILY.put("RELI", "RELIGION");

		//custom types:
		FAM_TO_FAMILY.put("FACT", "FACT");
		FAM_TO_FAMILY.put("EVEN", CUSTOM_EVENT_TAG);

		//fill up inverse relation
		for(final Map.Entry<String, String> entry : FAM_TO_FAMILY.entrySet()){
			final String value = entry.getValue();
			if(!CUSTOM_EVENT_TAG.equals(value))
				FAMILY_TO_FAM.put(value, entry.getKey());
		}

		//TODO include these ones
		//[CHR|BAPM|BARM|BASM|BLES|CHRA|CONF|FCOM|ORDN|IDNO]
	}

	private static final Collection<String> ADDRESS_TAGS = new HashSet<>(Arrays.asList("CONT", "ADR1", "ADR2", "ADR3"));


	public Transformer(final Protocol protocol){
		super(protocol);
	}


	/*
	for-each INDI id create INDIVIDUAL
		INDIVIDUAL.id = INDI.id
		for-each INDI.NAME create INDIVIDUAL.NAME
			name-components[name, surname, name-suffix] = INDI.NAME.value
			INDIVIDUAL.NAME.TYPE.value = INDI.NAME.TYPE.value
			INDIVIDUAL.NAME.TITLE.value = INDI.NAME.NPFX.value
			INDIVIDUAL.NAME.TITLE.PHONETIC.VALUE.value = INDI.NAME.FONE.NPFX.value
			INDIVIDUAL.NAME.TITLE.TRANSCRIPTION.value = INDI.NAME.ROMN.TYPE.value
			INDIVIDUAL.NAME.TITLE.TRANSCRIPTION.TYPE.value = INDI.NAME.ROMN.value
			INDIVIDUAL.NAME.TITLE.TRANSCRIPTION.VALUE.value = INDI.NAME.ROMN.NPFX.value
			INDIVIDUAL.NAME.PERSONAL_NAME.value = INDI.NAME.GIVN.value | name-components[name]
			INDIVIDUAL.NAME.PERSONAL_NAME.PHONETIC.TYPE.value = INDI.NAME.FONE.TYPE.value
			INDIVIDUAL.NAME.PERSONAL_NAME.PHONETIC.VALUE.value = INDI.NAME.FONE.GIVN.value
			INDIVIDUAL.NAME.PERSONAL_NAME.TRANSCRIPTION.value = INDI.NAME.ROMN.TYPE.value
			INDIVIDUAL.NAME.PERSONAL_NAME.TRANSCRIPTION.TYPE.value = INDI.NAME.ROMN.value
			INDIVIDUAL.NAME.PERSONAL_NAME.TRANSCRIPTION.VALUE.value = INDI.NAME.ROMN.GIVN.value
			INDIVIDUAL.NAME.PERSONAL_NAME.NAME_SUFFIX.value = INDI.NAME.NSFX.value | name-components[name-suffix]
			INDIVIDUAL.NAME.PERSONAL_NAME.NAME_SUFFIX.PHONETIC.TYPE.value = INDI.NAME.FONE.TYPE.value
			INDIVIDUAL.NAME.PERSONAL_NAME.NAME_SUFFIX.PHONETIC.VALUE.value = INDI.NAME.FONE.NSFX.value
			INDIVIDUAL.NAME.PERSONAL_NAME.NAME_SUFFIX.TRANSCRIPTION.value = INDI.NAME.ROMN.TYPE.value
			INDIVIDUAL.NAME.PERSONAL_NAME.NAME_SUFFIX.TRANSCRIPTION.TYPE.value = INDI.NAME.ROMN.value
			INDIVIDUAL.NAME.PERSONAL_NAME.NAME_SUFFIX.TRANSCRIPTION.VALUE.value = INDI.NAME.ROMN.NSFX.value
			INDIVIDUAL.NAME.INDIVIDUAL_NICKNAME.value = INDI.NAME.NICK.value
			INDIVIDUAL.NAME.INDIVIDUAL_NICKNAME.PHONETIC.TYPE.value = INDI.NAME.FONE.TYPE.value
			INDIVIDUAL.NAME.INDIVIDUAL_NICKNAME.PHONETIC.VALUE.value = INDI.NAME.FONE.NICK.value
			INDIVIDUAL.NAME.INDIVIDUAL_NICKNAME.TRANSCRIPTION.value = INDI.NAME.ROMN.TYPE.value
			INDIVIDUAL.NAME.INDIVIDUAL_NICKNAME.TRANSCRIPTION.TYPE.value = INDI.NAME.ROMN.value
			INDIVIDUAL.NAME.INDIVIDUAL_NICKNAME.TRANSCRIPTION.VALUE.value = INDI.NAME.ROMN.NICK.value
			INDIVIDUAL.NAME.FAMILY_NAME.value = INDI.NAME.SPFX.value + ' ' + INDI.NAME.SURN.value | name-components[surname]
			INDIVIDUAL.NAME.FAMILY_NAME.PHONETIC.TYPE.value = INDI.NAME.FONE.TYPE.value
			INDIVIDUAL.NAME.FAMILY_NAME.PHONETIC.VALUE.value = INDI.NAME.FONE.SPFX.value + ' ' + INDI.NAME.FONE.SURN.value
			INDIVIDUAL.NAME.FAMILY_NAME.TRANSCRIPTION.value = INDI.NAME.ROMN.TYPE.value
			INDIVIDUAL.NAME.FAMILY_NAME.TRANSCRIPTION.TYPE.value = INDI.NAME.ROMN.value
			INDIVIDUAL.NAME.FAMILY_NAME.TRANSCRIPTION.VALUE.value = INDI.NAME.ROMN.SPFX.value + ' ' + INDI.NAME.ROMN.SURN.value
			transfer INDI.NAME.NOTE to INDIVIDUAL.NAME.NOTE
			transfer INDI.NAME.FONE.NOTE to INDIVIDUAL.NAME.NOTE
			transfer INDI.NAME.ROMN.NOTE to INDIVIDUAL.NAME.NOTE
			transfer INDI.NAME.SOUR to INDIVIDUAL.NAME.SOURCE
			transfer INDI.NAME.FONE.SOUR to INDIVIDUAL.NAME.SOURCE
			transfer INDI.NAME.ROMN.SOUR to INDIVIDUAL.NAME.SOURCE
		INDIVIDUAL.SEX.value = INDI.SEX.value
		for-each INDI.FAMC create INDIVIDUAL.FAMILY_CHILD
			INDIVIDUAL.FAMILY_CHILD.xref = INDI.FAMC.xref
			transfer INDI.FAMC.NOTE to INDIVIDUAL.FAMILY_CHILD.NOTE
		for-each INDI.FAMS create INDIVIDUAL.FAMILY_SPOUSE
			INDIVIDUAL.FAMILY_SPOUSE.xref = INDI.FAMS.xref
			transfer INDI.FAMS.NOTE to INDIVIDUAL.FAMILY_SPOUSE.NOTE
		for-each INDI.ASSO create INDIVIDUAL.ASSOCIATION
			INDIVIDUAL.ASSOCIATION.xref = INDI.ASSO.xref
			INDIVIDUAL.ASSOCIATION.TYPE.value = INDI.ASSO.TYPE.value ('FAM'>'family', 'INDI'>'individual', 'SOUR|OBJE|REPO|SUBM|NOTE|SUBN'>nothing)
			INDIVIDUAL.ASSOCIATION.RELATIONSHIP.value = INDI.ASSO.RELA.value
			transfer INDI.ASSO.NOTE to INDIVIDUAL.ASSOCIATION.NOTE
			transfer INDI.ASSO.SOUR to INDIVIDUAL.ASSOCIATION.SOURCE
		for-each INDI.ALIA create INDIVIDUAL.ALIAS
			INDIVIDUAL.ALIAS.xref = INDI.ALIA.xref
		for-each INDI.[BIRT|CHR|DEAT|BURI|CREM|ADOP|BAPM|BARM|BASM|BLES|CHRA|CONF|FCOM|ORDN|NATU|EMIG|IMMI|CENS|PROB|WILL|GRAD|RETI|EVEN] create INDIVIDUAL.EVENT
		for-each INDI.FAMC create INDIVIDUAL.FAMILY_CHILD
			INDIVIDUAL.FAMILY_CHILD.EVENT.TYPE{ADOPTION}.PEDIGREE.PARENT1.value = INDI.FAMC.PEDI.value
			INDIVIDUAL.FAMILY_CHILD.EVENT.TYPE{ADOPTION}.PEDIGREE.PARENT2.value = INDI.FAMC.PEDI.value
			INDIVIDUAL.FAMILY_CHILD.EVENT.TYPE{ADOPTION}.CERTAINTY.value = INDI.FAMC.STAT.value
		for-each INDI.[CAST|DSCR|EDUC|IDNO|NATI|NCHI|NMR|OCCU|PROP|RELI|RESI|SSN|TITL|FACT] create INDIVIDUAL.EVENT
		transfer INDI.NOTE to INDIVIDUAL.NOTE
		transfer INDI.SOUR,OBJE to INDIVIDUAL.SOURCE
		INDIVIDUAL.RESTRICTION.value = INDI.RESN.value
	*/
	void individualRecordTo(final GedcomNode individual, final Gedcom origin, final Flef destination){
		final GedcomNode destinationIndividual = createWithID("INDIVIDUAL", individual.getID());
		final List<GedcomNode> nameStructures = traverseAsList(individual, "NAME[]");
		for(final GedcomNode nameStructure : nameStructures){
			String individualName = traverse(nameStructure, "GIVN").getValue();
			String individualNameSuffix = traverse(nameStructure, "NSFX").getValue();
			String familyName = composeSurname(nameStructure, "SPFX", "SURN");
			final String nameValue = nameStructure.getValue();
			if(nameValue != null){
				final int surnameBeginIndex = nameValue.indexOf('/');
				final int surnameEndIndex = nameValue.indexOf('/', surnameBeginIndex + 1);
				if(individualName == null && surnameBeginIndex > 0)
					individualName = nameValue.substring(0, surnameBeginIndex - 1);
				if(individualNameSuffix == null && surnameEndIndex >= 0 && nameValue.length() > surnameEndIndex + 2)
					individualNameSuffix = nameValue.substring(surnameEndIndex + 2);
				if(familyName == null && surnameBeginIndex >= 0)
					familyName = nameValue.substring(surnameBeginIndex + 1, (surnameEndIndex > 0? surnameEndIndex: nameValue.length() - 1));
			}

			final GedcomNode destinationName = create("NAME")
				.addChildValue("TYPE", traverse(nameStructure, "TYPE").getValue())
				.addChild(createWithValue("TITLE", traverse(nameStructure, "NPFX").getValue())
					.addChild(create("PHONETIC")
						.addChildValue("VALUE", traverse(nameStructure, "FONE.NPFX").getValue())
					)
					.addChild(createWithValue("TRANSCRIPTION", traverse(nameStructure, "ROMN.TYPE").getValue())
						.addChildValue("TYPE", traverse(nameStructure, "ROMN").getValue())
						.addChildValue("VALUE", traverse(nameStructure, "ROMN.NPFX").getValue())
					)
				)
				.addChild(createWithValue("INDIVIDUAL_NAME", individualName)
					.addChild(create("PHONETIC")
						.addChildValue("VALUE", traverse(nameStructure, "FONE.GIVN").getValue())
					)
					.addChild(createWithValue("TRANSCRIPTION", traverse(nameStructure, "ROMN.TYPE").getValue())
						.addChildValue("TYPE", traverse(nameStructure, "ROMN").getValue())
						.addChildValue("VALUE", traverse(nameStructure, "ROMN.GIVN").getValue())
					)
					.addChild(createWithValue("SUFFIX", individualNameSuffix)
						.addChild(create("PHONETIC")
							.addChildValue("VALUE", traverse(nameStructure, "FONE.NSFX").getValue())
						)
						.addChild(createWithValue("TRANSCRIPTION", traverse(nameStructure, "ROMN.TYPE").getValue())
							.addChildValue("TYPE", traverse(nameStructure, "ROMN").getValue())
							.addChildValue("VALUE", traverse(nameStructure, "ROMN.NSFX").getValue())
						)
					)
				)
				.addChild(createWithValue("INDIVIDUAL_NICKNAME", traverse(nameStructure, "NICK").getValue())
					.addChild(create("PHONETIC")
						.addChildValue("VALUE", traverse(nameStructure, "FONE.NICK").getValue())
					)
					.addChild(createWithValue("TRANSCRIPTION", traverse(nameStructure, "ROMN.TYPE").getValue())
						.addChildValue("TYPE", traverse(nameStructure, "ROMN").getValue())
						.addChildValue("VALUE", traverse(nameStructure, "ROMN.NICK").getValue())
					)
				)
				.addChild(createWithValue("FAMILY_NAME", familyName)
					.addChild(create("PHONETIC")
						.addChildValue("VALUE", composeSurname(nameStructure, "FONE.SPFX", "FONE.SURN"))
					)
					.addChild(createWithValue("TRANSCRIPTION", traverse(nameStructure, "ROMN.TYPE").getValue())
						.addChildValue("TYPE", traverse(nameStructure, "ROMN").getValue())
						.addChildValue("VALUE", composeSurname(nameStructure, "ROMN.SPFX", "ROMN.SURN"))
					)
				);
			destinationIndividual.addChild(destinationName);
			noteCitationTo(nameStructure, destinationIndividual, origin, destination);
			noteCitationTo(traverse(nameStructure, "FONE"), destinationIndividual, origin, destination);
			noteCitationTo(traverse(nameStructure, "ROMN"), destinationIndividual, origin, destination);
			sourceCitationTo(nameStructure, destinationIndividual, origin, destination);
			sourceCitationTo(traverse(nameStructure, "FONE"), destinationIndividual, origin, destination);
			sourceCitationTo(traverse(nameStructure, "ROMN"), destinationIndividual, origin, destination);
		}

		destinationIndividual.addChildValue("SEX", traverse(individual, "SEX").getValue());
		final List<GedcomNode> familyChildren = traverseAsList(individual, "FAMC[]");
		for(final GedcomNode familyChild : familyChildren){
			final GedcomNode destinationFamilyChild = createWithReference("FAMILY_CHILD", familyChild.getXRef());
			noteCitationTo(familyChild, destinationFamilyChild, origin, destination);

			destinationIndividual.addChild(destinationFamilyChild);
		}
		final List<GedcomNode> spouses = traverseAsList(individual, "FAMS[]");
		for(final GedcomNode spouse : spouses){
			final GedcomNode destinationFamilySpouse = createWithReference("FAMILY_SPOUSE", spouse.getXRef());
			noteCitationTo(spouse, destinationFamilySpouse, origin, destination);

			destinationIndividual.addChild(destinationFamilySpouse);
		}
		final List<GedcomNode> associations = traverseAsList(individual, "ASSO[]");
		for(final GedcomNode association : associations){
			String type = traverse(association, "TYPE").getValue();
			if("FAM".equals(type))
				type = "family";
			else if("INDI".equals(type))
				type = "individual";
			//otherwise ignore
			final GedcomNode destinationAssociation = createWithReference("ASSOCIATION", association.getXRef())
				.addChildValue("TYPE", type)
				.addChildValue("RELATIONSHIP", traverse(association, "RELA").getValue());
			noteCitationTo(association, destinationAssociation, origin, destination);
			sourceCitationTo(association, destinationAssociation, origin, destination);

			destinationIndividual.addChild(destinationAssociation);
		}
		final List<GedcomNode> aliases = traverseAsList(individual, "ALIA[]");
		for(final GedcomNode alias : aliases){
			final GedcomNode destinationAlias = createWithReference("ALIAS", alias.getXRef());

			destinationIndividual.addChild(destinationAlias);
		}

		//scan events one by one, maintaining order
		final List<GedcomNode> nodeChildren = individual.getChildren();
		for(final GedcomNode nodeChild : nodeChildren){
			final String tagFrom = nodeChild.getTag();
			final String valueTo = FAM_TO_FAMILY.get(tagFrom);
			if(valueTo != null){
				final GedcomNode destinationEvent = eventRecordTo(individual.getID(), valueTo, nodeChild, origin, destination);

				destinationIndividual.addChildReference("EVENT", destinationEvent.getID());
			}
		}

		noteCitationTo(individual, destinationIndividual, origin, destination);
		sourceCitationTo(individual, destinationIndividual, origin, destination);
		multimediaCitationTo(individual, destinationIndividual, origin, destination);
		destinationIndividual.addChildValue("RESTRICTION", traverse(individual, "RESN").getValue());

		destination.addIndividual(destinationIndividual);
	}

	private String composeSurname(final GedcomNode nameStructure, final String surnamePrefixTag, final String surnameTag){
		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		final String surnamePrefix = traverse(nameStructure, surnamePrefixTag).getValue();
		final String surname = traverse(nameStructure, surnameTag).getValue();
		if(surnamePrefix != null)
			sj.add(surnamePrefix);
		if(surname != null)
			sj.add(surname);
		return (sj.length() > 0? sj.toString(): null);
	}

	/*
		transfer FAM[EVEN] to FAMILY[EVENT]
		create EVENT{CHILDREN_COUNT}
			EVENT{CHILDREN_COUNT}.DESCRIPTION.value = FAM.NCHI.value
		transfer FAM.NOTE to FAMILY.NOTE
		transfer SOUR,OBJE xref to SOURCE
		FAMILY.RESTRICTION.value = FAM.RESN.value
	*/
	void familyRecordTo(final GedcomNode family, final Gedcom origin, final Flef destination){
		final GedcomNode destinationFamily = createWithID("FAMILY", family.getID())
			.addChildReference("INDIVIDUAL", traverse(family, "HUSB").getXRef())
			.addChildReference("INDIVIDUAL", traverse(family, "WIFE").getXRef());
		final List<GedcomNode> children = traverseAsList(family, "CHIL[]");
		for(final GedcomNode child : children)
			destinationFamily.addChildReference("CHILD", child.getXRef());
		//scan events one by one, maintaining order
		final List<GedcomNode> nodeChildren = family.getChildren();
		for(final GedcomNode nodeChild : nodeChildren){
			final String tagFrom = nodeChild.getTag();
			final String valueTo = FAM_TO_FAMILY.get(tagFrom);
			if(valueTo != null){
				final GedcomNode destinationEvent = eventRecordTo(null, valueTo, nodeChild, origin, destination);
				destinationEvent.addChildReference("FAMILY", family.getID());
				destinationFamily.addChildReference("EVENT", destinationEvent.getID());
			}
		}
		noteCitationTo(family, destinationFamily, origin, destination);
		sourceCitationTo(family, destinationFamily, origin, destination);
		multimediaCitationTo(family, destinationFamily, origin, destination);
		destinationFamily.addChildValue("CREDIBILITY", traverse(family, "RESN").getValue());

		destination.addFamily(destinationFamily);
	}

	/*
	TYPE.value = TYPE.value
	DESCRIPTION.value = value
	DATE.value = DATE.value
	transfer PLAC,ADDR to PLACE
	AGENCY.value = AGNC.value
	CAUSE.value = CAUS.value
	transfer NOTE to NOTE
	transfer SOUR,OBJE xref to SOURCE
	FAMILY_CHILD.value = FAMC.value
	FAMILY_CHILD.ADOPTED_BY.value = FAMC.ADOP.value
	*/
	private GedcomNode eventRecordTo(final String individualID, final String valueTo, final GedcomNode event, final Gedcom origin,
			final Flef destination){
		final String type = traverse(event, "TYPE").getValue();
		//skip useless `Y` as description
		final String description = (!"BIRTH".equals(valueTo) && !"DEATH".equals(valueTo) && !"BURIAL".equals(valueTo)
			&& !"MARRIAGE".equals(valueTo)? event.getValue(): type);
		final GedcomNode destinationEvent = create("EVENT")
			.addChildValue("TYPE", (CUSTOM_EVENT_TAG.equals(valueTo)? type: valueTo))
			.addChildValue("DESCRIPTION", description);
		final GedcomNode familyChild = traverse(event, "FAMC");
		if(!familyChild.isEmpty()){
			final String adoptedBy = traverse(familyChild, "ADOP").getValue();
			if(individualID != null){
				//EVEN BIRT
				destinationEvent.addChildReference("FAMILY", familyChild.getXRef());
				//EVEN ADOP
				final GedcomNode family = origin.getFamily(familyChild.getXRef());
				final String spouse1ID = traverse(family, "HUSB").getXRef();
				final String spouse2ID = traverse(family, "WIFE").getXRef();
				if(spouse1ID != null && ("HUSB".equals(adoptedBy) || "BOTH".equals(adoptedBy)))
					destinationEvent.addChild(createWithReference("PARENT_PEDIGREE", spouse1ID)
						.addChildValue("PEDIGREE", "adopted")
					);
				if(spouse2ID != null && ("WIFE".equals(adoptedBy) || "BOTH".equals(adoptedBy)))
					destinationEvent.addChild(createWithReference("PARENT_PEDIGREE", spouse2ID)
						.addChildValue("PEDIGREE", "adopted")
					);
			}
		}
		final String dateValue = traverse(event, "DATE").getValue();
		addDateTo(dateValue, destinationEvent, destination);
		placeAddressStructureTo(event, destinationEvent, origin, destination);
		contactStructureTo(event, destinationEvent);
		destinationEvent.addChildValue("AGENCY", traverse(event, "AGNC").getValue())
			.addChildValue("CAUSE", traverse(event, "CAUS").getValue());
		noteCitationTo(event, destinationEvent, origin, destination);
		sourceCitationTo(event, destinationEvent, origin, destination);
		multimediaCitationTo(event, destinationEvent, origin, destination);
		destinationEvent.addChildValue("RESTRICTION", traverse(event, "RESN").getValue());

		destination.addEvent(destinationEvent);

		return destinationEvent;
	}

	/*
	for-each HEAD value create HEADER
		HEADER.PROTOCOL.value = "FLEF"
		HEADER.PROTOCOL.NAME.value = "Family LEgacy Format"
		HEADER.PROTOCOL.VERSION.value = "0.05"
		HEADER.SOURCE.value = HEAD.SOUR.value
		HEADER.SOURCE.NAME.value = HEAD.SOUR.NAME.value
		HEADER.SOURCE.VERSION.value = HEAD.SOUR.VERS.value
		HEADER.SOURCE.CORPORATE.value = HEAD.SOUR.CORP.value
		HEADER.DATE.value = HEAD.DATE.value + " " + HEAD.DATE.TIME.value
		HEADER.DEFAULT_LOCALE.value = HEAD.LANG.value
		HEADER.COPYRIGHT.value = HEAD.COPR.value
		HEADER.SUBMITTER.NAME = HEAD.SUBM[rec].NAME
		HEADER.SUBMITTER.PLACE.ADDRESS = HEAD.SUBM[rec].ADDR
		HEADER.SUBMITTER.PLACE.ADDRESS.CITY = HEAD.SUBM[rec].ADDR.CITY
		HEADER.SUBMITTER.PLACE.ADDRESS.STATE = HEAD.SUBM[rec].ADDR.STAE
		HEADER.SUBMITTER.PLACE.ADDRESS.COUNTRY = HEAD.SUBM[rec].ADDR.CTRY
		for-each HEAD.SUBM[rec].[PHON|EMAIL|FAX|WWW] create HEADER.SUBMITTER.CONTACT
			for-each HEAD.SUBM[rec].PHONE value create HEADER.SUBMITTER.PHONE
				HEADER.SUBMITTER.PHONE.value = PHON.value
			for-each HEAD.SUBM[rec].EMAIL value create HEADER.SUBMITTER.EMAIL
				HEADER.SUBMITTER.EMAIL.value = EMAIL.value
			for-each HEAD.SUBM[rec].FAX value create HEADER.SUBMITTER.PHONE
				HEADER.SUBMITTER.PHONE.value = FAX.value
				HEADER.SUBMITTER.PHONE.TYPE.value = "fax"
			for-each HEAD.SUBM[rec].WWW value create HEADER.SUBMITTER.URL
				HEADER.SUBMITTER.URL.value = WWW.value
		transfer HEAD.SUBM[rec].NOTE[rec] to HEADER.SUBMITTER.NOTE[rec]
		HEADER.NOTE.value = HEAD.NOTE.value
	*/
	void headerTo(final GedcomNode header, final Gedcom origin, final Flef destination){
		final GedcomNode source = traverse(header, "SOUR");
		final GedcomNode date = traverse(header, "DATE");
		final GedcomNode time = traverse(date, "TIME");
		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		JavaHelper.addValueIfNotNull(sj, date);
		JavaHelper.addValueIfNotNull(sj, time);
		final GedcomNode destinationHeader = create("HEADER")
			.addChild(createWithValue("PROTOCOL", "FLEF")
				.addChildValue("NAME", "Family LEgacy Format")
				.addChildValue("VERSION", "0.0.5")
			)
			.addChild(createWithValue("SOURCE", source.getValue())
				.addChildValue("NAME", traverse(source, "NAME").getValue())
				.addChildValue("VERSION", traverse(source, "VERS").getValue())
				.addChildValue("CORPORATE", traverse(source, "CORP").getValue())
			)
			.addChildValue("DATE", (sj.length() > 0? sj.toString(): null))
			.addChildValue("COPYRIGHT", traverse(header, "COPR").getValue());
		final GedcomNode submitter = origin.getSubmitter(traverse(header, "SUBM").getXRef());
		if(submitter != null){
			final GedcomNode submitterAddress = traverse(submitter, "ADDR");
			final GedcomNode destinationSubmitter = create("SUBMITTER")
				.addChildValue("NAME", traverse(submitter, "NAME").getValue())
				.addChild(create("PLACE")
					.addChildValue("ADDRESS", extractAddressValue(submitterAddress))
					.addChildValue("CITY", traverse(submitterAddress, "CITY").getValue())
					.addChildValue("STATE", traverse(submitterAddress, "STAE").getValue())
					.addChildValue("COUNTRY", traverse(submitterAddress, "CTRY").getValue())
				);
			contactStructureTo(submitter, destinationSubmitter);
			destinationHeader.addChild(destinationSubmitter);
		}

		final List<GedcomNode> notes = traverseAsList(header, "NOTE[]");
		for(GedcomNode note : notes){
			final String noteXRef = note.getXRef();
			if(noteXRef != null)
				note = origin.getNote(noteXRef);

			destinationHeader.addChildValue("NOTE", note.getValue());
		}

		destination.setHeader(destinationHeader);
	}

	/*
	for-each OBJE create SOURCE
		if OBJE.xref != null
			load OBJE[rec] from OBJE.xref

		for-each OBJE[rec].SOUR
			pick-one: SOURCE.LOCATION.value = OBJE[rec].SOUR.PAGE.value
			pick-one: SOURCE.ROLE.value = OBJE[rec].SOUR.EVEN.ROLE.value
			transfer OBJE[rec].SOUR.NOTE to SOURCE.NOTE
		SOURCE.CUTOUT.value = OBJE._CUTD.value
		//FIXME
		remember = OBJE._PREF{Y}
		transfer OBJE[rec].NOTE to SOURCE.NOTE
		SOURCE.CREDIBILITY.value = OBJE[rec].QUAY.value

		for-each OBJE[rec].FILE create SOURCE[rec]
			SOURCE[rec].FILE.DESCRIPTION.value = OBJE[rec].SOUR.value
			SOURCE[rec].EVENT.value = OBJE[rec].SOUR.EVEN.value
			SOURCE[rec].TITLE.value = OBJE[rec].SOUR.FILE.TITL.value
			SOURCE[rec].DATE.value = OBJE[rec].SOUR.DATA.DATE.value
			SOURCE[rec].FILE.value = OBJE[rec].FILE.value
			pick-one: SOURCE[rec].MEDIA_TYPE.value = OBJE[rec].FILE.FORM.TYPE.value
			SOURCE[rec].FILE.EXTRACT.value = OBJE[rec].SOUR.DATA.TEXT.value | SOUR.TEXT.value
		for-each OBJE.FILE create SOURCE[rec]
			SOURCE[rec].TITLE.value = OBJE.TITL.value
			SOURCE[rec].DATE.value = OBJE._DATE.value
			SOURCE[rec].FILE.value = OBJE.FILE.value
			pick-one: SOURCE[rec].MEDIA_TYPE.value = OBJE.FORM.MEDI.value
			SOURCE[rec].FILE.RESTRICTION.value = OBJE.FORM._PUBL{Y}? null: 'private'
	*/
	void multimediaCitationTo(final GedcomNode parent, final GedcomNode destinationNode, final Gedcom origin, final Flef destination){
		final List<GedcomNode> objects = traverseAsList(parent, "OBJE[]");
		for(GedcomNode object : objects){
			final String objectXRef = object.getXRef();
			if(objectXRef != null)
				//load record
				object = origin.getObject(objectXRef);

			final GedcomNode destinationSource = create("SOURCE");
			String mediaType = null;
			final StringJoiner title = new StringJoiner(", ");
			final String restriction = ("Y".equals(traverse(object, "_PUBL").getValue())? null: "private");
			final List<GedcomNode> files = traverseAsList(object, "FILE[]");
			for(final GedcomNode file : files){
				destinationSource.addChild(createWithValue("FILE", file.getValue())
					.addChildValue("RESTRICTION", restriction)
				);
				if(mediaType == null)
					mediaType = traverse(file, "FORM.TYPE").getValue();
				JavaHelper.addValueIfNotNull(title, traverse(file, "TITL").getValue());
			}
			destinationSource
				.addChildValue("MEDIA_TYPE", mediaType)
				.addChildValue("TITLE", title.toString());
			noteCitationTo(object, destinationSource, origin, destination);
			sourceCitationTo(object, destinationSource, origin, destination);
			final List<GedcomNode> sourceCitations = traverseAsList(object, "SOUR[]");
			for(final GedcomNode sourceCitation : sourceCitations){
				final List<GedcomNode[]> addedSources = singleSourceCitationTo(sourceCitation, origin, destination);
				for(final GedcomNode[] addedSource : addedSources){
					final GedcomNode destinationSourceReference = addedSource[0];
					final GedcomNode destinationSourceRecord = addedSource[1];

					destinationSourceRecord
						.addChildValue("TITLE", traverse(object, "TITL").getValue())
						.addChildValue("DATE", traverse(object, "_DATE").getValue())
						.addChildValue("MEDIA_TYPE", traverse(object, "FORM.MEDI").getValue());
					final List<GedcomNode> files2 = traverseAsList(object, "FILE[]");
					for(final GedcomNode file : files2)
						destinationSource.addChild(createWithValue("FILE", file.getValue())
							.addChildValue("RESTRICTION", restriction)
						);

					//add source citation:
					destinationNode.addChild(destinationSourceReference);
				}
			}

			destination.addSource(destinationSource);
			destinationNode.addChild(createWithReference("SOURCE", destinationSource.getID())
				.addChildValue("CUTOUT", traverse(object, "_CUTD").getValue())
			);
		}
	}

	/*
		for-each SOUR.TEXT
			pick-each: SOURCE.FILE.EXTRACT.value = SOUR.DATA.TEXT.value
	*/
	private void assignExtractionsTo(final List<GedcomNode> extracts, final GedcomNode destinationSource){
		final List<GedcomNode> destinationFiles = traverseAsList(destinationSource, "FILE[]");
		final int fileCount = destinationFiles.size();
		if(extracts.size() > fileCount){
			//collect all extractions and assign to first
			final StringJoiner sj = new StringJoiner("\n");
			for(int index = 0; index < extracts.size(); index ++)
				sj.add("EXTRACT " + index)
					.add(extracts.get(index).getValue());
			if(fileCount == 0)
				destinationFiles.add(create("FILE"));
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
		for-each OBJE.FILE create SOURCE
			pick-each: SOURCE.FILE.value = OBJE.FILE.value
			pick-each: SOURCE.FILE.DESCRIPTION.value = OBJE.FILE.TITL.value
			pick-one: SOURCE.MEDIA_TYPE.value = OBJE.FILE.FORM.TYPE.value
		transfer OBJE.NOTE to SOURCE.NOTE
	*/
	void multimediaRecordTo(final GedcomNode object, final Gedcom origin, final Flef destination){
		final GedcomNode destinationSource = create("SOURCE");
		final List<GedcomNode> files = traverseAsList(object, "FILE[]");
		String mediaType = null;
		for(int i = 0; mediaType == null && i < files.size(); i ++)
			mediaType = traverse(files.get(i), "FORM.TYPE").getValue();
		destinationSource.addChildValue("MEDIA_TYPE", mediaType);
		for(final GedcomNode file : files)
			destinationSource.addChild(createWithValue("FILE", file.getValue())
				.addChildValue("DESCRIPTION", traverse(file, "TITL").getValue())
			);

		final List<GedcomNode> extracts = traverseAsList(object, "TEXT[]");
		assignExtractionsTo(extracts, destinationSource);
		noteCitationTo(object, destinationSource, origin, destination);
		destinationSource.addChildValue("CREDIBILITY", traverse(object, "QUAY").getValue());

		destination.addSource(destinationSource);
	}

	/*
	for-each REPO id create REPOSITORY
		REPOSITORY.id = REPO.id
		REPOSITORY.NAME.value = REPO.NAME.value
		transfer REPO.ADDR to REPOSITORY.PLACE
		transfer REPO.NOTE to REPOSITORY.NOTE
	*/
	void repositoryRecordTo(final GedcomNode repository, final Gedcom origin, final Flef destination){
		final GedcomNode destinationRepository = createWithID("REPOSITORY", repository.getID())
			.addChildValue("NAME", traverse(repository, "NAME").getValue());
		placeAddressStructureTo(repository, destinationRepository, origin, destination);
		contactStructureTo(repository, destinationRepository);
		noteCitationTo(repository, destinationRepository, origin, destination);

		destination.addRepository(destinationRepository);
	}

	/*
	for-each PLAC create PLACE
		PLACE.NAME.value = PLAC.value
		PLACE.MAP.LATITUDE.value = PLAC.MAP.LATI.value
		PLACE.MAP.LONGITUDE.value = PLAC.MAP.LONG.value
		transfer PLAC.NOTE to PLACE.NOTE
	*/
	void placeAddressStructureTo(final GedcomNode parent, final GedcomNode destinationNode, final Gedcom origin, final Flef destination){
		final GedcomNode address = traverse(parent, "ADDR");
		final GedcomNode place = traverse(parent, "PLAC");
		if(!address.isEmpty() || !place.isEmpty()){
			final GedcomNode map = traverse(place, "MAP");
			final GedcomNode destinationPlace = create("PLACE")
				.addChildValue("NAME", place.getValue())
				.addChild(createWithValue("ADDRESS", extractAddressValue(address))
					.addChildValue("CITY", traverse(address, "CITY").getValue())
					.addChildValue("STATE", traverse(address, "STAE").getValue())
					.addChildValue("COUNTRY", traverse(address, "CTRY").getValue())
				)
				.addChild(create("MAP")
					.addChildValue("LATITUDE", mapCoordinateTo(map, "LATI"))
					.addChildValue("LONGITUDE", mapCoordinateTo(map, "LONG"))
				);
			noteCitationTo(place, destinationPlace, origin, destination);

			destination.addPlace(destinationPlace);
			destinationNode.addChildReference("PLACE", destinationPlace.getID());
		}
	}

	private String mapCoordinateTo(final GedcomNode map, final String coordinateType){
		String value = traverse(map, coordinateType).getValue();
		if(value != null){
			final char firstCharacter = value.charAt(0);
			final boolean negative = (firstCharacter == 'S' || firstCharacter == 'W');
			value = value.substring(1);
			if(negative)
				value = "-" + value;
		}
		return value;
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
		final List<GedcomNode> phones = traverseAsList(parent, "PHON[]");
		for(final GedcomNode phone : phones)
			destinationContact.addChildValue("PHONE", phone.getValue());
		final List<GedcomNode> emails = traverseAsList(parent, "EMAIL[]");
		for(final GedcomNode email : emails)
			destinationContact.addChildValue("EMAIL", email.getValue());
		final List<GedcomNode> faxes = traverseAsList(parent, "FAX[]");
		for(final GedcomNode fax : faxes)
			destinationContact.addChild(createWithValue("PHONE", fax.getValue())
				.addChildValue("TYPE", "fax"));
		final List<GedcomNode> urls = traverseAsList(parent, "WWW[]");
		for(final GedcomNode url : urls)
			destinationContact.addChildValue("URL", url.getValue());
		destinationNode.addChild(destinationContact);
	}

	/*
	for-each SOUR xref create SOURCE, SOURCE[rec]
		SOURCE.xref = SOUR.xref
		SOURCE.LOCATION.value = SOUR.PAGE.value
		SOURCE.ROLE.value = SOUR.EVEN.ROLE.value
		load SOURCE[rec] from SOUR.xref
		SOURCE[rec].EVENT = SOUR.EVEN.value
		SOURCE[rec].DATE.value = SOUR.DATA.DATE.value
		transfer SOUR.OBJE to SOURCE[rec].SOURCE
		for-each SOUR.DATA.TEXT
			pick-each: SOURCE[rec].FILE.EXTRACT.value = SOUR.DATA.TEXT.value
		transfer SOUR.NOTE to SOURCE[rec].NOTE
		SOURCE.CREDIBILITY.value = SOUR.QUAY.value
	for-each SOUR value create SOURCE, SOURCE[rec]
		SOURCE[rec].TITLE = SOUR.value
		transfer SOUR.OBJE to SOURCE[rec].SOURCE
		for-each SOUR.TEXT
			pick-each: SOURCE[rec].FILE.EXTRACT.value = SOUR.DATA.TEXT.value
		transfer SOUR.NOTE to SOURCE[rec].NOTE
		SOURCE.CREDIBILITY.value = SOUR.QUAY.value
	*/
	void sourceCitationTo(final GedcomNode parent, final GedcomNode destinationNode, final Gedcom origin, final Flef destination){
		final List<GedcomNode> sourceCitations = traverseAsList(parent, "SOUR[]");
		for(final GedcomNode sourceCitation : sourceCitations){
			final List<GedcomNode[]> addedSources = singleSourceCitationTo(sourceCitation, origin, destination);
			//add source citation:
			for(final GedcomNode[] addedSource : addedSources)
				destinationNode.addChild(addedSource[0]);
		}
	}

	/**
	 * @return	An array of reference and record sources.
	 */
	private List<GedcomNode[]> singleSourceCitationTo(final GedcomNode sourceCitation, final Gedcom origin, final Flef destination){
		final List<GedcomNode[]> response = new ArrayList<>();
		String sourceCitationXRef = sourceCitation.getXRef();
		if(sourceCitationXRef == null){
			final GedcomNode destinationSourceRecord = create("SOURCE")
				.addChildValue("TITLE", sourceCitation.getValue());
			multimediaCitationTo(sourceCitation, destinationSourceRecord, origin, destination);
			noteCitationTo(sourceCitation, destinationSourceRecord, origin, destination);

			destination.addSource(destinationSourceRecord);
			final GedcomNode destinationSourceReference = createWithReference("SOURCE", destinationSourceRecord.getID())
				.addChildValue("CREDIBILITY", traverse(sourceCitation, "QUAY").getValue());
			response.add(new GedcomNode[]{destinationSourceReference, destinationSourceRecord});
		}
		else{
			final List<GedcomNode> sourcesAdded = sourceRecordTo(createEmpty().addChild(origin.getSource(sourceCitationXRef)), origin,
				destination);
			for(final GedcomNode destinationSourceRecord : sourcesAdded){
				final GedcomNode destinationSourceReference = createWithReference("SOURCE", destinationSourceRecord.getID())
					.addChildValue("LOCATION", traverse(sourceCitation, "PAGE").getValue())
					.addChildValue("ROLE", traverse(sourceCitation, "EVEN.ROLE").getValue());
				//load source by xref
				final String sourceCitationValue = traverse(sourceCitation, "EVEN").getValue();
				if(sourceCitationValue != null){
					GedcomNode event = traverse(destinationSourceRecord, "EVENT");
					if(event.isEmpty()){
						event = createWithValue("EVENT", sourceCitationValue);
						destinationSourceRecord.addChild(event);
					}
					else
						event.withValue(event.getValue() + "," + sourceCitationValue);
				}
				final String sourceCitationDateValue = traverse(destinationSourceRecord, "DATA.DATE").getValue();
				addDateTo(sourceCitationDateValue, destinationSourceRecord, destination);

				multimediaCitationTo(sourceCitation, destinationSourceReference, origin, destination);
				destinationSourceReference.addChildValue("CREDIBILITY", traverse(sourceCitation, "QUAY").getValue());
				response.add(new GedcomNode[]{destinationSourceReference, destinationSourceRecord});
			}
		}
		return response;
	}

	private void addDateTo(final String value, final GedcomNode destinationNode, final Flef destination){
		if(value != null){
			final String calendarType = CalendarParserBuilder.getCalendarType(value);
			//search for calendar type
			final GedcomNode calendar = destination.getCalendarByType(calendarType);
			if(calendar.isEmpty()){
				calendar.withTag("CALENDAR")
					.addChildValue("TYPE", calendarType);

				//insert calendar
				destination.addCalendar(calendar);
			}
			destinationNode.addChild(createWithValue("DATE", CalendarParserBuilder.removeCalendarType(value))
				.addChildReference("CALENDAR", calendar.getID())
			);
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
	List<GedcomNode> sourceRecordTo(final GedcomNode parent, final Gedcom origin, final Flef destination){
		final List<GedcomNode> sourcesAdded = new ArrayList<>(0);
		final List<GedcomNode> sources = traverseAsList(parent, "SOUR[]");
		for(final GedcomNode source : sources){
			final String value = source.getValue();
			final GedcomNode sourceData = traverse(source, "DATA");
			final GedcomNode sourceDataEvent = traverse(sourceData, "EVEN");
			final String event = sourceDataEvent.getValue();
			final String title = traverse(source, "TITL").getValue();
			final String author = traverse(source, "AUTH").getValue();
			final String publicationFacts = traverse(source, "PUBL").getValue();
			final GedcomNode destinationSource = createWithIDValue("SOURCE", source.getID(), value)
				.addChildValue("EVENT", event)
				.addChildValue("TITLE", title)
				.addChildValue("AUTHOR", author)
				.addChildValue("PUBLICATION_FACTS", publicationFacts);
			final String dateValue = traverse(sourceDataEvent, "DATE").getValue();
			addDateTo(dateValue, destinationSource, destination);
			repositoryCitationTo(source, destinationSource, origin, destination);
			multimediaCitationTo(source, destinationSource, origin, destination);
			noteCitationTo(sourceData, destinationSource, origin, destination);
			noteCitationTo(source, destinationSource, origin, destination);
			final GedcomNode extract = traverse(source, "TEXT");
			assignExtractionsTo(Collections.singletonList(extract), destinationSource);


			//if destination source contains multiple sources, splits them
			final List<GedcomNode> destinationSubSources = traverseAsList(destinationSource, "SOURCE[]");
			if(destinationSubSources.isEmpty()){
				destination.addSource(destinationSource);

				sourcesAdded.add(destinationSource);
			}
			else
				for(final GedcomNode destinationSubSource : destinationSubSources){
					//transfer common data
					destinationSubSource.withValue(value)
						.addChildValue("EVENT", event)
						.addChildValue("TITLE", title)
						.addChildValue("AUTHOR", author)
						.addChildValue("PUBLICATION_FACTS", publicationFacts);
					addDateTo(dateValue, destinationSubSource, destination);
					multimediaCitationTo(source, destinationSubSource, origin, destination);
					noteCitationTo(sourceData, destinationSource, origin, destination);
					noteCitationTo(source, destinationSource, origin, destination);

					sourcesAdded.add(destinationSubSource);
				}
		}
		return sourcesAdded;
	}

	/*
	for-each REPO xref|NULL create REPOSITORY
		REPOSITORY.xref = REPO.xref
		REPOSITORY.LOCATION.value = REPO.CALN.value
		transfer REPO.NOTE to REPOSITORY.NOTE
	*/
	void repositoryCitationTo(final GedcomNode parent, final GedcomNode destinationNode, final Gedcom origin, final Flef destination){
		final List<GedcomNode> repositoryCitations = traverseAsList(parent, "REPO[]");
		for(final GedcomNode repositoryCitation : repositoryCitations){
			final GedcomNode destinationRepositoryCitation = create("REPOSITORY")
				.addChildValue("LOCATION", traverse(repositoryCitation, "CALN").getValue());
			noteCitationTo(repositoryCitation, destinationRepositoryCitation, origin, destination);
			GedcomNode repository = origin.getRepository(repositoryCitation.getXRef());
			if(repository == null){
				repository = create("REPOSITORY")
					.addChildValue("NAME", "-- repository " + destination.getRepositories().size() + " --");
				destination.addRepository(repository);
			}
			destinationRepositoryCitation.withXRef(repository.getID());

			destinationNode.addChild(destinationRepositoryCitation);
		}
	}

	/*
	for-each NOTE xref create NOTE
		NOTE.xref = NOTE.xref
	*/
	void noteCitationTo(final GedcomNode parent, final GedcomNode destinationNode, final Gedcom origin, final Flef destination){
		final List<GedcomNode> notes = traverseAsList(parent, "NOTE[]");
		for(final GedcomNode note : notes){
			String noteXRef = note.getXRef();
			noteXRef = destination.addNote(noteXRef == null? createWithValue("NOTE", note.getValue()): origin.getNote(noteXRef));

			destinationNode.addChildReference("NOTE", noteXRef);
		}
	}

	/*
	for-each NOTE value create NOTE
		NOTE.value = NOTE.value
	*/
	void noteRecordTo(final GedcomNode note, final Flef destination){
		destination.addNote(createWithIDValue("NOTE", note.getID(), note.getValue()));
	}


	/*
	for-each INDIVIDUAL id create INDI
		INDI.id = INDIVIDUAL.id
		for-each INDIVIDUAL.NAME create INDI.NAME
			INDI.NAME.TYPE.value = INDIVIDUAL.NAME.TYPE.value
			INDI.NAME.NPFX.value = INDIVIDUAL.NAME.TITLE.value
			INDI.NAME.GIVN.value = INDIVIDUAL.NAME.PERSONAL_NAME.value
			INDI.NAME.NSFX.value = INDIVIDUAL.NAME.PERSONAL_NAME.NAME_SUFFIX.value
			INDI.NAME.NICK.value = INDIVIDUAL.NAME.INDIVIDUAL_NICKNAME.value
			INDI.NAME.SURN.value = INDIVIDUAL.NAME.FAMILY_NAME.value
			INDI.NAME.FONE.NPFX.value = INDIVIDUAL.NAME.TITLE.PHONETIC.VALUE.value
			INDI.NAME.FONE.GIVN.value = INDIVIDUAL.NAME.PERSONAL_NAME.PHONETIC.VALUE.value
			INDI.NAME.FONE.NSFX.value = INDIVIDUAL.NAME.PERSONAL_NAME.NAME_SUFFIX.PHONETIC.VALUE.value
			INDI.NAME.FONE.NICK.value = INDIVIDUAL.NAME.INDIVIDUAL_NICKNAME.PHONETIC.VALUE.value
			INDI.NAME.FONE.SURN.value = INDIVIDUAL.NAME.FAMILY_NAME.PHONETIC.VALUE.value
			INDI.NAME.ROMN.TYPE.value = INDIVIDUAL.NAME.TITLE.TRANSCRIPTION.value
			INDI.NAME.ROMN.value = INDIVIDUAL.NAME.TITLE.TRANSCRIPTION.TYPE.value
			INDI.NAME.ROMN.NPFX.value = INDIVIDUAL.NAME.TITLE.TRANSCRIPTION.VALUE.value
			INDI.NAME.ROMN.TYPE.value = INDIVIDUAL.NAME.PERSONAL_NAME.TRANSCRIPTION.value
			INDI.NAME.ROMN.value = INDIVIDUAL.NAME.PERSONAL_NAME.TRANSCRIPTION.TYPE.value
			INDI.NAME.ROMN.GIVN.value = INDIVIDUAL.NAME.PERSONAL_NAME.TRANSCRIPTION.VALUE.value
			INDI.NAME.ROMN.TYPE.value = INDIVIDUAL.NAME.PERSONAL_NAME.NAME_SUFFIX.TRANSCRIPTION.value
			INDI.NAME.ROMN.value = INDIVIDUAL.NAME.PERSONAL_NAME.NAME_SUFFIX.TRANSCRIPTION.TYPE.value
			INDI.NAME.ROMN.NSFX.value = INDIVIDUAL.NAME.PERSONAL_NAME.NAME_SUFFIX.TRANSCRIPTION.VALUE.value
			INDI.NAME.ROMN.TYPE.value = INDIVIDUAL.NAME.INDIVIDUAL_NICKNAME.TRANSCRIPTION.value
			INDI.NAME.ROMN.value = INDIVIDUAL.NAME.INDIVIDUAL_NICKNAME.TRANSCRIPTION.TYPE.value
			INDI.NAME.ROMN.NICK.value = INDIVIDUAL.NAME.INDIVIDUAL_NICKNAME.TRANSCRIPTION.VALUE.value
			INDI.NAME.ROMN.TYPE.value = INDIVIDUAL.NAME.FAMILY_NAME.TRANSCRIPTION.value
			INDI.NAME.ROMN.value = INDIVIDUAL.NAME.FAMILY_NAME.TRANSCRIPTION.TYPE.value
			INDI.NAME.ROMN.SURN.value = INDIVIDUAL.NAME.FAMILY_NAME.TRANSCRIPTION.VALUE.value
			transfer INDIVIDUAL.NAME.NOTE to INDI.NAME.NOTE
			transfer INDIVIDUAL.NAME.SOURCE to INDI.NAME.SOUR
		INDI.SEX.value = INDIVIDUAL.SEX.value
		for-each INDIVIDUAL.FAMILY_CHILD create INDI.FAMC
			INDI.FAMC.xref = INDIVIDUAL.FAMILY_CHILD.xref
			transfer INDIVIDUAL.FAMILY_CHILD.NOTE to INDI.FAMC.NOTE
		for-each INDIVIDUAL.FAMILY_SPOUSE create INDI.FAMS
			INDI.FAMS.xref = INDIVIDUAL.FAMILY_SPOUSE.xref
			transfer INDIVIDUAL.FAMILY_SPOUSE.NOTE to INDI.FAMS.NOTE
		for-each INDIVIDUAL.ASSOCIATION create INDI.ASSO
			INDI.ASSO.xref = INDIVIDUAL.ASSOCIATION.xref
			INDI.ASSO.TYPE.value = INDIVIDUAL.ASSOCIATION.TYPE.value ('family'>'FAM', 'individual'>'INDI')
			INDI.ASSO.RELA.value = INDIVIDUAL.ASSOCIATION.RELATIONSHIP.value
			transfer INDIVIDUAL.ASSOCIATION.NOTE to INDI.ASSO.NOTE
			transfer INDIVIDUAL.ASSOCIATION.SOURCE to INDI.ASSO.SOUR
		for-each INDIVIDUAL.ALIAS create INDI.ALIA
			INDI.ALIA.xref = INDIVIDUAL.ALIAS.xref
		for-each INDIVIDUAL.EVENT create INDI.[BIRT|CHR|DEAT|BURI|CREM|ADOP|BAPM|BARM|BASM|BLES|CHRA|CONF|FCOM|ORDN|NATU|EMIG|IMMI|CENS|PROB|WILL|GRAD|RETI|EVEN]
			for-each INDIVIDUAL.FAMILY_CHILD create INDI.FAMC
				INDI.FAMC.PEDI.value = INDIVIDUAL.FAMILY_CHILD.EVENT.TYPE{ADOPTION}.PEDIGREE.PARENT1.value
				INDI.FAMC.PEDI.value = INDIVIDUAL.FAMILY_CHILD.EVENT.TYPE{ADOPTION}.PEDIGREE.PARENT2.value
				INDI.FAMC.STAT.value = INDIVIDUAL.FAMILY_CHILD.EVENT.TYPE{ADOPTION}.CERTAINTY.value
		for-each INDIVIDUAL.EVENT create INDI.[CAST|DSCR|EDUC|IDNO|NATI|NCHI|NMR|OCCU|PROP|RELI|RESI|SSN|TITL|FACT]
		transfer INDIVIDUAL.NOTE to INDI.NOTE
		transfer INDIVIDUAL.SOURCE to INDI.SOUR,OBJE
		INDI.RESN.value = INDIVIDUAL.RESTRICTION.value
	*/
	void individualRecordFrom(final GedcomNode individual, final Flef origin, final Gedcom destination){
		final GedcomNode destinationIndividual = createWithID("INDI", individual.getID());
		final List<GedcomNode> nameStructures = traverseAsList(individual, "NAME[]");
		for(final GedcomNode nameStructure : nameStructures){
			final String individualName = traverse(nameStructure, "INDIVIDUAL_NAME").getValue();
			final String individualNameSuffix = traverse(nameStructure, "INDIVIDUAL_NAME.SUFFIX").getValue();
			final StringJoiner familyName = new StringJoiner(StringUtils.SPACE);
			final StringJoiner familyNamePhonetic = new StringJoiner(StringUtils.SPACE);
			final StringJoiner familyNameTranscription = new StringJoiner(StringUtils.SPACE);
			for(final GedcomNode name : traverseAsList(nameStructure, "FAMILY_NAME[]")){
				JavaHelper.addValueIfNotNull(familyName, name.getValue());
				JavaHelper.addValueIfNotNull(familyNamePhonetic, traverse(name, "PHONETIC.VALUE").getValue());
				JavaHelper.addValueIfNotNull(familyNameTranscription, traverse(name, "TRANSCRIPTION.VALUE").getValue());
			}
			final StringJoiner nameValue = new StringJoiner(StringUtils.SPACE);
			if(individualName != null)
				nameValue.add(individualName);
			if(familyName.length() > 0)
				nameValue.add("/" + familyName + "/");
			if(individualNameSuffix != null)
				nameValue.add(individualNameSuffix);
			final GedcomNode destinationName = createWithValue("NAME", (nameValue.length() > 0? nameValue.toString(): null))
				.addChildValue("TYPE", traverse(nameStructure, "TYPE").getValue())
				.addChildValue("NPFX", traverse(nameStructure, "TITLE").getValue())
				.addChildValue("GIVN", individualName)
				.addChildValue("NSFX", individualNameSuffix)
				.addChildValue("NICK", traverse(nameStructure, "INDIVIDUAL_NICKNAME").getValue())
				.addChildValue("SURN", (familyName.length() > 0? familyName.toString(): null))
				.addChild(create("FONE")
					.addChildValue("NPFX", traverse(nameStructure, "TITLE.PHONETIC.VALUE").getValue())
					.addChildValue("GIVN", traverse(nameStructure, "INDIVIDUAL_NAME.PHONETIC.VALUE").getValue())
					.addChildValue("NSFX", traverse(nameStructure, "INDIVIDUAL_NAME.NAME_SUFFIX.PHONETIC.VALUE").getValue())
					.addChildValue("NICK", traverse(nameStructure, "INDIVIDUAL_NICKNAME.PHONETIC.VALUE").getValue())
					.addChildValue("SURN", (familyNamePhonetic.length() > 0? familyNamePhonetic.toString(): null))
				)
				.addChild(createWithValue("ROMN", traverse(nameStructure, "TRANSCRIPTION.TYPE").getValue())
					.addChildValue("TYPE", traverse(nameStructure, "TRANSCRIPTION").getValue())
					.addChildValue("NPFX", traverse(nameStructure, "TITLE.TRANSCRIPTION.VALUE").getValue())
					.addChildValue("GIVN", traverse(nameStructure, "INDIVIDUAL_NAME.TRANSCRIPTION.VALUE").getValue())
					.addChildValue("NSFX", traverse(nameStructure, "INDIVIDUAL_NAME.NAME_SUFFIX.TRANSCRIPTION.VALUE").getValue())
					.addChildValue("NICK", traverse(nameStructure, "INDIVIDUAL_NICKNAME.TRANSCRIPTION.VALUE").getValue())
					.addChildValue("SURN", (familyNameTranscription.length() > 0? familyNameTranscription.toString(): null))
				);
			destinationIndividual.addChild(destinationName);
			noteCitationFrom(nameStructure, destinationIndividual);
			sourceCitationFrom(nameStructure, destinationIndividual, origin);
		}

		destinationIndividual.addChildValue("SEX", traverse(individual, "SEX").getValue());
		final List<GedcomNode> associations = traverseAsList(individual, "ASSOCIATION[]");
		for(final GedcomNode association : associations){
			String type = traverse(association, "TYPE").getValue();
			if("family".equals(type))
				type = "FAM";
			else if("individual".equals(type))
				type = "INDI";
			final GedcomNode destinationAssociation = createWithReference("ASSO", association.getXRef())
				.addChildValue("TYPE", type)
				.addChildValue("RELA", traverse(association, "RELATIONSHIP").getValue());
			noteCitationFrom(association, destinationAssociation);
			sourceCitationFrom(association, destinationAssociation, origin);

			destinationIndividual.addChild(destinationAssociation);
		}
		final List<GedcomNode> aliases = traverseAsList(individual, "ALIAS[]");
		for(final GedcomNode alias : aliases){
			final GedcomNode destinationAlias = createWithReference("ALIA", alias.getXRef());

			destinationIndividual.addChild(destinationAlias);
		}
		final List<GedcomNode> spouses = traverseAsList(individual, "FAMILY_SPOUSE[]");
		for(final GedcomNode spouse : spouses){
			final GedcomNode destinationFamilySpouse = createWithReference("FAMS", spouse.getXRef());
			noteCitationFrom(spouse, destinationFamilySpouse);

			destinationIndividual.addChild(destinationFamilySpouse);
		}
		final List<GedcomNode> familyChildren = traverseAsList(individual, "FAMILY_CHILD[]");
		for(final GedcomNode familyChild : familyChildren){
			final GedcomNode destinationFamilyChild = createWithReference("FAMC", familyChild.getXRef());
			noteCitationFrom(familyChild, destinationFamilyChild);

			destinationIndividual.addChild(destinationFamilyChild);
		}

		//scan events one by one, maintaining order
		final List<GedcomNode> events = traverseAsList(individual, "EVENT[]");
		for(GedcomNode event : events){
			event = origin.getEvent(event.getXRef());

			final String tagFrom = traverse(event, "TYPE").getValue();
			final String valueTo = FAMILY_TO_FAM.get(tagFrom);
			final GedcomNode destinationEvent;
			if(valueTo != null){
				destinationEvent = eventRecordFrom(valueTo, event, origin);

				//if event is adoption, then copy over pedigree to individual
				final GedcomNode destinationFamilyChild = traverse(destinationEvent, "FAMC");
				for(final GedcomNode familyChild : familyChildren)
					if(familyChild.getXRef().equals(destinationFamilyChild.getXRef())){
						final List<GedcomNode> parentPedigrees = traverseAsList(event, "PARENT_PEDIGREE[]");
						final int parentPedigreeCount = parentPedigrees.size();
						if(parentPedigreeCount > 0){
							final boolean adoptedByParent1 = "adopted".equals(traverse(parentPedigrees.get(0), "PEDIGREE").getValue());
							final boolean adoptedByParent2 = (parentPedigreeCount > 1
								&& "adopted".equals(traverse(parentPedigrees.get(1), "PEDIGREE").getValue()));
							String pedigreeValue = null;
							if(adoptedByParent1 && adoptedByParent2)
								pedigreeValue = "BOTH";
							else if(adoptedByParent1)
								pedigreeValue = "HUSB";
							else if(adoptedByParent2)
								pedigreeValue = "WIFE";

							destinationFamilyChild
								.addChildValue("PEDI", pedigreeValue)
								.addChildValue("STAT", traverse(familyChild, "CERTAINTY").getValue());

							destinationIndividual.addChild(destinationFamilyChild);

							break;
						}
					}
			}
			else
				//custom event
				destinationEvent = eventRecordFrom(null, event, origin);

			destinationIndividual.addChild(destinationEvent);
		}

		noteCitationFrom(individual, destinationIndividual);
		sourceCitationFrom(individual, destinationIndividual, origin);
		destinationIndividual.addChildValue("RESN", traverse(individual, "RESTRICTION").getValue());

		destination.addIndividual(destinationIndividual);
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
	void familyRecordFrom(final GedcomNode family, final Flef origin, final Gedcom destination){
		final GedcomNode destinationFamily = createWithID("FAM", family.getID());
		final List<GedcomNode> individuals = traverseAsList(family, "INDIVIDUAL[]");
		final int individualCount = individuals.size();
		if(individualCount > 0)
			destinationFamily.addChildReference("HUSB", individuals.get(0).getXRef());
		if(individualCount > 1)
			destinationFamily.addChildReference("WIFE", individuals.get(1).getXRef());
		final List<GedcomNode> children = traverseAsList(family, "CHILD[]");
		for(final GedcomNode child : children)
			destinationFamily.addChildReference("CHIL", child.getXRef());
		final List<GedcomNode> events = traverseAsList(family, "EVENT[]");
		//scan events one by one, maintaining order
		for(GedcomNode event : events){
			event = origin.getEvent(event.getXRef());

			final String tagTo = FAMILY_TO_FAM.get(traverse(event, "TYPE").getValue());
			final GedcomNode destinationEvent = eventRecordFrom(tagTo, event, origin);
			destinationFamily.addChild(destinationEvent);
		}
		noteCitationFrom(family, destinationFamily);
		sourceCitationFrom(family, destinationFamily, origin);
		destinationFamily.addChildValue("CREDIBILITY", traverse(family, "RESN").getValue());

		destination.addFamily(destinationFamily);
	}

	/*
	value = DESCRIPTION.value
	TYPE.value = TYPE.value
	DATE.value = DATE.value
	transfer PLACE to PLAC,ADDR
	AGNC.value = AGENCY.value
	CAUS.value = CAUSE.value
	transfer NOTE to NOTE
	transfer SOURCE to SOUR,OBJE
	FAMC.value = FAMILY_CHILD.value
	FAMC.ADOP.value = FAMILY_CHILD.ADOPTED_BY.value
	*/
	private GedcomNode eventRecordFrom(final String tagTo, final GedcomNode event, final Flef origin){
		final GedcomNode destinationEvent;
		final String description = traverse(event, "DESCRIPTION").getValue();
		if(tagTo == null)
			destinationEvent = createWithValue("EVEN", description)
				.addChildValue("TYPE", traverse(event, "TYPE").getValue());
		else if("BIRT".equals(tagTo) || "DEAT".equals(tagTo) || "BURI".equals(tagTo) || "MARR".equals(tagTo))
			//add useless `Y` as description
			destinationEvent = createWithValue(tagTo, "Y")
				.addChildValue("TYPE", description);
		else
			destinationEvent = createWithValue(tagTo, description);
		destinationEvent.addChildValue("DATE", traverse(event, "DATE").getValue());
		placeStructureFrom(event, destinationEvent, origin);
		addressStructureFrom(event, destinationEvent, origin);
		contactStructureFrom(event, destinationEvent);
		destinationEvent.addChildValue("AGNC", traverse(event, "AGENCY").getValue())
			.addChildValue("CAUS", traverse(event, "CAUSE").getValue());

		final GedcomNode familyChild = traverse(event, "FAMILY");
		if(("ADOP".equals(tagTo) || "BIRT".equals(tagTo)) && !familyChild.isEmpty()){
			final List<GedcomNode> parentPedigrees = traverseAsList(event, "PARENT_PEDIGREE[]");
			if(!parentPedigrees.isEmpty()){
				final boolean adoptedByParent1 = "adopted".equals(traverse(parentPedigrees.get(0), "PEDIGREE").getValue());
				final boolean adoptedByParent2 = (parentPedigrees.size() > 1
					&& "adopted".equals(traverse(parentPedigrees.get(1), "PEDIGREE").getValue()));
				String pedigreeValue = null;
				if(adoptedByParent1 && adoptedByParent2)
					pedigreeValue = "BOTH";
				else if(adoptedByParent1)
					pedigreeValue = "HUSB";
				else if(adoptedByParent2)
					pedigreeValue = "WIFE";

				destinationEvent
					//EVENT BIRTH
					.addChild(createWithReference("FAMC", familyChild.getXRef())
					//EVENT ADOPTION
					.addChildValue("ADOP", pedigreeValue)
				);
			}
		}
		noteCitationFrom(event, destinationEvent);
		sourceCitationFrom(event, destinationEvent, origin);
		destinationEvent.addChildValue("RESN", traverse(event, "RESTRICTION").getValue());
		return destinationEvent;
	}

	void sourceCitationFrom(final GedcomNode parent, final GedcomNode destinationNode, final Flef origin){
		final List<GedcomNode> sourceCitations = traverseAsList(parent, "SOURCE[]");
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
			final List<GedcomNode> files = traverseAsList(sourceRecord, "FILE[]");
			final String mediaType = traverse(sourceRecord, "MEDIA_TYPE").getValue();
			for(final GedcomNode file : files){
				destinationSource.addChild(create("OBJE"))
					.addChildValue("TITL", traverse(file, "DESCRIPTION").getValue())
					.addChildValue("_DATE", traverse(sourceRecord, "DATE").getValue())
					.addChild(create("FORM")
						.addChildValue("MEDI", mediaType)
					)
					.addChildValue("FILE", file.getValue())
					.addChildValue("_CUTD", traverse(sourceCitation, "CUTOUT").getValue());
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
		SOUR.DATA.EVEN.DATE.value = SOURCE.DATE.value
		SOUR.AUTH.value = SOURCE.AUTHOR.value
		SOUR.TITL.value = SOURCE.TITLE.value
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
	void sourceRecordFrom(final GedcomNode source, final Gedcom destination){
		//create source:
		final GedcomNode destinationSource = createWithID("SOUR", source.getID())
			.addChild(create("DATA")
				.addChild(create("EVEN")
					.withValue(traverse(source, "EVENT").getValue())
					.addChildValue("DATE", traverse(source, "DATE").getValue())
				)
			)
			.addChildValue("AUTH", traverse(source, "AUTHOR").getValue())
			.addChildValue("TITL", traverse(source, "TITLE").getValue())
			.addChildValue("PUBL", traverse(source, "PUBLICATION_FACTS").getValue());
		final List<GedcomNode> files = traverseAsList(source, "FILE[]");
		//collect all the extractions:
		final StringJoiner sj = new StringJoiner("\n");
		for(final GedcomNode file : files){
			final String extract = traverse(file, "EXTRACT").getValue();
			JavaHelper.addValueIfNotNull(sj, extract);
		}
		destinationSource.addChildValue("TEXT", sj.toString());
		repositoryCitationFrom(source, destinationSource);
		noteCitationFrom(source, destinationSource);
		//FIXME multimedia link (> OBJE)

		destination.addSource(destinationSource);
	}

	/*
	for-each HEADER value create HEAD
		HEAD.SOUR.value = HEADER.SOURCE.value
		HEAD.SOUR.VERS.value = HEADER.SOURCE.VERSION.value
		HEAD.SOUR.NAME.value = HEADER.SOURCE.NAME.value
		HEAD.SOUR.CORP.value = HEADER.SOURCE.CORPORATE.value
		HEAD.DATE.value = HEADER.DATE.value
		HEAD.SUBM[rec].NAME = HEADER.SUBMITTER.NAME
		HEAD.SUBM[rec].ADDR = HEADER.SUBMITTER.PLACE.ADDRESS
		HEAD.SUBM[rec].ADDR.CITY = HEADER.SUBMITTER.PLACE.ADDRESS.CITY
		HEAD.SUBM[rec].ADDR.STAE = HEADER.SUBMITTER.PLACE.ADDRESS.STATE
		HEAD.SUBM[rec].ADDR.CTRY = HEADER.SUBMITTER.PLACE.ADDRESS.COUNTRY
		for-each HEADER.SUBMITTER.CONTACT.PHONE whose TYPE != "fax" create HEAD.SUBM[rec].PHON
			HEAD.SUBM[rec].PHON.value = HEADER.SUBMITTER.CONTACT.PHONE.value
		for-each HEADER.SUBMITTER.CONTACT.PHONE whose TYPE == "fax" create HEAD.SUBM[rec].FAX
			HEAD.SUBM[rec].FAX.value = HEADER.SUBMITTER.CONTACT.PHONE.value
		for-each HEADER.SUBMITTER.CONTACT.EMAIL create HEAD.SUBM[rec].EMAIL
			HEAD.SUBM[rec].EMAIL.value = HEADER.SUBMITTER.CONTACT.EMAIL.value
		for-each HEADER.SUBMITTER.CONTACT.URL create HEAD.SUBM[rec].WWW
			HEAD.SUBM[rec].WWW.value = HEADER.SUBMITTER.CONTACT.URL.value
		transfer HEADER.SUBMITTER.NOTE[rec] to HEAD.SUBM[rec].NOTE[rec]
		HEAD.COPR.value = HEADER.COPYRIGHT.value
		HEAD.GEDC.VERS.value = "5.5.1"
		HEAD.GEDC.FORM.value = "LINEAGE-LINKED"
		HEAD.CHAR.value = "UTF-8"
		HEAD.LANG.value = HEADER.DEFAULT_LOCALE.value
		HEAD.NOTE.value = HEADER.NOTE.value
	*/
	void headerFrom(final GedcomNode header, final Gedcom destination){
		final GedcomNode submitter = traverse(header, "SUBMITTER");
		final GedcomNode destinationSubmitter = create("SUBM")
			.addChildValue("NAME", traverse(submitter, "NAME").getValue())
			.addChild(create("ADDR")
				.withValue(traverse(submitter, "PLACE.ADDRESS").getValue())
				.addChildValue("CITY", traverse(submitter, "PLACE.ADDRESS.CITY").getValue())
				.addChildValue("STAE", traverse(submitter, "PLACE.ADDRESS.STATE").getValue())
				.addChildValue("CTRY", traverse(submitter, "PLACE.ADDRESS.COUNTRY").getValue())
			);
		contactStructureFrom(submitter, destinationSubmitter);
		noteCitationFrom(submitter, destinationSubmitter);

		destination.addSubmitter(destinationSubmitter);


		final GedcomNode source = traverse(header, "SOURCE");
		final String date = traverse(header, "DATE").getValue();
		final int timeIndex = date.indexOf(' ', 9);
		final GedcomNode destinationHeader = create("HEAD")
			.addChild(createWithValue("SOUR", source.getValue())
				.addChildValue("NAME", traverse(source, "NAME").getValue())
				.addChildValue("VERS", traverse(source, "VERSION").getValue())
				.addChildValue("CORP", traverse(source, "CORPORATE").getValue())
			)
			.addChild(createWithValue("DATE", (timeIndex > 0? date.substring(0, timeIndex): date))
				.addChildValue("TIME", (timeIndex > 0? date.substring(timeIndex + 1): null))
			)
			.addChildReference("SUBM", destinationSubmitter.getID())
			.addChildValue("COPR", traverse(source, "COPYRIGHT").getValue())
			.addChild(create("GEDC")
				.addChildValue("VERS", "5.5.1")
				.addChildValue("FORM", "LINEAGE-LINKED")
			)
			.addChildValue("CHAR", "UTF-8")
			.addChildValue("NOTE", traverse(source, "NOTE").getValue());
		destination.setHeader(destinationHeader);
	}

	/*
	for-each REPOSITORY id create REPO
		REPO.id = REPOSITORY.id
		REPO.NAME.value = REPOSITORY.NAME.value
		transfer REPOSITORY.PLACE to REPO.ADDR
		transfer REPOSITORY.NOTE to REPO.NOTE
	*/
	void repositoryRecordFrom(final GedcomNode repository, final Flef origin, final Gedcom destination){
		final String repositoryID = repository.getID();
		final GedcomNode destinationRepository = createWithID((repositoryID.startsWith("SUBM")? "SUBM": "REPO"), repositoryID)
			.addChildValue("NAME", traverse(repository, "NAME").getValue());
		addressStructureFrom(repository, destinationRepository, origin);
		contactStructureFrom(repository, destinationRepository);
		noteCitationFrom(repository, destinationRepository);

		destination.addRepository(destinationRepository);
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
			destinationNode.addChild(createWithValue("ADDR", address.getValue())
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
			final GedcomNode destinationPlace = createWithValue("PLAC", traverse(placeRecord, "NAME").getValue())
				.addChild(create("MAP")
					.addChildValue("LATI", mapCoordinateFrom(map, "LATITUDE"))
					.addChildValue("LONG", mapCoordinateFrom(map, "LONGITUDE"))
				);
			noteCitationFrom(placeRecord, destinationPlace);
			destinationNode.addChild(destinationPlace);
		}
	}

	private String mapCoordinateFrom(final GedcomNode map, final String coordinateType){
		String value = traverse(map, coordinateType).getValue();
		if(value != null){
			final boolean negative = (value.charAt(0) == '-');
			if(negative)
				value = value.substring(1);
			if("LATITUDE".equals(coordinateType))
				value = (negative? "S" + value: "N" + value);
			else
				value = (negative? "W" + value: "E" + value);
		}
		return value;
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
		final List<GedcomNode> phones = traverseAsList(contact, "PHONE[]");
		for(final GedcomNode phone : phones)
			if(!"fax".equals(traverse(phone, "TYPE").getValue()))
				destinationNode.addChildValue("PHON", phone.getValue());
		final List<GedcomNode> emails = traverseAsList(contact, "EMAIL[]");
		for(final GedcomNode email : emails)
			destinationNode.addChildValue("EMAIL", email.getValue());
		for(final GedcomNode phone : phones)
			if("fax".equals(traverse(phone, "TYPE").getValue()))
				destinationNode.addChildValue("FAX", phone.getValue());
		final List<GedcomNode> urls = traverseAsList(contact, "URL[]");
		for(final GedcomNode url : urls)
			destinationNode.addChildValue("WWW", url.getValue());
	}

	/*
	for-each REPOSITORY xref create REPO
		REPO.xref = REPOSITORY.xref
		REPO.CALN.value = REPOSITORY.LOCATION.value
		transfer REPOSITORY.NOTE to REPO.NOTE
	*/
	void repositoryCitationFrom(final GedcomNode parent, final GedcomNode destinationNode){
		final List<GedcomNode> citations = traverseAsList(parent, "REPOSITORY[]");
		for(final GedcomNode citation : citations){
			final GedcomNode repositoryCitation = createWithReference("REPO", citation.getXRef())
				.addChildValue("CALN", traverse(citation, "LOCATION").getValue());
			noteCitationFrom(citation, repositoryCitation);

			destinationNode.addChild(repositoryCitation);
		}
	}

	/*
	for-each NOTE xref create NOTE
		NOTE.xref = NOTE.xref
	*/
	void noteCitationFrom(final GedcomNode parent, final GedcomNode destinationNode){
		final List<GedcomNode> notes = traverseAsList(parent, "NOTE[]");
		for(final GedcomNode note : notes)
			destinationNode.addChildReference("NOTE", note.getXRef());
	}

	/*
	for-each NOTE value create NOTE
		NOTE.value = NOTE.value
	*/
	void noteRecordFrom(final GedcomNode note, final Gedcom destination){
		destination.addNote(createWithIDValue("NOTE", note.getID(), note.getValue()));
	}

}
