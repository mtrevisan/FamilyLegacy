package io.github.mtrevisan.familylegacy.v2.io.grammar.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * An unnamed alternation between two or more types, written as {@code TypeA | TypeB} (e.g. {@code CalendarType | Text}).
 */
public final class AlternationType extends TypeDefinition{

	private final List<TypeDefinition> alternatives;


	public AlternationType(final List<TypeDefinition> alternatives){
		super(null);
		this.alternatives = Collections.unmodifiableList(new ArrayList<>(alternatives));
	}


	public List<TypeDefinition> getAlternatives(){
		return alternatives;
	}

	@Override
	public String toString(){
		return String.join(" | ", alternatives.stream().map(Object::toString).toList());
	}

}
