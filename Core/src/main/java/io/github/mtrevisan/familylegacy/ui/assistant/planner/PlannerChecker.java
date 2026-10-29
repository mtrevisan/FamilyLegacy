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
package io.github.mtrevisan.familylegacy.ui.assistant.planner;

import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticKeys;
import io.github.mtrevisan.familylegacy.ui.assistant.gap.GapData;
import io.github.mtrevisan.familylegacy.ui.assistant.gap.GapDraft;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * The rules of the Research Planner. They are pure: they read the research ({@link GapData}) and say what to do next, and why.
 * <p>
 * Two kinds of suggestion, both ordered by an estimated usefulness (a rule of thumb, not a judgement of the research):
 * </p>
 * <ul>
 *   <li>the next steps of an open question: the records that would answer it and are not yet in the tree, not yet cited by a source, and
 *   that no task or activity of the question already covers (a draft of a research task, with the reason);</li>
 *   <li>the brick walls worth working on: individuals whose parents are unknown, ranked by how much of the tree depends on them (a
 *   draft of a research question).</li>
 * </ul>
 * Nothing is created: the drafts are the start of the usual editors, saved only if the researcher confirms.
 */
final class PlannerChecker{

	/** The most steps proposed for one question. */
	static final int MAX_STEPS_PER_QUESTION = 3;
	/** The most steps proposed in all: the other ones are summed up in one finding. */
	static final int MAX_STEPS = 25;
	/** The most brick walls proposed: the other ones are summed up in one finding. */
	static final int MAX_BRICK_WALLS = 10;

	static final int HIGH_USEFULNESS = 70;
	static final int MEDIUM_USEFULNESS = 45;

	/** An individual this old, or older, is taken to be dead, so their death can be searched. */
	private static final int DEAD_AGE = 80;
	private static final int FIRST_CENSUS_YEAR = 1700;
	private static final int LAST_CENSUS_YEAR = 1950;
	/** How many individuals of a question are planned for: a question about a whole family would drown the plan. */
	private static final int MAX_PERSONS_PER_QUESTION = 3;
	/** How many searches that found nothing make the records worth asking the archive for. */
	private static final int FRUITLESS_SEARCHES = 2;


	/** The steps that can be proposed, with the words that say a task or an activity already covers them. */
	private enum Step{
		BIRTH_MISSING("birth_missing", "birth", "baptis", "nasc", "battes", "nato"),
		BIRTH_UNSOURCED("birth_unsourced", "birth", "baptis", "nasc", "battes", "nato"),
		PARENTS("parents", "marriage", "matrimon", "sposal", "nozze"),
		BAPTISMS("baptisms", "baptis", "battes"),
		MARRIAGE("marriage", "marriage", "matrimon", "sposal", "nozze"),
		DEATH("death", "death", "burial", "mort", "decess", "sepolt", "funer"),
		CENSUS("census", "census", "censim"),
		ARCHIVE("archive", "contact", "archiv", "letter", "e-mail", "email");

		private final String key;
		private final String[] keywords;


		Step(final String key, final String... keywords){
			this.key = key;
			this.keywords = keywords;
		}


		boolean isCoveredBy(final String text){
			for(final String keyword : keywords)
				if(text.contains(keyword))
					return true;
			return false;
		}

	}

	private record Candidate(Step step, GapData.Person person, int usefulness, String year, String count){}


	private PlannerChecker(){}


	/**
	 * @param data the research
	 * @return the suggestions, in no particular order (each has its usefulness as rank)
	 */
	static List<PlannerFinding> findings(final GapData data){
		final List<PlannerFinding> findings = new ArrayList<>();
		findings.addAll(steps(data));
		findings.addAll(brickWalls(data));
		return findings;
	}

	/** @return how useful a suggestion with that estimated usefulness is: high, medium or low */
	static String level(final int usefulness){
		if(usefulness >= HIGH_USEFULNESS)
			return "high";
		return (usefulness >= MEDIUM_USEFULNESS? "medium": "low");
	}

	/** @return the priority to propose for a task with that estimated usefulness */
	static String priority(final int usefulness){
		if(usefulness >= HIGH_USEFULNESS)
			return "high";
		return (usefulness >= MEDIUM_USEFULNESS? "normal": "low");
	}


	private static List<PlannerFinding> steps(final GapData data){
		final List<PlannerFinding> findings = new ArrayList<>();
		for(final GapData.Question question : data.questions()){
			if(!isOpen(question) || question.hasConclusion())
				continue;

			final List<GapData.Person> persons = personsOf(question, data);
			if(persons.isEmpty())
				continue;

			final String covered = coveredText(question, data);
			final boolean nothingStarted = nothingStarted(question, data);
			final List<Candidate> candidates = new ArrayList<>();
			for(final GapData.Person person : persons)
				candidates.addAll(candidates(person, data, nothingStarted));
			addArchive(question, persons.getFirst(), data, candidates);

			candidates.stream()
				.filter(candidate -> !candidate.step().isCoveredBy(covered))
				.sorted(Comparator.comparingInt(Candidate::usefulness).reversed())
				.limit(MAX_STEPS_PER_QUESTION)
				.forEach(candidate -> findings.add(step(question, candidate)));
		}
		return capped(findings);
	}

	private static boolean isOpen(final GapData.Question question){
		return (question.status() == null || question.status().isBlank() || "open".equals(question.status()));
	}

	/** @return the individuals a question is about: the ones it targets, and the principals of the events it targets */
	private static List<GapData.Person> personsOf(final GapData.Question question, final GapData data){
		final Set<String> ids = new LinkedHashSet<>();
		for(final String target : question.targets()){
			if(data.persons().containsKey(target))
				ids.add(target);
			else
				data.events().stream().filter(event -> event.id().equals(target)).forEach(event -> ids.addAll(event.principals()));
		}
		return ids.stream()
			.map(data.persons()::get)
			.filter(person -> person != null)
			.limit(MAX_PERSONS_PER_QUESTION)
			.toList();
	}

	/** @return what the tasks and the activities of a question say they do, in lower case (the abandoned ones do nothing) */
	private static String coveredText(final GapData.Question question, final GapData data){
		final StringBuilder sb = new StringBuilder();
		for(final GapData.Task task : data.tasks())
			if(task.questionIds().contains(question.id()) && !"abandoned".equals(task.status()) && task.description() != null)
				sb.append(task.description().toLowerCase(Locale.ROOT)).append('\n');
		for(final GapData.Activity activity : data.activities())
			if(activity.questionIds().contains(question.id()) && !"abandoned".equals(activity.status()) && activity.action() != null)
				sb.append(activity.action().toLowerCase(Locale.ROOT)).append('\n');
		return sb.toString();
	}

	private static boolean nothingStarted(final GapData.Question question, final GapData data){
		return (data.tasks().stream().noneMatch(task -> task.questionIds().contains(question.id()))
			&& data.activities().stream().noneMatch(activity -> activity.questionIds().contains(question.id())));
	}

	private static List<Candidate> candidates(final GapData.Person person, final GapData data, final boolean nothingStarted){
		final List<Candidate> candidates = new ArrayList<>();
		final int bonus = (nothingStarted? 5: 0);
		final Integer birthYear = person.birthYear();
		final String year = (birthYear != null? String.valueOf(birthYear): StringUtils.EMPTY);

		final List<GapData.Event> births = eventsOf(person, data, EventReader.ENUM_TYPE_BIRTH);
		if(births.isEmpty() && !person.hasBirth())
			candidates.add(new Candidate(Step.BIRTH_MISSING, person, 80 + bonus, year, ""));
		else if(births.stream().allMatch(event -> event.sourceCount() == 0))
			candidates.add(new Candidate(Step.BIRTH_UNSOURCED, person, 65 + bonus, year, ""));

		final boolean parentsKnown = data.parents().stream().anyMatch(link -> link.childId().equals(person.id()));
		final boolean married = data.unions().stream().anyMatch(union -> union.firstId().equals(person.id())
			|| union.secondId().equals(person.id()));
		final List<GapData.Event> marriages = eventsOf(person, data, EventReader.ENUM_TYPE_MARRIAGE);
		if(!parentsKnown){
			candidates.add(new Candidate(Step.PARENTS, person, 75 + bonus, year, ""));
			if(birthYear != null)
				candidates.add(new Candidate(Step.BAPTISMS, person, 55 + bonus, year, ""));
		}
		else if(married && (marriages.isEmpty() || marriages.stream().allMatch(event -> event.sourceCount() == 0)))
			candidates.add(new Candidate(Step.MARRIAGE, person, 60 + bonus, year, ""));

		final List<GapData.Event> deaths = eventsOf(person, data, EventReader.ENUM_TYPE_DEATH);
		final boolean deathUnknown = (deaths.isEmpty() && !person.hasDeath());
		final boolean deathUnsourced = (!deaths.isEmpty() && deaths.stream().allMatch(event -> event.sourceCount() == 0));
		if((deathUnknown || deathUnsourced) && birthYear != null && data.today().getYear() - birthYear >= DEAD_AGE)
			candidates.add(new Candidate(Step.DEATH, person, 55 + bonus, year, ""));

		if(!person.hasCensus() && birthYear != null && birthYear >= FIRST_CENSUS_YEAR && birthYear <= LAST_CENSUS_YEAR)
			candidates.add(new Candidate(Step.CENSUS, person, 40 + bonus, year, ""));
		return candidates;
	}

	private static List<GapData.Event> eventsOf(final GapData.Person person, final GapData data, final String type){
		return data.events().stream()
			.filter(event -> type.equals(event.type()) && event.principals().contains(person.id()))
			.toList();
	}

	/** Searches that found nothing are the sign that the records are not where they were looked for: it is time to ask the archive. */
	private static void addArchive(final GapData.Question question, final GapData.Person person, final GapData data,
			final List<Candidate> candidates){
		final long fruitless = data.activities().stream()
			.filter(activity -> activity.questionIds().contains(question.id()) && "completed".equals(activity.status())
				&& activity.result() != null && !"positive".equals(activity.result()))
			.count();
		if(fruitless >= FRUITLESS_SEARCHES)
			candidates.add(new Candidate(Step.ARCHIVE, person, 50, "", String.valueOf(fruitless)));
	}

	private static PlannerFinding step(final GapData.Question question, final Candidate candidate){
		final Step step = candidate.step();
		final String name = candidate.person().name();
		final String task = I18N.tf("planner.step." + step.key + ".task", name, candidate.year());
		final String why = I18N.tf("planner.step." + step.key + ".why", name, candidate.year(), candidate.count());
		final String title = (question.title() != null && !question.title().isBlank()? question.title(): question.id());
		final String level = level(candidate.usefulness());

		final Diagnostic diagnostic = new Diagnostic(Diagnostic.Severity.INFORMATION, AssistantCategory.RESEARCH,
			I18N.tf("planner.next_step.title", task),
			I18N.tf("planner.next_step.explanation", title, why) + StringUtils.SPACE + I18N.tf("gap.usefulness", I18N.t("gap.usefulness." + level),
				String.valueOf(candidate.usefulness())),
			List.of(question.id(), candidate.person().id()), null,
			DiagnosticKeys.of("planner.next_step", List.of(question.id(), candidate.person().id()), step.key))
			.withRank(candidate.usefulness());
		return new PlannerFinding(diagnostic, new GapDraft(GapDraft.Kind.TASK, "", task, List.of(question.id()),
			priority(candidate.usefulness())));
	}


	private static List<PlannerFinding> brickWalls(final GapData data){
		final Map<String, List<String>> children = new HashMap<>();
		final Set<String> withParents = new HashSet<>();
		for(final GapData.ParentLink link : data.parents()){
			children.computeIfAbsent(link.parentId(), k -> new ArrayList<>()).add(link.childId());
			withParents.add(link.childId());
		}
		final Set<String> worked = new HashSet<>();
		for(final GapData.Question question : data.questions())
			if(isOpen(question) || "on_hold".equals(question.status()))
				worked.addAll(question.targets());

		final List<PlannerFinding> findings = new ArrayList<>();
		for(final GapData.Person person : data.persons().values()){
			if(withParents.contains(person.id()) || worked.contains(person.id()))
				continue;

			final int descendants = descendants(person.id(), children);
			if(descendants == 0)
				continue;

			final int usefulness = Math.min(85, 40 + Math.min(30, descendants * 3) + (person.birthYear() != null? 10: 0)
				+ (person.eventCount() > 0? 5: 0));
			final String year = (person.birthYear() != null? String.valueOf(person.birthYear()): StringUtils.EMPTY);
			final String level = level(usefulness);
			final Diagnostic diagnostic = new Diagnostic(Diagnostic.Severity.INFORMATION, AssistantCategory.RESEARCH,
				I18N.tf("planner.brick_wall.title", person.name()),
				I18N.tf((person.birthYear() != null? "planner.brick_wall.explanation.year": "planner.brick_wall.explanation"),
					person.name(), String.valueOf(descendants), year) + StringUtils.SPACE + I18N.tf("gap.usefulness", I18N.t("gap.usefulness." + level),
					String.valueOf(usefulness)),
				List.of(person.id()), null, DiagnosticKeys.of("planner.brick_wall", List.of(person.id()), descendants))
				.withRank(usefulness);
			findings.add(new PlannerFinding(diagnostic, new GapDraft(GapDraft.Kind.QUESTION,
				I18N.tf("planner.brick_wall.draft.title", person.name()),
				I18N.tf((person.birthYear() != null? "planner.brick_wall.draft.question.year": "planner.brick_wall.draft.question"),
					person.name(), year), List.of(person.id()), null)));
		}
		return capped(findings, MAX_BRICK_WALLS, "planner.brick_wall");
	}

	/** @return how many individuals descend from an individual, through the links of descent */
	private static int descendants(final String id, final Map<String, List<String>> children){
		final Set<String> seen = new HashSet<>();
		final Deque<String> queue = new ArrayDeque<>(children.getOrDefault(id, List.of()));
		while(!queue.isEmpty()){
			final String next = queue.poll();
			if(!next.equals(id) && seen.add(next))
				queue.addAll(children.getOrDefault(next, List.of()));
		}
		return seen.size();
	}


	private static List<PlannerFinding> capped(final List<PlannerFinding> findings){
		return capped(findings, MAX_STEPS, "planner.next_step");
	}

	/** Keeps the findings with the highest rank, up to {@code max}, and sums up the other ones in a single finding. */
	private static List<PlannerFinding> capped(final List<PlannerFinding> findings, final int max, final String kind){
		if(findings.size() <= max)
			return findings;

		final List<PlannerFinding> sorted = new ArrayList<>(findings);
		sorted.sort(Comparator.comparingDouble((PlannerFinding f) -> f.diagnostic().rank()).reversed());
		final List<PlannerFinding> result = new ArrayList<>(sorted.subList(0, max));
		final int more = sorted.size() - max;
		result.add(new PlannerFinding(new Diagnostic(Diagnostic.Severity.INFORMATION, AssistantCategory.RESEARCH,
			I18N.tf(kind + ".more.title", String.valueOf(more)), I18N.tf(kind + ".more.explanation", String.valueOf(more),
			String.valueOf(max)), List.of(), null, DiagnosticKeys.of(kind + ".more", List.of(), more))
			.withRank(sorted.get(max).diagnostic().rank()), null));
		return result;
	}

}
