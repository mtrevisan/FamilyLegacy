package io.github.mtrevisan.familylegacy.v2.io.grammar.ast;

import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammar;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.List;


/**
 * Base type for every kind of type usable in the grammar, either named (top-level) or anonymous (inline).
 */
public abstract class TypeDefinition{

	/**
	 * {@code null} for anonymous/inline types (e.g. an inline {@code struct { ... }} used as a field type).
	 */
	private final String name;


	protected TypeDefinition(final String name){
		this.name = name;
	}


	public String getName(){
		return name;
	}

	public boolean isAnonymous(){
		return name == null;
	}


	/**
	 * Validates the current model node against this type definition.
	 */
	public abstract void validate(String contextPath, FLEFRecord record, FLEFGrammar grammar, List<String> errors);

}
