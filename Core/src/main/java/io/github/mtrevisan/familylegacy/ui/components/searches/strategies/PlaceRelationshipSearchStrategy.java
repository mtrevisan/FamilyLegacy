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
package io.github.mtrevisan.familylegacy.ui.components.searches.strategies;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.ui.components.searches.SearchMode;
import io.github.mtrevisan.familylegacy.ui.components.searches.SearchStrategy;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceRelationshipHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;
import java.util.function.Predicate;


/**
 * Search strategy for {@code PlaceRelationshipRecord}.
 * <p>
 * The strategy is intentionally minimal: a place relationship is
 * meaningful only in the context of the hierarchy it belongs to, so
 * the {@code RecordSelectionDialog} bypasses the flat-list search
 * pipeline for this type and drives the hierarchy's own filters
 * instead. This class still exists because the search registry
 * requires a strategy for every record type, and because
 * {@link #getDisplayText(FLEFRecord, FLEFModel)} is used when a
 * place relationship appears in other lists (e.g. the unlink dialog).
 * <p>
 * The predicate only implements the <b>type</b> filter, which is the
 * only filter the hierarchy can apply without breaking its structure.
 * A filter on {@code subject} or {@code object} would hide the edge
 * from the tree and leave a dangling node, which is worse than
 * showing an unfiltered edge. The search text is handled by the
 * hierarchy panel through {@code setSearchFilter}, not here.
 */
public class PlaceRelationshipSearchStrategy implements SearchStrategy{

	private static final PlaceRelationshipHandler HANDLER = PlaceRelationshipHandler.getInstance();

	private String typeFilter;
	private Set<SearchMode> modes;


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		typeFilter = criteria.getFilterFor(PlaceRelationshipReader.TAG_TYPE);
		modes = criteria.modes();

		return relationship -> {
			// Type filter
			if(StringUtils.isNotEmpty(typeFilter)){
				final String type = PlaceRelationshipReader.extractType(relationship);
				if(!SearchHelper.matches(type, typeFilter, modes))
					return false;
			}

			return true;
		};
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		return HANDLER.getDisplayText(record, model);
	}

}
