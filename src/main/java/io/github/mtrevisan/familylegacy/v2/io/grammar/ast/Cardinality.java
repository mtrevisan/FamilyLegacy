package io.github.mtrevisan.familylegacy.v2.io.grammar.ast;


public enum Cardinality{
	REQUIRED,      // no suffix
	OPTIONAL,      // ?
	ZERO_OR_MORE,  // *
	ONE_OR_MORE;   // +

	public String symbol(){
		return switch(this){
			case OPTIONAL -> "?";
			case ZERO_OR_MORE -> "*";
			case ONE_OR_MORE -> "+";
			default -> "";
		};
	}

}
