package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.List;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveTag;


public class AssociationStructureTransformation implements Transformation{

	private static final Transformation SOURCE_CITATION_TRANSFORMATION = new SourceCitationTransformation();
	private static final Transformation NOTE_STRUCTURE_TRANSFORMATION = new NoteStructureTransformation();


	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		node.withTag("ASSOCIATION")
			.addChild(GedcomNode.create("TYPE")
				.withValue("INDIVIDUAL"));
		moveTag("RELATIONSHIP", node, "RELA");
		final List<GedcomNode> sourceCitations = node.getChildrenWithTag("SOUR");
		for(final GedcomNode sourceCitation : sourceCitations)
			SOURCE_CITATION_TRANSFORMATION.to(sourceCitation, root);
		final List<GedcomNode> notes = node.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			NOTE_STRUCTURE_TRANSFORMATION.to(note, root);
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){
		final GedcomNode type = moveTag("_TYPE", node, "TYPE");
		final boolean isIndividual = "INDIVIDUAL".equals(type.getValue());
		if(isIndividual){
			node.withTag("ASSO");
			node.removeChild(type);
			moveTag("RELA", node, "RELATIONSHIP");
			final List<GedcomNode> sourceCitations = node.getChildrenWithTag("SOURCE");
			for(final GedcomNode sourceCitation : sourceCitations)
				SOURCE_CITATION_TRANSFORMATION.from(sourceCitation, root);
			final List<GedcomNode> notes = node.getChildrenWithTag("NOTE");
			for(final GedcomNode note : notes)
				NOTE_STRUCTURE_TRANSFORMATION.from(note, root);
		}
		else{
			node.withTag(isIndividual? "ASSO": "_ASSOCIATION");
			moveTag("_TYPE", node, "TYPE");
			moveTag("_RELATIONSHIP", node, "RELATIONSHIP");
			moveTag("_SOURCE", node, "SOURCE");
			moveTag("_NOTE", node, "NOTE");
		}
	}

}
