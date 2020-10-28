package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveTag;


public class EndOfFileTransformation implements Transformation{

	@Override
	public void to(final GedcomNode root){
		moveTag("EOF", root, "TRLR");
	}

	@Override
	public void from(final GedcomNode root){
		moveTag("TRLR", root, "EOF");
	}

}
