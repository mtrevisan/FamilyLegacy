package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.List;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.deleteTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveTag;


public class EventDetailTransformation implements Transformation{

	private static final Transformation PLACE_RECORD_TRANSFORMATION = new PlaceRecordTransformation();
	private static final Transformation NOTE_STRUCTURE_TRANSFORMATION = new NoteStructureTransformation();
	private static final Transformation SOURCE_CITATION_TRANSFORMATION = new SourceCitationTransformation();
	private static final Transformation MULTIMEDIA_LINK_TRANSFORMATION = new MultimediaLinkTransformation();


	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		node.withValue(node.getValueConcatenated());
		deleteTag(node, "CONC", "CONT");
		moveTag("_TYPE", node, "REFN", "TYPE");
		moveTag("_REFN", node, "REFN");
		moveTag("_RIN", node, "RIN");
		final List<GedcomNode> sourceCitations = node.getChildrenWithTag("SOUR");
		for(final GedcomNode sourceCitation : sourceCitations)
			SOURCE_CITATION_TRANSFORMATION.to(sourceCitation, root);
		final GedcomNode changeDate = extractSubStructure(node, "CHAN");
		if(!changeDate.isEmpty())
			CHANGE_DATE_TRANSFORMATION.to(changeDate, root);
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){
		node.withValueConcatenated(node.getValue());
		moveTag("_SUBMITTER", node, "SUBMITTER");
		moveTag("_RESTRICTION", node, "RESTRICTION");
		final List<GedcomNode> sourceCitations = node.getChildrenWithTag("SOURCE");
		for(final GedcomNode sourceCitation : sourceCitations)
			SOURCE_CITATION_TRANSFORMATION.from(sourceCitation, root);
		final GedcomNode changeDate = extractSubStructure(node, "CHANGE");
		if(!changeDate.isEmpty())
			CHANGE_DATE_TRANSFORMATION.from(changeDate, root);
	}

}
