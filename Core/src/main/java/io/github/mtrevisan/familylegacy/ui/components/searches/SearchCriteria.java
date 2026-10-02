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
package io.github.mtrevisan.familylegacy.ui.components.searches;

import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;


/**
 * Immutable criteria of a record search: the record type, the free-text
 * query, the admission mode, and the structural filters (sex, place,
 * date range, ...) set by the dynamic filter panel.
 * <p>
 * The mode is a {@link SearchMode} rather than a pair of booleans, so the
 * matcher in {@link SearchService} reads a single value and the dialog
 * exposes exactly one selected mode at a time.
 */
public final class SearchCriteria{

	private final RecordTypeHandler<?> handler;
	private final String query;
	private final SearchMode mode;
	private final Map<String, String> filters = new LinkedHashMap<>();


	public SearchCriteria(final RecordTypeHandler<?> handler, final String query, final SearchMode mode){
		this.handler = Objects.requireNonNull(handler, "handler must not be null");
		this.query = (query != null? query: StringUtils.EMPTY);
		this.mode = (mode != null? mode: SearchMode.FUZZY);
	}


	public RecordTypeHandler<?> handler(){
		return handler;
	}

	public String query(){
		return query;
	}

	public SearchMode mode(){
		return mode;
	}

	/**
	 * Registers a structural filter. The key and value are opaque to this
	 * class; the handler interprets them.
	 *
	 * @param key   the filter key (e.g., "sex", "place")
	 * @param value the filter value
	 * @return this criteria, for chaining
	 */
	public SearchCriteria withFilter(final String key, final String value){
		if(key != null && value != null)
			filters.put(key, value);

		return this;
	}

	public Map<String, String> filters(){
		return filters;
	}

	public String filter(final String key){
		return filters.get(key);
	}

	public boolean hasFilter(final String key){
		return filters.containsKey(key);
	}

	/**
	 * Returns the value of a type‑specific filter.
	 *
	 * @param key the filter key
	 * @return the value, or {@code null} if not present
	 */
	public String getFilterFor(final String key){
		return filters.get(key);
	}


	@Override
	public String toString(){
		return "SearchCriteria["
			+ "handler=" + handler.getType()
			+ ", query='" + query + '\''
			+ ", mode=" + mode
			+ ", filters=" + filters
			+ ']';
	}

}
