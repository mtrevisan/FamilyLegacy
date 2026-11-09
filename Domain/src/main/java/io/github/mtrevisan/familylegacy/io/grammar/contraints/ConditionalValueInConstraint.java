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
import java.util.Objects;


/**
 * {@code require if conditionField == conditionValue: field in (value, ...)}: when the condition holds, the value of 'field' (if it is
 * present) MUST be one of the listed values (compared ignoring case).
 * <p>
 * Example: {@code require if type == sex: value in (male, female, unknown)}
 */
public final class ConditionalValueInConstraint extends Constraint{

	private final String conditionField;
	private final String conditionValue;
	private final String field;
	private final List<String> allowedValues;


	public ConditionalValueInConstraint(final String conditionField, final String conditionValue, final String field,
			final List<String> allowedValues){
		this.conditionField = conditionField;
		this.conditionValue = conditionValue;
		this.field = field;
		this.allowedValues = List.copyOf(allowedValues);
	}


	public String getConditionField(){
		return conditionField;
	}

	public String getField(){
		return field;
	}

	public List<String> getAllowedValues(){
		return allowedValues;
	}

	@Override
	public void validate(final String contextPath, final FLEFRecord record, final RecordLookup model,
			final List<String> errors){
		final FLEFRecord condChild = FLEFRecordHelper.findChild(record, conditionField);
		if(condChild == null || !Objects.equals(condChild.getValue(), conditionValue))
			return;

		final FLEFRecord child = FLEFRecordHelper.findChild(record, field);
		final String value = (child != null? child.getValue(): null);
		if(value != null && allowedValues.stream().noneMatch(value.strip()::equalsIgnoreCase))
			errors.add(String.format("Constraint violation at '%s': field '%s' must be one of %s because '%s' is '%s' (found: %s)",
				contextPath, field, allowedValues, conditionField, conditionValue, value));
	}

	@Override
	public String toString(){
		return "require if " + conditionField + " == " + conditionValue + ": " + field + " in (" + String.join(", ", allowedValues) + ")";
	}

}
