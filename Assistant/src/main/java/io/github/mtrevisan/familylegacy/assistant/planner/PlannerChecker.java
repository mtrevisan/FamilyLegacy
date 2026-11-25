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
package io.github.mtrevisan.familylegacy.assistant.planner;

import io.github.mtrevisan.familylegacy.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.assistant.AssistantTuning;
import io.github.mtrevisan.familylegacy.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.assistant.DiagnosticKeys;
import io.github.mtrevisan.familylegacy.assistant.Sentences;
import io.github.mtrevisan.familylegacy.assistant.gap.GapData;
import io.github.mtrevisan.familylegacy.assistant.gap.GapDraft;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.readers.ConfidenceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchActivityReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
 *   that no task or activity of the question already covers (a draft of a research task, with the reason; if the tree has repositories at the places of the individual, or at the places that
 *   contain them, the task names them and the years to look at);</li>
 *   <li>the brick walls worth working on: individuals whose parents are unknown, ranked by how much of the tree depends on them (a
 *   draft of a research question);</li>
 *   <li>the kind of source to search next: the one that would serve most of the steps of a question and that no source cited for the
 *   individual is (a draft of a research task);</li>
 *   <li>the repositories to try beyond the places of the individual: the ones at the places of its parents, partners and children
 *   (a draft of a research task);</li>
 *   <li>the identity hypotheses that deserve attention: the ones no conclusion evaluates yet, ordered by their confidence, by whether an
 *   open question is about one of the two individuals and by whether the identity would give parents to an individual that has none (a
 *   draft of a research question).</li>
 * </ul>
 * Each recommendation is made from what the tree already holds, and says which data it comes from.
 * Nothing is created: the drafts are the start of the usual editors, saved only if the researcher confirms.
 */
public final class PlannerChecker{

	/** The most steps proposed for one question. */
	static final int MAX_STEPS_PER_QUESTION = 3;

	static final int HIGH_USEFULNESS = 70;
	static final int MEDIUM_USEFULNESS = 45;

	/** An individual this old, or older, is taken to be dead, so their death can be searched. */
	private static final int DEAD_AGE = 80;
	private static final int FIRST_CENSUS_YEAR = 1700;
	private static final int LAST_CENSUS_YEAR = 1950;
	/** How many individuals of a question are planned for: a question about a whole family would drown the plan. */
	private static final int MAX_PERSONS_PER_QUESTION = 3;
	/** How many repositories are named in a task. */
	private static final int MAX_REPOSITORIES = 3;
	/** How many searches that found nothing make the records worth asking the archive for. */
	private static final int FRUITLESS_SEARCHES = 2;
	/** How many repositories of the relatives are proposed for one individual. */
	private static final int MAX_LEADS_PER_PERSON = 2;
	/** Civil registration started around 1800 in much of Europe, so the records of an individual born before are only in the church ones. */
	private static final int CIVIL_REGISTRATION_FROM = 1780;
	/** What a repository of a relative is worth less than the step it would serve: it is a lead, not a gap. */
	private static final int LEAD_DISCOUNT = 10;
	/** What each further step served by the same kind of source adds to its usefulness. */
	private static final int SHARED_KIND_BONUS = 5;
	/** The usefulness of an identity hypothesis not evaluated yet, by its confidence. */
	private static final int HYPOTHESIS_HIGH = 70;
	private static final int HYPOTHESIS_MEDIUM = 55;
	private static final int HYPOTHESIS_LOW = 40;
	/** What an open question about one of the two individuals adds to the usefulness of their hypothesis. */
	private static final int HYPOTHESIS_QUESTION_BONUS = 15;
	/** What the parents that one of the two individuals would gain add to the usefulness of their hypothesis. */
	private static final int HYPOTHESIS_PARENTS_BONUS = 10;
	private static final int MAX_USEFULNESS = 95;


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


	/** The numbers of the user: the weights of the usefulness of each step, and the limits of the levels. */
	private final AssistantTuning tuning;


	private PlannerChecker(final AssistantTuning tuning){
		this.tuning = tuning;
	}


	/**
	 * @param data the research
	 * @return the suggestions, in no particular order (each has its usefulness as rank)
	 */
	public static List<PlannerFinding> findings(final GapData data){
		return findings(data, AssistantTuning.DEFAULTS);
	}

	/** @return the suggestions with the numbers of the user */
	public static List<PlannerFinding> findings(final GapData data, final AssistantTuning tuning){
		final PlannerChecker checker = new PlannerChecker(tuning);
		final List<PlannerFinding> findings = new ArrayList<>();
		findings.addAll(checker.steps(data));
		findings.addAll(checker.brickWalls(data));
		findings.addAll(checker.identityHypotheses(data));
		return findings;
	}

	/** @return how useful a suggestion with that estimated usefulness is: high, medium or low */
	static String level(final int usefulness){
		return level(usefulness, AssistantTuning.DEFAULTS);
	}

	static String level(final int usefulness, final AssistantTuning tuning){
		if(usefulness >= tuning.get(AssistantTuning.Parameter.PLANNER_HIGH))
			return ConfidenceReader.ENUM_HIGH;
		return (usefulness >= tuning.get(AssistantTuning.Parameter.PLANNER_MEDIUM)? ConfidenceReader.ENUM_MEDIUM: ConfidenceReader.ENUM_LOW);
	}

	/** @return the priority to propose for a task with that estimated usefulness */
	static String priority(final int usefulness){
		return priority(usefulness, AssistantTuning.DEFAULTS);
	}

	static String priority(final int usefulness, final AssistantTuning tuning){
		if(usefulness >= tuning.get(AssistantTuning.Parameter.PLANNER_HIGH))
			return ResearchTaskReader.ENUM_PRIORITY_HIGH;
		return (usefulness >= tuning.get(AssistantTuning.Parameter.PLANNER_MEDIUM)
			? ResearchTaskReader.ENUM_PRIORITY_NORMAL: ResearchTaskReader.ENUM_PRIORITY_LOW);
	}


	private List<PlannerFinding> steps(final GapData data){
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

			final List<Candidate> retained = candidates.stream()
				.filter(candidate -> !candidate.step().isCoveredBy(covered))
				.sorted(Comparator.comparingInt(Candidate::usefulness).reversed())
				.limit(MAX_STEPS_PER_QUESTION)
				.toList();
			for(final Candidate candidate : retained)
				findings.add(step(question, candidate));
			sourceKind(question, retained, covered, findings);
			repositories(question, retained, findings);
		}
		return findings;
	}

	private static boolean isOpen(final GapData.Question question){
		return (question.status() == null || question.status().isBlank() || ResearchQuestionReader.ENUM_STATUS_OPEN.equals(question.status()));
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
			if(task.questionIds().contains(question.id()) && !ResearchTaskReader.ENUM_STATUS_ABANDONED.equals(task.status()) && task.description() != null)
				sb.append(task.description().toLowerCase(Locale.ROOT)).append('\n');
		for(final GapData.Activity activity : data.activities())
			if(activity.questionIds().contains(question.id()) && !ResearchActivityReader.ENUM_STATUS_ABANDONED.equals(activity.status()) && activity.action() != null)
				sb.append(activity.action().toLowerCase(Locale.ROOT)).append('\n');
		return sb.toString();
	}

	private static boolean nothingStarted(final GapData.Question question, final GapData data){
		return (data.tasks().stream().noneMatch(task -> task.questionIds().contains(question.id()))
			&& data.activities().stream().noneMatch(activity -> activity.questionIds().contains(question.id())));
	}

	private List<Candidate> candidates(final GapData.Person person, final GapData data, final boolean nothingStarted){
		final List<Candidate> candidates = new ArrayList<>();
		final int bonus = (nothingStarted? 5: 0);
		final Integer birthYear = person.birthYear();
		final String year = (birthYear != null? String.valueOf(birthYear): StringUtils.EMPTY);

		final List<GapData.Event> births = eventsOf(person, data, EventReader.ENUM_TYPE_BIRTH);
		if(births.isEmpty() && !person.hasBirth())
			candidates.add(new Candidate(Step.BIRTH_MISSING, person, tuning.get(AssistantTuning.Parameter.PLANNER_BIRTH_MISSING) + bonus, year, ""));
		//a source that cites the parentage (usually the birth or baptism record) is a source of the birth too
		else if(births.stream().allMatch(event -> event.sourceCount() == 0) && data.parents().stream()
				.noneMatch(link -> link.childId().equals(person.id()) && link.sourceCount() > 0))
			candidates.add(new Candidate(Step.BIRTH_UNSOURCED, person, tuning.get(AssistantTuning.Parameter.PLANNER_BIRTH_UNSOURCED) + bonus, year, ""));

		final boolean parentsKnown = data.parents().stream().anyMatch(link -> link.childId().equals(person.id()));
		final boolean married = data.unions().stream().anyMatch(union -> union.firstId().equals(person.id())
			|| union.secondId().equals(person.id()));
		final List<GapData.Event> marriages = eventsOf(person, data, EventReader.ENUM_TYPE_MARRIAGE);
		if(!parentsKnown){
			candidates.add(new Candidate(Step.PARENTS, person, tuning.get(AssistantTuning.Parameter.PLANNER_PARENTS) + bonus, year, ""));
			if(birthYear != null)
				candidates.add(new Candidate(Step.BAPTISMS, person, tuning.get(AssistantTuning.Parameter.PLANNER_BAPTISMS) + bonus, year, ""));
		}
		//the same for a source that cites the marriage relationship
		else if(married && (marriages.isEmpty() || marriages.stream().allMatch(event -> event.sourceCount() == 0)) && data.unions().stream()
				.noneMatch(union -> (union.firstId().equals(person.id()) || union.secondId().equals(person.id())) && union.sourceCount() > 0))
			candidates.add(new Candidate(Step.MARRIAGE, person, tuning.get(AssistantTuning.Parameter.PLANNER_MARRIAGE) + bonus, year, ""));

		final List<GapData.Event> deaths = eventsOf(person, data, EventReader.ENUM_TYPE_DEATH);
		final boolean deathUnknown = (deaths.isEmpty() && !person.hasDeath());
		final boolean deathUnsourced = (!deaths.isEmpty() && deaths.stream().allMatch(event -> event.sourceCount() == 0));
		if((deathUnknown || deathUnsourced) && birthYear != null && data.today().getYear() - birthYear >= DEAD_AGE)
			candidates.add(new Candidate(Step.DEATH, person, tuning.get(AssistantTuning.Parameter.PLANNER_DEATH) + bonus, year, ""));

		if(!person.hasCensus() && birthYear != null && birthYear >= FIRST_CENSUS_YEAR && birthYear <= LAST_CENSUS_YEAR)
			candidates.add(new Candidate(Step.CENSUS, person, tuning.get(AssistantTuning.Parameter.PLANNER_CENSUS) + bonus, year, ""));
		return candidates;
	}

	private static List<GapData.Event> eventsOf(final GapData.Person person, final GapData data, final String type){
		return data.events().stream()
			.filter(event -> type.equals(event.type()) && event.principals().contains(person.id()))
			.toList();
	}

	/** Searches that found nothing are the sign that the records are not where they were looked for: it is time to ask the archive. */
	private void addArchive(final GapData.Question question, final GapData.Person person, final GapData data,
			final List<Candidate> candidates){
		final long fruitless = data.activities().stream()
			.filter(activity -> activity.questionIds().contains(question.id()) && ResearchActivityReader.ENUM_STATUS_COMPLETED.equals(activity.status())
				&& activity.result() != null && !ResearchActivityReader.ENUM_RESULT_POSITIVE.equals(activity.result()))
			.count();
		if(fruitless >= FRUITLESS_SEARCHES)
			candidates.add(new Candidate(Step.ARCHIVE, person, tuning.get(AssistantTuning.Parameter.PLANNER_ARCHIVE), "", String.valueOf(fruitless)));
	}

	/**
	 * Says where the records of the places of the individual may be kept (the repositories of the tree at those places, or at the places
	 * that contain them), and for which years.
	 */
	private static String where(final String task, final Candidate candidate){
		final List<String> repositories = candidate.person().repositories();
		if(repositories.isEmpty() || candidate.step() == Step.ARCHIVE)
			return task;

		final String names = Sentences.list(repositories.subList(0, Math.min(MAX_REPOSITORIES, repositories.size())));
		return I18N.tf((candidate.year().isEmpty()? "planner.step.where": "planner.step.where.year"), task, names, candidate.year());
	}

	private PlannerFinding step(final GapData.Question question, final Candidate candidate){
		final Step step = candidate.step();
		final String name = candidate.person().name() + " [" + candidate.person().id() + "]";
		final String task = where(I18N.tf("planner.step." + step.key + ".task", name, candidate.year()), candidate);
		final String why = I18N.tf("planner.step." + step.key + ".why", name, candidate.year(), candidate.count());
		final String title = (question.title() != null && !question.title().isBlank()? question.title() + " [" + question.id() + "]": question.id());
		final String level = level(candidate.usefulness(), tuning);

		final Diagnostic diagnostic = new Diagnostic(Diagnostic.Severity.INFORMATION, AssistantCategory.RESEARCH,
			I18N.tf("planner.next_step.title", task),
			Sentences.join(I18N.tf("planner.next_step.explanation", title, why),
				I18N.tf("gap.usefulness", I18N.t("gap.usefulness." + level), String.valueOf(candidate.usefulness()))),
			List.of(question.id(), candidate.person().id()), null,
			DiagnosticKeys.of("planner.next_step", List.of(question.id(), candidate.person().id()), step.key))
			.withRank(candidate.usefulness());
		return new PlannerFinding(diagnostic, new GapDraft(GapDraft.Kind.TASK, "", task, List.of(question.id()),
			priority(candidate.usefulness(), tuning)));
	}


	/** @return the kinds of source that can answer a step, the likeliest first */
	private static List<String> kindsOf(final Candidate candidate){
		final Integer birthYear = candidate.person().birthYear();
		final boolean civil = (birthYear == null || birthYear >= CIVIL_REGISTRATION_FROM);
		final List<String> kinds = new ArrayList<>();
		switch(candidate.step()){
			case BIRTH_MISSING, BIRTH_UNSOURCED, PARENTS, BAPTISMS, MARRIAGE -> {
				kinds.add(SourceReader.ENUM_KIND_CHURCH_REGISTER);
				if(civil)
					kinds.add(SourceReader.ENUM_KIND_CIVIL_REGISTRATION);
			}
			case DEATH -> {
				kinds.add(SourceReader.ENUM_KIND_CHURCH_REGISTER);
				if(civil)
					kinds.add(SourceReader.ENUM_KIND_CIVIL_REGISTRATION);
				kinds.add(SourceReader.ENUM_KIND_CEMETERY_RECORD);
				kinds.add(SourceReader.ENUM_KIND_WILL_PROBATE);
			}
			case CENSUS -> {
				kinds.add(SourceReader.ENUM_KIND_CENSUS);
				kinds.add(SourceReader.ENUM_KIND_TAX_ROLL);
				kinds.add(SourceReader.ENUM_KIND_DIRECTORY);
			}
			case ARCHIVE -> {}
		}
		return kinds;
	}

	private record KindKey(String personId, String kind){}

	private static String names(final List<Candidate> candidates){
		return Sentences.list(candidates.stream()
			.map(candidate -> I18N.t("planner.step." + candidate.step().key + ".label"))
			.distinct()
			.toList());
	}

	/**
	 * Recommends the kind of source that would serve most of the steps of the question and that no source cited for the individual is
	 * yet: it is where the records of the steps are written, not where the researcher happened to look.
	 */
	private void sourceKind(final GapData.Question question, final List<Candidate> retained, final String covered,
			final List<PlannerFinding> findings){
		final Map<KindKey, List<Candidate>> served = new LinkedHashMap<>();
		for(final Candidate candidate : retained)
			for(final String kind : kindsOf(candidate))
				if(!candidate.person().sourceKinds().contains(kind)
						&& !covered.contains(I18N.t("enum.source.kind." + kind, kind).toLowerCase(Locale.ROOT)))
					served.computeIfAbsent(new KindKey(candidate.person().id(), kind), k -> new ArrayList<>()).add(candidate);
		if(served.isEmpty())
			return;

		Map.Entry<KindKey, List<Candidate>> best = null;
		int bestUsefulness = 0;
		for(final Map.Entry<KindKey, List<Candidate>> entry : served.entrySet()){
			final int usefulness = Math.min(MAX_USEFULNESS, entry.getValue().stream().mapToInt(Candidate::usefulness).max().orElse(0)
				+ SHARED_KIND_BONUS * (entry.getValue().size() - 1));
			if(usefulness > bestUsefulness){
				best = entry;
				bestUsefulness = usefulness;
			}
		}

		final GapData.Person person = best.getValue().getFirst().person();
		final String name = person.name() + " [" + person.id() + "]";
		final String kind = I18N.t("enum.source.kind." + best.getKey().kind(), best.getKey().kind());
		final String cited = (person.sourceKinds().isEmpty()
			? I18N.tf("planner.source_kind.none_cited", name)
			: I18N.tf("planner.source_kind.cited", name, Sentences.list(person.sourceKinds().stream()
				.sorted()
				.map(k -> I18N.t("enum.source.kind." + k, k))
				.toList())));
		findings.add(recommendation("planner.source_kind", question, person, best.getKey().kind(), bestUsefulness,
			I18N.tf("planner.source_kind.title", kind, name),
			I18N.tf("planner.source_kind.explanation", questionTitle(question), kind, names(best.getValue()), cited),
			I18N.tf("planner.source_kind.task", kind, name)));
	}

	/**
	 * Recommends the repositories at the places of the relatives of an individual whose steps are open: families move, and the records of
	 * the place of a parent, a partner or a child may name the individual too.
	 */
	private void repositories(final GapData.Question question, final List<Candidate> retained, final List<PlannerFinding> findings){
		final Map<String, List<Candidate>> byPerson = new LinkedHashMap<>();
		for(final Candidate candidate : retained)
			if(candidate.step() != Step.ARCHIVE)
				byPerson.computeIfAbsent(candidate.person().id(), k -> new ArrayList<>()).add(candidate);

		for(final List<Candidate> candidates : byPerson.values()){
			final GapData.Person person = candidates.getFirst().person();
			final int usefulness = Math.max(1, candidates.stream().mapToInt(Candidate::usefulness).max().orElse(0) - LEAD_DISCOUNT);
			final String name = person.name() + " [" + person.id() + "]";
			for(final GapData.RepositoryLead lead : person.leads().subList(0, Math.min(MAX_LEADS_PER_PERSON, person.leads().size())))
				findings.add(recommendation("planner.repository", question, person, lead.repository(), usefulness,
					I18N.tf("planner.repository.title", lead.repository(), name),
					I18N.tf("planner.repository.explanation", questionTitle(question), name, lead.repository(), lead.relative(),
						I18N.t("planner.repository.relation." + lead.relation()), names(candidates)),
					I18N.tf("planner.repository.task", lead.repository(), name, lead.relative())));
		}
	}

	private PlannerFinding recommendation(final String kind, final GapData.Question question, final GapData.Person person,
			final String subject, final int usefulness, final String title, final String explanation, final String task){
		final String level = level(usefulness, tuning);
		final Diagnostic diagnostic = new Diagnostic(Diagnostic.Severity.INFORMATION, AssistantCategory.RESEARCH, title,
			Sentences.join(explanation, I18N.tf("gap.usefulness", I18N.t("gap.usefulness." + level), String.valueOf(usefulness))),
			List.of(question.id(), person.id()), null, DiagnosticKeys.of(kind, List.of(question.id(), person.id()), subject))
			.withRank(usefulness);
		return new PlannerFinding(diagnostic, new GapDraft(GapDraft.Kind.TASK, "", task, List.of(question.id()), priority(usefulness, tuning)));
	}

	private static String questionTitle(final GapData.Question question){
		return (question.title() != null && !question.title().isBlank()? question.title() + " [" + question.id() + "]": question.id());
	}


	/**
	 * The identity hypotheses that no conclusion evaluates yet: the ones with more confidence first, and those that an open question is
	 * about, or that would give parents to an individual that has none, before the others.
	 */
	private List<PlannerFinding> identityHypotheses(final GapData data){
		final Set<String> withParents = new HashSet<>();
		for(final GapData.ParentLink link : data.parents())
			withParents.add(link.childId());
		final Set<String> asked = new HashSet<>();
		for(final GapData.Question question : data.questions())
			if(isOpen(question))
				asked.addAll(question.targets());

		final List<PlannerFinding> findings = new ArrayList<>();
		for(final GapData.Hypothesis hypothesis : data.hypotheses()){
			final GapData.Person first = data.persons().get(hypothesis.firstId());
			final GapData.Person second = data.persons().get(hypothesis.secondId());
			if(hypothesis.evaluated() || first == null || second == null)
				continue;

			final String confidence = (hypothesis.confidence() != null? hypothesis.confidence(): ConfidenceReader.ENUM_LOW);
			int usefulness = switch(confidence){
				case ConfidenceReader.ENUM_HIGH -> HYPOTHESIS_HIGH;
				case ConfidenceReader.ENUM_MEDIUM -> HYPOTHESIS_MEDIUM;
				default -> HYPOTHESIS_LOW;
			};
			final String firstName = first.name() + " [" + first.id() + "]";
			final String secondName = second.name() + " [" + second.id() + "]";
			final List<String> reasons = new ArrayList<>();
			reasons.add(I18N.tf("planner.identity.reason.confidence", I18N.t("enum.confidence." + confidence)));
			if(asked.contains(first.id()) || asked.contains(second.id())){
				usefulness += HYPOTHESIS_QUESTION_BONUS;
				reasons.add(I18N.t("planner.identity.reason.question"));
			}
			final boolean firstHasParents = withParents.contains(first.id());
			if(firstHasParents != withParents.contains(second.id())){
				usefulness += HYPOTHESIS_PARENTS_BONUS;
				reasons.add(I18N.tf("planner.identity.reason.parents", (firstHasParents? secondName: firstName),
					(firstHasParents? firstName: secondName)));
			}
			usefulness = Math.min(MAX_USEFULNESS, usefulness);

			final String level = level(usefulness, tuning);
			final Diagnostic diagnostic = new Diagnostic(Diagnostic.Severity.INFORMATION, AssistantCategory.RESEARCH,
				I18N.tf("planner.identity.title", firstName, secondName),
				Sentences.join(I18N.tf("planner.identity.explanation", hypothesis.id(), firstName, secondName, Sentences.items(reasons)),
					I18N.tf("gap.usefulness", I18N.t("gap.usefulness." + level), String.valueOf(usefulness))),
				List.of(first.id(), second.id(), hypothesis.id()), null,
				DiagnosticKeys.of("planner.identity", List.of(first.id(), second.id(), hypothesis.id())))
				.withRank(usefulness);
			findings.add(new PlannerFinding(diagnostic, new GapDraft(GapDraft.Kind.QUESTION,
				I18N.tf("planner.identity.draft.title", first.name(), second.name()),
				I18N.tf("planner.identity.draft.question", first.name(), second.name()), List.of(first.id(), second.id()), null)));
		}
		return findings;
	}

	private List<PlannerFinding> brickWalls(final GapData data){
		final Map<String, List<String>> children = new HashMap<>();
		final Set<String> withParents = new HashSet<>();
		for(final GapData.ParentLink link : data.parents()){
			children.computeIfAbsent(link.parentId(), k -> new ArrayList<>()).add(link.childId());
			withParents.add(link.childId());
		}
		final Set<String> worked = new HashSet<>();
		for(final GapData.Question question : data.questions())
			if(isOpen(question) || ResearchQuestionReader.ENUM_STATUS_ON_HOLD.equals(question.status()))
				worked.addAll(question.targets());

		final List<PlannerFinding> findings = new ArrayList<>();
		for(final GapData.Person person : data.persons().values()){
			if(withParents.contains(person.id()) || worked.contains(person.id()))
				continue;

			final int descendants = descendants(person.id(), children);
			if(descendants == 0)
				continue;

			final int usefulness = Math.min(tuning.get(AssistantTuning.Parameter.PLANNER_BRICK_WALL) + 45,
				tuning.get(AssistantTuning.Parameter.PLANNER_BRICK_WALL) + Math.min(30, descendants * 3) + (person.birthYear() != null? 10: 0)
				+ (person.eventCount() > 0? 5: 0));
			final String year = (person.birthYear() != null? String.valueOf(person.birthYear()): StringUtils.EMPTY);
			final String level = level(usefulness, tuning);
			final Diagnostic diagnostic = new Diagnostic(Diagnostic.Severity.INFORMATION, AssistantCategory.RESEARCH,
				I18N.tf("planner.brick_wall.title", person.name() + " [" + person.id() + "]"),
				Sentences.join(I18N.tf((person.birthYear() != null? "planner.brick_wall.explanation.year": "planner.brick_wall.explanation"),
					person.name() + " [" + person.id() + "]", String.valueOf(descendants), year),
					I18N.tf("gap.usefulness", I18N.t("gap.usefulness." + level), String.valueOf(usefulness))),
				List.of(person.id()), null, DiagnosticKeys.of("planner.brick_wall", List.of(person.id()), descendants))
				.withRank(usefulness);
			findings.add(new PlannerFinding(diagnostic, new GapDraft(GapDraft.Kind.QUESTION,
				I18N.tf("planner.brick_wall.draft.title", person.name()),
				I18N.tf((person.birthYear() != null? "planner.brick_wall.draft.question.year": "planner.brick_wall.draft.question"),
					person.name(), year), List.of(person.id()), null)));
		}
		return findings;
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


}
