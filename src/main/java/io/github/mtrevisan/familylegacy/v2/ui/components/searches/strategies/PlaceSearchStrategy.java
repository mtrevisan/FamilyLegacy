package io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.StringJoiner;
import java.util.function.Predicate;


/**
 * Search strategy for Place records.
 * Supports filtering by place name and type.
 */
public class PlaceSearchStrategy implements SearchStrategy{

	private static final String TAG_TYPE = "type";

	private static final double FUZZY_THRESHOLD = 0.05;


	private static final PlaceHandler HANDLER = PlaceHandler.getInstance();

	private String name;
	private String type;
	private boolean fuzzy;
	private boolean wholeWord;


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		name = criteria.getFilterFor(PlaceFilterPanel.FILTER_KEY_NAME);
		type = criteria.getFilterFor(PlaceFilterPanel.FILTER_KEY_TYPE);
		fuzzy = criteria.isFuzzy();
		wholeWord = criteria.isWholeWord();

		return place -> {
			// Name filter
			if(!SearchHelper.matchesName(place, name, fuzzy, wholeWord, FUZZY_THRESHOLD))
				return false;

			// Type filter
			if(StringUtils.isNotEmpty(type)){
				final String placeType = FLEFRecordHelper.getChildValue(place, TAG_TYPE);
				if(!type.equalsIgnoreCase(placeType))
					return false;
			}

			return true;
		};
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final String baseDisplayText = HANDLER.getDisplayText(record, model);

		final String type = FLEFRecordHelper.getChildValue(record, TAG_TYPE);

		final StringJoiner details = new StringJoiner(", ", " (", ")");
		details.setEmptyValue(StringUtils.EMPTY);

		if(StringUtils.isNotEmpty(type))
			details.add("Type: " + type);

		return baseDisplayText + details;
	}

}
