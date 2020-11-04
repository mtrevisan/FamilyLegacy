package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;


public class PlaceRecordTransformation implements Transformation{

	private static final Transformation PLACE_ADDRESS_STRUCTURE_TRANSFORMATION = new PlaceAddressStructureTransformation();


	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		PLACE_ADDRESS_STRUCTURE_TRANSFORMATION.to(node, root);

		//create place record:
		final GedcomNode address = extractSubStructure(node, "ADDRESS");
		final String placeID = Flef.getNextPlaceID(root.getChildrenWithTag("PLACE").size());
		node.removeChild(address)
			//write reference to place
			.addChild(GedcomNode.create("PLACE")
				.withID(placeID));
		//add place to root
		root.addChild(GedcomNode.create("PLACE")
			.withID(placeID)
			.addChild(address));
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){
		//read place record
		final GedcomNode place = root.getChildWithIDAndTag(node.getID(), "PLACE");

		//TODO add ADDRESS

		PLACE_ADDRESS_STRUCTURE_TRANSFORMATION.from(node, root);
	}

}
