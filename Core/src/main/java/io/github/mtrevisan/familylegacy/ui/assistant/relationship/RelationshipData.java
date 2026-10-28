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
package io.github.mtrevisan.familylegacy.ui.assistant.relationship;

import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.ui.assistant.chronology.DateRange;

import java.util.List;
import java.util.Map;


/**
 * What the relationship checks need to know about the research, apart from the model: the relationships, the people and groups they
 * link, and the events that give evidence of them. Keeping it apart makes the checks plain logic, testable without a model.
 *
 * @param persons       the individuals, by identifier
 * @param groups        the names of the groups, by identifier
 * @param relationships the relationships between individuals and groups
 * @param events        the events the individuals took part in
 */
record RelationshipData(Map<String, Person> persons, Map<String, String> groups, List<Relationship> relationships,
		List<Event> events){

	/** What a relationship links. */
	enum Kind{
		INDIVIDUAL,
		GROUP
	}

	/**
	 * @param id    the identifier of the individual
	 * @param name  the name to show
	 * @param sex   the sex
	 * @param birth the days of the birth ({@code null} if not known)
	 */
	record Person(String id, String name, SexType sex, DateRange birth){}

	/**
	 * @param id          the identifier of the relationship
	 * @param subjectId   the subject: the entity whose role is described by the type (the child of a parent-child relationship)
	 * @param subjectKind whether the subject is an individual or a group
	 * @param targetId    the target
	 * @param targetKind  whether the target is an individual or a group
	 * @param type        the type ({@code biological_child}, {@code civil_spouse}, ...)
	 * @param role        the role of the subject, or {@code null}
	 * @param from        the days the relationship began ({@code null} if not known)
	 * @param to          the days the relationship ended ({@code null} if not known)
	 * @param fromText    the date the relationship began, as written
	 * @param ended       whether the relationship is known to be over (status ended), with or without a date
	 */
	record Relationship(String id, String subjectId, Kind subjectKind, String targetId, Kind targetKind, String type, String role,
			DateRange from, DateRange to, String fromText, boolean ended){}

	/**
	 * @param id           the identifier of the event
	 * @param type         the type of the event
	 * @param date         the days of the event ({@code null} if not dated)
	 * @param dateText     the date, as written
	 * @param participants the individuals that took part in it
	 */
	record Event(String id, String type, DateRange date, String dateText, List<Participant> participants){}

	/**
	 * @param personId  the individual
	 * @param principal whether the event is about them (not a witness or an officiant)
	 */
	record Participant(String personId, boolean principal){}

}
