package io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions;

import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammar;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
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
	public void validate(final String contextPath, final FLEFRecord record, final FLEFModel model,
			final FLEFGrammar grammar, final List<String> errors){
		String tag = record.getTag();
		FLEFRecord targetRecord = record;

		// If the current tag is the field name itself (and not one of the union choices),
		// look at its first child to determine the selected union branch.
		if(!choices.containsKey(tag) && !record.getChildren().isEmpty()){
			final FLEFRecord firstChild = record.getChildren().getFirst();
			if(choices.containsKey(firstChild.getTag())){
				tag = firstChild.getTag();
				targetRecord = firstChild;
			}
		}

		TypeDefinition targetType = choices.get(tag);

		// Ensure the record tag matches one of the defined union branches
		if(targetType == null){
			errors.add(String.format("Invalid union choice '%s' under '%s'. Expected one of: %s",
				tag, contextPath, choices.keySet()));

			return;
		}

		// Dereference symbolic types (ScalarType) against the grammar registry
		if(targetType instanceof ScalarType scalar){
			final TypeDefinition resolved = grammar.getType(scalar.getName());
			if(resolved != null)
				targetType = resolved;
		}

		// Delegate validation to the underlying choice record
		targetType.validate(contextPath, targetRecord, model, grammar, errors);
	}

	@Override
	public String toString(){
		return "oneof" + choices;
	}

}
