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
package io.github.mtrevisan.familylegacy.v2.io.grammar.contraints;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import org.apache.commons.lang3.Strings;

import java.util.List;


/**
 * {@code require at_least_one(fieldA, fieldB, ...)}: at least one of the listed fields MUST be present.
 */
public final class EqualTypeConstraint extends Constraint{

	private final List<String> fields;


	public EqualTypeConstraint(final List<String> fields){
		if(fields.size() != 2)
			throw new IllegalArgumentException("EqualTypeConstraint requires exactly two fields");

		this.fields = List.copyOf(fields);
	}


	public List<String> getFields(){
		return fields;
	}

	@Override
	public void validate(final String contextPath, final FLEFRecord record, final FLEFModel model,
			final List<String> errors){
		FLEFRecord field1 = FLEFRecordHelper.findChild(record, fields.get(0));
		if(field1.getChildren().size() != 1 || field1.getTheOnlyChild().isEmpty()){
			errors.add(String.format("Constraint violation at '%s': field '%s' is empty",
				contextPath, field1));

			return;
		}
		field1 = field1.getTheOnlyChild();
		FLEFRecord field2 = FLEFRecordHelper.findChild(record, fields.get(1));
		if(field2.getChildren().size() != 1 || field2.getTheOnlyChild().isEmpty()){
			errors.add(String.format("Constraint violation at '%s': field '%s' is empty",
				contextPath, field2));

			return;
		}
		field2 = field2.getTheOnlyChild();

		final String ref1 = field1.getValue();
		final String ref2 = field2.getValue();
		if(ref1 == null || ref2 == null){
			errors.add(String.format("Constraint violation at '%s': invalid reference in %s or %s",
				contextPath, fields.get(0), fields.get(1)));

			return;
		}

		final FLEFRecord target1 = model.getRecordById(ref1);
		final FLEFRecord target2 = model.getRecordById(ref2);
		if(target1 == null || target2 == null)
			return;

		final String type1 = target1.getTag();
		final String type2 = target2.getTag();
		if(!Strings.CI.equals(type1, type2))
			errors.add(String.format(
				"Constraint violation at '%s': %s (%s) and %s (%s) must be of the same type",
				contextPath, fields.get(0), type1, fields.get(1), type2));
	}

	@Override
	public String toString(){
		return "require at_least_one(" + String.join(", ", fields) + ")";
	}

}
