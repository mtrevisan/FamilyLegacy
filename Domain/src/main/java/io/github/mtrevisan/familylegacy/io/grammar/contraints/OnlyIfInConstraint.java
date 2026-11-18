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
package io.github.mtrevisan.familylegacy.io.grammar.contraints;

import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.record.RecordLookup;

import java.util.List;


/**
 * {@code require field only if conditionField in (value, ...)}: 'field' MAY be present only if the value of 'conditionField' is among the
 * listed values.
 * <p>
 * Example: {@code require mapping only if kind in (image, document, video)}
 */
public final class OnlyIfInConstraint extends Constraint{

	private final String field;
	private final String conditionField;
	private final List<String> allowedValues;


	public OnlyIfInConstraint(final String field, final String conditionField, final List<String> allowedValues){
		this.field = field;
		this.conditionField = conditionField;
		this.allowedValues = List.copyOf(allowedValues);
	}


	public String getField(){
		return field;
	}

	public String getConditionField(){
		return conditionField;
	}

	public List<String> getAllowedValues(){
		return allowedValues;
	}

	@Override
	public void validate(final String contextPath, final FLEFRecord record, final RecordLookup model,
			final List<String> errors){
		//field not present: nothing to check
		if(FLEFRecordHelper.findChild(record, field) == null)
			return;

		final FLEFRecord condChild = FLEFRecordHelper.findChild(record, conditionField);
		final String conditionValue = (condChild != null? condChild.getValue(): null);
		if(conditionValue == null || !allowedValues.contains(conditionValue))
			errors.add(String.format(
				"Constraint violation at '%s': field '%s' is allowed only if '%s' is one of %s (found: %s), record %s",
				contextPath, field, conditionField, allowedValues, conditionValue, record));
	}

	@Override
	public String toString(){
		return "require " + field + " only if " + conditionField + " in (" + String.join(", ", allowedValues) + ")";
	}

}
