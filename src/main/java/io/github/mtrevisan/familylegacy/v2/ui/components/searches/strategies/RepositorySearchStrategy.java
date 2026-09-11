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
package io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.TextSearchHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.StringJoiner;
import java.util.function.Predicate;


/**
 * Search strategy for Repository records.
 * Supports filtering by repository name, custodian reference, and location.
 */
public class RepositorySearchStrategy implements SearchStrategy{

	private static final String TAG_CUSTODIAN = "custodian";
	private static final String TAG_PLACE = "place";

	private static final double FUZZY_THRESHOLD = 0.05;


	private static final RepositoryHandler HANDLER = RepositoryHandler.getInstance();


	private String name;
	private String custodian;
	private String location;
	private boolean fuzzy;
	private boolean wholeWord;


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		name = criteria.getFilterFor(RepositoryFilterPanel.FILTER_KEY_NAME);
		custodian = criteria.getFilterFor(RepositoryFilterPanel.FILTER_KEY_CUSTODIAN);
		location = criteria.getFilterFor(RepositoryFilterPanel.FILTER_KEY_LOCATION);
		fuzzy = criteria.isFuzzy();
		wholeWord = criteria.isWholeWord();

		return repository -> {
			// Name filter
			if(!SearchHelper.matchesName(repository, name, fuzzy, wholeWord, FUZZY_THRESHOLD))
				return false;

			// Custodian filter
			if(StringUtils.isNotEmpty(custodian)){
				final String custodianRef = FLEFRecordHelper.getChildValue(repository, TAG_CUSTODIAN);
				if(custodianRef != null){
					final FLEFRecord custodianRecord = model.getRecordById(custodianRef);
					if(custodianRecord != null){
						final String custodianDisplayText = IndividualHandler.getInstance().getDisplayText(custodianRecord, model);
						if(!TextSearchHelper.matchesText(custodianDisplayText, custodian, fuzzy, wholeWord, FUZZY_THRESHOLD))
							return false;
					}
					else
						return false;
				}
				else
					return false;
			}

			// Location filter
			if(StringUtils.isNotEmpty(location)){
				final FLEFRecord placeCitation = FLEFRecordHelper.findChild(repository, TAG_PLACE);
				if(placeCitation != null){
					final String placeId = placeCitation.getTheOnlyChild().getValue();
					final FLEFRecord placeRecord = model.getRecordById(placeId);
					final String place = PlaceHandler.getInstance()
						.getDisplayText(placeRecord, model);
					return TextSearchHelper.matchesText(place, place, fuzzy, wholeWord, FUZZY_THRESHOLD);
				}
			}

			return true;
		};
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final String baseDisplayText = HANDLER.getDisplayText(record, model);

		final String place = FLEFRecordHelper.extractPlace(record, model);

		final StringJoiner details = new StringJoiner(", ", " (", ")");
		details.setEmptyValue(StringUtils.EMPTY);

		if(StringUtils.isNotEmpty(place))
			details.add(" - " + place);

		return baseDisplayText + details;
	}

}
