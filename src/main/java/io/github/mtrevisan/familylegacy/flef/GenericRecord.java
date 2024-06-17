package io.github.mtrevisan.familylegacy.flef;

import java.util.HashMap;
import java.util.Map;


public class GenericRecord{

	private final Map<String, Object> fields;


	public GenericRecord(){
		this.fields = new HashMap<>();
	}


	public void setField(final String key, final Object value){
		fields.put(key, value);
	}

	public Object getField(final String key){
		return fields.get(key);
	}

	public Map<String, Object> getFields(){
		return fields;
	}


	@Override
	public String toString(){
		return "Record{"
			+ "fields=" + fields
			+ '}';
	}

}
