package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.List;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.deleteMultipleTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveMultipleTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveTag;


public class NoteRecordTransformation implements Transformation{

	private static final Transformation SOURCE_CITATION_TRANSFORMATION = new SourceCitationTransformation();
	private static final Transformation CHANGE_DATE_TRANSFORMATION = new ChangeDateTransformation();


	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		node.withValue(node.extractValueConcatenated());
		deleteMultipleTag(node, "CONC");
		deleteMultipleTag(node, "CONT");
		moveMultipleTag("_REFN", node, "REFN");
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
		moveMultipleTag("_SUBMITTER", node, "SUBMITTER");
		moveTag("_RESTRICTION", node, "RESTRICTION");
		final List<GedcomNode> sourceCitations = node.getChildrenWithTag("SOURCE");
		for(final GedcomNode sourceCitation : sourceCitations)
			SOURCE_CITATION_TRANSFORMATION.from(sourceCitation, root);
		final GedcomNode changeDate = extractSubStructure(node, "CHANGE");
		if(!changeDate.isEmpty())
			CHANGE_DATE_TRANSFORMATION.from(changeDate, root);
	}

}
