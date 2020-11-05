package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveTag;


public class IndividualEventDetailTransformation implements Transformation{

	private static final Transformation EVENT_DETAIL_TRANSFORMATION = new EventDetailTransformation();


	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		moveTag("_AGE", node, "AGE");
		EVENT_DETAIL_TRANSFORMATION.to(node, root);
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){
		EVENT_DETAIL_TRANSFORMATION.from(node, root);
	}

}
