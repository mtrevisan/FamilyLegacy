package io.github.mtrevisan.familylegacy.v2.io.grammar;

import io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions.TypeDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * In-memory representation of a parsed FLEF (.gedg) grammar file.
 * <p>
 * A grammar is composed of exactly one {@link FileDefinition} (the {@code file ... { ... }} statement) plus a set of
 * named top-level {@link TypeDefinition}s: scalar aliases, enums, structs, records, and {@code oneof} unions.
 */
public final class FLEFGrammar{

	/**
	 * Primitive terminal types that RHS of an {@code alias} may resolve to; they are not themselves defined types.
	 */
	public static final Set<String> PRIMITIVE_TYPES = Set.of("string", "bool", "int");


	private final FileDefinition fileDefinition;
	private final Map<String, TypeDefinition> types;
	private final List<String> parseWarnings;


	public FLEFGrammar(final FileDefinition fileDefinition,
			final Map<String, TypeDefinition> types, final List<String> parseWarnings){
		this.fileDefinition = fileDefinition;
		this.types = Collections.unmodifiableMap(new LinkedHashMap<>(types));
		this.parseWarnings = List.copyOf(parseWarnings);
	}


	public FileDefinition getFileDefinition(){
		return fileDefinition;
	}

	public TypeDefinition getType(final String name){
		return types.get(name);
	}

	public boolean hasType(final String name){
		return types.containsKey(name);
	}

	public Set<String> getTypeNames(){
		return types.keySet();
	}

	public Map<String, TypeDefinition> getTypes(){
		return types;
	}

	/**
	 * Non-fatal issues detected while parsing (e.g. duplicate type definitions).
	 */
	public List<String> getParseWarnings(){
		return parseWarnings;
	}

}
