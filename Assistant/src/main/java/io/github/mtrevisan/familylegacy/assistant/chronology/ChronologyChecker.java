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
package io.github.mtrevisan.familylegacy.assistant.chronology;

import io.github.mtrevisan.familylegacy.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.assistant.AssistantTuning;
import io.github.mtrevisan.familylegacy.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.assistant.DiagnosticKeys;
import io.github.mtrevisan.familylegacy.events.EventTypeInfoService;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateRange;
import io.github.mtrevisan.familylegacy.quality.DateRules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleConsumer;


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
public final class ChronologyChecker{

	/**
	 * The limits of what is plausible.
	 *
	 * @param minMotherAgeYears the youngest age of a mother
	 * @param minFatherAgeYears the youngest age of a father
	 * @param maxMotherAgeYears the oldest age of a mother
	 * @param maxFatherAgeYears the oldest age of a father (also for a parent of unknown sex)
	 * @param maxGestationDays  the longest time between the death of the father and the birth of the child
	 * @param maxLifespanYears  the longest plausible life
	 * @param minBirthIntervalDays the shortest plausible time between the births of two children of the same mother (not twins)
	 * @param maxTwinGapDays    the longest time between the births of twins: a closer pair is never reported
	 * <p>The youngest age for a marriage is the one of a parent of the same sex.</p>
	 */
	public record Rules(int minMotherAgeYears, int minFatherAgeYears, int maxMotherAgeYears, int maxFatherAgeYears, int maxGestationDays,
			int maxLifespanYears, int minBirthIntervalDays, int maxTwinGapDays){

		static final Rules DEFAULT = new Rules(12, 14, 55, 90, 300, 120, 270, 7);

		/** @return the rules with the numbers of the user (the defaults are the ones of {@link #DEFAULT}) */
		static Rules of(final AssistantTuning tuning){
			return new Rules(tuning.get(AssistantTuning.Parameter.MIN_MOTHER_AGE), tuning.get(AssistantTuning.Parameter.MIN_FATHER_AGE),
				tuning.get(AssistantTuning.Parameter.MAX_MOTHER_AGE), tuning.get(AssistantTuning.Parameter.MAX_FATHER_AGE),
				tuning.get(AssistantTuning.Parameter.MAX_GESTATION_DAYS), tuning.get(AssistantTuning.Parameter.MAX_LIFESPAN_YEARS),
				tuning.get(AssistantTuning.Parameter.MIN_BIRTH_INTERVAL_DAYS), tuning.get(AssistantTuning.Parameter.MAX_TWIN_GAP_DAYS));
		}

	}


	private static final Set<String> MARRIAGE_TYPES = EventTypeInfoService.marriageFormationTypes();

	/** Years are counted with 365 days for the lower limits and 366 for the upper ones, so that a limit is never exceeded by rounding. */
	private static final int DAYS_IN_YEAR_SHORT = 365;
	private static final int DAYS_IN_YEAR_LONG = 366;


	/** The number of checks of {@link #check(ChronologyData, Rules, double, double, DoubleConsumer)}. */
	private static final int CHECKS = 10;


	private ChronologyChecker(){}


	static List<Diagnostic> check(final ChronologyData data){
		return check(data, Rules.DEFAULT);
	}

	static List<Diagnostic> check(final ChronologyData data, final Rules rules){
		return check(data, rules, 0., 1., fraction -> {});
	}

	/**
	 * The same, telling how far it got: the ten checks are equal shares of the part of the progress from {@code from} to {@code to}.
	 *
	 * @param data      the data to check
	 * @param rules     the limits
	 * @param from      the progress when the checks start
	 * @param to        the progress when the checks end
	 * @param progress  told, from time to time, the share of the work done (0 to 1)
	 * @return	The findings.
	 */
	static List<Diagnostic> check(final ChronologyData data, final Rules rules, final double from, final double to,
			final DoubleConsumer progress){
		final List<Diagnostic> findings = new ArrayList<>();
		final double step = (to - from) / CHECKS;
		progress.accept(from);
		checkLifespans(data, rules, findings);
		progress.accept(from + step);
		checkEvents(data, findings);
		progress.accept(from + 2 * step);
		checkParents(data, rules, findings);
		progress.accept(from + 3 * step);
		checkUnions(data, findings);
		progress.accept(from + 4 * step);
		checkMemberships(data, findings);
		progress.accept(from + 5 * step);
		checkAttributes(data, findings);
		progress.accept(from + 6 * step);
		checkUnreadable(data, findings);
		progress.accept(from + 7 * step);
		checkOverlappingMarriages(data, findings);
		progress.accept(from + 8 * step);
		checkBirthIntervals(data, rules, findings);
		progress.accept(from + 9 * step);
		checkMarriageAges(data, rules, findings);
		progress.accept(to);
		return findings;
	}


	/**
	 * Two children of the same mother born less than the shortest plausible interval apart (a convention, so a warning). Twins are
	 * not reported: a pair that may be born the same day, or within a few days, is left alone.
	 */
	private static void checkBirthIntervals(final ChronologyData data, final Rules rules, final List<Diagnostic> findings){
		final Map<String, List<ChronologyData.Person>> childrenOfMother = new LinkedHashMap<>();
		for(final ChronologyData.ParentLink link : data.parents()){
			final ChronologyData.Person child = data.persons().get(link.childId());
			final ChronologyData.Person parent = data.persons().get(link.parentId());
			if(link.biological() && child != null && parent != null && parent.sex() == SexType.FEMALE && child.birth() != null){
				final List<ChronologyData.Person> children = childrenOfMother.computeIfAbsent(parent.id(), k -> new ArrayList<>());
				if(!children.contains(child))
					children.add(child);
			}
		}

		for(final Map.Entry<String, List<ChronologyData.Person>> entry : childrenOfMother.entrySet()){
			final ChronologyData.Person mother = data.persons().get(entry.getKey());
			final List<ChronologyData.Person> children = entry.getValue();
			for(int i = 0; i < children.size(); i ++)
				for(int j = i + 1; j < children.size(); j ++){
					ChronologyData.Person earlier = children.get(i);
					ChronologyData.Person later = children.get(j);
					if(!earlier.birth().isBefore(later.birth())){
						earlier = children.get(j);
						later = children.get(i);
						if(!earlier.birth().isBefore(later.birth()))
							continue;
					}

					final long certainGap = later.birth().min() - earlier.birth().max();
					final long longestGap = spanDays(later.birth().max(), earlier.birth().min());
					if(certainGap > rules.maxTwinGapDays() && longestGap < rules.minBirthIntervalDays())
						findings.add(finding(Diagnostic.Severity.WARNING, "births_too_close",
							List.of(mother.id(), earlier.id(), later.id()), mother.name(), earlier.name(), later.name(),
							rules.minBirthIntervalDays(), earlier.birthText(), later.birthText()));
				}
		}
	}

	/** A marriage (event or union) when the spouse was younger than the youngest plausible age (a convention, so a warning). */
	private static void checkMarriageAges(final ChronologyData data, final Rules rules, final List<Diagnostic> findings){
		final Set<String> reported = new HashSet<>();
		for(final ChronologyData.LifeEvent event : data.events()){
			final ChronologyData.Person person = data.persons().get(event.personId());
			if(person != null && event.principal() && EventReader.ENUM_TYPE_MARRIAGE.equals(event.type()) && event.date() != null
					&& isTooYoung(person, event.date(), rules)){
				reported.add(person.id());
				findings.add(finding(Diagnostic.Severity.WARNING, "marriage_too_young", List.of(person.id(), event.eventId()),
					person.name(), minMarriageAge(person, rules), person.birthText(), I18N.tf("chronology.location.event", event.dateText(), event.eventId())));
			}
		}
		for(final ChronologyData.Union union : data.unions()){
			if(!union.marriage() || union.from() == null)
				continue;

			for(final String partnerId : List.of(union.firstId(), union.secondId())){
				final ChronologyData.Person partner = data.persons().get(partnerId);
				if(partner != null && isTooYoung(partner, union.from(), rules) && reported.add(partner.id()))
					findings.add(finding(Diagnostic.Severity.WARNING, "marriage_too_young",
						List.of(partner.id(), union.relationshipId()), partner.name(), minMarriageAge(partner, rules),
						partner.birthText(), I18N.tf("chronology.location.relationship", union.fromText(), union.relationshipId())));
			}
		}
	}

	private static boolean isTooYoung(final ChronologyData.Person person, final DateRange marriage, final Rules rules){
		//a marriage before the birth is an error reported elsewhere
		if(person.birth() == null || marriage.isBefore(person.birth()))
			return false;

		return spanDays(marriage.max(), person.birth().min()) < (long)minMarriageAge(person, rules) * DAYS_IN_YEAR_SHORT;
	}

	private static int minMarriageAge(final ChronologyData.Person person, final Rules rules){
		return (person.sex() == SexType.FEMALE? rules.minMotherAgeYears(): (person.sex() == SexType.MALE
			? rules.minFatherAgeYears(): Math.min(rules.minMotherAgeYears(), rules.minFatherAgeYears())));
	}


	private static void checkLifespans(final ChronologyData data, final Rules rules, final List<Diagnostic> findings){
		for(final ChronologyData.Person person : data.persons().values()){
			if(person.birth() == null || person.death() == null)
				continue;

			if(DateRules.deathBeforeBirth(person.death(), person.birth()))
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
			if(DateRules.eventBeforeBirth(event.date(), person.birth(), type))
				findings.add(finding(Diagnostic.Severity.ERROR, (type != null && MARRIAGE_TYPES.contains(type)? "marriage_before_birth": "event_before_birth"),
					List.of(person.id(), event.eventId()), person.name(), type + " [" + event.eventId() + "]", event.dateText(), person.birthText()));
			if(DateRules.eventAfterDeath(event.date(), person.death(), type))
				findings.add(finding(Diagnostic.Severity.ERROR, "event_after_death", List.of(person.id(), event.eventId()),
					person.name(), type + " [" + event.eventId() + "]", event.dateText(), person.deathText()));
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
				if(DateRules.childBeforeParent(child.birth(), parent.birth())){
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
					if(DateRules.bornAfterMotherDeath(child.birth(), parent.death()))
						findings.add(finding(Diagnostic.Severity.ERROR, "born_after_mother_death", ids, child.name(), parent.name(),
							child.birthText(), parent.deathText()));
				}
				else if(DateRules.bornAfterFatherDeath(child.birth(), parent.death(), rules.maxGestationDays()))
					findings.add(finding(Diagnostic.Severity.ERROR, "born_after_father_death", ids, child.name(), parent.name(),
						rules.maxGestationDays(), child.birthText(), parent.deathText()));
			}
		}
	}

	/**
	 * A membership of a group that begins before the member was born or after they died, or ends before they were born (the end is
	 * reported only when the beginning does not already say it).
	 */
	private static void checkMemberships(final ChronologyData data, final List<Diagnostic> findings){
		for(final ChronologyData.Membership membership : data.memberships()){
			final ChronologyData.Person member = data.persons().get(membership.memberId());
			if(member == null)
				continue;

			final List<String> ids = List.of(member.id(), membership.relationshipId());
			for(final DateRules.PeriodProblem problem : DateRules.period(membership.from(), membership.to(), member.birth(), member.death()))
				switch(problem){
					case BEGINS_BEFORE_BIRTH -> findings.add(finding(Diagnostic.Severity.ERROR, "membership_before_birth", ids, member.name(),
						membership.fromText(), member.birthText(), membership.relationshipId()));
					case BEGINS_AFTER_DEATH -> findings.add(finding(Diagnostic.Severity.ERROR, "membership_after_death", ids, member.name(),
						membership.fromText(), member.deathText(), membership.relationshipId()));
					case ENDS_BEFORE_BIRTH -> findings.add(finding(Diagnostic.Severity.ERROR, "membership_ended_before_birth", ids,
						member.name(), membership.toText(), member.birthText(), membership.relationshipId()));
				}
		}
	}

	/** An attribute of an individual (a residence, an occupation) that begins before they were born or after they died, or ends before. */
	private static void checkAttributes(final ChronologyData data, final List<Diagnostic> findings){
		for(final ChronologyData.Attribute attribute : data.attributes()){
			final ChronologyData.Person owner = data.persons().get(attribute.individualId());
			if(owner == null)
				continue;

			final List<String> ids = List.of(owner.id(), attribute.attributeId());
			for(final DateRules.PeriodProblem problem : DateRules.period(attribute.from(), attribute.to(), owner.birth(), owner.death()))
				switch(problem){
					case BEGINS_BEFORE_BIRTH -> findings.add(finding(Diagnostic.Severity.ERROR, "attribute_before_birth", ids, owner.name(),
						attribute.fromText(), owner.birthText(), attribute.attributeId(), attribute.description()));
					case BEGINS_AFTER_DEATH -> findings.add(finding(Diagnostic.Severity.ERROR, "attribute_after_death", ids, owner.name(),
						attribute.fromText(), owner.deathText(), attribute.attributeId(), attribute.description()));
					case ENDS_BEFORE_BIRTH -> findings.add(finding(Diagnostic.Severity.ERROR, "attribute_ended_before_birth", ids,
						owner.name(), attribute.toText(), owner.birthText(), attribute.attributeId(), attribute.description()));
				}
		}
	}

	/**
	 * The dates the other checks could not judge, so that they are not silent about them: a date not valid in its calendar is a
	 * problem of the file (a warning, as in Validate File); one whose calendar needs the Ephemeris plugin, which is not installed, is
	 * only not checked (information: the file is not wrong).
	 */
	private static void checkUnreadable(final ChronologyData data, final List<Diagnostic> findings){
		for(final ChronologyData.UnreadableDate date : data.unreadable())
			findings.add(finding((date.needsPlugin()? Diagnostic.Severity.INFORMATION: Diagnostic.Severity.WARNING),
				(date.needsPlugin()? "date_not_checked": "date_invalid"), List.of(date.recordId()), date.description(), date.field(),
				date.text()));
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
						partner.birthText(), union.relationshipId()));
				if(partner.death() != null && partner.death().isBefore(union.from()))
					findings.add(finding(Diagnostic.Severity.ERROR, "union_after_death", ids, partner.name(), union.fromText(),
						partner.deathText(), union.relationshipId()));
			}
		}
	}

	/** Two marriages of the same person that certainly overlap: it is not an error (polygamy exists), but it is worth a look. */
	private static void checkOverlappingMarriages(final ChronologyData data, final List<Diagnostic> findings){
		final Set<String> reported = new HashSet<>();
		//the marriages of each person, found once (in the order of the unions)
		final Map<String, List<ChronologyData.Union>> marriagesOf = new HashMap<>();
		for(final ChronologyData.Union union : data.unions())
			if(union.marriage()){
				if(union.firstId() != null)
					marriagesOf.computeIfAbsent(union.firstId(), k -> new ArrayList<>())
						.add(union);
				if(union.secondId() != null && !union.secondId().equals(union.firstId()))
					marriagesOf.computeIfAbsent(union.secondId(), k -> new ArrayList<>())
						.add(union);
			}
		for(final ChronologyData.Person person : data.persons().values()){
			final List<ChronologyData.Union> marriages = marriagesOf.getOrDefault(person.id(), List.of());
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
							name(data, firstPartner), name(data, secondPartner), first.relationshipId(), second.relationshipId()));
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
