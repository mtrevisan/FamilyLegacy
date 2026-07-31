package io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions;


import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammar;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.List;
import java.util.regex.Pattern;


/**
 * A reference to another named type (built-in primitive, alias, struct, record, enum, or union) by name.
 */
public final class ScalarType extends TypeDefinition{

	//FIXME this covers only ISO 8601 dates, not any other calendars!
	private static final Pattern ISO_8601_PATTERN = Pattern.compile(
		"^\\d{4}(?:-(?:0[1-9]|1[0-2])(?:-(?:0[1-9]|[12]\\d|3[01]))?)" +
			"(?:[T ](?:[01]\\d|2[0-3]):[0-5]\\d(?::[0-5]\\d(?:\\.\\d{1,9})?)?" +
			"(?:Z|[+-](?:[01]\\d|2[0-3]):?[0-5]\\d)?)?$"
	);


	public ScalarType(final String name){
		super(name);
	}


	@Override
	public void validate(final String contextPath, final FLEFRecord record, final FLEFGrammar grammar,
			final List<String> errors){
		final String tag = record.getTag();
		final String value = record.getValue();
		if("date".equalsIgnoreCase(tag) && value != null)
			if(!ISO_8601_PATTERN.matcher(value).matches())
				errors.add(String.format("Invalid date format '%s' at '%s'. Expected YYYY-MM-DD",
					value, contextPath));
	}

	@Override
	public String toString(){
		return getName();
	}

}
