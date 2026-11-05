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
package io.github.mtrevisan.familylegacy.events;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.places.PlaceNameResolver;
import org.apache.commons.lang3.StringUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;


/**
 * The one-line description of an event: type, date, complete place, participants and description
 * ("birth (2 MAR 1850) – Borgo, Treviso, Veneto – Mario Rossi, Anna Bianchi +2 – Baptised at home [E12]").
 * <p>
 * Used wherever a list of events is shown or searched, so that the same text is read and matched everywhere.
 * <p>
 * The indexes (place hierarchy, participations) are built once for a model and reused for a short time, so that describing
 * thousands of events in a row costs one scan of the model instead of one per event. The model has no change notification, so
 * the index expires after {@link #CACHE_MILLIS} and when the number of records changes.
 */
public final class EventSummary{

	private static final long CACHE_MILLIS = 2_000L;
	private static final int MAX_PARTICIPANTS = 3;
	private static final int MAX_DESCRIPTION_LENGTH = 60;
	private static final String SEPARATOR = " – ";
	private static final String ELLIPSIS = "…";


	private record Index(WeakReference<FLEFModel> model, int recordCount, long builtAt, PlaceNameResolver places,
		Map<String, List<EventHelper.Participation>> participations){}


	private static volatile Index cache;


	private EventSummary(){}


	/**
	 * @param event	the event
	 * @param model	the model
	 * @return	the complete name of the place of the event (the place and all its containers), or the text of the place as written
	 * 	in the source, or {@code null} if the event has no place
	 */
	public static String placeName(final FLEFRecord event, final FLEFModel model){
		final String placeId = EventHelper.eventPlaceId(event);
		if(placeId != null)
			return indexOf(model).places().fullName(placeId);

		return PlaceCitationReader.extractPlace(event, model);
	}

	/**
	 * @param event	the event
	 * @param model	the model
	 * @return	the names of the participants, the principal ones first, at most {@value #MAX_PARTICIPANTS} followed by "+N" for the
	 * 	others; {@code null} if the event has no participants
	 */
	public static String participantNames(final FLEFRecord event, final FLEFModel model){
		final List<EventHelper.Participation> participations = indexOf(model).participations()
			.getOrDefault(event.getId(), List.of());
		if(participations.isEmpty())
			return null;

		final List<EventHelper.Participation> ordered = new ArrayList<>(participations.size());
		for(final EventHelper.Participation participation : participations)
			if(isPrincipal(participation))
				ordered.add(participation);
		for(final EventHelper.Participation participation : participations)
			if(!isPrincipal(participation))
				ordered.add(participation);

		final StringJoiner names = new StringJoiner(", ");
		final int shown = Math.min(ordered.size(), MAX_PARTICIPANTS);
		for(int i = 0; i < shown; i ++)
			names.add(participantName(ordered.get(i), model));
		final String text = names.toString();
		return (ordered.size() > shown? text + " +" + (ordered.size() - shown): text);
	}

	/**
	 * @param event	the event
	 * @param model	the model
	 * @return	the one-line description of the event
	 */
	public static String describe(final FLEFRecord event, final FLEFModel model){
		return describe(event, model, true);
	}

	/**
	 * @param event	the event
	 * @param model	the model
	 * @return	the description of the event without its participants, for the texts that already say who took part
	 * 	(the description of a participation)
	 */
	public static String describeWithoutParticipants(final FLEFRecord event, final FLEFModel model){
		return describe(event, model, false);
	}

	private static String describe(final FLEFRecord event, final FLEFModel model, final boolean withParticipants){
		final StringBuilder sb = new StringBuilder();

		final String type = EventHelper.eventType(event);
		if(StringUtils.isNotBlank(type))
			sb.append(type);

		final String date = EventHelper.eventDateRaw(event);
		if(StringUtils.isNotBlank(date)){
			if(!sb.isEmpty())
				sb.append(StringUtils.SPACE);
			sb.append('(')
				.append(date)
				.append(')');
		}

		appendPart(sb, placeName(event, model));
		if(withParticipants)
			appendPart(sb, participantNames(event, model));
		final String description = EventHelper.eventDescription(event);
		if(StringUtils.isNotBlank(description))
			appendPart(sb, StringUtils.abbreviate(StringUtils.normalizeSpace(description), ELLIPSIS, MAX_DESCRIPTION_LENGTH));

		if(sb.isEmpty())
			sb.append("Event");
		return sb.append(" [")
			.append(event.getId())
			.append(']')
			.toString();
	}


	private static void appendPart(final StringBuilder sb, final String part){
		if(StringUtils.isBlank(part))
			return;

		if(!sb.isEmpty())
			sb.append(SEPARATOR);
		sb.append(part);
	}

	private static boolean isPrincipal(final EventHelper.Participation participation){
		return (participation.role() == null || EventParticipationReader.ROLES[0].equalsIgnoreCase(participation.role()));
	}

	private static String participantName(final EventHelper.Participation participation, final FLEFModel model){
		final String id = participation.participantId();
		final FLEFRecord record = model.getRecordById(id);
		if(record == null)
			return (id != null? id: "?");

		final String name;
		if(PlaceReader.isOfType(record))
			name = PlaceReader.displayName(record);
		else if(GroupReader.TYPE.equalsIgnoreCase(record.getTag()))
			name = GroupReader.extractPrimaryName(record);
		else
			name = RecordDisplay.describe(record, model);
		return (StringUtils.isNotBlank(name)? name: id);
	}

	private static Index indexOf(final FLEFModel model){
		final long now = System.currentTimeMillis();
		final Index current = cache;
		if(current != null && current.model().get() == model && current.recordCount() == model.getRecordCount()
				&& now - current.builtAt() < CACHE_MILLIS)
			return current;

		final Index index = new Index(new WeakReference<>(model), model.getRecordCount(), now, new PlaceNameResolver(model),
			EventHelper.participationsByEvent(model));
		cache = index;
		return index;
	}

	/** Drops the indexes, so that the next description reads the model again. */
	public static void invalidate(){
		cache = null;
	}

}
