package io.github.mtrevisan.familylegacy.v2.io.grammar;


/**
 * Represents a validation error found during grammar validation.
 */
public record ValidationError(String path, String message){

	@Override
	public String toString(){
		return (path != null && !path.isEmpty()? path + ": ": "") + message;
	}

}
