package io.github.mtrevisan.familylegacy.gedcom.transformations2;

import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.Store;


public interface Transformation<FROM extends Store<FROM>, TO extends Store<TO>>{

	void to(final FROM origin, final TO destination);

	void from(final TO origin, final FROM destination) throws GedcomGrammarParseException;

}
