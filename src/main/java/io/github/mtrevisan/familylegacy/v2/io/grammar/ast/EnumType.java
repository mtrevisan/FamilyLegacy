package io.github.mtrevisan.familylegacy.v2.io.grammar.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public final class EnumType extends TypeDefinition{

	private final List<String> values;
	/**
	 * Whether the enum is followed by {@code | Text}, i.e. custom/free-text values are also allowed.
	 */
	private final boolean allowCustomText;


	public EnumType(final String name, final List<String> values, final boolean allowCustomText){
		super(name);
		this.values = Collections.unmodifiableList(new ArrayList<>(values));
		this.allowCustomText = allowCustomText;
	}


	public List<String> getValues(){
		return values;
	}

	public boolean isAllowCustomText(){
		return allowCustomText;
	}

	@Override
	public String toString(){
		return "enum{" + values + (allowCustomText? " | Text": "") + "}";
	}

}
