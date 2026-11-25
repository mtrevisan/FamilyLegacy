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
package io.github.mtrevisan.familylegacy.assistant.gap;

import io.github.mtrevisan.familylegacy.io.model.readers.date.DateRange;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;


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
 * @param hypotheses  the identity hypotheses between individuals
 */
public record GapData(LocalDate today, Map<String, Person> persons, List<Event> events, List<ParentLink> parents, List<Union> unions,
		List<Question> questions, List<Activity> activities, List<Task> tasks, List<Hypothesis> hypotheses){

	public static final String RELATION_PARENT = "parent";
	public static final String RELATION_PARTNER = "partner";
	public static final String RELATION_CHILD = "child";


	public GapData{
		hypotheses = List.copyOf(hypotheses);
	}

	public GapData(final LocalDate today, final Map<String, Person> persons, final List<Event> events, final List<ParentLink> parents,
			final List<Union> unions, final List<Question> questions, final List<Activity> activities, final List<Task> tasks){
		this(today, persons, events, parents, unions, questions, activities, tasks, List.of());
	}


	/**
	 * @param id           the identifier of the individual
	 * @param name         the name to show
	 * @param birthYear    the year of the birth ({@code null} if not known)
	 * @param hasBirth     whether the individual has a birth event (dated or not)
	 * @param hasDeath     whether the individual has a death event (dated or not)
	 * @param hasResidence whether the individual has a residence among the attributes
	 * @param hasCensus    whether the individual appears in a census
	 * @param eventCount   how many events the individual is the principal of
	 * @param places       the names of the places of the events of the individual other than the birth (without repetitions, in
	 *                     alphabetical order): where the individual is known to have been
	 * @param deathYear    the year of the death ({@code null} if not known): a death bounds the birth of the relatives
		 * @param repositories the names of the repositories placed at the places of the individual (birth included) or in a place that
		 *                     contains them, in alphabetical order: where the records of those places may be kept
		 * @param sourceKinds  the kinds of the sources that cite the individual, its events or its relationships: where it was looked for already
		 * @param leads        the repositories at the places of the relatives of the individual that are not among its own
	 */
	public record Person(String id, String name, Integer birthYear, boolean hasBirth, boolean hasDeath, boolean hasResidence,
			boolean hasCensus, int eventCount, List<String> places, Integer deathYear, List<String> repositories, Set<String> sourceKinds,
			List<RepositoryLead> leads){

		public Person{
			places = List.copyOf(places);
			repositories = List.copyOf(repositories);
			sourceKinds = Set.copyOf(sourceKinds);
			leads = List.copyOf(leads);
		}

		public Person(final String id, final String name, final Integer birthYear, final boolean hasBirth, final boolean hasDeath,
				final boolean hasResidence, final boolean hasCensus, final int eventCount, final List<String> places, final Integer deathYear,
				final List<String> repositories){
			this(id, name, birthYear, hasBirth, hasDeath, hasResidence, hasCensus, eventCount, places, deathYear, repositories, Set.of(), List.of());
		}

		public Person(final String id, final String name, final Integer birthYear, final boolean hasBirth, final boolean hasDeath,
				final boolean hasResidence, final boolean hasCensus, final int eventCount, final List<String> places, final Integer deathYear){
			this(id, name, birthYear, hasBirth, hasDeath, hasResidence, hasCensus, eventCount, places, deathYear, List.of());
		}

		public Person(final String id, final String name, final Integer birthYear, final boolean hasBirth, final boolean hasDeath,
				final boolean hasResidence, final boolean hasCensus, final int eventCount, final List<String> places){
			this(id, name, birthYear, hasBirth, hasDeath, hasResidence, hasCensus, eventCount, places, null);
		}

		public Person(final String id, final String name, final Integer birthYear, final boolean hasBirth, final boolean hasDeath,
				final boolean hasResidence, final boolean hasCensus, final int eventCount){
			this(id, name, birthYear, hasBirth, hasDeath, hasResidence, hasCensus, eventCount, List.of());
		}

		/** @return a copy that also knows the kinds of source already cited and the repositories of the relatives */
		public Person withLeads(final Set<String> sourceKinds, final List<RepositoryLead> leads){
			return new Person(id, name, birthYear, hasBirth, hasDeath, hasResidence, hasCensus, eventCount, places, deathYear, repositories,
				sourceKinds, leads);
		}

	}

	/**
	 * A repository the individual has not been looked for at, but a relative has: families move, and the records of the place of a
	 * parent, a partner or a child may name the individual too.
	 *
	 * @param repository the name of the repository
	 * @param relative   the name of the relative, with the identifier
	 * @param relation   {@code parent}, {@code partner} or {@code child}
	 */
	public record RepositoryLead(String repository, String relative, String relation){}

	/**
	 * An identity hypothesis between two individuals.
	 *
	 * @param id         the identifier of the hypothesis
	 * @param firstId    a candidate
	 * @param secondId   the other candidate
	 * @param confidence the confidence of the hypothesis ({@code low}, {@code medium}, {@code high}), or {@code null}
	 * @param evaluated  whether a conclusion already evaluates it
	 */
	public record Hypothesis(String id, String firstId, String secondId, String confidence, boolean evaluated){}

	/**
	 * @param id          the identifier of the event
	 * @param type        the type of the event
	 * @param sourceCount how many sources cite the event, the participation of its principals or the principals themselves
	 * @param principals  the individuals the event is about
	 */
	public record Event(String id, String type, int sourceCount, List<String> principals){}

	/**
	 * @param childId     the child
	 * @param parentId    the parent
	 * @param sourceCount how many sources cite the relationship (a record of the parentage, usually of the birth or the baptism)
	 */
	public record ParentLink(String childId, String parentId, int sourceCount){

		public ParentLink(final String childId, final String parentId){
			this(childId, parentId, 0);
		}
	}

	/**
	 * @param firstId     a partner
	 * @param secondId    the other partner
	 * @param sourceCount how many sources cite the relationship (a record of the marriage, usually)
	 */
	public record Union(String firstId, String secondId, int sourceCount){

		public Union(final String firstId, final String secondId){
			this(firstId, secondId, 0);
		}
	}

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
