package io.github.mtrevisan.familylegacy.v2.iov3.ast;


public record Xref<T>(String id){

	public static <T> Xref<T> of(String id){
		return new Xref<>(id);
	}

}
