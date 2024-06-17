package io.github.mtrevisan.familylegacy.flef.sql;

import java.util.Arrays;


class GenericRecord{

	private final Object[] fields;


	static GenericRecord create(final Object[] fields){
		return new GenericRecord(fields);
	}


	private GenericRecord(final Object[] fields){
		this.fields = fields;
	}


	Object[] getFields(){
		return fields;
	}


	@Override
	public String toString(){
		return "Record{"
			+ "fields=" + Arrays.toString(fields)
			+ '}';
	}

}