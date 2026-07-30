package io.github.mtrevisan.familylegacy.v2.io;

import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammar;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;


public class FLEFGrammarParser{

	private final List<String> tokens;
	private int pos;

	private FLEFGrammarParser(List<String> tokens){
		this.tokens = tokens;
		this.pos = 0;
	}

	public static FLEFGrammar parse(Path grammarPath) throws IOException{
		String content = Files.readString(grammarPath);
		List<String> tokens = tokenize(content);
		FLEFGrammarParser parser = new FLEFGrammarParser(tokens);
		return parser.parseGrammar();
	}

	private static List<String> tokenize(String content){
		// Remove comments (// ...)
		String[] lines = content.split("\n");
		List<String> tokenList = new ArrayList<>();
		for(String line : lines){
			int commentIdx = line.indexOf("//");
			if(commentIdx != -1){
				line = line.substring(0, commentIdx);
			}
			// Split on whitespace and punctuation
			// We'll use a simple scanner: add spaces around punctuation
			line = line.replaceAll("([{}\\[\\]:,=<>])", " $1 ");
			// split on whitespace
			for(String token : line.split("\\s+")){
				if(!token.isEmpty()){
					tokenList.add(token);
				}
			}
		}
		return tokenList;
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
			throw new IllegalArgumentException("Expected '" + expected + "', got '" + actual + "' at token " + pos);
		}
	}

	private FLEFGrammar parseGrammar(){
		// Parse top-level definitions
		Map<String, FLEFGrammar.TypeDefinition> types = new LinkedHashMap<>();
		FLEFGrammar.FileDefinition fileDef = null;

		while(peek() != null){
			String token = peek();
			if(token.equals("file")){
				fileDef = parseFileDefinition();
			}
			else if(token.equals("alias")){
				parseAlias(types);
			}
			else if(token.equals("enum")){
				parseEnum(types);
			}
			else if(token.equals("struct")){
				parseStruct(types);
			}
			else if(token.equals("record")){
				parseRecord(types);
			}
			else if(token.equals("oneof")){
				// Actually, oneof is used in type alias definitions like "DateValue = oneof { ... }"
				// So we need to parse a type alias that starts with an identifier, then '=', then 'oneof'
				String identifier = next();
				if(!"oneof".equals(peek())){
					// It might be a reference to a type definition? For simplicity, we assume it's a oneof alias.
					throw new IllegalArgumentException("Expected 'oneof' after identifier, got " + peek());
				}
				// parse oneof alias: name = oneof { ... }
				parseOneofAlias(types, identifier);
			}
			else{
				// Could be a type definition without keyword? For now, skip.
				next();
			}
		}

		// Resolve references: after all types are parsed, we need to resolve type references (e.g., Xref<IndividualRecord>)
		// This will be done inside the validation stage or we can resolve lazily.
		// For simplicity, we store references as strings and resolve on demand.
		// We'll implement a method to resolve types after parsing.

		return new FLEFGrammar(fileDef, types);
	}

	// Parse: file FileName { header: Header, records*: Record }
	private FLEFGrammar.FileDefinition parseFileDefinition(){
		expect("file");
		String name = next();
		expect("{");
		FLEFGrammar.FieldDefinition headerField = parseFieldDefinition();
		expect(",");
		FLEFGrammar.FieldDefinition recordsField = parseFieldDefinition();
		expect("}");
		return new FLEFGrammar.FileDefinition(name, headerField, recordsField);
	}

	// Parse: alias AliasName = Type
	private void parseAlias(Map<String, FLEFGrammar.TypeDefinition> types){
		expect("alias");
		String aliasName = next();
		expect("=");
		FLEFGrammar.TypeDefinition type = parseType();
		// Create a ScalarType with that name (or just store as reference)
		types.put(aliasName, type);
	}

	// Parse: enum EnumName { VALUE1, VALUE2, ... } [| Text]
	private void parseEnum(Map<String, FLEFGrammar.TypeDefinition> types){
		expect("enum");
		String enumName = next();
		expect("{");
		List<String> values = new ArrayList<>();
		while(!"}".equals(peek())){
			String val = next();
			if(",".equals(val)) continue;
			values.add(val);
		}
		expect("}");
		boolean allowCustom = false;
		if("|".equals(peek())){
			next(); // consume |
			expect("Text");
			allowCustom = true;
		}
		types.put(enumName, new FLEFGrammar.EnumType(enumName, values, allowCustom));
	}

	// Parse: struct StructName { field: Type, ... }
	private void parseStruct(Map<String, FLEFGrammar.TypeDefinition> types){
		expect("struct");
		String structName = next();
		List<FLEFGrammar.FieldDefinition> fields = parseFieldList();
		types.put(structName, new FLEFGrammar.StructType(structName, fields));
	}

	// Parse: record RecordName { field: Type, ... }
	private void parseRecord(Map<String, FLEFGrammar.TypeDefinition> types){
		expect("record");
		String recordName = next();
		List<FLEFGrammar.FieldDefinition> fields = parseFieldList();
		types.put(recordName, new FLEFGrammar.RecordType(recordName, fields));
	}

	// Parse: Name = oneof { choice: Type, ... }
	private void parseOneofAlias(Map<String, FLEFGrammar.TypeDefinition> types, String name){
		expect("oneof");
		expect("{");
		Map<String, FLEFGrammar.TypeDefinition> choices = new LinkedHashMap<>();
		while(!"}".equals(peek())){
			String choiceName = next();
			expect(":");
			FLEFGrammar.TypeDefinition choiceType = parseType();
			choices.put(choiceName, choiceType);
			if(",".equals(peek())){
				next(); // consume comma
			}
		}
		expect("}");
		// Create a UnionType
		types.put(name, new FLEFGrammar.UnionType(name, choices));
	}

	// Parse a list of fields: { field: Type, field?: Type, field*: Type, field+: Type, ... }
	private List<FLEFGrammar.FieldDefinition> parseFieldList(){
		expect("{");
		List<FLEFGrammar.FieldDefinition> fields = new ArrayList<>();
		while(!"}".equals(peek())){
			fields.add(parseFieldDefinition());
			if(",".equals(peek())){
				next(); // consume comma
			}
		}
		expect("}");
		return fields;
	}

	private FLEFGrammar.FieldDefinition parseFieldDefinition(){
		String fieldName = next();
		// Determine cardinality from the suffix
		FLEFGrammar.Cardinality cardinality;
		if(fieldName.endsWith("?")){
			cardinality = FLEFGrammar.Cardinality.OPTIONAL;
			fieldName = fieldName.substring(0, fieldName.length() - 1);
		}
		else if(fieldName.endsWith("*")){
			cardinality = FLEFGrammar.Cardinality.ZERO_OR_MORE;
			fieldName = fieldName.substring(0, fieldName.length() - 1);
		}
		else if(fieldName.endsWith("+")){
			cardinality = FLEFGrammar.Cardinality.ONE_OR_MORE;
			fieldName = fieldName.substring(0, fieldName.length() - 1);
		}
		else{
			cardinality = FLEFGrammar.Cardinality.REQUIRED;
		}
		expect(":");
		FLEFGrammar.TypeDefinition type = parseType();
		return new FLEFGrammar.FieldDefinition(fieldName, type, cardinality);
	}

	// Parse a type: could be scalar, enum, struct, record, union, reference, or inline struct/union
	private FLEFGrammar.TypeDefinition parseType(){
		String token = peek();
		// Handle Xref<...> or XrefOrVoid<...>
		if(token.equals("Xref") || token.equals("XrefOrVoid")){
			return parseReferenceType();
		}
		// Handle inline struct: struct { ... }
		if(token.equals("struct")){
			next(); // consume struct keyword
			List<FLEFGrammar.FieldDefinition> fields = parseFieldList();
			// create an anonymous struct type with a generated name? For now, we create a StructType with no name.
			return new FLEFGrammar.StructType(null, fields);
		}
		// Handle enum values? not needed
		// Handle union? not for type parsing.
		// Otherwise it's a named type (scalar, enum, struct, record, union)
		String typeName = next();
		// Check if it's a built-in scalar: Text, Date, HistoricalDate, Duration, Uri, LocaleCode, SemVer, Coord, UUID, Bool, Int
		// We'll treat them as ScalarType
		return new FLEFGrammar.ScalarType(typeName);
	}

	private FLEFGrammar.TypeDefinition parseReferenceType(){
		String keyword = next(); // Xref or XrefOrVoid
		boolean voidable = keyword.equals("XrefOrVoid");
		expect("<");
		String targetTypeName = next();
		expect(">");
		// Create a ReferenceType with name = keyword + "<" + targetTypeName + ">" (for display)
		return new FLEFGrammar.ReferenceType(keyword + "<" + targetTypeName + ">", targetTypeName, voidable);
	}

}
