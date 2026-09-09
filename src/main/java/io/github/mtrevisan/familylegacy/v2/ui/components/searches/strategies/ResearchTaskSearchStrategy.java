package io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.TextSearchHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchTaskHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.StringJoiner;
import java.util.function.Predicate;


/**
 * Search strategy for ResearchTask records.
 * Supports filtering by task description, status, priority, and outcome notes.
 */
public class ResearchTaskSearchStrategy implements SearchStrategy{

	private static final String TAG_DESCRIPTION = "description";
	private static final String TAG_STATUS = "status";
	private static final String TAG_PRIORITY = "priority";
	private static final String TAG_OUTCOME = "outcome";

	private static final double FUZZY_THRESHOLD = 0.05;

	private static final ResearchTaskHandler HANDLER = ResearchTaskHandler.getInstance();

	private String description;
	private String status;
	private String priority;
	private String outcome;
	private boolean fuzzy;
	private boolean wholeWord;


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		description = criteria.getFilterFor(ResearchTaskFilterPanel.FILTER_KEY_DESCRIPTION);
		status = criteria.getFilterFor(ResearchTaskFilterPanel.FILTER_KEY_STATUS);
		priority = criteria.getFilterFor(ResearchTaskFilterPanel.FILTER_KEY_PRIORITY);
		outcome = criteria.getFilterFor(ResearchTaskFilterPanel.FILTER_KEY_OUTCOME);
		fuzzy = criteria.isFuzzy();
		wholeWord = criteria.isWholeWord();

		return task -> {
			// Description filter
			if(StringUtils.isNotEmpty(description)){
				final String description = FLEFRecordHelper.getChildValue(task, TAG_DESCRIPTION);
				if(!TextSearchHelper.matchesText(description, this.description, fuzzy, wholeWord, FUZZY_THRESHOLD))
					return false;
			}

			// Status filter
			if(StringUtils.isNotEmpty(status)){
				final String recordStatus = FLEFRecordHelper.getChildValue(task, TAG_STATUS);
				if(!status.equalsIgnoreCase(recordStatus))
					return false;
			}

			// Priority filter
			if(StringUtils.isNotEmpty(priority)){
				final String recordPriority = FLEFRecordHelper.getChildValue(task, TAG_PRIORITY);
				if(!priority.equalsIgnoreCase(recordPriority))
					return false;
			}

			// Outcome filter
			if(StringUtils.isNotEmpty(outcome)){
				final String outcome = FLEFRecordHelper.getChildValue(task, TAG_OUTCOME);
				if(!TextSearchHelper.matchesText(outcome, this.outcome, fuzzy, wholeWord, FUZZY_THRESHOLD))
					return false;
			}

			return true;
		};
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final String baseDisplayText = HANDLER.getDisplayText(record, model);

		final String status = FLEFRecordHelper.getChildValue(record, TAG_STATUS);
		final String priority = FLEFRecordHelper.getChildValue(record, TAG_PRIORITY);

		final StringJoiner details = new StringJoiner(", ", " (", ")");
		details.setEmptyValue(StringUtils.EMPTY);

		if(StringUtils.isNotEmpty(status))
			details.add("Status: " + status);
		if(StringUtils.isNotEmpty(priority))
			details.add("Priority: " + priority);

		return baseDisplayText + details;
	}

}
