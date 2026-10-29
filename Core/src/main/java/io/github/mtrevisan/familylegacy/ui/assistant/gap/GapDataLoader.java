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
package io.github.mtrevisan.familylegacy.ui.assistant.gap;

import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchActivityReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateNormalizer;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.ui.assistant.chronology.DateRange;
import io.github.mtrevisan.familylegacy.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchActivityHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchTaskHandler;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Reads from the model what the research gap checks need. The individuals hidden by the privacy settings are left out, as they are from
 * the views.
 */
public final class GapDataLoader{

	private static final String ROLE_PRINCIPAL = "principal";
	private static final String TYPE_RESIDENCE = "residence";
	private static final String EVENT_CENSUS = "census";
	private static final String TAG_FOLLOW_UP = ResearchActivityReader.TAG_FOLLOW_UP;
	private static final long UNIX_EPOCH_JDN = 2_440_588L;


	private GapDataLoader(){}


	public static GapData load(final FLEFModel model, final LocalDate today){
		final Map<String, FLEFRecord> individuals = new LinkedHashMap<>();
		for(final FLEFRecord individual : model.getRecordsByType(IndividualHandler.TYPE))
			if(individual.getId() != null && !PrivacyService.isHidden(model, individual))
				individuals.put(individual.getId(), individual);

		final Map<String, List<String>> principals = new HashMap<>();
		final Map<String, Set<String>> eventsOfPerson = new HashMap<>();
		for(final FLEFRecord participation : model.getRecordsByType(EventParticipationHandler.TYPE)){
			final String personId = armValue(FLEFRecordHelper.findChild(participation, EventParticipationReader.TAG_PARTICIPANT));
			final String eventId = FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_EVENT);
			final String role = FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_ROLE);
			if(personId == null || eventId == null || !individuals.containsKey(personId)
					|| (role != null && !ROLE_PRINCIPAL.equals(role)))
				continue;

			principals.computeIfAbsent(eventId, k -> new ArrayList<>())
				.add(personId);
			eventsOfPerson.computeIfAbsent(personId, k -> new HashSet<>())
				.add(eventId);
		}

		final List<GapData.Event> events = new ArrayList<>();
		final Map<String, Integer> birthYears = new HashMap<>();
		final Set<String> withBirth = new HashSet<>();
		final Set<String> withDeath = new HashSet<>();
		final Set<String> withCensus = new HashSet<>();
		for(final Map.Entry<String, List<String>> entry : principals.entrySet()){
			final FLEFRecord event = model.getRecordById(entry.getKey());
			if(!EventHandler.isOfType(event))
				continue;

			final String type = EventReader.extractType(event);
			events.add(new GapData.Event(entry.getKey(), type, FLEFRecordHelper.findChildren(event, EventReader.TAG_SOURCE).size(),
				entry.getValue()));
			for(final String personId : entry.getValue())
				if(EventReader.ENUM_TYPE_BIRTH.equals(type)){
					withBirth.add(personId);
					final Integer year = year(event);
					if(year != null)
						birthYears.merge(personId, year, Math::min);
				}
				else if(EventReader.ENUM_TYPE_DEATH.equals(type))
					withDeath.add(personId);
				else if(EVENT_CENSUS.equals(type))
					withCensus.add(personId);
		}

		final Set<String> withResidence = new HashSet<>();
		for(final FLEFRecord attribute : model.getRecordsByType(IndividualAttributeHandler.TYPE))
			if(TYPE_RESIDENCE.equals(FLEFRecordHelper.getChildValue(attribute, IndividualAttributeReader.TAG_TYPE)))
				withResidence.add(FLEFRecordHelper.getChildValue(attribute, IndividualAttributeReader.TAG_INDIVIDUAL));

		final Map<String, GapData.Person> persons = new LinkedHashMap<>();
		for(final FLEFRecord individual : individuals.values()){
			final String id = individual.getId();
			final String name = IndividualReader.extractPrimaryFullname(individual);
			persons.put(id, new GapData.Person(id, (name != null && !name.isBlank()? name: id), birthYears.get(id),
				withBirth.contains(id), withDeath.contains(id), withResidence.contains(id), withCensus.contains(id),
				eventsOfPerson.getOrDefault(id, Set.of()).size()));
		}

		final List<GapData.ParentLink> parents = new ArrayList<>();
		final List<GapData.Union> unions = new ArrayList<>();
		for(final FLEFRecord relationship : model.getRecordsByType(RelationshipHandler.TYPE)){
			final String type = RelationshipReader.extractType(relationship);
			final String subject = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualHandler.TYPE);
			final String target = relationship.extractReferencedId(RelationshipReader.TAG_TARGET, IndividualHandler.TYPE);
			if(type == null || subject == null || target == null || !persons.containsKey(subject) || !persons.containsKey(target))
				continue;

			//the subject of a relationship of descent is the child, the target the parent
			if(RelationshipReader.isTypeChild(type))
				parents.add(new GapData.ParentLink(subject, target));
			else if(RelationshipReader.isTypePartner(type))
				unions.add(new GapData.Union(subject, target));
		}

		return loadResearch(model, today, persons, events, parents, unions);
	}


	private static GapData loadResearch(final FLEFModel model, final LocalDate today, final Map<String, GapData.Person> persons,
			final List<GapData.Event> events, final List<GapData.ParentLink> parents, final List<GapData.Union> unions){
		final List<GapData.Question> questions = new ArrayList<>();
		for(final FLEFRecord question : model.getRecordsByType(ResearchQuestionHandler.TYPE))
			if(question.getId() != null)
				questions.add(new GapData.Question(question.getId(), ResearchQuestionReader.extractTitle(question),
					ResearchQuestionReader.extractStatus(question), notBlank(FLEFRecordHelper.getChildValue(question,
						ResearchQuestionReader.TAG_CONCLUSION)),
					targets(question)));

		final List<FLEFRecord> activityRecords = model.getRecordsByType(ResearchActivityHandler.TYPE);
		final List<FLEFRecord> taskRecords = model.getRecordsByType(ResearchTaskHandler.TYPE);
		final Set<String> parentActivities = new HashSet<>();
		for(final FLEFRecord activity : activityRecords){
			final String parent = FLEFRecordHelper.getChildValue(activity, ResearchActivityReader.TAG_PARENT_ACTIVITY);
			if(parent != null)
				parentActivities.add(parent);
		}
		final Set<String> activitiesWithTasks = new HashSet<>();
		for(final FLEFRecord task : taskRecords){
			final String createdBy = FLEFRecordHelper.getChildValue(task, ResearchTaskReader.TAG_CREATED_BY);
			if(createdBy != null)
				activitiesWithTasks.add(createdBy);
		}

		final List<GapData.Activity> activities = new ArrayList<>();
		for(final FLEFRecord activity : activityRecords)
			if(activity.getId() != null)
				activities.add(new GapData.Activity(activity.getId(), ResearchActivityReader.extractAction(activity),
					ResearchActivityReader.extractStatus(activity), ResearchActivityReader.extractResult(activity),
					values(activity, ResearchActivityReader.TAG_QUESTION),
					!FLEFRecordHelper.findChildren(activity, TAG_FOLLOW_UP).isEmpty() || activitiesWithTasks.contains(activity.getId()),
					parentActivities.contains(activity.getId())));

		final List<GapData.Task> tasks = new ArrayList<>();
		for(final FLEFRecord task : taskRecords)
			if(task.getId() != null)
				tasks.add(new GapData.Task(task.getId(), ResearchTaskReader.extractDescription(task), ResearchTaskReader.extractStatus(task),
					ResearchTaskReader.extractPriority(task), due(FLEFRecordHelper.getChildValue(task, ResearchTaskReader.TAG_DUE_DATE)),
					values(task, ResearchTaskReader.TAG_QUESTION)));

		return new GapData(today, persons, events, parents, unions, questions, activities, tasks);
	}


	private static List<String> values(final FLEFRecord record, final String tag){
		final List<String> values = new ArrayList<>();
		for(final FLEFRecord child : FLEFRecordHelper.findChildren(record, tag))
			if(child.getValue() != null)
				values.add(child.getValue());
		return values;
	}

	/** @return the records a question is about: each target is a "oneof" that holds one reference */
	private static List<String> targets(final FLEFRecord question){
		final List<String> targets = new ArrayList<>();
		for(final FLEFRecord target : FLEFRecordHelper.findChildren(question, ResearchQuestionReader.TAG_TARGET)){
			final String id = armValue(target);
			if(id != null)
				targets.add(id);
		}
		return targets;
	}

	private static boolean notBlank(final String text){
		return (text != null && !text.isBlank());
	}

	/** @return the day a task is due (an ISO date), or {@code null} if there is none or it cannot be read */
	private static DateRange due(final String text){
		if(!notBlank(text))
			return null;

		try{
			return DateRange.day(LocalDate.parse(text.strip().substring(0, Math.min(10, text.strip().length()))).toEpochDay()
				+ UNIX_EPOCH_JDN);
		}
		catch(final DateTimeParseException ignored){
			return null;
		}
	}

	/** @return the year of the date of an event, or {@code null} if it has none, or not a bounded one */
	private static Integer year(final FLEFRecord event){
		final List<FLEFRecord> dates = FLEFRecordHelper.extractStructures(event, EventReader.TAG_DATE);
		if(dates.isEmpty())
			return null;

		try{
			final DateRange range = DateRange.of(DateNormalizer.normalize(dates.getFirst()));
			return (range != null && range.min() != Long.MIN_VALUE
				? CalendarType.jdnToProlepticGregorianLocalDate(range.min()).getYear()
				: null);
		}
		catch(final RuntimeException ignored){
			return null;
		}
	}

	/** @return the value of the alternative a "oneof" holds (an Xref), or {@code null} */
	private static String armValue(final FLEFRecord union){
		return (union == null || union.getChildren().isEmpty()? null: union.getChildren().getFirst().getValue());
	}

}
