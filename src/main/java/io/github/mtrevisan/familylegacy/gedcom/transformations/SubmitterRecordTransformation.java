package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.List;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveMultipleTag;


public class SubmitterRecordTransformation implements Transformation{

	private static final Transformation PLACE_RECORD_TRANSFORMATION = new PlaceRecordTransformation();
	private static final Transformation MULTIMEDIA_LINK_TRANSFORMATION = new MultimediaLinkTransformation();
	private static final Transformation NOTE_STRUCTURE_TRANSFORMATION = new NoteStructureTransformation();
	private static final Transformation CHANGE_DATE_TRANSFORMATION = new ChangeDateTransformation();


	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		node.withTag("SUBMITTER");
		PLACE_RECORD_TRANSFORMATION.to(node, root);
		final List<GedcomNode> multimediaLinks = node.getChildrenWithTag("OBJE");
		for(final GedcomNode multimediaLink : multimediaLinks)
			MULTIMEDIA_LINK_TRANSFORMATION.to(multimediaLink, root);
		moveMultipleTag("LANGUAGE", node, "LANG");
		moveMultipleTag("_RFN", node, "RFN");
		moveMultipleTag("_RIN", node, "RIN");
		final List<GedcomNode> notes = node.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			NOTE_STRUCTURE_TRANSFORMATION.to(note, root);
		final GedcomNode changeDate = extractSubStructure(node, "CHAN");
		if(!changeDate.isEmpty())
			CHANGE_DATE_TRANSFORMATION.to(changeDate, root);
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){
		node.withTag("SUBM");
		PLACE_RECORD_TRANSFORMATION.from(node, root);
		final List<GedcomNode> multimediaLinks = node.getChildrenWithTag("OBJE");
		for(final GedcomNode multimediaLink : multimediaLinks)
			MULTIMEDIA_LINK_TRANSFORMATION.from(multimediaLink, root);
		final List<GedcomNode> languages = moveMultipleTag("LANG", node, "LANGUAGE");
		for(int i = 3; i < languages.size(); i ++)
			languages.get(i).withTag("_LANGUAGE");
		//keep language bounded to at most three
		moveMultipleTag("_INDIVIDUAL", node, "INDIVIDUAL");
		final List<GedcomNode> notes = node.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			NOTE_STRUCTURE_TRANSFORMATION.from(note, root);
		final GedcomNode changeDate = extractSubStructure(node, "CHAN");
		if(!changeDate.isEmpty())
			CHANGE_DATE_TRANSFORMATION.from(changeDate, root);
	}

}
