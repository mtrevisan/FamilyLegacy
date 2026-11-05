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
package io.github.mtrevisan.familylegacy.search.strategies;

import io.github.mtrevisan.familylegacy.events.EventHelper;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.search.SearchCriteria;
import io.github.mtrevisan.familylegacy.search.SearchStrategy;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Predicate;


/**
 * Search strategy for EventParticipation records.
 * Supports filtering by role, by type of the event, and by type of the participant (individual, group or place); the name of the
 * participant and the data of the event are matched by the text query, against the display text.
 */
public class EventParticipationSearchStrategy implements SearchStrategy{

	/** Filter key: the type of the participant ({@code individual}, {@code group} or {@code place}). */
	public static final String FILTER_PARTICIPANT_TYPE = EventParticipationReader.TAG_PARTICIPANT;


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		final String role = criteria.getFilterFor(EventParticipationReader.TAG_ROLE);
		final String eventType = criteria.getFilterFor(EventReader.TAG_TYPE);
		final String participantType = criteria.getFilterFor(FILTER_PARTICIPANT_TYPE);

		return record -> {
			final EventHelper.Participation participation = EventHelper.toParticipation(record);

			if(StringUtils.isNotEmpty(role) && !role.equalsIgnoreCase(participation.role()))
				return false;

			if(StringUtils.isNotEmpty(participantType) && !participantType.equalsIgnoreCase(participation.participantType()))
				return false;

			if(StringUtils.isNotEmpty(eventType)){
				final FLEFRecord event = model.getRecordById(participation.eventId());
				if(!eventType.equalsIgnoreCase(EventHelper.eventType(event)))
					return false;
			}

			return true;
		};
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		return RecordDisplay.describe(record, model);
	}

}
