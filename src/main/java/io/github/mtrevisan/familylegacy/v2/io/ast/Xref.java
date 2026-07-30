package io.github.mtrevisan.familylegacy.v2.io.ast;


public record Xref<T>(String id){

	public static <T> Xref<T> of(String id){
		return new Xref<>(id);
	}

}
