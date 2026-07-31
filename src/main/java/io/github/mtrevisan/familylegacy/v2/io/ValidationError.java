package io.github.mtrevisan.familylegacy.v2.io;


/**
 * @param path optional
 */
public record ValidationError(String message, String path){

	public ValidationError(String message){
		this(message, null);
	}


	@Override
	public String toString(){
		return (path != null? "[" + path + "] ": "") + message;
	}

}