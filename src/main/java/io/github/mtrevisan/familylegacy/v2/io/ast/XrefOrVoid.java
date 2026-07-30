package io.github.mtrevisan.familylegacy.v2.io.ast;


public record XrefOrVoid<T>(String id, boolean isVoid){

	public static <T> XrefOrVoid<T> of(String id){
		if("@VOID@".equalsIgnoreCase(id))
			return new XrefOrVoid<>(null, true);

		return new XrefOrVoid<>(id, false);
	}

}
