package io.github.mtrevisan.familylegacy.v2.iov3.grammar;

import org.apache.commons.lang3.StringUtils;


/**
 * Represents a validation failure within a FLEF structure.
 */
public final class ValidationError{

	private final String path;
	private final String message;


	public static ValidationError create(final String path, final String message){
		return new ValidationError(path, message);
	}


	private ValidationError(final String path, final String message){
		this.path = path;
		this.message = message;
	}

	public String getPath(){
		return path;
	}

	public String getMessage(){
		return message;
	}

	@Override
	public String toString(){
		return (path != null && !path.isEmpty()? path + ": ": StringUtils.EMPTY) + message;
	}

}