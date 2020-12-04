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

import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;


public class IndividualTransformation extends Transformation<Gedcom, Flef>{

	@Override
	public void to(final Gedcom origin, final Flef destination){
		final List<GedcomNode> individuals = origin.getIndividuals();
		for(final GedcomNode individual : individuals)
			individualRecordTo(individual, origin, destination);
	}

	private void individualRecordTo(final GedcomNode individual, final Gedcom origin, final Flef destination){
		final GedcomNode destinationIndividual = transformerTo.create("INDIVIDUAL")
			.withID(individual.getID());
		personalNameTo(individual, destinationIndividual, origin, destination);
		destinationIndividual.addChildValue("SEX", transformerTo.traverse(individual, "SEX")
				.getValue());
		childToFamilyLinkTo(individual, destinationIndividual, destination);
		spouseToFamilyLinkTo(individual, destinationIndividual, destination);
		associationTo(individual, destinationIndividual, origin, destination);
		aliasTo(individual, destinationIndividual, destination);
		transformerTo.eventTo(individual, destinationIndividual, origin, destination, "BIRT", "BIRTH");
		transformerTo.eventTo(individual, destinationIndividual, origin, destination, "ADOP", "ADOPTION");
		transformerTo.eventTo(individual, destinationIndividual, origin, destination, "DEAT", "DEATH");
		transformerTo.eventTo(individual, destinationIndividual, origin, destination, "BURI", "BURIAL");
		transformerTo.eventTo(individual, destinationIndividual, origin, destination, "CREM", "CREMATION");
		transformerTo.eventTo(individual, destinationIndividual, origin, destination, "NATU", "NATURALIZATION");
		transformerTo.eventTo(individual, destinationIndividual, origin, destination, "EMIG", "EMIGRATION");
		transformerTo.eventTo(individual, destinationIndividual, origin, destination, "IMMI", "IMMIGRATION");
		transformerTo.eventTo(individual, destinationIndividual, origin, destination, "CENS", "CENSUS");
		transformerTo.eventTo(individual, destinationIndividual, origin, destination, "PROB", "PROBATE");
		transformerTo.eventTo(individual, destinationIndividual, origin, destination, "WILL", "WILL");
		transformerTo.eventTo(individual, destinationIndividual, origin, destination, "GRAD", "GRADUATION");
		transformerTo.eventTo(individual, destinationIndividual, origin, destination, "RETI", "RETIREMENT");
		transformerTo.eventTo(individual, destinationIndividual, origin, destination, "EVEN", "EVENT");
		attributeTo(individual, destinationIndividual, origin, destination, "CAST", "CASTE");
		attributeTo(individual, destinationIndividual, origin, destination, "DSCR", "CHARACTERISTIC");
		attributeTo(individual, destinationIndividual, origin, destination, "EDUC", "EDUCATION");
		attributeTo(individual, destinationIndividual, origin, destination, "NATI", "ORIGIN");
		attributeTo(individual, destinationIndividual, origin, destination, "NCHI", "CHILDREN_COUNT");
		attributeTo(individual, destinationIndividual, origin, destination, "NMR", "MARRIAGES_COUNT");
		attributeTo(individual, destinationIndividual, origin, destination, "OCCU", "OCCUPATION");
		attributeTo(individual, destinationIndividual, origin, destination, "PROP", "POSSESSION");
		attributeTo(individual, destinationIndividual, origin, destination, "RELI", "RELIGION");
		attributeTo(individual, destinationIndividual, origin, destination, "RESI", "RESIDENCE");
		attributeTo(individual, destinationIndividual, origin, destination, "SSN", "SSN");
		attributeTo(individual, destinationIndividual, origin, destination, "TITL", "TITLE");
		attributeTo(individual, destinationIndividual, origin, destination, "FACT", null);
		transformerTo.noteCitationTo(individual, destinationIndividual, destination);
		transformerTo.sourceCitationTo(individual, destinationIndividual, origin, destination);
		transformerTo.multimediaCitationTo(individual, destinationIndividual, destination);
		destinationIndividual.addChildValue("RESTRICTION", transformerTo.traverse(individual, "RESN")
			.getValue());

		destination.addIndividual(destinationIndividual);
	}

	private void personalNameTo(final GedcomNode individual, final GedcomNode destinationNode, final Gedcom origin, final Flef destination){
		final List<GedcomNode> personalNameStructures = individual.getChildrenWithTag("NAME");
		for(final GedcomNode personalNameStructure : personalNameStructures){
			final GedcomNode destinationName = transformerTo.create("NAME");
			personalNamePiecesTo(personalNameStructure, destinationName, origin, destination);

			final GedcomNode destinationPhonetic = transformerTo.create("PHONETIC");
			personalNamePiecesTo(transformerTo.traverse(personalNameStructure, "FONE"), destinationPhonetic, origin, destination);
			destinationName.addChild(destinationPhonetic);

			final GedcomNode destinationTranscription = transformerTo.create("TRANSCRIPTION");
			personalNamePiecesTo(transformerTo.traverse(personalNameStructure, "ROMN"), destinationTranscription, origin, destination);
			destinationName.addChild(destinationTranscription);

			destinationNode.addChild(destinationName);
		}
	}

	private void personalNamePiecesTo(final GedcomNode personalNameStructure, final GedcomNode destinationNode, final Gedcom origin,
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
		final StringJoiner sj = new StringJoiner(" ");
		final GedcomNode surnamePrefix = transformerTo.traverse(personalNameStructure, "SPFX");
		JavaHelper.addValueIfNotNull(sj, surnamePrefix);
		JavaHelper.addValueIfNotNull(sj, surname);
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
		transformerTo.noteCitationTo(personalNameStructure, destinationNode, destination);
		transformerTo.sourceCitationTo(personalNameStructure, destinationNode, origin, destination);
	}

	private void childToFamilyLinkTo(final GedcomNode individual, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> childToFamilyLinks = individual.getChildrenWithTag("FAMC");
		for(final GedcomNode childToFamilyLink : childToFamilyLinks){
			final GedcomNode pedigree = transformerTo.traverse(childToFamilyLink, "PEDI");
			final GedcomNode destinationFamilyChild = transformerTo.create("FAMILY_CHILD")
				.withXRef(childToFamilyLink.getXRef())
				.addChild(transformerTo.create("PEDIGREE")
					.addChildValue("PARENT1", pedigree.getValue())
					.addChildValue("PARENT2", pedigree.getValue())
				)
				.addChildValue("CERTAINTY", transformerTo.traverse(childToFamilyLink, "STAT")
					.getValue());
			transformerTo.noteCitationTo(childToFamilyLink, destinationFamilyChild, destination);
			destinationNode.addChild(destinationFamilyChild);
		}
	}

	private void spouseToFamilyLinkTo(final GedcomNode individual, final GedcomNode destinationNode, final Flef destination){
		structureTo(individual, destinationNode, destination, "FAMS", "FAMILY_SPOUSE");
	}

	private void associationTo(final GedcomNode individual, final GedcomNode destinationNode, final Gedcom origin, final Flef destination){
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
			transformerTo.noteCitationTo(association, destinationAssociation, destination);
			transformerTo.sourceCitationTo(association, destinationAssociation, origin, destination);
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
			final GedcomNode newNode = transformerTo.create(toTag)
				.withXRef(node.getXRef());
			transformerTo.noteCitationTo(node, newNode, destination);
			destinationNode.addChild(newNode);
		}
	}

	private void attributeTo(final GedcomNode individual, final GedcomNode destinationNode, final Gedcom origin, final Flef destination,
			final String tagFrom, final String valueTo){
		final List<GedcomNode> attributes = individual.getChildrenWithTag(tagFrom);
		for(final GedcomNode attribute : attributes){
			final GedcomNode destinationAttribute = createAttributeTo(valueTo, attribute, origin, destination);
			destinationNode.addChild(destinationAttribute);
		}
	}

	private GedcomNode createAttributeTo(final String valueTo, final GedcomNode attribute, final Gedcom origin, final Flef destination){
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
		transformerTo.noteCitationTo(attribute, destinationAttribute, destination);
		transformerTo.sourceCitationTo(attribute, destinationAttribute, origin, destination);
		transformerTo.multimediaCitationTo(attribute, destinationAttribute, destination);
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
		personalNameFrom(individual, destinationIndividual, origin);
		destinationIndividual.addChildValue("SEX", transformerFrom.traverse(individual, "SEX")
			.getValue());
		childToFamilyLinkFrom(individual, destinationIndividual);
		spouseToFamilyLinkFrom(individual, destinationIndividual);
		associationFrom(individual, destinationIndividual, origin);
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
		transformerFrom.noteCitationFrom(individual, destinationIndividual);
		transformerFrom.sourceCitationFrom(individual, destinationIndividual, origin);

		destination.addIndividual(destinationIndividual);
	}

	private void personalNameFrom(final GedcomNode individual, final GedcomNode destinationNode, final Flef origin){
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
			transformerFrom.sourceCitationFrom(personalNameStructure, attributeNickname, origin);
			//add nickname fact to individual (as first fact)
			individual.addChildBefore(attributeNickname, lastElement);
			final GedcomNode temporaryNotes = transformerFrom.create("TMP_NOTE");
			final GedcomNode temporarySourceCitations = transformerFrom.create("TMP_SOURCE_CITATIONS");
			//collect notes and source citations, they will be added as last elements of NAME
			transformerFrom.noteCitationFrom(personalNameStructure, temporaryNotes);
			transformerFrom.sourceCitationFrom(personalNameStructure, temporarySourceCitations, origin);

			final GedcomNode destinationPhonetic = transformerFrom.create("FONE");
			personalNamePiecesFrom(transformerFrom.traverse(personalNameStructure, "PHONETIC"), destinationPhonetic);
			//collect notes and source citations, they will be added as last elements of NAME
			transformerFrom.noteCitationFrom(destinationPhonetic, temporaryNotes);
			transformerFrom.sourceCitationFrom(destinationPhonetic, temporarySourceCitations, origin);
			destinationName.addChild(destinationPhonetic);

			final GedcomNode destinationTranscription = transformerFrom.create("ROMN");
			personalNamePiecesFrom(transformerFrom.traverse(personalNameStructure, "TRANSCRIPTION"), destinationTranscription);
			//collect notes and source citations, they will be added as last elements of NAME
			transformerFrom.noteCitationFrom(destinationTranscription, temporaryNotes);
			transformerFrom.sourceCitationFrom(destinationTranscription, temporarySourceCitations, origin);
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
		JavaHelper.addValueIfNotNull(sj, title);
		JavaHelper.addValueIfNotNull(sj, givenName);
		JavaHelper.addValueIfNotNull(sj, nameSuffix);
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
			final String pedigreeParent1 = transformerFrom.traverse(pedigree, "PARENT1")
				.getValue();
			final String pedigreeParent2 = transformerFrom.traverse(pedigree, "PARENT2")
				.getValue();
			@SuppressWarnings("StringEquality")
			final String pedigreeValue = (pedigreeParent1 == pedigreeParent2 || pedigreeParent1.equals(pedigreeParent2)?
				pedigreeParent1: "PARENT1: " + pedigreeParent1 + ", PARENT2: " + pedigreeParent2);
			final GedcomNode destinationFamilyChild = transformerFrom.create("FAMC")
				.addChildValue("PEDI", pedigreeValue)
				.addChildValue("STAT", transformerFrom.traverse(childToFamilyLink, "CERTAINTY")
					.getValue());
			transformerFrom.noteCitationFrom(childToFamilyLink, destinationFamilyChild);
			destinationNode.addChild(destinationFamilyChild);
		}
	}

	private void spouseToFamilyLinkFrom(final GedcomNode individual, final GedcomNode destinationNode){
		structureFrom(individual, destinationNode, "FAMILY_SPOUSE", "FAMS");
	}

	private void associationFrom(final GedcomNode individual, final GedcomNode destinationNode, final Flef origin){
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
			transformerFrom.noteCitationFrom(association, destinationAssociation);
			transformerFrom.sourceCitationFrom(association, destinationAssociation, origin);
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
			transformerFrom.noteCitationFrom(node, newNode);
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
		transformerFrom.noteCitationFrom(attribute, destinationAttribute);
		transformerFrom.sourceCitationFrom(attribute, destinationAttribute, origin);
		return destinationAttribute;
	}

}
