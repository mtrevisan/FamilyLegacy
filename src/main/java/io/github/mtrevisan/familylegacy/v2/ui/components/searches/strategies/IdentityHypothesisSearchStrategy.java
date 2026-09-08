package io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.TextSearchHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IdentityHypothesisHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.StringJoiner;
import java.util.function.Predicate;


/**
 * Search strategy for IdentityHypothesis records.
 * Supports filtering by target candidate records and hypothesis comments.
 */
public class IdentityHypothesisSearchStrategy implements SearchStrategy{

	private static final String TAG_IDENTITY = "identity";
	private static final String TAG_COMMENT = "comment";

	private static final double FUZZY_THRESHOLD = 0.05;


	private static final IdentityHypothesisHandler HANDLER = IdentityHypothesisHandler.getInstance();


	private String candidateContains;
	private String commentContains;
	private boolean fuzzy;
	private boolean wholeWord;


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		candidateContains = criteria.getFilterFor(IdentityHypothesisFilterPanel.FILTER_KEY_CANDIDATE);
		commentContains = criteria.getFilterFor(IdentityHypothesisFilterPanel.FILTER_KEY_COMMENT);
		fuzzy = criteria.isFuzzy();
		wholeWord = criteria.isWholeWord();

		return hypothesis -> {
			// Candidate filter (checks resolved display text for candidate records)
			if(StringUtils.isNotEmpty(candidateContains)){
				final List<FLEFRecord> candidates = FLEFRecordHelper.getChildren(hypothesis, TAG_IDENTITY);
				boolean matched = false;
				for(final FLEFRecord candidate : candidates){
					final String targetRef = candidate.getValue();
					if(targetRef != null){
						final FLEFRecord targetRecord = model.getRecordById(targetRef);
						if(targetRecord != null){
							final String candidateDisplayText = model.getRecordHandler(targetRecord)
								.getDisplayText(targetRecord, model);
							if(TextSearchHelper.matchesText(candidateDisplayText, candidateContains, fuzzy, wholeWord, FUZZY_THRESHOLD)){
								matched = true;
								break;
							}
						}
					}
				}
				if(!matched)
					return false;
			}

			// Comment filter
			if(StringUtils.isNotEmpty(commentContains)){
				final String comment = FLEFRecordHelper.getChildValue(hypothesis, TAG_COMMENT);
				if(!TextSearchHelper.matchesText(comment, commentContains, fuzzy, wholeWord, FUZZY_THRESHOLD))
					return false;
			}

			return true;
		};
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final String baseDisplayText = HANDLER.getDisplayText(record, model);

		final String commentVal = FLEFRecordHelper.getChildValue(record, TAG_COMMENT);

		final StringJoiner details = new StringJoiner(", ", " (", ")");
		details.setEmptyValue(StringUtils.EMPTY);

		if(StringUtils.isNotEmpty(commentVal))
			details.add(commentVal);

		return baseDisplayText + details;
	}

}
