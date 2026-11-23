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
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PreferredSex;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateNormalizer;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateRange;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;

import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * The dates that cannot be right, whatever the day: a death before the birth, an event after the death or before the birth of one who
 * took part in it, a child born before the parent was, or after the death of the mother (or too long after the death of the father),
 * a union that begins before a partner was born or after they died, a membership of a group, or an attribute (a residence, an
 * occupation), that begins before the member was born or after they died. They are errors of the file, because no day that the dates may
 * stand for makes them true. The rules themselves are in {@link DateRules}, shared with the Chronology Assistant.
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

	/** @return	Whether an event of that type can be dated after the death of the person (a burial, a probate, a commemoration). */
	public static boolean isPosthumous(final String type){
		return DateRules.isPosthumous(type);
	}

	/**
	 * The events that a record of the time attests: a birth, a death (or a burial) is written down when, or after, it
	 * happens. A marriage is not here: its banns are published, and cited, before the wedding.
	 */
	private static final Set<String> RECORDED_AFTER_TYPES = Set.of(EventReader.ENUM_TYPE_BIRTH, EventReader.ENUM_TYPE_DEATH, EventReader.ENUM_TYPE_BURIAL,
		EventReader.ENUM_TYPE_CREMATION);

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
		checkCitedSources(model, issues);

		final List<LifeEvent> events = loadEvents(model);
		final Map<String, List<LifeEvent>> eventsByPerson = new HashMap<>();
		for(final LifeEvent event : events)
			eventsByPerson.computeIfAbsent(event.personId(), k -> new ArrayList<>())
				.add(event);
		final Map<String, Person> persons = new LinkedHashMap<>();
		final Map<String, List<FLEFRecord>> sexes = PreferredSex.indexByIndividual(model);
		for(final FLEFRecord individual : model.getRecordsByType(IndividualReader.TYPE))
			if(individual.getId() != null)
				persons.put(individual.getId(), loadPerson(individual, eventsByPerson.getOrDefault(individual.getId(), List.of()),
					PreferredSex.of(individual, sexes, model)));

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
		for(final FLEFRecord attribute : model.getRecordsByType(IndividualAttributeReader.TYPE)){
			final Person owner = persons.get(IndividualAttributeReader.extractIndividualId(attribute));
			if(owner != null)
				checkAttribute(model, attribute, owner, issues);
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
		for(final String type : List.of(RelationshipReader.TYPE, PlaceRelationshipReader.TYPE, IndividualAttributeReader.TYPE))
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

	/**
	 * A source dated before the event it is cited for: the record of a birth, a baptism, a death or a burial is normally not older than
	 * what it records (a year that was typed wrong, in the event or in the source, is the usual cause). It is only a warning, though, not
	 * an error: a source can be older than the event if it was completed afterwards (a note added in the margin of a register, a later
	 * annotation about another individual), so it is a thing to verify, not a proof. It is judged by all the days both dates may stand
	 * for, like the others.
	 */
	private static void checkCitedSources(final FLEFModel model, final List<ConsistencyService.Issue> issues){
		for(final FLEFRecord event : model.getRecordsByType(EventReader.TYPE)){
			if(!RECORDED_AFTER_TYPES.contains(EventReader.extractType(event)))
				continue;

			final List<FLEFRecord> dates = FLEFRecordHelper.extractStructures(event, EventReader.TAG_DATE);
			final DateRange eventDate = (dates.isEmpty()? null: range(dates.getFirst()));
			if(eventDate == null)
				continue;

			for(final FLEFRecord citation : FLEFRecordHelper.findChildren(event, EventReader.TAG_SOURCE)){
				if(SourceCitationReader.isContradicting(citation))
					continue;

				final FLEFRecord source = model.getRecordById(SourceCitationReader.extractSource(citation));
				if(source == null)
					continue;

				final FLEFRecord sourceDateStructure = FLEFRecordHelper.findChild(source, SourceReader.TAG_DATE);
				final DateRange sourceDate = range(sourceDateStructure);
				if(sourceDate != null && sourceDate.isBefore(eventDate))
					issues.add(new ConsistencyService.Issue(ConsistencyService.Severity.WARNING, event.getId(),
						I18N.tf(KEY_PREFIX + "source_before_event", RecordDisplay.describe(event, model), text(dates.getFirst()),
						RecordDisplay.describe(source, model), text(sourceDateStructure)), "source_before_event"));
			}
		}
	}

	private static void checkLifespans(final Map<String, Person> persons, final List<ConsistencyService.Issue> issues){
		for(final Person person : persons.values())
			if(DateRules.deathBeforeBirth(person.death(), person.birth()))
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

			if(DateRules.eventBeforeBirth(event.date(), person.birth(), type))
				issues.add(issue("event_before_birth", event.eventId(), person.name(), type, event.dateText(), person.birthText()));
			if(DateRules.eventAfterDeath(event.date(), person.death(), type))
				issues.add(issue("event_after_death", event.eventId(), person.name(), type, event.dateText(), person.deathText()));
		}
	}

	/** A child that descends from the parent (an adoptive, foster, guarded or step child can be older than the parent). */
	private static void checkParent(final String relationshipId, final Person child, final Person parent,
			final List<ConsistencyService.Issue> issues){
		if(child.birth() == null)
			return;

		if(DateRules.childBeforeParent(child.birth(), parent.birth())){
			issues.add(issue("child_before_parent", relationshipId, child.name(), parent.name(), child.birthText(), parent.birthText()));
			return;
		}

		if(parent.death() == null)
			return;

		if(parent.sex() == SexType.FEMALE){
			if(DateRules.bornAfterMotherDeath(child.birth(), parent.death()))
				issues.add(issue("born_after_mother_death", relationshipId, child.name(), parent.name(), child.birthText(),
					parent.deathText()));
		}
		//the father, or a parent of unknown sex: a child is not born more than the gestation after the death
		else if(DateRules.bornAfterFatherDeath(child.birth(), parent.death(), DateRules.DEFAULT_GESTATION_DAYS))
			issues.add(issue("born_after_father_death", relationshipId, child.name(), parent.name(), DateRules.DEFAULT_GESTATION_DAYS,
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
		for(final Person partner : List.of(first, second))
			for(final DateRules.PeriodProblem problem : DateRules.period(from, null, partner.birth(), partner.death()))
				if(problem == DateRules.PeriodProblem.BEGINS_BEFORE_BIRTH)
					issues.add(issue("union_before_birth", relationship.getId(), partner.name(), fromText, partner.birthText()));
				else
					issues.add(issue("union_after_death", relationship.getId(), partner.name(), fromText, partner.deathText()));
	}


	/** A membership of a group that begins before the member was born or after they died, or ends before they were born. */
	private static void checkMembership(final FLEFRecord relationship, final Person member, final List<ConsistencyService.Issue> issues){
		final FLEFRecord fromStructure = FLEFRecordHelper.findChild(relationship, RelationshipReader.TAG_VALID_FROM);
		final FLEFRecord toStructure = FLEFRecordHelper.findChild(relationship, RelationshipReader.TAG_VALID_TO);
		for(final DateRules.PeriodProblem problem : DateRules.period(range(fromStructure), range(toStructure), member.birth(), member.death()))
			switch(problem){
				case BEGINS_BEFORE_BIRTH -> issues.add(issue("membership_before_birth", relationship.getId(), member.name(),
					text(fromStructure), member.birthText()));
				case BEGINS_AFTER_DEATH -> issues.add(issue("membership_after_death", relationship.getId(), member.name(),
					text(fromStructure), member.deathText()));
				case ENDS_BEFORE_BIRTH -> issues.add(issue("membership_ended_before_birth", relationship.getId(), member.name(),
					text(toStructure), member.birthText()));
			}
	}

	/** An attribute of an individual (a residence, an occupation) that begins before they were born or after they died, or ends before. */
	private static void checkAttribute(final FLEFModel model, final FLEFRecord attribute, final Person owner,
			final List<ConsistencyService.Issue> issues){
		final FLEFRecord fromStructure = FLEFRecordHelper.findChild(attribute, IndividualAttributeReader.TAG_VALID_FROM);
		final FLEFRecord toStructure = FLEFRecordHelper.findChild(attribute, IndividualAttributeReader.TAG_VALID_TO);
		final String description = RecordDisplay.describe(attribute, model);
		for(final DateRules.PeriodProblem problem : DateRules.period(range(fromStructure), range(toStructure), owner.birth(), owner.death()))
			switch(problem){
				case BEGINS_BEFORE_BIRTH -> issues.add(issue("attribute_before_birth", attribute.getId(), owner.name(), description,
					text(fromStructure), owner.birthText()));
				case BEGINS_AFTER_DEATH -> issues.add(issue("attribute_after_death", attribute.getId(), owner.name(), description,
					text(fromStructure), owner.deathText()));
				case ENDS_BEFORE_BIRTH -> issues.add(issue("attribute_ended_before_birth", attribute.getId(), owner.name(), description,
					text(toStructure), owner.birthText()));
			}
	}


	/* ======================================================================
	 *                          Reading the dates
	 * ====================================================================== */

	private static List<LifeEvent> loadEvents(final FLEFModel model){
		final List<LifeEvent> events = new ArrayList<>();
		for(final FLEFRecord participation : model.getRecordsByType(EventParticipationReader.TYPE)){
			final FLEFRecord participant = FLEFRecordHelper.findChild(participation, EventParticipationReader.TAG_PARTICIPANT);
			final FLEFRecord reference = (participant != null? participant.getTheOnlyChild(): null);
			final String personId = (IndividualReader.isOfType(reference) ? reference.getValue(): null);
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
		catch(final IllegalArgumentException | DateTimeException ignored){
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
