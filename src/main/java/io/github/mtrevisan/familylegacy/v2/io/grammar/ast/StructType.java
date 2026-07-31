package io.github.mtrevisan.familylegacy.v2.io.grammar.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


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
	public String toString(){
		return "struct" + (getName() != null? " " + getName(): "") + fields;
	}

}
