package io.github.mtrevisan.familylegacy.v2.io.grammar.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * {@code require one_of(fieldA, fieldB, ...)}: at least one of the listed fields MUST be present.
 */
public final class OneOfConstraint extends Constraint{

	private final List<String> fields;


	public OneOfConstraint(final List<String> fields){
		this.fields = Collections.unmodifiableList(new ArrayList<>(fields));
	}


	public List<String> getFields(){
		return fields;
	}

	@Override
	public String toString(){
		return "require one_of(" + String.join(", ", fields) + ")";
	}

}
