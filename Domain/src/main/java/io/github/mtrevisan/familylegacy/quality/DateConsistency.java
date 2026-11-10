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
package io.github.mtrevisan.familylegacy.quality;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PreferredSex;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateNormalizer;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateRange;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * The dates that cannot be right, whatever the day: a death before the birth, an event after the death or before the birth of one who
 * took part in it, a child born before the parent was, or after the death of the mother (or too long after the death of the father),
 * a union that begins before a partner was born or after they died, a membership of a group that begins before the member was born
 * or after they died. They are errors of the file, because no day that the dates may
 * stand for makes them true.
 * <p>
 * A date is rarely exact. A year covers 366 days, a decade 3653, a century 36525; an approximate date has a margin (two years if it
 * says none); a bounded date lies between two days; an open one has an end missing. Each date is therefore the {@link DateRange} of the
 * days it may stand for, and a problem is reported only if <em>every</em> day of one range is before <em>every</em> day of the other,
 * so that an uncertain date never raises a false alarm: a death in the 1850s is not before a birth in 1855, and one in the 18th century
 * is not before a birth in the 18th century. With several births (or deaths) of the same person the days they all cover are used.
 * <p>
 * The dates that cannot be read are reported too (a warning): a date that is not valid in its calendar, and a date that is not checked
 * because the plugin of its calendar is missing, which is not a problem of the file.
 * <p>
 * Only what is provable is here. The ages (a mother of twelve, a life of over a hundred and twenty years) are conventions, and are
 * warnings of the Chronology Assistant.
 */
public final class DateConsistency{

	/** The events that happen after the death (or are about it): a date after the death is not a problem for them. */
	private static final Set<String> POSTHUMOUS_TYPES = Set.of(EventReader.ENUM_TYPE_DEATH, EventReader.ENUM_TYPE_BURIAL,
		EventReader.ENUM_TYPE_CREMATION, EventReader.ENUM_TYPE_CORONER_REPORT, EventReader.ENUM_TYPE_PROBATE, EventReader.ENUM_TYPE_HONOR,
		EventReader.ENUM_TYPE_PARDON, EventReader.ENUM_TYPE_DEED, EventReader.ENUM_TYPE_ESCROW, EventReader.ENUM_TYPE_CHANCERY,
		EventReader.CUSTOM_TYPE_ANECDOTE, EventReader.CUSTOM_TYPE_COMMEMORATION);
	/** @return	Whether an event of that type can be dated after the death of the person (a burial, a probate, a commemoration). */
	public static boolean isPosthumous(final String type){
		return POSTHUMOUS_TYPES.contains(type);
	}

	/** The longest time between the death of the father and the birth of the child, in days. */
	private static final int MAX_GESTATION_DAYS = 300;
	private static final String ROLE_PRINCIPAL = EventParticipationReader.ENUM_ROLE_PRINCIPAL;
	private static final Set<String> BIOLOGICAL_TYPES = Set.of(RelationshipReader.ENUM_TYPE_CHILD,
		RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD);

	private static final String KEY_PREFIX = "text.consistency.issue.";


	private DateConsistency(){}


	/** The birth and the death of an individual, as the days they may stand for ({@code null} if not known), and as written. */
	private record Person(String id, String name, SexType sex, DateRange birth, DateRange death, String birthText, String deathText){}

	/** An event that an individual took part in. */
	private record LifeEvent(String eventId, String personId, String type, DateRange date, String dateText, boolean principal){}


	/**
	 * @param model	The model.
	 * @return	The errors, each naming the people and the dates as written.
	 */
	public static List<ConsistencyService.Issue> check(final FLEFModel model){
		final List<ConsistencyService.Issue> issues = new ArrayList<>();
		checkReadable(model, issues);

		final List<LifeEvent> events = loadEvents(model);
		final Map<String, Person> persons = new LinkedHashMap<>();
		final Map<String, List<FLEFRecord>> sexes = PreferredSex.indexByIndividual(model);
		for(final FLEFRecord individual : model.getRecordsByType(IndividualReader.TYPE))
			if(individual.getId() != null)
				persons.put(individual.getId(), loadPerson(individual, events, PreferredSex.of(individual, sexes, model)));

		checkLifespans(persons, issues);
		checkEvents(persons, events, issues);
		for(final FLEFRecord relationship : model.getRecordsByType(RelationshipReader.TYPE)){
			final String type = RelationshipReader.extractType(relationship);
			final Person subject = persons.get(relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualReader.TYPE));
			final Person target = persons.get(relationship.extractReferencedId(RelationshipReader.TAG_TARGET, IndividualReader.TYPE));
			if(type == null || subject == null || target == null)
				continue;

			if(RelationshipReader.isTypeChild(type) && BIOLOGICAL_TYPES.contains(type))
				//the subject is the child, the target the parent
				checkParent(relationship.getId(), subject, target, issues);
			else if(RelationshipReader.isTypePartner(type))
				checkUnion(relationship, subject, target, issues);
		}
		for(final FLEFRecord relationship : model.getRecordsByType(RelationshipReader.TYPE)){
			if(!RelationshipReader.isTypeGroupMember(RelationshipReader.extractType(relationship)))
				continue;

			final Person member = persons.get(relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualReader.TYPE));
			if(member != null)
				checkMembership(relationship, member, issues);
		}
		return issues;
	}


	/**
	 * The dates that cannot be read, so that the checks of the others are not silent about them: a date that is not valid in the notation
	 * of its calendar is a problem of the file; one whose calendar needs the Ephemeris plugin, which is missing, is not: it is not checked,
	 * and the file is not wrong. Both are warnings, in two kinds, so that the second is not mistaken for the first.
	 */
	private static void checkReadable(final FLEFModel model, final List<ConsistencyService.Issue> issues){
		for(final FLEFRecord event : model.getRecordsByType(EventReader.TYPE))
			for(final FLEFRecord date : FLEFRecordHelper.extractStructures(event, EventReader.TAG_DATE))
				checkReadable(model, event, EventReader.TAG_DATE, date, issues);
		for(final String type : List.of(RelationshipReader.TYPE, PlaceRelationshipReader.TYPE))
			for(final FLEFRecord relationship : model.getRecordsByType(type))
				for(final String tag : List.of(RelationshipReader.TAG_VALID_FROM, RelationshipReader.TAG_VALID_TO))
					checkReadable(model, relationship, tag, FLEFRecordHelper.findChild(relationship, tag), issues);
	}

	private static void checkReadable(final FLEFModel model, final FLEFRecord record, final String field, final FLEFRecord date,
			final List<ConsistencyService.Issue> issues){
		final DateNormalizer.Problem problem = DateNormalizer.problem(date);
		if(problem == DateNormalizer.Problem.NONE)
			return;

		final String kind = (problem == DateNormalizer.Problem.INVALID? "date_invalid": "date_not_checked");
		issues.add(new ConsistencyService.Issue(ConsistencyService.Severity.WARNING, record.getId(),
			I18N.tf(KEY_PREFIX + kind, RecordDisplay.describe(record, model), field, text(date)), kind));
	}

	private static void checkLifespans(final Map<String, Person> persons, final List<ConsistencyService.Issue> issues){
		for(final Person person : persons.values())
			if(person.birth() != null && person.death() != null && person.death().isBefore(person.birth()))
				issues.add(issue("death_before_birth", person.id(), person.name(), person.deathText(), person.birthText()));
	}

	/**
	 * An event before the birth or after the death of a participant; a birth, and the events that follow the death, are exempt. The birth
	 * and the death of the individual are the dates that the others are compared to: they are not judged against each other here (a death
	 * before the birth is one problem, {@code death_before_birth}, not three).
	 */
	private static void checkEvents(final Map<String, Person> persons, final List<LifeEvent> events,
			final List<ConsistencyService.Issue> issues){
		for(final LifeEvent event : events){
			final Person person = persons.get(event.personId());
			if(person == null || event.date() == null)
				continue;

			final String type = event.type();
			if(event.principal() && (EventReader.ENUM_TYPE_BIRTH.equals(type) || EventReader.ENUM_TYPE_DEATH.equals(type)))
				continue;

			if(person.birth() != null && !EventReader.ENUM_TYPE_BIRTH.equals(type) && event.date().isBefore(person.birth()))
				issues.add(issue("event_before_birth", event.eventId(), person.name(), type, event.dateText(), person.birthText()));
			if(person.death() != null && !POSTHUMOUS_TYPES.contains(type) && person.death().isBefore(event.date()))
				issues.add(issue("event_after_death", event.eventId(), person.name(), type, event.dateText(), person.deathText()));
		}
	}

	/** A child that descends from the parent (an adoptive, foster, guarded or step child can be older than the parent). */
	private static void checkParent(final String relationshipId, final Person child, final Person parent,
			final List<ConsistencyService.Issue> issues){
		if(child.birth() == null)
			return;

		if(parent.birth() != null && child.birth().isBefore(parent.birth())){
			issues.add(issue("child_before_parent", relationshipId, child.name(), parent.name(), child.birthText(), parent.birthText()));
			return;
		}

		if(parent.death() == null)
			return;

		if(parent.sex() == SexType.FEMALE){
			if(parent.death().isBefore(child.birth()))
				issues.add(issue("born_after_mother_death", relationshipId, child.name(), parent.name(), child.birthText(),
					parent.deathText()));
		}
		//the father, or a parent of unknown sex: a child is not born more than the gestation after the death
		else if(DateRange.plus(parent.death().max(), MAX_GESTATION_DAYS) < child.birth().min())
			issues.add(issue("born_after_father_death", relationshipId, child.name(), parent.name(), MAX_GESTATION_DAYS,
				child.birthText(), parent.deathText()));
	}

	/** A union that begins before a partner was born or after they died. */
	private static void checkUnion(final FLEFRecord relationship, final Person first, final Person second,
			final List<ConsistencyService.Issue> issues){
		final FLEFRecord fromStructure = FLEFRecordHelper.findChild(relationship, RelationshipReader.TAG_VALID_FROM);
		final DateRange from = range(fromStructure);
		if(from == null)
			return;

		final String fromText = text(fromStructure);
		for(final Person partner : List.of(first, second)){
			if(partner.birth() != null && from.isBefore(partner.birth()))
				issues.add(issue("union_before_birth", relationship.getId(), partner.name(), fromText, partner.birthText()));
			if(partner.death() != null && partner.death().isBefore(from))
				issues.add(issue("union_after_death", relationship.getId(), partner.name(), fromText, partner.deathText()));
		}
	}


	/** A membership of a group that begins before the member was born or after they died, or ends before they were born. */
	private static void checkMembership(final FLEFRecord relationship, final Person member, final List<ConsistencyService.Issue> issues){
		final FLEFRecord fromStructure = FLEFRecordHelper.findChild(relationship, RelationshipReader.TAG_VALID_FROM);
		final FLEFRecord toStructure = FLEFRecordHelper.findChild(relationship, RelationshipReader.TAG_VALID_TO);
		final DateRange from = range(fromStructure);
		final DateRange to = range(toStructure);
		if(from != null){
			if(member.birth() != null && from.isBefore(member.birth()))
				issues.add(issue("membership_before_birth", relationship.getId(), member.name(), text(fromStructure), member.birthText()));
			if(member.death() != null && member.death().isBefore(from))
				issues.add(issue("membership_after_death", relationship.getId(), member.name(), text(fromStructure), member.deathText()));
		}
		//an end before the birth, when the beginning does not already say it (the beginning is missing, or it is after the birth)
		if(to != null && member.birth() != null && to.isBefore(member.birth()) && (from == null || !from.isBefore(member.birth())))
			issues.add(issue("membership_ended_before_birth", relationship.getId(), member.name(), text(toStructure), member.birthText()));
	}


	/* ======================================================================
	 *                          Reading the dates
	 * ====================================================================== */

	private static List<LifeEvent> loadEvents(final FLEFModel model){
		final List<LifeEvent> events = new ArrayList<>();
		for(final FLEFRecord participation : model.getRecordsByType(EventParticipationReader.TYPE)){
			final FLEFRecord participant = FLEFRecordHelper.findChild(participation, EventParticipationReader.TAG_PARTICIPANT);
			final FLEFRecord reference = (participant != null? participant.getTheOnlyChild(): null);
			final String personId = (reference != null && IndividualReader.isOfType(reference)? reference.getValue(): null);
			final String eventId = FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_EVENT);
			if(personId == null || eventId == null)
				continue;

			final FLEFRecord event = model.getRecordById(eventId);
			if(event == null || !EventReader.TYPE.equalsIgnoreCase(event.getTag()))
				continue;

			final String role = FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_ROLE);
			final List<FLEFRecord> dates = FLEFRecordHelper.extractStructures(event, EventReader.TAG_DATE);
			final FLEFRecord date = (dates.isEmpty()? null: dates.getFirst());
			events.add(new LifeEvent(eventId, personId, EventReader.extractType(event), range(date), text(date),
				role == null || ROLE_PRINCIPAL.equals(role)));
		}
		return events;
	}

	/** The birth and the death of an individual are the events of that type the individual is the principal of. */
	private static Person loadPerson(final FLEFRecord individual, final List<LifeEvent> events, final SexType sex){
		DateRange birth = null;
		DateRange death = null;
		String birthText = null;
		String deathText = null;
		for(final LifeEvent event : events){
			if(!event.principal() || !individual.getId().equals(event.personId()) || event.date() == null)
				continue;

			//with several events of the same type the days they all cover are used: a problem must hold for every one of them
			if(EventReader.ENUM_TYPE_BIRTH.equals(event.type())){
				birth = (birth != null? birth.hull(event.date()): event.date());
				birthText = (birthText != null? birthText + " / " + event.dateText(): event.dateText());
			}
			else if(EventReader.ENUM_TYPE_DEATH.equals(event.type())){
				death = (death != null? death.hull(event.date()): event.date());
				deathText = (deathText != null? deathText + " / " + event.dateText(): event.dateText());
			}
		}

		final String name = IndividualReader.extractPrimaryFullname(individual);
		return new Person(individual.getId(), (StringUtils.isNotBlank(name)? name + " (" + individual.getId() + ")": individual.getId()),
			sex, birth, death, birthText, deathText);
	}

	/** @return the days of a date structure, {@code null} if there is none or it cannot be read (it is judged where it is edited) */
	private static DateRange range(final FLEFRecord dateStructure){
		if(dateStructure == null)
			return null;

		try{
			return DateRange.of(DateNormalizer.normalize(dateStructure));
		}
		catch(final RuntimeException ignored){
			return null;
		}
	}

	private static String text(final FLEFRecord dateStructure){
		if(dateStructure == null)
			return StringUtils.EMPTY;

		try{
			return DateReader.extractPrettyPrintDate(dateStructure);
		}
		catch(final RuntimeException ignored){
			return StringUtils.EMPTY;
		}
	}

	private static ConsistencyService.Issue issue(final String kind, final String recordId, final Object... arguments){
		return new ConsistencyService.Issue(ConsistencyService.Severity.ERROR, recordId, I18N.tf(KEY_PREFIX + kind, arguments), kind);
	}

}
