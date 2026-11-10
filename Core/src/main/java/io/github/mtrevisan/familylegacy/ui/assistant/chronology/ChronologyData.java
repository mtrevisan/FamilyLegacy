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

import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateRange;

import java.util.List;
import java.util.Map;


/**
 * What the chronology checks need to know about the research, apart from the model: the dates of the people, of their events
 * and of their relationships. Keeping it apart makes the checks plain logic, testable without a model.
 *
 * @param persons the people, by identifier
 * @param events  the events the people took part in
 * @param parents the parent-child links
 * @param unions  the couples
 * @param memberships the memberships of individuals in groups
 * @param unreadable  the dates that could not be read, so that the checks above skipped them
 */
record ChronologyData(Map<String, Person> persons, List<LifeEvent> events, List<ParentLink> parents, List<Union> unions,
		List<Membership> memberships, List<UnreadableDate> unreadable){

	/** The data of a research without memberships of groups. */
	ChronologyData(final Map<String, Person> persons, final List<LifeEvent> events, final List<ParentLink> parents,
			final List<Union> unions){
		this(persons, events, parents, unions, List.of(), List.of());
	}

	/** The data of a research without dates that cannot be read. */
	ChronologyData(final Map<String, Person> persons, final List<LifeEvent> events, final List<ParentLink> parents,
			final List<Union> unions, final List<Membership> memberships){
		this(persons, events, parents, unions, memberships, List.of());
	}

	/**
	 * @param recordId    the identifier of the record that has the date
	 * @param description the record, to show
	 * @param field       the field of the date
	 * @param text        the date, as written
	 * @param needsPlugin whether the date is valid but its calendar needs the Ephemeris plugin, which is not installed (not a problem of
	 *                    the file), or it is not valid in the notation of its calendar
	 */
	record UnreadableDate(String recordId, String description, String field, String text, boolean needsPlugin){}

	/**
	 * @param relationshipId the identifier of the relationship
	 * @param memberId       the individual that is a member
	 * @param from           the days the membership began ({@code null} if not known)
	 * @param to             the days the membership ended ({@code null} if not known)
	 * @param fromText       the date the membership began, as written
	 * @param toText         the date the membership ended, as written
	 */
	record Membership(String relationshipId, String memberId, DateRange from, DateRange to, String fromText, String toText){}

	/**
	 * @param id        the identifier of the individual
	 * @param name      the name to show
	 * @param sex       the sex, for the age limits of the parents
	 * @param birth     the days of the birth ({@code null} if not known); if there are several birth events, the days they cover
	 * @param death     the days of the death ({@code null} if not known)
	 * @param birthText the date of the birth, as written
	 * @param deathText the date of the death, as written
	 */
	record Person(String id, String name, SexType sex, DateRange birth, DateRange death, String birthText, String deathText){}

	/**
	 * @param eventId  the identifier of the event
	 * @param personId the participant
	 * @param type     the type of the event
	 * @param date     the days of the event ({@code null} if not dated)
	 * @param dateText the date, as written
	 * @param principal whether the participant is the one the event is about (not a witness or an officiant)
	 */
	record LifeEvent(String eventId, String personId, String type, DateRange date, String dateText, boolean principal){}

	/**
	 * @param relationshipId the identifier of the relationship
	 * @param childId        the child
	 * @param parentId       the parent
	 * @param biological     whether the child descends from the parent (not an adoptive, foster, guarded or step child)
	 */
	record ParentLink(String relationshipId, String childId, String parentId, boolean biological){}

	/**
	 * @param relationshipId the identifier of the relationship
	 * @param firstId        a partner
	 * @param secondId       the other partner
	 * @param marriage       whether it is a marriage (civil, religious, customary), not a cohabitation or an engagement
	 * @param from           the days the union began ({@code null} if not known)
	 * @param to             the days the union ended ({@code null} if not known)
	 * @param endedUnknown   whether the union is known to be over, without a date
	 * @param fromText       the date the union began, as written
	 */
	record Union(String relationshipId, String firstId, String secondId, boolean marriage, DateRange from, DateRange to,
			boolean endedUnknown, String fromText){}

}
