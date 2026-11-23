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

import io.github.mtrevisan.familylegacy.events.EventTypeInfo;
import io.github.mtrevisan.familylegacy.events.EventTypeInfoService;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * The rules on the dates that are provable for every day the dates may stand for, written once: Validate File ({@link DateConsistency},
 * where they are errors of the file) and the Chronology Assistant (where they are findings with an explanation) both ask them here, so
 * that the two cannot drift apart. Each rule takes the days that the dates may stand for ({@link DateRange}, {@code null} if not known)
 * and says whether it is broken: it is broken only if <em>every</em> day of one range is before <em>every</em> day of the other, so an
 * uncertain date never raises a false alarm.
 * <p>
 * The texts (the messages, the explanations, the records they name) are not here: they are the business of the caller. Only the
 * conventions that are not provable (the ages) are left to the assistant.
 * </p>
 */
public final class DateRules{

	/** The longest time between the death of the father and the birth of the child, in days (the assistant lets the user change it). */
	public static final int DEFAULT_GESTATION_DAYS = 300;

	/** The custom types (not in the protocol list, so unknown to the catalog) that can be dated after the death. */
	private static final Set<String> CUSTOM_POSTHUMOUS_TYPES = Set.of(EventReader.CUSTOM_TYPE_ANECDOTE, EventReader.CUSTOM_TYPE_COMMEMORATION);


	/** What is wrong with a period (a membership, a union, an attribute) against the life of the individual it is about. */
	public enum PeriodProblem{
		/** The period begins before the individual was born. */
		BEGINS_BEFORE_BIRTH,
		/** The period begins after the individual died. */
		BEGINS_AFTER_DEATH,
		/** The period ends before the individual was born (reported only if the beginning does not already say it). */
		ENDS_BEFORE_BIRTH
	}


	private DateRules(){}


	/** @return	Whether an event of that type can be dated after the death of the person (a burial, a probate, a commemoration). */
	public static boolean isPosthumous(final String type){
		return (type != null && EventTypeInfoService.find(type).map(EventTypeInfo::allowedAfterDeath).orElseGet(() -> CUSTOM_POSTHUMOUS_TYPES.contains(type)));
	}

	/** @return	Whether the death is certainly before the birth. */
	public static boolean deathBeforeBirth(final DateRange death, final DateRange birth){
		return (death != null && birth != null && death.isBefore(birth));
	}

	/** @return	Whether an event (a birth is exempt: it is the date the others are compared to) is certainly before the birth. */
	public static boolean eventBeforeBirth(final DateRange event, final DateRange birth, final String type){
		return (event != null && birth != null && !EventReader.ENUM_TYPE_BIRTH.equals(type) && event.isBefore(birth));
	}

	/** @return	Whether an event (a posthumous one is exempt) is certainly after the death. */
	public static boolean eventAfterDeath(final DateRange event, final DateRange death, final String type){
		return (event != null && death != null && !isPosthumous(type) && death.isBefore(event));
	}

	/** @return	Whether a child that descends from the parent is certainly born before the parent. */
	public static boolean childBeforeParent(final DateRange childBirth, final DateRange parentBirth){
		return (childBirth != null && parentBirth != null && childBirth.isBefore(parentBirth));
	}

	/** @return	Whether a child is certainly born after the death of the mother. */
	public static boolean bornAfterMotherDeath(final DateRange childBirth, final DateRange motherDeath){
		return (childBirth != null && motherDeath != null && motherDeath.isBefore(childBirth));
	}

	/**
	 * @param gestationDays	The longest time between the death of the father and the birth of the child.
	 * @return	Whether a child is certainly born more than the gestation after the death of the father (or of a parent of unknown sex).
	 */
	public static boolean bornAfterFatherDeath(final DateRange childBirth, final DateRange fatherDeath, final int gestationDays){
		return (childBirth != null && fatherDeath != null && DateRange.plus(fatherDeath.max(), gestationDays) < childBirth.min());
	}

	/**
	 * What is wrong with a period against the life of an individual: a membership of a group, a union, an attribute (a residence, an
	 * occupation). The end is reported only when the beginning does not already say it (the beginning is missing, or it is after the birth).
	 *
	 * @param from	The days the period begins ({@code null} if not known).
	 * @param to	The days the period ends ({@code null} if not known, or if the end is not judged).
	 * @param birth	The days of the birth of the individual ({@code null} if not known).
	 * @param death	The days of the death of the individual ({@code null} if not known).
	 * @return	The problems, in the order beginning before the birth, beginning after the death, end before the birth.
	 */
	public static List<PeriodProblem> period(final DateRange from, final DateRange to, final DateRange birth, final DateRange death){
		final List<PeriodProblem> problems = new ArrayList<>(3);
		if(from != null){
			if(birth != null && from.isBefore(birth))
				problems.add(PeriodProblem.BEGINS_BEFORE_BIRTH);
			if(death != null && death.isBefore(from))
				problems.add(PeriodProblem.BEGINS_AFTER_DEATH);
		}
		if(to != null && birth != null && to.isBefore(birth) && (from == null || !from.isBefore(birth)))
			problems.add(PeriodProblem.ENDS_BEFORE_BIRTH);
		return problems;
	}

}
