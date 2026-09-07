package io.github.mtrevisan.familylegacy.v2.ui.components.searches;

import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * Container for all search parameters.
 * Generic text filters are always available; type‑specific filters are stored in a map.
 */
public class SearchCriteria{

	private final RecordTypeHandler<?> handlerType;
	private final String searchText;
	private final boolean fuzzy;
	private final boolean wholeWord;
	private final Map<String, Object> specificFilters = new HashMap<>();

	public SearchCriteria(final RecordTypeHandler<?> handlerType,
		final String searchText,
		final boolean fuzzy,
		final boolean wholeWord){
		this.handlerType = handlerType;
		this.searchText = searchText;
		this.fuzzy = fuzzy;
		this.wholeWord = wholeWord;
	}

	public RecordTypeHandler<?> getHandlerType(){
		return handlerType;
	}

	public String getSearchText(){
		return searchText;
	}

	public boolean isFuzzy(){
		return fuzzy;
	}

	public boolean isWholeWord(){
		return wholeWord;
	}

	/**
	 * Adds a type‑specific filter.
	 *
	 * @param key   the filter key (e.g., "eventType", "dateFrom")
	 * @param value the filter value
	 * @return this instance for chaining
	 */
	public SearchCriteria withFilter(final String key, final Object value){
		specificFilters.put(key, value);
		return this;
	}

	/**
	 * Returns the value of a type‑specific filter.
	 *
	 * @param key the filter key
	 * @param <T> the expected type
	 * @return the value, or null if not present
	 */
	@SuppressWarnings("unchecked")
	public <T> T getFilter(final String key){
		return (T)specificFilters.get(key);
	}

	public Map<String, Object> getSpecificFilters(){
		return Collections.unmodifiableMap(specificFilters);
	}

}
