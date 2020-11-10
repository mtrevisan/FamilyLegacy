package io.github.mtrevisan.familylegacy.gedcom.transformations2;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.StringJoiner;

import static io.github.mtrevisan.familylegacy.gedcom.transformations2.TransformationHelper.extractSubStructure;


public class RepositoryRecordTransformation implements Transformation<Gedcom, Flef>{

	private static final Collection<String> ADDRESS_TAGS = new HashSet<>(Arrays.asList("CONT", "ADR1", "ADR2", "ADR3"));


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

//		+1 CONTACT
//			+2 PHONE <PHONE_NUMBER>    {0:M}	/* A phone number. */
//				+3 TYPE <PHONE_NUMBER_TYPE>    {0:1}	/* Indicates the phone number type (blog, personal, social, work, etc.). */
//				+3 CALLER_ID    {0:1}	/* Indicates the name of the person associated with this contact. */
//				+3 RESTRICTION <RESTRICTION_NOTICE>    {0:1}	/* Specifies how the superstructure should be treated. Known values and their meaning are: "confidential" (should not be distributed or exported), "locked" (should not be edited), "private" (has had information omitted to maintain confidentiality) */
//			+2 EMAIL <ADDRESS_EMAIL>    {0:M}	/* An electronic address that can be used for contact such as an email address following RFC 5322 specifications. */
//				+3 TYPE <ADDRESS_EMAIL_TYPE>    {0:1}	/* Indicates the email type (blog, personal, social, work, etc.). */
//				+3 CALLER_ID    {0:1}	/* Indicates the name of the person associated with this contact. */
//				+3 RESTRICTION <RESTRICTION_NOTICE>    {0:1}	/* Specifies how the superstructure should be treated. Known values and their meaning are: "confidential" (should not be distributed or exported), "locked" (should not be edited), "private" (has had information omitted to maintain confidentiality) */
//			+2 URL <ADDRESS_WEB_PAGE>    {0:M}	/* The world wide web page address following RFC 1736 specifications. */
//				+3 TYPE <ADDRESS_WEB_PAGE_TYPE>    {0:1}	/* Indicates the web page type (blog, personal, social, work, etc.). */
//		+1 NOTE @<XREF:NOTE>@    {0:M}	/* An xref ID of a note record. */

		final GedcomNode destinationRepository = GedcomNode.create("REPOSITORY")
			.withID(repository.getID());
		final String name = extractSubStructure(repository, "NAME")
			.getValue();
		destinationRepository.addChildValue("NAME", name);
		addressStructureTo(repository, destinationRepository, destination);

		//TODO

		destination.addRepository(destinationRepository);
	}

	private void addressStructureTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final GedcomNode address = extractSubStructure(parent, "ADDR");
		if(!address.isEmpty()){
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

			final String placeID = destination.getNextPlaceID();
			destination.addPlace(GedcomNode.create("PLACE")
				.withID(placeID)
				.addChildValue("ADDRESS", sj.toString())
				.addChildValue("CITY", extractSubStructure(address, "CITY").getValue())
				.addChildValue("STATE", extractSubStructure(address, "STAE").getValue())
				.addChildValue("COUNTRY", extractSubStructure(address, "CTRY").getValue())
			);
			destinationNode.addChildReference("PLACE", placeID);
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
