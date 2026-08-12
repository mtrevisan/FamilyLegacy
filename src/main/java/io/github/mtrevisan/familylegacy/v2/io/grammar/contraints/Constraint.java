package io.github.mtrevisan.familylegacy.v2.io.grammar.contraints;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.List;


/**
 * A structural validation rule attached to a struct/record body, introduced by the {@code require} keyword.
 */
public abstract class Constraint{

	/**
	 * Validates the given record against this constraint.
	 *
	 * @param contextPath	the current hierarchical path for error reporting
	 * @param record	the record containing the fields to evaluate
	 * @param model	the FLEF model (needed for resolving cross-references)
	 * @param errors	the list to collect validation error messages
	 */
	public abstract void validate(final String contextPath, final FLEFRecord record, final FLEFModel model,
		final List<String> errors);

}
