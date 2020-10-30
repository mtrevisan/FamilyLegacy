package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveTag;


public class EndOfFileTransformation implements Transformation{

	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		node.withTag("EOF");
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){
		node.withTag("TRLR");
	}

}
