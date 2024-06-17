package io.github.mtrevisan.familylegacy.flef;

import java.util.Arrays;


public class GenericRecord{

	private final String[] fields;


	public static GenericRecord create(final String[] fields){
		return new GenericRecord(fields);
	}


	private GenericRecord(final String[] fields){
		this.fields = fields;
	}


	public String[] getFields(){
		return fields;
	}


	@Override
	public String toString(){
		return "Record{"
			+ "fields=" + Arrays.toString(fields)
			+ '}';
	}

}
