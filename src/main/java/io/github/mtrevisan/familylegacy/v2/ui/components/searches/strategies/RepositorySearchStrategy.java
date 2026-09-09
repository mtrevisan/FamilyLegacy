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
					if(!TextSearchHelper.matchesText(place, place, fuzzy, wholeWord, FUZZY_THRESHOLD))
						return false;
				}
			}

			return true;
		};
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final String baseDisplayText = HANDLER.getDisplayText(record, model);

		final String place = SearchHelper.extractPlace(record, model);

		final StringJoiner details = new StringJoiner(", ", " (", ")");
		details.setEmptyValue(StringUtils.EMPTY);

		if(StringUtils.isNotEmpty(place))
			details.add(" - " + place);

		return baseDisplayText + details;
	}

}
