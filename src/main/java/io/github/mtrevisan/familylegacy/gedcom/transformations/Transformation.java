package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;


public interface Transformation{

	void to(final GedcomNode node, final GedcomNode root);

	void from(final GedcomNode node, final GedcomNode root);

}
