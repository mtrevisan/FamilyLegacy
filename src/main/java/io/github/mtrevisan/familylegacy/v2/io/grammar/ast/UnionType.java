package io.github.mtrevisan.familylegacy.v2.io.grammar.ast;

import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammar;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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
	public void validate(final String contextPath, final FLEFRecord record, final FLEFGrammar grammar,
			final List<String> errors){
		for(final FLEFRecord child : record.getChildren()){
			final TypeDefinition choiceType = choices.get(child.getTag());
			if(choiceType == null)
				errors.add(String.format("Invalid union choice '%s' under '%s'", child.getTag(), contextPath));
			else
				choiceType.validate(contextPath + "." + child.getTag(), child, grammar, errors);
		}
	}

	@Override
	public String toString(){
		return "oneof" + choices;
	}

}
