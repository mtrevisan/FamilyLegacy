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

import io.github.mtrevisan.familylegacy.ui.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticKeys;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Finds the gaps of the research: what is probably missing from the tree, and where the research itself has stopped.
 * <p>
 * Missing from the tree: the death of an individual who must be dead, the birth of one who has children or events, the marriage of
 * the parents of a child, the sources of the vital events, the residence of the people found in a census. Stopped research: an open
 * question that no activity has worked on, or whose activities are all over with nothing planned, a search with no result that has no
 * follow-up, a task past its due date, a completed activity with no result.
 * </p>
 * <p>
 * Every gap has an estimated usefulness, from 0 to 100, that orders the findings: a death that is certainly missing and a vital event
 * with no source come before the residence of a census. It is an estimate by rules of thumb, not a judgement of the research. The
 * assistant only suggests: for a gap it prepares the draft of a research question (what to find out) or of a research task (what to
 * do), to be completed and confirmed in the usual editor, and it never creates anything on its own.
 * </p>
 * <p>
 * What other assistants report is not repeated: an identity hypothesis with no conclusion is a finding of the Conclusion Assistant.
 * </p>
 */
final class GapChecker{

	/** An individual born so long ago is certainly dead. */
	static final int CERTAINLY_DEAD_YEARS = 110;
	/** An individual born so long ago is probably dead. */
	static final int PROBABLY_DEAD_YEARS = 90;
	/** The usefulness from which a gap is called high, and the one from which it is called medium. */
	static final int HIGH_USEFULNESS = 70;
	static final int MEDIUM_USEFULNESS = 45;
	/** The most records a finding about many of them lists. */
	static final int MAX_LISTED = 200;
	/** The most findings there are about the same kind of gap, one for each individual: from there on they are one finding. */
	static final int MAX_INDIVIDUAL_FINDINGS = 25;

	/** The Julian Day Number of 1970-01-01, to turn a day into a Julian Day Number. */
	private static final long UNIX_EPOCH_JDN = 2_440_588L;

	private static final Set<String> VITAL_EVENTS = Set.of("birth", "death", "marriage");
	private static final Set<String> OPEN_QUESTIONS = Set.of("open", "on_hold");
	private static final Set<String> FINISHED_ACTIVITIES = Set.of("completed", "abandoned");
	private static final Set<String> UNSUCCESSFUL_RESULTS = Set.of("negative", "inconclusive", "unavailable");
	private static final Set<String> ACTIVE_TASKS = Set.of("open", "in_progress");


	private GapChecker(){}


	/** @return the findings, without the drafts */
	static List<Diagnostic> check(final GapData data){
		return findings(data).stream()
			.map(GapFinding::diagnostic)
			.toList();
	}

	/** @return the findings, each with the draft of the question or of the task that would start to fill the gap */
	static List<GapFinding> findings(final GapData data){
		final List<GapFinding> findings = new ArrayList<>();
		final Map<String, Integer> children = new HashMap<>();
		data.parents().forEach(link -> children.merge(link.parentId(), 1, Integer::sum));

		checkDeaths(data, findings);
		checkBirths(data, children, findings);
		checkMarriages(data, findings);
		checkSources(data, findings);
		checkCensusResidences(data, findings);
		checkQuestions(data, findings);
		checkActivities(data, findings);
		checkTasks(data, findings);
		return findings;
	}

	/** @return how useful it would be to fill a gap with that estimated usefulness: high, medium or low */
	static String level(final int usefulness){
		if(usefulness >= HIGH_USEFULNESS)
			return "high";
		return (usefulness >= MEDIUM_USEFULNESS? "medium": "low");
	}


	/** The death of an individual who is certainly (or probably) dead: they were born a century ago, and no death is recorded. */
	private static void checkDeaths(final GapData data, final List<GapFinding> findings){
		final List<GapFinding> group = new ArrayList<>();
		for(final GapData.Person person : data.persons().values()){
			if(person.hasDeath() || person.birthYear() == null)
				continue;

			final int age = data.today().getYear() - person.birthYear();
			final int usefulness = (age >= CERTAINLY_DEAD_YEARS? 85: (age >= PROBABLY_DEAD_YEARS? 55: 0));
			if(usefulness == 0)
				continue;

			group.add(finding(Diagnostic.Severity.INFORMATION, (age >= CERTAINLY_DEAD_YEARS? "no_death_certain": "no_death_probable"),
				List.of(person.id()), usefulness,
				new GapDraft(GapDraft.Kind.QUESTION, I18N.tf("gap.draft.death.title", person.name()),
					I18N.tf("gap.draft.death.question", person.name()), List.of(person.id()), null),
				person.name(), String.valueOf(person.birthYear()), age));
		}
		addOrAggregate("many_no_death", group, findings, "gap.draft.many_deaths.task");
	}

	/** The birth of an individual the research is about (they have children, or events): where do they come from? */
	private static void checkBirths(final GapData data, final Map<String, Integer> children, final List<GapFinding> findings){
		final List<GapFinding> group = new ArrayList<>();
		for(final GapData.Person person : data.persons().values()){
			final int sons = children.getOrDefault(person.id(), 0);
			if(person.hasBirth() || (sons == 0 && person.eventCount() == 0))
				continue;

			final int usefulness = 45 + (sons > 0? 10: 0) + (person.eventCount() > 0? 5: 0);
			group.add(finding(Diagnostic.Severity.INFORMATION, "no_birth", List.of(person.id()), usefulness,
				new GapDraft(GapDraft.Kind.QUESTION, I18N.tf("gap.draft.birth.title", person.name()),
					I18N.tf("gap.draft.birth.question", person.name()), List.of(person.id()), null),
				person.name(), sons, person.eventCount()));
		}
		addOrAggregate("many_no_birth", group, findings, "gap.draft.many_births.task");
	}

	/** The parents of a child with no union between them, and no marriage event: children exist, but no marriage is known. */
	private static void checkMarriages(final GapData data, final List<GapFinding> findings){
		final Map<String, List<String>> parentsOf = new LinkedHashMap<>();
		for(final GapData.ParentLink link : data.parents()){
			final List<String> parents = parentsOf.computeIfAbsent(link.childId(), k -> new ArrayList<>());
			if(!parents.contains(link.parentId()))
				parents.add(link.parentId());
		}

		final Map<String, List<String>> childrenOfCouple = new LinkedHashMap<>();
		final Map<String, List<String>> couples = new LinkedHashMap<>();
		for(final Map.Entry<String, List<String>> entry : parentsOf.entrySet()){
			if(entry.getValue().size() != 2 || !data.persons().containsKey(entry.getValue().get(0))
					|| !data.persons().containsKey(entry.getValue().get(1)))
				continue;

			final String key = pair(entry.getValue().get(0), entry.getValue().get(1));
			childrenOfCouple.computeIfAbsent(key, k -> new ArrayList<>())
				.add(entry.getKey());
			couples.putIfAbsent(key, entry.getValue());
		}

		final Set<String> married = new HashSet<>();
		data.unions().forEach(u -> married.add(pair(u.firstId(), u.secondId())));
		for(final GapData.Event event : data.events())
			if("marriage".equals(event.type()))
				for(int i = 0; i < event.principals().size(); i ++)
					for(int j = i + 1; j < event.principals().size(); j ++)
						married.add(pair(event.principals().get(i), event.principals().get(j)));

		final List<GapFinding> group = new ArrayList<>();
		for(final Map.Entry<String, List<String>> entry : childrenOfCouple.entrySet()){
			if(married.contains(entry.getKey()))
				continue;

			final List<String> couple = couples.get(entry.getKey());
			final String first = data.persons().get(couple.get(0)).name();
			final String second = data.persons().get(couple.get(1)).name();
			final List<String> ids = new ArrayList<>(couple);
			ids.addAll(entry.getValue());
			group.add(finding(Diagnostic.Severity.INFORMATION, "children_without_marriage", ids,
				Math.min(70, 45 + 5 * entry.getValue().size()),
				new GapDraft(GapDraft.Kind.QUESTION, I18N.tf("gap.draft.marriage.title", first, second),
					I18N.tf("gap.draft.marriage.question", first, second), couple, null),
				first, second, entry.getValue().size()));
		}
		addOrAggregate("many_children_without_marriage", group, findings, "gap.draft.many_marriages.task");
	}

	/** The vital events (birth, death, marriage) that no source cites: the assertions with nothing behind them. One finding for all. */
	private static void checkSources(final GapData data, final List<GapFinding> findings){
		final List<String> unsourced = data.events().stream()
			.filter(e -> VITAL_EVENTS.contains(e.type()) && e.sourceCount() == 0)
			.map(GapData.Event::id)
			.toList();
		if(unsourced.isEmpty())
			return;

		findings.add(finding(Diagnostic.Severity.INFORMATION, "unsourced_vital_events", unsourced.stream().limit(MAX_LISTED).toList(), 70,
			new GapDraft(GapDraft.Kind.TASK, "", I18N.tf("gap.draft.sources.task", unsourced.size()), List.of(), "normal"),
			unsourced.size(), Math.min(unsourced.size(), MAX_LISTED)));
	}

	/** The people found in a census who have no residence recorded: the census says where they lived. One finding for all. */
	private static void checkCensusResidences(final GapData data, final List<GapFinding> findings){
		final List<String> withoutResidence = data.persons().values().stream()
			.filter(p -> p.hasCensus() && !p.hasResidence())
			.map(GapData.Person::id)
			.toList();
		if(withoutResidence.isEmpty())
			return;

		findings.add(finding(Diagnostic.Severity.INFORMATION, "census_without_residence",
			withoutResidence.stream().limit(MAX_LISTED).toList(), 40,
			new GapDraft(GapDraft.Kind.TASK, "", I18N.tf("gap.draft.census.task", withoutResidence.size()), List.of(), "low"),
			withoutResidence.size(), Math.min(withoutResidence.size(), MAX_LISTED)));
	}

	/** The open questions that nobody works on, or that have run out of things to do. */
	private static void checkQuestions(final GapData data, final List<GapFinding> findings){
		for(final GapData.Question question : data.questions()){
			if(!OPEN_QUESTIONS.contains(question.status()))
				continue;

			final List<GapData.Activity> activities = data.activities().stream()
				.filter(a -> a.questionIds().contains(question.id()))
				.toList();
			if(activities.isEmpty()){
				findings.add(finding(Diagnostic.Severity.INFORMATION, "question_without_activities", List.of(question.id()),
					("open".equals(question.status())? 75: 45),
					new GapDraft(GapDraft.Kind.TASK, "", I18N.tf("gap.draft.activity.task", question.title()),
						List.of(question.id()), "normal"),
					question.title(), question.status()));
				continue;
			}

			final boolean allOver = activities.stream().allMatch(a -> FINISHED_ACTIVITIES.contains(a.status()));
			final boolean somethingPlanned = data.tasks().stream()
				.anyMatch(t -> t.questionIds().contains(question.id()) && ACTIVE_TASKS.contains(t.status()));
			if("open".equals(question.status()) && allOver && !somethingPlanned && !question.hasConclusion())
				findings.add(finding(Diagnostic.Severity.INFORMATION, "question_stalled", List.of(question.id()), 65,
					new GapDraft(GapDraft.Kind.TASK, "", I18N.tf("gap.draft.stalled.task", question.title()),
						List.of(question.id()), "normal"),
					question.title(), activities.size()));
		}
	}

	/** A search that found nothing (or could not be done) and led to nothing; and the completed activities with no result. */
	private static void checkActivities(final GapData data, final List<GapFinding> findings){
		final Map<String, GapData.Question> questions = new HashMap<>();
		data.questions().forEach(q -> questions.put(q.id(), q));

		final List<String> withoutResult = new ArrayList<>();
		for(final GapData.Activity activity : data.activities()){
			if(!"completed".equals(activity.status()))
				continue;

			if(activity.result() == null){
				withoutResult.add(activity.id());
				continue;
			}

			final boolean concerned = activity.questionIds().isEmpty() || activity.questionIds().stream()
				.anyMatch(id -> questions.get(id) != null && OPEN_QUESTIONS.contains(questions.get(id).status()));
			if(UNSUCCESSFUL_RESULTS.contains(activity.result()) && !activity.hasFollowUp() && !activity.isParent() && concerned){
				final List<String> ids = new ArrayList<>(List.of(activity.id()));
				ids.addAll(activity.questionIds());
				findings.add(finding(Diagnostic.Severity.INFORMATION, "no_follow_up", ids, 50,
					new GapDraft(GapDraft.Kind.TASK, "", I18N.tf("gap.draft.follow_up.task", activity.action(), activity.result()),
						activity.questionIds(), "normal"),
					activity.action(), activity.result()));
			}
		}

		if(!withoutResult.isEmpty())
			findings.add(finding(Diagnostic.Severity.INFORMATION, "activities_without_result",
				withoutResult.stream().limit(MAX_LISTED).toList(), 25, null, withoutResult.size(),
				Math.min(withoutResult.size(), MAX_LISTED)));
	}

	/** A task that is not done and past its due date. */
	private static void checkTasks(final GapData data, final List<GapFinding> findings){
		final long today = data.today().toEpochDay() + UNIX_EPOCH_JDN;
		for(final GapData.Task task : data.tasks()){
			if(!ACTIVE_TASKS.contains(task.status()) || task.due() == null || task.due().max() >= today)
				continue;

			final long days = today - task.due().max();
			final int bonus = ("high".equals(task.priority())? 15: ("normal".equals(task.priority())? 5: 0));
			final List<String> ids = new ArrayList<>(List.of(task.id()));
			ids.addAll(task.questionIds());
			findings.add(finding(Diagnostic.Severity.WARNING, "overdue_task", ids, 70 + bonus, null, task.description(), days));
		}
	}


	/**
	 * A gap that is about each individual is a finding for each of them, until there are too many to be useful: an imported tree with
	 * hundreds of ancestors with no death would bury every other finding, and the count of the indicator. Then it is one finding, with
	 * the individuals listed, the usefulness of the most useful of them and a task for all.
	 *
	 * @param group the findings, one for each individual or couple
	 */
	private static void addOrAggregate(final String aggregateKey, final List<GapFinding> group, final List<GapFinding> findings,
			final String taskKey){
		if(group.size() <= MAX_INDIVIDUAL_FINDINGS){
			findings.addAll(group);
			return;
		}

		final List<String> ids = group.stream()
			.sorted((a, b) -> Double.compare(b.diagnostic().rank(), a.diagnostic().rank()))
			.flatMap(f -> f.diagnostic().recordIds().stream())
			.distinct()
			.limit(MAX_LISTED)
			.toList();
		final int usefulness = (int)group.stream().mapToDouble(f -> f.diagnostic().rank()).max().orElse(0);
		findings.add(finding(Diagnostic.Severity.INFORMATION, aggregateKey, ids, usefulness,
			new GapDraft(GapDraft.Kind.TASK, "", I18N.tf(taskKey, group.size()), List.of(), "normal"),
			group.size(), Math.min(group.size(), MAX_LISTED)));
	}

	private static String pair(final String first, final String second){
		return (first.compareTo(second) <= 0? first + "|" + second: second + "|" + first);
	}

	/**
	 * @param usefulness how useful it would be to fill the gap, from 0 to 100: it orders the findings
	 * @param draft      the question or the task that would start to fill it ({@code null} if there is none)
	 */
	private static GapFinding finding(final Diagnostic.Severity severity, final String key, final List<String> recordIds,
			final int usefulness, final GapDraft draft, final Object... arguments){
		final String level = level(usefulness);
		final Diagnostic diagnostic = new Diagnostic(severity, AssistantCategory.RESEARCH, I18N.tf("gap." + key + ".title", arguments),
			I18N.tf("gap." + key + ".explanation", arguments) + " " + I18N.tf("gap.usefulness", I18N.t("gap.usefulness." + level),
				usefulness),
			recordIds, null, DiagnosticKeys.of("gap." + key, recordIds, arguments), List.of(), usefulness);
		return new GapFinding(diagnostic, draft);
	}

}
