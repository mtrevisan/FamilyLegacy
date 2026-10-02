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
package io.github.mtrevisan.familylegacy.ui.components.searches.strategies;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.ui.components.searches.SearchMode;
import io.github.mtrevisan.familylegacy.ui.components.searches.SearchStrategy;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.StringJoiner;
import java.util.function.Predicate;


/**
 * Search strategy for Source records.
 * Supports filtering by title, author, publisher, media type, and place.
 */
public class SourceSearchStrategy implements SearchStrategy{

	private static final double FUZZY_THRESHOLD = 0.05;


	private static final SourceHandler HANDLER = SourceHandler.getInstance();


	private String title;
	private String author;
	private String publisher;
	private String mediaType;
	private String place;
	private SearchMode mode;


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		title = criteria.getFilterFor(SourceReader.TAG_TITLE);
		author = criteria.getFilterFor(SourceReader.TAG_AUTHOR);
		publisher = criteria.getFilterFor(SourceReader.TAG_PUBLISHER);
		mediaType = criteria.getFilterFor(SourceReader.TAG_MEDIA_TYPE);
		place = criteria.getFilterFor(SourceReader.TAG_PLACE);
		mode = criteria.mode();

		return source -> {
			// Title filter
			if(StringUtils.isNotEmpty(title)){
				final List<String> titles = SourceReader.extractTitles(source);
				boolean matched = false;
				for(final String recordTitle : titles){
					if(SearchHelper.matches(recordTitle, title, mode)){
						matched = true;

						break;
					}
				}
				if(!matched)
					return false;
			}

			// Author filter
			if(StringUtils.isNotEmpty(author)){
				final String recordAuthor = SourceReader.extractAuthor(source);
				if(!SearchHelper.matches(recordAuthor, author, mode))
					return false;
			}

			// Publisher filter
			if(StringUtils.isNotEmpty(publisher)){
				final String recordPublisher = SourceReader.extractPublisher(source);
				if(!SearchHelper.matches(recordPublisher, place, mode))
					return false;
			}

			// Media Type filter
			if(StringUtils.isNotEmpty(mediaType)){
				final String recordMediaType = SourceReader.extractMediaType(source);
				if(!mediaType.equalsIgnoreCase(recordMediaType))
					return false;
			}

			// Place filter
			if(!SearchHelper.matchesPlace(source, place, model, mode, FUZZY_THRESHOLD))
				return false;

			return true;
		};
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final String baseDisplayText = HANDLER.getDisplayText(record, model);

		final String authorVal = SourceReader.extractAuthor(record);
		final String mediaTypeVal = SourceReader.extractMediaType(record);

		final StringJoiner details = new StringJoiner(", ", " (", ")");
		details.setEmptyValue(StringUtils.EMPTY);

		if(StringUtils.isNotEmpty(authorVal))
			details.add(authorVal);
		if(StringUtils.isNotEmpty(mediaTypeVal))
			details.add(mediaTypeVal);

		return baseDisplayText + details;
	}

}
