/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.TextSearchHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.StringJoiner;
import java.util.function.Predicate;


/**
 * Search strategy for Source records.
 * Supports filtering by title, author, publisher, media type, and place.
 */
public class SourceSearchStrategy implements SearchStrategy{

	private static final String DOT = ".";

	private static final String TAG_TITLE = "title";
	private static final String TAG_VALUE = "value";
	private static final String TAG_TITLE_VALUE = TAG_TITLE + DOT + TAG_VALUE;
	private static final String TAG_AUTHOR = "author";
	private static final String TAG_PUBLISHER = "publisher";
	private static final String TAG_MEDIA_TYPE = "media_type";

	private static final double FUZZY_THRESHOLD = 0.05;


	private static final SourceHandler HANDLER = SourceHandler.getInstance();


	private String title;
	private String author;
	private String publisher;
	private String mediaType;
	private String place;
	private boolean fuzzy;
	private boolean wholeWord;


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		title = criteria.getFilterFor(SourceFilterPanel.FILTER_KEY_TITLE);
		author = criteria.getFilterFor(SourceFilterPanel.FILTER_KEY_AUTHOR);
		publisher = criteria.getFilterFor(SourceFilterPanel.FILTER_KEY_PUBLISHER);
		mediaType = criteria.getFilterFor(SourceFilterPanel.FILTER_KEY_MEDIA_TYPE);
		place = criteria.getFilterFor(SourceFilterPanel.FILTER_KEY_PLACE);
		fuzzy = criteria.isFuzzy();
		wholeWord = criteria.isWholeWord();

		return source -> {
			// Title filter
			if(StringUtils.isNotEmpty(title)){
				final List<FLEFRecord> titles = FLEFRecordHelper.findChildren(source, TAG_TITLE_VALUE);
				boolean matched = false;
				for(final FLEFRecord title : titles){
					if(TextSearchHelper.matchesText(title.getValue(), this.title, fuzzy, wholeWord, FUZZY_THRESHOLD)){
						matched = true;

						break;
					}
				}
				if(!matched)
					return false;
			}

			// Author filter
			if(StringUtils.isNotEmpty(author)){
				final String author = FLEFRecordHelper.getChildValue(source, TAG_AUTHOR);
				if(!TextSearchHelper.matchesText(author, this.author, fuzzy, wholeWord, FUZZY_THRESHOLD))
					return false;
			}

			// Publisher filter
			if(StringUtils.isNotEmpty(publisher)){
				final String publisher = FLEFRecordHelper.getChildValue(source, TAG_PUBLISHER);
				if(!TextSearchHelper.matchesText(publisher, this.publisher, fuzzy, wholeWord, FUZZY_THRESHOLD))
					return false;
			}

			// Media Type filter
			if(StringUtils.isNotEmpty(mediaType)){
				final String recordMediaType = FLEFRecordHelper.getChildValue(source, TAG_MEDIA_TYPE);
				if(!mediaType.equalsIgnoreCase(recordMediaType))
					return false;
			}

			// Place filter
			if(!SearchHelper.matchesPlace(source, place, model, fuzzy, wholeWord, FUZZY_THRESHOLD))
				return false;

			return true;
		};
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final String baseDisplayText = HANDLER.getDisplayText(record, model);

		final String authorVal = FLEFRecordHelper.getChildValue(record, TAG_AUTHOR);
		final String mediaTypeVal = FLEFRecordHelper.getChildValue(record, TAG_MEDIA_TYPE);

		final StringJoiner details = new StringJoiner(", ", " (", ")");
		details.setEmptyValue(StringUtils.EMPTY);

		if(StringUtils.isNotEmpty(authorVal))
			details.add(authorVal);
		if(StringUtils.isNotEmpty(mediaTypeVal))
			details.add(mediaTypeVal);

		return baseDisplayText + details;
	}

}
