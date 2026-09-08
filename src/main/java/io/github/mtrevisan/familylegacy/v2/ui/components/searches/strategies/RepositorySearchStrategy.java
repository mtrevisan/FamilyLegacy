package io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.TextSearchHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.StringJoiner;
import java.util.function.Predicate;


/* TODO */
/**
 * Search strategy for Repository records.
 * Supports filtering by place/location and email contact information.
 */
public class RepositorySearchStrategy implements SearchStrategy{

	private static final String DOT = ".";

	private static final String TAG_PLACE = "place";
	private static final String TAG_ADDRESS = "address";
	private static final String TAG_EMAIL = "email";
	private static final String TAG_VALUE = "value";

	private static final String TAG_PLACE_PLACE = TAG_PLACE + DOT + TAG_PLACE;

	private static final double FUZZY_THRESHOLD = 0.05;

	private static final RepositoryHandler HANDLER = RepositoryHandler.getInstance();

	private String locationContains;
	private String emailContains;
	private boolean fuzzy;
	private boolean wholeWord;


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		locationContains = criteria.getFilterFor("locationContains");
		emailContains = criteria.getFilterFor("emailContains");
		fuzzy = criteria.isFuzzy();
		wholeWord = criteria.isWholeWord();

		return repository -> {
			// Location / Place filter
			if(StringUtils.isNotEmpty(locationContains)){
				final String placeRef = FLEFRecordHelper.getChildValue(repository, TAG_PLACE_PLACE);
				boolean matched = false;

				if(placeRef != null){
					final FLEFRecord placeRecord = model.getRecordById(placeRef);
					if(placeRecord != null){
						final String placeDisplayText = PlaceHandler.getInstance().getDisplayText(placeRecord, model);
						matched = TextSearchHelper.matchesText(placeDisplayText, locationContains, fuzzy, wholeWord, FUZZY_THRESHOLD);
					}
				}

				if(!matched){
					final String addressVal = FLEFRecordHelper.getChildValue(repository, TAG_ADDRESS + DOT + TAG_VALUE);
					if(!TextSearchHelper.matchesText(addressVal, locationContains, fuzzy, wholeWord, FUZZY_THRESHOLD)){
						return false;
					}
				}
			}

			// Email filter
			if(StringUtils.isNotEmpty(emailContains)){
				final String emailVal = FLEFRecordHelper.getChildValue(repository, TAG_EMAIL + DOT + TAG_VALUE);
				if(!TextSearchHelper.matchesText(emailVal, emailContains, fuzzy, wholeWord, FUZZY_THRESHOLD)){
					return false;
				}
			}

			return true;
		};
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final String baseDisplayText = HANDLER.getDisplayText(record, model);

		final String emailVal = FLEFRecordHelper.getChildValue(record, TAG_EMAIL + DOT + TAG_VALUE);
		final String placeRef = FLEFRecordHelper.getChildValue(record, TAG_PLACE_PLACE);

		final StringJoiner details = new StringJoiner(", ", " (", ")");
		details.setEmptyValue(StringUtils.EMPTY);

		if(placeRef != null){
			final FLEFRecord placeRecord = model.getRecordById(placeRef);
			if(placeRecord != null){
				details.add(PlaceHandler.getInstance().getDisplayText(placeRecord, model));
			}
		}
		if(StringUtils.isNotEmpty(emailVal)){
			details.add("✉ " + emailVal);
		}

		return baseDisplayText + details;
	}

}
