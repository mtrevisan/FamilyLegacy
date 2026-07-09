package io.github.mtrevisan.familylegacy.v2.io.grammar;

import java.util.List;


/**
 * Thrown when validation fails.
 */
public final class ValidationException extends Exception{

	private final List<ValidationError> errors;

	public ValidationException(List<ValidationError> errors){
		super("Validation failed with " + errors.size() + " error(s)");
		this.errors = errors;
	}

	public List<ValidationError> getErrors(){
		return errors;
	}

	@Override
	public String getMessage(){
		StringBuilder sb = new StringBuilder(super.getMessage());
		for(ValidationError e : errors){
			sb.append("\n  ").append(e);
		}
		return sb.toString();
	}

}
