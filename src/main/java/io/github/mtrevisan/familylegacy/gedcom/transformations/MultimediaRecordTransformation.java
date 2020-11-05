package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.List;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.deleteTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveMultipleTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveTag;


public class MultimediaRecordTransformation implements Transformation{

	private static final Transformation NOTE_STRUCTURE_TRANSFORMATION = new NoteStructureTransformation();
	private static final Transformation SOURCE_CITATION_TRANSFORMATION = new SourceCitationTransformation();
	private static final Transformation CHANGE_DATE_TRANSFORMATION = new ChangeDateTransformation();


	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		node.withTag("DOCUMENT");
		final List<GedcomNode> files = node.getChildrenWithTag("FILE");
		for(final GedcomNode file : files){
			final GedcomNode format = moveTag("FORMAT", file, "FORM");
			final GedcomNode type = extractSubStructure(format, "TYPE");
			deleteTag(format, "TYPE");
			file.addChild(type);
			moveTag("TITLE", file, "TITL");
		}
		moveTag("_TYPE", node, "REFN", "TYPE");
		moveMultipleTag("SUBMITTER", node, "REFN");
		moveTag("_RIN", node, "RIN");
		final List<GedcomNode> notes = node.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			NOTE_STRUCTURE_TRANSFORMATION.to(note, root);
		final List<GedcomNode> sourceCitations = node.getChildrenWithTag("SOUR");
		for(final GedcomNode sourceCitation : sourceCitations)
			SOURCE_CITATION_TRANSFORMATION.to(sourceCitation, root);
		final GedcomNode changeDate = extractSubStructure(node, "CHAN");
		if(!changeDate.isEmpty())
			CHANGE_DATE_TRANSFORMATION.to(changeDate, root);
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){
		node.withTag("OBJE");
		moveTag("_TITLE", node, "TITLE");
		final List<GedcomNode> files = node.getChildrenWithTag("FILE");
		for(final GedcomNode file : files){
			final GedcomNode format = moveTag("FORM", file, "FORMAT");
			final GedcomNode type = extractSubStructure(file, "TYPE");
			deleteTag(file, "TYPE");
			format.addChild(type);
			moveTag("TITL", file, "TITLE");
			moveTag("_CUT", file, "CUT");
		}
		moveMultipleTag("REFN", node, "SUBMITTER");
		moveTag("RIN", node, "_RIN");
		final List<GedcomNode> notes = node.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			NOTE_STRUCTURE_TRANSFORMATION.from(note, root);
		final List<GedcomNode> sourceCitations = node.getChildrenWithTag("SOURCE");
		for(final GedcomNode sourceCitation : sourceCitations)
			SOURCE_CITATION_TRANSFORMATION.from(sourceCitation, root);
		moveTag("_RESN", node, "RESTRICTION");
		final GedcomNode changeDate = extractSubStructure(node, "CHANGE");
		if(!changeDate.isEmpty())
			CHANGE_DATE_TRANSFORMATION.from(changeDate, root);
	}

}
