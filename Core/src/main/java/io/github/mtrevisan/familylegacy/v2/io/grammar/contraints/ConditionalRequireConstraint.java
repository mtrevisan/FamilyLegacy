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

import java.util.List;
import java.util.Objects;


/**
 * {@code require if conditionField == conditionValue: requiredField, ...}.
 */
public final class ConditionalRequireConstraint extends Constraint{

	private final String conditionField;
	private final String conditionValue;
	private final List<String> requiredFields;


	public ConditionalRequireConstraint(final String conditionField, final String conditionValue,
		final List<String> requiredFields){
		this.conditionField = conditionField;
		this.conditionValue = conditionValue;
		this.requiredFields = List.copyOf(requiredFields);
	}


	public String getConditionField(){
		return conditionField;
	}

	public String getConditionValue(){
		return conditionValue;
	}

	public List<String> getRequiredFields(){
		return requiredFields;
	}

	@Override
	public void validate(final String contextPath, final FLEFRecord record, final FLEFModel model,
			final List<String> errors){
		final FLEFRecord condChild = FLEFRecordHelper.findChild(record, conditionField);

		// Skip validation if condition field is missing or value does not match
		if(condChild == null || !Objects.equals(condChild.getValue(), conditionValue))
			return;

		// Validate mandatory presence of required fields when condition evaluates to true
		for(final String reqField : requiredFields)
			if(FLEFRecordHelper.findChild(record, reqField) == null)
				errors.add(String.format(
					"Constraint violation at '%s': field '%s' is required because '%s' is '%s'",
					contextPath, reqField, conditionField, conditionValue
				));
	}

	@Override
	public String toString(){
		return "require if " + conditionField + " == " + conditionValue + ": " + String.join(", ", requiredFields);
	}

}
