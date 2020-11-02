package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.List;


public class SpouseToFamilyLinkTransformation implements Transformation{

	private static final Transformation NOTE_STRUCTURE_TRANSFORMATION = new NoteStructureTransformation();


	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		node.withTag("FAMILY_SPOUSE");
		final List<GedcomNode> notes = node.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			NOTE_STRUCTURE_TRANSFORMATION.to(note, root);
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){
		node.withTag("FAMS");
		final List<GedcomNode> notes = node.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			NOTE_STRUCTURE_TRANSFORMATION.from(note, root);
	}

}
