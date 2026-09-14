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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.events;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.DateNormalizer;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.NormalizedDate;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.TemporalSpan;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


/**
 * Pre-computed index of all event participants in a FLEF model.
 * <p>
 * Each entry represents one participation: who participated, in which
 * event, with which role. Duplicate participations of the same person in
 * the same event are kept separate, because the role is the piece of
 * information that the search view makes visible.
 * <p>
 * The index exposes the full list, the set of distinct roles and event
 * types, and a case-insensitive substring search on participant names
 * with an optional role filter.
 * <p>
 * Instances are immutable and thread-safe.
 */
public final class ParticipantIndex{

	private static final String TAG_PARTICIPANT = "participant";
	private static final String TAG_EVENT = "event";
	private static final String TAG_ROLE = "role";
	private static final String TAG_TYPE = "type";
	private static final String TAG_DATE = "date";
	private static final String TAG_PLACE = "place";
	private static final String TAG_NAME = "name";
	private static final String TAG_VALUE = "value";


	/**
	 * One participation record, resolved to display values.
	 *
	 * @param participantId         the id of the participating entity
	 * @param participantName       the display name
	 * @param participantSearchText the lowercase name, used for the search
	 * @param isIndividual          whether the participant is an individual
	 *                              (as opposed to a group)
	 * @param role                  the role in the event, or an empty string
	 * @param eventId               the event id
	 * @param eventType             the event type
	 * @param eventDate             the normalized date, or {@code null}
	 * @param placeId               the place id, or {@code null}
	 * @param placeName             the place name, or {@code null}
	 */
	public record ParticipantEntry(
		String participantId,
		String participantName,
		String participantSearchText,
		boolean isIndividual,
		String role,
		String eventId,
		String eventType,
		NormalizedDate eventDate,
		String placeId,
		String placeName
	){
	}


	private final List<ParticipantEntry> allEntries;
	private final List<String> distinctRoles;
	private final List<String> distinctEventTypes;


	private ParticipantIndex(final List<ParticipantEntry> allEntries, final List<String> distinctRoles,
		final List<String> distinctEventTypes){
		this.allEntries = allEntries;
		this.distinctRoles = distinctRoles;
		this.distinctEventTypes = distinctEventTypes;
	}


	/**
	 * Builds the index from the given model.
	 *
	 * @param model the FLEF model (must not be {@code null})
	 * @return the index
	 */
	public static ParticipantIndex build(final FLEFModel model){
		if(model == null)
			throw new IllegalArgumentException("Model must not be null");

		final DateNormalizer normalizer = new DateNormalizer();
		final List<ParticipantEntry> entries = new ArrayList<>();
		final Set<String> roles = new LinkedHashSet<>();
		final Set<String> eventTypes = new LinkedHashSet<>();

		for(final FLEFRecord participation : model.getRecordsByType(EventParticipationHandler.TYPE)){
			final FLEFRecord participantField = FLEFRecordHelper.findChild(participation, TAG_PARTICIPANT);
			if(participantField == null)
				continue;
			final FLEFRecord ref = participantField.getTheOnlyChild();
			if(ref == null || ref.getValue() == null)
				continue;

			final String participantId = ref.getValue();
			final String refTag = ref.getTag();
			final FLEFRecord participantRecord = model.getRecordById(participantId);
			if(participantRecord == null)
				continue;

			final boolean isIndividual = IndividualHandler.TYPE.equalsIgnoreCase(refTag);
			final String name = resolveName(participantRecord, model, isIndividual, participantId);

			String role = FLEFRecordHelper.getChildValue(participation, TAG_ROLE);
			if(role == null)
				role = StringUtils.EMPTY;
			if(!role.isBlank())
				roles.add(role);

			final String eventId = FLEFRecordHelper.getChildValue(participation, TAG_EVENT);
			if(eventId == null)
				continue;
			final FLEFRecord event = model.getRecordById(eventId);
			if(event == null)
				continue;

			String eventType = FLEFRecordHelper.getChildValue(event, TAG_TYPE);
			if(eventType == null)
				eventType = "unknown";
			eventTypes.add(eventType);

			final NormalizedDate date = resolveDate(event, normalizer);
			final String placeId;
			final String placeName;
			final FLEFRecord placeCitation = FLEFRecordHelper.findChild(event, TAG_PLACE);
			if(placeCitation != null){
				final FLEFRecord placeRef = FLEFRecordHelper.findChild(placeCitation, TAG_PLACE);
				if(placeRef != null && placeRef.getValue() != null){
					placeId = placeRef.getValue();
					placeName = resolvePlaceName(model, placeId);
				}
				else{
					placeId = null;
					placeName = null;
				}
			}
			else{
				placeId = null;
				placeName = null;
			}

			entries.add(new ParticipantEntry(
				participantId, name, name.toLowerCase(Locale.ROOT),
				isIndividual, role, eventId, eventType, date, placeId, placeName));
		}

		// Sort by name, then role, then date.
		entries.sort((a, b) -> {
			int c = a.participantName().compareToIgnoreCase(b.participantName());
			if(c != 0)
				return c;
			c = a.role().compareToIgnoreCase(b.role());
			if(c != 0)
				return c;
			final long ja = (a.eventDate() != null? a.eventDate().jdn(): Long.MAX_VALUE);
			final long jb = (b.eventDate() != null? b.eventDate().jdn(): Long.MAX_VALUE);
			return Long.compare(ja, jb);
		});

		final List<String> sortedRoles = new ArrayList<>(roles);
		sortedRoles.sort(String::compareToIgnoreCase);

		final List<String> sortedTypes = new ArrayList<>(eventTypes);
		sortedTypes.sort(String::compareToIgnoreCase);

		return new ParticipantIndex(
			Collections.unmodifiableList(entries),
			Collections.unmodifiableList(sortedRoles),
			Collections.unmodifiableList(sortedTypes));
	}


	/* ======================================================================
	 *                          Accessors
	 * ====================================================================== */

	public List<ParticipantEntry> allEntries(){
		return allEntries;
	}

	public List<String> distinctRoles(){
		return distinctRoles;
	}

	public List<String> distinctEventTypes(){
		return distinctEventTypes;
	}

	public boolean isEmpty(){
		return allEntries.isEmpty();
	}

	public int size(){
		return allEntries.size();
	}


	/* ======================================================================
	 *                          Search
	 * ====================================================================== */

	/**
	 * Returns the entries whose participant name contains the given query
	 * (case-insensitive substring) and whose role matches the given
	 * filter, when the filter is non-blank.
	 *
	 * @param query      the search text; {@code null} or blank matches all
	 * @param roleFilter the exact role to match; {@code null} or blank
	 *                   matches all roles
	 * @return the matching entries, in the original sorted order
	 */
	public List<ParticipantEntry> search(final String query, final String roleFilter){
		final String q = (query != null? query.trim()
			.toLowerCase(Locale.ROOT): StringUtils.EMPTY);
		final boolean allRoles = (roleFilter == null || roleFilter.isBlank());

		final List<ParticipantEntry> result = new ArrayList<>();
		for(final ParticipantEntry e : allEntries){
			if(!q.isEmpty() && !e.participantSearchText()
				.contains(q))
				continue;
			if(!allRoles && !roleFilter.equalsIgnoreCase(e.role()))
				continue;
			result.add(e);
		}
		return result;
	}


	/* ======================================================================
	 *                          Helpers
	 * ====================================================================== */

	private static String resolveName(final FLEFRecord record, final FLEFModel model,
		final boolean isIndividual, final String fallback){
		try{
			final String text = (isIndividual
				? IndividualHandler.getInstance()
				.getDisplayText(record, model)
				: GroupHandler.getInstance()
					.getDisplayText(record, model));
			if(text != null && !text.isBlank())
				return text;
		}
		catch(final RuntimeException ignored){
			// Fall through to the id-based fallback.
		}
		return fallback;
	}

	private static NormalizedDate resolveDate(final FLEFRecord event, final DateNormalizer normalizer){
		final FLEFRecord dateStruct = FLEFRecordHelper.findChild(event, TAG_DATE);
		if(dateStruct == null)
			return null;
		final TemporalSpan span = normalizer.normalize(dateStruct);
		return (span != null? span.start(): null);
	}

	private static String resolvePlaceName(final FLEFModel model, final String placeId){
		final FLEFRecord place = model.getRecordById(placeId);
		if(place == null)
			return placeId;
		final FLEFRecord name = FLEFRecordHelper.findChild(place, TAG_NAME);
		if(name != null){
			final String v = FLEFRecordHelper.getChildValue(name, TAG_VALUE);
			if(v != null && !v.isBlank())
				return v;
		}
		return placeId;
	}

}
