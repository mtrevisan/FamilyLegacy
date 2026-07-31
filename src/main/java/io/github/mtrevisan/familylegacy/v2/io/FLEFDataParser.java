package io.github.mtrevisan.familylegacy.v2.io;

import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammar;
import io.github.mtrevisan.familylegacy.v2.io.grammar.ast.Cardinality;
import io.github.mtrevisan.familylegacy.v2.io.grammar.ast.EnumType;
import io.github.mtrevisan.familylegacy.v2.io.grammar.ast.FieldDefinition;
import io.github.mtrevisan.familylegacy.v2.io.grammar.ast.RecordType;
import io.github.mtrevisan.familylegacy.v2.io.grammar.ast.ReferenceType;
import io.github.mtrevisan.familylegacy.v2.io.grammar.ast.ScalarType;
import io.github.mtrevisan.familylegacy.v2.io.grammar.ast.StructType;
import io.github.mtrevisan.familylegacy.v2.io.grammar.ast.TypeDefinition;
import io.github.mtrevisan.familylegacy.v2.io.grammar.ast.UnionType;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public final class FLEFDataParser{

	private static final String TAG_OPEN_CURLY_BRACE = "{";
	private static final String TAG_CLOSE_CURLY_BRACE = "}";
	private static final String TAG_OPEN_SQUARE_BRACE = "[";
	private static final String TAG_CLOSE_SQUARE_BRACE = "]";
	private static final String TAG_DOUBLE_QUOTE = "\"";
	private static final String TAG_COMMA = ",";
	private static final String TAG_COLON = ":";
	private static final String TAG_IDENTIFIER = "id";
	private static final String XREF_PREFIX = "@";
	private static final String XREF_SUFFIX = "@";


	private final FLEFGrammar grammar;
	private final List<String> tokens;
	private int position;
	private final List<ValidationError> errors = new ArrayList<>();
	private final FLEFModel model;


	public static FLEFModel parse(final Path filePath, final FLEFGrammar grammar) throws IOException,
			ValidationException{
		final String content = Files.readString(filePath, StandardCharsets.UTF_8);
		final FLEFDataParser parser = new FLEFDataParser(content, grammar);
		parser.parse();
		if(!parser.errors.isEmpty())
			throw new ValidationException(parser.errors);

		return parser.model;
	}

	public static void write(final FLEFModel model, final Path filePath) throws IOException{
		final String content = serialize(model);
		Files.writeString(filePath, content, StandardCharsets.UTF_8);
	}

	public static String serialize(final FLEFModel model){
		final StringBuilder sb = new StringBuilder();
		// Header
		final FLEFRecord header = model.getHeader();
		if(header != null){
			sb.append("Header ");
			serializeRecord(header, sb, 0);
			sb.append(StringUtils.LF);
		}
		// Records
		for(final FLEFRecord record : model.getRecords()){
			sb.append(record.getTag());
			sb.append(StringUtils.SPACE);
			serializeRecord(record, sb, 0);
			sb.append(StringUtils.LF);
		}
		return sb.toString();
	}

	private static void serializeRecord(final FLEFRecord record, final StringBuilder sb, final int indent){
		sb.append(TAG_OPEN_CURLY_BRACE)
			.append(StringUtils.LF);
		final Map<String, List<FLEFRecord>> childrenByTag = new LinkedHashMap<>();
		for(final FLEFRecord child : record.getChildren())
			childrenByTag.computeIfAbsent(child.getTag(), k -> new ArrayList<>()).add(child);
		for(final Map.Entry<String, List<FLEFRecord>> entry : childrenByTag.entrySet()){
			final String tag = entry.getKey();
			final List<FLEFRecord> values = entry.getValue();
			final String indentStr = StringUtils.repeat("  ", indent + 1);
			for(final FLEFRecord child : values){
				sb.append(indentStr)
					.append(tag);
				if(child.getValue() != null)
					sb.append(StringUtils.SPACE)
						.append(child.getValue());
				else if(child.hasChildren()){
					sb.append(StringUtils.SPACE);
					serializeRecord(child, sb, indent + 1);
				}
				else
					sb.append(" null");
				sb.append(StringUtils.LF);
			}
		}
		sb.append(StringUtils.repeat("  ", indent))
			.append(TAG_CLOSE_CURLY_BRACE);
	}

	private static String quoteValue(final String value){
		if(value == null)
			return "null";

		if(value.startsWith(XREF_PREFIX) && value.endsWith(XREF_SUFFIX))
			return value;

		if(value.matches("^[A-Za-z_][A-Za-z0-9_]*$"))
			return value;

		return TAG_DOUBLE_QUOTE + value.replace(TAG_DOUBLE_QUOTE, "\\\"") + TAG_DOUBLE_QUOTE;
	}

	// ---------- Parser implementation ----------
	private FLEFDataParser(final String content, final FLEFGrammar grammar){
		this.grammar = grammar;
		this.tokens = tokenize(content);
		this.position = 0;
		this.model = new FLEFModel();
	}

	private List<String> tokenize(final String content){
		final List<String> tokens = new ArrayList<>();
		int i = 0;
		while(i < content.length()){
			final char c = content.charAt(i);
			if(Character.isWhitespace(c)){
				i ++;

				continue;
			}
			if(c == '/' && i + 1 < content.length() && content.charAt(i + 1) == '/'){
				while(i < content.length() && content.charAt(i) != '\n')
					i ++;

				continue;
			}
			if(c == '"'){
				final int start = i;
				i ++;
				while(i < content.length() && content.charAt(i) != '"'){
					if(content.charAt(i) == '\\')
						i += 2;
					else
						i ++;
				}
				if(i < content.length() && content.charAt(i) == '"')
					i ++;
				tokens.add(content.substring(start, i));

				continue;
			}
			if(c == '@'){
				final int start = i;
				i ++;
				while(i < content.length() && content.charAt(i) != '@')
					i ++;
				if(i < content.length() && content.charAt(i) == '@')
					i ++;
				tokens.add(content.substring(start, i));

				continue;
			}
			if(c == '{' || c == '}' || c == '[' || c == ']' || c == ':' || c == ',' || c == '='){
				tokens.add(String.valueOf(c));
				i ++;

				continue;
			}
			if(Character.isDigit(c) || (c == '-' && i + 1 < content.length() && Character.isDigit(content.charAt(i + 1)))){
				final int start = i;
				if(c == '-')
					i ++;
				while(i < content.length() && Character.isDigit(content.charAt(i)))
					i ++;
				if(i < content.length() && content.charAt(i) == '.'){
					i ++;
					while(i < content.length() && Character.isDigit(content.charAt(i)))
						i ++;
				}
				tokens.add(content.substring(start, i));

				continue;
			}
			if(Character.isLetter(c) || c == '_'){
				final int start = i;
				while(i < content.length() && (Character.isLetterOrDigit(content.charAt(i)) || content.charAt(i) == '_'))
					i ++;
				tokens.add(content.substring(start, i));

				continue;
			}
			i ++;
		}
		return tokens;
	}

	private String peek(){
		return (position < tokens.size()? tokens.get(position): null);
	}

	private String next(){
		return (position < tokens.size()? tokens.get(position ++): null);
	}

	private void expect(final String expected){
		final String actual = next();
		if(!expected.equals(actual))
			throw new RuntimeException("Expected '" + expected + "', got '" + actual + "' at token " + position);
	}

	private void parse(){
		// Parse Header
		if(peek() != null && "Header".equals(peek())){
			next();
			final TypeDefinition headerType = grammar.getType("Header");
			if(!(headerType instanceof StructType)){
				errors.add(new ValidationError("Header type must be a struct"));
				return;
			}

			final FLEFRecord header = parseStruct((StructType)headerType);
			model.setHeader(header);
		}

		// Parse records
		final TypeDefinition recordUnion = grammar.getType("Record");
		if(!(recordUnion instanceof UnionType union)){
			errors.add(new ValidationError("Record type must be a union"));

			return;
		}

		final Set<String> recordTypeNames = union.getChoices().keySet();
		while(peek() != null){
			final String token = peek();
			if(!recordTypeNames.contains(token))
				break;

			final String recordType = next();
			final TypeDefinition recordDef = grammar.getType(recordType);
			if(!(recordDef instanceof RecordType)){
				errors.add(new ValidationError("Record type " + recordType + " is not a record"));
				skipBlock();

				continue;
			}
			final FLEFRecord record = parseRecord(recordType, (RecordType)recordDef);
			model.addRecord(record);
		}
	}

	private FLEFRecord parseRecord(final String recordType, final RecordType recordDef){
		final FLEFRecord record = FLEFRecord.createMainRecord(null, recordType);
		final Map<String, List<Object>> fieldValues = new LinkedHashMap<>();

		expect(TAG_OPEN_CURLY_BRACE);
		while(!TAG_CLOSE_CURLY_BRACE.equals(peek())){
			final String fieldName = next();
			expect(TAG_COLON);
			final FieldDefinition fieldDef = findFieldDef(recordDef, fieldName);
			boolean isArray = (fieldDef != null &&
				(fieldDef.cardinality() == Cardinality.ZERO_OR_MORE ||
				fieldDef.cardinality() == Cardinality.ONE_OR_MORE));
			final Object value;
			if(isArray)
				value = parseArrayValue(fieldDef.type());
			else
				value = parseTypedValue(fieldDef != null? fieldDef.type(): null);
			fieldValues.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(value);
			if(TAG_COMMA.equals(peek()))
				next();
		}
		expect(TAG_CLOSE_CURLY_BRACE);

		// Convert field values to children
		for(final Map.Entry<String, List<Object>> entry : fieldValues.entrySet()){
			final String fieldName = entry.getKey();
			final List<Object> values = entry.getValue();
			final FieldDefinition fieldDef = findFieldDef(recordDef, fieldName);
			boolean isArray = (fieldDef != null &&
				(fieldDef.cardinality() == Cardinality.ZERO_OR_MORE ||
				fieldDef.cardinality() == Cardinality.ONE_OR_MORE));
			if(isArray){
				// Each value in the list should be a separate child with the same tag
				for(final Object obj : values){
					if(obj instanceof List){
						for(final Object inner : (List<?>)obj){
							final FLEFRecord child = createChildFromValue(fieldName, inner);
							if(child != null)
								record.addChild(child);
						}
					}
					else{
						final FLEFRecord child = createChildFromValue(fieldName, obj);
						if(child != null)
							record.addChild(child);
					}
				}
			}
			else{
				// Single value
				if(!values.isEmpty()){
					final Object obj = values.getFirst();
					// Special case: if fieldName is "id", set the record's id
					if(TAG_IDENTIFIER.equals(fieldName)){
						if(obj instanceof String)
							record.setId((String)obj);
						else if(obj instanceof FLEFRecord)
							record.setId(((FLEFRecord)obj).getValue());
					}
					else{
						final FLEFRecord child = createChildFromValue(fieldName, obj);
						if(child != null)
							record.addChild(child);
					}
				}
			}
		}

		// Check required fields (except "id" which is handled separately)
		for(final FieldDefinition fieldDef : recordDef.getFields()){
			if(TAG_IDENTIFIER.equals(fieldDef.name()))
				continue;

			final boolean required = fieldDef.cardinality() == Cardinality.REQUIRED;
			final boolean oneOrMore = fieldDef.cardinality() == Cardinality.ONE_OR_MORE;
			if(required || oneOrMore){
				final List<Object> values = fieldValues.get(fieldDef.name());
				if(values == null || values.isEmpty())
					errors.add(new ValidationError("Missing required field: " + fieldDef.name() + " in " + recordType));
			}
		}

		return record;
	}

	private FLEFRecord parseStruct(final StructType structDef){
		final FLEFRecord record = FLEFRecord.createEmpty();
		final Map<String, List<Object>> fieldValues = new LinkedHashMap<>();

		expect(TAG_OPEN_CURLY_BRACE);
		while(!TAG_CLOSE_CURLY_BRACE.equals(peek())){
			final String fieldName = next();
			expect(TAG_COLON);
			final FieldDefinition fieldDef = findFieldDef(structDef, fieldName);
			boolean isArray = (fieldDef != null &&
				(fieldDef.cardinality() == Cardinality.ZERO_OR_MORE ||
				fieldDef.cardinality() == Cardinality.ONE_OR_MORE));
			final Object value;
			if(isArray)
				value = parseArrayValue(fieldDef.type());
			else
				value = parseTypedValue(fieldDef != null? fieldDef.type(): null);
			fieldValues.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(value);
			if(TAG_COMMA.equals(peek()))
				next();
		}
		expect(TAG_CLOSE_CURLY_BRACE);

		// Convert field values to children
		for(final Map.Entry<String, List<Object>> entry : fieldValues.entrySet()){
			final String fieldName = entry.getKey();
			final List<Object> values = entry.getValue();
			final FieldDefinition fieldDef = findFieldDef(structDef, fieldName);
			boolean isArray = (fieldDef != null &&
				(fieldDef.cardinality() == Cardinality.ZERO_OR_MORE ||
				fieldDef.cardinality() == Cardinality.ONE_OR_MORE));
			if(isArray){
				for(final Object obj : values){
					if(obj instanceof List){
						for(final Object inner : (List<?>)obj){
							final FLEFRecord child = createChildFromValue(fieldName, inner);
							if(child != null)
								record.addChild(child);
						}
					}
					else{
						final FLEFRecord child = createChildFromValue(fieldName, obj);
						if(child != null)
							record.addChild(child);
					}
				}
			}
			else if(!values.isEmpty()){
				final Object obj = values.getFirst();
				final FLEFRecord child = createChildFromValue(fieldName, obj);
				if(child != null)
					record.addChild(child);
			}
		}

		// Check required fields (except id)
		for(final FieldDefinition fieldDef : structDef.getFields()){
			if(TAG_IDENTIFIER.equals(fieldDef.name()))
				continue;

			final boolean required = fieldDef.cardinality() == Cardinality.REQUIRED;
			final boolean oneOrMore = fieldDef.cardinality() == Cardinality.ONE_OR_MORE;
			if(required || oneOrMore){
				final List<Object> values = fieldValues.get(fieldDef.name());
				if(values == null || values.isEmpty())
					errors.add(new ValidationError("Missing required field: " + fieldDef.name() + " in " + structDef.getName()));
			}
		}

		return record;
	}

	private FLEFRecord createChildFromValue(final String fieldName, final Object obj){
		switch(obj){
			case null -> {
				return null;
			}
			case FLEFRecord child -> {
				child.setTag(fieldName);
				return child;
			}
			case String s -> {
				return FLEFRecord.createChildWithValue(fieldName, s);
			}
			default -> {}
		}

		return null;
	}

	private Object parseArrayValue(final TypeDefinition elementType){
		final List<Object> list = new ArrayList<>();
		expect(TAG_OPEN_SQUARE_BRACE);
		while(!TAG_CLOSE_SQUARE_BRACE.equals(peek())){
			final Object value = parseTypedValue(elementType);
			list.add(value);
			if(TAG_COMMA.equals(peek()))
				next();
		}
		expect(TAG_CLOSE_SQUARE_BRACE);
		return list;
	}

	private Object parseTypedValue(final TypeDefinition type){
		return switch(type){
			case ScalarType ignored -> parseScalarValue();
			case EnumType ignored -> parseScalarValue();
			case ReferenceType ignored -> parseScalarValue(); // returns string with @
			case StructType structType -> parseStruct(structType);
			case UnionType unionType -> parseUnion(unionType);
			case null, default -> parseGenericValue();
		};
	}

	private Object parseUnion(final UnionType unionDef){
		final String token = peek();
		if(token == null)
			return null;

		if(unionDef.getChoices().containsKey(token)){
			final String choiceName = next();
			final TypeDefinition choiceType = unionDef.getChoices().get(choiceName);
			final Object value = parseTypedValue(choiceType);
			final FLEFRecord wrapper = FLEFRecord.createChild(choiceName);
			if(value instanceof FLEFRecord valueRec){
				if(valueRec.hasChildren()){
					for(final FLEFRecord child : valueRec.getChildren())
						wrapper.addChild(child);
				}
				else if(valueRec.getValue() != null)
					wrapper.setValue(valueRec.getValue());
			}
			else if(value instanceof String)
				wrapper.setValue((String)value);
			else if(value instanceof List){
				for(final Object obj : (List<?>)value){
					final FLEFRecord child = createChildFromValue(null, obj);
					if(child != null)
						wrapper.addChild(child);
				}
			}
			return wrapper;
		}

		return parseGenericValue();
	}

	private Object parseGenericValue(){
		final String token = peek();
		if(token == null)
			return null;

		if(token.equals(TAG_OPEN_CURLY_BRACE))
			return parseGenericStruct();
		if(token.equals(TAG_OPEN_SQUARE_BRACE))
			return parseGenericArray();
		return parseScalarValue();
	}

	private Object parseGenericStruct(){
		final FLEFRecord record = FLEFRecord.createEmpty();
		expect(TAG_OPEN_CURLY_BRACE);
		while(!TAG_CLOSE_CURLY_BRACE.equals(peek())){
			final String fieldName = next();
			expect(TAG_COLON);
			final Object value = parseGenericValue();
			final FLEFRecord child = createChildFromValue(fieldName, value);
			if(child != null)
				record.addChild(child);
			if(TAG_COMMA.equals(peek()))
				next();
		}
		expect(TAG_CLOSE_CURLY_BRACE);
		return record;
	}

	private Object parseGenericArray(){
		final List<Object> list = new ArrayList<>();
		expect(TAG_OPEN_SQUARE_BRACE);
		while(!TAG_CLOSE_SQUARE_BRACE.equals(peek())){
			final Object value = parseGenericValue();
			list.add(value);
			if(TAG_COMMA.equals(peek()))
				next();
		}
		expect(TAG_CLOSE_SQUARE_BRACE);
		return list;
	}

	private String parseScalarValue(){
		final String token = next();
		if(token.startsWith(TAG_DOUBLE_QUOTE) && token.endsWith(TAG_DOUBLE_QUOTE))
			return token.substring(1, token.length() - 1);

		if(token.startsWith(XREF_PREFIX) && token.endsWith(XREF_SUFFIX))
			return token;

		return token;
	}

	private FieldDefinition findFieldDef(final StructType structDef, final String fieldName){
		for(final FieldDefinition def : structDef.getFields())
			if(def.name().equals(fieldName))
				return def;
		return null;
	}

	private void skipBlock(){
		int depth = 0;
		while(peek() != null){
			final String token = next();
			if(TAG_OPEN_CURLY_BRACE.equals(token))
				depth ++;
			else if(TAG_CLOSE_CURLY_BRACE.equals(token)){
				depth --;
				if(depth == 0)
					break;
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
