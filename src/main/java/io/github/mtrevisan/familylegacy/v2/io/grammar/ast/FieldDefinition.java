package io.github.mtrevisan.familylegacy.v2.io.grammar.ast;


public record FieldDefinition(String name, TypeDefinition type, Cardinality cardinality){

	@Override
	public String toString(){
		return name + cardinality.symbol() + ": " + type;
	}

}
