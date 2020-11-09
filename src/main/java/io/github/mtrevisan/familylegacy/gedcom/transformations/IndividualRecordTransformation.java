package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.List;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.deleteMultipleTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.deleteTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveMultipleTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveTag;


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
		moveMultipleTag("_REFN", node, "REFN");
		final List<GedcomNode> associationStructures = node.getChildrenWithTag("ASSO");
		for(final GedcomNode associationStructure : associationStructures)
			ASSOCIATION_STRUCTURE_TRANSFORMATION.to(associationStructure, root);
		moveMultipleTag("ALIAS", node, "ALIA");
		deleteMultipleTag(node, "ANCI");
		deleteMultipleTag(node, "DESI");
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
		moveMultipleTag("_SUBMITTER", node, "SUBMITTER");
		moveMultipleTag("RESN", node, "RESTRICTION");
		final GedcomNode changeDate = extractSubStructure(node, "CHANGE");
		if(!changeDate.isEmpty())
			CHANGE_DATE_TRANSFORMATION.from(changeDate, root);
	}

}
