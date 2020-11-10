package io.github.mtrevisan.familylegacy.gedcom.transformations2;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.List;
import java.util.StringJoiner;

import static io.github.mtrevisan.familylegacy.gedcom.transformations2.TransformationHelper.deleteTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations2.TransformationHelper.extractSubStructure;
import static io.github.mtrevisan.familylegacy.gedcom.transformations2.TransformationHelper.moveTag;


public class SourceRecordTransformation implements Transformation<Gedcom, Flef>{

	@Override
	public void to(final Gedcom origin, final Flef destination){
		final List<GedcomNode> sources = origin.getSources();
		for(final GedcomNode source : sources)
			sourceTo(source, origin, destination);
	}

	private void sourceTo(final GedcomNode source, final Gedcom origin, final Flef destination){
		final GedcomNode title = extractSubStructure(source, "TITL");
		final List<GedcomNode> events = extractSubStructure(source, "DATA")
			.getChildrenWithTag("EVENT");
		final String author = extractSubStructure(source, "AUTH")
			.getValueConcatenated();
		final String publication = extractSubStructure(source, "PUBL")
			.getValueConcatenated();
		final String text = extractSubStructure(source, "TEXT")
			.getValueConcatenated();
		final List<GedcomNode> repositories = source.getChildrenWithTag("REPO");

		final GedcomNode destinationSource = GedcomNode.create("SOURCE")
			.withID(source.getID())
			.addChildValue("TITLE", title.getValueConcatenated());
		String date = null;
		for(final GedcomNode event : events){
			if(date == null)
				date = extractSubStructure(event, "DATE")
					.getValue();

			destinationSource.addChildValue("EVENT", event.getValue());
		}
		destinationSource.addChildValue("DATE", date);
		final StringJoiner sj = new StringJoiner(", ");
		if(author != null)
			sj.add(author);
		if(publication != null)
			sj.add(publication);
		if(sj.length() > 0){
			final String noteID = Flef.getNextNoteID(destination.getNotes().size());
			destinationSource.addChildReference("NOTE", noteID);
			destination.addNote(GedcomNode.create("NOTE", noteID, sj.toString()));
		}
		destinationSource.addChildValue("TEXT", text);
		final List<GedcomNode> documents = source.getChildrenWithTag("OBJE");
		for(final GedcomNode document : documents){
			if(document.getID() == null){
				final GedcomNode destinationDocument = GedcomNode.create("SOURCE")
					.withValue(text);
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
				destination.addSource(destinationDocument);
			}
			final String documentID = Flef.getNextSourceID(destination.getSources().size());
			destinationSource.addChildReference("SOURCE", documentID);
		}
		notesTo(source, destinationSource, destination);
		for(final GedcomNode repository : repositories){
			final GedcomNode destinationRepository = GedcomNode.create("REPOSITORY");
			final String repositoryID = (repository.getID() == null?
				Flef.getNextRepositoryID(destination.getRepositories().size()): repository.getID());
			if(repository.getID() == null){
				destination.addRepository(destinationRepository);

				notesTo(repository, destinationRepository, destination);
			}
			destinationSource.addChild(destinationRepository
				.withID(repositoryID));
		}
		destination.addSource(destinationSource);
	}

	private void notesTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> notes = parent.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes){
			String noteID = note.getID();
			if(noteID == null){
				noteID = Flef.getNextNoteID(destination.getNotes().size());
				destination.addNote(GedcomNode.create("NOTE", noteID, note.getValueConcatenated()));
			}
			destinationNode.addChildReference("NOTE", noteID);
		}
	}

	@Override
	public void from(final Flef origin, final Gedcom destination){
		final GedcomNode header = origin.getHeader();

//		+1 TYPE [ DIGITAL_ARCHIVE | MICROFILM | DATABASE | GRAVE_MARKER | <CUSTOM_TYPE> | <NULL> ]    {0:1}
//		+1 TITLE <SOURCE_DESCRIPTIVE_TITLE>    {0:1}	/* The title of the work, record, or item and, when appropriate, the title of the larger work or series of which it is a part. */
//		+1 EVENT <EVENTS_RECORDED>    {0:M}	/* An enumeration of the different kinds of events that were recorded in a particular source. Each enumeration is separated by a comma. Such as the type of event which was responsible for the source entry being recorded (CASTE, EDUCATION, NATIONALITY, OCCUPATION, PROPERTY, RELIGION, RESIDENCE, TITLE, FACT, ANNULMENT, CENSUS, DIVORCE, DIVORCE_FILED, ENGAGEMENT, MARRIAGE, MARRIAGE_BANN, MARRIAGE_CONTRACT, MARRIAGE_LICENCE, MARRIAGE_SETTLEMENT, ADOPTION, BIRTH, BURIAL, CREMATION, DEATH, EMIGRATION, GRADUATION, IMMIGRATION, NATURALIZATION, RETIREMENT, DEED, PROBATE, WILL, EVENT, etc). For example, if the entry was created to record a birth of a child, then the type would be BIRTH regardless of the assertions made from that record, such as the mother's name or mother's birth date. This will allow a prioritized best view choice and a determination of the certainty associated with the source used in asserting the cited fact. */
//		+1 DATE <ENTRY_RECORDING_DATE>    {0:1}	/* A date_value() object giving the date that this event data was entered into the original source document. */
//		+1 TEXT <TEXT_FROM_SOURCE>    {0:1}	/* A verbatim copy of any description contained within the source. This indicates notes or text that are actually contained in the source document, not the submitter's opinion about the source. */
//		+1 DOCUMENT @<XREF:DOCUMENT>@    {0:M}	/* An xref ID of a document record. */
//		+1 NOTE @<XREF:NOTE>@    {0:M}	/* An xref ID of a note record. May contain information to identify a book (author, publisher, ISBN code, ...), a digital archive (website name, creator, ...), a microfilm (record title, record file, collection, film ID, roll number, ...), etc. */
//		+1 REPOSITORY @<XREF:REPOSITORY>@    {0:M}	/* A list of xref ID of repository records who owns this source. */
//			+2 REPOSITORY_LOCATION <REPOSITORY_LOCATION_TEXT>    {0:1}	/* A note on the location of the document inside the repository (Usually an identification or reference description used to file and retrieve items from the holdings of a repository, or the page and number of the entry inside a registry). */
//			+2 NOTE @<XREF:NOTE>@    {0:M}	/* An xref ID of a note record. */
//		+1 URL <ADDRESS_WEB_PAGE>    {0:1}	/* The world wide web page address following RFC 1736 specifications. */
//		+1 RESTRICTION <RESTRICTION_NOTICE>    {0:1}	/* Specifies how the superstructure should be treated. Known values and their meaning are: "confidential" (should not be distributed or exported), "locked" (should not be edited), "private" (has had information omitted to maintain confidentiality) */

//		+1 DATA    {0:1}
//			+2 EVEN <EVENTS_RECORDED>    {0:M}	/* An enumeration of the different kinds of events that were recorded in a particular source. Each enumeration is separated by a comma. Such as a parish register of births, deaths, and marriages would be BIRT, DEAT, MARR. These can be enumerated over more than one vector element. */
//				+3 DATE <DATE_PERIOD>    {0:1}	/* A date_period() object associated with the period covered by the course. */
//				+3 PLAC <SOURCE_JURISDICTION_PLACE>    {0:1}	/* The name of the lowest jurisdiction that encompasses all lower-level places named in this source. */
//			+2 AGNC <RESPONSIBLE_AGENCY>    {0:1}	/* The organization, institution, corporation, person, or other entity that has responsibility for the associated context. For example, an employer of a person of an associated occupation, or a church that administered rites or events, or an organization responsible for creating and/or archiving records. */
//			+2 <<NOTE_STRUCTURE>>    {0:M}	/* A list of NOTE_STRUCTURE() objects associated with the data in this source. */
//		+1 AUTH <SOURCE_ORIGINATOR>    {0:1}	/* The person, agency, or entity who created the record. For a published work, this could be the author, compiler, transcriber, abstractor, or editor. For an unpublished source, this may be an individual, a government agency, church organization, or private organization, etc. */
//			+2 [ CONC | CONT ] <SOURCE_ORIGINATOR>    {0:M}
//		+1 TITL <SOURCE_DESCRIPTIVE_TITLE>    {0:1}	/* The title of the work, record, or item and, when appropriate, the title of the larger work or series of which it is a part. */
//			+2 [ CONC | CONT ] <SOURCE_DESCRIPTIVE_TITLE>    {0:M}
//		+1 ABBR <SOURCE_FILED_BY_ENTRY>    {0:1}	/* This entry is to provide a short title used for sorting, filing, and retrieving source records. */
//		+1 PUBL <SOURCE_PUBLICATION_FACTS>    {0:1}	/* When and where the record was created. */
//			+2 [ CONC | CONT ] <SOURCE_PUBLICATION_FACTS>    {0:M}
//		+1 TEXT <TEXT_FROM_SOURCE>    {0:1}	/* A verbatim copy of any description contained within the source. This indicates notes or text that are actually contained in the source document, not the submitter's opinion about the source. */
//			+2 [ CONC | CONT ] <TEXT_FROM_SOURCE>    {0:M}
//		+1 <<SOURCE_REPOSITORY_CITATION>>    {0:M}	/* A list of SOURCE_REPOSITORY_CITATION() objects. */
//		+1 REFN <USER_REFERENCE_NUMBER>    {0:M}	/* A user-defined number or text that the submitter uses to identify this record. */
//			+2 TYPE <USER_REFERENCE_TYPE>    {0:1}	/* A user-defined definition of the user_reference_number. */
//		+1 RIN <AUTOMATED_RECORD_ID>    {0:1}	/* A unique record identification number assigned to the record by the source system. This number is intended to serve as a more sure means of identification of a record for reconciling differences in data between two interfacing systems. */
//		+1 <<CHANGE_DATE>>    {0:1}	/* A CHANGE_DATE() object giving the time this record was last modified. If not provided, the current date is used. */
//		+1 <<NOTE_STRUCTURE>>    {0:M}	/* A list of NOTE_STRUCTURE() objects. */
//		+1 <<MULTIMEDIA_LINK>>    {0:M}	/* A list of MULTIMEDIA_LINK() objects */

		header.withTag("HEAD");
		final GedcomNode source = extractSubStructure(header, "SOURCE");
		source.withTag("SOUR");
		moveTag("VERS", source, "VERSION");
		moveTag("CORP", source, "CORPORATE");
		final GedcomNode date = extractSubStructure(header, "DATE");
		if(!date.isEmpty()){
			source.addChild(GedcomNode.create("DATA")
				.addChild(date));
		}
		final GedcomNode copyright = extractSubStructure(header, "COPYRIGHT");
		header.removeChild(copyright);
		moveTag("SUBM", source, "SUBMITTER");
		deleteTag(header, "PROTOCOL_VERSION");
		final GedcomNode gedcom = GedcomNode.create("GEDC")
			.addChild(GedcomNode.create("VERS")
				.withValue("5.5.1"))
			.addChild(GedcomNode.create("FORM")
				.withValue("LINEAGE_LINKED"));
		final GedcomNode charset = extractSubStructure(header, "CHARSET");
		charset.withTag("CHAR");
		header.addChildBefore(gedcom, charset);
		header.addChildBefore(copyright, charset);
		final GedcomNode note = extractSubStructure(header, "NOTE");
		note.withValueConcatenated(note.getValue());

		destination.getHeader()
			.cloneFrom(header);
	}

}
