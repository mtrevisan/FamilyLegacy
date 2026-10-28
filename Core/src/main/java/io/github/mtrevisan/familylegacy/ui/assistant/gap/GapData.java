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

import io.github.mtrevisan.familylegacy.ui.assistant.chronology.DateRange;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;


/**
 * What the research gap checks need to know about the research, apart from the model: the individuals and what is known of them, the
 * events, and the questions, activities and tasks of the research itself. Keeping it apart makes the checks plain logic, testable
 * without a model.
 *
 * @param today       the day the checks are made (an individual born a century ago is probably dead, a task is overdue)
 * @param persons     the individuals, by identifier
 * @param events      the events
 * @param parents     the parent-child links
 * @param unions      the couples
 * @param questions   the research questions
 * @param activities  the research activities
 * @param tasks       the research tasks
 */
public record GapData(LocalDate today, Map<String, Person> persons, List<Event> events, List<ParentLink> parents, List<Union> unions,
		List<Question> questions, List<Activity> activities, List<Task> tasks){

	/**
	 * @param id           the identifier of the individual
	 * @param name         the name to show
	 * @param birthYear    the year of the birth ({@code null} if not known)
	 * @param hasBirth     whether the individual has a birth event (dated or not)
	 * @param hasDeath     whether the individual has a death event (dated or not)
	 * @param hasResidence whether the individual has a residence among the attributes
	 * @param hasCensus    whether the individual appears in a census
	 * @param eventCount   how many events the individual is the principal of
	 */
	public record Person(String id, String name, Integer birthYear, boolean hasBirth, boolean hasDeath, boolean hasResidence,
			boolean hasCensus, int eventCount){}

	/**
	 * @param id          the identifier of the event
	 * @param type        the type of the event
	 * @param sourceCount how many sources cite the event
	 * @param principals  the individuals the event is about
	 */
	public record Event(String id, String type, int sourceCount, List<String> principals){}

	/**
	 * @param childId  the child
	 * @param parentId the parent
	 */
	public record ParentLink(String childId, String parentId){}

	/**
	 * @param firstId  a partner
	 * @param secondId the other partner
	 */
	public record Union(String firstId, String secondId){}

	/**
	 * @param id           the identifier of the question
	 * @param title        the title
	 * @param status       the status ({@code open}, {@code on_hold}, {@code resolved}, {@code disproven}, {@code closed})
	 * @param hasConclusion whether the question has a working conclusion
	 */
	public record Question(String id, String title, String status, boolean hasConclusion, List<String> targets){

		public Question(final String id, final String title, final String status, final boolean hasConclusion){
			this(id, title, status, hasConclusion, List.of());
		}

	}

	/**
	 * @param id          the identifier of the activity
	 * @param action      what was done
	 * @param status      {@code planned}, {@code in_progress}, {@code completed} or {@code abandoned}
	 * @param result      the result ({@code positive}, {@code negative}, ...), or {@code null}
	 * @param questionIds the questions the activity contributes to
	 * @param hasFollowUp whether the activity has follow-up tasks
	 * @param isParent    whether another activity originated from this one
	 */
	public record Activity(String id, String action, String status, String result, List<String> questionIds, boolean hasFollowUp,
			boolean isParent){}

	/**
	 * @param id          the identifier of the task
	 * @param description the work to do
	 * @param status      {@code open}, {@code in_progress}, {@code completed} or {@code abandoned}
	 * @param priority    {@code low}, {@code normal}, {@code high}, or {@code null}
	 * @param due         the day it is due ({@code null} if none)
	 * @param questionIds the questions the task supports
	 */
	public record Task(String id, String description, String status, String priority, DateRange due, List<String> questionIds){}

}
