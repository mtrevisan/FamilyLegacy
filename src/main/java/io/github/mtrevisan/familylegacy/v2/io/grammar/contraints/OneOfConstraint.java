package io.github.mtrevisan.familylegacy.v2.io.grammar.contraints;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.List;


/**
 * {@code require one_of(fieldA, fieldB, ...)}: at least one of the listed fields MUST be present.
 */
public final class OneOfConstraint extends Constraint{

	private final List<String> fields;


	public OneOfConstraint(final List<String> fields){
		this.fields = List.copyOf(fields);
	}


	public List<String> getFields(){
		return fields;
	}

	@Override
	public void validate(final String contextPath, final FLEFRecord record, final List<String> errors){
		int count = 0;
		for(final String fieldName : fields)
			if(record.findChild(fieldName) != null)
				count ++;

		if(count != 1)
			errors.add(String.format(
				"Constraint violation at '%s': expected exactly one of %s, but found %d",
				contextPath, fields, count
			));
	}

	@Override
	public String toString(){
		return "require one_of(" + String.join(", ", fields) + ")";
	}

}
