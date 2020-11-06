package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.deleteTag;


public class SubmissionRecordTransformation implements Transformation{

	@Override
	public void to(final GedcomNode parent, final GedcomNode root){
		deleteTag(parent, "SUBN");
	}

	@Override
	public void from(final GedcomNode parent, final GedcomNode root){}

}
