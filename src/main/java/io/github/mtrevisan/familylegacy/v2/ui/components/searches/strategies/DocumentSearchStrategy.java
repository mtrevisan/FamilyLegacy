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


/* TODO */
/**
 * Search strategy for Document records.
 * Supports filtering by format, mime type, and file location/path.
 */
public class DocumentSearchStrategy implements SearchStrategy{

	private static final String DOT = ".";

	private static final String TAG_FORMAT = "format";
	private static final String TAG_MIME_TYPE = "mime_type";
	private static final String TAG_LOCATION = "location";
	private static final String TAG_VALUE = "value";

	private static final double FUZZY_THRESHOLD = 0.05;

	private static final DocumentHandler HANDLER = DocumentHandler.getInstance();

	private String format;
	private String mimeType;
	private String locationContains;
	private boolean fuzzy;
	private boolean wholeWord;


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		format = criteria.getFilterFor("format");
		mimeType = criteria.getFilterFor("mimeType");
		locationContains = criteria.getFilterFor("locationContains");
		fuzzy = criteria.isFuzzy();
		wholeWord = criteria.isWholeWord();

		return document -> {
			// Format filter (digital vs physical)
			if(StringUtils.isNotEmpty(format)){
				final String recordFormat = FLEFRecordHelper.getChildValue(document, TAG_FORMAT);
				if(!format.equalsIgnoreCase(recordFormat)){
					return false;
				}
			}

			// MIME type filter
			if(StringUtils.isNotEmpty(mimeType)){
				final String recordMime = FLEFRecordHelper.getChildValue(document, TAG_MIME_TYPE);
				if(!TextSearchHelper.matchesText(recordMime, mimeType, fuzzy, wholeWord, FUZZY_THRESHOLD)){
					return false;
				}
			}

			// File location / URL / URI filter
			if(StringUtils.isNotEmpty(locationContains)){
				final String recordLocation = FLEFRecordHelper.getChildValue(document, TAG_LOCATION + DOT + TAG_VALUE);
				if(!TextSearchHelper.matchesText(recordLocation, locationContains, fuzzy, wholeWord, FUZZY_THRESHOLD)){
					return false;
				}
			}

			return true;
		};
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final String baseDisplayText = HANDLER.getDisplayText(record, model);

		final String formatVal = FLEFRecordHelper.getChildValue(record, TAG_FORMAT);
		final String mimeVal = FLEFRecordHelper.getChildValue(record, TAG_MIME_TYPE);

		final StringJoiner details = new StringJoiner(", ", " (", ")");
		details.setEmptyValue(StringUtils.EMPTY);

		if(StringUtils.isNotEmpty(formatVal)){
			details.add(formatVal);
		}
		if(StringUtils.isNotEmpty(mimeVal)){
			details.add(mimeVal);
		}

		return baseDisplayText + details;
	}

}
