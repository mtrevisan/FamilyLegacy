package io.github.mtrevisan.familylegacy.v2.io.grammar.contraints;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.ArrayList;
import java.util.Collections;
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
	public void validate(final String contextPath, final FLEFRecord record, final List<String> errors){
		final FLEFRecord condChild = record.findChild(conditionField);

		// Skip validation if condition field is missing or value does not match
		if(condChild == null || !Objects.equals(condChild.getValue(), conditionValue))
			return;

		// Validate mandatory presence of required fields when condition evaluates to true
		for(final String reqField : requiredFields)
			if(record.findChild(reqField) == null)
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
