package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;


public interface Transformation{

	void to(final GedcomNode root, final Flef flef);

	void from(final GedcomNode root, final Gedcom gedcom);

}
