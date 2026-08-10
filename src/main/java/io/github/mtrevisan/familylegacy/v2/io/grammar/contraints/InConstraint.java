package io.github.mtrevisan.familylegacy.v2.io.grammar.contraints;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;

import java.util.List;


/**
 * {@code require field in container}:
 * the value of 'field' must be among the values of 'container'.
 * Esempio: {@code require preferred in resolves}
 */
public final class InConstraint extends Constraint{

	private final String field;
	private final String containerField;


	public InConstraint(final String field, final String containerField){
		this.field = field;
		this.containerField = containerField;
	}


	public String getField(){
		return field;
	}

	public String getContainerField(){
		return containerField;
	}

	@Override
	public void validate(final String contextPath, final FLEFRecord record, final FLEFModel model,
			final List<String> errors){
		final FLEFRecord fieldChild = FLEFRecordHelper.findChild(record, field);
		if(fieldChild == null)
			return;

		final FLEFRecord containerChild = FLEFRecordHelper.findChild(record, containerField);
		if(containerChild == null){
			errors.add(String.format("Constraint violation at '%s': container field '%s' not found",
				contextPath, containerField));

			return;
		}

		final String fieldValue = fieldChild.getValue();
		if(fieldValue == null || fieldValue.isEmpty()){
			errors.add(String.format("Constraint violation at '%s': field '%s' has no value",
				contextPath, field));

			return;
		}

		boolean found = false;
		for(final FLEFRecord child : containerChild.getChildren())
			if(fieldValue.equals(child.getValue())){

				found = true;
				break;
			}

		if(!found)
			errors.add(String.format("Constraint violation at '%s': '%s' (value: %s) must be one of the values in '%s'",
				contextPath, field, fieldValue, containerField));
	}

	@Override
	public String toString(){
		return "require " + field + " in " + containerField;
	}

}
