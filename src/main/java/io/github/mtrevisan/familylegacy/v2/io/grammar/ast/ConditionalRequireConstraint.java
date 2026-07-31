package io.github.mtrevisan.familylegacy.v2.io.grammar.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


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
		this.requiredFields = Collections.unmodifiableList(new ArrayList<>(requiredFields));
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
	public String toString(){
		return "require if " + conditionField + " == " + conditionValue + ": " + String.join(", ", requiredFields);
	}

}
