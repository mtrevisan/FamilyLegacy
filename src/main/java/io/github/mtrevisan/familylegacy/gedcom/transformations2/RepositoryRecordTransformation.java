package io.github.mtrevisan.familylegacy.gedcom.transformations2;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.List;

import static io.github.mtrevisan.familylegacy.gedcom.transformations2.TransformationHelper.extractSubStructure;
import static io.github.mtrevisan.familylegacy.gedcom.transformations2.TransformationHelper.joinIfNotNull;


public class RepositoryRecordTransformation implements Transformation<Gedcom, Flef>{

	@Override
	public void to(final Gedcom origin, final Flef destination){
		final List<GedcomNode> repositories = origin.getRepositories();
		for(final GedcomNode repository : repositories)
			repositoryTo(repository, destination);
	}

	private void repositoryTo(final GedcomNode repository, final Flef destination){
//		+1 PHON <PHONE_NUMBER>    {0:3}	/* A phone number. */
//		+1 EMAIL <ADDRESS_EMAIL>    {0:3}	/* An electronic address that can be used for contact such as an email address. */
//		+1 FAX <ADDRESS_FAX>    {0:3}	/* A FAX telephone number appropriate for sending data facsimiles. */
//		+1 WWW <ADDRESS_WEB_PAGE>    {0:3}	/* The world wide web page address. */
//		+1 <<NOTE_STRUCTURE>>    {0:M}	/* A list of NOTE_STRUCTURE() objects. */

//		+1 <<CONTACT_STRUCTURE>>    {0:1}	/* A CONTACT_STRUCTURE() object giving the contacts of the repository. */
//		+1 NOTE @<XREF:NOTE>@    {0:M}	/* An xref ID of a note record. */

		final GedcomNode destinationRepository = GedcomNode.create("REPOSITORY")
			.withID(repository.getID());
		final String name = extractSubStructure(repository, "NAME")
			.getValue();
		destinationRepository.addChildValue("NAME", name);
		addressStructureTo(repository, destinationRepository, destination);




		String date = null;
		final List<GedcomNode> events = extractSubStructure(repository, "DATA")
			.getChildrenWithTag("EVEN");
		for(final GedcomNode event : events){
			if(date == null)
				date = extractSubStructure(event, "DATE")
					.getValue();

			destinationRepository.addChildValue("EVENT", event.getValue());
		}
		destinationRepository.addChildValue("DATE", date);
		destinationRepository.addChildValue("TEXT", extractSubStructure(repository, "TEXT")
			.getValueConcatenated());
		final String author = extractSubStructure(repository, "AUTH")
			.getValueConcatenated();
		final String publication = extractSubStructure(repository, "PUBL")
			.getValueConcatenated();
		final String noteAuthorPublication = joinIfNotNull(", ", author, publication);
		if(noteAuthorPublication != null){
			final String noteID = destination.getNextNoteID();
			destinationRepository.addChildReference("NOTE", noteID);
			destination.addNote(GedcomNode.create("NOTE", noteID, noteAuthorPublication));
		}
		destination.addRepository(destinationRepository);
	}

	private void addressStructureTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
//		+1 <<ADDRESS_STRUCTURE>>    {0:1}	/* An ADDRESS_STRUCTURE() object giving details of the repository address. */
//		+1 ADDR <ADDRESS_LINE>    {1:1}	/* The address lines usually contain the addressee’s street and city information so that it forms an address that meets mailing requirements. */
//			+" CONT <ADDRESS_LINE>    {0:3}
//			+2 ADR1 <ADDRESS_LINE1>    {0:1}
//			+2 ADR2 <ADDRESS_LINE2>    {0:1}
//			+2 ADR3 <ADDRESS_LINE3>    {0:1}
//			+2 CITY <ADDRESS_CITY>    {0:1}	/* The name of the city/town used in the address. Isolated for sorting or indexing. */
//			+2 STAE <ADDRESS_STATE>    {0:1}	/* The name of the US state/UK county used in the address. Isolated for sorting or indexing. */
//			+2 POST <ADDRESS_POSTAL_CODE>    {0:1}	/* The ZIP or postal code used by the various localities in handling of mail. Isolated for sorting or indexing. */
//			+2 CTRY <ADDRESS_COUNTRY>    {0:1}	/* The name of the country that pertains to the associated address. Isolated by some systems for sorting or indexing. Used in most cases to facilitate automatic sorting of mail. */

//		+1 PLACE @<XREF:PLACE>@    {0:1}	/* A PLACE_RECORD() object giving the location of the repository. */
//plus, in destination:
// 	n @<XREF:PLACE>@ PLACE    {1:1}	/* An xref ID of a place record. */
//		+1 NAME <PLACE_NAME>    {0:1}	/* The name of the place formatted for display and address generation. */
//		+1 <<PLACE_STRUCTURE>>    {1:M}	/* A list of PLACE_STRUCTURE() objects. */
//		+1 SUBORDINATE @<XREF:PLACE>@    {0:1}	/* A PLACE_RECORD() object this place structure is subordinated to. */
//		+1 NOTE @<XREF:NOTE>@    {0:M}	/* An xref ID of a note record. */
//		+1 RESTRICTION <RESTRICTION_NOTICE>    {0:1}	/* Specifies how the superstructure should be treated. Known values and their meaning are: "confidential" (should not be distributed or exported), "locked" (should not be edited), "private" (has had information omitted to maintain confidentiality) */

		final List<GedcomNode> notes = parent.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes){
			String noteID = note.getID();
			if(noteID == null){
				noteID = destination.getNextNoteID();

				destination.addNote(GedcomNode.create("NOTE", noteID, note.getValueConcatenated()));
			}
			destinationNode.addChildReference("NOTE", noteID);
		}
	}

	private void notesTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> notes = parent.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes){
			String noteID = note.getID();
			if(noteID == null){
				noteID = destination.getNextNoteID();

				destination.addNote(GedcomNode.create("NOTE", noteID, note.getValueConcatenated()));
			}
			destinationNode.addChildReference("NOTE", noteID);
		}
	}


	@Override
	public void from(final Flef origin, final Gedcom destination){
		final List<GedcomNode> repositories = origin.getRepositories();
		for(final GedcomNode repository : repositories)
			repositoryFrom(repository, destination);
	}

	private void repositoryFrom(final GedcomNode source, final Gedcom destination){
//	n @<XREF:REPOSITORY>@ REPOSITORY    {1:1}	/* An xref ID of a repository record. */
//		+1 NAME <NAME_OF_REPOSITORY>    {0:1}	/* The official name of the archive in which the stated source material is stored. */
//		+1 INDIVIDUAL @<XREF:INDIVIDUAL>@    {0:1}	/* An xref ID of the individual, if present in the tree and is the repository of a source. */
//		+1 PLACE @<XREF:PLACE>@    {0:1}	/* A PLACE_RECORD() object giving the location of the repository. */
//		+1 <<CONTACT_STRUCTURE>>    {0:1}	/* A CONTACT_STRUCTURE() object giving the contacts of the repository. */
//		+1 <<SOURCE_CITATION>>    {0:M}	/* A list of SOURCE_CITATION() objects. */
//		+1 NOTE @<XREF:NOTE>@    {0:M}	/* An xref ID of a note record. */

//	n @<XREF:REPO>@ REPO    {1:1}	/* An xref ID of a repository record. */
//		+1 NAME <NAME_OF_REPOSITORY>    {1:1}	/* The official name of the archive in which the stated source material is stored. */
//		+1 <<ADDRESS_STRUCTURE>>    {0:1}	/* An ADDRESS_STRUCTURE() object giving details of the repository address. */
//		+1 <<NOTE_STRUCTURE>>    {0:M}	/* A list of NOTE_STRUCTURE() objects. */
//		+1 REFN <USER_REFERENCE_NUMBER>    {0:M}	/*  user-defined number or text that the submitter uses to identify this record. */
//		+2 TYPE <USER_REFERENCE_TYPE>    {0:1}	/* A user-defined definition of the user_reference_number. */
//		+1 RIN <AUTOMATED_RECORD_ID>    {0:1}	/* A unique record identification number assigned to the record by the source system. This number is intended to serve as a more sure means of identification of a record for reconciling differences in data between two interfacing systems. */
//		+1 <<CHANGE_DATE>>    {0:1}	/* A CHANGE_DATE() object giving the time this record was last modified. If not provided, the current date is used. */

		final GedcomNode destinationRepository = GedcomNode.create("REPO")
			.withID(source.getID());
		final String date = extractSubStructure(source, "DATE")
			.getValue();
		final GedcomNode destinationData = GedcomNode.create("DATA");
		final List<GedcomNode> events = source.getChildrenWithTag("EVENT");
		for(final GedcomNode event : events)
			destinationData.addChild(GedcomNode.create("EVEN")
				.withValue(event.getValue())
				.addChildValue("DATE", date));
		destinationRepository.addChild(destinationData);
		final GedcomNode title = extractSubStructure(source, "TITLE");
		destinationRepository.addChildValue("TITL", title.getValueConcatenated());
		final GedcomNode text = extractSubStructure(source, "TEXT");
		destinationRepository.addChildValue("TEXT", text.getValueConcatenated());
		destination.addRepository(destinationRepository);
	}

	private void notesFrom(final GedcomNode parent, final GedcomNode destinationNode){
		final List<GedcomNode> notes = parent.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			destinationNode.addChild(GedcomNode.create("NOTE")
				.withID(note.getID()));
	}

}
