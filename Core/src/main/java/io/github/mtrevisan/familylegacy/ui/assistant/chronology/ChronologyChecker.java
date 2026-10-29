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
package io.github.mtrevisan.familylegacy.ui.assistant.chronology;

import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticKeys;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Detects the chronological inconsistencies of the research: a child born before the parent, a parent too young (or too old),
 * a child born after the death of the parent, an event after the death or before the birth, a union before the birth or after the
 * death of a partner, a death before the birth, an implausible lifespan, overlapping marriages.
 * <p>
 * A finding is reported only when it holds for every day the dates may stand for ({@link DateRange}): an uncertain date never
 * raises a false alarm. The age limits are conventions ({@link Rules}), so their findings are warnings, not errors. Every finding
 * explains its reasoning, and none of them changes the data.
 * </p>
 */
final class ChronologyChecker{

	/**
	 * The limits of what is plausible.
	 *
	 * @param minMotherAgeYears the youngest age of a mother
	 * @param minFatherAgeYears the youngest age of a father
	 * @param maxMotherAgeYears the oldest age of a mother
	 * @param maxFatherAgeYears the oldest age of a father (also for a parent of unknown sex)
	 * @param maxGestationDays  the longest time between the death of the father and the birth of the child
	 * @param maxLifespanYears  the longest plausible life
	 */
	record Rules(int minMotherAgeYears, int minFatherAgeYears, int maxMotherAgeYears, int maxFatherAgeYears, int maxGestationDays,
			int maxLifespanYears){

		static final Rules DEFAULT = new Rules(12, 14, 55, 90, 300, 120);

	}


	/** The events that happen after the death (or are about it): they are not reported when the date is after the death. */
	private static final Set<String> POSTHUMOUS_TYPES = Set.of(EventReader.ENUM_TYPE_DEATH, EventReader.ENUM_TYPE_BURIAL,
		EventReader.ENUM_TYPE_CREMATION, "coroner_report", "probate", "honor", "pardon", "deed", "escrow", "chancery");
	private static final Set<String> MARRIAGE_TYPES = Set.of(EventReader.ENUM_TYPE_MARRIAGE, EventReader.ENUM_TYPE_MARRIAGE_BANN,
		EventReader.ENUM_TYPE_MARRIAGE_CONTRACT, EventReader.ENUM_TYPE_MARRIAGE_LICENSE, EventReader.ENUM_TYPE_MARRIAGE_SETTLEMENT,
		"engagement");

	/** Years are counted with 365 days for the lower limits and 366 for the upper ones, so that a limit is never exceeded by rounding. */
	private static final int DAYS_IN_YEAR_SHORT = 365;
	private static final int DAYS_IN_YEAR_LONG = 366;


	private ChronologyChecker(){}


	static List<Diagnostic> check(final ChronologyData data){
		return check(data, Rules.DEFAULT);
	}

	static List<Diagnostic> check(final ChronologyData data, final Rules rules){
		final List<Diagnostic> findings = new ArrayList<>();
		checkLifespans(data, rules, findings);
		checkEvents(data, findings);
		checkParents(data, rules, findings);
		checkUnions(data, findings);
		checkOverlappingMarriages(data, findings);
		return findings;
	}


	private static void checkLifespans(final ChronologyData data, final Rules rules, final List<Diagnostic> findings){
		for(final ChronologyData.Person person : data.persons().values()){
			if(person.birth() == null || person.death() == null)
				continue;

			if(person.death().isBefore(person.birth()))
				findings.add(finding(Diagnostic.Severity.ERROR, "death_before_birth", List.of(person.id()),
					person.name(), person.deathText(), person.birthText()));
			else{
				final long days = spanDays(person.death().min(), person.birth().max());
				if(days > (long)rules.maxLifespanYears() * DAYS_IN_YEAR_LONG)
					findings.add(finding(Diagnostic.Severity.WARNING, "implausible_lifespan", List.of(person.id()),
						person.name(), days / DAYS_IN_YEAR_LONG, rules.maxLifespanYears(), person.birthText(), person.deathText()));
			}
		}
	}

	private static void checkEvents(final ChronologyData data, final List<Diagnostic> findings){
		for(final ChronologyData.LifeEvent event : data.events()){
			final ChronologyData.Person person = data.persons().get(event.personId());
			if(person == null || event.date() == null)
				continue;

			final String type = event.type();
			if(person.birth() != null && !EventReader.ENUM_TYPE_BIRTH.equals(type) && event.date().isBefore(person.birth()))
				findings.add(finding(Diagnostic.Severity.ERROR, (MARRIAGE_TYPES.contains(type)? "marriage_before_birth": "event_before_birth"),
					List.of(person.id(), event.eventId()), person.name(), type, event.dateText(), person.birthText()));
			if(person.death() != null && !POSTHUMOUS_TYPES.contains(type) && person.death().isBefore(event.date()))
				findings.add(finding(Diagnostic.Severity.ERROR, "event_after_death", List.of(person.id(), event.eventId()),
					person.name(), type, event.dateText(), person.deathText()));
		}
	}

	private static void checkParents(final ChronologyData data, final Rules rules, final List<Diagnostic> findings){
		for(final ChronologyData.ParentLink link : data.parents()){
			//a child that does not descend from the parent (adoptive, foster, guarded, step) can be older than the parent
			final ChronologyData.Person child = data.persons().get(link.childId());
			final ChronologyData.Person parent = data.persons().get(link.parentId());
			if(!link.biological() || child == null || parent == null)
				continue;

			final List<String> ids = List.of(child.id(), parent.id(), link.relationshipId());
			if(child.birth() != null && parent.birth() != null){
				if(child.birth().isBefore(parent.birth())){
					findings.add(finding(Diagnostic.Severity.ERROR, "child_before_parent", ids, child.name(), parent.name(),
						child.birthText(), parent.birthText()));
					continue;
				}

				final boolean female = (parent.sex() == SexType.FEMALE);
				final int minYears = (female? rules.minMotherAgeYears(): (parent.sex() == SexType.MALE
					? rules.minFatherAgeYears(): Math.min(rules.minMotherAgeYears(), rules.minFatherAgeYears())));
				if(spanDays(child.birth().max(), parent.birth().min()) < (long)minYears * DAYS_IN_YEAR_SHORT)
					findings.add(finding(Diagnostic.Severity.WARNING, "parent_too_young", ids, parent.name(), child.name(), minYears,
						parent.birthText(), child.birthText()));

				final int maxYears = (female? rules.maxMotherAgeYears(): rules.maxFatherAgeYears());
				if(spanDays(child.birth().min(), parent.birth().max()) > (long)maxYears * DAYS_IN_YEAR_LONG)
					findings.add(finding(Diagnostic.Severity.WARNING, "parent_too_old", ids, parent.name(), child.name(), maxYears,
						parent.birthText(), child.birthText()));
			}

			if(child.birth() != null && parent.death() != null){
				if(parent.sex() == SexType.FEMALE){
					if(parent.death().isBefore(child.birth()))
						findings.add(finding(Diagnostic.Severity.ERROR, "born_after_mother_death", ids, child.name(), parent.name(),
							child.birthText(), parent.deathText()));
				}
				else if(DateRange.plus(parent.death().max(), rules.maxGestationDays()) < child.birth().min())
					findings.add(finding(Diagnostic.Severity.ERROR, "born_after_father_death", ids, child.name(), parent.name(),
						rules.maxGestationDays(), child.birthText(), parent.deathText()));
			}
		}
	}

	private static void checkUnions(final ChronologyData data, final List<Diagnostic> findings){
		for(final ChronologyData.Union union : data.unions()){
			if(union.from() == null)
				continue;

			for(final String partnerId : List.of(union.firstId(), union.secondId())){
				final ChronologyData.Person partner = data.persons().get(partnerId);
				if(partner == null)
					continue;

				final List<String> ids = List.of(partner.id(), union.relationshipId());
				if(partner.birth() != null && union.from().isBefore(partner.birth()))
					findings.add(finding(Diagnostic.Severity.ERROR, "union_before_birth", ids, partner.name(), union.fromText(),
						partner.birthText()));
				if(partner.death() != null && partner.death().isBefore(union.from()))
					findings.add(finding(Diagnostic.Severity.ERROR, "union_after_death", ids, partner.name(), union.fromText(),
						partner.deathText()));
			}
		}
	}

	/** Two marriages of the same person that certainly overlap: it is not an error (polygamy exists), but it is worth a look. */
	private static void checkOverlappingMarriages(final ChronologyData data, final List<Diagnostic> findings){
		final Set<String> reported = new HashSet<>();
		for(final ChronologyData.Person person : data.persons().values()){
			final List<ChronologyData.Union> marriages = data.unions().stream()
				.filter(ChronologyData.Union::marriage)
				.filter(u -> person.id().equals(u.firstId()) || person.id().equals(u.secondId()))
				.toList();
			for(int i = 0; i < marriages.size(); i ++)
				for(int j = i + 1; j < marriages.size(); j ++){
					final ChronologyData.Union first = marriages.get(i);
					final ChronologyData.Union second = marriages.get(j);
					final String firstPartner = otherPartner(first, person.id());
					final String secondPartner = otherPartner(second, person.id());
					//a civil and a religious marriage of the same couple are not two marriages
					if(firstPartner.equals(secondPartner) || !reported.add(first.relationshipId() + "|" + second.relationshipId()))
						continue;

					final long firstEnd = certainEnd(first, data.persons());
					final long secondEnd = certainEnd(second, data.persons());
					if(first.from() != null && second.from() != null && first.from().max() <= secondEnd
							&& second.from().max() <= firstEnd)
						findings.add(finding(Diagnostic.Severity.INFORMATION, "overlapping_marriages",
							List.of(person.id(), first.relationshipId(), second.relationshipId()), person.name(),
							name(data, firstPartner), name(data, secondPartner)));
				}
		}
	}

	/**
	 * @return the last day the union is sure to be in effect: the end of the union, or the first death that can end it; the lowest
	 * value if the end is not known
	 */
	private static long certainEnd(final ChronologyData.Union union, final Map<String, ChronologyData.Person> persons){
		if(union.to() != null)
			return union.to().min();
		if(union.endedUnknown())
			return Long.MIN_VALUE;

		long end = Long.MAX_VALUE;
		for(final String id : List.of(union.firstId(), union.secondId())){
			final ChronologyData.Person partner = persons.get(id);
			if(partner != null && partner.death() != null)
				end = Math.min(end, partner.death().min());
		}
		return end;
	}

	private static String otherPartner(final ChronologyData.Union union, final String personId){
		return (personId.equals(union.firstId())? union.secondId(): union.firstId());
	}

	private static String name(final ChronologyData data, final String id){
		final ChronologyData.Person person = data.persons().get(id);
		return (person != null? person.name(): id);
	}

	/**
	 * @return the days from {@code earlier} to {@code later}; the largest (or smallest) value if an end is open, so that an
	 * unknown date never makes a limit look exceeded
	 */
	private static long spanDays(final long later, final long earlier){
		if(later == Long.MAX_VALUE || earlier == Long.MIN_VALUE)
			return Long.MAX_VALUE;
		if(later == Long.MIN_VALUE || earlier == Long.MAX_VALUE)
			return Long.MIN_VALUE;
		return later - earlier;
	}

	private static Diagnostic finding(final Diagnostic.Severity severity, final String key, final List<String> recordIds,
			final Object... arguments){
		return new Diagnostic(severity, AssistantCategory.DIAGNOSTICS, I18N.tf("chronology." + key + ".title", arguments),
			I18N.tf("chronology." + key + ".explanation", arguments), recordIds, null,
			DiagnosticKeys.of("chronology." + key, recordIds, arguments));
	}

}
