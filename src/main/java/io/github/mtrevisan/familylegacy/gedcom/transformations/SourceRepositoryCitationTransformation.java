package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.List;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveTag;


public class SourceRepositoryCitationTransformation implements Transformation{

	private static final Transformation NOTE_STRUCTURE_TRANSFORMATION = new NoteStructureTransformation();


	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		node.withTag("REPOSITORY");
		if(node.getID() == null){
			//create a repository in the root:
//			node.withID(Flef.getNextRepositoryID(root.getChildrenWithTag("REPOSITORY").size()));
			root.addChild(GedcomNode.create("REPOSITORY")
				.withID(node.getID())
				.withValue(node.extractValueConcatenated()));
			node.removeValue();
		}
		final List<GedcomNode> notes = node.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			NOTE_STRUCTURE_TRANSFORMATION.to(note, root);
		final List<GedcomNode> calns = node.getChildrenWithTag("CALN");
		for(final GedcomNode caln : calns){
			moveTag("_MEDI", caln, "MEDI");
			caln.withTag("_CALN");
		}
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){
		node.withTag("REPO");
		final List<GedcomNode> notes = node.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			NOTE_STRUCTURE_TRANSFORMATION.from(note, root);
		final List<GedcomNode> calns = node.getChildrenWithTag("_CALN");
		for(final GedcomNode caln : calns){
			moveTag("MEDI", caln, "_MEDI");
			caln.withTag("CALN");
		}
	}

}
