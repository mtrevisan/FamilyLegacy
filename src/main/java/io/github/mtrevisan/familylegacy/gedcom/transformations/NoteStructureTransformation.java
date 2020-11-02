package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.transferNoteTo;


public class NoteStructureTransformation implements Transformation{

	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		transferNoteTo(node, root);
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){}

}
