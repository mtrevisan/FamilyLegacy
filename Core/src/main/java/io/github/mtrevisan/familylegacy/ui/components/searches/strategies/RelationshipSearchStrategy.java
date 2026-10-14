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
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.ui.components.searches.SearchMode;
import io.github.mtrevisan.familylegacy.ui.components.searches.SearchStrategy;
import io.github.mtrevisan.familylegacy.ui.handlers.RelationshipHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;
import java.util.function.Predicate;


/**
 * Search strategy for Relationship records.
 */
public class RelationshipSearchStrategy implements SearchStrategy{

	private static final double FUZZY_THRESHOLD = 0.05;


	private static final RelationshipHandler HANDLER = RelationshipHandler.getInstance();

	private String typeFilter;
	private String roleFilter;
	private Set<SearchMode> modes;


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		typeFilter = criteria.getFilterFor(RelationshipReader.TAG_TYPE);
		roleFilter = criteria.getFilterFor(RelationshipReader.TAG_ROLE);
		modes = criteria.modes();

		return relationship -> {
			// Type filter
			if(StringUtils.isNotEmpty(typeFilter)){
				final String type = RelationshipReader.extractType(relationship);
				if(!SearchHelper.matches(type, typeFilter, modes))
					return false;
			}

			// Role filter
			final String roleRecord = FLEFRecordHelper.getChildValue(relationship, RelationshipReader.TAG_ROLE);
			if(!SearchHelper.matches(roleRecord, roleFilter, modes, FUZZY_THRESHOLD))
				return false;

			return true;
		};
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		return HANDLER.getDisplayText(record, model);
	}

}
