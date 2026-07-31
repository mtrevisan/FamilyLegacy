package io.github.mtrevisan.familylegacy.v2.io.grammar;

import io.github.mtrevisan.familylegacy.v2.io.grammar.contraints.ConditionalRequireConstraint;
import io.github.mtrevisan.familylegacy.v2.io.grammar.contraints.Constraint;
import io.github.mtrevisan.familylegacy.v2.io.grammar.contraints.OneOfConstraint;
import io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions.AlternationType;
import io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions.Cardinality;
import io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions.EnumType;
import io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions.FieldDefinition;
import io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions.ReferenceType;
import io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions.ScalarType;
import io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions.StructType;
import io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions.TypeDefinition;
import io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions.UnionType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Performs semantic validation over an already-parsed {@link FLEFGrammar}: unresolved type references, duplicate
 * definitions, {@code require} constraints pointing at unknown fields, enum consistency, and basic sanity checks
 * on the {@code file} definition.
 * <p>
 * Parsing (syntax) and validation (semantics) are intentionally separate steps, mirroring a typical compiler
 * pipeline: {@link FLEFGrammarParser} never fails on a semantically-invalid-but-syntactically-correct grammar.
 */
public final class FLEFGrammarValidator{

	private static final String DOT = ".";


	public record ValidationResult(List<String> errors, List<String> warnings){
		public boolean isValid(){
			return errors.isEmpty();
		}
	}


	private final FLEFGrammar grammar;
	private final List<String> errors = new ArrayList<>();
	private final List<String> warnings = new ArrayList<>();


	private FLEFGrammarValidator(final FLEFGrammar grammar){
		this.grammar = grammar;
	}

	public static ValidationResult validate(final FLEFGrammar grammar){
		final FLEFGrammarValidator validator = new FLEFGrammarValidator(grammar);
		validator.run();
		return new ValidationResult(validator.errors, validator.warnings);
	}


	private void run(){
		warnings.addAll(grammar.getParseWarnings());

		validateFileDefinition();

		for(final Map.Entry<String, TypeDefinition> entry : grammar.getTypes().entrySet())
			validateNamedType(entry.getKey(), entry.getValue());
	}

	private void validateFileDefinition(){
		final FileDefinition fileDef = grammar.getFileDefinition();
		if(fileDef == null){
			errors.add("No 'file' definition present");

			return;
		}

		final String context = "file " + fileDef.name();
		validateTypeUsage(fileDef.headerField().type(), context + ".header");
		validateTypeUsage(fileDef.recordsField().type(), context + ".records");

		if(fileDef.recordsField().cardinality() != Cardinality.ZERO_OR_MORE)
			warnings.add(context + ": 'records' field usually has cardinality '*' (zero or more)");
		if(fileDef.headerField().cardinality() != Cardinality.REQUIRED)
			warnings.add(context + ": 'header' field is usually required (no cardinality suffix)");
	}

	private void validateNamedType(final String name, final TypeDefinition type){
		final String context = "type '" + name + "'";
		if(type instanceof ScalarType scalar)
			checkTypeReference(scalar.getName(), context);
		else if(type instanceof StructType struct)
			validateStructLike(struct, context);
		else if(type instanceof UnionType union)
			validateUnion(union, context);
		else if(type instanceof EnumType enumType)
			validateEnum(enumType, context);
		else if(type instanceof ReferenceType ref)
			checkTypeReference(ref.getTargetTypeName(), context + " (Xref target)");
		else if(type instanceof AlternationType alt)
			validateAlternation(alt, context);
	}

	/**
	 * Validates a type as it's *used* somewhere (a field type, a union choice, an alternative), recursing into anonymous types.
	 */
	private void validateTypeUsage(final TypeDefinition type, final String context){
		if(type == null){
			errors.add(context + ": missing type");

			return;
		}
		if(type instanceof ScalarType scalar)
			checkTypeReference(scalar.getName(), context);
		else if(type instanceof ReferenceType ref)
			checkTypeReference(ref.getTargetTypeName(), context + " (Xref target)");
		else if(type instanceof StructType struct)
			validateStructLike(struct, context);
		else if(type instanceof UnionType union)
			validateUnion(union, context);
		else if(type instanceof EnumType enumType)
			validateEnum(enumType, context);
		else if(type instanceof AlternationType alt)
			validateAlternation(alt, context);
	}

	private void validateStructLike(final StructType struct, final String context){
		final Set<String> fieldNames = new HashSet<>();
		for(final FieldDefinition field : struct.getFields()){
			if(!fieldNames.add(field.name()))
				errors.add(context + ": duplicate field '" + field.name() + "'");
			validateTypeUsage(field.type(), context + DOT + field.name());
		}
		for(final Constraint constraint : struct.getConstraints())
			validateConstraint(constraint, fieldNames, context);
	}

	private void validateConstraint(final Constraint constraint, final Set<String> fieldNames, final String context){
		if(constraint instanceof OneOfConstraint oneOf){
			if(oneOf.getFields().size() < 2)
				warnings.add(context + ": " + oneOf + " lists fewer than two fields");
			for(final String f : oneOf.getFields())
				if(!fieldNames.contains(f))
					errors.add(context + ": " + oneOf + " references unknown field '" + f + "'");
		}
		else if(constraint instanceof ConditionalRequireConstraint cond){
			if(!fieldNames.contains(cond.getConditionField()))
				errors.add(context + ": " + cond + " has unknown condition field '" + cond.getConditionField() + "'");
			for(final String f : cond.getRequiredFields())
				if(!fieldNames.contains(f))
					errors.add(context + ": " + cond + " requires unknown field '" + f + "'");
		}
	}

	private void validateUnion(final UnionType union, final String context){
		if(union.getChoices().isEmpty())
			warnings.add(context + ": oneof has no choices");
		for(final Map.Entry<String, TypeDefinition> choice : union.getChoices().entrySet())
			validateTypeUsage(choice.getValue(), context + DOT + choice.getKey());
	}

	private void validateEnum(final EnumType enumType, final String context){
		if(enumType.getValues().isEmpty()){
			errors.add(context + ": enum has no values");

			return;
		}
		final Set<String> seen = new HashSet<>();
		for(final String value : enumType.getValues())
			if(!seen.add(value))
				errors.add(context + ": duplicate enum value '" + value + "'");
	}

	private void validateAlternation(final AlternationType alt, final String context){
		if(alt.getAlternatives().size() < 2){
			warnings.add(context + ": alternation with fewer than two alternatives");
		}
		int i = 0;
		for(final TypeDefinition alternative : alt.getAlternatives())
			validateTypeUsage(alternative, context + " (alt " + (i++) + ")");
	}

	private void checkTypeReference(final String name, final String context){
		if(name == null){
			errors.add(context + ": missing type reference");

			return;
		}
		if(FLEFGrammar.PRIMITIVE_TYPES.contains(name))
			return;
		if(grammar.hasType(name))
			return;

		errors.add(context + ": unresolved type reference '" + name + "'");
	}

}
