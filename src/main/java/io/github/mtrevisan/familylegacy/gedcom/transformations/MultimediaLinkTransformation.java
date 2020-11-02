package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.List;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.deleteTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveTag;


public class MultimediaLinkTransformation implements Transformation{


	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		node.withTag("DOCUMENT");
		if(node.getID() == null){
			final GedcomNode title = extractSubStructure(node, "TITL");
			deleteTag(node, "TITL");
			title.withTag("TITLE");
			final GedcomNode format = extractSubStructure(node, "FORM");
			deleteTag(node, "FORM");
			format.withTag("FORMAT");
			moveTag("_MEDI", format, "MEDI");
			final List<GedcomNode> docFiles = node.getChildrenWithTag("FILE");
			deleteTag(node, "FILE");
			node.withID(Flef.getNextDocumentID(root.getChildrenWithTag("DOCUMENT").size()));
			final GedcomNode doc = GedcomNode.create("DOCUMENT")
				.withID(node.getID())
				.addChild(title);
			for(final GedcomNode docFile : docFiles)
				doc.addChild(docFile
					.addChild(format));
			root.addChild(doc, 1);
		}
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){
		node.withTag("OBJE");
		moveTag("_TITLE", node, "TITLE");
		final List<GedcomNode> files = node.getChildrenWithTag("FILE");
		for(final GedcomNode file : files){
			final GedcomNode format = moveTag("FORM", file, "FORMAT");
			final GedcomNode type = extractSubStructure(file, "TYPE");
			deleteTag(file, "TYPE");
			format.addChild(type);
			moveTag("TITL", file, "TITLE");
			moveTag("_CUT", file, "CUT");
		}
		moveTag("REFN", node, "SUBMITTER");
		moveTag("RIN", node, "_RIN");
	}

}
