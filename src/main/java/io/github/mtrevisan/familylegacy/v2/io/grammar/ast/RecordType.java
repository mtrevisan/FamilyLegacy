package io.github.mtrevisan.familylegacy.v2.io.grammar.ast;

import java.util.List;


public final class RecordType extends StructType{

	public RecordType(final String name, final List<FieldDefinition> fields, final List<Constraint> constraints){
		super(name, fields, constraints);
	}

}
