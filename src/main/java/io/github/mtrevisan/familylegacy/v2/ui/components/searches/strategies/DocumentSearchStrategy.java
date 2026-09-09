package io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.TextSearchHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.DocumentHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.StringJoiner;
import java.util.function.Predicate;


/**
 * Search strategy for Document records.
 * Supports filtering by description, mapping projection, and URI path.
 */
public class DocumentSearchStrategy implements SearchStrategy{

	private static final String TAG_DESCRIPTION = "description";
	private static final String TAG_MAPPING = "mapping";
	private static final String TAG_URI = "uri";

	private static final double FUZZY_THRESHOLD = 0.05;


	private static final DocumentHandler HANDLER = DocumentHandler.getInstance();


	private String description;
	private String mapping;
	private String uri;
	private boolean fuzzy;
	private boolean wholeWord;


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		description = criteria.getFilterFor(DocumentFilterPanel.FILTER_KEY_DESCRIPTION);
		mapping = criteria.getFilterFor(DocumentFilterPanel.FILTER_KEY_MAPPING);
		uri = criteria.getFilterFor(DocumentFilterPanel.FILTER_KEY_URI);
		fuzzy = criteria.isFuzzy();
		wholeWord = criteria.isWholeWord();

		return document -> {
			// Description filter
			if(StringUtils.isNotEmpty(description)){
				final String description = FLEFRecordHelper.getChildValue(document, TAG_DESCRIPTION);
				if(!TextSearchHelper.matchesText(description, this.description, fuzzy, wholeWord, FUZZY_THRESHOLD))
					return false;
			}

			// Mapping filter
			if(StringUtils.isNotEmpty(mapping)){
				final String recordMapping = FLEFRecordHelper.getChildValue(document, TAG_MAPPING);
				if(!mapping.equalsIgnoreCase(recordMapping))
					return false;
			}

			// URI filter
			if(StringUtils.isNotEmpty(uri)){
				final String uri = FLEFRecordHelper.getChildValue(document, TAG_URI);
				if(!TextSearchHelper.matchesText(uri, this.uri, fuzzy, wholeWord, FUZZY_THRESHOLD))
					return false;
			}

			return true;
		};
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final String baseDisplayText = HANDLER.getDisplayText(record, model);

		final String description = FLEFRecordHelper.getChildValue(record, TAG_DESCRIPTION);
		final String mapping = FLEFRecordHelper.getChildValue(record, TAG_MAPPING);

		final StringJoiner details = new StringJoiner(", ", " (", ")");
		details.setEmptyValue(StringUtils.EMPTY);

		if(StringUtils.isNotEmpty(mapping))
			details.add("(" + mapping + ")");
		if(StringUtils.isNotEmpty(description) && !baseDisplayText.contains(description))
			details.add(description);

		return baseDisplayText + details;
	}

}
