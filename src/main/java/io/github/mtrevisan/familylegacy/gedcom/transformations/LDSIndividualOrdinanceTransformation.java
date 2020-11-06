package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.List;


public class LDSIndividualOrdinanceTransformation implements Transformation{

	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		final List<GedcomNode> children = node.getChildrenWithTag("BAPL", "CONL", "ENDL", "SLGC");
		for(final GedcomNode child : children)
			node.removeChild(child);
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){}

}
