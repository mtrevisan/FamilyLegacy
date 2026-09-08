package io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.TextSearchHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.StringJoiner;
import java.util.function.Predicate;


/* TODO */
/**
 * Search strategy for CulturalNorm records.
 * Supports filtering by norm type, associated location/region, and time period.
 */
public class CulturalNormSearchStrategy implements SearchStrategy{

	private static final String DOT = ".";

	private static final String TAG_TYPE = "type";
	private static final String TAG_PLACE = "place";
	private static final String TAG_TIME_PERIOD = "time_period";
	private static final String TAG_VALUE = "value";

	private static final String TAG_PLACE_PLACE = TAG_PLACE + DOT + TAG_PLACE;

	private static final double FUZZY_THRESHOLD = 0.05;

	private static final CulturalNormHandler HANDLER = CulturalNormHandler.getInstance();

	private String normType;
	private String locationContains;
	private String timePeriodContains;
	private boolean fuzzy;
	private boolean wholeWord;


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		normType = criteria.getFilterFor("normType");
		locationContains = criteria.getFilterFor("locationContains");
		timePeriodContains = criteria.getFilterFor("timePeriodContains");
		fuzzy = criteria.isFuzzy();
		wholeWord = criteria.isWholeWord();

		return norm -> {
			// Norm type filter
			if(StringUtils.isNotEmpty(normType)){
				final String recordType = FLEFRecordHelper.getChildValue(norm, TAG_TYPE);
				if(!normType.equalsIgnoreCase(recordType)){
					return false;
				}
			}

			// Location / Region filter
			if(StringUtils.isNotEmpty(locationContains)){
				final String placeRef = FLEFRecordHelper.getChildValue(norm, TAG_PLACE_PLACE);
				if(placeRef != null){
					final FLEFRecord placeRecord = model.getRecordById(placeRef);
					if(placeRecord != null){
						final String placeDisplayText = PlaceHandler.getInstance().getDisplayText(placeRecord, model);
						if(!TextSearchHelper.matchesText(placeDisplayText, locationContains, fuzzy, wholeWord, FUZZY_THRESHOLD)){
							return false;
						}
					}
					else{
						return false;
					}
				}
				else{
					return false;
				}
			}

			// Time period text filter
			if(StringUtils.isNotEmpty(timePeriodContains)){
				final String periodVal = FLEFRecordHelper.getChildValue(norm, TAG_TIME_PERIOD + DOT + TAG_VALUE);
				if(!TextSearchHelper.matchesText(periodVal, timePeriodContains, fuzzy, wholeWord, FUZZY_THRESHOLD)){
					return false;
				}
			}

			return true;
		};
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final String baseDisplayText = HANDLER.getDisplayText(record, model);

		final String type = FLEFRecordHelper.getChildValue(record, TAG_TYPE);
		final String period = FLEFRecordHelper.getChildValue(record, TAG_TIME_PERIOD + DOT + TAG_VALUE);

		final StringJoiner details = new StringJoiner(", ", " (", ")");
		details.setEmptyValue(StringUtils.EMPTY);

		if(StringUtils.isNotEmpty(type)){
			details.add(type);
		}
		if(StringUtils.isNotEmpty(period)){
			details.add("🕒 " + period);
		}

		return baseDisplayText + details;
	}

}
