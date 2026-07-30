package io.github.mtrevisan.familylegacy.v2.io;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammar;
import io.github.mtrevisan.familylegacy.v2.io.grammar.ValidationError;
import io.github.mtrevisan.familylegacy.v2.io.grammar.ValidationException;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;


public final class FLEFDataParser{

	private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

	private final FLEFGrammar grammar;
	private final List<String> tokens;
	private int pos;
	private final List<ValidationError> errors = new ArrayList<>();
	private final FLEFModel model;

	// ---------- Factory methods ----------
	public static FLEFModel parse(Path filePath, FLEFGrammar grammar) throws IOException, ValidationException{
		String content = Files.readString(filePath, StandardCharsets.UTF_8);
		FLEFDataParser parser = new FLEFDataParser(content, grammar);
		parser.parse();
		if(!parser.errors.isEmpty()){
			throw new ValidationException(parser.errors);
		}
		return parser.model;
	}

	public static void write(FLEFModel model, Path filePath) throws IOException{
		String content = serialize(model);
		Files.writeString(filePath, content, StandardCharsets.UTF_8);
	}

	public static String serialize(FLEFModel model){
		StringBuilder sb = new StringBuilder();
		// Header
		FLEFRecord header = model.getHeader();
		if(header != null){
			sb.append("Header ");
			serializeRecord(header, sb, 0);
			sb.append("\n");
		}
		// Records
		for(FLEFRecord record : model.getRecords()){
			sb.append(record.getTag());
			sb.append(" ");
			serializeRecord(record, sb, 0);
			sb.append("\n");
		}
		return sb.toString();
	}

	private static void serializeRecord(FLEFRecord record, StringBuilder sb, int indent){
		sb.append("{\n");
		Map<String, List<FLEFRecord>> childrenByTag = new LinkedHashMap<>();
		for(FLEFRecord child : record.getChildren())
			childrenByTag.computeIfAbsent(child.getTag(), k -> new ArrayList<>()).add(child);
		for(Map.Entry<String, List<FLEFRecord>> entry : childrenByTag.entrySet()){
			String tag = entry.getKey();
			List<FLEFRecord> values = entry.getValue();
			String indentStr = StringUtils.repeat("  ", indent + 1);
			for(FLEFRecord child : values){
				sb.append(indentStr)
					.append(tag);
				if(child.getValue() != null)
					sb.append(" ")
						.append(child.getValue());
				else if(child.hasChildren()){
					sb.append(" ");
					serializeRecord(child, sb, indent + 1);
				}
				else
					sb.append(" null");
				sb.append("\n");
			}
		}
		sb.append(StringUtils.repeat("  ", indent))
			.append("}");
	}

	private static String quoteValue(String value){
		if(value == null) return "null";
		if(value.startsWith("@") && value.endsWith("@")) return value;
		if(value.matches("^[A-Za-z_][A-Za-z0-9_]*$")) return value;
		return "\"" + value.replace("\"", "\\\"") + "\"";
	}

	// ---------- Parser implementation ----------
	private FLEFDataParser(String content, FLEFGrammar grammar){
		this.grammar = grammar;
		this.tokens = tokenize(content);
		this.pos = 0;
		this.model = new FLEFModel();
	}

	private List<String> tokenize(String content){
		List<String> tokens = new ArrayList<>();
		int i = 0;
		while(i < content.length()){
			char c = content.charAt(i);
			if(Character.isWhitespace(c)){
				i++;
				continue;
			}
			if(c == '/' && i + 1 < content.length() && content.charAt(i + 1) == '/'){
				while(i < content.length() && content.charAt(i) != '\n') i++;
				continue;
			}
			if(c == '"'){
				int start = i;
				i++;
				while(i < content.length() && content.charAt(i) != '"'){
					if(content.charAt(i) == '\\') i += 2;
					else i++;
				}
				if(i < content.length() && content.charAt(i) == '"') i++;
				tokens.add(content.substring(start, i));
				continue;
			}
			if(c == '@'){
				int start = i;
				i++;
				while(i < content.length() && content.charAt(i) != '@') i++;
				if(i < content.length() && content.charAt(i) == '@') i++;
				tokens.add(content.substring(start, i));
				continue;
			}
			if(c == '{' || c == '}' || c == '[' || c == ']' || c == ':' || c == ',' || c == '='){
				tokens.add(String.valueOf(c));
				i++;
				continue;
			}
			if(Character.isDigit(c) || (c == '-' && i + 1 < content.length() && Character.isDigit(content.charAt(i + 1)))){
				int start = i;
				if(c == '-') i++;
				while(i < content.length() && Character.isDigit(content.charAt(i))) i++;
				if(i < content.length() && content.charAt(i) == '.'){
					i++;
					while(i < content.length() && Character.isDigit(content.charAt(i))) i++;
				}
				tokens.add(content.substring(start, i));
				continue;
			}
			if(Character.isLetter(c) || c == '_'){
				int start = i;
				while(i < content.length() && (Character.isLetterOrDigit(content.charAt(i)) || content.charAt(i) == '_'))
					i++;
				tokens.add(content.substring(start, i));
				continue;
			}
			i++;
		}
		return tokens;
	}

	private String peek(){
		return pos < tokens.size()? tokens.get(pos): null;
	}

	private String next(){
		return pos < tokens.size()? tokens.get(pos++): null;
	}

	private void expect(String expected){
		String actual = next();
		if(!expected.equals(actual)){
			throw new RuntimeException("Expected '" + expected + "', got '" + actual + "' at token " + pos);
		}
	}

	private void parse(){
		// Parse Header
		if(peek() != null && "Header".equals(peek())){
			next();
			FLEFGrammar.TypeDefinition headerType = grammar.getType("Header");
			if(!(headerType instanceof FLEFGrammar.StructType)){
				errors.add(new ValidationError("Header type must be a struct"));
				return;
			}
			FLEFRecord header = parseStruct((FLEFGrammar.StructType)headerType);
			model.setHeader(header);
		}

		// Parse records
		FLEFGrammar.TypeDefinition recordUnion = grammar.getType("Record");
		if(!(recordUnion instanceof FLEFGrammar.UnionType)){
			errors.add(new ValidationError("Record type must be a union"));
			return;
		}
		FLEFGrammar.UnionType union = (FLEFGrammar.UnionType)recordUnion;
		Set<String> recordTypeNames = union.getChoices().keySet();

		while(peek() != null){
			String token = peek();
			if(!recordTypeNames.contains(token)){
				break;
			}
			String recordType = next();
			FLEFGrammar.TypeDefinition recordDef = grammar.getType(recordType);
			if(!(recordDef instanceof FLEFGrammar.RecordType)){
				errors.add(new ValidationError("Record type " + recordType + " is not a record"));
				skipBlock();
				continue;
			}
			FLEFRecord record = parseRecord(recordType, (FLEFGrammar.RecordType)recordDef);
			if(record != null){
				model.addRecord(record);
			}
		}
	}

	private FLEFRecord parseRecord(String recordType, FLEFGrammar.RecordType recordDef){
		FLEFRecord record = FLEFRecord.createMainRecord(null, recordType);
		Map<String, List<Object>> fieldValues = new LinkedHashMap<>();

		expect("{");
		while(!"}".equals(peek())){
			String fieldName = next();
			expect(":");
			FLEFGrammar.FieldDefinition fieldDef = findFieldDef(recordDef, fieldName);
			boolean isArray = fieldDef != null &&
										(fieldDef.getCardinality() == FLEFGrammar.Cardinality.ZERO_OR_MORE ||
											 fieldDef.getCardinality() == FLEFGrammar.Cardinality.ONE_OR_MORE);
			Object value;
			if(isArray){
				value = parseArrayValue(fieldDef.getType());
			}
			else{
				value = parseTypedValue(fieldDef != null? fieldDef.getType(): null);
			}
			fieldValues.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(value);
			if(",".equals(peek())){
				next();
			}
		}
		expect("}");

		// Convert field values to children
		for(Map.Entry<String, List<Object>> entry : fieldValues.entrySet()){
			String fieldName = entry.getKey();
			List<Object> values = entry.getValue();
			FLEFGrammar.FieldDefinition fieldDef = findFieldDef(recordDef, fieldName);
			boolean isArray = fieldDef != null &&
										(fieldDef.getCardinality() == FLEFGrammar.Cardinality.ZERO_OR_MORE ||
											 fieldDef.getCardinality() == FLEFGrammar.Cardinality.ONE_OR_MORE);
			if(isArray){
				// Each value in the list should be a separate child with the same tag
				for(Object obj : values){
					if(obj instanceof List){
						for(Object inner : (List<?>)obj){
							FLEFRecord child = createChildFromValue(fieldName, inner);
							if(child != null) record.addChild(child);
						}
					}
					else{
						FLEFRecord child = createChildFromValue(fieldName, obj);
						if(child != null) record.addChild(child);
					}
				}
			}
			else{
				// Single value
				if(!values.isEmpty()){
					Object obj = values.get(0);
					// Special case: if fieldName is "id", set the record's id
					if("id".equals(fieldName)){
						if(obj instanceof String){
							record.setId((String)obj);
						}
						else if(obj instanceof FLEFRecord){
							record.setId(((FLEFRecord)obj).getValue());
						}
					}
					else{
						FLEFRecord child = createChildFromValue(fieldName, obj);
						if(child != null) record.addChild(child);
					}
				}
			}
		}

		// Check required fields (except "id" which is handled separately)
		for(FLEFGrammar.FieldDefinition fieldDef : recordDef.getFields()){
			if("id".equals(fieldDef.getName())) continue;
			boolean required = fieldDef.getCardinality() == FLEFGrammar.Cardinality.REQUIRED;
			boolean oneOrMore = fieldDef.getCardinality() == FLEFGrammar.Cardinality.ONE_OR_MORE;
			if(required || oneOrMore){
				List<Object> values = fieldValues.get(fieldDef.getName());
				if(values == null || values.isEmpty()){
					errors.add(new ValidationError("Missing required field: " + fieldDef.getName() + " in " + recordType));
				}
			}
		}

		return record;
	}

	private FLEFRecord parseStruct(FLEFGrammar.StructType structDef){
		FLEFRecord record = FLEFRecord.createEmpty();
		Map<String, List<Object>> fieldValues = new LinkedHashMap<>();

		expect("{");
		while(!"}".equals(peek())){
			String fieldName = next();
			expect(":");
			FLEFGrammar.FieldDefinition fieldDef = findFieldDef(structDef, fieldName);
			boolean isArray = fieldDef != null &&
										(fieldDef.getCardinality() == FLEFGrammar.Cardinality.ZERO_OR_MORE ||
											 fieldDef.getCardinality() == FLEFGrammar.Cardinality.ONE_OR_MORE);
			Object value;
			if(isArray){
				value = parseArrayValue(fieldDef.getType());
			}
			else{
				value = parseTypedValue(fieldDef != null? fieldDef.getType(): null);
			}
			fieldValues.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(value);
			if(",".equals(peek())){
				next();
			}
		}
		expect("}");

		// Convert field values to children
		for(Map.Entry<String, List<Object>> entry : fieldValues.entrySet()){
			String fieldName = entry.getKey();
			List<Object> values = entry.getValue();
			FLEFGrammar.FieldDefinition fieldDef = findFieldDef(structDef, fieldName);
			boolean isArray = fieldDef != null &&
										(fieldDef.getCardinality() == FLEFGrammar.Cardinality.ZERO_OR_MORE ||
											 fieldDef.getCardinality() == FLEFGrammar.Cardinality.ONE_OR_MORE);
			if(isArray){
				for(Object obj : values){
					if(obj instanceof List){
						for(Object inner : (List<?>)obj){
							FLEFRecord child = createChildFromValue(fieldName, inner);
							if(child != null) record.addChild(child);
						}
					}
					else{
						FLEFRecord child = createChildFromValue(fieldName, obj);
						if(child != null) record.addChild(child);
					}
				}
			}
			else{
				if(!values.isEmpty()){
					Object obj = values.get(0);
					FLEFRecord child = createChildFromValue(fieldName, obj);
					if(child != null) record.addChild(child);
				}
			}
		}

		// Check required fields (except id)
		for(FLEFGrammar.FieldDefinition fieldDef : structDef.getFields()){
			if("id".equals(fieldDef.getName())) continue;
			boolean required = fieldDef.getCardinality() == FLEFGrammar.Cardinality.REQUIRED;
			boolean oneOrMore = fieldDef.getCardinality() == FLEFGrammar.Cardinality.ONE_OR_MORE;
			if(required || oneOrMore){
				List<Object> values = fieldValues.get(fieldDef.getName());
				if(values == null || values.isEmpty()){
					errors.add(new ValidationError("Missing required field: " + fieldDef.getName() + " in " + structDef.getName()));
				}
			}
		}

		return record;
	}

	private FLEFRecord createChildFromValue(String fieldName, Object obj){
		if(obj == null) return null;
		if(obj instanceof FLEFRecord){
			FLEFRecord child = (FLEFRecord)obj;
			child.setTag(fieldName);
			return child;
		}
		else if(obj instanceof String){
			return FLEFRecord.createChildWithValue(fieldName, (String)obj);
		}
		else{
			return null;
		}
	}

	private Object parseArrayValue(FLEFGrammar.TypeDefinition elementType){
		List<Object> list = new ArrayList<>();
		expect("[");
		while(!"]".equals(peek())){
			Object value = parseTypedValue(elementType);
			list.add(value);
			if(",".equals(peek())){
				next();
			}
		}
		expect("]");
		return list;
	}

	private Object parseTypedValue(FLEFGrammar.TypeDefinition type){
		if(type == null){
			return parseGenericValue();
		}
		if(type instanceof FLEFGrammar.ScalarType){
			return parseScalarValue();
		}
		else if(type instanceof FLEFGrammar.EnumType){
			return parseScalarValue();
		}
		else if(type instanceof FLEFGrammar.ReferenceType){
			return parseScalarValue(); // returns string with @
		}
		else if(type instanceof FLEFGrammar.StructType){
			return parseStruct((FLEFGrammar.StructType)type);
		}
		else if(type instanceof FLEFGrammar.UnionType){
			return parseUnion((FLEFGrammar.UnionType)type);
		}
		else{
			return parseGenericValue();
		}
	}

	private Object parseUnion(FLEFGrammar.UnionType unionDef){
		String token = peek();
		if(token == null) return null;
		if(unionDef.getChoices().containsKey(token)){
			String choiceName = next();
			FLEFGrammar.TypeDefinition choiceType = unionDef.getChoices().get(choiceName);
			Object value = parseTypedValue(choiceType);
			FLEFRecord wrapper = FLEFRecord.createChild(choiceName);
			if(value instanceof FLEFRecord){
				FLEFRecord valueRec = (FLEFRecord)value;
				if(valueRec.hasChildren()){
					for(FLEFRecord child : valueRec.getChildren()){
						wrapper.addChild(child);
					}
				}
				else if(valueRec.getValue() != null){
					wrapper.setValue(valueRec.getValue());
				}
			}
			else if(value instanceof String){
				wrapper.setValue((String)value);
			}
			else if(value instanceof List){
				for(Object obj : (List<?>)value){
					FLEFRecord child = createChildFromValue(null, obj);
					if(child != null) wrapper.addChild(child);
				}
			}
			return wrapper;
		}
		else{
			return parseGenericValue();
		}
	}

	private Object parseGenericValue(){
		String token = peek();
		if(token == null) return null;
		if(token.equals("{")){
			return parseGenericStruct();
		}
		else if(token.equals("[")){
			return parseGenericArray();
		}
		else{
			return parseScalarValue();
		}
	}

	private Object parseGenericStruct(){
		FLEFRecord record = FLEFRecord.createEmpty();
		expect("{");
		while(!"}".equals(peek())){
			String fieldName = next();
			expect(":");
			Object value = parseGenericValue();
			FLEFRecord child = createChildFromValue(fieldName, value);
			if(child != null) record.addChild(child);
			if(",".equals(peek())){
				next();
			}
		}
		expect("}");
		return record;
	}

	private Object parseGenericArray(){
		List<Object> list = new ArrayList<>();
		expect("[");
		while(!"]".equals(peek())){
			Object value = parseGenericValue();
			list.add(value);
			if(",".equals(peek())){
				next();
			}
		}
		expect("]");
		return list;
	}

	private String parseScalarValue(){
		String token = next();
		if(token.startsWith("\"") && token.endsWith("\"")){
			return token.substring(1, token.length() - 1);
		}
		if(token.startsWith("@") && token.endsWith("@")){
			return token;
		}
		return token;
	}

	private FLEFGrammar.FieldDefinition findFieldDef(FLEFGrammar.StructType structDef, String fieldName){
		for(FLEFGrammar.FieldDefinition def : structDef.getFields()){
			if(def.getName().equals(fieldName)){
				return def;
			}
		}
		return null;
	}

	private void skipBlock(){
		int depth = 0;
		while(peek() != null){
			String token = next();
			if("{".equals(token)) depth++;
			else if("}".equals(token)){
				depth--;
				if(depth == 0) break;
			}
		}
	}

	public FLEFModel getModel(){
		return model;
	}

	public List<ValidationError> getErrors(){
		return errors;
	}

}
