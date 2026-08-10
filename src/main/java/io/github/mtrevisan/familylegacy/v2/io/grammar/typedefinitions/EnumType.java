package io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions;

import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammar;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Locale;


public final class EnumType extends TypeDefinition{

	private final List<String> values;
	/**
	 * Whether the enum is followed by {@code | Text}, i.e. custom/free-text values are also allowed.
	 */
	private final boolean allowCustomText;


	public EnumType(final String name, final List<String> values, final boolean allowCustomText){
		super(name);

		this.values = List.copyOf(values);
		this.allowCustomText = allowCustomText;
	}


	public List<String> getValues(){
		return values;
	}

	public boolean isAllowCustomText(){
		return allowCustomText;
	}

	@Override
	public void validate(final String contextPath, final FLEFRecord record, final FLEFModel model,
			final FLEFGrammar grammar, final List<String> errors){
		final String val = record.getValue();
		if(val != null && !values.contains(val.toLowerCase(Locale.ROOT)) && !allowCustomText)
			errors.add(String.format("Invalid enum value '%s' at '%s'. Allowed: %s", val, contextPath, values));
	}

	@Override
	public String toString(){
		return "enum{" + values + (allowCustomText? " | Text": StringUtils.EMPTY) + "}";
	}

}
