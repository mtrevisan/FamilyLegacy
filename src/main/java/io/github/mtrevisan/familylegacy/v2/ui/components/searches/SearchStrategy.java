package io.github.mtrevisan.familylegacy.v2.ui.components.searches;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.function.Predicate;


/**
 * Strategy for filtering records of a specific type based on search criteria.
 */
public interface SearchStrategy {

	/**
	 * Builds a predicate that tests whether a record matches the given criteria.
	 *
	 * @param criteria the search criteria
	 * @return a predicate for filtering records
	 */
	Predicate<FLEFRecord> buildPredicate(SearchCriteria criteria, FLEFModel model);

	String getDisplayText(FLEFRecord record, FLEFModel model);

}
