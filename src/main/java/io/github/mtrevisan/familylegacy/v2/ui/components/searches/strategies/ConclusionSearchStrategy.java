package io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.TextSearchHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.StringJoiner;
import java.util.function.Predicate;


/* TODO */
/**
 * Search strategy for Conclusion records.
 * Supports filtering by confidence level, target individual, and associated research question.
 */
public class ConclusionSearchStrategy implements SearchStrategy{

	private static final String DOT = ".";

	private static final String TAG_CONFIDENCE = "confidence";
	private static final String TAG_TARGET = "target";
	private static final String TAG_INDIVIDUAL = "individual";
	private static final String TAG_QUESTION = "question";

	private static final String TAG_TARGET_INDIVIDUAL = TAG_TARGET + DOT + TAG_INDIVIDUAL;
	private static final String TAG_QUESTION_QUESTION = TAG_QUESTION + DOT + TAG_QUESTION;

	private static final double FUZZY_THRESHOLD = 0.05;

	private static final ConclusionHandler HANDLER = ConclusionHandler.getInstance();

	private String confidence;
	private String targetIndividualContains;
	private String researchQuestionContains;
	private boolean fuzzy;
	private boolean wholeWord;


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		confidence = criteria.getFilterFor("confidence");
		targetIndividualContains = criteria.getFilterFor("targetIndividualContains");
		researchQuestionContains = criteria.getFilterFor("researchQuestionContains");
		fuzzy = criteria.isFuzzy();
		wholeWord = criteria.isWholeWord();

		return conclusion -> {
			// Confidence filter
			if(StringUtils.isNotEmpty(confidence)){
				final String recordConfidence = FLEFRecordHelper.getChildValue(conclusion, TAG_CONFIDENCE);
				if(!confidence.equalsIgnoreCase(recordConfidence)){
					return false;
				}
			}

			// Target Individual filter
			if(StringUtils.isNotEmpty(targetIndividualContains)){
				final String individualRef = FLEFRecordHelper.getChildValue(conclusion, TAG_TARGET_INDIVIDUAL);
				if(individualRef != null){
					final FLEFRecord indRecord = model.getRecordById(individualRef);
					if(indRecord != null){
						final String indDisplayText = IndividualHandler.getInstance().getDisplayText(indRecord, model);
						if(!TextSearchHelper.matchesText(indDisplayText, targetIndividualContains, fuzzy, wholeWord, FUZZY_THRESHOLD)){
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

			// Associated Research Question filter
			if(StringUtils.isNotEmpty(researchQuestionContains)){
				final String questionRef = FLEFRecordHelper.getChildValue(conclusion, TAG_QUESTION_QUESTION);
				if(questionRef != null){
					final FLEFRecord questionRecord = model.getRecordById(questionRef);
					if(questionRecord != null){
						final String questionDisplayText = ResearchQuestionHandler.getInstance().getDisplayText(questionRecord, model);
						if(!TextSearchHelper.matchesText(questionDisplayText, researchQuestionContains, fuzzy, wholeWord, FUZZY_THRESHOLD)){
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

		final String confidenceVal = FLEFRecordHelper.getChildValue(record, TAG_CONFIDENCE);

		final StringJoiner details = new StringJoiner(", ", " (", ")");
		details.setEmptyValue(StringUtils.EMPTY);

		if(StringUtils.isNotEmpty(confidenceVal)){
			details.add("Confidence: " + confidenceVal);
		}

		return baseDisplayText + details;
	}

}
