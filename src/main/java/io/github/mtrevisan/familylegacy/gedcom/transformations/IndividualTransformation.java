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

import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;


public class IndividualTransformation extends Transformation<Gedcom, Flef>{

	@Override
	public void to(final Gedcom origin, final Flef destination){
		final List<GedcomNode> individuals = origin.getIndividuals();
		for(final GedcomNode individual : individuals)
			individualRecordTo(individual, destination);
	}

	private void individualRecordTo(final GedcomNode individual, final Flef destination){
		final GedcomNode destinationIndividual = transformerTo.create("INDIVIDUAL")
			.withID(individual.getID());
		personalNameTo(individual, destinationIndividual, destination);
		destinationIndividual.addChildValue("SEX", transformerTo.traverse(individual, "SEX")
				.getValue());
		childToFamilyLinkTo(individual, destinationIndividual, destination);
		spouseToFamilyLinkTo(individual, destinationIndividual, destination);
		associationTo(individual, destinationIndividual, destination);
		aliasTo(individual, destinationIndividual, destination);
		transformerTo.eventTo(individual, destinationIndividual, destination, "BIRT", "BIRTH");
		transformerTo.eventTo(individual, destinationIndividual, destination, "ADOP", "ADOPTION");
		transformerTo.eventTo(individual, destinationIndividual, destination, "DEAT", "DEATH");
		transformerTo.eventTo(individual, destinationIndividual, destination, "BURI", "BURIAL");
		transformerTo.eventTo(individual, destinationIndividual, destination, "CREM", "CREMATION");
		transformerTo.eventTo(individual, destinationIndividual, destination, "NATU", "NATURALIZATION");
		transformerTo.eventTo(individual, destinationIndividual, destination, "EMIG", "EMIGRATION");
		transformerTo.eventTo(individual, destinationIndividual, destination, "IMMI", "IMMIGRATION");
		transformerTo.eventTo(individual, destinationIndividual, destination, "CENS", "CENSUS");
		transformerTo.eventTo(individual, destinationIndividual, destination, "PROB", "PROBATE");
		transformerTo.eventTo(individual, destinationIndividual, destination, "WILL", "WILL");
		transformerTo.eventTo(individual, destinationIndividual, destination, "GRAD", "GRADUATION");
		transformerTo.eventTo(individual, destinationIndividual, destination, "RETI", "RETIREMENT");
		transformerTo.eventTo(individual, destinationIndividual, destination, "EVEN", "EVENT");
		attributeTo(individual, destinationIndividual, destination, "CAST", "CASTE");
		attributeTo(individual, destinationIndividual, destination, "DSCR", "CHARACTERISTIC");
		attributeTo(individual, destinationIndividual, destination, "EDUC", "EDUCATION");
		attributeTo(individual, destinationIndividual, destination, "NATI", "ORIGIN");
		attributeTo(individual, destinationIndividual, destination, "NCHI", "CHILDREN_COUNT");
		attributeTo(individual, destinationIndividual, destination, "NMR", "MARRIAGES_COUNT");
		attributeTo(individual, destinationIndividual, destination, "OCCU", "OCCUPATION");
		attributeTo(individual, destinationIndividual, destination, "PROP", "POSSESSION");
		attributeTo(individual, destinationIndividual, destination, "RELI", "RELIGION");
		attributeTo(individual, destinationIndividual, destination, "RESI", "RESIDENCE");
		attributeTo(individual, destinationIndividual, destination, "SSN", "SSN");
		attributeTo(individual, destinationIndividual, destination, "TITL", "TITLE");
		attributeTo(individual, destinationIndividual, destination, "FACT", null);
		transformerTo.noteTo(individual, destinationIndividual, destination);
		transformerTo.sourceCitationTo(individual, destinationIndividual, destination);
		transformerTo.documentTo(individual, destinationIndividual, destination);
		destinationIndividual.addChildValue("RESTRICTION", transformerTo.traverse(individual, "RESN")
			.getValue());

		destination.addIndividual(destinationIndividual);
	}

	private void personalNameTo(final GedcomNode individual, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> personalNameStructures = individual.getChildrenWithTag("NAME");
		for(final GedcomNode personalNameStructure : personalNameStructures){
			final GedcomNode destinationName = transformerTo.create("NAME");
			personalNamePiecesTo(personalNameStructure, destinationName, destination);

			final GedcomNode destinationPhonetic = transformerTo.create("PHONETIC");
			personalNamePiecesTo(transformerTo.traverse(personalNameStructure, "FONE"), destinationPhonetic, destination);
			destinationName.addChild(destinationPhonetic);

			final GedcomNode destinationTranscription = transformerTo.create("TRANSCRIPTION");
			personalNamePiecesTo(transformerTo.traverse(personalNameStructure, "ROMN"), destinationTranscription, destination);
			destinationName.addChild(destinationTranscription);

			destinationNode.addChild(destinationName);
		}
	}

	private void personalNamePiecesTo(final GedcomNode personalNameStructure, final GedcomNode destinationNode,
			final Flef destination){
		String givenName = transformerTo.traverse(personalNameStructure, "GIVN")
			.getValue();
		String personalNameSuffix = transformerTo.traverse(personalNameStructure, "NSFX")
			.getValue();
		String surname = transformerTo.traverse(personalNameStructure, "SURN")
			.getValue();
		final String nameValue = personalNameStructure.getValue();
		if(nameValue != null){
			final int surnameBeginIndex = nameValue.indexOf('/');
			final int surnameEndIndex = nameValue.indexOf('/', surnameBeginIndex + 1);
			if(givenName == null && surnameBeginIndex > 0)
				givenName = nameValue.substring(0, surnameBeginIndex - 1);
			if(personalNameSuffix == null && surnameEndIndex >= 0)
				personalNameSuffix = nameValue.substring(surnameEndIndex + 1);
			if(surname == null && surnameBeginIndex >= 0)
				surname = nameValue.substring(surnameBeginIndex + 1, (surnameEndIndex > 0? surnameEndIndex: nameValue.length() - 1));
		}
		final String surnamePrefix = transformerTo.traverse(personalNameStructure, "SPFX")
			.getValue();
		final StringJoiner sj = new StringJoiner(" ");
		if(surnamePrefix != null)
			sj.add(surnamePrefix);
		if(surname != null)
			sj.add(surname);
		destinationNode
			.addChildValue("TYPE", transformerTo.traverse(personalNameStructure, "TYPE")
				.getValue())
			.addChildValue("TITLE", transformerTo.traverse(personalNameStructure, "NPFX")
				.getValue())
			.addChild(transformerTo.create("PERSONAL_NAME")
				.withValue(givenName)
				.addChildValue("NAME_SUFFIX", personalNameSuffix)
			)
			.addChildValue("INDIVIDUAL_NICKNAME", transformerTo.traverse(personalNameStructure, "NICK")
				.getValue())
			.addChildValue("FAMILY_NAME", (sj.length() > 0? sj.toString(): null));
		transformerTo.noteTo(personalNameStructure, destinationNode, destination);
		transformerTo.sourceCitationTo(personalNameStructure, destinationNode, destination);
	}

	private void childToFamilyLinkTo(final GedcomNode individual, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> childToFamilyLinks = individual.getChildrenWithTag("FAMC");
		for(final GedcomNode childToFamilyLink : childToFamilyLinks){
			final GedcomNode pedigree = transformerTo.traverse(childToFamilyLink, "PEDI");
			final GedcomNode destinationFamilyChild = transformerTo.create("FAMILY_CHILD")
				.addChild(transformerTo.create("PEDIGREE")
					.addChildValue("SPOUSE1", pedigree.getValue())
					.addChildValue("SPOUSE2", pedigree.getValue())
				)
				.addChildValue("CERTAINTY", transformerTo.traverse(childToFamilyLink, "STAT")
					.getValue());
			transformerTo.noteTo(childToFamilyLink, destinationFamilyChild, destination);
			destinationNode.addChild(destinationFamilyChild);
		}
	}

	private void spouseToFamilyLinkTo(final GedcomNode individual, final GedcomNode destinationNode, final Flef destination){
		structureTo(individual, destinationNode, destination, "FAMS", "FAMILY_SPOUSE");
	}

	private void associationTo(final GedcomNode individual, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> associations = individual.getChildrenWithTag("ASSO");
		for(final GedcomNode association : associations){
			String type = transformerTo.traverse(association, "TYPE")
				.getValue();
			if("FAM".equals(type))
				type = "FAMILY";
			else if("INDI".equals(type))
				type = "INDIVIDUAL";
			final GedcomNode destinationAssociation = transformerTo.create("ASSOCIATION")
				.withXRef(association.getXRef())
				.addChildValue("TYPE", type)
				.addChildValue("RELATIONSHIP", transformerTo.traverse(association, "RELA")
					.getValue());
			transformerTo.noteTo(association, destinationAssociation, destination);
			transformerTo.sourceCitationTo(association, destinationAssociation, destination);
			destinationNode.addChild(destinationAssociation);
		}
	}

	private void aliasTo(final GedcomNode individual, final GedcomNode destinationNode, final Flef destination){
		structureTo(individual, destinationNode, destination, "ALIA", "ALIAS");
	}

	private void structureTo(final GedcomNode individual, final GedcomNode destinationNode, final Flef destination,
			final String fromTag, final String toTag){
		final List<GedcomNode> nodes = individual.getChildrenWithTag(fromTag);
		for(final GedcomNode node : nodes){
			final GedcomNode newNode = transformerTo.create(toTag);
			transformerTo.noteTo(node, newNode, destination);
			destinationNode.addChild(newNode);
		}
	}

	private void attributeTo(final GedcomNode individual, final GedcomNode destinationNode, final Flef destination,
			final String tagFrom, final String valueTo){
		final List<GedcomNode> attributes = individual.getChildrenWithTag(tagFrom);
		for(final GedcomNode attribute : attributes){
			final GedcomNode destinationAttribute = createAttributeTo(valueTo, attribute, destination);
			destinationNode.addChild(destinationAttribute);
		}
	}

	private GedcomNode createAttributeTo(final String valueTo, final GedcomNode attribute, final Flef destination){
		final GedcomNode destinationAttribute = transformerTo.create("ATTRIBUTE")
			.withValue(valueTo)
			.addChildValue("VALUE", attribute.getValue())
			.addChildValue("TYPE", transformerTo.traverse(attribute, "TYPE")
				.getValue());
		transformerTo.placeAddressStructureTo(attribute, destinationAttribute, destination);
		destinationAttribute.addChildValue("AGENCY", transformerTo.traverse(attribute, "AGNC")
			.getValue())
			.addChildValue("CAUSE", transformerTo.traverse(attribute, "CAUS")
				.getValue())
			.addChildValue("RESTRICTION", transformerTo.traverse(attribute, "RESN")
				.getValue());
		transformerTo.noteTo(attribute, destinationAttribute, destination);
		transformerTo.sourceCitationTo(attribute, destinationAttribute, destination);
		transformerTo.documentTo(attribute, destinationAttribute, destination);
		return destinationAttribute;
	}


	@Override
	public void from(final Flef origin, final Gedcom destination){
		final List<GedcomNode> individuals = origin.getIndividuals();
		for(final GedcomNode individual : individuals)
			individualRecordFrom(individual, origin, destination);
	}

	private void individualRecordFrom(final GedcomNode individual, final Flef origin, final Gedcom destination){
		final GedcomNode destinationIndividual = transformerFrom.create("INDI")
			.withID(individual.getID());
		destinationIndividual.addChildValue("RESN", transformerFrom.traverse(individual, "RESTRICTION")
			.getValue());
		personalNameFrom(individual, destinationIndividual);
		destinationIndividual.addChildValue("SEX", transformerFrom.traverse(individual, "SEX")
			.getValue());
		childToFamilyLinkFrom(individual, destinationIndividual);
		spouseToFamilyLinkFrom(individual, destinationIndividual);
		associationFrom(individual, destinationIndividual);
		aliasFrom(individual, destinationIndividual);
		final List<GedcomNode> events = individual.getChildrenWithTag("EVENT");
		transformerFrom.eventFrom(events, destinationIndividual, origin, "BIRTH", "BIRT");
		transformerFrom.eventFrom(events, destinationIndividual, origin, "ADOPTION", "ADOP");
		transformerFrom.eventFrom(events, destinationIndividual, origin, "DEATH", "DEAT");
		transformerFrom.eventFrom(events, destinationIndividual, origin, "BURIAL", "BURI");
		transformerFrom.eventFrom(events, destinationIndividual, origin, "CREMATION", "CREM");
		transformerFrom.eventFrom(events, destinationIndividual, origin, "NATURALIZATION", "NATU");
		transformerFrom.eventFrom(events, destinationIndividual, origin, "EMIGRATION", "EMIG");
		transformerFrom.eventFrom(events, destinationIndividual, origin, "IMMIGRATION", "IMMI");
		transformerFrom.eventFrom(events, destinationIndividual, origin, "CENSUS", "CENS");
		transformerFrom.eventFrom(events, destinationIndividual, origin, "PROBATE", "PROB");
		transformerFrom.eventFrom(events, destinationIndividual, origin, "WILL", "WILL");
		transformerFrom.eventFrom(events, destinationIndividual, origin, "GRADUATION", "GRAD");
		transformerFrom.eventFrom(events, destinationIndividual, origin, "RETIREMENT", "RETI");
		transformerFrom.eventFrom(events, destinationIndividual, origin, "@EVENT@", "EVEN");
		final List<GedcomNode> attributes = individual.getChildrenWithTag("ATTRIBUTE");
		attributeFrom(attributes, destinationIndividual, origin, "CASTE", "CAST");
		attributeFrom(attributes, destinationIndividual, origin, "CHARACTERISTIC", "DSCR");
		attributeFrom(attributes, destinationIndividual, origin, "EDUCATION", "EDUC");
		attributeFrom(attributes, destinationIndividual, origin, "ORIGIN", "NATI");
		attributeFrom(attributes, destinationIndividual, origin, "NAME", "_NAME");
		attributeFrom(attributes, destinationIndividual, origin, "CHILDREN_COUNT", "NCHI");
		attributeFrom(attributes, destinationIndividual, origin, "MARRIAGES_COUNT", "NMR");
		attributeFrom(attributes, destinationIndividual, origin, "OCCUPATION", "OCCU");
		attributeFrom(attributes, destinationIndividual, origin, "POSSESSION", "PROP");
		attributeFrom(attributes, destinationIndividual, origin, "RELIGION", "RELI");
		attributeFrom(attributes, destinationIndividual, origin, "RESIDENCE", "RESI");
		attributeFrom(attributes, destinationIndividual, origin, "SSN", "SSN");
		attributeFrom(attributes, destinationIndividual, origin, "TITLE", "TITL");
		attributeFrom(attributes, destinationIndividual, origin, "@ATTRIBUTE@", "FACT");
		transformerFrom.noteFrom(individual, destinationIndividual);
		transformerFrom.sourceCitationFrom(individual, destinationIndividual);

		destination.addIndividual(destinationIndividual);
	}

	private void personalNameFrom(final GedcomNode individual, final GedcomNode destinationNode){
		//extract first ATTRIBUTE, or first NOTE, or first SOURCE, or RESTRICTION, or as last element
		GedcomNode lastElement = transformerFrom.traverse(individual, "(ATTRIBUTE|NOTE|SOURCE|RESTRICTION)[0]");
		if(lastElement.isEmpty()){
			final List<GedcomNode> children = individual.getChildren();
			lastElement = children.get(children.size() - 1);
		}

		final List<GedcomNode> personalNameStructures = individual.getChildrenWithTag("NAME");
		for(final GedcomNode personalNameStructure : personalNameStructures){
			final GedcomNode destinationName = transformerFrom.create("NAME");
			personalNamePiecesFrom(personalNameStructure, destinationName);
			//transform nickname into an attribute of individual
			final GedcomNode attributeNickname = transformerFrom.create("ATTRIBUTE")
				.withValue("_FAMILY_NICKNAME")
				.addChildValue("TYPE", "Family Nickname")
				.addChildValue("VALUE", transformerFrom.traverse(personalNameStructure, "FAMILY_NICKNAME")
					.getValue());
			transformerFrom.sourceCitationFrom(personalNameStructure, attributeNickname);
			//add nickname fact to individual (as first fact)
			individual.addChildBefore(attributeNickname, lastElement);
			final GedcomNode temporaryNotes = transformerFrom.create("TMP_NOTE");
			final GedcomNode temporarySourceCitations = transformerFrom.create("TMP_SOURCE_CITATIONS");
			//collect notes and source citations, they will be added as last elements of NAME
			transformerFrom.noteFrom(personalNameStructure, temporaryNotes);
			transformerFrom.sourceCitationFrom(personalNameStructure, temporarySourceCitations);

			final GedcomNode destinationPhonetic = transformerFrom.create("FONE");
			personalNamePiecesFrom(transformerFrom.traverse(personalNameStructure, "PHONETIC"), destinationPhonetic);
			//collect notes and source citations, they will be added as last elements of NAME
			transformerFrom.noteFrom(destinationPhonetic, temporaryNotes);
			transformerFrom.sourceCitationFrom(destinationPhonetic, temporarySourceCitations);
			destinationName.addChild(destinationPhonetic);

			final GedcomNode destinationTranscription = transformerFrom.create("ROMN");
			personalNamePiecesFrom(transformerFrom.traverse(personalNameStructure, "TRANSCRIPTION"), destinationTranscription);
			//collect notes and source citations, they will be added as last elements of NAME
			transformerFrom.noteFrom(destinationTranscription, temporaryNotes);
			transformerFrom.sourceCitationFrom(destinationTranscription, temporarySourceCitations);
			destinationName.addChild(destinationTranscription);

			//add collected notes and source citations as last elements of NAME
			for(final GedcomNode temporaryNote : temporaryNotes.getChildren())
				destinationName.addChild(temporaryNote);
			for(final GedcomNode temporarySourceCitation : temporarySourceCitations.getChildren())
				destinationName.addChild(temporarySourceCitation);

			destinationNode.addChild(destinationName);
		}
	}

	private void personalNamePiecesFrom(final GedcomNode personalNameStructure, final GedcomNode destinationNode){
		final String title = transformerFrom.traverse(personalNameStructure, "TITLE").getValue();
		final GedcomNode personalName = transformerFrom.traverse(personalNameStructure, "PERSONAL_NAME");
		final String givenName = personalName.getValue();
		final String nameSuffix = transformerFrom.traverse(personalName, "NAME_SUFFIX")
			.getValue();
		final String individualNickname = transformerFrom.traverse(personalName, "INDIVIDUAL_NICKNAME")
			.getValue();
		final String familyName = transformerFrom.traverse(personalName, "FAMILY_NAME")
			.getValue();
		final StringJoiner sj = new StringJoiner(" ");
		if(title != null)
			sj.add(title);
		if(givenName != null)
			sj.add(givenName);
		if(nameSuffix != null)
			sj.add(nameSuffix);
		if(familyName != null)
			sj.add("/ " + familyName + " /");
		if(sj.length() > 0)
			destinationNode.withValue(sj.toString());

		destinationNode
			.addChildValue("TYPE", transformerFrom.traverse(personalNameStructure, "TYPE")
				.getValue())
			.addChildValue("NPFX", title)
			.addChildValue("GIVN", givenName)
			.addChildValue("NICK", individualNickname)
			.addChildValue("SURN", familyName)
			.addChildValue("NSFX", nameSuffix);
	}

	private void childToFamilyLinkFrom(final GedcomNode individual, final GedcomNode destinationNode){
		final List<GedcomNode> childToFamilyLinks = individual.getChildrenWithTag("FAMILY_CHILD");
		for(final GedcomNode childToFamilyLink : childToFamilyLinks){
			final GedcomNode pedigree = transformerFrom.traverse(childToFamilyLink, "PEDIGREE");
			final String pedigreeSpouse1 = transformerFrom.traverse(pedigree, "SPOUSE1")
				.getValue();
			final String pedigreeSpouse2 = transformerFrom.traverse(pedigree, "SPOUSE2")
				.getValue();
			@SuppressWarnings("StringEquality")
			final String pedigreeValue = (pedigreeSpouse1 == pedigreeSpouse2 || pedigreeSpouse1.equals(pedigreeSpouse2)?
				pedigreeSpouse1: "SPOUSE1: " + pedigreeSpouse1 + ", SPOUSE2: " + pedigreeSpouse2);
			final GedcomNode destinationFamilyChild = transformerFrom.create("FAMC")
				.addChildValue("PEDI", pedigreeValue)
				.addChildValue("STAT", transformerFrom.traverse(childToFamilyLink, "CERTAINTY")
					.getValue());
			transformerFrom.noteFrom(childToFamilyLink, destinationFamilyChild);
			destinationNode.addChild(destinationFamilyChild);
		}
	}

	private void spouseToFamilyLinkFrom(final GedcomNode individual, final GedcomNode destinationNode){
		structureFrom(individual, destinationNode, "FAMILY_SPOUSE", "FAMS");
	}

	private void associationFrom(final GedcomNode individual, final GedcomNode destinationNode){
		final List<GedcomNode> associations = individual.getChildrenWithTag("ASSOCIATION");
		for(final GedcomNode association : associations){
			String type = transformerFrom.traverse(association, "TYPE")
				.getValue();
			if("FAMILY".equals(type))
				type = "FAM";
			else if("INDIVIDUAL".equals(type))
				type = "INDI";
			final GedcomNode destinationAssociation = transformerFrom.create("ASSO")
				.withXRef(association.getXRef())
				.addChildValue("TYPE", type)
				.addChildValue("RELA", transformerFrom.traverse(association, "RELATIONSHIP")
					.getValue());
			transformerFrom.noteFrom(association, destinationAssociation);
			transformerFrom.sourceCitationFrom(association, destinationAssociation);
			destinationNode.addChild(destinationAssociation);
		}
	}

	private void aliasFrom(final GedcomNode individual, final GedcomNode destinationNode){
		structureFrom(individual, destinationNode, "ALIAS", "ALIA");
	}

	private void structureFrom(final GedcomNode individual, final GedcomNode destinationNode, final String fromTag, final String toTag){
		final List<GedcomNode> nodes = individual.getChildrenWithTag(fromTag);
		for(final GedcomNode node : nodes){
			final GedcomNode newNode = transformerFrom.create(toTag)
				.withXRef(node.getXRef());
			transformerFrom.noteFrom(node, newNode);
			destinationNode.addChild(newNode);
		}
	}

	private void attributeFrom(final Iterable<GedcomNode> attributes, final GedcomNode destinationNode, final Flef origin,
			final String valueFrom, final String tagTo){
		final Iterator<GedcomNode> itr = attributes.iterator();
		while(itr.hasNext()){
			final GedcomNode attribute = itr.next();
			if("@ATTRIBUTE@".equals(valueFrom) || valueFrom.equals(attribute.getValue())){
				final GedcomNode destinationAttribute = createAttributeFrom(tagTo, attribute, origin);
				destinationNode.addChild(destinationAttribute);

				itr.remove();
			}
		}
	}

	private GedcomNode createAttributeFrom(final String tagTo, final GedcomNode attribute, final Flef origin){
		final GedcomNode destinationAttribute = transformerFrom.create(tagTo)
			.withValue(attribute.getValue())
			.addChildValue("TYPE", transformerFrom.traverse(attribute, "TYPE")
				.getValue());
		transformerFrom.placeStructureFrom(attribute, destinationAttribute, origin);
		transformerFrom.addressStructureFrom(attribute, destinationAttribute, origin);
		destinationAttribute.addChildValue("AGENCY", transformerFrom.traverse(attribute, "AGNC")
			.getValue())
			.addChildValue("CAUSE", transformerFrom.traverse(attribute, "CAUS")
				.getValue())
			.addChildValue("RESTRICTION", transformerFrom.traverse(attribute, "RESN")
				.getValue());
		transformerFrom.noteFrom(attribute, destinationAttribute);
		transformerFrom.sourceCitationFrom(attribute, destinationAttribute);
		return destinationAttribute;
	}

}
