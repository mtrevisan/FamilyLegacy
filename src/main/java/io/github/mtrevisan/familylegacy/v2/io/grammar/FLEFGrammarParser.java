package io.github.mtrevisan.familylegacy.v2.io.grammar;

import io.github.mtrevisan.familylegacy.v2.io.grammar.contraints.AtLeastOneConstraint;
import io.github.mtrevisan.familylegacy.v2.io.grammar.contraints.ComparisonConstraint;
import io.github.mtrevisan.familylegacy.v2.io.grammar.contraints.ConditionalRequireConstraint;
import io.github.mtrevisan.familylegacy.v2.io.grammar.contraints.Constraint;
import io.github.mtrevisan.familylegacy.v2.io.grammar.contraints.EqualTypeConstraint;
import io.github.mtrevisan.familylegacy.v2.io.grammar.contraints.InConstraint;
import io.github.mtrevisan.familylegacy.v2.io.grammar.contraints.OneOfConstraint;
import io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions.AlternationType;
import io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions.Cardinality;
import io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions.EnumType;
import io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions.FieldDefinition;
import io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions.ReferenceType;
import io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions.ScalarType;
import io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions.StructType;
import io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions.TypeDefinition;
import io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions.UnionType;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * Recursive-descent parser for the FLEF grammar language (.gedg files).
 * <p>
 * Grammar (informal):
 * <pre>
 * grammar      := (fileDef | aliasDef | enumDef | structDef | recordDef | oneofAliasDef)*
 * fileDef      := 'file' IDENT structBody
 * aliasDef     := 'alias' IDENT '=' type
 * enumDef      := 'enum' IDENT '{' identList '}' ('|' IDENT)?
 * structDef    := 'struct' IDENT structBody
 * recordDef    := 'record' IDENT structBody
 * oneofAliasDef:= IDENT '=' 'oneof' '{' (IDENT ':' type)* '}'
 * structBody   := '{' (require | field)* '}'
 * field        := IDENT ('?' | '*' | '+')? ':' type
 * require      := 'require' 'one_of' '(' identList ')'
 *               | 'require' 'at_least_one' '(' identList ')'
 *               | 'require' 'if' IDENT '==' IDENT ':' identList
 *               | 'require' IDENT 'in' IDENT
 *               | 'require' IDENT ('!=' | '==' | '>' | '>=' | '<' | '<=') IDENT
 * type         := atomicType ('|' atomicType)*
 * atomicType   := ('Xref' | 'XrefOrVoid') '&lt;' IDENT '&gt;'
 *               | 'struct' structBody
 *               | 'enum' '{' identList '}' ('|' IDENT)?
 *               | IDENT
 * </pre>
 */
public final class FLEFGrammarParser{

	private static final String TAG_COMMENT = "//";
	private static final String TAG_OPEN_CURLY_BRACE = "{";
	private static final String TAG_CLOSE_CURLY_BRACE = "}";
	private static final String TAG_OPEN_ANGLE_BRACKET = "<";
	private static final String TAG_CLOSE_ANGLE_BRACKET = ">";
	private static final String TAG_COMMA = ",";
	private static final String TAG_COLON = ":";
	private static final String TAG_EQUALS = "=";
	private static final String TAG_PIPE = "|";
	private static final String TAG_CARDINALITY_ZERO_OR_ONE = "?";
	private static final String TAG_CARDINALITY_ZERO_OR_MORE = "*";
	private static final String TAG_CARDINALITY_ONE_OR_MORE = "+";
	private static final String NOT_EQUALS = "!=";
	private static final String EQUALS = "==";
	private static final String GREATER_THAN = ">";
	private static final String LESS_THAN = "<";
	private static final String GREATER_THAN_OR_EQUALS = ">=";
	private static final String LESS_THAN_OR_EQUALS = "<=";

	private static final String TAG_FILE = "file";
	private static final String TAG_ALIAS = "alias";
	private static final String TAG_ENUM = "enum";
	private static final String TAG_STRUCT = "struct";
	private static final String TAG_RECORD = "record";
	private static final String TAG_ONEOF = "oneof";
	private static final String TAG_REQUIRE = "require";
	private static final String TAG_ONE_OF_FN = "one_of";
	private static final String TAG_AT_LEAST_ONE_FN = "at_least_one";
	private static final String TAG_TYPE_FN = "type";
	private static final String TAG_IF = "if";
	private static final String TAG_IN = "in";
	private static final String TAG_XREF = "Xref";
	private static final String TAG_XREF_OR_VOID = TAG_XREF + "OrVoid";

	private static final String FIELD_HEADER = "header";
	private static final String FIELD_RECORDS = "records";


	/**
	 * A tiny holder for a struct/record body: its fields plus any {@code require} constraints.
	 */
	private record StructBody(List<FieldDefinition> fields, List<Constraint> constraints){}

	private record Token(String text, int line){}


	private final List<Token> tokens;
	private final List<String> warnings = new ArrayList<>();
	private int position;


	private FLEFGrammarParser(final List<Token> tokens){
		this.tokens = tokens;
		this.position = 0;
	}


	public static FLEFGrammar parse(final Path grammarPath) throws IOException{
		final String content = Files.readString(grammarPath);
		return parse(content);
	}

	public static FLEFGrammar parse(final String content){
		final List<Token> tokens = tokenize(content);
		final FLEFGrammarParser parser = new FLEFGrammarParser(tokens);
		return parser.parseGrammar();
	}


	// ------------------------------------------------------------------
	// Lexer
	// ------------------------------------------------------------------

	private static final String SINGLE_CHAR_TOKENS = "{}[]:,=<>()?*+|!";


	private static List<Token> tokenize(final String content){
		final String normalized = content.replace("\r\n", "\n").replace('\r', '\n');
		final String[] lines = StringUtils.split(normalized, '\n');


		final List<Token> result = new ArrayList<>();
		for(int lineIdx = 0; lineIdx < lines.length; lineIdx ++){
			String line = lines[lineIdx];
			final int commentIdx = line.indexOf(TAG_COMMENT);
			if(commentIdx != -1)
				line = line.substring(0, commentIdx);

			final int lineNumber = lineIdx + 1;
			final int length = line.length();
			int i = 0;
			while(i < length){
				final char c = line.charAt(i);
				if(Character.isWhitespace(c)){
					i ++;
				}
				else if(Character.isLetterOrDigit(c) || c == '_' || c == '.'){
					final int start = i;
					while(i < length && (Character.isLetterOrDigit(line.charAt(i)) || line.charAt(i) == '_'
							|| line.charAt(i) == '.'))
						i ++;
					result.add(new Token(line.substring(start, i), lineNumber));
				}
				else if(SINGLE_CHAR_TOKENS.indexOf(c) != -1){
					final String twoChars = line.substring(i, Math.min(i + 2, length));
					if(twoChars.equals(NOT_EQUALS) || twoChars.equals(GREATER_THAN_OR_EQUALS)
							|| twoChars.equals(LESS_THAN_OR_EQUALS) || twoChars.equals(EQUALS)){
						result.add(new Token(twoChars, lineNumber));

						i += 2;
					}
					else{
						result.add(new Token(String.valueOf(c), lineNumber));

						i ++;
					}
				}
				else
					throw new FLEFGrammarParseException("Unexpected character '" + c + "'", lineNumber);
			}
		}
		return result;
	}


	// ------------------------------------------------------------------
	// Token stream helpers
	// ------------------------------------------------------------------

	private Token peekToken(){
		return (position < tokens.size()? tokens.get(position): null);
	}

	private String peek(){
		final Token t = peekToken();
		return (t != null? t.text(): null);
	}

	private boolean peekIs(final String expected){
		return expected.equals(peek());
	}

	private Token nextToken(){
		if(position >= tokens.size())
			throw new FLEFGrammarParseException("Unexpected end of input", (tokens.isEmpty()? 0: tokens.getLast().line()));

		return tokens.get(position ++);
	}

	private String next(){
		return nextToken().text();
	}

	private void expect(final String expected){
		final Token t = (position < tokens.size()? tokens.get(position): null);
		if(t == null || !expected.equals(t.text()))
			throw new FLEFGrammarParseException(
				"Expected '" + expected + "' but found " + (t == null? "end of input": "'" + t.text() + "'"),
				(t != null? t.line(): (tokens.isEmpty()? 0: tokens.getLast().line())));

		position ++;
	}

	private void putType(final Map<String, TypeDefinition> types, final String name,
			final TypeDefinition definition, final int line){
		if(types.containsKey(name))
			warnings.add("Line " + line + ": duplicate definition of type '" + name + "' (previous definition is overwritten)");
		types.put(name, definition);
	}


	// ------------------------------------------------------------------
	// Top-level grammar
	// ------------------------------------------------------------------

	private FLEFGrammar parseGrammar(){
		final Map<String, TypeDefinition> types = new LinkedHashMap<>();
		FileDefinition fileDef = null;

		while(peek() != null){
			final String token = peek();
			switch(token){
				case TAG_FILE -> {
					if(fileDef != null)
						warnings.add("Line " + peekToken().line() + ": multiple 'file' definitions found, keeping the last one");
					fileDef = parseFileDefinition();
				}
				case TAG_ALIAS -> parseAlias(types);
				case TAG_ENUM -> parseTopLevelEnum(types);
				case TAG_STRUCT -> parseTopLevelStruct(types);
				case TAG_RECORD -> parseTopLevelRecord(types);
				default -> parseIdentifierLedStatement(types);
			}
		}

		if(fileDef == null)
			warnings.add("No 'file' definition found in the grammar");

		return new FLEFGrammar(fileDef, types, warnings);
	}

	/**
	 * Handles top-level statements starting with a bare identifier: {@code Name = oneof { ... }}.
	 */
	private void parseIdentifierLedStatement(final Map<String, TypeDefinition> types){
		final Token nameTok = nextToken();
		expect(TAG_EQUALS);
		if(peekIs(TAG_ONEOF))
			parseOneofAlias(types, nameTok.text(), nameTok.line());
		else
			// Be lenient: treat `Name = <type>` as an alias even without the `alias` keyword.
			putType(types, nameTok.text(), parseType(), nameTok.line());
	}

	// `file FileName { header: Header, records*: Record }`
	private FileDefinition parseFileDefinition(){
		expect(TAG_FILE);
		final String name = next();
		final StructBody body = parseStructBody();

		FieldDefinition header = null;
		FieldDefinition records = null;
		for(final FieldDefinition fd : body.fields()){
			if(FIELD_HEADER.equals(fd.name()))
				header = fd;
			else if(FIELD_RECORDS.equals(fd.name()))
				records = fd;
		}
		if(header == null || records == null)
			throw new FLEFGrammarParseException(
				"'file " + name + "' must declare both a '" + FIELD_HEADER + "' and a '" + FIELD_RECORDS + "' field",
				0);

		return new FileDefinition(name, header, records);
	}

	// `alias AliasName = Type`
	private void parseAlias(final Map<String, TypeDefinition> types){
		expect(TAG_ALIAS);
		final Token nameTok = nextToken();
		expect(TAG_EQUALS);
		final TypeDefinition type = parseType();
		putType(types, nameTok.text(), type, nameTok.line());
	}

	// `enum EnumName { VALUE1, VALUE2, ... } [| Text]`
	private void parseTopLevelEnum(final Map<String, TypeDefinition> types){
		expect(TAG_ENUM);
		final Token nameTok = nextToken();
		final List<String> values = parseEnumValues();
		final boolean allowCustom = parseOptionalCustomTextMarker();
		putType(types, nameTok.text(), new EnumType(nameTok.text(), values, allowCustom), nameTok.line());
	}

	// `struct StructName { ... }`
	private void parseTopLevelStruct(final Map<String, TypeDefinition> types){
		expect(TAG_STRUCT);
		final Token nameTok = nextToken();
		final StructBody body = parseStructBody();
		putType(types, nameTok.text(), new StructType(nameTok.text(), body.fields(), body.constraints()), nameTok.line());
	}

	// `record RecordName { ... }`
	private void parseTopLevelRecord(final Map<String, TypeDefinition> types){
		expect(TAG_RECORD);
		final Token nameTok = nextToken();
		final StructBody body = parseStructBody();
		putType(types, nameTok.text(), new RecordType(nameTok.text(), body.fields(), body.constraints()), nameTok.line());
	}

	// `Name = oneof { choice: Type, ... }`
	private void parseOneofAlias(final Map<String, TypeDefinition> types, final String name,
			final int nameLine){
		expect(TAG_ONEOF);
		expect(TAG_OPEN_CURLY_BRACE);
		final Map<String, TypeDefinition> choices = new LinkedHashMap<>();
		while(!peekIs(TAG_CLOSE_CURLY_BRACE)){
			final String choiceName = next();
			expect(TAG_COLON);
			final TypeDefinition choiceType = parseType();
			choices.put(choiceName, choiceType);
			if(peekIs(TAG_COMMA))
				next();
		}
		expect(TAG_CLOSE_CURLY_BRACE);
		putType(types, name, new UnionType(name, choices), nameLine);
	}


	// ------------------------------------------------------------------
	// Struct bodies, fields, and `require` constraints
	// ------------------------------------------------------------------

	private StructBody parseStructBody(){
		expect(TAG_OPEN_CURLY_BRACE);
		final List<FieldDefinition> fields = new ArrayList<>();
		final List<Constraint> constraints = new ArrayList<>();
		while(!peekIs(TAG_CLOSE_CURLY_BRACE)){
			if(peekIs(TAG_REQUIRE))
				constraints.add(parseConstraint());
			else
				fields.add(parseFieldDefinition());

			if(peekIs(TAG_COMMA))
				next();
		}
		expect(TAG_CLOSE_CURLY_BRACE);
		return new StructBody(fields, constraints);
	}

	private FieldDefinition parseFieldDefinition(){
		final String fieldName = next();

		Cardinality cardinality = Cardinality.REQUIRED;
		if(peekIs(TAG_CARDINALITY_ZERO_OR_ONE)){
			next();

			cardinality = Cardinality.OPTIONAL;
		}
		else if(peekIs(TAG_CARDINALITY_ZERO_OR_MORE)){
			next();

			cardinality = Cardinality.ZERO_OR_MORE;
		}
		else if(peekIs(TAG_CARDINALITY_ONE_OR_MORE)){
			next();

			cardinality = Cardinality.ONE_OR_MORE;
		}

		expect(TAG_COLON);
		final TypeDefinition type = parseType();
		return new FieldDefinition(fieldName, type, cardinality);
	}

	private Constraint parseConstraint(){
		expect(TAG_REQUIRE);

		// 1. require one_of(fieldA, fieldB, ...)
		if(peekIs(TAG_ONE_OF_FN)){
			next();

			expect("(");
			final List<String> fields = parseIdentListUntil(")");
			expect(")");
			return new OneOfConstraint(fields);
		}

		// 2. require at_least_one(fieldA, fieldB, ...)
		if(peekIs(TAG_AT_LEAST_ONE_FN)){
			next();

			expect("(");
			final List<String> fields = parseIdentListUntil(")");
			expect(")");
			return new AtLeastOneConstraint(fields);
		}

		// 3. require type(fieldA) == type(fieldB)
		if(peekIs(TAG_TYPE_FN)){
			next();

			expect("(");
			final List<String> firstFields = parseIdentListUntil(")");
			if(firstFields.size() != 1){
				final Token t = (position < tokens.size()? tokens.get(position): null);
				throw new FLEFGrammarParseException(
					"Expected one field in type(...), found [" + StringUtils.join(firstFields, ", ") + "]",
					(t != null? t.line(): (tokens.isEmpty()? 0: tokens.getLast().line())));
			}
			expect(")");
			expect(EQUALS);
			expect(TAG_TYPE_FN);
			expect("(");
			final List<String> secondFields = parseIdentListUntil(")");
			if(secondFields.size() != 1){
				final Token t = (position < tokens.size()? tokens.get(position): null);
				throw new FLEFGrammarParseException(
					"Expected one field in second type(...), found [" + StringUtils.join(secondFields, ", ") + "]",
					(t != null? t.line(): (tokens.isEmpty()? 0: tokens.getLast().line())));
			}
			expect(")");

			firstFields.addAll(secondFields);
			return new EqualTypeConstraint(firstFields);
		}

		// 4. require if conditionField == conditionValue : requiredField, ...
		if(peekIs(TAG_IF)){
			next();

			final String conditionField = next();
			if(!peekIs(EQUALS))
				throw new FLEFGrammarParseException("Expected '==' after field in 'require if'", peekToken().line());

			next();

			final String conditionValue = next();
			expect(TAG_COLON);
			final List<String> requiredFields = parseCommaSeparatedIdents();
			return new ConditionalRequireConstraint(conditionField, conditionValue, requiredFields);
		}

		// 5. require field in container
		final String firstToken = next();
		if(peekIs(TAG_IN)){
			next();
			final String container = next();
			return new InConstraint(firstToken, container);
		}

		// 6. require left operator right (operators: !=, ==, >, >=, <, <=)
		final String left = firstToken;
		final String op = peek();
		if(NOT_EQUALS.equals(op) || EQUALS.equals(op) || GREATER_THAN.equals(op) || GREATER_THAN_OR_EQUALS.equals(op)
				|| LESS_THAN.equals(op) || LESS_THAN_OR_EQUALS.equals(op)){
			next();
			final String right = next();
			return new ComparisonConstraint(left, op, right);
		}

		throw new FLEFGrammarParseException("Expected 'one_of', 'at_least_one', 'if', 'in', or comparison after 'require', found '" + peek() + "'",
			(peekToken() != null? peekToken().line(): 0));
	}

	/**
	 * Reads comma-separated identifiers until (but not consuming) the given closing token.
	 */
	private List<String> parseIdentListUntil(final String closing){
		final List<String> list = new ArrayList<>();
		while(!peekIs(closing)){
			list.add(next());

			if(peekIs(TAG_COMMA))
				next();
		}
		return list;
	}

	/**
	 * Reads a comma-separated identifier list with no explicit closing token: stops as soon as no comma follows.
	 */
	private List<String> parseCommaSeparatedIdents(){
		final List<String> list = new ArrayList<>();
		list.add(next());
		while(peekIs(TAG_COMMA)){
			next();

			list.add(next());
		}
		return list;
	}

	private List<String> parseEnumValues(){
		expect(TAG_OPEN_CURLY_BRACE);
		final List<String> values = new ArrayList<>();
		while(!peekIs(TAG_CLOSE_CURLY_BRACE)){
			values.add(next().toLowerCase(Locale.ROOT));

			if(peekIs(TAG_COMMA))
				next();
		}
		expect(TAG_CLOSE_CURLY_BRACE);
		return values;
	}

	/**
	 * Consumes an optional trailing {@code | Text} marker (used after both top-level and inline enums).
	 */
	private boolean parseOptionalCustomTextMarker(){
		if(peekIs(TAG_PIPE)){
			next();
			next();
			return true;
		}
		return false;
	}


	// ------------------------------------------------------------------
	// Types
	// ------------------------------------------------------------------

	private TypeDefinition parseType(){
		final TypeDefinition base = parseAtomicType();
		if(peekIs(TAG_PIPE)){
			final List<TypeDefinition> alternatives = new ArrayList<>();
			alternatives.add(base);
			while(peekIs(TAG_PIPE)){
				next();

				alternatives.add(parseAtomicType());
			}
			return new AlternationType(alternatives);
		}
		return base;
	}

	private TypeDefinition parseAtomicType(){
		final String token = peek();
		if(token == null)
			throw new FLEFGrammarParseException("Unexpected end of input, expected a type",
				(tokens.isEmpty()? 0: tokens.getLast().line()));

		switch(token){
			case TAG_XREF, TAG_XREF_OR_VOID -> {
				return parseReferenceType();
			}
			case TAG_STRUCT -> {
				next();

				final StructBody body = parseStructBody();
				return new StructType(null, body.fields(), body.constraints());
			}
			case TAG_ENUM -> {
				next();

				final List<String> values = parseEnumValues();
				final boolean allowCustom = parseOptionalCustomTextMarker();
				return new EnumType(null, values, allowCustom);
			}
		}

		// A plain named-type reference (built-in scalar, alias, struct, record, enum, or union name).
		final String typeName = next();
		return new ScalarType(typeName);
	}

	private TypeDefinition parseReferenceType(){
		final String keyword = next();
		final boolean voidable = TAG_XREF_OR_VOID.equals(keyword);
		expect(TAG_OPEN_ANGLE_BRACKET);
		final String targetTypeName = next();
		expect(TAG_CLOSE_ANGLE_BRACKET);
		return new ReferenceType(keyword + TAG_OPEN_ANGLE_BRACKET + targetTypeName + TAG_CLOSE_ANGLE_BRACKET, targetTypeName, voidable);
	}


	public static void main(final String[] args) throws Exception{
		final Path path = Paths.get("src/main/resources/gedg/flef_0.1.1.gedg");
		final FLEFGrammar grammar = FLEFGrammarParser.parse(path);

		System.out.println("File definition: " + (grammar.getFileDefinition() != null
			? grammar.getFileDefinition()
			.name()
			: "<none>"));
		System.out.println("Total top-level types: " + grammar.getTypeNames().size());

		final Map<String, Integer> counts = new LinkedHashMap<>();
		for(final TypeDefinition t : grammar.getTypes().values()){
			final String kind = t.getClass().getSimpleName();
			counts.merge(kind, 1, Integer::sum);
		}
		counts.forEach((k, v) -> System.out.println("  " + k + ": " + v));

		final FLEFGrammarValidator.ValidationResult result = FLEFGrammarValidator.validate(grammar);

		System.out.println("Warnings (" + result.warnings().size() + "):");
		result.warnings().forEach(w -> System.out.println("  - " + w));

		System.out.println("Errors (" + result.errors().size() + "):");
		result.errors().forEach(e -> System.out.println("  - " + e));

		System.out.println(result.isValid()? "VALID grammar.": "INVALID grammar.");

		if(!result.isValid())
			System.exit(1);
	}

}
