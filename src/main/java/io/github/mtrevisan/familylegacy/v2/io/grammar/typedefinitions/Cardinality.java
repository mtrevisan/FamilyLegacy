package io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions;


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

	public boolean isRequired(){
		return (this == REQUIRED || this == ONE_OR_MORE);
	}

	public boolean isSingle(){
		return false;
	}

	/**
	 * Validates whether a given occurrence count satisfies this cardinality constraint.
	 *
	 * @param count the number of occurrences found in the record
	 * @return {@code true} if the count is valid according to this cardinality; {@code false} otherwise
	 */
	public boolean isValidCount(final int count){
		return switch(this){
			case REQUIRED -> (count == 1);
			case OPTIONAL -> (count <= 1);
			case ONE_OR_MORE -> (count >= 1);
			case ZERO_OR_MORE -> (count >= 0);
		};
	}

}
