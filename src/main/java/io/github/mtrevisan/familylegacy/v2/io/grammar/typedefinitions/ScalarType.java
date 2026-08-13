package io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions;

import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammar;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.List;
import java.util.regex.Pattern;


/**
 * A reference to another named type (built-in primitive, alias, struct, record, enum, or union) by name.
 */
public final class ScalarType extends TypeDefinition{

	public ScalarType(final String name){
		super(name);
	}


	@Override
	public void validate(final String contextPath, final FLEFRecord record, final FLEFModel model,
		final FLEFGrammar grammar, final List<String> errors){}

	@Override
	public String toString(){
		return getName();
	}

}
