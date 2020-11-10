package io.github.mtrevisan.familylegacy.gedcom.transformations2;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

import static io.github.mtrevisan.familylegacy.gedcom.transformations2.TransformationHelper.deleteTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations2.TransformationHelper.extractSubStructure;
import static io.github.mtrevisan.familylegacy.gedcom.transformations2.TransformationHelper.moveTag;


public class PeopleTransformation implements Transformation<Gedcom, Flef>{

	@Override
	public void to(final Gedcom origin, final Flef destination){
		final List<GedcomNode> people = origin.getPeople();

		for(final GedcomNode individual : people)
			individualTo(individual, origin, destination);

		destination.getPeople()
			.addAll(people);
	}

	private void individualTo(final GedcomNode individual, final Gedcom origin, final Flef destination){
		individual.withTag("INDIVIDUAL");
		final GedcomNode restriction = extractSubStructure(individual, "RESN");
		individual.removeChild(restriction);
		final List<GedcomNode> personalNames = individual.getChildrenWithTag("NAME");
		for(final GedcomNode personalName : personalNames){
			personalNameTo(personalName);

			final GedcomNode phonetic = extractSubStructure(personalName, "FONE");
			if(!phonetic.isEmpty()){
				phonetic.withTag("PHONETIC");
				personalNameTo(phonetic);
			}

			deleteTag(personalName, "ROMN");

			final List<GedcomNode> notes = personalName.getChildrenWithTag("NOTE");
			for(final GedcomNode note : notes)
				noteTo(note, origin, destination);
			final List<GedcomNode> sourceCitations = personalName.getChildrenWithTag("SOUR");
			for(final GedcomNode sourceCitation : sourceCitations)
				sourceCitationTo(sourceCitation, origin, destination);
		}

//		+1 SEX <SEX_VALUE>    {0:1}	/* A code that indicates the sex of the individual. */
//		+1 <<INDIVIDUAL_EVENT_STRUCTURE>>    {0:M}	/* A list of INDIVIDUAL_EVENT_STRUCTURE() objects giving the events associated with this individual. */
//		+1 <<INDIVIDUAL_ATTRIBUTE_STRUCTURE>>    {0:M}	/* A list of INDIVIDUAL_ATTRIBUTE_STRUCTURE() objects giving the attributes associated with this individual. */
//		+1 <<LDS_INDIVIDUAL_ORDINANCE>>    {0:M}
//		+1 <<CHILD_TO_FAMILY_LINK>>    {0:M}	/* A list of CHILD_TO_FAMILY_LINK() objects giving the details of families this individual is a child of. */
//		+1 <<SPOUSE_TO_FAMILY_LINK>>    {0:M}	/* A list of SPOUSE_TO_FAMILY_LINK() objects giving the details of families this individual is a spouse of. */
//		+1 SUBM @<XREF:SUBM>@    {0:M}	/* A vector of xref IDs of submitters of this record. */
//		+1 <<ASSOCIATION_STRUCTURE>>    {0:M}	/* A list of ASSOCIATION_STRUCTURE() objects giving the details of individuals this individual is associated with. */
//		+1 ALIA @<XREF:INDI>@    {0:M}	/* A vector of xref IDs of individual aliases of this individual. */
//		+1 ANCI @<XREF:SUBM>@    {0:M}	/* A vector of xref IDs of submitters with an interest in ancestors of this individual. */
//		+1 DESI @<XREF:SUBM>@    {0:M}	/* A vector of xref IDs of submitters with an interest in descendants of this individual. */
//		+1 RFN <PERMANENT_RECORD_FILE_NUMBER>    {0:1}	/* The record number that uniquely identifies this record within a registered network resource. */
//		+1 AFN <ANCESTRAL_FILE_NUMBER>    {0:1}	/* A unique permanent record number of an individual record contained in the Family History Department's Ancestral File. */
//		+1 REFN <USER_REFERENCE_NUMBER>    {0:M}	/* A user-defined number or text that the submitter uses to identify this record. */
//			+2 TYPE <USER_REFERENCE_TYPE>    {0:1}	/* A user-defined definition of the user_reference_number. */
//		+1 RIN <AUTOMATED_RECORD_ID>    {0:1}	/* A unique record identification number assigned to the record by the source system. This number is intended to serve as a more sure means of identification of a record for reconciling differences in data between two interfacing systems. */
//		+1 <<CHANGE_DATE>>    {0:1}	/* A CHANGE_DATE() object giving the time this record was last modified. If not provided, the current date is used. */
//		+1 <<NOTE_STRUCTURE>>    {0:M}	/* A list of NOTE_STRUCTURE() objects. */
//		+1 <<SOURCE_CITATION>>    {0:M}	/* A list of SOURCE_CITATION() objects. */
//		+1 <<MULTIMEDIA_LINK>>    {0:M}	/* A list of MULTIMEDIA_LINK() objects */

//		+1 SEX <SEX_VALUE>    {0:1}	/* A code that indicates the sexual anatomy of the individual (one of MALE, FEMALE). */
//		+1 GENDER <GENDER_VALUE>    {0:1}	/* A code that indicates the gender of the individual (one of MALE, FEMALE, INTERSEX, UNKNOWN). */
//		+1 SEXUAL_ORIENTATION <SEXUAL_ORIENTATION_VALUE>    {0:1}	/* A code that indicates the sexual orientation of the individual (one of MALE, FEMALE, BOTH). */
//		+1 <<CHILD_TO_FAMILY_LINK>>    {0:M}	/* A list of CHILD_TO_FAMILY_LINK() objects giving the details of families this individual is a child of. */
//		+1 <<SPOUSE_TO_FAMILY_LINK>>    {0:M}	/* A list of SPOUSE_TO_FAMILY_LINK() objects giving the details of families this individual is a spouse of. */
//		+1 <<ASSOCIATION_STRUCTURE>>    {0:M}	/* A list of ASSOCIATION_STRUCTURE() objects giving the details of individuals or families this individual is associated with. */
//		+1 ALIAS @<XREF:INDIVIDUAL>@    {0:M}	/* A vector of xref IDs of individual aliases of this individual. */
//			+2 NOTE @<XREF:NOTE>@    {0:M}	/* An xref ID of a note record associated with the alias. */
//		+1 <<INDIVIDUAL_EVENT_STRUCTURE>>    {0:M}	/* A list of INDIVIDUAL_EVENT_STRUCTURE() objects giving the events associated with this individual. */
//		+1 <<INDIVIDUAL_ATTRIBUTE_STRUCTURE>>    {0:M}	/* A list of INDIVIDUAL_ATTRIBUTE_STRUCTURE() objects giving the attributes associated with this individual. */
//		+1 NOTE @<XREF:NOTE>@    {0:M}	/* An xref ID of a note record. */
//		+1 <<SOURCE_CITATION>>    {0:M}	/* A list of SOURCE_CITATION() objects. */
//		+1 DOCUMENT @<XREF:DOCUMENT>@    {0:M}	/* An xref ID of a document record. */
//		+1 SUBMITTER @<XREF:SUBMITTER>@    {0:M}	/* A vector of xref IDs of submitters of this record. */
//		+1 RESTRICTION <RESTRICTION_NOTICE>    {0:1}	/* Specifies how the superstructure should be treated. Known values and their meaning are: "confidential" (should not be distributed or exported), "locked" (should not be edited), "private" (has had information omitted to maintain confidentiality) */
//		+1 <<CHANGE_DATE>>    {0:1}	/* A CHANGE_DATE() object giving the time this record was last modified. If not provided, the current date is used. */

//		moveMultipleTag("RESTRICTION", node, "RESN");
//		final List<GedcomNode> names = node.getChildrenWithTag("NAME");
//		for(final GedcomNode name : names)
//			PERSONAL_NAME_TRANSFORMATION.to(name, root);
//		INDIVIDUAL_EVENT_STRUCTURE_TRANSFORMATION.to(node, root);
//		INDIVIDUAL_ATTRIBUTE_STRUCTURE_TRANSFORMATION.to(node, root);
//		LDS_INDIVIDUAL_ORDINANCE_TRANSFORMATION.to(node, root);
//		final List<GedcomNode> childToFamilyLinks = node.getChildrenWithTag("FAMC");
//		for(final GedcomNode childToFamilyLink : childToFamilyLinks)
//			CHILD_TO_FAMILY_LINK_TRANSFORMATION.to(childToFamilyLink, root);
//		final List<GedcomNode> spouseToFamilyLinks = node.getChildrenWithTag("FAMS");
//		for(final GedcomNode spouseToFamilyLink : spouseToFamilyLinks)
//			SPOUSE_TO_FAMILY_LINK_TRANSFORMATION.to(spouseToFamilyLink, root);
//		moveMultipleTag("_REFN", node, "REFN");
//		final List<GedcomNode> associationStructures = node.getChildrenWithTag("ASSO");
//		for(final GedcomNode associationStructure : associationStructures)
//			ASSOCIATION_STRUCTURE_TRANSFORMATION.to(associationStructure, root);
//		moveMultipleTag("ALIAS", node, "ALIA");
//		deleteMultipleTag(node, "ANCI");
//		deleteMultipleTag(node, "DESI");
//		deleteTag(node, "RFN");
//		deleteTag(node, "AFN");
//		moveTag("_RIN", node, "RIN");
//		final GedcomNode changeDate = extractSubStructure(node, "CHAN");
//		if(!changeDate.isEmpty())
//			CHANGE_DATE_TRANSFORMATION.to(changeDate, root);
//		final List<GedcomNode> notes = node.getChildrenWithTag("NOTE");
//		for(final GedcomNode note : notes)
//			NOTE_STRUCTURE_TRANSFORMATION.to(note, root);
//		final List<GedcomNode> sourceCitations = node.getChildrenWithTag("SOUR");
//		for(final GedcomNode sourceCitation : sourceCitations)
//			SOURCE_CITATION_TRANSFORMATION.to(sourceCitation, root);
//		final List<GedcomNode> multimediaLinks = node.getChildrenWithTag("OBJE");
//		for(final GedcomNode multimediaLink : multimediaLinks)
//			MULTIMEDIA_LINK_TRANSFORMATION.to(multimediaLink, root);
	}

	private void personalNameTo(final GedcomNode personalName){
		final String personalNameValue = personalName.getValue();
		personalName.removeValue();
		final int slashIndex = personalNameValue.indexOf('/');
		final String nameComponent = (personalNameValue != null && slashIndex >= 0?
			personalNameValue.substring(0, slashIndex - 1): personalNameValue);
		final String surnameComponent = (personalNameValue != null && slashIndex >= 0?
			personalNameValue.substring(slashIndex + 1, personalNameValue.length() - 1): null);
		moveTag("NAME_PREFIX", personalName, "NPFX");
		final GedcomNode givenName = extractSubStructure(personalName, "GIVN");
		if(givenName.getValue() == null)
			givenName.withValue(nameComponent);
		if(givenName.getValue() == null)
			deleteTag(personalName, "GIVN");
		else
			personalName.withTag("PERSONAL_NAME");
		final GedcomNode nickname = extractSubStructure(personalName, "NICK");
		deleteTag(personalName, "NICK");
		final GedcomNode surnamePrefix = extractSubStructure(personalName, "SPFX");
		deleteTag(personalName, "SPFX");
		final GedcomNode surname = extractSubStructure(personalName, "SURN");
		deleteTag(personalName, "SURN");
		if(surname.getValue() == null)
			surname.withValue(surnameComponent);
		if(surname.getValue() != null && !surnamePrefix.isEmpty())
			surname.withValue(StringUtils.replace(surnamePrefix.getValue(), ",", StringUtils.SPACE)
				+ StringUtils.SPACE + surname.getValue());
		final GedcomNode nameSuffix = extractSubStructure(personalName, "NSFX");
		deleteTag(personalName, "NSFX");
		personalName.addChild(nameSuffix);
		personalName.addChild(nickname);
		personalName.addChild(surname);
	}

	private void noteTo(final GedcomNode note, final Gedcom origin, final Flef destination){
		if(note.getID() == null){
			final String noteID = Flef.getNextNoteID(origin.getNotes().size());
			final String noteValue = note.extractValueConcatenated();

			note.withID(noteID);

			destination.addNote(GedcomNode.create("NOTE")
				.withID(noteID)
				.withValue(noteValue));
		}
	}

	private void sourceCitationTo(final GedcomNode sourceCitation, final Gedcom origin, final Flef destination){
		sourceCitation.withTag("SOURCE");
		if(sourceCitation.getID() != null){
			final GedcomNode page = extractSubStructure(sourceCitation, "PAGE");
			sourceCitation.removeChild(page);
			moveTag("EVENT", sourceCitation, "EVEN");
			final GedcomNode data = extractSubStructure(sourceCitation, "DATA");
			sourceCitation.removeChild(data);
			final GedcomNode dataDate = extractSubStructure(data, "DATE");
			final GedcomNode dataText = extractSubStructure(data, "TEXT");
			dataText.withValue(dataText.extractValueConcatenated());
			sourceCitation.addChild(dataDate);
			sourceCitation.addChild(dataText);
			sourceCitation.addChild(page);
			deleteTag(sourceCitation, "OBJE");
		}
		else{
			final String description = sourceCitation.extractValueConcatenated();
			final GedcomNode text = extractSubStructure(sourceCitation, "TEXT");

			//create a new source record
			final String sourceRecordID = Flef.getNextSourceID(origin.getSources().size());
			final GedcomNode sourceRecord = GedcomNode.create("SOURCE")
				.withID(sourceRecordID)
				.addChild(GedcomNode.create("TITLE")
					.withValue(description))
				.addChild(text);
			final List<GedcomNode> documents = sourceCitation.getChildrenWithTag("OBJE");
			for(final GedcomNode document : documents)
				documentTo(document, origin, destination);
		}
		final List<GedcomNode> notes = sourceCitation.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			noteTo(note, origin, destination);
		moveTag("CREDIBILITY", sourceCitation, "QUAY");
	}

	private void documentTo(final GedcomNode document, final Gedcom origin, final Flef destination){
		//TODO
		if(document.getID() == null){
			final String documentID = Flef.getNextDocumentID(origin.getDocuments().size());
			final GedcomNode title = extractSubStructure(document, "TITL");
			if(!title.isEmpty())
				title.withTag("TITLE");
			final GedcomNode format = extractSubStructure(document, "FORM");
			if(!format.isEmpty())
				format.withTag("FORMAT");
			format.removeChildren();
			final GedcomNode formatMedia = extractSubStructure(document, "FORM", "MEDI");
			final GedcomNode file = extractSubStructure(document, "FILE");
			final GedcomNode cut = extractSubStructure(document, "_CUTD");
			if(!cut.isEmpty()){
				cut.withTag("CUT");
				document.removeChild(cut);
			}
			file.addChild(format);
			if(!formatMedia.isEmpty()){
				formatMedia.withTag("MEDIA");
				file.addChild(formatMedia)
					.addChild(cut);
			}

			document.withID(documentID);
			document.removeChildren();

//			destination.addDocument(GedcomNode.create("DOCUMENT")
//				.withID(documentID))
//				.addChild(title)
//				.addChild(file)
//				.addChild(notes)
//				.addChild(origin)
//				.addChild(individual)
//				.addChild(place)
//				.addChild(submitter)
//				.addChild(restriction)
//			;
		}
	}


	@Override
	public void from(final Flef origin, final Gedcom destination){
		final List<GedcomNode> header = origin.getPeople();

//		header.withTag("HEAD");
//		final GedcomNode source = extractSubStructure(header, "SOURCE");
//		source.withTag("SOUR");
//		moveTag("VERS", source, "VERSION");
//		moveTag("CORP", source, "CORPORATE");
//		final GedcomNode date = extractSubStructure(header, "DATE");
//		if(!date.isEmpty()){
//			source.addChild(GedcomNode.create("DATA")
//				.addChild(date));
//		}
//		final GedcomNode copyright = extractSubStructure(header, "COPYRIGHT");
//		header.removeChild(copyright);
//		moveTag("SUBM", source, "SUBMITTER");
//		deleteTag(header, "PROTOCOL_VERSION");
//		final GedcomNode gedcom = GedcomNode.create("GEDC")
//			.addChild(GedcomNode.create("VERS")
//				.withValue("5.5.1"))
//			.addChild(GedcomNode.create("FORM")
//				.withValue("LINEAGE_LINKED"));
//		final GedcomNode charset = extractSubStructure(header, "CHARSET");
//		charset.withTag("CHAR");
//		header.addChildBefore(gedcom, charset);
//		header.addChildBefore(copyright, charset);
//		final GedcomNode note = extractSubStructure(header, "NOTE");
//		note.withValueConcatenated(note.getValue());
//
//		destination.getPeople()
//			.addAll(people);
	}

}
