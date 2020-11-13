package io.github.mtrevisan.familylegacy.gedcom.transformations2;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.StringJoiner;

import static io.github.mtrevisan.familylegacy.gedcom.transformations2.TransformationHelper.extractSubStructure;


public class IndividualTransformation implements Transformation<Gedcom, Flef>{

	private static final Collection<String> ADDRESS_TAGS = new HashSet<>(Arrays.asList("CONT", "ADR1", "ADR2", "ADR3"));


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
		attributeTo(individual, destinationIndividual, destination, "DSCR", "PHYSICAL_DESCRIPTION");
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
		attributeTo(individual, destinationIndividual, destination, "FACT");
		notesTo(individual, destinationIndividual, destination);
		sourceCitationTo(individual, destinationIndividual, destination);
		documentsTo(individual, destinationIndividual, destination);
		destinationIndividual.addChildValue("RESTRICTION", extractSubStructure(individual, "RESN")
			.getValue());
		destination.addIndividual(destinationIndividual);
	}

	private void personalNameTo(final GedcomNode individual, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> personalNameStructures = individual.getChildrenWithTag("NAME");
		for(final GedcomNode personalNameStructure : personalNameStructures){
			final GedcomNode destinationName = GedcomNode.create("NAME");
			personalNamePiecesTo(personalNameStructure, destinationName, destination);
			final GedcomNode destinationPhoneticName = GedcomNode.create("PHONETIC");
			personalNamePiecesTo(extractSubStructure(personalNameStructure, "FONE"), destinationPhoneticName, destination);
			destinationName.addChild(destinationPhoneticName);
			final GedcomNode destinationRomanizedName = GedcomNode.create("TRANSCRIPTION");
			personalNamePiecesTo(extractSubStructure(personalNameStructure, "ROMN"), destinationRomanizedName, destination);
			destinationName.addChild(destinationRomanizedName);
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
		if(givenName == null)
			//extract given name component
			givenName = (nameValue != null && StringUtils.contains(nameValue, '/')?
				nameValue.substring(0, nameValue.indexOf('/') - 1): nameValue);
		if(sj.length() == 0){
			//extract surnname component
			final String surnameComponent = (nameValue != null && StringUtils.contains(nameValue, '/')?
				nameValue.substring(nameValue.indexOf('/') + 1, nameValue.length() - 1): null);
			sj.add(surnameComponent);
		}
		destinationNode
			.addChildValue("TYPE", extractSubStructure(personalNameStructure, "TYPE")
				.getValue())
			.addChildValue("TITLE", extractSubStructure(personalNameStructure, "NPFX")
				.getValue())
			.addChild(GedcomNode.create("PERSONAL_NAME")
				.withValue(givenName)
				.addChildValue("NAME_SUFFIX", extractSubStructure(personalNameStructure, "NSFX")
					.getValue())
			)
			.addChildValue("INDIVIDUAL_NICKNAME", extractSubStructure(personalNameStructure, "NICK")
				.getValue())
			.addChildValue("FAMILY_NAME", sj.toString());
		notesTo(personalNameStructure, destinationNode, destination);
		sourceCitationTo(personalNameStructure, destinationNode, destination);
	}

	private void childToFamilyLinkTo(final GedcomNode individual, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> childToFamilyLinks = individual.getChildrenWithTag("FAMC");
		for(final GedcomNode childToFamilyLink : childToFamilyLinks){
			final GedcomNode destinationFamilyChild = GedcomNode.create("FAMILY_CHILD")
				.addChildValue("PEDIGREE", extractSubStructure(childToFamilyLink, "PEDI")
					.getValue())
				.addChildValue("CERTAINTY", extractSubStructure(childToFamilyLink, "STAT")
					.getValue());
			notesTo(childToFamilyLink, destinationFamilyChild, destination);
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
			notesTo(association, destinationAssociation, destination);
			sourceCitationTo(association, destinationAssociation, destination);
			destinationNode.addChild(destinationAssociation);
		}
	}

	private void sourceCitationTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> sourceCitations = parent.getChildrenWithTag("SOUR");
		for(final GedcomNode sourceCitation : sourceCitations){
			String sourceCitationID = sourceCitation.getID();
			if(sourceCitationID == null){
				sourceCitationID = destination.getNextSourceID();

				//create source:
				final GedcomNode note = GedcomNode.create("NOTE")
					.withID(destination.getNextNoteID())
					.withValue(sourceCitation.getValue());
				destination.addNote(note);
				final GedcomNode destinationSource = GedcomNode.create("SOURCE")
					.withID(sourceCitationID)
					.addChildValue("TEXT", extractSubStructure(sourceCitation, "TEXT")
						.getValue())
					.addChildReference("NOTE", note.getID());
				documentsTo(sourceCitation, destinationSource, destination);
				notesTo(sourceCitation, destinationSource, destination);
				destination.addSource(destinationSource);

				//add source citation
				destinationNode.addChild(GedcomNode.create("SOURCE")
					.withID(sourceCitationID)
					.addChildValue("CREDIBILITY", extractSubStructure(sourceCitation, "QUAY")
						.getValue()));
			}
			else{
				//create source:
				final GedcomNode note = GedcomNode.create("NOTE")
					.withID(destination.getNextNoteID())
					.withValue(sourceCitation.getValue());
				destination.addNote(note);
				final GedcomNode eventNode = extractSubStructure(sourceCitation, "EVEN");
				final GedcomNode data = extractSubStructure(sourceCitation, "DATA");
				final GedcomNode destinationSource = GedcomNode.create("SOURCE")
					.withID(sourceCitationID)
					.addChildValue("EVENT", eventNode.getValue())
					.addChildValue("DATE", extractSubStructure(data, "DATE")
						.getValue());
				final List<GedcomNode> texts = data.getChildrenWithTag( "TEXT");
				for(final GedcomNode text : texts)
					destinationSource.addChildValue("TEXT", text
						.getValue());
				destinationSource.addChildReference("NOTE", note.getID());
				documentsTo(sourceCitation, destinationSource, destination);
				notesTo(sourceCitation, destinationSource, destination);
				destination.addSource(destinationSource);

				//add source citation
				destinationNode.addChild(GedcomNode.create("SOURCE")
					.withID(sourceCitationID)
					.addChildValue("PAGE", extractSubStructure(sourceCitation, "PAGE")
						.getValue())
					.addChildValue("ROLE", extractSubStructure(eventNode, "ROLE")
						.getValue())
					.addChildValue("CREDIBILITY", extractSubStructure(sourceCitation, "QUAY")
						.getValue()));
			}
		}
	}

	private void aliasTo(final GedcomNode individual, final GedcomNode destinationNode, final Flef destination){
		structureTo(individual, destinationNode, destination, "ALIA", "ALIAS");
	}

	private void structureTo(final GedcomNode individual, final GedcomNode destinationNode, final Flef destination,
			final String fromTag, final String toTag){
		final List<GedcomNode> aliases = individual.getChildrenWithTag(fromTag);
		for(final GedcomNode alias : aliases){
			final GedcomNode destinationAlias = GedcomNode.create(toTag);
			notesTo(alias, destinationAlias, destination);
			destinationNode.addChild(destinationAlias);
		}
	}

	private void notesTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> notes = parent.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes){
			String noteID = note.getID();
			if(noteID == null){
				noteID = destination.getNextNoteID();

				destination.addNote(GedcomNode.create("NOTE", noteID, note.getValue()));
			}
			destinationNode.addChildReference("NOTE", noteID);
		}
	}

	private void documentsTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> documents = parent.getChildrenWithTag("OBJE");
		for(final GedcomNode document : documents){
			String documentID = document.getID();
			if(documentID == null){
				documentID = destination.getNextSourceID();

				final GedcomNode destinationDocument = GedcomNode.create("SOURCE");
				final String documentTitle = extractSubStructure(document, "TITL")
					.getValue();
				final String documentFormat = extractSubStructure(document, "FORM")
					.getValue();
				final String documentMedia = extractSubStructure(document, "FORM", "MEDI")
					.getValue();
				final String documentFile = extractSubStructure(document, "FILE")
					.getValue();
				final String documentCut = extractSubStructure(document, "_CUTD")
					.getValue();

				destinationDocument.addChildValue("TITLE", documentTitle);
				if(documentFormat != null || documentMedia != null)
					destinationDocument.addChild(GedcomNode.create("FILE")
						.withValue(documentFile)
						.addChildValue("FORMAT", documentFormat)
						.addChildValue("MEDIA", documentMedia)
						.addChildValue("CUT", documentCut)
					);
				destination.addSource(destinationDocument
					.withID(documentID));
			}
			destinationNode.addChildReference("SOURCE", documentID);
		}
	}

	private void eventTo(final GedcomNode individual, final GedcomNode destinationNode, final Flef destination,
		final String tagFrom, final String tagTo){
		final List<GedcomNode> events = individual.getChildrenWithTag(tagFrom);
		for(final GedcomNode event : events){
			final GedcomNode destinationEvent = GedcomNode.create("EVENT")
				.withValue(tagTo)
				.addChildValue("TYPE", extractSubStructure(event, "TYPE")
					.getValue())
				.addChildValue("DATE", extractSubStructure(event, "DATE")
					.getValue());
			placeAddressStructureTo(event, destinationEvent, destination);
			final GedcomNode familyChild = extractSubStructure(event, "FAMC");
			destinationEvent.addChildValue("AGENCY", extractSubStructure(event, "AGNC")
				.getValue())
				.addChildValue("CAUSE", extractSubStructure(event, "CAUS")
					.getValue());
			notesTo(event, destinationEvent, destination);
			sourceCitationTo(event, destinationEvent, destination);
			documentsTo(event, destinationEvent, destination);
			destinationEvent.addChildValue("RESTRICTION", extractSubStructure(event, "RESN")
					.getValue())
				.addChild(GedcomNode.create("FAMILY_CHILD")
					.withID(familyChild.getID())
					.addChildValue("ADOPTED_BY", extractSubStructure(familyChild, "ADOP")
						.getValue())
				);
			destinationNode.addChild(destinationEvent);
		}
	}

	private void attributeTo(final GedcomNode individual, final GedcomNode destinationNode, final Flef destination,
			final String tagFrom, final String tagTo){
		final List<GedcomNode> attributes = individual.getChildrenWithTag(tagFrom);
		for(final GedcomNode attribute : attributes){
			final GedcomNode destinationAttribute = GedcomNode.create("ATTRIBUTE")
				.withValue(tagTo)
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
			notesTo(attribute, destinationAttribute, destination);
			sourceCitationTo(attribute, destinationAttribute, destination);
			documentsTo(attribute, destinationAttribute, destination);
			destinationNode.addChild(destinationAttribute);
		}
	}

	private void attributeTo(final GedcomNode individual, final GedcomNode destinationNode, final Flef destination,
			final String tagFrom){
		final List<GedcomNode> attributes = individual.getChildrenWithTag(tagFrom);
		for(final GedcomNode attribute : attributes){
			final GedcomNode destinationAttribute = GedcomNode.create("ATTRIBUTE")
				.withValue(attribute.getValue())
				.addChildValue("TYPE", extractSubStructure(attribute, "TYPE")
					.getValue());
			placeAddressStructureTo(attribute, destinationAttribute, destination);
			destinationAttribute.addChildValue("AGENCY", extractSubStructure(attribute, "AGNC")
				.getValue())
				.addChildValue("CAUSE", extractSubStructure(attribute, "CAUS")
					.getValue())
				.addChildValue("RESTRICTION", extractSubStructure(attribute, "RESN")
					.getValue());
			notesTo(attribute, destinationAttribute, destination);
			sourceCitationTo(attribute, destinationAttribute, destination);
			documentsTo(attribute, destinationAttribute, destination);
			destinationNode.addChild(destinationAttribute);
		}
	}

	private void placeAddressStructureTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final GedcomNode place = extractSubStructure(parent, "PLAC");
		final GedcomNode map = extractSubStructure(place, "MAP");
		final String placeID = destination.getNextPlaceID();
		final GedcomNode destinationPlace = GedcomNode.create("PLACE")
			.withID(placeID)
			.withValue(place.getValue())
			.addChild(GedcomNode.create("MAP")
				.addChildValue("LATITUDE", extractSubStructure(map, "LATI")
					.getValue())
				.addChildValue("LONGITUDE", extractSubStructure(map, "LONG")
					.getValue())
			);
		notesTo(place, destinationPlace, destination);

		final GedcomNode address = extractSubStructure(parent, "ADDR");
		final StringJoiner sj = new StringJoiner(" - ");
		final String wholeAddress = address.extractValueConcatenated();
		if(wholeAddress != null)
			sj.add(wholeAddress);
		for(final GedcomNode child : address.getChildren())
			if(ADDRESS_TAGS.contains(child.getTag())){
				final String value = child.getValue();
				if(value != null)
					sj.add(value);
			}

		destination.addPlace(GedcomNode.create("PLACE")
			.withID(placeID)
			.addChildValue("ADDRESS", sj.toString())
			.addChildValue("CITY", extractSubStructure(address, "CITY").getValue())
			.addChildValue("STATE", extractSubStructure(address, "STAE").getValue())
			.addChildValue("COUNTRY", extractSubStructure(address, "CTRY").getValue())
		);
		destinationNode.addChildReference("PLACE", placeID);
	}


	@Override
	public void from(final Flef origin, final Gedcom destination){
		final List<GedcomNode> individuals = origin.getIndividuals();
		for(final GedcomNode individual : individuals)
			individualRecordFrom(individual, destination);
	}

	private void individualRecordFrom(final GedcomNode individual, final Gedcom destination){
		final GedcomNode destinationIndividual = GedcomNode.create("INDI")
			.withID(individual.getID());
		final String date = extractSubStructure(individual, "DATE")
			.getValue();
		final GedcomNode destinationData = GedcomNode.create("DATA");
		final List<GedcomNode> events = individual.getChildrenWithTag("EVENT");
		for(final GedcomNode event : events)
			destinationData.addChild(GedcomNode.create("EVEN")
				.withValue(event.getValue())
				.addChildValue("DATE", date));
		destinationIndividual.addChild(destinationData);
		final GedcomNode title = extractSubStructure(individual, "TITLE");
		destinationIndividual.addChildValue("TITL", title.getValue());
		final GedcomNode text = extractSubStructure(individual, "TEXT");
		destinationIndividual.addChildValue("TEXT", text.getValue());
		destination.addIndividual(destinationIndividual);
	}

}
