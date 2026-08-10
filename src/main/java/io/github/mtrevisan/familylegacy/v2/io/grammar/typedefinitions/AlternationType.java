package io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions;

import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammar;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.List;


/**
 * An unnamed alternation between two or more types, written as {@code TypeA | TypeB} (e.g. {@code CalendarType | Text}).
 */
	public final class AlternationType extends TypeDefinition{

	private final List<TypeDefinition> alternatives;


	public AlternationType(final List<TypeDefinition> alternatives){
		super(null);

		this.alternatives = List.copyOf(alternatives);
	}


	public List<TypeDefinition> getAlternatives(){
		return alternatives;
	}

	@Override
	public void validate(final String contextPath, final FLEFRecord record, final FLEFModel model,
			final FLEFGrammar grammar, final List<String> errors){
		boolean matchesAny = true;
		for(final TypeDefinition alternative : alternatives)
			switch(alternative){
				case StructType structType -> structType.validate(contextPath, record, model, grammar, errors);
				case EnumType enumType -> enumType.validate(contextPath, record, model, grammar, errors);
				case UnionType unionType -> unionType.validate(contextPath, record, model, grammar, errors);
				case AlternationType alternationType -> alternationType.validate(contextPath, record, model, grammar, errors);
				case ReferenceType refType -> refType.validate(contextPath, record, model, grammar, errors);
				case ScalarType scalarType -> scalarType.validate(contextPath, record, model, grammar, errors);
				default -> matchesAny = false;
			}

		if(!matchesAny)
			errors.add(String.format("Record at '%s' does not match any allowed alternation type", contextPath));
	}

	@Override
	public String toString(){
		return String.join(" | ", alternatives.stream().map(Object::toString).toList());
	}

}
