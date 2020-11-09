package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.List;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveMultipleTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveTag;


public class FamilyRecordTransformation implements Transformation{

	private static final Transformation FAMILY_EVENT_STRUCTURE_TRANSFORMATION = new FamilyEventStructureTransformation();
	private static final Transformation LDS_SPOUSE_SEALING_TRANSFORMATION = new LDSSpouseSealingTransformation();
	private static final Transformation SUBMITTER_RECORD_TRANSFORMATION = new SubmitterRecordTransformation();
	private static final Transformation CHANGE_DATE_TRANSFORMATION = new ChangeDateTransformation();
	private static final Transformation NOTE_STRUCTURE_TRANSFORMATION = new NoteStructureTransformation();
	private static final Transformation SOURCE_CITATION_TRANSFORMATION = new SourceCitationTransformation();
	private static final Transformation MULTIMEDIA_LINK_TRANSFORMATION = new MultimediaLinkTransformation();


	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		node.withTag("FAMILY");
		moveMultipleTag("RESTRICTION", node, "RESN");
		FAMILY_EVENT_STRUCTURE_TRANSFORMATION.to(node, root);
		moveTag("SPOUSE1", node, "HUSB");
		moveTag("SPOUSE2", node, "WIFE");
		moveMultipleTag("CHILD", node, "CHIL");
		//NOTE: should be transformed into a fact
		moveMultipleTag("_NCHI", node, "NCHI");
		final List<GedcomNode> submitters = node.getChildrenWithTag("SLGS");
		for(final GedcomNode submitter : submitters)
			SUBMITTER_RECORD_TRANSFORMATION.to(submitter, root);
		final List<GedcomNode> ldsSpouseSealings = node.getChildrenWithTag("SUBM");
		for(final GedcomNode ldsSpouseSealing : ldsSpouseSealings)
			LDS_SPOUSE_SEALING_TRANSFORMATION.to(ldsSpouseSealing, root);
		moveMultipleTag("_REFN", node, "REFN");
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
//		node.withTag("FAM");
//		final List<GedcomNode> names = node.getChildrenWithTag("NAME");
//		for(final GedcomNode name : names)
//			PERSONAL_NAME_TRANSFORMATION.from(name, root);
//		moveTag("_GENDER", node, "GENDER");
//		moveTag("_SEXUAL_ORIENTATION", node, "SEXUAL_ORIENTATION");
//		final List<GedcomNode> childToFamilyLinks = node.getChildrenWithTag("FAMILY_CHILD");
//		for(final GedcomNode childToFamilyLink : childToFamilyLinks)
//			CHILD_TO_FAMILY_LINK_TRANSFORMATION.from(childToFamilyLink, root);
//		final List<GedcomNode> spouseToFamilyLinks = node.getChildrenWithTag("FAMILY_SPOUSE");
//		for(final GedcomNode spouseToFamilyLink : spouseToFamilyLinks)
//			SPOUSE_TO_FAMILY_LINK_TRANSFORMATION.from(spouseToFamilyLink, root);
//		final List<GedcomNode> associationStructures = node.getChildrenWithTag("ASSOCIATION");
//		for(final GedcomNode associationStructure : associationStructures)
//			ASSOCIATION_STRUCTURE_TRANSFORMATION.from(associationStructure, root);
//		moveMultipleTag("ALIA", node, "ALIAS");
//		INDIVIDUAL_EVENT_STRUCTURE_TRANSFORMATION.from(node, root);
//		INDIVIDUAL_ATTRIBUTE_STRUCTURE_TRANSFORMATION.from(node, root);
//		final List<GedcomNode> notes = node.getChildrenWithTag("NOTE");
//		for(final GedcomNode note : notes)
//			NOTE_STRUCTURE_TRANSFORMATION.from(note, root);
//		final List<GedcomNode> sourceCitations = node.getChildrenWithTag("SOURCE");
//		for(final GedcomNode sourceCitation : sourceCitations)
//			SOURCE_CITATION_TRANSFORMATION.from(sourceCitation, root);
//		final List<GedcomNode> multimediaLinks = node.getChildrenWithTag("DOCUMENT");
//		for(final GedcomNode multimediaLink : multimediaLinks)
//			MULTIMEDIA_LINK_TRANSFORMATION.from(multimediaLink, root);
//		moveMultipleTag("_SUBMITTER", node, "SUBMITTER");
//		moveMultipleTag("RESN", node, "RESTRICTION");
//		final GedcomNode changeDate = extractSubStructure(node, "CHANGE");
//		if(!changeDate.isEmpty())
//			CHANGE_DATE_TRANSFORMATION.from(changeDate, root);
	}

}
