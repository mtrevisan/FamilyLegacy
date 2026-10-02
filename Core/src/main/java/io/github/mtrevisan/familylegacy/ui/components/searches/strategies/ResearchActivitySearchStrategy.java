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
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchActivityReader;
import io.github.mtrevisan.familylegacy.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.ui.components.searches.SearchMode;
import io.github.mtrevisan.familylegacy.ui.components.searches.SearchStrategy;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchActivityHandler;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.StringJoiner;
import java.util.function.Predicate;


/**
 * Search strategy for ResearchActivity records.
 * Supports filtering by activity type, status, action, result, and observation.
 */
public class ResearchActivitySearchStrategy implements SearchStrategy{

	private static final ResearchActivityHandler HANDLER = ResearchActivityHandler.getInstance();

	private String activityType;
	private String status;
	private String action;
	private String result;
	private String observation;
	private SearchMode mode;


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		activityType = criteria.getFilterFor(ResearchActivityReader.TAG_ACTIVITY_TYPE);
		status = criteria.getFilterFor(ResearchActivityReader.TAG_STATUS);
		action = criteria.getFilterFor(ResearchActivityReader.TAG_ACTION);
		result = criteria.getFilterFor(ResearchActivityReader.TAG_RESULT);
		observation = criteria.getFilterFor(ResearchActivityReader.TAG_OBSERVATION);
		mode = criteria.mode();

		return activity -> {
			// Activity Type filter
			if(StringUtils.isNotEmpty(activityType)){
				final String recordType = ResearchActivityReader.extractActivityType(activity);
				if(!Strings.CI.equals(activityType, recordType))
					return false;
			}

			// Status filter
			if(StringUtils.isNotEmpty(status)){
				final String recordStatus = ResearchActivityReader.extractStatus(activity);
				if(!Strings.CI.equals(status, recordStatus))
					return false;
			}

			// Action filter
			if(StringUtils.isNotEmpty(action)){
				final String recordAction = ResearchActivityReader.extractAction(activity);
				if(!SearchHelper.matches(recordAction, action, mode))
					return false;
			}

			// Result filter
			if(StringUtils.isNotEmpty(result)){
				final String recordResult = ResearchActivityReader.extractResult(activity);
				if(!result.equalsIgnoreCase(recordResult))
					return false;
			}

			// Observation filter
			if(StringUtils.isNotEmpty(observation)){
				final String recordObservation = ResearchActivityReader.extractObservation(activity);
				if(!SearchHelper.matches(recordObservation, observation, mode))
					return false;
			}

			return true;
		};
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final String baseDisplayText = HANDLER.getDisplayText(record, model);

		final String type = ResearchActivityReader.extractActivityType(record);
		final String status = ResearchActivityReader.extractStatus(record);
		final String result = ResearchActivityReader.extractResult(record);

		final StringJoiner details = new StringJoiner(", ", " (", ")");
		details.setEmptyValue(StringUtils.EMPTY);

		if(StringUtils.isNotEmpty(type))
			details.add(I18N.t("dialog.research.activity.type") + ": " + type);
		if(StringUtils.isNotEmpty(status))
			details.add(I18N.t("dialog.research.activity.status") + ": " + status);
		if(StringUtils.isNotEmpty(result))
			details.add(I18N.t("dialog.research.activity.result") + ": " + result);

		return baseDisplayText + details;
	}

}
