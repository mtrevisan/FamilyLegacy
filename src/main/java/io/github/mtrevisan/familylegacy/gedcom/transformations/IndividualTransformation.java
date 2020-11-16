package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.addressStructureFrom;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.createEventTo;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.documentTo;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.eventFrom;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.noteTo;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.notesFrom;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.placeAddressStructureTo;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.placeStructureFrom;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.sourceCitationFrom;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.sourceCitationTo;


public class IndividualTransformation implements Transformation<Gedcom, Flef>{


	@Override
	public void to(final Gedcom origin, final Flef destination){
		final List<GedcomNode> individuals = origin.getIndividuals();
		for(final GedcomNode individual : individuals)
			individualRecordTo(individual, destination);
	}

	private void individualRecordTo(final GedcomNode individual, final Flef destination){
		final GedcomNode destinationIndividual = GedcomNode.create("INDIVIDUAL")
			.withID(individual.getID());
		personalNameTo(individual, destinationIndividual, destination);
		destinationIndividual.addChildValue("SEX", extractSubStructure(individual, "SEX")
				.getValue());
		childToFamilyLinkTo(individual, destinationIndividual, destination);
		spouseToFamilyLinkTo(individual, destinationIndividual, destination);
		associationTo(individual, destinationIndividual, destination);
		aliasTo(individual, destinationIndividual, destination);
		eventTo(individual, destinationIndividual, destination, "BIRT", "BIRTH");
		eventTo(individual, destinationIndividual, destination, "ADOP", "ADOPTION");
		eventTo(individual, destinationIndividual, destination, "DEAT", "DEATH");
		eventTo(individual, destinationIndividual, destination, "BURI", "BURIAL");
		eventTo(individual, destinationIndividual, destination, "CREM", "CREMATION");
		eventTo(individual, destinationIndividual, destination, "NATU", "NATURALIZATION");
		eventTo(individual, destinationIndividual, destination, "EMIG", "EMIGRATION");
		eventTo(individual, destinationIndividual, destination, "IMMI", "IMMIGRATION");
		eventTo(individual, destinationIndividual, destination, "CENS", "CENSUS");
		eventTo(individual, destinationIndividual, destination, "PROB", "PROBATE");
		eventTo(individual, destinationIndividual, destination, "WILL", "WILL");
		eventTo(individual, destinationIndividual, destination, "GRAD", "GRADUATION");
		eventTo(individual, destinationIndividual, destination, "RETI", "RETIREMENT");
		eventTo(individual, destinationIndividual, destination, "EVEN", "EVENT");
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
		noteTo(individual, destinationIndividual, destination);
		sourceCitationTo(individual, destinationIndividual, destination);
		documentTo(individual, destinationIndividual, destination);
		destinationIndividual.addChildValue("RESTRICTION", extractSubStructure(individual, "RESN")
			.getValue());

		destination.addIndividual(destinationIndividual);
	}

	private void personalNameTo(final GedcomNode individual, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> personalNameStructures = individual.getChildrenWithTag("NAME");
		for(final GedcomNode personalNameStructure : personalNameStructures){
			final GedcomNode destinationName = GedcomNode.create("NAME");
			personalNamePiecesTo(personalNameStructure, destinationName, destination);

			final GedcomNode destinationPhonetic = GedcomNode.create("PHONETIC");
			personalNamePiecesTo(extractSubStructure(personalNameStructure, "FONE"), destinationPhonetic, destination);
			destinationName.addChild(destinationPhonetic);

			final GedcomNode destinationTranscription = GedcomNode.create("TRANSCRIPTION");
			personalNamePiecesTo(extractSubStructure(personalNameStructure, "ROMN"), destinationTranscription, destination);
			destinationName.addChild(destinationTranscription);

			destinationNode.addChild(destinationName);
		}
	}

	private void personalNamePiecesTo(final GedcomNode personalNameStructure, final GedcomNode destinationNode,
			final Flef destination){
		final String surname = extractSubStructure(personalNameStructure, "SURN")
			.getValue();
		final String surnamePrefix = extractSubStructure(personalNameStructure, "SPFX")
			.getValue();
		final StringJoiner sj = new StringJoiner(" ");
		if(surnamePrefix != null)
			sj.add(surnamePrefix);
		if(surname != null)
			sj.add(surname);
		final String nameValue = personalNameStructure.getValue();
		String givenName = extractSubStructure(personalNameStructure, "GIVN")
			.getValue();
		final int surnameBeginIndex = (nameValue != null? nameValue.indexOf('/'): -1);
		int surnameEndIndex = -1;
		if(nameValue != null && surnameBeginIndex >= 0){
			surnameEndIndex = nameValue.indexOf('/', surnameBeginIndex + 1);
			if(givenName == null)
				//extract given name component
				givenName = nameValue.substring(0, surnameBeginIndex - 1);
			//extract surname component
			sj.add(nameValue.substring(surnameBeginIndex + 1,
				(surnameEndIndex > 0? surnameEndIndex: nameValue.length() - 1)));
		}
		String personalNameSuffix = extractSubStructure(personalNameStructure, "NSFX").getValue();
		if(personalNameSuffix == null && nameValue != null)
			personalNameSuffix = nameValue.substring(surnameEndIndex + 1);
		destinationNode
			.addChildValue("TYPE", extractSubStructure(personalNameStructure, "TYPE")
				.getValue())
			.addChildValue("TITLE", extractSubStructure(personalNameStructure, "NPFX")
				.getValue())
			.addChild(GedcomNode.create("PERSONAL_NAME")
				.withValue(givenName)
				.addChildValue("NAME_SUFFIX", personalNameSuffix)
			)
			.addChildValue("INDIVIDUAL_NICKNAME", extractSubStructure(personalNameStructure, "NICK")
				.getValue())
			.addChildValue("FAMILY_NAME", (sj.length() > 0? sj.toString(): null));
		noteTo(personalNameStructure, destinationNode, destination);
		sourceCitationTo(personalNameStructure, destinationNode, destination);
	}

	private void childToFamilyLinkTo(final GedcomNode individual, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> childToFamilyLinks = individual.getChildrenWithTag("FAMC");
		for(final GedcomNode childToFamilyLink : childToFamilyLinks){
			final GedcomNode pedigree = extractSubStructure(childToFamilyLink, "PEDI");
			final GedcomNode destinationFamilyChild = GedcomNode.create("FAMILY_CHILD")
				.addChild(GedcomNode.create("PEDIGREE")
					.addChildValue("SPOUSE1", pedigree.getValue())
					.addChildValue("SPOUSE2", pedigree.getValue())
				)
				.addChildValue("CERTAINTY", extractSubStructure(childToFamilyLink, "STAT")
					.getValue());
			noteTo(childToFamilyLink, destinationFamilyChild, destination);
			destinationNode.addChild(destinationFamilyChild);
		}
	}

	private void spouseToFamilyLinkTo(final GedcomNode individual, final GedcomNode destinationNode, final Flef destination){
		structureTo(individual, destinationNode, destination, "FAMS", "FAMILY_SPOUSE");
	}

	private void associationTo(final GedcomNode individual, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> associations = individual.getChildrenWithTag("ASSO");
		for(final GedcomNode association : associations){
			final GedcomNode destinationAssociation = GedcomNode.create("ASSOCIATION")
				.withID(association.getID())
				.addChildValue("TYPE", extractSubStructure(association, "TYPE")
					.getValue())
				.addChildValue("RELATIONSHIP", extractSubStructure(association, "RELA")
					.getValue());
			noteTo(association, destinationAssociation, destination);
			sourceCitationTo(association, destinationAssociation, destination);
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
			final GedcomNode newNode = GedcomNode.create(toTag);
			noteTo(node, newNode, destination);
			destinationNode.addChild(newNode);
		}
	}

	private void eventTo(final GedcomNode individual, final GedcomNode destinationNode, final Flef destination,
			final String tagFrom, final String valueTo){
		final List<GedcomNode> events = individual.getChildrenWithTag(tagFrom);
		for(final GedcomNode event : events){
			final GedcomNode destinationEvent = createEventTo(valueTo, event, destination);
			destinationNode.addChild(destinationEvent);
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
		final GedcomNode destinationAttribute = GedcomNode.create("ATTRIBUTE")
			.withValue(valueTo)
			.addChildValue("VALUE", attribute.getValue())
			.addChildValue("TYPE", extractSubStructure(attribute, "TYPE")
				.getValue());
		placeAddressStructureTo(attribute, destinationAttribute, destination);
		destinationAttribute.addChildValue("AGENCY", extractSubStructure(attribute, "AGNC")
			.getValue())
			.addChildValue("CAUSE", extractSubStructure(attribute, "CAUS")
				.getValue())
			.addChildValue("RESTRICTION", extractSubStructure(attribute, "RESN")
				.getValue());
		noteTo(attribute, destinationAttribute, destination);
		sourceCitationTo(attribute, destinationAttribute, destination);
		documentTo(attribute, destinationAttribute, destination);
		return destinationAttribute;
	}


	@Override
	public void from(final Flef origin, final Gedcom destination) throws GedcomGrammarParseException{
		final List<GedcomNode> individuals = origin.getIndividuals();
		for(final GedcomNode individual : individuals)
			individualRecordFrom(individual, origin, destination);
	}

	private void individualRecordFrom(final GedcomNode individual, final Flef origin, final Gedcom destination)
			throws GedcomGrammarParseException{
		final GedcomNode destinationIndividual = GedcomNode.create("INDI")
			.withID(individual.getID());
		destinationIndividual.addChildValue("RESN", extractSubStructure(individual, "RESTRICTION")
			.getValue());
		personalNameFrom(individual, destinationIndividual);
		destinationIndividual.addChildValue("SEX", extractSubStructure(individual, "SEX")
			.getValue());
		childToFamilyLinkFrom(individual, destinationIndividual);
		spouseToFamilyLinkFrom(individual, destinationIndividual);
		associationFrom(individual, destinationIndividual);
		aliasFrom(individual, destinationIndividual);
		final List<GedcomNode> events = individual.getChildrenWithTag("EVENT");
		eventFrom(events, destinationIndividual, origin, "BIRTH", "BIRT");
		eventFrom(events, destinationIndividual, origin, "ADOPTION", "ADOP");
		eventFrom(events, destinationIndividual, origin, "DEATH", "DEAT");
		eventFrom(events, destinationIndividual, origin, "BURIAL", "BURI");
		eventFrom(events, destinationIndividual, origin, "CREMATION", "CREM");
		eventFrom(events, destinationIndividual, origin, "NATURALIZATION", "NATU");
		eventFrom(events, destinationIndividual, origin, "EMIGRATION", "EMIG");
		eventFrom(events, destinationIndividual, origin, "IMMIGRATION", "IMMI");
		eventFrom(events, destinationIndividual, origin, "CENSUS", "CENS");
		eventFrom(events, destinationIndividual, origin, "PROBATE", "PROB");
		eventFrom(events, destinationIndividual, origin, "WILL", "WILL");
		eventFrom(events, destinationIndividual, origin, "GRADUATION", "GRAD");
		eventFrom(events, destinationIndividual, origin, "RETIREMENT", "RETI");
		eventFrom(events, destinationIndividual, origin, "@EVENT@", "EVEN");
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
		notesFrom(individual, destinationIndividual);
		sourceCitationFrom(individual, destinationIndividual);

		destination.addIndividual(destinationIndividual);
	}

	private void personalNameFrom(final GedcomNode individual, final GedcomNode destinationNode){
		//extract first ATTRIBUTE, or first NOTE, or first SOURCE, or RESTRICTION, or as last element
		final GedcomNode lastElement = individual.getFirstChildWithTag("ATTRIBUTE", "NOTE", "SOURCE", "RESTRICTION");

		final List<GedcomNode> personalNameStructures = individual.getChildrenWithTag("NAME");
		for(final GedcomNode personalNameStructure : personalNameStructures){
			final GedcomNode destinationName = GedcomNode.create("NAME");
			personalNamePiecesFrom(personalNameStructure, destinationName);
			//transform nickname into an attribute of individual
			final GedcomNode attributeNickname = GedcomNode.create("ATTRIBUTE")
				.withValue("_FAMILY_NICKNAME")
				.addChildValue("TYPE", "Family Nickname")
				.addChildValue("VALUE", extractSubStructure(personalNameStructure, "FAMILY_NICKNAME")
					.getValue());
			sourceCitationFrom(personalNameStructure, attributeNickname);
			//add nickname fact to individual (as first fact)
			individual.addChildBefore(attributeNickname, lastElement);
			final GedcomNode temporaryNotes = GedcomNode.create("TMP_NOTE");
			final GedcomNode temporarySourceCitations = GedcomNode.create("TMP_SOURCE_CITATIONS");
			//collect notes and source citations, they will be added as last elements of NAME
			notesFrom(personalNameStructure, temporaryNotes);
			sourceCitationFrom(personalNameStructure, temporarySourceCitations);

			final GedcomNode destinationPhonetic = GedcomNode.create("FONE");
			personalNamePiecesFrom(extractSubStructure(personalNameStructure, "PHONETIC"), destinationPhonetic);
			//collect notes and source citations, they will be added as last elements of NAME
			notesFrom(destinationPhonetic, temporaryNotes);
			sourceCitationFrom(destinationPhonetic, temporarySourceCitations);
			destinationName.addChild(destinationPhonetic);

			final GedcomNode destinationTranscription = GedcomNode.create("ROMN");
			personalNamePiecesFrom(extractSubStructure(personalNameStructure, "TRANSCRIPTION"), destinationTranscription);
			//collect notes and source citations, they will be added as last elements of NAME
			notesFrom(destinationTranscription, temporaryNotes);
			sourceCitationFrom(destinationTranscription, temporarySourceCitations);
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
		final String title = extractSubStructure(personalNameStructure, "TITLE").getValue();
		final GedcomNode personalName = extractSubStructure(personalNameStructure, "PERSONAL_NAME");
		final String givenName = personalName.getValue();
		final String nameSuffix = extractSubStructure(personalName, "NAME_SUFFIX")
			.getValue();
		final String individualNickname = extractSubStructure(personalName, "INDIVIDUAL_NICKNAME")
			.getValue();
		final String familyName = extractSubStructure(personalName, "FAMILY_NAME")
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
			destinationNode
				.withValue(sj.toString());

		destinationNode
			.addChildValue("TYPE", extractSubStructure(personalNameStructure, "TYPE")
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
			final GedcomNode pedigree = extractSubStructure(childToFamilyLink, "PEDIGREE");
			final String pedigreeSpouse1 = extractSubStructure(pedigree, "SPOUSE1")
				.getValue();
			final String pedigreeSpouse2 = extractSubStructure(pedigree, "SPOUSE2")
				.getValue();
			@SuppressWarnings({"StringEquality", "ConstantConditions"})
			final String pedigreeValue = (pedigreeSpouse1 == pedigreeSpouse2 || pedigreeSpouse1.equals(pedigreeSpouse2)?
				pedigreeSpouse1: "SPOUSE1: " + pedigreeSpouse1 + ", SPOUSE2: " + pedigreeSpouse2);
			final GedcomNode destinationFamilyChild = GedcomNode.create("FAMC")
				.addChildValue("PEDI", pedigreeValue)
				.addChildValue("STAT", extractSubStructure(childToFamilyLink, "CERTAINTY")
					.getValue());
			notesFrom(childToFamilyLink, destinationFamilyChild);
			destinationNode.addChild(destinationFamilyChild);
		}
	}

	private void spouseToFamilyLinkFrom(final GedcomNode individual, final GedcomNode destinationNode){
		structureFrom(individual, destinationNode, "FAMILY_SPOUSE", "FAMS");
	}

	private void associationFrom(final GedcomNode individual, final GedcomNode destinationNode){
		final List<GedcomNode> associations = individual.getChildrenWithTag("ASSOCIATION");
		for(final GedcomNode association : associations){
			final GedcomNode destinationAssociation = GedcomNode.create("ASSO")
				.withID(association.getID())
				.addChildValue("TYPE", extractSubStructure(association, "TYPE")
					.getValue())
				.addChildValue("RELA", extractSubStructure(association, "RELATIONSHIP")
					.getValue());
			notesFrom(association, destinationAssociation);
			sourceCitationFrom(association, destinationAssociation);
			destinationNode.addChild(destinationAssociation);
		}
	}

	private void aliasFrom(final GedcomNode individual, final GedcomNode destinationNode){
		structureFrom(individual, destinationNode, "ALIAS", "ALIA");
	}

	private void structureFrom(final GedcomNode individual, final GedcomNode destinationNode, final String fromTag, final String toTag){
		final List<GedcomNode> nodes = individual.getChildrenWithTag(fromTag);
		for(final GedcomNode node : nodes){
			final GedcomNode newNode = GedcomNode.create(toTag)
				.withID(node.getID());
			notesFrom(node, newNode);
			destinationNode.addChild(newNode);
		}
	}

	private void attributeFrom(final Iterable<GedcomNode> attributes, final GedcomNode destinationNode, final Flef origin,
			final String valueFrom, final String tagTo) throws GedcomGrammarParseException{
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

	private GedcomNode createAttributeFrom(final String tagTo, final GedcomNode attribute, final Flef origin)
			throws GedcomGrammarParseException{
		final GedcomNode destinationAttribute = GedcomNode.create(tagTo)
			.withValue(attribute.getValue())
			.addChildValue("TYPE", extractSubStructure(attribute, "TYPE")
				.getValue());
		placeStructureFrom(attribute, destinationAttribute, origin);
		addressStructureFrom(attribute, destinationAttribute, origin);
		destinationAttribute.addChildValue("AGENCY", extractSubStructure(attribute, "AGNC")
			.getValue())
			.addChildValue("CAUSE", extractSubStructure(attribute, "CAUS")
				.getValue())
			.addChildValue("RESTRICTION", extractSubStructure(attribute, "RESN")
				.getValue());
		notesFrom(attribute, destinationAttribute);
		sourceCitationFrom(attribute, destinationAttribute);
		return destinationAttribute;
	}

}
