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
package io.github.mtrevisan.familylegacy.ui.tools.reports.index;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventParticipationHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;


public final class EventIndex{

	private static final String TAG_PARTICIPANT = "participant";
	private static final String TAG_ROLE = "role";


	/**
	 * A participant of an event: the referenced record, its branch kind (e.g. "individual", "group"),
	 * the role it plays (may be {@code null}), and the participation record itself.
	 */
	public record Participant(FLEFRecord record, String kind, String role, FLEFRecord participation){
	}

	private record Ref(String tag, String id){
	}

	private final FLEFModel model;
	private final Map<String, List<FLEFRecord>> personToEventsMap = new HashMap<>();
	private final Map<String, List<FLEFRecord>> participantToParticipationsMap = new HashMap<>();
	private final Map<String, List<FLEFRecord>> eventToParticipationsMap = new HashMap<>();
	private final Map<String, List<FLEFRecord>> groupToEventsMap = new HashMap<>();

	public EventIndex(final FLEFModel model, final Predicate<FLEFRecord> filter){
		this.model = model;
		if(model == null)
			return;

		final List<FLEFRecord> participations = model.getRecordsByType(EventParticipationHandler.TYPE);
		for(int i = 0, size = participations.size(); i < size; i ++){
			final FLEFRecord ep = participations.get(i);
			if(!filter.test(ep))
				continue;

			final String eventId = FLEFRecordHelper.getChildValue(ep, EventHandler.TYPE);
			final Ref participantRef = extractOneOf(ep, TAG_PARTICIPANT);
			if(eventId == null || participantRef == null)
				continue;

			final String participantId = participantRef.id();
			participantToParticipationsMap.computeIfAbsent(participantId, k -> new ArrayList<>()).add(ep);
			eventToParticipationsMap.computeIfAbsent(eventId, k -> new ArrayList<>()).add(ep);

			final FLEFRecord event = model.getRecordById(eventId);
			if(event != null && filter.test(event)){
				personToEventsMap.computeIfAbsent(participantId, k -> new ArrayList<>()).add(event);

				if("group".equalsIgnoreCase(participantRef.tag())){
					groupToEventsMap.computeIfAbsent(participantId, k -> new ArrayList<>()).add(event);
				}
			}
		}
	}

	public List<FLEFRecord> eventsOf(final FLEFRecord person){
		if(person == null || person.getId() == null)
			return Collections.emptyList();
		return personToEventsMap.getOrDefault(person.getId(), Collections.emptyList());
	}

	public List<FLEFRecord> participationsOfParticipant(final FLEFRecord p){
		if(p == null || p.getId() == null)
			return Collections.emptyList();
		return participantToParticipationsMap.getOrDefault(p.getId(), Collections.emptyList());
	}

	public List<FLEFRecord> participationsOfEvent(final FLEFRecord e){
		if(e == null || e.getId() == null)
			return Collections.emptyList();
		return eventToParticipationsMap.getOrDefault(e.getId(), Collections.emptyList());
	}

	public List<FLEFRecord> eventsOfGroup(final FLEFRecord g){
		if(g == null || g.getId() == null)
			return Collections.emptyList();
		return groupToEventsMap.getOrDefault(g.getId(), Collections.emptyList());
	}

	/**
	 * Resolves every participant of the given event into a {@link Participant} object,
	 * attaching the participation record, role, and referenced model entity.
	 */
	public List<Participant> participantsOf(final FLEFRecord event){
		if(event == null || event.getId() == null)
			return Collections.emptyList();

		final List<FLEFRecord> participations = participationsOfEvent(event);
		if(participations.isEmpty())
			return Collections.emptyList();

		final List<Participant> out = new ArrayList<>(participations.size());
		for(int i = 0, size = participations.size(); i < size; i ++){
			final FLEFRecord ep = participations.get(i);
			final Ref ref = extractOneOf(ep, TAG_PARTICIPANT);
			if(ref == null)
				continue;

			final FLEFRecord rec = model.getRecordById(ref.id());
			if(rec == null)
				continue;

			final String role = FLEFRecordHelper.getChildValue(ep, TAG_ROLE);
			out.add(new Participant(rec, ref.tag(), role, ep));
		}
		return out;
	}

	private static Ref extractOneOf(final FLEFRecord rec, final String fieldTag){
		final FLEFRecord field = FLEFRecordHelper.findChild(rec, fieldTag);
		if(field == null)
			return null;
		final FLEFRecord ref = field.getTheOnlyChild();
		if(ref == null)
			return null;
		final String tag = ref.getTag();
		final String id = ref.getValue();
		if(tag == null || id == null || id.isBlank())
			return null;
		return new Ref(tag.toLowerCase(Locale.ROOT), id);
	}

}
