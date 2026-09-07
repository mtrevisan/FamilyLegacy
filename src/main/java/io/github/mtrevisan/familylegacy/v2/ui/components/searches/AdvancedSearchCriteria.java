package io.github.mtrevisan.familylegacy.v2.ui.components.searches;

import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;


/**
 * Advanced search criteria.
 *
 * @param handlerType      the record type handler (e.g., IndividualHandler)
 * @param searchText       the text to search for
 * @param fuzzy            enable fuzzy (trigram) matching
 * @param wholeWord        enable whole‑word matching
 * @param eventType        filter by event type (e.g., "birth", "death"), null for any
 * @param dateFrom         lower bound of date range (inclusive), null for no bound
 * @param dateTo           upper bound of date range (inclusive), null for no bound
 * @param locationContains substring to match in event location (case‑insensitive), null for any
 */
public record AdvancedSearchCriteria(
	RecordTypeHandler<?> handlerType,
	String searchText,
	boolean fuzzy,
	boolean wholeWord,
	String eventType,
	String dateFrom,
	String dateTo,
	String locationContains
){
}
