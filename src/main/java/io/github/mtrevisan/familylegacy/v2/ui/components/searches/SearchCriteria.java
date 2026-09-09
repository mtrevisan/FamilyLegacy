/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
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

	private final RecordTypeHandler<?> handler;

	private final String searchText;
	private final boolean fuzzy;
	private final boolean wholeWord;

	private final Map<String, Object> specificFilters = new HashMap<>();


	public SearchCriteria(final RecordTypeHandler<?> handler, final String searchText, final boolean fuzzy,
			final boolean wholeWord){
		this.handler = handler;

		this.searchText = searchText;
		this.fuzzy = fuzzy;
		this.wholeWord = wholeWord;
	}

	public RecordTypeHandler<?> getHandler(){
		return handler;
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
	public <T> T getFilterFor(final String key){
		return (T)specificFilters.get(key);
	}

	public Map<String, Object> getSpecificFilters(){
		return Collections.unmodifiableMap(specificFilters);
	}

}
