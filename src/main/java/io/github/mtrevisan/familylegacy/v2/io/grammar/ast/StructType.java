package io.github.mtrevisan.familylegacy.v2.io.grammar.ast;

import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammar;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A group of fields (and optional constraints), either named ({@code struct Name { ... }}) or inline.
 */
public class StructType extends TypeDefinition{

	private final List<FieldDefinition> fields;
	private final List<Constraint> constraints;


	public StructType(final String name, final List<FieldDefinition> fields, final List<Constraint> constraints){
		super(name);
		this.fields = Collections.unmodifiableList(new ArrayList<>(fields));
		this.constraints = Collections.unmodifiableList(new ArrayList<>(constraints));
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
	public void validate(final String contextPath, final FLEFRecord record, final FLEFGrammar grammar,
			final List<String> errors){
		// 1. Child and cardinality validation
		final Map<String, List<FLEFRecord>> childrenByTag = new HashMap<>();
		for(final FLEFRecord child : record.getChildren())
			childrenByTag.computeIfAbsent(child.getTag(), k -> new ArrayList<>()).add(child);

		for(final FieldDefinition field : fields){
			final String fieldName = field.name();
			final List<FLEFRecord> actualChildren = childrenByTag.remove(fieldName);
			final int count = (actualChildren != null? actualChildren.size(): 0);
			final Cardinality card = field.cardinality();

			if(card.isRequired() && count == 0)
				errors.add(String.format("Missing required child '%s' under '%s'", fieldName, contextPath));
			if(card.isSingle() && count > 1)
				errors.add(String.format("Too many occurrences of '%s' under '%s'", fieldName, contextPath));

			if(actualChildren != null){
				final TypeDefinition childTypeDef = grammar.getType(field.type().getName());
				if(childTypeDef != null)
					for(final FLEFRecord childRecord : actualChildren)
						childTypeDef.validate(contextPath + "." + fieldName, childRecord, grammar, errors);
			}
		}

		for(final String unexpectedTag : childrenByTag.keySet())
			errors.add(String.format("Unexpected element '%s' found under '%s'", unexpectedTag, contextPath));

		// 2. Struct constraint validation
		for(final Constraint constraint : constraints)
			constraint.validate(contextPath, record, errors);
	}

	@Override
	public String toString(){
		return "struct" + (getName() != null? " " + getName(): "") + fields;
	}

}
