package io.github.mtrevisan.familylegacy.v2.io.grammar.contraints;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;

import java.util.List;


/**
 * {@code require at_least_one(fieldA, fieldB, ...)}: at least one of the listed fields MUST be present.
 */
public final class AtLeastOneConstraint extends Constraint{

	private final List<String> fields;


	public AtLeastOneConstraint(final List<String> fields){
		this.fields = List.copyOf(fields);
	}


	public List<String> getFields(){
		return fields;
	}

	@Override
	public void validate(final String contextPath, final FLEFRecord record, final FLEFModel model,
			final List<String> errors){
		int count = 0;
		for(final String fieldName : fields)
			if(FLEFRecordHelper.findChild(record, fieldName) != null)
				count ++;

		if(count == 0)
			errors.add(String.format(
				"Constraint violation at '%s': expected exactly one of %s, but found %d",
				contextPath, fields, count
			));
	}

	@Override
	public String toString(){
		return "require at_least_one(" + String.join(", ", fields) + ")";
	}

}
