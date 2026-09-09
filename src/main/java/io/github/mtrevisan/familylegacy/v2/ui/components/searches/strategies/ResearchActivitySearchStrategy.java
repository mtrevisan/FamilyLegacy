package io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.TextSearchHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchActivityHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.StringJoiner;
import java.util.function.Predicate;


/**
 * Search strategy for ResearchActivity records.
 * Supports filtering by activity type, status, action, result, and observation.
 */
public class ResearchActivitySearchStrategy implements SearchStrategy{

	private static final String TAG_ACTIVITY_TYPE = "activity_type";
	private static final String TAG_STATUS = "status";
	private static final String TAG_ACTION = "action";
	private static final String TAG_RESULT = "result";
	private static final String TAG_OBSERVATION = "observation";

	private static final double FUZZY_THRESHOLD = 0.05;

	private static final ResearchActivityHandler HANDLER = ResearchActivityHandler.getInstance();

	private String activityType;
	private String status;
	private String action;
	private String result;
	private String observation;
	private boolean fuzzy;
	private boolean wholeWord;


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		activityType = criteria.getFilterFor(ResearchActivityFilterPanel.FILTER_KEY_ACTIVITY_TYPE);
		status = criteria.getFilterFor(ResearchActivityFilterPanel.FILTER_KEY_STATUS);
		action = criteria.getFilterFor(ResearchActivityFilterPanel.FILTER_KEY_ACTION);
		result = criteria.getFilterFor(ResearchActivityFilterPanel.FILTER_KEY_RESULT);
		observation = criteria.getFilterFor(ResearchActivityFilterPanel.FILTER_KEY_OBSERVATION);
		fuzzy = criteria.isFuzzy();
		wholeWord = criteria.isWholeWord();

		return activity -> {
			// Activity Type filter
			if(StringUtils.isNotEmpty(activityType)){
				final String recordType = FLEFRecordHelper.getChildValue(activity, TAG_ACTIVITY_TYPE);
				if(!activityType.equalsIgnoreCase(recordType))
					return false;
			}

			// Status filter
			if(StringUtils.isNotEmpty(status)){
				final String recordStatus = FLEFRecordHelper.getChildValue(activity, TAG_STATUS);
				if(!status.equalsIgnoreCase(recordStatus))
					return false;
			}

			// Action filter
			if(StringUtils.isNotEmpty(action)){
				final String action = FLEFRecordHelper.getChildValue(activity, TAG_ACTION);
				if(!TextSearchHelper.matchesText(action, this.action, fuzzy, wholeWord, FUZZY_THRESHOLD))
					return false;
			}

			// Result filter
			if(StringUtils.isNotEmpty(result)){
				final String recordResult = FLEFRecordHelper.getChildValue(activity, TAG_RESULT);
				if(!result.equalsIgnoreCase(recordResult))
					return false;
			}

			// Observation filter
			if(StringUtils.isNotEmpty(observation)){
				final String observation = FLEFRecordHelper.getChildValue(activity, TAG_OBSERVATION);
				if(!TextSearchHelper.matchesText(observation, this.observation, fuzzy, wholeWord, FUZZY_THRESHOLD))
					return false;
			}

			return true;
		};
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final String baseDisplayText = HANDLER.getDisplayText(record, model);

		final String type = FLEFRecordHelper.getChildValue(record, TAG_ACTIVITY_TYPE);
		final String status = FLEFRecordHelper.getChildValue(record, TAG_STATUS);
		final String result = FLEFRecordHelper.getChildValue(record, TAG_RESULT);

		final StringJoiner details = new StringJoiner(", ", " (", ")");
		details.setEmptyValue(StringUtils.EMPTY);

		if(StringUtils.isNotEmpty(type))
			details.add("Type: " + type);
		if(StringUtils.isNotEmpty(status))
			details.add("Status: " + status);
		if(StringUtils.isNotEmpty(result))
			details.add("Result: " + result);

		return baseDisplayText + details;
	}

}
