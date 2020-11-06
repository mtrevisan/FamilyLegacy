package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.List;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.deleteTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveMultipleTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveTag;


/*
left to do

LINEAGE_LINKED_GEDCOM :=
n <<HEADER>>    {1:1}
n <<SUBMISSION_RECORD>>    {0:1}
n <<RECORD>>    {1:M}
n <<END_OF_FILE>>    {1:1}


RECORD :=
[
n <<FAMILY_RECORD>>    {1:1}
|
n <<INDIVIDUAL_RECORD>>    {1:1}
|
n <<MULTIMEDIA_RECORD>>    {1:1}
|
n <<NOTE_RECORD>>    {1:1}
|
n <<REPOSITORY_RECORD>>    {1:1}
|
n <<SOURCE_RECORD>>    {1:1}
|
n <<SUBMITTER_RECORD>>    {1:1}
]


FAMILY_RECORD :=
n @<XREF:FAM>@ FAM    {1:1}
  +1 RESN <RESTRICTION_NOTICE>    {0:1}
	+1 <<FAMILY_EVENT_STRUCTURE>>    {0:M}
	+1 HUSB @<XREF:INDI>@    {0:1}
	+1 WIFE @<XREF:INDI>@    {0:1}
	+1 CHIL @<XREF:INDI>@    {0:M}
	+1 NCHI <COUNT_OF_CHILDREN>    {0:1}
	+1 SUBM @<XREF:SUBM>@    {0:M}
	+1 <<LDS_SPOUSE_SEALING>>    {0:M}
	+1 REFN <USER_REFERENCE_NUMBER>    {0:M}
	+2 TYPE <USER_REFERENCE_TYPE>    {0:1}
	+1 RIN <AUTOMATED_RECORD_ID>    {0:1}
	+1 <<CHANGE_DATE>>    {0:1}
	+1 <<NOTE_STRUCTURE>>    {0:M}
	+1 <<SOURCE_CITATION>>    {0:M}
	+1 <<MULTIMEDIA_LINK>>    {0:M}


	SOURCE_RECORD :=
	n @<XREF:SOUR>@ SOUR    {1:1}
	+1 DATA    {0:1}
	+2 EVEN <EVENTS_RECORDED>    {0:M}
	+3 DATE <DATE_PERIOD>    {0:1}
	+3 PLAC <SOURCE_JURISDICTION_PLACE>    {0:1}
	+2 AGNC <RESPONSIBLE_AGENCY>    {0:1}
	+2 <<NOTE_STRUCTURE>>    {0:M}
	+1 AUTH <SOURCE_ORIGINATOR>    {0:1}
	+2 [CONC|CONT] <SOURCE_ORIGINATOR>    {0:M}
	+1 TITL <SOURCE_DESCRIPTIVE_TITLE>    {0:1}
	+2 [CONC|CONT] <SOURCE_DESCRIPTIVE_TITLE>    {0:M}
	+1 ABBR <SOURCE_FILED_BY_ENTRY>    {0:1}
	+1 PUBL <SOURCE_PUBLICATION_FACTS>    {0:1}
	+2 [CONC|CONT] <SOURCE_PUBLICATION_FACTS>    {0:M}
	+1 TEXT <TEXT_FROM_SOURCE>    {0:1}
	+2 [CONC|CONT] <TEXT_FROM_SOURCE>    {0:M}
	+1 <<SOURCE_REPOSITORY_CITATION>>    {0:M}
	+1 REFN <USER_REFERENCE_NUMBER>    {0:M}
	+2 TYPE <USER_REFERENCE_TYPE>    {0:1}
	+1 RIN <AUTOMATED_RECORD_ID>    {0:1}
	+1 <<CHANGE_DATE>>    {0:1}
	+1 <<NOTE_STRUCTURE>>    {0:M}
	+1 <<MULTIMEDIA_LINK>>    {0:M}


	SUBMISSION_RECORD :=
	n @<XREF:SUBN>@ SUBN    {1:1}
	+1 SUBM @<XREF:SUBM>@    {0:1}
	+1 FAMF <NAME_OF_FAMILY_FILE>    {0:1}
	+1 TEMP <TEMPLE_CODE>    {0:1}
	+1 ANCE <GENERATIONS_OF_ANCESTORS>    {0:1}
	+1 DESC <GENERATIONS_OF_DESCENDANTS>    {0:1}
	+1 ORDI <ORDINANCE_PROCESS_FLAG>    {0:1}
	+1 RIN <AUTOMATED_RECORD_ID>    {0:1}
	+1 <<NOTE_STRUCTURE>>    {0:M}
	+1 <<CHANGE_DATE>>    {0:1}


	INDIVIDUAL_ATTRIBUTE_STRUCTURE :=
	[
	n CAST <CASTE_NAME>    {1:1}
	+1 <<INDIVIDUAL_EVENT_DETAIL>>    {0:1}
	|
	n DSCR <PHYSICAL_DESCRIPTION>    {1:1}
	+1 [CONC | CONT ] <PHYSICAL_DESCRIPTION>    {0:M}
	+1 <<INDIVIDUAL_EVENT_DETAIL>>    {0:1}
	|
	n EDUC <SCHOLASTIC_ACHIEVEMENT>    {1:1}
	+1 <<INDIVIDUAL_EVENT_DETAIL>>    {0:1}
	|
	n IDNO <NATIONAL_ID_NUMBER>    {1:1}
	+1 <<INDIVIDUAL_EVENT_DETAIL>>    {0:1}
	|
	n NATI <NATIONAL_OR_TRIBAL_ORIGIN>    {1:1}
	+1 <<INDIVIDUAL_EVENT_DETAIL>>    {0:1}
	|
	n NCHI <COUNT_OF_CHILDREN>    {1:1}
	+1 <<INDIVIDUAL_EVENT_DETAIL>>    {0:1}
	|
	n NMR <COUNT_OF_MARRIAGES>    {1:1}
	+1 <<INDIVIDUAL_EVENT_DETAIL>>    {0:1}
	|
	n OCCU <OCCUPATION>    {1:1}
	+1 <<INDIVIDUAL_EVENT_DETAIL>>    {0:1}
	|
	n PROP <POSSESSIONS>    {1:1}
	+1 <<INDIVIDUAL_EVENT_DETAIL>>    {0:1}
	|
	n RELI <RELIGIOUS_AFFILIATION>    {1:1}
	+1 <<INDIVIDUAL_EVENT_DETAIL>>    {0:1}
	|
	n RESI {1:1}
	+1 <<INDIVIDUAL_EVENT_DETAIL>>    {0:1}
	|
	n SSN <SOCIAL_SECURITY_NUMBER>    {1:1}
	+1 <<INDIVIDUAL_EVENT_DETAIL>>    {0:1}
	|
	n TITL <NOBILITY_TYPE_TITLE>    {1:1}
	+1 <<INDIVIDUAL_EVENT_DETAIL>>    {0:1}
	|
	n FACT <ATTRIBUTE_DESCRIPTOR>    {1:1}
	+1 <<INDIVIDUAL_EVENT_DETAIL>>    {0:1}
	]


	LDS_INDIVIDUAL_ORDINANCE :=
	[
	n [ BAPL | CONL ]    {1:1}
	+1 DATE <DATE_LDS_ORD>    {0:1}
	+1 TEMP <TEMPLE_CODE>    {0:1}
	+1 PLAC <PLACE_LIVING_ORDINANCE>    {0:1}
	+1 STAT <LDS_BAPTISM_DATE_STATUS>    {0:1}
	+2 DATE <CHANGE_DATE>    {1:1}
	+1 <<NOTE_STRUCTURE>>    {0:M}
	+1 <<SOURCE_CITATION>>    {0:M}
	|
	n ENDL    {1:1}
	+1 DATE <DATE_LDS_ORD>    {0:1}
	+1 TEMP <TEMPLE_CODE>    {0:1}
	+1 PLAC <PLACE_LIVING_ORDINANCE>    {0:1}
	+1 STAT <LDS_ENDOWMENT_DATE_STATUS>    {0:1}
	+2 DATE <CHANGE_DATE>    {1:1}
	+1 <<NOTE_STRUCTURE>>    {0:M}
	+1 <<SOURCE_CITATION>>    {0:M}
	|
	n SLGC    {1:1}
	+1 DATE <DATE_LDS_ORD>    {0:1}
	+1 TEMP <TEMPLE_CODE>    {0:1}
	+1 PLAC <PLACE_LIVING_ORDINANCE>    {0:1}
	+1 FAMC @<XREF:FAM>@    {1:1}
	+1 STAT <LDS_CHILD_SEALING_DATE_STATUS>    {0:1}
	+2 DATE <CHANGE_DATE>    {1:1}
	+1 <<NOTE_STRUCTURE>>    {0:M}
	+1 <<SOURCE_CITATION>>    {0:M}
	]


	LDS_SPOUSE_SEALING :=
	n SLGS    {1:1}
	+1 DATE <DATE_LDS_ORD>    {0:1}
	+1 TEMP <TEMPLE_CODE>    {0:1}
	+1 PLAC <PLACE_LIVING_ORDINANCE>    {0:1}
	+1 STAT <LDS_SPOUSE_SEALING_DATE_STATUS>    {0:1}
	+2 DATE <CHANGE_DATE>    {1:1}
	+1 <<NOTE_STRUCTURE>>    {0:M}
	+1 <<SOURCE_CITATION>>    {0:M}
*/
//TODO
public class IndividualRecordTransformation implements Transformation{

	private static final Transformation PERSONAL_NAME_TRANSFORMATION = new PersonalNameStructureTransformation();
	private static final Transformation CHILD_TO_FAMILY_LINK_TRANSFORMATION = new ChildToFamilyLinkTransformation();
	private static final Transformation SPOUSE_TO_FAMILY_LINK_TRANSFORMATION = new SpouseToFamilyLinkTransformation();
	private static final Transformation ASSOCIATION_STRUCTURE_TRANSFORMATION = new AssociationStructureTransformation();
	private static final Transformation NOTE_STRUCTURE_TRANSFORMATION = new NoteStructureTransformation();
	private static final Transformation INDIVIDUAL_EVENT_STRUCTURE_TRANSFORMATION = new IndividualEventStructureTransformation();


	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		final GedcomNode person = moveTag("INDIVIDUAL", root, "INDI");
		moveMultipleTag("RESTRICTION", person, "RESN");
		moveMultipleTag("ALIAS", person, "ALIA");
		moveMultipleTag("SUBMITTER", person, "SUBM");
		deleteTag(person, "ANCI");
		deleteTag(person, "DESI");
		deleteTag(person, "RFN");
		deleteTag(person, "AFN");
		deleteTag(person, "REFN");
		deleteTag(person, "RIN");
		deleteTag(person, "BAPL");
		deleteTag(person, "CONL");
		deleteTag(person, "ENDL");
		deleteTag(person, "SLGC");
		final List<GedcomNode> names = person.getChildrenWithTag("NAME");
		for(final GedcomNode name : names)
			PERSONAL_NAME_TRANSFORMATION.to(name, root);
		final List<GedcomNode> childToFamilyLinks = person.getChildrenWithTag("FAMC");
		for(final GedcomNode childToFamilyLink : childToFamilyLinks)
			CHILD_TO_FAMILY_LINK_TRANSFORMATION.to(childToFamilyLink, root);
		final List<GedcomNode> spouseToFamilyLinks = person.getChildrenWithTag("FAMS");
		for(final GedcomNode spouseToFamilyLink : spouseToFamilyLinks)
			SPOUSE_TO_FAMILY_LINK_TRANSFORMATION.to(spouseToFamilyLink, root);
		final List<GedcomNode> associationStructures = person.getChildrenWithTag("ASSO");
		for(final GedcomNode associationStructure : associationStructures)
			ASSOCIATION_STRUCTURE_TRANSFORMATION.to(associationStructure, root);
		final List<GedcomNode> notes = node.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			NOTE_STRUCTURE_TRANSFORMATION.to(note, root);
		INDIVIDUAL_EVENT_STRUCTURE_TRANSFORMATION.to(node, root);

		//TODO
/*
		+1 <<INDIVIDUAL_ATTRIBUTE_STRUCTURE>>    {0:M}	+1 <<INDIVIDUAL_ATTRIBUTE_STRUCTURE>>    {0:M}
		+1 <<SOURCE_CITATION>>    {0:M}						+1 SOURCE @<XREF:SOURCE>@    {0:M}
		+1 <<MULTIMEDIA_LINK>>    {0:M}						+1 DOCUMENT @<XREF:DOCUMENT>@    {0:M}
		+2 TYPE <USER_REFERENCE_TYPE>    {0:1}
		+1 <<CHANGE_DATE>>    {0:1}							+1 <<CHANGE_DATE>>    {0:1}
*/
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){
		final GedcomNode person = moveTag("INDI", root, "INDIVIDUAL");
		deleteTag(person, "GENDER");
		deleteTag(person, "SEXUAL_ORIENTATION");



//		final GedcomNode header = moveTag("HEAD", root, "HEADER");
//		final GedcomNode headerSource = moveMultipleTag("SOUR", header, "SOURCE");
//		moveMultipleTag("VERS", headerSource, "VERSION");
//		final GedcomNode headerCorporate = moveMultipleTag("CORP", headerSource, "CORPORATE");
//		final GedcomNode sourceCorporatePlace = extractSubStructure(headerCorporate, "PLACE");
//		if(!sourceCorporatePlace.isEmpty()){
//			GedcomNode place = null;
//			final List<GedcomNode> places = root.getChildrenWithTag("PLACE");
//			for(final GedcomNode p : places)
//				if(p.getID().equals(sourceCorporatePlace.getID())){
//					place = p;
//					break;
//				}
//			if(place == null)
//				throw new IllegalArgumentException("Cannot find place with ID " + sourceCorporatePlace.getID());
//
//			place.removeID();
//			deleteTag(headerCorporate, "PLACE");
//			headerCorporate.addChild(place);
//		}
//		moveMultipleTag("SUBM", header, "SUBMITTER");
//		moveMultipleTag("COPR", header, "COPYRIGHT");
//		deleteTag(header, "PROTOCOL_VERSION");
//		header.addChild(GedcomNode.create("GEDC")
//			.addChild(GedcomNode.create("VERS")
//				.withValue("5.5.1")));
//		transferValues(header, "CHARSET", header, "CHAR");
//		splitNote(header, "NOTE");
	}

}
