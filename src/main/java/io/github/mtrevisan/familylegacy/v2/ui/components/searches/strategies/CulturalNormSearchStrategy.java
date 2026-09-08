package io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.TextSearchHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.ParsedGenealogicalDate;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.UniversalDateConverter;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Predicate;


/**
 * Search strategy for CulturalNorm records.
 * Supports filtering by title, rule type, location, and validity date range.
 */
public class CulturalNormSearchStrategy implements SearchStrategy{

	private static final String TAG_TITLE = "title";
	private static final String TAG_RULE_TYPE = "rule_type";
	private static final String TAG_PLACE = "place";
	private static final String TAG_DATE = "date";

	private static final double FUZZY_THRESHOLD = 0.05;


	private static final CulturalNormHandler HANDLER = CulturalNormHandler.getInstance();

	private String title;
	private String ruleType;
	private String place;
	private String validFrom;
	private String calendarFrom;
	private String validTo;
	private String calendarTo;
	private boolean fuzzy;
	private boolean wholeWord;


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		title = criteria.getFilterFor(CulturalNormFilterPanel.FILTER_KEY_TITLE);
		ruleType = criteria.getFilterFor(CulturalNormFilterPanel.FILTER_KEY_RULE_TYPE);
		place = criteria.getFilterFor(CulturalNormFilterPanel.FILTER_KEY_PLACE);
		validFrom = criteria.getFilterFor(CulturalNormFilterPanel.FILTER_KEY_VALID_FROM);
		calendarFrom = criteria.getFilterFor(CulturalNormFilterPanel.FILTER_KEY_CALENDAR_FROM);
		validTo = criteria.getFilterFor(CulturalNormFilterPanel.FILTER_KEY_VALID_TO);
		calendarTo = criteria.getFilterFor(CulturalNormFilterPanel.FILTER_KEY_CALENDAR_FROM);
		fuzzy = criteria.isFuzzy();
		wholeWord = criteria.isWholeWord();

		return culturalNorm -> {
			// Title filter
			if(StringUtils.isNotEmpty(title)){
				final String title = FLEFRecordHelper.getChildValue(culturalNorm, TAG_TITLE);
				if(!TextSearchHelper.matchesText(title, this.title, fuzzy, wholeWord, FUZZY_THRESHOLD))
					return false;
			}

			// Rule Type filter
			if(StringUtils.isNotEmpty(ruleType)){
				final String recordRuleType = FLEFRecordHelper.getChildValue(culturalNorm, TAG_RULE_TYPE);
				if(!ruleType.equalsIgnoreCase(recordRuleType))
					return false;
			}

			// Place filter
			if(StringUtils.isNotEmpty(place)){
				final FLEFRecord placeCitation = FLEFRecordHelper.findChild(culturalNorm, TAG_PLACE);
				if(placeCitation != null){
					final String placeId = placeCitation.getTheOnlyChild().getValue();
					final FLEFRecord placeRecord = model.getRecordById(placeId);
					final String place = PlaceHandler.getInstance()
						.getDisplayText(placeRecord, model);
					if(!TextSearchHelper.matchesText(place, place, fuzzy, wholeWord, FUZZY_THRESHOLD))
						return false;
				}
			}

			// Date range
			if(StringUtils.isNotEmpty(validFrom) || StringUtils.isNotEmpty(validTo)){
				final FLEFRecord validRecord = FLEFRecordHelper.findChild(culturalNorm, TAG_DATE);
				final Integer fromYear = (StringUtils.isNotEmpty(validFrom)
					? SearchHelper.extractYear(validFrom, calendarFrom)
					: null);
				final Integer toYear = (StringUtils.isNotEmpty(validTo)
					? SearchHelper.extractYear(validTo, calendarTo)
					: null);
				if(!SearchHelper.isDateInRange(validRecord, null, null, fromYear, toYear))
					return false;
			}

			return true;
		};
	}


	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final String baseDisplayText = HANDLER.getDisplayText(record, model);

		final String title = FLEFRecordHelper.getChildValue(record, TAG_TITLE);
		final String ruleType = FLEFRecordHelper.getChildValue(record, TAG_RULE_TYPE);

		final StringJoiner details = new StringJoiner(", ", " (", ")");
		details.setEmptyValue(StringUtils.EMPTY);

		if(StringUtils.isNotEmpty(title))
			details.add(title);
		if(StringUtils.isNotEmpty(ruleType))
			details.add("Type: " + ruleType);

		return baseDisplayText + details;
	}

}
