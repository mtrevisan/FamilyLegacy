package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;


public class EndOfFileTransformation implements Transformation{

	@Override
	public void to(final GedcomNode root, final Flef flef){
		TransformationHelper.moveTag("EOF", root, "TRLR");
	}

	@Override
	public void from(final GedcomNode root){
		TransformationHelper.moveTag("TRLR", root, "EOF");
	}

}
