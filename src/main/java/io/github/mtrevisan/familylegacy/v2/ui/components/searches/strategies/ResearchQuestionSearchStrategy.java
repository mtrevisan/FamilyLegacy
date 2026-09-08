package io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.TextSearchHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.StringJoiner;
import java.util.function.Predicate;


/* TODO */
/**
 * Search strategy for ResearchQuestion records.
 * Supports filtering by status, priority, and target focus individual.
 */
public class ResearchQuestionSearchStrategy implements SearchStrategy{

	private static final String DOT = ".";

	private static final String TAG_STATUS = "status";
	private static final String TAG_PRIORITY = "priority";
	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_INDIVIDUAL = "individual";

	private static final String TAG_SUBJECT_INDIVIDUAL = TAG_SUBJECT + DOT + TAG_INDIVIDUAL;

	private static final double FUZZY_THRESHOLD = 0.05;

	private static final ResearchQuestionHandler HANDLER = ResearchQuestionHandler.getInstance();

	private String status;
	private String priority;
	private String focusIndividual;
	private boolean fuzzy;
	private boolean wholeWord;


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		status = criteria.getFilterFor("status");
		priority = criteria.getFilterFor("priority");
		focusIndividual = criteria.getFilterFor("focusIndividual");
		fuzzy = criteria.isFuzzy();
		wholeWord = criteria.isWholeWord();

		return question -> {
			// Status filter
			if(StringUtils.isNotEmpty(status)){
				final String recordStatus = FLEFRecordHelper.getChildValue(question, TAG_STATUS);
				if(!status.equalsIgnoreCase(recordStatus)){
					return false;
				}
			}

			// Priority filter
			if(StringUtils.isNotEmpty(priority)){
				final String recordPriority = FLEFRecordHelper.getChildValue(question, TAG_PRIORITY);
				if(!priority.equalsIgnoreCase(recordPriority)){
					return false;
				}
			}

			// Target/Subject Individual filter
			if(StringUtils.isNotEmpty(focusIndividual)){
				final String individualRef = FLEFRecordHelper.getChildValue(question, TAG_SUBJECT_INDIVIDUAL);
				if(individualRef != null){
					final FLEFRecord individualRecord = model.getRecordById(individualRef);
					if(individualRecord != null){
						final String indDisplayText = IndividualHandler.getInstance().getDisplayText(individualRecord, model);
						if(!TextSearchHelper.matchesText(indDisplayText, focusIndividual, fuzzy, wholeWord, FUZZY_THRESHOLD)){
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

			return true;
		};
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final String baseDisplayText = HANDLER.getDisplayText(record, model);

		final String statusVal = FLEFRecordHelper.getChildValue(record, TAG_STATUS);
		final String priorityVal = FLEFRecordHelper.getChildValue(record, TAG_PRIORITY);

		final StringJoiner details = new StringJoiner(", ", " (", ")");
		details.setEmptyValue(StringUtils.EMPTY);

		if(StringUtils.isNotEmpty(statusVal)){
			details.add("Status: " + statusVal);
		}
		if(StringUtils.isNotEmpty(priorityVal)){
			details.add("Priority: " + priorityVal);
		}

		return baseDisplayText + details;
	}

}
