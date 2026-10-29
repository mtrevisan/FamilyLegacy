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
package io.github.mtrevisan.familylegacy.ui.assistant.conclusion;

import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.ui.assistant.chronology.DateRange;

import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * What the conclusion checks need to know about the research, apart from the model: the competing assertions and the conclusions
 * that evaluate them. Keeping it apart makes the checks plain logic, testable without a model.
 *
 * @param persons     the people, by identifier
 * @param events      the births and deaths, by the person they are about
 * @param parents     the parent-child links
 * @param attributes  the attributes that have a single value
 * @param hypotheses  the identity hypotheses between individuals
 * @param conclusions the evaluations of the researcher
 */
record ConclusionData(Map<String, Person> persons, List<PrincipalEvent> events, List<ParentLink> parents,
		List<Attribute> attributes, List<Hypothesis> hypotheses, List<Conclusion> conclusions){

	/**
	 * @param id   the identifier of the individual
	 * @param name the name to show
	 * @param sex  the sex
	 */
	record Person(String id, String name, SexType sex){}

	/**
	 * A birth or a death of an individual, as asserted by an event.
	 *
	 * @param eventId       the identifier of the event
	 * @param personId      the individual the event is about
	 * @param type          {@code birth} or {@code death}
	 * @param date          the days of the event ({@code null} if not dated)
	 * @param dateText      the date, as written
	 * @param recordIds     the event and the participations of the individual in it: a conclusion can point to any of them
	 * @param sourceCount   the sources that cite the event
	 * @param confidence    the confidence of the assertion ({@code low}, {@code medium}, {@code high}), or {@code null}
	 */
	record PrincipalEvent(String eventId, String personId, String type, DateRange date, String dateText, Set<String> recordIds,
			int sourceCount, String confidence){}

	/**
	 * @param relationshipId the identifier of the relationship
	 * @param childId        the child
	 * @param parentId       the parent
	 * @param type           the type of the relationship ({@code biological_child}, {@code adoptive_child}, ...)
	 */
	record ParentLink(String relationshipId, String childId, String parentId, String type){}

	/**
	 * @param id       the identifier of the attribute record
	 * @param personId the individual the attribute is about
	 * @param type     the type of the attribute
	 * @param value    the value
	 */
	record Attribute(String id, String personId, String type, String value){}

	/**
	 * @param id       the identifier of the hypothesis
	 * @param firstId  a candidate
	 * @param secondId the other candidate
	 */
	record Hypothesis(String id, String firstId, String secondId){}

	/**
	 * @param id          the identifier of the conclusion
	 * @param resolves    the identifiers of the assertions it evaluates
	 * @param preferred   the identifier of the assertion judged most likely to be correct, or {@code null}
	 * @param proofStatus the proof status ({@code conflicting_evidence}, {@code supported}, {@code proven}, {@code disproven})
	 */
	record Conclusion(String id, Set<String> resolves, String preferred, String proofStatus){}

}
