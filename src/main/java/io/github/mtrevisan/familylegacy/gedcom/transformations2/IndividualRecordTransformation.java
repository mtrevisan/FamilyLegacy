package io.github.mtrevisan.familylegacy.gedcom.transformations2;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.StringJoiner;

import static io.github.mtrevisan.familylegacy.gedcom.transformations2.TransformationHelper.extractSubStructure;


public class IndividualRecordTransformation implements Transformation<Gedcom, Flef>{

	@Override
	public void to(final Gedcom origin, final Flef destination){
		final List<GedcomNode> individuals = origin.getIndividuals();
		for(final GedcomNode individual : individuals)
			individualRecordTo(individual, destination);
	}

	private void individualRecordTo(final GedcomNode individual, final Flef destination){
		final GedcomNode destinationIndividual = GedcomNode.create("INDIVIDUAL")
			.withID(individual.getID());
		destinationIndividual.addChildValue("SEX", extractSubStructure(individual, "SEX")
			.getValue());
		final List<GedcomNode> personalNameStructures = individual.getChildrenWithTag("NAME");
		for(final GedcomNode personalNameStructure : personalNameStructures)
			personalNameTo(personalNameStructure, destination);
		final List<GedcomNode> childToFamilyLinks = individual.getChildrenWithTag("FAMC");
		for(final GedcomNode childToFamilyLink : childToFamilyLinks){
			final GedcomNode destinationFamilyChild = GedcomNode.create("FAMILY_CHILD")
				.addChildValue("PEDIGREE", extractSubStructure(childToFamilyLink, "PEDI")
					.getValue())
				.addChildValue("STATUS", extractSubStructure(childToFamilyLink, "STAT")
					.getValue());
			notesTo(childToFamilyLink, destinationFamilyChild, destination);
			destinationIndividual.addChild(destinationFamilyChild);
		}
		final List<GedcomNode> spouseToFamilyLinks = individual.getChildrenWithTag("FAMS");
		for(final GedcomNode spouseToFamilyLink : spouseToFamilyLinks){
			final GedcomNode destinationFamilySpouse = GedcomNode.create("FAMILY_SPOUSE");
			notesTo(spouseToFamilyLink, destinationFamilySpouse, destination);
			destinationIndividual.addChild(destinationFamilySpouse);
		}
		final List<GedcomNode> associations = individual.getChildrenWithTag("ASSO");
		final List<GedcomNode> aliases = individual.getChildrenWithTag("ALIA");
		final List<GedcomNode> eventBirths = individual.getChildrenWithTag("BIRT");
		final List<GedcomNode> eventDeaths = individual.getChildrenWithTag("DEAT");
		final List<GedcomNode> eventBurials = individual.getChildrenWithTag("BURI");
		final List<GedcomNode> eventCremations = individual.getChildrenWithTag("CREM");
		final List<GedcomNode> eventAdoptions = individual.getChildrenWithTag("ADOP");
		final List<GedcomNode> eventNaturalizations = individual.getChildrenWithTag("NATU");
		final List<GedcomNode> eventEmigrations = individual.getChildrenWithTag("EMIG");
		final List<GedcomNode> eventImmigrations = individual.getChildrenWithTag("IMMI");
		final List<GedcomNode> eventCensus = individual.getChildrenWithTag("CENS");
		final List<GedcomNode> eventProbates = individual.getChildrenWithTag("PROB");
		final List<GedcomNode> eventWill = individual.getChildrenWithTag("WILL");
		final List<GedcomNode> eventGraduations = individual.getChildrenWithTag("GRAD");
		final List<GedcomNode> eventRetirements = individual.getChildrenWithTag("RETI");
		final List<GedcomNode> eventEvents = individual.getChildrenWithTag("EVENT");
		final List<GedcomNode> attributeCastes = individual.getChildrenWithTag("CAST");
		final List<GedcomNode> attributePhysicalDescriptions = individual.getChildrenWithTag("DSCR");
		final List<GedcomNode> attributeEducations = individual.getChildrenWithTag("EDUC");
		final List<GedcomNode> attributeNationalID = individual.getChildrenWithTag("IDNO");
		final List<GedcomNode> attributeOrigin = individual.getChildrenWithTag("NATI");
		final List<GedcomNode> attributeChildrenCount = individual.getChildrenWithTag("NCHI");
		final List<GedcomNode> attributeMarriagesCount = individual.getChildrenWithTag("NMR");
		final List<GedcomNode> attributeOccupations = individual.getChildrenWithTag("OCCU");
		final List<GedcomNode> attributePossessions = individual.getChildrenWithTag("PROP");
		final List<GedcomNode> attributeReligions = individual.getChildrenWithTag("RELI");
		final List<GedcomNode> attributeResidences = individual.getChildrenWithTag("RESI");
		final List<GedcomNode> attributeSSNs = individual.getChildrenWithTag("SSN");
		final List<GedcomNode> attributeTitles = individual.getChildrenWithTag("TITL");
		final List<GedcomNode> attributeFacts = individual.getChildrenWithTag("FACT");
		final List<GedcomNode> notes = individual.getChildrenWithTag("NOTE");
		final List<GedcomNode> sourceCitations = individual.getChildrenWithTag("SOUR");
		final List<GedcomNode> documents = individual.getChildrenWithTag("OBJE");
		destinationIndividual.addChildValue("RESTRICTION", extractSubStructure(individual, "RESN")
			.getValue());

//		+1 FAMILY_SPOUSE @<XREF:FAMILY>@    {0:M}	/* An xref ID of a family record this individual is a spouse of. */
//			+2 NOTE @<XREF:NOTE>@    {0:M}	/* An xref ID of a note record. */
//		+1 ASSOCIATION @<XREF:ID>@    {0:M}	/* An xref ID of the individual/family this individual is associated with. */
//			+2 TYPE <ASSOCIATION_TYPE>    {1:1}	/* The type of associated record. May be 'individual', or 'family'. */
//			+2 RELATIONSHIP <RELATION_IS_DESCRIPTOR>    {1:1}	/* A word or phrase that states object 1's relation is object 2. If the payload text is R, the person described by the record pointed to by the payload of the superstructure is P, and the person described by the superstructure of the superstructure is Q then this payload means “P is Q’s R”.*/
//			+2 NOTE @<XREF:NOTE>@    {0:M}	/* An xref ID of a note record. */
//			+2 <<SOURCE_CITATION>>    {0:M}	/* A list of SOURCE_CITATION() objects. */
//		+1 ALIAS @<XREF:INDIVIDUAL>@    {0:M}	/* A vector of xref IDs of individual aliases of this individual. */
//			+2 NOTE @<XREF:NOTE>@    {0:M}	/* An xref ID of a note record associated with the alias. */
//		+1 <<INDIVIDUAL_EVENT_STRUCTURE>>    {0:M}	/* A list of INDIVIDUAL_EVENT_STRUCTURE() objects giving the events associated with this individual. */
//		+1 <<INDIVIDUAL_ATTRIBUTE_STRUCTURE>>    {0:M}	/* A list of INDIVIDUAL_ATTRIBUTE_STRUCTURE() objects giving the attributes associated with this individual. */
//		+1 NOTE @<XREF:NOTE>@    {0:M}	/* An xref ID of a note record. */
//		+1 <<SOURCE_CITATION>>    {0:M}	/* A list of SOURCE_CITATION() objects. */

		//...
		destination.addIndividual(destinationIndividual);
	}

	private void personalNameTo(final GedcomNode personalNameStructure, final Flef destination){
		final GedcomNode destinationName = GedcomNode.create("NAME")
			.addChildValue("TYPE", extractSubStructure(personalNameStructure, "TYPE")
				.getValue());
		personalNamePiecesTo(personalNameStructure, destinationName, destination);
		final GedcomNode destinationPhoneticName = GedcomNode.create("PHONETIC");
		personalNamePiecesTo(extractSubStructure(personalNameStructure, "FONE"), destinationPhoneticName, destination);
		destinationName.addChild(destinationPhoneticName);
		final GedcomNode destinationRomanizedName = GedcomNode.create("TRANSCRIPTION");
		personalNamePiecesTo(extractSubStructure(personalNameStructure, "ROMN"), destinationRomanizedName, destination);
		destinationName.addChild(destinationRomanizedName);
		personalNameStructure.addChild(destinationName);
	}

	private void personalNamePiecesTo(final GedcomNode personalNameStructure, final GedcomNode destinationNode, final Flef destination){
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
		destination.addSource(destinationIndividual);
	}

}
