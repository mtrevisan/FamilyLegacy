package io.github.mtrevisan.familylegacy.ui.assistant.gap;

import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.assistant.chronology.DateRange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


class GapCheckerTest{

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 28);

	/** A data set built step by step. */
	private static final class Builder{

		private final Map<String, GapData.Person> persons = new LinkedHashMap<>();
		private final List<GapData.Event> events = new ArrayList<>();
		private final List<GapData.ParentLink> parents = new ArrayList<>();
		private final List<GapData.Union> unions = new ArrayList<>();
		private final List<GapData.Question> questions = new ArrayList<>();
		private final List<GapData.Activity> activities = new ArrayList<>();
		private final List<GapData.Task> tasks = new ArrayList<>();

		Builder person(final String id, final Integer birthYear, final boolean hasDeath){
			persons.put(id, new GapData.Person(id, "Person " + id, birthYear, true, hasDeath, false, false, 0));
			return this;
		}

		Builder person(final GapData.Person person){
			persons.put(person.id(), person);
			return this;
		}

		Builder event(final String id, final String type, final int sources, final String... principals){
			events.add(new GapData.Event(id, type, sources, List.of(principals)));
			return this;
		}

		Builder parent(final String child, final String parent){
			parents.add(new GapData.ParentLink(child, parent));
			return this;
		}

		Builder union(final String first, final String second){
			unions.add(new GapData.Union(first, second));
			return this;
		}

		Builder question(final String id, final String status, final boolean conclusion){
			questions.add(new GapData.Question(id, "Title of " + id, status, conclusion));
			return this;
		}

		Builder activity(final String id, final String status, final String result, final boolean followUp, final boolean parent,
				final String... questions){
			activities.add(new GapData.Activity(id, "action " + id, status, result, List.of(questions), followUp, parent));
			return this;
		}

		Builder task(final String id, final String status, final String priority, final String due, final String... questions){
			tasks.add(new GapData.Task(id, "do " + id, status, priority,
				(due != null? DateRange.day(CalendarType.GREGORIAN.parseToJdn(due)): null), List.of(questions)));
			return this;
		}

		List<Diagnostic> check(){
			return GapChecker.check(data());
		}

		List<GapFinding> findings(){
			return GapChecker.findings(data());
		}

		private GapData data(){
			return new GapData(TODAY, persons, events, parents, unions, questions, activities, tasks);
		}

	}

	private static List<String> titles(final List<Diagnostic> findings){
		return findings.stream()
			.map(Diagnostic::title)
			.toList();
	}

	private static Diagnostic only(final List<Diagnostic> findings){
		Assertions.assertEquals(1, findings.size(), titles(findings).toString());
		return findings.getFirst();
	}

	private static GapData.Person person(final String id, final boolean hasBirth, final boolean hasResidence, final boolean hasCensus,
			final int events){
		return new GapData.Person(id, "Person " + id, null, hasBirth, false, hasResidence, hasCensus, events);
	}


	@Test
	void aWellDocumentedResearchHasNoGaps(){
		Assertions.assertTrue(new Builder()
			.person("a", 1950, false)
			.person("b", 1850, true)
			.event("e1", "birth", 1, "b")
			.question("q", "resolved", true)
			.activity("act", "completed", "positive", false, false, "q")
			.task("t", "completed", "normal", "2020-01-01", "q")
			.check().isEmpty());
	}

	@Test
	void aCertainlyDeadIndividualWithoutADeath(){
		final GapFinding finding = new Builder().person("a", 1900, false).findings().getFirst();

		Assertions.assertEquals(AssistantCategory.RESEARCH, finding.diagnostic().category());
		Assertions.assertEquals(Diagnostic.Severity.INFORMATION, finding.diagnostic().severity());
		Assertions.assertTrue(finding.diagnostic().title().startsWith("No death recorded: Person a (born 1900)"));
		Assertions.assertEquals(85., finding.diagnostic().rank());
		Assertions.assertEquals(List.of("a"), finding.diagnostic().recordIds());
		Assertions.assertTrue(finding.diagnostic().explanation().contains("certainly dead"), finding.diagnostic().explanation());
		Assertions.assertTrue(finding.diagnostic().explanation().contains("Estimated usefulness: high (85 of 100)"));
		//the question to start: what to find out, about whom
		Assertions.assertEquals(GapDraft.Kind.QUESTION, finding.draft().kind());
		Assertions.assertEquals("Death of Person a", finding.draft().title());
		Assertions.assertEquals("When and where did Person a die?", finding.draft().text());
		Assertions.assertEquals(List.of("a"), finding.draft().records());
	}

	@Test
	void theAgeThatMakesADeathMissing(){
		//110 years or more: certainly dead; 90 or more: probably; the people alive today are not a gap
		Assertions.assertEquals(85., only(new Builder().person("a", 2026 - GapChecker.CERTAINLY_DEAD_YEARS, false).check()).rank());
		Assertions.assertEquals(55., only(new Builder().person("a", 2026 - GapChecker.CERTAINLY_DEAD_YEARS + 1, false).check()).rank());
		Assertions.assertEquals(55., only(new Builder().person("a", 2026 - GapChecker.PROBABLY_DEAD_YEARS, false).check()).rank());
		Assertions.assertTrue(new Builder().person("a", 2026 - GapChecker.PROBABLY_DEAD_YEARS + 1, false).check().isEmpty());
		Assertions.assertTrue(only(new Builder().person("a", 1930, false).check()).title().startsWith("Probably dead, no death recorded"));
	}

	@Test
	void fewDeathsAreAFindingEachManyAreOne(){
		final Builder few = new Builder();
		for(int i = 0; i < GapChecker.MAX_INDIVIDUAL_FINDINGS; i ++)
			few.person("a" + i, 1900, false);
		Assertions.assertEquals(GapChecker.MAX_INDIVIDUAL_FINDINGS, few.check().size());

		//one more than the limit: an imported tree full of ancestors without a death is not a list of hundreds of findings
		final Builder many = new Builder();
		for(int i = 0; i < GapChecker.MAX_INDIVIDUAL_FINDINGS + 1; i ++)
			many.person("a" + i, 1900, false);
		final GapFinding finding = many.findings().getFirst();
		Assertions.assertEquals(1, many.findings().size());
		Assertions.assertEquals((GapChecker.MAX_INDIVIDUAL_FINDINGS + 1) + " individuals with no death recorded",
			finding.diagnostic().title());
		Assertions.assertEquals(85., finding.diagnostic().rank());
		Assertions.assertEquals(GapChecker.MAX_INDIVIDUAL_FINDINGS + 1, finding.diagnostic().recordIds().size());
		Assertions.assertEquals(GapDraft.Kind.TASK, finding.draft().kind());
		Assertions.assertTrue(finding.draft().text().contains((GapChecker.MAX_INDIVIDUAL_FINDINGS + 1) + " individuals"));
	}

	@Test
	void theAggregateTakesTheUsefulnessOfTheMostUsefulAndIsCapped(){
		final Builder builder = new Builder();
		for(int i = 0; i < GapChecker.MAX_LISTED + 40; i ++)
			builder.person("a" + i, (i == 0? 1900: 1930), false);

		final Diagnostic finding = only(builder.check());

		//one of them is certainly dead (85), the others probably (55)
		Assertions.assertEquals(85., finding.rank());
		Assertions.assertEquals(GapChecker.MAX_LISTED, finding.recordIds().size());
		//the most useful comes first in the list
		Assertions.assertEquals("a0", finding.recordIds().getFirst());
	}

	@Test
	void manyBirthsAndManyCouplesAreOneFindingEach(){
		final Builder births = new Builder();
		for(int i = 0; i < GapChecker.MAX_INDIVIDUAL_FINDINGS + 5; i ++)
			births.person(person("p" + i, false, false, false, 1));
		Assertions.assertEquals((GapChecker.MAX_INDIVIDUAL_FINDINGS + 5) + " individuals with no birth recorded",
			only(births.check()).title());

		final Builder marriages = new Builder();
		for(int i = 0; i < GapChecker.MAX_INDIVIDUAL_FINDINGS + 5; i ++)
			marriages.person("a" + i, null, true).person("b" + i, null, true).person("c" + i, null, true)
				.parent("c" + i, "a" + i).parent("c" + i, "b" + i);
		Assertions.assertEquals((GapChecker.MAX_INDIVIDUAL_FINDINGS + 5) + " couples with children and no known marriage",
			only(marriages.check()).title());
	}

	@Test
	void aDeathOrNoBirthYearIsNotAGap(){
		Assertions.assertTrue(new Builder().person("a", 1850, true).check().isEmpty());
		Assertions.assertTrue(new Builder().person(new GapData.Person("a", "A", null, true, false, false, false, 0)).check().isEmpty());
	}

	@Test
	void anIndividualWithChildrenOrEventsButNoBirth(){
		final Diagnostic withChildren = only(new Builder()
			.person(person("p", false, false, false, 0)).person(person("c", true, false, false, 0)).parent("c", "p")
			.check());
		Assertions.assertTrue(withChildren.title().startsWith("No birth recorded: Person p"));
		Assertions.assertEquals(55., withChildren.rank());

		final Diagnostic withEvents = only(new Builder().person(person("p", false, false, false, 2)).check());
		Assertions.assertEquals(50., withEvents.rank());

		//nobody is asked about a person the research knows nothing of
		Assertions.assertTrue(new Builder().person(person("p", false, false, false, 0)).check().isEmpty());
		Assertions.assertTrue(new Builder().person(person("p", true, false, false, 3)).check().isEmpty());
	}

	@Test
	void childrenWithoutAKnownMarriage(){
		final GapFinding finding = new Builder()
			.person("a", null, true).person("b", null, true).person("c", null, true)
			.parent("c", "a").parent("c", "b")
			.findings().getFirst();

		Assertions.assertTrue(finding.diagnostic().title().startsWith("Children without a known marriage: Person a and Person b"));
		Assertions.assertEquals(List.of("a", "b", "c"), finding.diagnostic().recordIds());
		Assertions.assertEquals(50., finding.diagnostic().rank());
		Assertions.assertEquals("Were Person a and Person b married? When and where?", finding.draft().text());
		Assertions.assertEquals(List.of("a", "b"), finding.draft().records());
	}

	@Test
	void theMoreChildrenTheMoreUsefulTheMarriage(){
		final Builder builder = new Builder().person("a", null, true).person("b", null, true);
		for(int i = 0; i < 3; i ++)
			builder.person("c" + i, null, true).parent("c" + i, "a").parent("c" + i, "b");
		Assertions.assertEquals(60., only(builder.check()).rank());

		final Builder many = new Builder().person("a", null, true).person("b", null, true);
		for(int i = 0; i < 9; i ++)
			many.person("c" + i, null, true).parent("c" + i, "a").parent("c" + i, "b");
		Assertions.assertEquals(70., only(many.check()).rank());
	}

	@Test
	void aMarriageOrAUnionMakesTheChildrenFine(){
		final Builder base = new Builder().person("a", null, true).person("b", null, true).person("c", null, true)
			.parent("c", "a").parent("c", "b");

		Assertions.assertTrue(new Builder().person("a", null, true).person("b", null, true).person("c", null, true)
			.parent("c", "a").parent("c", "b").union("b", "a").check().isEmpty());
		Assertions.assertTrue(new Builder().person("a", null, true).person("b", null, true).person("c", null, true)
			.parent("c", "a").parent("c", "b").event("m", "marriage", 1, "b", "a").check().isEmpty());
		//a marriage of other people is no help
		Assertions.assertEquals(1, base.event("m", "marriage", 1, "a", "x").check().size());
	}

	@Test
	void onlyAChildWithTwoParentsIsAboutAMarriage(){
		Assertions.assertTrue(new Builder().person("a", null, true).person("c", null, true).parent("c", "a").check().isEmpty());
		Assertions.assertTrue(new Builder().person("a", null, true).person("b", null, true).person("d", null, true)
			.person("c", null, true).parent("c", "a").parent("c", "b").parent("c", "d").check().isEmpty());
	}

	@Test
	void theVitalEventsWithoutASourceAreOneFinding(){
		final GapFinding finding = new Builder()
			.event("e1", "birth", 0, "a").event("e2", "death", 0, "a").event("e3", "marriage", 0, "a", "b")
			.event("e4", "birth", 2, "c").event("e5", "census", 0, "a").event("e6", "burial", 0, "a")
			.findings().getFirst();

		Assertions.assertEquals("3 vital event(s) without a source", finding.diagnostic().title());
		Assertions.assertEquals(List.of("e1", "e2", "e3"), finding.diagnostic().recordIds());
		Assertions.assertEquals(70., finding.diagnostic().rank());
		//the task to start: to find them
		Assertions.assertEquals(GapDraft.Kind.TASK, finding.draft().kind());
		Assertions.assertTrue(finding.draft().text().contains("3 vital events"));
		Assertions.assertEquals("normal", finding.draft().priority());
	}

	@Test
	void theListOfTheUnsourcedEventsIsCapped(){
		final Builder builder = new Builder();
		for(int i = 0; i < GapChecker.MAX_LISTED + 30; i ++)
			builder.event("e" + i, "birth", 0, "a");

		final Diagnostic finding = only(builder.check());

		Assertions.assertEquals(GapChecker.MAX_LISTED, finding.recordIds().size());
		Assertions.assertTrue(finding.title().startsWith((GapChecker.MAX_LISTED + 30) + " vital event(s)"));
	}

	@Test
	void theSourcedOrOtherEventsAreNotGaps(){
		Assertions.assertTrue(new Builder().event("e1", "birth", 1, "a").event("e2", "census", 0, "a").check().isEmpty());
	}

	@Test
	void thePeopleOfACensusWithoutAResidence(){
		final GapFinding finding = new Builder()
			.person(person("a", true, false, true, 1)).person(person("b", true, true, true, 1)).person(person("c", true, false, false, 1))
			.findings().getFirst();

		Assertions.assertEquals("1 individual(s) in a census without a residence", finding.diagnostic().title());
		Assertions.assertEquals(List.of("a"), finding.diagnostic().recordIds());
		Assertions.assertEquals(40., finding.diagnostic().rank());
		Assertions.assertEquals("low", finding.draft().priority());
	}

	@Test
	void anOpenQuestionWithNoActivity(){
		final GapFinding finding = new Builder().question("q", "open", false).findings().getFirst();

		Assertions.assertEquals("Research question with no activity: Title of q", finding.diagnostic().title());
		Assertions.assertEquals(75., finding.diagnostic().rank());
		Assertions.assertEquals(GapDraft.Kind.TASK, finding.draft().kind());
		Assertions.assertEquals(List.of("q"), finding.draft().records());
		Assertions.assertTrue(finding.draft().text().contains("Title of q"));

		//a question on hold is less urgent, a closed one is not a gap
		Assertions.assertEquals(45., only(new Builder().question("q", "on_hold", false).check()).rank());
		Assertions.assertTrue(new Builder().question("q", "resolved", false).check().isEmpty());
		Assertions.assertTrue(new Builder().question("q", "disproven", false).check().isEmpty());
		Assertions.assertTrue(new Builder().question("q", "closed", false).check().isEmpty());
	}

	@Test
	void aQuestionThatIsWorkedOnIsNotWithoutActivity(){
		Assertions.assertTrue(new Builder().question("q", "open", false)
			.activity("a", "in_progress", null, false, false, "q").check().isEmpty());
	}

	@Test
	void aQuestionWhoseActivitiesAreAllOverIsStalled(){
		final Diagnostic finding = only(new Builder().question("q", "open", false)
			.activity("a1", "completed", "positive", false, false, "q").activity("a2", "abandoned", null, false, false, "q")
			.check());

		Assertions.assertTrue(finding.title().startsWith("Research question with nothing left to do: Title of q"));
		Assertions.assertEquals(65., finding.rank());
	}

	@Test
	void aQuestionThatIsNotStalled(){
		final Builder base = new Builder().question("q", "open", false).activity("a1", "completed", "positive", false, false, "q");

		//a task is planned
		Assertions.assertTrue(base.task("t", "open", "normal", null, "q").check().isEmpty());
		//a conclusion
		Assertions.assertTrue(new Builder().question("q", "open", true).activity("a1", "completed", "positive", false, false, "q")
			.check().isEmpty());
		//an activity planned
		Assertions.assertTrue(new Builder().question("q", "open", false).activity("a1", "completed", "positive", false, false, "q")
			.activity("a2", "planned", null, false, false, "q").check().isEmpty());
		//on hold it is not asked to move
		Assertions.assertTrue(new Builder().question("q", "on_hold", false).activity("a1", "completed", "positive", false, false, "q")
			.check().isEmpty());
		//a task that is over does not count as planned
		Assertions.assertEquals(1, new Builder().question("q", "open", false).activity("a1", "completed", "positive", false, false, "q")
			.task("t", "completed", "normal", null, "q").check().size());
	}

	@Test
	void aSearchThatFoundNothingAndLedToNothing(){
		final GapFinding finding = new Builder().question("q", "open", false)
			.activity("a", "completed", "negative", false, false, "q").question("q2", "resolved", true)
			.activity("a2", "completed", "positive", false, false, "q2")
			.findings().stream().filter(f -> f.diagnostic().title().startsWith("Search without a follow-up")).findFirst().orElseThrow();

		Assertions.assertTrue(finding.diagnostic().title().contains("action a"));
		Assertions.assertEquals(List.of("a", "q"), finding.diagnostic().recordIds());
		Assertions.assertEquals(50., finding.diagnostic().rank());
		Assertions.assertEquals(GapDraft.Kind.TASK, finding.draft().kind());
		Assertions.assertEquals(List.of("q"), finding.draft().records());
		Assertions.assertTrue(finding.draft().text().contains("negative"));
	}

	@Test
	void aSearchThatHasAFollowUpOrNoLoss(){
		for(final String result : List.of("negative", "inconclusive", "unavailable"))
			Assertions.assertTrue(new Builder().question("q", "open", false).task("t", "open", "normal", null, "q")
				.activity("a", "completed", result, false, false, "q").check().stream()
				.anyMatch(f -> f.title().startsWith("Search without a follow-up")), result);

		//a follow-up task, an activity that originated from it, a positive result, a question that is closed: nothing is missing
		final String followUp = "Search without a follow-up";
		Assertions.assertTrue(new Builder().question("q", "open", false).activity("a", "completed", "negative", true, false, "q")
			.check().stream().noneMatch(f -> f.title().startsWith(followUp)));
		Assertions.assertTrue(new Builder().question("q", "open", false).activity("a", "completed", "negative", false, true, "q")
			.check().stream().noneMatch(f -> f.title().startsWith(followUp)));
		Assertions.assertTrue(new Builder().question("q", "open", false).activity("a", "completed", "positive", false, false, "q")
			.check().stream().noneMatch(f -> f.title().startsWith(followUp)));
		Assertions.assertTrue(new Builder().question("q", "resolved", true).activity("a", "completed", "negative", false, false, "q")
			.check().stream().noneMatch(f -> f.title().startsWith(followUp)));
		//a search of no question in particular is still a search
		Assertions.assertTrue(new Builder().activity("a", "completed", "negative", false, false)
			.check().stream().anyMatch(f -> f.title().startsWith(followUp)));
	}

	@Test
	void aTaskPastItsDueDate(){
		final GapFinding finding = new Builder().question("q", "resolved", true)
			.task("t", "open", "high", "2026-10-27", "q").findings().getFirst();

		Assertions.assertEquals(Diagnostic.Severity.WARNING, finding.diagnostic().severity());
		Assertions.assertEquals("Overdue task: do t", finding.diagnostic().title());
		Assertions.assertEquals(85., finding.diagnostic().rank());
		Assertions.assertTrue(finding.diagnostic().explanation().contains("1 day(s) ago"));
		Assertions.assertEquals(List.of("t", "q"), finding.diagnostic().recordIds());
		//the task is there already: there is no draft to start
		Assertions.assertNull(finding.draft());
		Assertions.assertEquals(75., only(new Builder().task("t", "in_progress", "normal", "2020-01-01").check()).rank());
		Assertions.assertEquals(70., only(new Builder().task("t", "open", "low", "2020-01-01").check()).rank());
		Assertions.assertEquals(70., only(new Builder().task("t", "open", null, "2020-01-01").check()).rank());
	}

	@Test
	void aTaskThatIsNotOverdue(){
		//due today is not overdue, a task that is done or has no date is not either
		Assertions.assertTrue(new Builder().task("t", "open", "high", "2026-10-28").check().isEmpty());
		Assertions.assertTrue(new Builder().task("t", "open", "high", "2027-01-01").check().isEmpty());
		Assertions.assertTrue(new Builder().task("t", "completed", "high", "2020-01-01").check().isEmpty());
		Assertions.assertTrue(new Builder().task("t", "abandoned", "high", "2020-01-01").check().isEmpty());
		Assertions.assertTrue(new Builder().task("t", "open", "high", null).check().isEmpty());
	}

	@Test
	void theCompletedActivitiesWithoutAResultAreOneFinding(){
		final Diagnostic finding = only(new Builder()
			.activity("a1", "completed", null, false, false).activity("a2", "completed", null, false, false)
			.activity("a3", "planned", null, false, false).activity("a4", "completed", "positive", false, false)
			.check());

		Assertions.assertEquals("2 completed activity(ies) without a result", finding.title());
		Assertions.assertEquals(List.of("a1", "a2"), finding.recordIds());
		Assertions.assertEquals(25., finding.rank());
	}

	@Test
	void theUsefulnessHasALevel(){
		Assertions.assertEquals("high", GapChecker.level(85));
		Assertions.assertEquals("high", GapChecker.level(GapChecker.HIGH_USEFULNESS));
		Assertions.assertEquals("medium", GapChecker.level(GapChecker.HIGH_USEFULNESS - 1));
		Assertions.assertEquals("medium", GapChecker.level(GapChecker.MEDIUM_USEFULNESS));
		Assertions.assertEquals("low", GapChecker.level(GapChecker.MEDIUM_USEFULNESS - 1));
		Assertions.assertTrue(only(new Builder().activity("a", "completed", null, false, false).check()).explanation()
			.contains("Estimated usefulness: low (25 of 100)"));
	}

	@Test
	void everyFindingHasAKeyAndTheyAreDistinct(){
		final List<Diagnostic> findings = new Builder()
			.person("a", 1900, false).person(person("p", false, false, true, 2)).person("b", null, true).person("c", null, true)
			.parent("c", "a").parent("c", "b").parent("a", "p")
			.event("e", "birth", 0, "a")
			.question("q", "open", false).question("q2", "open", false).activity("x", "completed", "negative", false, false, "q2")
			.activity("y", "completed", null, false, false)
			.task("t", "open", "high", "2020-01-01", "q")
			.check();

		Assertions.assertTrue(findings.size() >= 8, titles(findings).toString());
		Assertions.assertTrue(findings.stream().allMatch(Diagnostic::isIgnorable));
		Assertions.assertEquals(findings.size(), findings.stream().map(Diagnostic::key).distinct().count());
		Assertions.assertTrue(findings.stream().allMatch(f -> f.rank() > 0));
	}

	@Test
	void theKeyFollowsTheDataNotTheDay(){
		final String key = only(new Builder().person("a", 1900, false).check()).key();

		Assertions.assertEquals(key, only(new Builder().person("a", 1900, false).check()).key());
		//another person, or another birth year: another finding
		Assertions.assertNotEquals(key, only(new Builder().person("b", 1900, false).check()).key());
		Assertions.assertNotEquals(key, only(new Builder().person("a", 1899, false).check()).key());
	}

}
