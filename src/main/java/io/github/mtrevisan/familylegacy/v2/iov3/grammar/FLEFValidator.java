package io.github.mtrevisan.familylegacy.v2.iov3.grammar;

import io.github.mtrevisan.familylegacy.v2.iov3.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.iov3.model.FLEFRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Validates a {@link FLEFModel} structure against a loaded {@link FLEFGrammar}.
 */
public final class FLEFValidator{

	private final FLEFGrammar grammar;


	/**
	 * Creates a new validator with the given grammar.
	 *
	 * @param grammar	The grammar to validate against
	 */
	public static FLEFValidator create(final FLEFGrammar grammar){
		return new FLEFValidator(grammar);
	}


	private FLEFValidator(final FLEFGrammar grammar){
		this.grammar = grammar;
	}


	/**
	 * Validates the given model against the grammar.
	 *
	 * @param model	The model to validate
	 * @return	A list of validation errors; empty if the model is valid
	 */
	public List<ValidationError> validate(final FLEFModel model){
		final List<ValidationError> errors = new ArrayList<>();

		if(model.getHeader() != null)
			validateRecordAgainstEntity("Header", model.getHeader(), "header", errors);

		for(final FLEFRecord record : model.getRecords()){
			final String recordType = record.getTag();
			final FLEFGrammar.EntityDef entityDef = grammar.getEntity(recordType);

			if(entityDef == null)
				errors.add(ValidationError.create(record.getId() != null ? record.getId() : recordType,
					"Unknown record type definition in grammar: " + recordType));
			else
				validateRecordAgainstEntity(recordType, record, record.getId() != null? record.getId(): recordType, errors);
		}

		return errors;
	}

	private void validateRecordAgainstEntity(final String entityName, final FLEFRecord record, final String currentPath,
			final List<ValidationError> errors){
		final FLEFGrammar.EntityDef entityDef = grammar.getEntity(entityName);
		if(entityDef == null)
			return;

		for(final Map.Entry<String, FLEFGrammar.FieldDef> entry : entityDef.getFields().entrySet()){
			final String fieldName = entry.getKey();
			final FLEFGrammar.FieldDef fieldDef = entry.getValue();

			final List<FLEFRecord> matchingChildren = record.findChildren(fieldName);
			final int count = matchingChildren.size();

			// Validate Cardinality
			switch(fieldDef.getCardinality()){
				case EXACTLY_ONE -> {
					if(count != 1 && record.getChildValue(fieldName) == null)
						errors.add(ValidationError.create(currentPath + "." + fieldName,
							"Field is required (exactly 1 expected, found " + count + ")"));
				}
				case ONE_OR_MORE -> {
					if(count < 1)
						errors.add(ValidationError.create(currentPath + "." + fieldName,
							"Field requires at least 1 element, found 0"));
				}
			}

			// Validate Enum types if present
			if(fieldDef.isEnumInline() && count > 0){
				for(final FLEFRecord child : matchingChildren){
					final String val = child.getValue();
					if(val != null && !fieldDef.getInlineEnumValues().contains(val))
						errors.add(ValidationError.create(currentPath + "." + fieldName,
							"Value '" + val + "' is not valid for enum " + fieldDef.getInlineEnumValues()));
				}
			}

			// Recursive validation for struct types
			if(grammar.getEntities().containsKey(fieldDef.getTypeName()))
				for(final FLEFRecord child : matchingChildren)
					validateRecordAgainstEntity(fieldDef.getTypeName(), child, currentPath + "." + fieldName,
						errors);
		}
	}

}
