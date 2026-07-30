package io.github.mtrevisan.familylegacy.v2.io.grammar;


public class ValidationError{

	private final String message;
	private final String path; // optional

	public ValidationError(String message){
		this(message, null);
	}

	public ValidationError(String message, String path){
		this.message = message;
		this.path = path;
	}

	public String getMessage(){
		return message;
	}

	public String getPath(){
		return path;
	}

	@Override
	public String toString(){
		return (path != null? "[" + path + "] ": "") + message;
	}
}