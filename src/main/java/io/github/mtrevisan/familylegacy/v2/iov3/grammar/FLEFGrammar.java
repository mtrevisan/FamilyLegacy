package io.github.mtrevisan.familylegacy.v2.iov3.grammar;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parses and holds grammar definitions written in the FLEF Protocol DSL.
 */
public final class FLEFGrammar{

	public enum Cardinality{
		EXACTLY_ONE, // default
		OPTIONAL,    // ?
		ZERO_OR_MORE,// *
		ONE_OR_MORE  // +
	}

	public static class FieldDef{
		private final String name;
		private final String typeName;
		private final Cardinality cardinality;
		private final boolean isEnumInline;
		private final Set<String> inlineEnumValues;

		public FieldDef(String name, String typeName, Cardinality cardinality, boolean isEnumInline,
				Set<String> inlineEnumValues){
			this.name = name;
			this.typeName = typeName;
			this.cardinality = cardinality;
			this.isEnumInline = isEnumInline;
			this.inlineEnumValues = inlineEnumValues;
		}

		public String getName(){
			return name;
		}

		public String getTypeName(){
			return typeName;
		}

		public Cardinality getCardinality(){
			return cardinality;
		}

		public boolean isEnumInline(){
			return isEnumInline;
		}

		public Set<String> getInlineEnumValues(){
			return inlineEnumValues;
		}

	}

	public static class EntityDef{
		private final String name;
		private final boolean isRecord;
		private final Map<String, FieldDef> fields = new LinkedHashMap<>();

		public EntityDef(String name, boolean isRecord){
			this.name = name;
			this.isRecord = isRecord;
		}

		public String getName(){
			return name;
		}

		public boolean isRecord(){
			return isRecord;
		}

		public Map<String, FieldDef> getFields(){
			return fields;
		}

	}

	private final Map<String, String> aliases = new HashMap<>();
	private final Map<String, Set<String>> enums = new HashMap<>();
	private final Map<String, EntityDef> entities = new HashMap<>();


	private FLEFGrammar(){}


	public static FLEFGrammar createFromPath(final Path grammarPath) throws IOException{
		try(final FileReader reader = new FileReader(grammarPath.toFile())){
			return createFromReader(reader);
		}
	}

	public static FLEFGrammar createFromContent(final String grammarContent){
		try(final StringReader reader = new StringReader(grammarContent)){
			return createFromReader(reader);
		}
		catch(final IOException e){
			throw new IllegalStateException("Unexpected error reading grammar string", e);
		}
	}

	public static FLEFGrammar createFromReader(final Reader reader) throws IOException{
		final FLEFGrammar grammar = new FLEFGrammar();
		final BufferedReader br = (reader instanceof BufferedReader? (BufferedReader)reader: new BufferedReader(reader));

		final StringBuilder sb = new StringBuilder();
		String line;
		while((line = br.readLine()) != null){
			final int commentIdx = line.indexOf("//");
			if(commentIdx != -1)
				line = line.substring(0, commentIdx);
			sb.append(line).append("\n");
		}

		grammar.parseDSL(sb.toString());
		return grammar;
	}

	private void parseDSL(final String content){
		// 1. Parse Aliases
		final Matcher aliasMatcher = Pattern.compile("alias\\s+([A-Za-z0-9_]+)\\s*=\\s*([A-Za-z0-9_]+)")
			.matcher(content);
		while(aliasMatcher.find())
			aliases.put(aliasMatcher.group(1), aliasMatcher.group(2));

		// 2. Parse standalone Enums
		final Matcher enumMatcher = Pattern.compile("enum\\s+([A-Za-z0-9_]+)\\s*\\{([^}]+)}")
			.matcher(content);
		while(enumMatcher.find()){
			final String enumName = enumMatcher.group(1);
			final String[] vals = enumMatcher.group(2).split("[,\\s]+");
			final Set<String> set = new HashSet<>();
			for(final String v : vals)
				if(!v.isBlank())
					set.add(v.trim());
			enums.put(enumName, set);
		}

		// 3. Parse Structs and Records
		final Pattern entityPattern = Pattern.compile("(struct|record)\\s+([A-Za-z0-9_]+)\\s*\\{([^}]+)}");
		final Matcher entityMatcher = entityPattern.matcher(content);

		while(entityMatcher.find()){
			final boolean isRecord = "record".equals(entityMatcher.group(1));
			final String entityName = entityMatcher.group(2);
			final String body = entityMatcher.group(3);

			final EntityDef entityDef = new EntityDef(entityName, isRecord);

			for(final String rawField : body.split("\n")){
				final String fieldLine = rawField.trim();
				if(fieldLine.isEmpty() || fieldLine.startsWith("require"))
					continue;

				final int colonIdx = fieldLine.indexOf(':');
				if(colonIdx != -1){
					final String rawName = fieldLine.substring(0, colonIdx)
						.trim();
					final String rawType = fieldLine.substring(colonIdx + 1)
						.trim();

					Cardinality card = Cardinality.EXACTLY_ONE;
					String fieldName = rawName;
					if(rawName.endsWith("?")){
						card = Cardinality.OPTIONAL;
						fieldName = rawName.substring(0, rawName.length() - 1);
					}
					else if(rawName.endsWith("*")){
						card = Cardinality.ZERO_OR_MORE;
						fieldName = rawName.substring(0, rawName.length() - 1);
					}
					else if(rawName.endsWith("+")){
						card = Cardinality.ONE_OR_MORE;
						fieldName = rawName.substring(0, rawName.length() - 1);
					}

					boolean isInlineEnum = false;
					final Set<String> inlineEnumValues = new HashSet<>();
					String typeName = rawType;

					if(rawType.startsWith("enum")){
						isInlineEnum = true;
						final int openBrace = rawType.indexOf('{');
						final int closeBrace = rawType.indexOf('}');
						if(openBrace != -1 && closeBrace != -1){
							final String[] vals = rawType.substring(openBrace + 1, closeBrace)
								.split("[,\\s]+");
							for(final String v : vals)
								if(!v.isBlank())
									inlineEnumValues.add(v.trim());
						}
						typeName = "enum";
					}

					entityDef.fields.put(fieldName, new FieldDef(fieldName, typeName, card, isInlineEnum, inlineEnumValues));
				}
			}
			entities.put(entityName, entityDef);
		}
	}

	public Map<String, String> getAliases(){
		return aliases;
	}

	public Map<String, Set<String>> getEnums(){
		return enums;
	}

	public Map<String, EntityDef> getEntities(){
		return entities;
	}

	public EntityDef getEntity(final String name){
		return entities.get(name);
	}

}
