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
			errors.add(String.format("Invalid enum value '%s' at '%s'. Allowed: %s, record %s", val, contextPath, values, record));
	}

	@Override
	public String toString(){
		return "enum{" + values + (allowCustomText? " | Text": StringUtils.EMPTY) + "}";
	}

}
