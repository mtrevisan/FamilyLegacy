package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.List;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveTag;


public class RepositoryRecordTransformation implements Transformation{

	private static final Transformation PLACE_RECORD_TRANSFORMATION = new PlaceRecordTransformation();
	private static final Transformation NOTE_STRUCTURE_TRANSFORMATION = new NoteStructureTransformation();
	private static final Transformation CHANGE_DATE_TRANSFORMATION = new ChangeDateTransformation();


	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		node.withTag("REPOSITORY");
		PLACE_RECORD_TRANSFORMATION.to(node, root);
		final List<GedcomNode> notes = node.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			NOTE_STRUCTURE_TRANSFORMATION.to(note, root);
		moveTag("_TYPE", node, "REFN", "TYPE");
		moveTag("_REFN", node, "REFN");
		moveTag("_RIN", node, "RIN");
		final GedcomNode changeDate = extractSubStructure(node, "CHAN");
		if(!changeDate.isEmpty())
			CHANGE_DATE_TRANSFORMATION.to(changeDate, root);
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){
		node.withTag("REPO");
		PLACE_RECORD_TRANSFORMATION.from(node, root);
		final List<GedcomNode> notes = node.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			NOTE_STRUCTURE_TRANSFORMATION.from(note, root);
		moveTag("_SUBMITTER", node, "SUBMITTER");
		moveTag("_RESTRICTION", node, "RESTRICTION");
		final GedcomNode changeDate = extractSubStructure(node, "CHANGE");
		if(!changeDate.isEmpty())
			CHANGE_DATE_TRANSFORMATION.from(changeDate, root);
	}

}
