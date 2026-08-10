package io.github.mtrevisan.familylegacy.v2.io.grammar.contraints;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;

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
		final FLEFRecord field1 = FLEFRecordHelper.findChild(record, fields.get(0));
		final FLEFRecord field2 = FLEFRecordHelper.findChild(record, fields.get(1));
		if(field1 == null || field2 == null)
			return;

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
		if(!type1.equals(type2))
			errors.add(String.format(
				"Constraint violation at '%s': %s (%s) and %s (%s) must be of the same type",
				contextPath, fields.get(0), type1, fields.get(1), type2));
	}

	@Override
	public String toString(){
		return "require at_least_one(" + String.join(", ", fields) + ")";
	}

}
