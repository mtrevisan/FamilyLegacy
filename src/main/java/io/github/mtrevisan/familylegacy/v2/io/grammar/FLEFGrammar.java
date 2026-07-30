package io.github.mtrevisan.familylegacy.v2.io.grammar;

import java.util.*;


public class FLEFGrammar{

	private final FileDefinition fileDef;
	private final Map<String, TypeDefinition> types;

	public FLEFGrammar(FileDefinition fileDef, Map<String, TypeDefinition> types){
		this.fileDef = fileDef;
		this.types = Collections.unmodifiableMap(types);
	}

	public FileDefinition getFileDef(){
		return fileDef;
	}

	public TypeDefinition getType(String name){
		return types.get(name);
	}

	public Set<String> getTypeNames(){
		return types.keySet();
	}

	public static class FileDefinition{
		private final String name;
		private final FieldDefinition headerField;
		private final FieldDefinition recordsField;

		public FileDefinition(String name, FieldDefinition headerField, FieldDefinition recordsField){
			this.name = name;
			this.headerField = headerField;
			this.recordsField = recordsField;
		}

		public String getName(){
			return name;
		}

		public FieldDefinition getHeaderField(){
			return headerField;
		}

		public FieldDefinition getRecordsField(){
			return recordsField;
		}
	}

	public static class FieldDefinition{
		private final String name;
		private final TypeDefinition type;
		private final Cardinality cardinality;

		public FieldDefinition(String name, TypeDefinition type, Cardinality cardinality){
			this.name = name;
			this.type = type;
			this.cardinality = cardinality;
		}

		public String getName(){
			return name;
		}

		public TypeDefinition getType(){
			return type;
		}

		public Cardinality getCardinality(){
			return cardinality;
		}
	}

	public enum Cardinality{
		REQUIRED,      // no suffix
		OPTIONAL,      // ?
		ZERO_OR_MORE,  // *
		ONE_OR_MORE    // +
	}

	public static abstract class TypeDefinition{
		private final String name;

		protected TypeDefinition(String name){
			this.name = name;
		}

		public String getName(){
			return name;
		}
	}

	public static class ScalarType extends TypeDefinition{
		public ScalarType(String name){
			super(name);
		}
	}

	public static class EnumType extends TypeDefinition{
		private final List<String> values;
		private final boolean allowCustom;

		public EnumType(String name, List<String> values, boolean allowCustom){
			super(name);
			this.values = Collections.unmodifiableList(values);
			this.allowCustom = allowCustom;
		}

		public List<String> getValues(){
			return values;
		}

		public boolean isAllowCustom(){
			return allowCustom;
		}
	}

	public static class ReferenceType extends TypeDefinition{
		private final String targetTypeName;
		private final boolean voidable;

		public ReferenceType(String name, String targetTypeName, boolean voidable){
			super(name);
			this.targetTypeName = targetTypeName;
			this.voidable = voidable;
		}

		public String getTargetTypeName(){
			return targetTypeName;
		}

		public boolean isVoidable(){
			return voidable;
		}
	}

	public static class StructType extends TypeDefinition{
		private final List<FieldDefinition> fields;

		public StructType(String name, List<FieldDefinition> fields){
			super(name);
			this.fields = Collections.unmodifiableList(fields);
		}

		public List<FieldDefinition> getFields(){
			return fields;
		}
	}

	public static class RecordType extends StructType{
		public RecordType(String name, List<FieldDefinition> fields){
			super(name, fields);
		}
	}

	public static class UnionType extends TypeDefinition{
		private final Map<String, TypeDefinition> choices;

		public UnionType(String name, Map<String, TypeDefinition> choices){
			super(name);
			this.choices = Collections.unmodifiableMap(choices);
		}

		public Map<String, TypeDefinition> getChoices(){
			return choices;
		}
	}

}
