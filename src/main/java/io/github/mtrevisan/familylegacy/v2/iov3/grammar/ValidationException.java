package io.github.mtrevisan.familylegacy.v2.iov3.grammar;

import java.util.Collections;
import java.util.List;


/**
 * Exception thrown when validation against a grammar fails.
 */
public class ValidationException extends Exception{

	private final List<ValidationError> errors;


	public static ValidationException create(final List<ValidationError> errors){
		return new ValidationException(errors);
	}


	private ValidationException(final List<ValidationError> errors){
		super("FLEF model validation failed with " + errors.size() + " error(s).");

		this.errors = Collections.unmodifiableList(errors);
	}

	public List<ValidationError> getErrors(){
		return errors;
	}


	@Override
	public String getMessage(){
		final StringBuilder sb = new StringBuilder(super.getMessage());
		for(final ValidationError ve : errors)
			sb.append("\n  ")
				.append(ve);
		return sb.toString();
	}

}
