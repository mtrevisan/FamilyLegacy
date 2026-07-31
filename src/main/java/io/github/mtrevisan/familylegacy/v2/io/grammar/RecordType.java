package io.github.mtrevisan.familylegacy.v2.io.grammar;

import io.github.mtrevisan.familylegacy.v2.io.grammar.contraints.Constraint;
import io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions.FieldDefinition;
import io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions.StructType;

import java.util.List;


public final class RecordType extends StructType{

	public RecordType(final String name, final List<FieldDefinition> fields, final List<Constraint> constraints){
		super(name, fields, constraints);
	}

}
