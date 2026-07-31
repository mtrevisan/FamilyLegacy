package io.github.mtrevisan.familylegacy.v2.io.grammar.ast;


/**
 * A reference to another named type (built-in primitive, alias, struct, record, enum, or union) by name.
 */
public final class ScalarType extends TypeDefinition{

	public ScalarType(final String name){
		super(name);
	}


	@Override
	public String toString(){
		return getName();
	}

}
