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

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.readers.ConfidenceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchActivityReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticKeys;
import org.apache.commons.lang3.StringUtils;

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
	/** The years from the birth of a parent to the birth of a child, when only the birth of the parent is known. */
	static final int GENERATION_YEARS = 25;
	/** The usefulness from which a gap is called high, and the one from which it is called medium. */
	static final int HIGH_USEFULNESS = 70;
	static final int MEDIUM_USEFULNESS = 45;

	/** The Julian Day Number of 1970-01-01, to turn a day into a Julian Day Number. */
	private static final long UNIX_EPOCH_JDN = 2_440_588L;

	private static final Set<String> VITAL_EVENTS = Set.of(EventReader.ENUM_TYPE_BIRTH, EventReader.ENUM_TYPE_DEATH, EventReader.ENUM_TYPE_MARRIAGE);
	private static final Set<String> OPEN_QUESTIONS = Set.of(ResearchQuestionReader.ENUM_STATUS_OPEN, ResearchQuestionReader.ENUM_STATUS_ON_HOLD);
	private static final Set<String> FINISHED_ACTIVITIES = Set.of(ResearchActivityReader.ENUM_STATUS_COMPLETED, ResearchActivityReader.ENUM_STATUS_ABANDONED);
	private static final Set<String> UNSUCCESSFUL_RESULTS = Set.of(ResearchActivityReader.ENUM_RESULT_NEGATIVE, ResearchActivityReader.ENUM_RESULT_INCONCLUSIVE,
		ResearchActivityReader.ENUM_RESULT_UNAVAILABLE);
	private static final Set<String> ACTIVE_TASKS = Set.of(ResearchTaskReader.ENUM_STATUS_OPEN, ResearchTaskReader.ENUM_STATUS_IN_PROGRESS);


	/** The most places named in the explanation and in the draft of a birth. */
	private static final int MAX_PLACES_LISTED = 3;


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
			return ConfidenceReader.ENUM_HIGH;
		return (usefulness >= MEDIUM_USEFULNESS? ConfidenceReader.ENUM_MEDIUM: ConfidenceReader.ENUM_LOW);
	}


	/** The death of an individual who is certainly (or probably) dead: they were born a century ago, and no death is recorded. */
	private static void checkDeaths(final GapData data, final List<GapFinding> findings){
		final List<GapFinding> group = new ArrayList<>();
		final Map<String, List<String>> childrenOf = new HashMap<>();
		final Map<String, List<String>> parentsOf = new HashMap<>();
		for(final GapData.ParentLink link : data.parents()){
			childrenOf.computeIfAbsent(link.parentId(), k -> new ArrayList<>())
				.add(link.childId());
			parentsOf.computeIfAbsent(link.childId(), k -> new ArrayList<>())
				.add(link.parentId());
		}

		final int thisYear = data.today().getYear();
		for(final GapData.Person person : data.persons().values()){
			if(person.hasDeath())
				continue;

			//own year of birth first; if there is none, the year the relatives allow, then the one estimated from the parents
			String key = "no_death";
			Integer year = person.birthYear();
			Integer parentYear = null;
			if(year == null){
				year = latestBirthYear(person.id(), data, childrenOf, parentsOf, new HashSet<>());
				key = "no_death_%s_relatives";
				if(year == null || thisYear - year < PROBABLY_DEAD_YEARS){
					parentYear = latestParentBirthYear(person.id(), data, parentsOf);
					year = (parentYear != null? parentYear + GENERATION_YEARS: null);
					key = "no_death_probable_estimated";
				}
			}
			if(year == null)
				continue;

			final int age = thisYear - year;
			final boolean certain = (age >= CERTAINLY_DEAD_YEARS && !key.endsWith("_estimated"));
			final int usefulness = (certain? 85: (age >= PROBABLY_DEAD_YEARS? 55: 0));
			if(usefulness == 0)
				continue;

			if(key.contains("%s"))
				key = key.formatted(certain? "certain": "probable");
			else if(key.equals("no_death"))
				key = (certain? "no_death_certain": "no_death_probable");
			group.add(finding(Diagnostic.Severity.INFORMATION, key, List.of(person.id()), usefulness,
				new GapDraft(GapDraft.Kind.QUESTION, I18N.tf("gap.draft.death.title", person.name()),
					I18N.tf("gap.draft.death.question", person.name()), List.of(person.id()), null),
				person.name() + " [" + person.id() + "]", String.valueOf(year), age, String.valueOf(parentYear), GENERATION_YEARS));
		}
		findings.addAll(group);
	}

	/**
	 * @return the latest year the individual can have been born, from the dates of their descendants and ancestors (a parent is born no
	 * later than a child is born or dies, a child is born no later than the year after the death of a parent), or {@code null} if the
	 * relatives say nothing; {@code visiting} guards against loops
	 */
	private static Integer latestBirthYear(final String id, final GapData data, final Map<String, List<String>> childrenOf,
			final Map<String, List<String>> parentsOf, final Set<String> visiting){
		if(!visiting.add(id))
			return null;

		Integer latest = null;
		for(final String childId : childrenOf.getOrDefault(id, List.of())){
			final GapData.Person child = data.persons().get(childId);
			if(child == null)
				continue;

			latest = min(latest, child.birthYear());
			latest = min(latest, child.deathYear());
			latest = min(latest, latestBirthYear(childId, data, childrenOf, parentsOf, visiting));
		}
		for(final String parentId : parentsOf.getOrDefault(id, List.of())){
			final GapData.Person parent = data.persons().get(parentId);
			if(parent != null && parent.deathYear() != null)
				latest = min(latest, parent.deathYear() + 1);
		}
		visiting.remove(id);
		return latest;
	}

	/** @return the year of birth of the youngest parent who has one, or {@code null} */
	private static Integer latestParentBirthYear(final String id, final GapData data, final Map<String, List<String>> parentsOf){
		Integer latest = null;
		for(final String parentId : parentsOf.getOrDefault(id, List.of())){
			final GapData.Person parent = data.persons().get(parentId);
			if(parent != null && parent.birthYear() != null && (latest == null || parent.birthYear() > latest))
				latest = parent.birthYear();
		}
		return latest;
	}

	private static Integer min(final Integer a, final Integer b){
		return (a == null? b: (b == null? a: Integer.valueOf(Math.min(a, b))));
	}

	/** The birth of an individual the research is about (they have children, or events): where do they come from? */
	private static void checkBirths(final GapData data, final Map<String, Integer> children, final List<GapFinding> findings){
		final List<GapFinding> group = new ArrayList<>();
		for(final GapData.Person person : data.persons().values()){
			final int sons = children.getOrDefault(person.id(), 0);
			if(person.hasBirth() || (sons == 0 && person.eventCount() == 0))
				continue;

			final int usefulness = 45 + (sons > 0? 10: 0) + (person.eventCount() > 0? 5: 0);
			if(!person.places().isEmpty()){
				//a place is known but not the birth record: the registers of that place are where to start
				final String places = String.join(", ", person.places().stream()
					.limit(MAX_PLACES_LISTED)
					.toList());
				group.add(finding(Diagnostic.Severity.INFORMATION, "no_birth_place", List.of(person.id()), usefulness + 5,
					new GapDraft(GapDraft.Kind.QUESTION, I18N.tf("gap.draft.birth.title", person.name()),
						I18N.tf("gap.draft.birth_place.question", person.name(), places), List.of(person.id()), null),
					person.name() + " [" + person.id() + "]", sons, person.eventCount(), places));
				continue;
			}

			group.add(finding(Diagnostic.Severity.INFORMATION, "no_birth", List.of(person.id()), usefulness,
				new GapDraft(GapDraft.Kind.QUESTION, I18N.tf("gap.draft.birth.title", person.name()),
					I18N.tf("gap.draft.birth.question", person.name()), List.of(person.id()), null),
				person.name() + " [" + person.id() + "]", sons, person.eventCount()));
		}
		findings.addAll(group);
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
			if(EventReader.ENUM_TYPE_MARRIAGE.equals(event.type()))
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
				first + " [" + couple.get(0) + "]", second + " [" + couple.get(1) + "]", entry.getValue().size()));
		}
		findings.addAll(group);
	}

	/** The vital events (birth, death, marriage) that no source cites: the assertions with nothing behind them. One finding for each. */
	private static void checkSources(final GapData data, final List<GapFinding> findings){
		for(final GapData.Event event : data.events()){
			if(!VITAL_EVENTS.contains(event.type()) || event.sourceCount() != 0)
				continue;

			final List<String> ids = new ArrayList<>(List.of(event.id()));
			ids.addAll(event.principals());
			final String who = (event.principals().isEmpty() || !data.persons().containsKey(event.principals().getFirst())
				? event.id()
				: data.persons().get(event.principals().getFirst()).name() + " [" + event.principals().getFirst() + "]");
			findings.add(finding(Diagnostic.Severity.INFORMATION, "unsourced_vital_event", ids, 70,
				new GapDraft(GapDraft.Kind.TASK, "", I18N.tf("gap.draft.source.task", event.type(), who), event.principals(),
					ResearchTaskReader.ENUM_PRIORITY_NORMAL),
				event.type(), who, event.id()));
		}
	}

	/** The people found in a census who have no residence recorded: the census says where they lived. One finding for each. */
	private static void checkCensusResidences(final GapData data, final List<GapFinding> findings){
		for(final GapData.Person person : data.persons().values())
			if(person.hasCensus() && !person.hasResidence())
				findings.add(finding(Diagnostic.Severity.INFORMATION, "census_without_residence", List.of(person.id()), 40,
					new GapDraft(GapDraft.Kind.TASK, "", I18N.tf("gap.draft.census.task", person.name()), List.of(person.id()),
						ResearchTaskReader.ENUM_PRIORITY_LOW),
					person.name() + " [" + person.id() + "]"));
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
					(ResearchQuestionReader.ENUM_STATUS_OPEN.equals(question.status())? 75: 45),
					new GapDraft(GapDraft.Kind.TASK, "", I18N.tf("gap.draft.activity.task", question.title()),
						List.of(question.id()), ResearchTaskReader.ENUM_PRIORITY_NORMAL),
					question.title() + " [" + question.id() + "]", question.status()));
				continue;
			}

			final boolean allOver = activities.stream().allMatch(a -> FINISHED_ACTIVITIES.contains(a.status()));
			final boolean somethingPlanned = data.tasks().stream()
				.anyMatch(t -> t.questionIds().contains(question.id()) && ACTIVE_TASKS.contains(t.status()));
			if(ResearchQuestionReader.ENUM_STATUS_OPEN.equals(question.status()) && allOver && !somethingPlanned && !question.hasConclusion())
				findings.add(finding(Diagnostic.Severity.INFORMATION, "question_stalled", List.of(question.id()), 65,
					new GapDraft(GapDraft.Kind.TASK, "", I18N.tf("gap.draft.stalled.task", question.title()),
						List.of(question.id()), ResearchTaskReader.ENUM_PRIORITY_NORMAL),
					question.title() + " [" + question.id() + "]", activities.size()));
		}
	}

	/** A search that found nothing (or could not be done) and led to nothing; and the completed activities with no result. */
	private static void checkActivities(final GapData data, final List<GapFinding> findings){
		final Map<String, GapData.Question> questions = new HashMap<>();
		data.questions().forEach(q -> questions.put(q.id(), q));

		for(final GapData.Activity activity : data.activities()){
			if(!ResearchActivityReader.ENUM_STATUS_COMPLETED.equals(activity.status()))
				continue;

			if(activity.result() == null){
				findings.add(finding(Diagnostic.Severity.INFORMATION, "activities_without_result", List.of(activity.id()), 25, null,
					activity.action() + " [" + activity.id() + "]"));
				continue;
			}

			final boolean concerned = activity.questionIds().isEmpty() || activity.questionIds().stream()
				.anyMatch(id -> questions.get(id) != null && OPEN_QUESTIONS.contains(questions.get(id).status()));
			if(UNSUCCESSFUL_RESULTS.contains(activity.result()) && !activity.hasFollowUp() && !activity.isParent() && concerned){
				final List<String> ids = new ArrayList<>(List.of(activity.id()));
				ids.addAll(activity.questionIds());
				findings.add(finding(Diagnostic.Severity.INFORMATION, "no_follow_up", ids, 50,
					new GapDraft(GapDraft.Kind.TASK, "", I18N.tf("gap.draft.follow_up.task", activity.action(), activity.result()),
						activity.questionIds(), ResearchTaskReader.ENUM_PRIORITY_NORMAL),
					activity.action() + " [" + activity.id() + "]", activity.result()));
			}
		}
	}

	/** A task that is not done and past its due date. */
	private static void checkTasks(final GapData data, final List<GapFinding> findings){
		final long today = data.today().toEpochDay() + UNIX_EPOCH_JDN;
		for(final GapData.Task task : data.tasks()){
			if(!ACTIVE_TASKS.contains(task.status()) || task.due() == null || task.due().max() >= today)
				continue;

			final long days = today - task.due().max();
			final int bonus = (ResearchTaskReader.ENUM_PRIORITY_HIGH.equals(task.priority())? 15: (ResearchTaskReader.ENUM_PRIORITY_NORMAL.equals(task.priority())? 5: 0));
			final List<String> ids = new ArrayList<>(List.of(task.id()));
			ids.addAll(task.questionIds());
			findings.add(finding(Diagnostic.Severity.WARNING, "overdue_task", ids, 70 + bonus, null, task.description() + " [" + task.id() + "]", days));
		}
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
			I18N.tf("gap." + key + ".explanation", arguments) + StringUtils.SPACE + I18N.tf("gap.usefulness", I18N.t("gap.usefulness." + level),
				usefulness),
			recordIds, null, DiagnosticKeys.of("gap." + key, recordIds, arguments), List.of(), usefulness);
		return new GapFinding(diagnostic, draft);
	}

}
