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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * A {@code Name = oneof { choice: Type, ... }} named union.
 */
public final class UnionType extends TypeDefinition{

	private final Map<String, TypeDefinition> choices;


	public UnionType(final String name, final Map<String, TypeDefinition> choices){
		super(name);

		this.choices = Collections.unmodifiableMap(new LinkedHashMap<>(choices));
	}


	public Map<String, TypeDefinition> getChoices(){
		return choices;
	}

	@Override
	public void validate(final String contextPath, final FLEFRecord record, final FLEFModel model,
			final FLEFGrammar grammar, final List<String> errors){
		String tag = record.getTag();
		FLEFRecord targetRecord = record;

		// If the current tag is the field name itself (and not one of the union choices),
		// look at its first child to determine the selected union branch.
		if(!choices.containsKey(tag) && !record.getChildren().isEmpty()){
			final FLEFRecord firstChild = record.getTheOnlyChild();
			if(firstChild != null && !firstChild.isEmpty() && choices.containsKey(firstChild.getTag())){
				tag = firstChild.getTag();
				targetRecord = firstChild;
			}
		}

		TypeDefinition targetType = choices.get(tag);

		// Ensure the record tag matches one of the defined union branches
		if(targetType == null){
			errors.add(String.format("Invalid union choice '%s' under '%s'. Expected one of: %s, found %s, record %s",
				tag, contextPath, choices.keySet(), tag, record));

			return;
		}

		// Dereference symbolic types (ScalarType) against the grammar registry
		if(targetType instanceof ScalarType scalar){
			final TypeDefinition resolved = grammar.getType(scalar.getName());
			if(resolved != null)
				targetType = resolved;
		}

		// Delegate validation to the underlying choice record
		targetType.validate(contextPath, targetRecord, model, grammar, errors);
	}

	@Override
	public String toString(){
		return "oneof" + choices;
	}

}
