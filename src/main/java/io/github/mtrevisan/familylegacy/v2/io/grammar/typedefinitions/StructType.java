package io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions;

import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammar;
import io.github.mtrevisan.familylegacy.v2.io.grammar.contraints.Constraint;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import org.apache.commons.lang3.StringUtils;

import java.util.List;


/**
 * A group of fields (and optional constraints), either named ({@code struct Name { ... }}) or inline.
 */
public class StructType extends TypeDefinition{

	private static final String DOT = ".";


	private final List<FieldDefinition> fields;
	private final List<Constraint> constraints;


	public StructType(final String name, final List<FieldDefinition> fields, final List<Constraint> constraints){
		super(name);

		this.fields = List.copyOf(fields);
		this.constraints = List.copyOf(constraints);
	}


	public List<FieldDefinition> getFields(){
		return fields;
	}

	public List<Constraint> getConstraints(){
		return constraints;
	}

	public FieldDefinition getField(final String name){
		for(final FieldDefinition f : fields)
			if(f.name().equals(name)){
				return f;
			}
		return null;
	}

	@Override
	public void validate(final String contextPath, final FLEFRecord record, final FLEFModel model,
			final FLEFGrammar grammar, final List<String> errors){
		// Check all fields defined for this structure
		for(final FieldDefinition fieldDef : fields){
			final String fieldName = fieldDef.name();
			final String currentPath = (contextPath.isEmpty()? fieldName: contextPath + DOT + fieldName);

			// Extract all child AST records matching the current field definition name
			final List<FLEFRecord> children = record.getChildren().stream()
				 .filter(c -> c.getTag().equals(fieldName))
				 .toList();

			// 1. Validate cardinality constraints (?, *, +, required)
			if(!fieldDef.cardinality().isValidCount(children.size())){
				errors.add(String.format("Missing required child '%s' under '%s' (found %d, expected %s)",
					fieldName, contextPath, children.size(), fieldDef.cardinality()));

				continue;
			}

			// 2. Resolve the field's declared type
			TypeDefinition fieldType = fieldDef.type();
			// If the type is a symbolic reference (ScalarType), dereference it against the grammar registry
			if(fieldType instanceof ScalarType scalar){
				final TypeDefinition resolved = grammar.getType(scalar.getName());
				if(resolved != null)
					fieldType = resolved;
			}

			// 3. Delegate recursive validation of child records to the resolved type
			for(final FLEFRecord child : children)
				fieldType.validate(currentPath, child, model, grammar, errors);
		}

		// Validate structural constraints (e.g., require one_of, require if)
		for(final Constraint constraint : constraints)
			constraint.validate(contextPath, record, model, errors);
	}

	@Override
	public String toString(){
		return "struct" + (getName() != null? StringUtils.SPACE + getName(): StringUtils.EMPTY) + fields;
	}

}
