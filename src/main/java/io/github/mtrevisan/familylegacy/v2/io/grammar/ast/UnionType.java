package io.github.mtrevisan.familylegacy.v2.io.grammar.ast;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * A {@code Name = oneof { choice: Type, ... }} named union.
 */
public final class UnionType extends TypeDefinition{

	private final Map<String, TypeDefinition> choices;


	public UnionType(final String name, final Map<String, TypeDefinition> choices){
		super(name);
		this.choices = Collections.unmodifiableMap(new LinkedHashMap<>(choices));
	}


	public Map<String, TypeDefinition> getChoices(){
		return choices;
	}

	@Override
	public String toString(){
		return "oneof" + choices;
	}

}
