package io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.TextSearchHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.StringJoiner;
import java.util.function.Predicate;


/* TODO */
/**
 * Search strategy for IdentityHypothesis records.
 * Supports filtering by status, confidence level, and linked individuals.
 */
public class IdentityHypothesisSearchStrategy implements SearchStrategy{

	private static final String DOT = ".";

	private static final String TAG_STATUS = "status";
	private static final String TAG_CONFIDENCE = "confidence";
	private static final String TAG_INDIVIDUAL = "individual";
	private static final String TAG_PERSON = "person";

	private static final String TAG_INDIVIDUAL_PERSON = TAG_INDIVIDUAL + DOT + TAG_PERSON;

	private static final double FUZZY_THRESHOLD = 0.05;

	private static final IdentityHypothesisHandler HANDLER = IdentityHypothesisHandler.getInstance();

	private String status;
	private String confidence;
	private String individualContains;
	private boolean fuzzy;
	private boolean wholeWord;


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		status = criteria.getFilterFor("status");
		confidence = criteria.getFilterFor("confidence");
		individualContains = criteria.getFilterFor("individualContains");
		fuzzy = criteria.isFuzzy();
		wholeWord = criteria.isWholeWord();

		return hypothesis -> {
			// Status filter
			if(StringUtils.isNotEmpty(status)){
				final String recordStatus = FLEFRecordHelper.getChildValue(hypothesis, TAG_STATUS);
				if(!status.equalsIgnoreCase(recordStatus)){
					return false;
				}
			}

			// Confidence filter
			if(StringUtils.isNotEmpty(confidence)){
				final String recordConfidence = FLEFRecordHelper.getChildValue(hypothesis, TAG_CONFIDENCE);
				if(!confidence.equalsIgnoreCase(recordConfidence)){
					return false;
				}
			}

			// Individual filter (checks if any linked individual matches)
			if(StringUtils.isNotEmpty(individualContains)){
				final List<FLEFRecord> indRecords = FLEFRecordHelper.findChildren(hypothesis, TAG_INDIVIDUAL);
				boolean matched = false;
				for(final FLEFRecord indRefRecord : indRecords){
					final String personRef = FLEFRecordHelper.getChildValue(indRefRecord, TAG_PERSON);
					if(personRef != null){
						final FLEFRecord indRecord = model.getRecordById(personRef);
						if(indRecord != null){
							final String indDisplayText = IndividualHandler.getInstance().getDisplayText(indRecord, model);
							if(TextSearchHelper.matchesText(indDisplayText, individualContains, fuzzy, wholeWord, FUZZY_THRESHOLD)){
								matched = true;
								break;
							}
						}
					}
				}
				if(!matched){
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
		final String confidenceVal = FLEFRecordHelper.getChildValue(record, TAG_CONFIDENCE);

		final StringJoiner details = new StringJoiner(", ", " (", ")");
		details.setEmptyValue(StringUtils.EMPTY);

		if(StringUtils.isNotEmpty(statusVal)){
			details.add("Status: " + statusVal);
		}
		if(StringUtils.isNotEmpty(confidenceVal)){
			details.add("Confidence: " + confidenceVal);
		}

		return baseDisplayText + details;
	}

}
