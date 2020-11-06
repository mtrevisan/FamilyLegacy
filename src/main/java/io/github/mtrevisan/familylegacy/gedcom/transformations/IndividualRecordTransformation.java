package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.List;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.deleteTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveMultipleTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveTag;


//TODO
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
*/
public class IndividualRecordTransformation implements Transformation{

	private static final Transformation PERSONAL_NAME_TRANSFORMATION = new PersonalNameStructureTransformation();
	private static final Transformation INDIVIDUAL_EVENT_STRUCTURE_TRANSFORMATION = new IndividualEventStructureTransformation();
	private static final Transformation INDIVIDUAL_ATTRIBUTE_STRUCTURE_TRANSFORMATION = new IndividualAttributeStructureTransformation();
	private static final Transformation LDS_INDIVIDUAL_ORDINANCE_TRANSFORMATION = new LDSIndividualOrdinanceTransformation();
	private static final Transformation CHILD_TO_FAMILY_LINK_TRANSFORMATION = new ChildToFamilyLinkTransformation();
	private static final Transformation SPOUSE_TO_FAMILY_LINK_TRANSFORMATION = new SpouseToFamilyLinkTransformation();
	private static final Transformation ASSOCIATION_STRUCTURE_TRANSFORMATION = new AssociationStructureTransformation();
	private static final Transformation CHANGE_DATE_TRANSFORMATION = new ChangeDateTransformation();
	private static final Transformation NOTE_STRUCTURE_TRANSFORMATION = new NoteStructureTransformation();
	private static final Transformation SOURCE_CITATION_TRANSFORMATION = new SourceCitationTransformation();
	private static final Transformation MULTIMEDIA_LINK_TRANSFORMATION = new MultimediaLinkTransformation();


	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		node.withTag("INDIVIDUAL");
		moveMultipleTag("RESTRICTION", node, "RESN");
		final List<GedcomNode> names = node.getChildrenWithTag("NAME");
		for(final GedcomNode name : names)
			PERSONAL_NAME_TRANSFORMATION.to(name, root);
		INDIVIDUAL_EVENT_STRUCTURE_TRANSFORMATION.to(node, root);
		INDIVIDUAL_ATTRIBUTE_STRUCTURE_TRANSFORMATION.to(node, root);
		LDS_INDIVIDUAL_ORDINANCE_TRANSFORMATION.to(node, root);
		final List<GedcomNode> childToFamilyLinks = node.getChildrenWithTag("FAMC");
		for(final GedcomNode childToFamilyLink : childToFamilyLinks)
			CHILD_TO_FAMILY_LINK_TRANSFORMATION.to(childToFamilyLink, root);
		final List<GedcomNode> spouseToFamilyLinks = node.getChildrenWithTag("FAMS");
		for(final GedcomNode spouseToFamilyLink : spouseToFamilyLinks)
			SPOUSE_TO_FAMILY_LINK_TRANSFORMATION.to(spouseToFamilyLink, root);
		moveMultipleTag("SUBMITTER", node, "REFN");
		final List<GedcomNode> associationStructures = node.getChildrenWithTag("ASSO");
		for(final GedcomNode associationStructure : associationStructures)
			ASSOCIATION_STRUCTURE_TRANSFORMATION.to(associationStructure, root);
		moveMultipleTag("ALIAS", node, "ALIA");
		deleteTag(node, "ANCI");
		deleteTag(node, "DESI");
		deleteTag(node, "RFN");
		deleteTag(node, "AFN");
		moveTag("_RIN", node, "RIN");
		final GedcomNode changeDate = extractSubStructure(node, "CHAN");
		if(!changeDate.isEmpty())
			CHANGE_DATE_TRANSFORMATION.to(changeDate, root);
		final List<GedcomNode> notes = node.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			NOTE_STRUCTURE_TRANSFORMATION.to(note, root);
		final List<GedcomNode> sourceCitations = node.getChildrenWithTag("SOUR");
		for(final GedcomNode sourceCitation : sourceCitations)
			SOURCE_CITATION_TRANSFORMATION.to(sourceCitation, root);
		final List<GedcomNode> multimediaLinks = node.getChildrenWithTag("OBJE");
		for(final GedcomNode multimediaLink : multimediaLinks)
			MULTIMEDIA_LINK_TRANSFORMATION.to(multimediaLink, root);
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){
		node.withTag("INDI");
		final List<GedcomNode> names = node.getChildrenWithTag("NAME");
		for(final GedcomNode name : names)
			PERSONAL_NAME_TRANSFORMATION.from(name, root);
		moveTag("_GENDER", node, "GENDER");
		moveTag("_SEXUAL_ORIENTATION", node, "SEXUAL_ORIENTATION");
		final List<GedcomNode> childToFamilyLinks = node.getChildrenWithTag("FAMILY_CHILD");
		for(final GedcomNode childToFamilyLink : childToFamilyLinks)
			CHILD_TO_FAMILY_LINK_TRANSFORMATION.from(childToFamilyLink, root);
		final List<GedcomNode> spouseToFamilyLinks = node.getChildrenWithTag("FAMILY_SPOUSE");
		for(final GedcomNode spouseToFamilyLink : spouseToFamilyLinks)
			SPOUSE_TO_FAMILY_LINK_TRANSFORMATION.from(spouseToFamilyLink, root);
		final List<GedcomNode> associationStructures = node.getChildrenWithTag("ASSOCIATION");
		for(final GedcomNode associationStructure : associationStructures)
			ASSOCIATION_STRUCTURE_TRANSFORMATION.from(associationStructure, root);
		moveMultipleTag("ALIA", node, "ALIAS");
		INDIVIDUAL_EVENT_STRUCTURE_TRANSFORMATION.from(node, root);
		INDIVIDUAL_ATTRIBUTE_STRUCTURE_TRANSFORMATION.from(node, root);
		final List<GedcomNode> notes = node.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			NOTE_STRUCTURE_TRANSFORMATION.from(note, root);
		final List<GedcomNode> sourceCitations = node.getChildrenWithTag("SOURCE");
		for(final GedcomNode sourceCitation : sourceCitations)
			SOURCE_CITATION_TRANSFORMATION.from(sourceCitation, root);
		final List<GedcomNode> multimediaLinks = node.getChildrenWithTag("DOCUMENT");
		for(final GedcomNode multimediaLink : multimediaLinks)
			MULTIMEDIA_LINK_TRANSFORMATION.from(multimediaLink, root);
		moveMultipleTag("REFN", node, "SUBMITTER");
		moveMultipleTag("RESN", node, "RESTRICTION");
		final GedcomNode changeDate = extractSubStructure(node, "CHANGE");
		if(!changeDate.isEmpty())
			CHANGE_DATE_TRANSFORMATION.from(changeDate, root);
	}

}
