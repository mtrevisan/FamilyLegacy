package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.List;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveTag;


public class PlaceStructureTransformation implements Transformation{

	private static final Transformation NOTE_STRUCTURE_TRANSFORMATION = new NoteStructureTransformation();


	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		moveTag("PLACE_NAME", node, "FORM");
		final GedcomNode phoneticNode = extractSubStructure(node, "FONE");
		if(!phoneticNode.isEmpty()){
			phoneticNode.withTag("_FONE");
			moveTag("_TYPE", phoneticNode, "TYPE");
		}
		final GedcomNode romanizedNode = extractSubStructure(node, "ROMN");
		if(!romanizedNode.isEmpty()){
			romanizedNode.withTag("_ROMN");
			moveTag("_TYPE", romanizedNode, "TYPE");
		}
		final GedcomNode mapNode = extractSubStructure(node, "MAP");
		if(!mapNode.isEmpty()){
			moveTag("LATITUDE", mapNode, "LATI");
			moveTag("LONGITUDE", mapNode, "LONG");
		}
		final List<GedcomNode> notes = node.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			NOTE_STRUCTURE_TRANSFORMATION.to(note, root);

		/** Add current node to a temporary variable (this will be handled by a subsequent {@link AddressStructureTransformation}). */
		root.addChild(GedcomNode.create("!PLACE_STRUCTURE")
			.addChild(node));
		//TODO remove current node
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){
		final GedcomNode temporaryNode = extractSubStructure(root, "!PLACE_STRUCTURE");

		moveTag("FORM", node, "PLACE_NAME")
			.withValue(extractSubStructure(temporaryNode, "PLACE_NAME")
				.getValue());
		final GedcomNode phoneticNode = extractSubStructure(node, "_FONE");
		if(!phoneticNode.isEmpty()){
			phoneticNode.withTag("FONE");
			moveTag("TYPE", phoneticNode, "_TYPE");
		}
		final GedcomNode romanizedNode = extractSubStructure(node, "_ROMN");
		if(!romanizedNode.isEmpty()){
			romanizedNode.withTag("ROMN");
			moveTag("TYPE", romanizedNode, "_TYPE");
		}
		final GedcomNode temporaryMapNode = extractSubStructure(temporaryNode, "MAP");
		final GedcomNode mapNode = extractSubStructure(node, "MAP");
		if(!temporaryMapNode.isEmpty())
			node.addChild(GedcomNode.create("MAP")
				.withChildren(mapNode.getChildren()));
		final List<GedcomNode> notes = node.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			NOTE_STRUCTURE_TRANSFORMATION.from(note, root);

		root.removeChild(temporaryNode);
	}

}
