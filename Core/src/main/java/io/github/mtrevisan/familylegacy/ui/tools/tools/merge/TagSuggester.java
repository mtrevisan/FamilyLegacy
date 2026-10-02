/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.ui.tools.tools.merge;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Provides the tag and value suggestions for a FLEF field being edited,
 * based on the {@code .gedg} grammar file.
 * <p>
 * The class parses the grammar directly, so it does not depend on the
 * internal {@code FLEFGrammar} model classes. The grammar is loaded
 * from the classpath first ({@code /gedg/flef_0.1.3.gedg}), then from
 * the filesystem ({@code src/main/resources/gedg/flef_0.1.3.gedg}),
 * so the class works both when run from an IDE and from a packaged
 * JAR.
 * <p>
 * The suggester knows three things about the position it is serving:
 * <ul>
 *   <li>the type of the record that owns the field (e.g.
 *       {@code IndividualRecord}); this is what is suggested when the
 *       caret is outside the field's own block;</li>
 *   <li>the tag of the field being edited (e.g. {@code name});</li>
 *   <li>the type of the field itself (e.g.
 *       {@code PersonalNameStructure}); this is what is suggested when
 *       the caret is inside the field's block.</li>
 * </ul>
 * A suggestion carries the cardinality of the field (from the grammar's
 * modifier: none for mandatory, {@code ?} for optional, {@code *} for
 * zero-or-more, {@code +} for one-or-more) and, for blocks, an optional
 * prefill tag that is used only when the block has exactly one mandatory
 * child.
 * <p>
 * <b>Inline structs.</b> The grammar allows both named types and inline
 * anonymous structs ({@code document_part*: struct { ... }}). An inline
 * struct has no name, so it is not covered by the {@code TypeDeclaration}
 * pass, and a path that walks through one could not be resolved. To fix
 * that, every inline struct is registered in the type table under a
 * synthetic name derived from its parent type and its field name
 * (e.g. {@code ExtractStructure.document_part}). The rest of the code
 * does not need to know: as soon as a path segment resolves to a
 * {@code FieldInfo} whose {@code typeName} is a key in the table, the
 * walk continues normally.
 * <p>
 * When the caret is at a <b>value position</b> (after a completed tag
 * on the current line), {@link #valueSuggestionsFor(String, int)} returns
 * the allowed values if the field is an enum, filtered by the prefix
 * the user has already typed, and {@link #valueKindAt(String, int)}
 * tells the editor what shape the value must have for validation.
 * <p>
 * When the field being edited is a record reference ({@code Xref<...>}),
 * {@link #referenceAt(String, int)} returns the referenced record's tag
 * and id, so the editor can open the referenced record's edit dialog on
 * a double-click.
 */
public final class TagSuggester{

	private static final Logger LOGGER = LoggerFactory.getLogger(TagSuggester.class);

	private static final String GRAMMAR_CLASSPATH = "/gedg/flef_0.1.3.gedg";
	private static final String GRAMMAR_FILE = "src/main/resources/gedg/flef_0.1.3.gedg";

	/**
	 * Matches the start of a type declaration: {@code struct X {}},
	 * {@code record X {}}, or the {@code X = oneof {}} form. The
	 * {@code MULTILINE} flag is required because the pattern is applied
	 * to the whole content, not line by line.
	 */
	private static final Pattern TYPE_DECL = Pattern.compile(
		"^(?:struct|record)\\s+(\\w+)\\s*\\{|^(\\w+)\\s*=\\s*oneof\\s*\\{",
		Pattern.MULTILINE);
	private static final Pattern FIELD_DECL = Pattern.compile(
		"^(\\w+)([?*+]?)\\s*:\\s*(.*)$");
	private static final Pattern ONEOF_MAPPING = Pattern.compile(
		"^(\\w+)\\s*:\\s*(\\w+)\\s*$");


	/* ======================================================================
	 *                          Public data types
	 * ====================================================================== */

	/**
	 * Cardinality modifier of a field, as declared in the grammar.
	 */
	public enum Cardinality{
		/** No modifier: exactly one, and the value is mandatory. */
		REQUIRED(StringUtils.EMPTY),
		/** {@code ?}: zero or one. */
		OPTIONAL("?"),
		/** {@code *}: zero or more. */
		MANY_OPTIONAL("*"),
		/** {@code +}: one or more. */
		MANY_REQUIRED("+");


		private final String suffix;


		Cardinality(final String suffix){
			this.suffix = suffix;
		}


		public String suffix(){
			return suffix;
		}

		/** {@code true} when the field requires at least one value. */
		public boolean mandatory(){
			return (this == REQUIRED || this == MANY_REQUIRED);
		}

		/** {@code true} when the field can appear more than once. */
		public boolean list(){
			return (this == MANY_OPTIONAL || this == MANY_REQUIRED);
		}

		static Cardinality fromModifier(final String modifier){
			if(modifier == null || modifier.isEmpty())
				return REQUIRED;
			return switch(modifier){
				case "?" -> OPTIONAL;
				case "*" -> MANY_OPTIONAL;
				case "+" -> MANY_REQUIRED;
				default -> REQUIRED;
			};
		}
	}

	/**
	 * The kind of value expected by a leaf field, derived from the alias
	 * the grammar gives to its type. Used by the editor to validate the
	 * value the user has typed against the schema: a field declared as
	 * {@code Int} must contain an integer, a field declared as
	 * {@code Date} a date, and so on.
	 */
	public enum ValueKind{
		/** Free text, no check. */
		ANY,
		/** Integer, possibly negative. */
		INT,
		/** Boolean: {@code true} or {@code false}. */
		BOOL,
		/** Full ISO calendar date: {@code YYYY-MM-DD}. */
		DATE,
		/** Historical date: {@code YYYY}, {@code YYYY-MM} or {@code YYYY-MM-DD}. */
		HISTORICAL_DATE,
		/** URI with a scheme. */
		URI,
		/** Semantic version, e.g. {@code 1.2.3}. */
		SEMVER,
		/** Locale tag, e.g. {@code it-IT}. */
		LOCALE,
		/** ISO 8601 duration, e.g. {@code P2Y}. */
		DURATION,
		/** Geographic coordinates, ISO 6709. */
		COORD,
		/** One of the enumerated values of the field. */
		ENUM;


		/**
		 * A short, human-readable description of the format expected for
		 * this kind, or {@code null} when the format is obvious enough
		 * that a hint would be noise (free text, integer, boolean,
		 * enum).
		 */
		public String hint(){
			return switch(this){
				case ANY, INT, BOOL, ENUM -> null;
				case DATE -> "date, e.g. 1894-03-17";
				case HISTORICAL_DATE -> "historical date, e.g. 1894, 1894-03, or 1894-03-17";
				case URI -> "URI with a scheme, e.g. https://example.org/record/42";
				case SEMVER -> "semantic version, e.g. 1.2.3";
				case LOCALE -> "locale tag, e.g. it-IT";
				case DURATION -> "ISO 8601 duration, e.g. P2Y";
				case COORD -> "coordinates, ISO 6709, e.g. +45.4386+012.3352";
			};
		}
	}

	/**
	 * A tag that can be inserted at the current position.
	 *
	 * @param tag         the tag name
	 * @param block       {@code true} when the tag opens a nested block
	 * @param prefill     the tag to pre-fill inside the block, or
	 *                    {@code null} when the block has no single
	 *                    mandatory child
	 * @param cardinality the cardinality modifier from the grammar
	 */
	public record Suggestion(String tag, boolean block, String prefill,
									 Cardinality cardinality){}

	/** Context of the caret inside the text being edited. */
	public record Context(String parentType, String prefix, String indent,
								 boolean suppressed){}

	/**
	 * The values allowed for an enum field.
	 *
	 * @param values the enumerated values
	 * @param open   {@code true} when the enum accepts free text in
	 *               addition to the values (i.e. the grammar declares it
	 *               as {@code enum { ... } | Text})
	 */
	public record EnumValues(List<String> values, boolean open){}

	/**
	 * A reference to another record found at the caret position.
	 *
	 * @param recordTag the tag of the referenced record (e.g. {@code document})
	 * @param recordId  the id of the referenced record (e.g. {@code D27})
	 */
	public record ReferenceHit(String recordTag, String recordId){}


	/* ======================================================================
	 *                          Fields
	 * ====================================================================== */

	private final Map<String, List<FieldInfo>> types = new LinkedHashMap<>();
	private final Map<String, String> recordTagToType = new LinkedHashMap<>();

	private final String fieldTag;
	private final String recordType;
	private final String fieldType;


	/* ======================================================================
	 *                          Construction
	 * ====================================================================== */

	public TagSuggester(final String recordTag, final String fieldTag){
		this.fieldTag = fieldTag;

		final String content = loadGrammar();
		if(content != null)
			parseGrammar(content);

		this.recordType = recordTagToType.get(recordTag);
		this.fieldType = computeFieldType(this.recordType, fieldTag);

		LOGGER.info("TagSuggester ready: types={}, records={}, recordType={}, fieldType={}",
			types.size(), recordTagToType.size(), recordType, fieldType);

		if(recordType == null)
			LOGGER.warn("TagSuggester: record tag '{}' not mapped", recordTag);
		if(fieldType == null)
			LOGGER.warn("TagSuggester: field '{}' not found in record type '{}'",
				fieldTag, recordType);
	}


	/* ======================================================================
	 *                          Grammar loading
	 * ====================================================================== */

	private static String loadGrammar(){
		try(final InputStream is = TagSuggester.class.getResourceAsStream(GRAMMAR_CLASSPATH)){
			if(is != null){
				LOGGER.info("Grammar loaded from classpath {}", GRAMMAR_CLASSPATH);
				return new String(is.readAllBytes(), StandardCharsets.UTF_8);
			}
		}
		catch(final IOException ex){
			LOGGER.warn("Classpath grammar read failed: {}", ex.getMessage());
		}

		final Path path = Paths.get(GRAMMAR_FILE);
		if(Files.exists(path)){
			try{
				LOGGER.info("Grammar loaded from file {}", path.toAbsolutePath());
				return Files.readString(path);
			}
			catch(final IOException ex){
				LOGGER.error("Unable to read grammar file {}", path, ex);
			}
		}

		LOGGER.error("Grammar not found: autocomplete will return no suggestions");
		return null;
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	public List<Suggestion> suggestionsFor(final String text, final int caret){
		final Context ctx = resolveContext(text, caret);
		if(ctx.suppressed())
			return Collections.emptyList();
		if(isValuePosition(text, caret))
			return Collections.emptyList();
		if(ctx.parentType() == null)
			return Collections.emptyList();

		final List<FieldInfo> fields = types.get(ctx.parentType());
		if(fields == null)
			return Collections.emptyList();

		final List<Suggestion> out = new ArrayList<>();
		final String prefix = ctx.prefix();
		for(final FieldInfo f : fields)
			if(prefix.isEmpty() || f.name.startsWith(prefix))
				out.add(new Suggestion(f.name, f.block, f.prefill, f.cardinality));

		LOGGER.debug("Suggestions for type '{}', prefix '{}': {} → {}",
			ctx.parentType(), prefix, fields.size(), out.size());
		return out;
	}

	public List<String> valueSuggestionsFor(final String text, final int caret){
		if(text == null || caret < 0 || caret > text.length())
			return Collections.emptyList();
		if(!isValuePosition(text, caret))
			return Collections.emptyList();

		final FieldInfo field = findFieldAtLine(text, caret);
		if(field == null || field.enumValues == null)
			return Collections.emptyList();

		final List<String> all = field.enumValues.values();
		if(all.isEmpty())
			return Collections.emptyList();

		final String prefix = currentPrefix(text, caret);
		if(prefix.isEmpty())
			return all;

		final List<String> filtered = new ArrayList<>();
		for(final String v : all)
			if(v.startsWith(prefix))
				filtered.add(v);
		return filtered;
	}

	public boolean isClosedEnumAt(final String text, final int caret){
		if(text == null || caret < 0 || caret > text.length())
			return false;
		if(!isValuePosition(text, caret))
			return false;

		final FieldInfo field = findFieldAtLine(text, caret);
		return (field != null
			&& field.enumValues != null
			&& !field.enumValues.values().isEmpty()
			&& !field.enumValues.open());
	}

	public ValueKind valueKindAt(final String text, final int caret){
		if(text == null || caret < 0 || caret > text.length())
			return ValueKind.ANY;
		if(!isValuePosition(text, caret))
			return ValueKind.ANY;

		final FieldInfo field = findFieldAtLine(text, caret);
		if(field == null)
			return ValueKind.ANY;
		return kindOf(field);
	}

	public ValueKind valueKindOf(final String parentType, final String fieldName){
		if(parentType == null || fieldName == null)
			return ValueKind.ANY;
		final List<FieldInfo> fields = types.get(parentType);
		if(fields == null)
			return ValueKind.ANY;
		for(final FieldInfo f : fields)
			if(f.name.equals(fieldName))
				return kindOf(f);
		return ValueKind.ANY;
	}

	public String valueHintAt(final String text, final int caret){
		final ValueKind kind = valueKindAt(text, caret);
		return kind.hint();
	}

	public Set<String> allKnownTags(){
		final Set<String> tags = new LinkedHashSet<>();
		for(final List<FieldInfo> fields : types.values())
			for(final FieldInfo f : fields)
				tags.add(f.name);
		return tags;
	}

	public boolean isLeafTag(final String tag){
		if(tag == null)
			return false;
		for(final List<FieldInfo> fields : types.values())
			for(final FieldInfo f : fields)
				if(f.name.equals(tag) && !f.block)
					return true;
		return false;
	}

	public boolean isBlockTag(final String tag){
		if(tag == null)
			return false;
		for(final List<FieldInfo> fields : types.values())
			for(final FieldInfo f : fields)
				if(f.name.equals(tag) && f.block)
					return true;
		return false;
	}

	/**
	 * Returns the record reference under the given caret position, or
	 * {@code null} when the caret is not on a value of an {@code Xref}
	 * field. Used by the comparison dialog to open the referenced
	 * record's edit dialog on double-click.
	 * <p>
	 * The line under the caret is scanned for its leading tag; if the
	 * tag resolves to an {@code Xref} field in the enclosing parent
	 * type, the value after the tag is taken as the record id. The
	 * {@code @...@} delimiters, when present, are stripped.
	 * <p>
	 * The parent type is resolved through the same path walker used by
	 * the tag suggestions, so a reference nested inside an inline struct
	 * (e.g. {@code document} inside {@code extract.document_part}) is
	 * correctly identified.
	 *
	 * @param text  the text of the whole column
	 * @param caret the caret position inside the text
	 * @return the hit, or {@code null}
	 */
	public ReferenceHit referenceAt(final String text, final int caret){
		if(text == null || caret < 0 || caret > text.length())
			return null;

		int lineStart = caret;
		while(lineStart > 0 && text.charAt(lineStart - 1) != '\n')
			lineStart --;
		int lineEnd = text.indexOf('\n', lineStart);
		if(lineEnd < 0)
			lineEnd = text.length();

		int tagStart = lineStart;
		while(tagStart < lineEnd
			&& (text.charAt(tagStart) == ' ' || text.charAt(tagStart) == '\t'))
			tagStart ++;
		int tagEnd = tagStart;
		while(tagEnd < lineEnd){
			final char c = text.charAt(tagEnd);
			if(!Character.isLetterOrDigit(c) && c != '_' && c != '.')
				break;
			tagEnd ++;
		}
		if(tagEnd <= tagStart)
			return null;

		final String fullTag = text.substring(tagStart, tagEnd);
		final int lastDot = fullTag.lastIndexOf('.');
		final String lastSegment = (lastDot >= 0? fullTag.substring(lastDot + 1): fullTag);

		final Context ctx = resolveContext(text, lineStart);
		if(ctx.parentType() == null)
			return null;
		final List<FieldInfo> fields = types.get(ctx.parentType());
		if(fields == null)
			return null;

		FieldInfo field = null;
		for(final FieldInfo f : fields)
			if(f.name.equals(lastSegment)){
				field = f;
				break;
			}
		if(field == null || field.referenceTag == null)
			return null;

		int valueStart = tagEnd;
		while(valueStart < lineEnd
			&& (text.charAt(valueStart) == ' ' || text.charAt(valueStart) == '\t'))
			valueStart ++;
		if(valueStart >= lineEnd)
			return null;

		String value = text.substring(valueStart, lineEnd).trim();
		if(value.isEmpty())
			return null;

		if(value.length() >= 2 && value.startsWith("@") && value.endsWith("@"))
			value = value.substring(1, value.length() - 1);
		if(value.isEmpty())
			return null;

		return new ReferenceHit(field.referenceTag, value);
	}


	/* ======================================================================
	 *                          Value position
	 * ====================================================================== */

	public static boolean isValuePosition(final String text, final int caret){
		if(text == null || caret < 0 || caret > text.length())
			return false;

		int lineStart = caret;
		while(lineStart > 0 && text.charAt(lineStart - 1) != '\n')
			lineStart --;
		final String rawLine = text.substring(lineStart, caret);

		int i = 0;
		while(i < rawLine.length()
			&& (rawLine.charAt(i) == ' ' || rawLine.charAt(i) == '\t'))
			i ++;
		final String stripped = rawLine.substring(i);

		if(stripped.isEmpty())
			return false;
		final char last = stripped.charAt(stripped.length() - 1);
		if(last == '{' || last == '}')
			return false;
		if(last == ' ' || last == '\t')
			return true;

		int j = 0;
		while(j < stripped.length()){
			final char c = stripped.charAt(j);
			if(!Character.isLetterOrDigit(c) && c != '_' && c != '.')
				break;
			j ++;
		}
		return (j < stripped.length());
	}


	/* ======================================================================
	 *                          Context resolution
	 * ====================================================================== */

	public Context resolveContext(final String text, final int caret){
		if(text == null || caret < 0 || caret > text.length())
			return new Context(null, StringUtils.EMPTY, StringUtils.EMPTY, true);

		final Deque<String> tagStack = new ArrayDeque<>();
		final StringBuilder token = new StringBuilder();
		boolean tokenComplete = false;
		boolean inLineComment = false;
		boolean inBlockComment = false;
		boolean inMultilineString = false;

		int i = 0;
		while(i < caret){
			final char c = text.charAt(i);

			if(inMultilineString){
				if(c == '"' && i + 2 < caret && text.charAt(i + 1) == '"'
					&& text.charAt(i + 2) == '"'){
					inMultilineString = false;
					i += 3;
					continue;
				}
				i ++;
				continue;
			}
			if(inBlockComment){
				if(c == '*' && i + 1 < caret && text.charAt(i + 1) == '/'){
					inBlockComment = false;
					i += 2;
					continue;
				}
				i ++;
				continue;
			}
			if(inLineComment){
				if(c == '\n') inLineComment = false;
				i ++;
				continue;
			}
			if(c == '/' && i + 1 < caret){
				final char n = text.charAt(i + 1);
				if(n == '/'){ inLineComment = true; i += 2; continue; }
				if(n == '*'){ inBlockComment = true; i += 2; continue; }
			}
			if(c == '#'){ inLineComment = true; i ++; continue; }
			if(c == '"' && i + 2 < caret && text.charAt(i + 1) == '"'
				&& text.charAt(i + 2) == '"'){
				inMultilineString = true;
				i += 3;
				continue;
			}

			if(c == '{'){
				final String raw = token.toString().trim();
				if(!raw.isEmpty()){
					final String[] segments = StringUtils.split(raw, '.');
					for(int s = segments.length - 1; s >= 0; s --){
						final String seg = segments[s].trim();
						if(!seg.isEmpty())
							tagStack.push(seg);
					}
				}
				token.setLength(0);
				tokenComplete = false;
			}
			else if(c == '}'){
				if(!tagStack.isEmpty())
					tagStack.pop();
				token.setLength(0);
				tokenComplete = false;
			}
			else if(c == '\n'){
				token.setLength(0);
				tokenComplete = false;
			}
			else if(c == ' ' || c == '\t'){
				if(!token.isEmpty())
					tokenComplete = true;
			}
			else if(!tokenComplete)
				token.append(c);

			i ++;
		}

		final List<String> path = new ArrayList<>(tagStack);
		Collections.reverse(path);

		final String parentType = resolveTypeFromPath(path);
		final String prefix = currentPrefix(text, caret);
		final String indent = currentIndent(text, caret);
		final boolean suppressed = inMultilineString || inBlockComment || inLineComment;

		LOGGER.debug("resolveContext: caret={}, path={}, parentType={}, prefix='{}', suppressed={}",
			caret, path, parentType, prefix, suppressed);

		return new Context(parentType, prefix, indent, suppressed);
	}

	/**
	 * Walks the open-block path and returns the type whose fields should
	 * be suggested. The walk is forgiving: if an intermediate tag is not
	 * a valid child of the current type, the walk stops and returns the
	 * last valid type, so the user still sees useful suggestions.
	 * <p>
	 * Inline structs are resolved through the synthetic names registered
	 * during parsing, so a path like
	 * {@code source → extract → document_part → document} resolves
	 * correctly and the reference at {@code document} is found.
	 */
	private String resolveTypeFromPath(final List<String> path){
		if(path.isEmpty())
			return recordType;
		if(!fieldTag.equals(path.get(0)))
			return recordType;

		String currentType = fieldType;
		if(currentType == null)
			return recordType;

		for(int i = 1; i < path.size(); i ++){
			final String tag = path.get(i);
			final List<FieldInfo> fields = types.get(currentType);
			if(fields == null)
				return currentType;
			String nextType = null;
			for(final FieldInfo f : fields)
				if(f.name.equals(tag)){
					nextType = f.typeName;
					break;
				}
			if(nextType == null)
				return currentType;
			currentType = nextType;
		}
		return currentType;
	}

	private static String currentPrefix(final String text, final int caret){
		int i = caret;
		while(i > 0){
			final char c = text.charAt(i - 1);
			if(!Character.isLetterOrDigit(c) && c != '_')
				break;
			i --;
		}
		return text.substring(i, caret);
	}

	private static String currentIndent(final String text, final int caret){
		int lineStart = caret;
		while(lineStart > 0 && text.charAt(lineStart - 1) != '\n')
			lineStart --;
		int i = lineStart;
		while(i < text.length() && i < caret){
			final char c = text.charAt(i);
			if(c != ' ' && c != '\t')
				break;
			i ++;
		}
		return text.substring(lineStart, i);
	}


	/* ======================================================================
	 *                          Field lookup
	 * ====================================================================== */

	private FieldInfo findFieldAtLine(final String text, final int caret){
		int lineStart = caret;
		while(lineStart > 0 && text.charAt(lineStart - 1) != '\n')
			lineStart --;

		int tagStart = lineStart;
		while(tagStart < text.length()
			&& (text.charAt(tagStart) == ' ' || text.charAt(tagStart) == '\t'))
			tagStart ++;
		int tagEnd = tagStart;
		while(tagEnd < text.length()){
			final char c = text.charAt(tagEnd);
			if(!Character.isLetterOrDigit(c) && c != '_' && c != '.')
				break;
			tagEnd ++;
		}
		if(tagEnd <= tagStart)
			return null;

		final String fullTag = text.substring(tagStart, tagEnd);
		final int lastDot = fullTag.lastIndexOf('.');
		final String lastSegment = (lastDot >= 0? fullTag.substring(lastDot + 1): fullTag);

		final Context ctx = resolveContext(text, lineStart);
		if(ctx.parentType() == null)
			return null;
		final List<FieldInfo> fields = types.get(ctx.parentType());
		if(fields == null)
			return null;

		for(final FieldInfo f : fields)
			if(f.name.equals(lastSegment))
				return f;
		return null;
	}

	private static ValueKind kindOf(final FieldInfo field){
		if(field.enumValues != null && !field.enumValues.values().isEmpty())
			return ValueKind.ENUM;
		return kindOfType(field.rawTypeName);
	}

	private static ValueKind kindOfType(final String typeName){
		if(typeName == null)
			return ValueKind.ANY;
		return switch(typeName){
			case "Int" -> ValueKind.INT;
			case "Bool" -> ValueKind.BOOL;
			case "Date" -> ValueKind.DATE;
			case "HistoricalDate" -> ValueKind.HISTORICAL_DATE;
			case "Uri" -> ValueKind.URI;
			case "SemVer" -> ValueKind.SEMVER;
			case "LocaleCode" -> ValueKind.LOCALE;
			case "Duration" -> ValueKind.DURATION;
			case "Coord" -> ValueKind.COORD;
			default -> ValueKind.ANY;
		};
	}

	private String computeFieldType(final String recordTypeName, final String fieldTagName){
		if(recordTypeName == null || fieldTagName == null)
			return null;
		final List<FieldInfo> fields = types.get(recordTypeName);
		if(fields == null)
			return null;
		for(final FieldInfo f : fields)
			if(f.name.equals(fieldTagName))
				return f.typeName;
		return null;
	}


	/* ======================================================================
	 *                          Grammar parsing
	 * ====================================================================== */

	private void parseGrammar(final String rawContent){
		// Pass 0: strip line comments from every line, so later passes
		// never match inside commented-out declarations or inside the
		// trailing part of a field declaration.
		final StringBuilder sb = new StringBuilder(rawContent.length());
		for(final String raw : rawContent.split(StringUtils.LF, -1))
			sb.append(stripLineComment(raw))
				.append('\n');
		final String content = sb.toString();

		// Pass 1: collect every declared type name. Both `struct X {`
		// and `record X {` and `X = oneof {` forms are recognised.
		final Set<String> typeNames = new LinkedHashSet<>();
		final Matcher tm = TYPE_DECL.matcher(content);
		while(tm.find()){
			final String name = (tm.group(1) != null? tm.group(1): tm.group(2));
			if(name != null)
				typeNames.add(name);
		}
		LOGGER.debug("Declared type names ({}): {}", typeNames.size(), typeNames);

		// Pass 2: parse each declared type. Inline structs are parsed
		// recursively and registered under a synthetic name, so a path
		// walker can continue through them.
		final Matcher m = TYPE_DECL.matcher(content);
		while(m.find()){
			final String typeName = (m.group(1) != null? m.group(1): m.group(2));
			if(typeName == null)
				continue;
			final int bracePos = m.end() - 1;
			final int bodyEnd = findMatchingBrace(content, bracePos);
			if(bodyEnd < 0){
				LOGGER.warn("Unterminated body for type '{}'", typeName);
				continue;
			}
			final String body = content.substring(m.end(), bodyEnd);
			types.put(typeName, parseBody(body, typeNames, typeName));
		}
		LOGGER.debug("Parsed types ({}): {}", types.size(), types.keySet());

		// Pass 3: Record = oneof { tag: Type, ... }.
		final int oneofIdx = content.indexOf("Record = oneof");
		if(oneofIdx >= 0){
			final int bodyStart = content.indexOf('{', oneofIdx);
			final int bodyEnd = findMatchingBrace(content, bodyStart);
			if(bodyStart >= 0 && bodyEnd > bodyStart){
				final String body = content.substring(bodyStart + 1, bodyEnd);
				for(final String line : body.split(StringUtils.LF, -1)){
					final Matcher om = ONEOF_MAPPING.matcher(line.trim());
					if(om.matches())
						recordTagToType.put(om.group(1), om.group(2));
				}
			}
		}
		LOGGER.debug("Record tag mapping ({}): {}",
			recordTagToType.size(), recordTagToType);

		// Pass 4: prefill only for blocks whose single child is mandatory.
		for(final List<FieldInfo> fields : types.values())
			for(final FieldInfo f : fields)
				if(f.block && f.typeName != null){
					final List<FieldInfo> children = types.get(f.typeName);
					if(children != null && children.size() == 1
						&& children.get(0).cardinality.mandatory())
						f.prefill = children.get(0).name;
				}
	}

	/**
	 * Parses the body of a type declaration, returning its top-level
	 * fields. Inline structs and oneofs are parsed recursively and
	 * registered in the type table under a synthetic name of the form
	 * {@code parentType.fieldName}, so the rest of the code can walk
	 * into them as if they were named types.
	 *
	 * @param body           the body of the declaration (between the
	 *                       outer braces)
	 * @param knownTypes     the set of type names declared somewhere in
	 *                       the grammar, used to distinguish a named
	 *                       block from an inline one
	 * @param parentTypeName the name of the type being parsed, used to
	 *                       build the synthetic name of inline structs
	 * @return the list of top-level fields
	 */
	private List<FieldInfo> parseBody(final String body, final Set<String> knownTypes,
		final String parentTypeName){
		final List<FieldInfo> fields = new ArrayList<>();
		final String[] lines = body.split(StringUtils.LF, -1);
		int depth = 0;
		int i = 0;
		while(i < lines.length){
			final String line = lines[i].trim();

			if(depth == 0 && !line.isEmpty() && !line.startsWith("//")){
				final Matcher fm = FIELD_DECL.matcher(line);
				if(fm.matches()){
					final String name = fm.group(1);
					final String modifier = fm.group(2);
					final String typeText = fm.group(3);
					final Cardinality cardinality = Cardinality.fromModifier(modifier);

					// Enum: extract the values, no type registration needed.
					if(typeText.startsWith("enum")){
						final StringBuilder acc = new StringBuilder(line);
						int enumDepth = countChar(line, '{') - countChar(line, '}');
						int j = i + 1;
						while(enumDepth > 0 && j < lines.length){
							acc.append('\n').append(lines[j]);
							enumDepth += countChar(lines[j], '{') - countChar(lines[j], '}');
							j ++;
						}
						final EnumValues en = extractEnum(acc.toString());
						fields.add(new FieldInfo(name, null, "enum",
							false, cardinality, en, null));
						i = j;
						continue;
					}

					// A field whose type starts with Xref<...> is a
					// record reference. The field's own tag is used as
					// the record tag: in the FLEF grammar the reference
					// field always takes the name of the record it
					// points to (source: Xref<SourceRecord>,
					// document: Xref<DocumentRecord>, ...).
					final String referenceTag = (typeText.startsWith("Xref<")? name: null);

					final boolean inlineStruct = typeText.startsWith("struct");
					final boolean inlineOneof = typeText.startsWith("oneof");
					final String baseType = extractBaseType(typeText);
					final boolean namedBlock = (baseType != null && knownTypes.contains(baseType));
					final boolean block = inlineStruct || inlineOneof || namedBlock;

					if(inlineStruct || inlineOneof){
						// Inline struct/oneof: parse its body recursively
						// and register the result under a synthetic name.
						// The synthetic name is parentTypeName + "." + name,
						// guaranteed to be unique within the grammar
						// because real type names cannot contain dots
						// (the declaration regex only allows \w+).
						final String syntheticName = parentTypeName + "." + name;

						// Accumulate the inline body starting from the
						// current line, up to the matching closing brace.
						final StringBuilder acc = new StringBuilder(line);
						int structDepth = countChar(line, '{') - countChar(line, '}');
						int j = i + 1;
						while(structDepth > 0 && j < lines.length){
							acc.append('\n').append(lines[j]);
							structDepth += countChar(lines[j], '{') - countChar(lines[j], '}');
							j ++;
						}

						// Extract the text between the first '{' and the
						// matching '}' of the accumulated block.
						final String accStr = acc.toString();
						final int open = accStr.indexOf('{');
						final int close = accStr.lastIndexOf('}');
						final String inlineBody = (open >= 0 && close > open
							? accStr.substring(open + 1, close)
							: StringUtils.EMPTY);

						// Recursive parse and register.
						types.put(syntheticName,
							parseBody(inlineBody, knownTypes, syntheticName));

						fields.add(new FieldInfo(name, syntheticName, baseType,
							true, cardinality, new EnumValues(List.of(), false), referenceTag));

						i = j;
						continue;
					}

					// Named block or leaf field.
					final String typeName = (namedBlock? baseType: null);
					fields.add(new FieldInfo(name, typeName, baseType,
						block, cardinality, new EnumValues(List.of(), false), referenceTag));
				}
			}

			depth += countChar(line, '{') - countChar(line, '}');
			if(depth < 0)
				depth = 0;
			i ++;
		}
		return fields;
	}

	private static EnumValues extractEnum(final String accumulated){
		final int open = accumulated.indexOf('{');
		final int close = accumulated.lastIndexOf('}');
		if(open < 0 || close <= open)
			return new EnumValues(List.of(), false);

		final String inner = accumulated.substring(open + 1, close);
		final List<String> values = new ArrayList<>();
		for(final String token : inner.split("[,\\s]+")){
			final String t = token.trim();
			if(!t.isEmpty() && Character.isLetterOrDigit(t.charAt(0)))
				values.add(t);
		}

		final String after = accumulated.substring(close + 1).trim();
		final boolean isOpen = after.startsWith("|");
		return new EnumValues(values, isOpen);
	}

	private static String stripLineComment(final String line){
		boolean inString = false;
		for(int i = 0; i < line.length() - 1; i ++){
			final char c = line.charAt(i);
			if(c == '"'){
				inString = !inString;
				continue;
			}
			if(!inString && c == '/' && line.charAt(i + 1) == '/')
				return line.substring(0, i);
		}
		return line;
	}

	private static int countChar(final String s, final char target){
		int n = 0;
		for(int i = 0; i < s.length(); i ++)
			if(s.charAt(i) == target)
				n ++;
		return n;
	}

	private static String extractBaseType(final String typeText){
		if(typeText == null)
			return null;
		final String s = typeText.trim();
		if(s.isEmpty())
			return null;
		int end = 0;
		while(end < s.length()){
			final char c = s.charAt(end);
			if(!Character.isLetterOrDigit(c) && c != '_')
				break;
			end ++;
		}
		return (end == 0? null: s.substring(0, end));
	}

	private static int findMatchingBrace(final String content, final int openPos){
		if(openPos < 0 || openPos >= content.length() || content.charAt(openPos) != '{')
			return -1;

		int depth = 0;
		boolean inLine = false;
		boolean inBlock = false;
		boolean inString = false;

		for(int i = openPos; i < content.length(); i ++){
			final char c = content.charAt(i);

			if(inLine){
				if(c == '\n') inLine = false;
				continue;
			}
			if(inBlock){
				if(c == '*' && i + 1 < content.length() && content.charAt(i + 1) == '/'){
					inBlock = false;
					i ++;
				}
				continue;
			}
			if(inString){
				if(c == '"') inString = false;
				continue;
			}
			if(c == '/' && i + 1 < content.length()){
				final char n = content.charAt(i + 1);
				if(n == '/'){ inLine = true; i ++; continue; }
				if(n == '*'){ inBlock = true; i ++; continue; }
			}
			if(c == '"'){ inString = true; continue; }
			if(c == '{') depth ++;
			else if(c == '}'){
				depth --;
				if(depth == 0)
					return i;
			}
		}
		return -1;
	}


	/* ======================================================================
	 *                          Internal field descriptor
	 * ====================================================================== */

	private static final class FieldInfo{
		final String name;
		final String typeName;
		final String rawTypeName;
		final boolean block;
		final Cardinality cardinality;
		final EnumValues enumValues;
		final String referenceTag;
		String prefill;

		FieldInfo(final String name, final String typeName, final String rawTypeName,
			final boolean block, final Cardinality cardinality,
			final EnumValues enumValues, final String referenceTag){
			this.name = name;
			this.typeName = typeName;
			this.rawTypeName = rawTypeName;
			this.block = block;
			this.cardinality = cardinality;
			this.enumValues = enumValues;
			this.referenceTag = referenceTag;
		}
	}

}
