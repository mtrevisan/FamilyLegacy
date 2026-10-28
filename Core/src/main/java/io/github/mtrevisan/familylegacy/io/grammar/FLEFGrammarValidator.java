/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.io.grammar;

import io.github.mtrevisan.familylegacy.io.grammar.contraints.ConditionalRequireConstraint;
import io.github.mtrevisan.familylegacy.io.grammar.contraints.Constraint;
import io.github.mtrevisan.familylegacy.io.grammar.contraints.OneOfConstraint;
import io.github.mtrevisan.familylegacy.io.grammar.contraints.OnlyIfInConstraint;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.AlternationType;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.Cardinality;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.EnumType;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FLEFGrammar;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FieldDefinition;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FileDefinition;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.ReferenceType;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.ScalarType;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.StructType;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.TypeDefinition;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.UnionType;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;

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
 * pipeline: {@link FLEFGrammarParser} never fails on a semantically invalid but syntactically correct grammar.
 */
public final class FLEFGrammarValidator{

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

		final Set<Map.Entry<String, TypeDefinition>> entries = grammar.getTypes().entrySet();
		for(final Map.Entry<String, TypeDefinition> entry : entries)
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
		switch(type){
			case null -> errors.add(context + ": missing type");
			case ScalarType scalar -> checkTypeReference(scalar.getName(), context);
			case ReferenceType ref -> checkTypeReference(ref.getTargetTypeName(), context + " (Xref target)");
			case StructType struct -> validateStructLike(struct, context);
			case UnionType union -> validateUnion(union, context);
			case EnumType enumType -> validateEnum(enumType, context);
			case AlternationType alt -> validateAlternation(alt, context);
			default -> {}
		}
	}

	private void validateStructLike(final StructType struct, final String context){
		final Set<String> fieldNames = new HashSet<>();
		final List<FieldDefinition> fields = struct.getFields();
		for(final FieldDefinition field : fields){
			if(!fieldNames.add(field.name()))
				errors.add(context + ": duplicate field '" + field.name() + "'");
			validateTypeUsage(field.type(), FLEFRecordHelper.composePath(context, field.name()));
		}

		final List<Constraint> constraints = struct.getConstraints();
		for(final Constraint constraint : constraints)
			validateConstraint(constraint, fieldNames, context);
	}

	private void validateConstraint(final Constraint constraint, final Set<String> fieldNames, final String context){
		if(constraint instanceof OneOfConstraint oneOf){
			final List<String> fields = oneOf.getFields();
			if(fields.size() < 2)
				warnings.add(context + ": " + oneOf + " lists fewer than two fields");
			for(final String f : fields)
				if(!fieldNames.contains(f))
					errors.add(context + ": " + oneOf + " references unknown field '" + f + "'");
		}
		else if(constraint instanceof ConditionalRequireConstraint cond){
			if(!fieldNames.contains(cond.getConditionField()))
				errors.add(context + ": " + cond + " has unknown condition field '" + cond.getConditionField() + "'");

			final List<String> fields = cond.getRequiredFields();
			for(final String f : fields)
				if(!fieldNames.contains(f))
					errors.add(context + ": " + cond + " requires unknown field '" + f + "'");
		}
		else if(constraint instanceof OnlyIfInConstraint onlyIf){
			if(!fieldNames.contains(onlyIf.getField()))
				errors.add(context + ": " + onlyIf + " references unknown field '" + onlyIf.getField() + "'");
			if(!fieldNames.contains(onlyIf.getConditionField()))
				errors.add(context + ": " + onlyIf + " has unknown condition field '" + onlyIf.getConditionField() + "'");
		}
	}

	private void validateUnion(final UnionType union, final String context){
		if(union.getChoices().isEmpty())
			warnings.add(context + ": oneof has no choices");
		final Set<Map.Entry<String, TypeDefinition>> choices = union.getChoices().entrySet();
		for(final Map.Entry<String, TypeDefinition> choice : choices)
			validateTypeUsage(choice.getValue(), FLEFRecordHelper.composePath(context, choice.getKey()));
	}

	private void validateEnum(final EnumType enumType, final String context){
		final List<String> values = enumType.getValues();
		if(values.isEmpty()){
			errors.add(context + ": enum has no values");

			return;
		}

		final Set<String> seen = new HashSet<>();
		for(final String value : values)
			if(!seen.add(value))
				errors.add(context + ": duplicate enum value '" + value + "'");
	}

	private void validateAlternation(final AlternationType alt, final String context){
		final List<TypeDefinition> alternatives = alt.getAlternatives();
		if(alternatives.size() < 2)
			warnings.add(context + ": alternation with fewer than two alternatives");

		int i = 0;
		for(final TypeDefinition alternative : alternatives)
			validateTypeUsage(alternative, context + " (alt " + (i ++) + ")");
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
