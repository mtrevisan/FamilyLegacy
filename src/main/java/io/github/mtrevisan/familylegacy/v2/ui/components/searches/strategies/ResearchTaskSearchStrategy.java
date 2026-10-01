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
import io.github.mtrevisan.familylegacy.v2.io.model.readers.ResearchTaskReader;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchMode;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchTaskHandler;
import io.github.mtrevisan.familylegacy.v2.ui.i18n.I18N;
import org.apache.commons.lang3.StringUtils;

import java.util.StringJoiner;
import java.util.function.Predicate;


/**
 * Search strategy for ResearchTask records.
 * Supports filtering by task description, status, priority, and outcome notes.
 */
public class ResearchTaskSearchStrategy implements SearchStrategy{

	private static final ResearchTaskHandler HANDLER = ResearchTaskHandler.getInstance();


	private String description;
	private String status;
	private String priority;
	private String outcome;
	private SearchMode mode;


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		description = criteria.getFilterFor(ResearchTaskReader.TAG_DESCRIPTION);
		status = criteria.getFilterFor(ResearchTaskReader.TAG_STATUS);
		priority = criteria.getFilterFor(ResearchTaskReader.TAG_PRIORITY);
		outcome = criteria.getFilterFor(ResearchTaskReader.TAG_OUTCOME);
		mode = criteria.mode();

		return task -> {
			// Description filter
			if(StringUtils.isNotEmpty(description)){
				final String recordDescription = ResearchTaskReader.extractDescription(task);
				if(!SearchHelper.matches(recordDescription, description, mode))
					return false;
			}

			// Status filter
			if(StringUtils.isNotEmpty(status)){
				final String recordStatus = ResearchTaskReader.extractStatus(task);
				if(!status.equalsIgnoreCase(recordStatus))
					return false;
			}

			// Priority filter
			if(StringUtils.isNotEmpty(priority)){
				final String recordPriority = ResearchTaskReader.extractPriority(task);
				if(!priority.equalsIgnoreCase(recordPriority))
					return false;
			}

			// Outcome filter
			if(StringUtils.isNotEmpty(outcome)){
				final String recordOutcome = ResearchTaskReader.extractOutcome(task);
				if(!SearchHelper.matches(recordOutcome, outcome, mode))
					return false;
			}

			return true;
		};
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final String baseDisplayText = HANDLER.getDisplayText(record, model);

		final String status = ResearchTaskReader.extractStatus(record);
		final String priority = ResearchTaskReader.extractPriority(record);

		final StringJoiner details = new StringJoiner(", ", " (", ")");
		details.setEmptyValue(StringUtils.EMPTY);

		if(StringUtils.isNotEmpty(status))
			details.add(I18N.t("dialog.research.task.status") + ": " + status);
		if(StringUtils.isNotEmpty(priority))
			details.add(I18N.t("dialog.research.task.priority") + ": " + priority);

		return baseDisplayText + details;
	}

}
