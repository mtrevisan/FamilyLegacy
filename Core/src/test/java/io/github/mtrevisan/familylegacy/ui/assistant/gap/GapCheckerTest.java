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

import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ConfidenceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchActivityReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateRange;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
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
			.event("e1", EventReader.ENUM_TYPE_BIRTH, 1, "b")
			.question("q", ResearchQuestionReader.ENUM_STATUS_RESOLVED, true)
			.activity("act", ResearchTaskReader.ENUM_STATUS_COMPLETED, ResearchActivityReader.ENUM_RESULT_POSITIVE, false, false, "q")
			.task("t", ResearchTaskReader.ENUM_STATUS_COMPLETED, ResearchTaskReader.ENUM_PRIORITY_NORMAL, "2020-01-01", "q")
			.check().isEmpty());
	}

	@Test
	void aCertainlyDeadIndividualWithoutADeath(){
		final GapFinding finding = new Builder().person("a", 1900, false).findings().getFirst();

		Assertions.assertEquals(AssistantCategory.RESEARCH, finding.diagnostic().category());
		Assertions.assertEquals(Diagnostic.Severity.INFORMATION, finding.diagnostic().severity());
		Assertions.assertTrue(finding.diagnostic().title().startsWith("No death recorded: Person a [a] (born 1900)"));
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
	void everyDeathBirthAndCoupleIsAFindingNoneIsSummedUp(){
		final Builder deaths = new Builder();
		for(int i = 0; i < 40; i ++)
			deaths.person("a" + i, (i == 0? 1900: 1930), false);
		final List<GapFinding> found = deaths.findings();
		Assertions.assertEquals(40, found.size());
		//one finding for each individual, the certainly dead first by usefulness, each with its own question
		Assertions.assertTrue(found.stream().allMatch(f -> f.diagnostic().recordIds().size() == 1 && f.draft().kind() == GapDraft.Kind.QUESTION));
		Assertions.assertEquals(85., found.stream().mapToDouble(f -> f.diagnostic().rank()).max().orElse(0));

		final Builder births = new Builder();
		for(int i = 0; i < 30; i ++)
			births.person(person("p" + i, false, false, false, 1));
		Assertions.assertEquals(30, births.check().size());

		final Builder marriages = new Builder();
		for(int i = 0; i < 30; i ++)
			marriages.person("a" + i, null, true).person("b" + i, null, true).person("c" + i, null, true)
				.parent("c" + i, "a" + i).parent("c" + i, "b" + i);
		Assertions.assertEquals(30, marriages.check().size());
	}

	@Test
	void aDeathOrNoBirthYearIsNotAGap(){
		Assertions.assertTrue(new Builder().person("a", 1850, true).check().isEmpty());
		Assertions.assertTrue(new Builder().person(new GapData.Person("a", "A", null, true, false, false, false, 0)).check().isEmpty());
	}

	/** An individual with a birth event, so that only the death is in question; {@code null} years are unknown. */
	private static GapData.Person dated(final String id, final Integer birthYear, final boolean hasDeath, final Integer deathYear){
		return new GapData.Person(id, "Person " + id, birthYear, true, hasDeath, false, false, 0, List.of(), deathYear);
	}

	@Test
	void aChildBornLongAgoMakesTheParentCertainlyDead(){
		final Diagnostic finding = only(new Builder()
			.person(dated("p", null, false, null)).person(dated("c", 1900, true, null)).parent("c", "p")
			.check());

		Assertions.assertTrue(finding.title().startsWith("No death recorded: Person p [p] (born 1900 at the latest, from relatives)"),
			finding.title());
		Assertions.assertEquals(85., finding.rank());
		Assertions.assertTrue(finding.explanation().contains("certainly dead"), finding.explanation());
	}

	@Test
	void theDeathOfAChildOrOfAParentBoundsTheBirth(){
		//the child died in 1905: the parent was born in 1905 at the latest
		final Diagnostic childDied = only(new Builder()
			.person(dated("p", null, false, null)).person(dated("c", null, true, 1905)).parent("c", "p")
			.check());
		Assertions.assertTrue(childDied.title().contains("born 1905 at the latest"), childDied.title());

		//the parent died in 1900: the child (posthumous, maybe) was born in 1901 at the latest
		final Diagnostic parentDied = only(new Builder()
			.person(dated("c", null, false, null)).person(dated("p", null, true, 1900)).parent("c", "p")
			.check());
		Assertions.assertTrue(parentDied.title().contains("born 1901 at the latest"), parentDied.title());
		Assertions.assertEquals(85., parentDied.rank());
	}

	@Test
	void theBirthIsDeducedFromTheDescendantsToo(){
		final Diagnostic finding = only(new Builder()
			.person(dated("g", null, false, null)).person(dated("p", null, true, null)).person(dated("c", 1930, true, null))
			.parent("p", "g").parent("c", "p")
			.check());

		Assertions.assertTrue(finding.title().startsWith("Probably dead, no death recorded: Person g [g] (born 1930 at the latest, from relatives)"),
			finding.title());
		Assertions.assertEquals(55., finding.rank());
	}

	@Test
	void theBirthIsEstimatedFromTheParentsOnlyAsProbable(){
		//a parent born in 1890 puts the birth around 1915: 111 years ago, but only an estimate, so probably dead
		final Diagnostic finding = only(new Builder()
			.person(dated("c", null, false, null)).person(dated("p", 1890, true, null)).parent("c", "p")
			.check());

		Assertions.assertTrue(finding.title().startsWith("Probably dead, no death recorded: Person c [c] (born around 1915, from the parents)"),
			finding.title());
		Assertions.assertEquals(55., finding.rank());
	}

	@Test
	void youngRelativesSayNothingAboutTheDeath(){
		Assertions.assertTrue(new Builder()
			.person(dated("p", null, false, null)).person(dated("c", 2000, false, null)).parent("c", "p")
			.check().isEmpty());
		Assertions.assertTrue(new Builder()
			.person(dated("c", null, false, null)).person(dated("p", 1950, false, null)).parent("c", "p")
			.check().isEmpty());
	}

	@Test
	void anIndividualBornInAnImpossibleLoopDoesNotHangTheCheck(){
		Assertions.assertTrue(new Builder()
			.person(dated("a", null, false, null)).person(dated("b", null, false, null)).parent("a", "b").parent("b", "a")
			.check().isEmpty());
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
	void aKnownPlaceWithoutTheBirthPointsToItsRegisters(){
		final GapData.Person person = new GapData.Person("p", "Person p", null, false, false, false, false, 2,
			List.of("Treviso", "Venezia"));

		final GapFinding finding = new Builder().person(person).findings().getFirst();

		Assertions.assertTrue(finding.diagnostic().title().startsWith("Place known, birth record missing: Person p"),
			finding.diagnostic().title());
		Assertions.assertTrue(finding.diagnostic().explanation().contains("Treviso, Venezia"));
		//5 more than the same gap without a place: the registers give somewhere to start
		Assertions.assertEquals(55., finding.diagnostic().rank());
		Assertions.assertTrue(finding.draft().text().contains("Treviso, Venezia"));
		Assertions.assertEquals(GapDraft.Kind.QUESTION, finding.draft().kind());

		//only three places are named
		final GapFinding many = new Builder().person(new GapData.Person("p", "Person p", null, false, false, false, false, 4,
			List.of("A", "B", "C", "D"))).findings().getFirst();
		Assertions.assertTrue(many.draft().text().contains("A, B, C"));
		Assertions.assertFalse(many.draft().text().contains("D"));
	}

	@Test
	void childrenWithoutAKnownMarriage(){
		final GapFinding finding = new Builder()
			.person("a", null, true).person("b", null, true).person("c", null, true)
			.parent("c", "a").parent("c", "b")
			.findings().getFirst();

		Assertions.assertTrue(finding.diagnostic().title().startsWith("Children without a known marriage: Person a [a] and Person b [b]"));
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
			.parent("c", "a").parent("c", "b").event("m", EventReader.ENUM_TYPE_MARRIAGE, 1, "b", "a").check().isEmpty());
		//a marriage of other people is no help
		Assertions.assertEquals(1, base.event("m", EventReader.ENUM_TYPE_MARRIAGE, 1, "a", "x").check().size());
	}

	@Test
	void onlyAChildWithTwoParentsIsAboutAMarriage(){
		Assertions.assertTrue(new Builder().person("a", null, true).person("c", null, true).parent("c", "a").check().isEmpty());
		Assertions.assertTrue(new Builder().person("a", null, true).person("b", null, true).person("d", null, true)
			.person("c", null, true).parent("c", "a").parent("c", "b").parent("c", "d").check().isEmpty());
	}

	@Test
	void theVitalEventsWithoutASourceAreAFindingEach(){
		final List<GapFinding> found = new Builder()
			.event("e1", EventReader.ENUM_TYPE_BIRTH, 0, "a").event("e2", EventReader.ENUM_TYPE_DEATH, 0, "a").event("e3", EventReader.ENUM_TYPE_MARRIAGE, 0, "a", "b")
			.event("e4", EventReader.ENUM_TYPE_BIRTH, 2, "c").event("e5", EventReader.ENUM_TYPE_CENSUS, 0, "a").event("e6", EventReader.ENUM_TYPE_BURIAL, 0, "a")
			.findings();

		Assertions.assertEquals(3, found.size(), found.toString());
		final GapFinding finding = found.getFirst();
		Assertions.assertTrue(finding.diagnostic().title().startsWith("Vital event without a source: "), finding.diagnostic().title());
		Assertions.assertEquals("e1", finding.diagnostic().recordIds().getFirst());
		Assertions.assertTrue(finding.diagnostic().recordIds().contains("a"));
		//the marriage is about both
		Assertions.assertTrue(found.get(2).diagnostic().recordIds().containsAll(List.of("e3", "a", "b")));
		Assertions.assertEquals(70., finding.diagnostic().rank());
		//the task to start: to find a source for it
		Assertions.assertEquals(GapDraft.Kind.TASK, finding.draft().kind());
		Assertions.assertEquals(ResearchTaskReader.ENUM_PRIORITY_NORMAL, finding.draft().priority());
	}

	@Test
	void everyUnsourcedEventIsListedNoneIsSummedUp(){
		final Builder builder = new Builder();
		for(int i = 0; i < 230; i ++)
			builder.event("e" + i, EventReader.ENUM_TYPE_BIRTH, 0, "a");

		Assertions.assertEquals(230, builder.check().size());
	}

	@Test
	void theSourcedOrOtherEventsAreNotGaps(){
		Assertions.assertTrue(new Builder().event("e1", EventReader.ENUM_TYPE_BIRTH, 1, "a").event("e2", EventReader.ENUM_TYPE_CENSUS, 0, "a").check().isEmpty());
	}

	@Test
	void thePeopleOfACensusWithoutAResidence(){
		final GapFinding finding = new Builder()
			.person(person("a", true, false, true, 1)).person(person("b", true, true, true, 1)).person(person("c", true, false, false, 1))
			.findings().getFirst();

		Assertions.assertTrue(finding.diagnostic().title().startsWith("In a census without a residence: "), finding.diagnostic().title());
		Assertions.assertEquals(List.of("a"), finding.diagnostic().recordIds());
		Assertions.assertEquals(40., finding.diagnostic().rank());
		Assertions.assertEquals(ConfidenceReader.ENUM_LOW, finding.draft().priority());
	}

	@Test
	void anOpenQuestionWithNoActivity(){
		final GapFinding finding = new Builder().question("q", ResearchQuestionReader.ENUM_STATUS_OPEN, false).findings().getFirst();

		Assertions.assertEquals("Research question with no activity: Title of q [q]", finding.diagnostic().title());
		Assertions.assertEquals(75., finding.diagnostic().rank());
		Assertions.assertEquals(GapDraft.Kind.TASK, finding.draft().kind());
		Assertions.assertEquals(List.of("q"), finding.draft().records());
		Assertions.assertTrue(finding.draft().text().contains("Title of q"));

		//a question on hold is less urgent, a closed one is not a gap
		Assertions.assertEquals(45., only(new Builder().question("q", ResearchQuestionReader.ENUM_STATUS_ON_HOLD, false).check()).rank());
		Assertions.assertTrue(new Builder().question("q", ResearchQuestionReader.ENUM_STATUS_RESOLVED, false).check().isEmpty());
		Assertions.assertTrue(new Builder().question("q", ConclusionReader.ENUM_PROOF_STATUS_DISPROVEN, false).check().isEmpty());
		Assertions.assertTrue(new Builder().question("q", ResearchTaskReader.ENUM_STATUS_ABANDONED, false).check().isEmpty());
	}

	@Test
	void aQuestionThatIsWorkedOnIsNotWithoutActivity(){
		Assertions.assertTrue(new Builder().question("q", ResearchQuestionReader.ENUM_STATUS_OPEN, false)
			.activity("a", ResearchActivityReader.ENUM_STATUS_IN_PROGRESS, null, false, false, "q").check().isEmpty());
	}

	@Test
	void aQuestionWhoseActivitiesAreAllOverIsStalled(){
		final Diagnostic finding = only(new Builder().question("q", ResearchQuestionReader.ENUM_STATUS_OPEN, false)
			.activity("a1", ResearchTaskReader.ENUM_STATUS_COMPLETED, ResearchActivityReader.ENUM_RESULT_POSITIVE, false, false, "q").activity("a2", ResearchTaskReader.ENUM_STATUS_ABANDONED, null, false, false, "q")
			.check());

		Assertions.assertTrue(finding.title().startsWith("Research question with nothing left to do: Title of q"));
		Assertions.assertEquals(65., finding.rank());
	}

	@Test
	void aQuestionThatIsNotStalled(){
		final Builder base = new Builder().question("q", ResearchQuestionReader.ENUM_STATUS_OPEN, false).activity("a1", ResearchTaskReader.ENUM_STATUS_COMPLETED, ResearchActivityReader.ENUM_RESULT_POSITIVE, false, false, "q");

		//a task is planned
		Assertions.assertTrue(base.task("t", ResearchQuestionReader.ENUM_STATUS_OPEN, ResearchTaskReader.ENUM_PRIORITY_NORMAL, null, "q").check().isEmpty());
		//a conclusion
		Assertions.assertTrue(new Builder().question("q", ResearchQuestionReader.ENUM_STATUS_OPEN, true).activity("a1", ResearchTaskReader.ENUM_STATUS_COMPLETED, ResearchActivityReader.ENUM_RESULT_POSITIVE, false, false, "q")
			.check().isEmpty());
		//an activity planned
		Assertions.assertTrue(new Builder().question("q", ResearchQuestionReader.ENUM_STATUS_OPEN, false).activity("a1", ResearchTaskReader.ENUM_STATUS_COMPLETED, ResearchActivityReader.ENUM_RESULT_POSITIVE, false, false, "q")
			.activity("a2", ResearchActivityReader.ENUM_STATUS_PLANNED, null, false, false, "q").check().isEmpty());
		//on hold it is not asked to move
		Assertions.assertTrue(new Builder().question("q", ResearchQuestionReader.ENUM_STATUS_ON_HOLD, false).activity("a1", ResearchTaskReader.ENUM_STATUS_COMPLETED, ResearchActivityReader.ENUM_RESULT_POSITIVE, false, false, "q")
			.check().isEmpty());
		//a task that is over does not count as planned
		Assertions.assertEquals(1, new Builder().question("q", ResearchQuestionReader.ENUM_STATUS_OPEN, false).activity("a1", ResearchTaskReader.ENUM_STATUS_COMPLETED, ResearchActivityReader.ENUM_RESULT_POSITIVE, false, false, "q")
			.task("t", ResearchTaskReader.ENUM_STATUS_COMPLETED, ResearchTaskReader.ENUM_PRIORITY_NORMAL, null, "q").check().size());
	}

	@Test
	void aSearchThatFoundNothingAndLedToNothing(){
		final GapFinding finding = new Builder().question("q", ResearchQuestionReader.ENUM_STATUS_OPEN, false)
			.activity("a", ResearchTaskReader.ENUM_STATUS_COMPLETED, ResearchActivityReader.ENUM_RESULT_NEGATIVE, false, false, "q").question("q2", ResearchQuestionReader.ENUM_STATUS_RESOLVED, true)
			.activity("a2", ResearchTaskReader.ENUM_STATUS_COMPLETED, ResearchActivityReader.ENUM_RESULT_POSITIVE, false, false, "q2")
			.findings().stream().filter(f -> f.diagnostic().title().startsWith("Search without a follow-up")).findFirst().orElseThrow();

		Assertions.assertTrue(finding.diagnostic().title().contains("action a"));
		Assertions.assertEquals(List.of("a", "q"), finding.diagnostic().recordIds());
		Assertions.assertEquals(50., finding.diagnostic().rank());
		Assertions.assertEquals(GapDraft.Kind.TASK, finding.draft().kind());
		Assertions.assertEquals(List.of("q"), finding.draft().records());
		Assertions.assertTrue(finding.draft().text().contains(ResearchActivityReader.ENUM_RESULT_NEGATIVE));
	}

	@Test
	void aSearchThatHasAFollowUpOrNoLoss(){
		for(final String result : List.of(ResearchActivityReader.ENUM_RESULT_NEGATIVE, ResearchActivityReader.ENUM_RESULT_INCONCLUSIVE, ResearchActivityReader.ENUM_RESULT_UNAVAILABLE))
			Assertions.assertTrue(new Builder().question("q", ResearchQuestionReader.ENUM_STATUS_OPEN, false).task("t", ResearchQuestionReader.ENUM_STATUS_OPEN, ResearchTaskReader.ENUM_PRIORITY_NORMAL, null, "q")
				.activity("a", ResearchTaskReader.ENUM_STATUS_COMPLETED, result, false, false, "q").check().stream()
				.anyMatch(f -> f.title().startsWith("Search without a follow-up")), result);

		//a follow-up task, an activity that originated from it, a positive result, a question that is closed: nothing is missing
		final String followUp = "Search without a follow-up";
		Assertions.assertTrue(new Builder().question("q", ResearchQuestionReader.ENUM_STATUS_OPEN, false).activity("a", ResearchTaskReader.ENUM_STATUS_COMPLETED, ResearchActivityReader.ENUM_RESULT_NEGATIVE, true, false, "q")
			.check().stream().noneMatch(f -> f.title().startsWith(followUp)));
		Assertions.assertTrue(new Builder().question("q", ResearchQuestionReader.ENUM_STATUS_OPEN, false).activity("a", ResearchTaskReader.ENUM_STATUS_COMPLETED, ResearchActivityReader.ENUM_RESULT_NEGATIVE, false, true, "q")
			.check().stream().noneMatch(f -> f.title().startsWith(followUp)));
		Assertions.assertTrue(new Builder().question("q", ResearchQuestionReader.ENUM_STATUS_OPEN, false).activity("a", ResearchTaskReader.ENUM_STATUS_COMPLETED, ResearchActivityReader.ENUM_RESULT_POSITIVE, false, false, "q")
			.check().stream().noneMatch(f -> f.title().startsWith(followUp)));
		Assertions.assertTrue(new Builder().question("q", ResearchQuestionReader.ENUM_STATUS_RESOLVED, true).activity("a", ResearchTaskReader.ENUM_STATUS_COMPLETED, ResearchActivityReader.ENUM_RESULT_NEGATIVE, false, false, "q")
			.check().stream().noneMatch(f -> f.title().startsWith(followUp)));
		//a search of no question in particular is still a search
		Assertions.assertTrue(new Builder().activity("a", ResearchTaskReader.ENUM_STATUS_COMPLETED, ResearchActivityReader.ENUM_RESULT_NEGATIVE, false, false)
			.check().stream().anyMatch(f -> f.title().startsWith(followUp)));
	}

	@Test
	void aTaskPastItsDueDate(){
		final GapFinding finding = new Builder().question("q", ResearchQuestionReader.ENUM_STATUS_RESOLVED, true)
			.task("t", ResearchQuestionReader.ENUM_STATUS_OPEN, ConfidenceReader.ENUM_HIGH, "2026-10-27", "q").findings().getFirst();

		Assertions.assertEquals(Diagnostic.Severity.WARNING, finding.diagnostic().severity());
		Assertions.assertEquals("Overdue task: do t [t]", finding.diagnostic().title());
		Assertions.assertEquals(85., finding.diagnostic().rank());
		Assertions.assertTrue(finding.diagnostic().explanation().contains("1 day(s) ago"));
		Assertions.assertEquals(List.of("t", "q"), finding.diagnostic().recordIds());
		//the task is there already: there is no draft to start
		Assertions.assertNull(finding.draft());
		Assertions.assertEquals(75., only(new Builder().task("t", ResearchActivityReader.ENUM_STATUS_IN_PROGRESS, ResearchTaskReader.ENUM_PRIORITY_NORMAL, "2020-01-01").check()).rank());
		Assertions.assertEquals(70., only(new Builder().task("t", ResearchQuestionReader.ENUM_STATUS_OPEN, ConfidenceReader.ENUM_LOW, "2020-01-01").check()).rank());
		Assertions.assertEquals(70., only(new Builder().task("t", ResearchQuestionReader.ENUM_STATUS_OPEN, null, "2020-01-01").check()).rank());
	}

	@Test
	void aTaskThatIsNotOverdue(){
		//due today is not overdue, a task that is done or has no date is not either
		Assertions.assertTrue(new Builder().task("t", ResearchQuestionReader.ENUM_STATUS_OPEN, ConfidenceReader.ENUM_HIGH, "2026-10-28").check().isEmpty());
		Assertions.assertTrue(new Builder().task("t", ResearchQuestionReader.ENUM_STATUS_OPEN, ConfidenceReader.ENUM_HIGH, "2027-01-01").check().isEmpty());
		Assertions.assertTrue(new Builder().task("t", ResearchTaskReader.ENUM_STATUS_COMPLETED, ConfidenceReader.ENUM_HIGH, "2020-01-01").check().isEmpty());
		Assertions.assertTrue(new Builder().task("t", ResearchTaskReader.ENUM_STATUS_ABANDONED, ConfidenceReader.ENUM_HIGH, "2020-01-01").check().isEmpty());
		Assertions.assertTrue(new Builder().task("t", ResearchQuestionReader.ENUM_STATUS_OPEN, ConfidenceReader.ENUM_HIGH, null).check().isEmpty());
	}

	@Test
	void theCompletedActivitiesWithoutAResultAreAFindingEach(){
		final List<Diagnostic> found = new Builder()
			.activity("a1", ResearchTaskReader.ENUM_STATUS_COMPLETED, null, false, false).activity("a2", ResearchTaskReader.ENUM_STATUS_COMPLETED, null, false, false)
			.activity("a3", ResearchActivityReader.ENUM_STATUS_PLANNED, null, false, false).activity("a4", ResearchTaskReader.ENUM_STATUS_COMPLETED, ResearchActivityReader.ENUM_RESULT_POSITIVE, false, false)
			.check();

		Assertions.assertEquals(2, found.size(), found.toString());
		Assertions.assertTrue(found.getFirst().title().startsWith("Completed activity without a result: "), found.getFirst().title());
		Assertions.assertEquals(List.of("a1"), found.get(0).recordIds());
		Assertions.assertEquals(List.of("a2"), found.get(1).recordIds());
		Assertions.assertEquals(25., found.getFirst().rank());
	}

	@Test
	void theUsefulnessHasALevel(){
		Assertions.assertEquals(ConfidenceReader.ENUM_HIGH, GapChecker.level(85));
		Assertions.assertEquals(ConfidenceReader.ENUM_HIGH, GapChecker.level(GapChecker.HIGH_USEFULNESS));
		Assertions.assertEquals(ConfidenceReader.ENUM_MEDIUM, GapChecker.level(GapChecker.HIGH_USEFULNESS - 1));
		Assertions.assertEquals(ConfidenceReader.ENUM_MEDIUM, GapChecker.level(GapChecker.MEDIUM_USEFULNESS));
		Assertions.assertEquals(ConfidenceReader.ENUM_LOW, GapChecker.level(GapChecker.MEDIUM_USEFULNESS - 1));
		Assertions.assertTrue(only(new Builder().activity("a", ResearchTaskReader.ENUM_STATUS_COMPLETED, null, false, false).check()).explanation()
			.contains("Estimated usefulness: low (25 of 100)"));
	}

	@Test
	void everyFindingHasAKeyAndTheyAreDistinct(){
		final List<Diagnostic> findings = new Builder()
			.person("a", 1900, false).person(person("p", false, false, true, 2)).person("b", null, true).person("c", null, true)
			.parent("c", "a").parent("c", "b").parent("a", "p")
			.event("e", EventReader.ENUM_TYPE_BIRTH, 0, "a")
			.question("q", ResearchQuestionReader.ENUM_STATUS_OPEN, false).question("q2", ResearchQuestionReader.ENUM_STATUS_OPEN, false).activity("x", ResearchTaskReader.ENUM_STATUS_COMPLETED, ResearchActivityReader.ENUM_RESULT_NEGATIVE, false, false, "q2")
			.activity("y", ResearchTaskReader.ENUM_STATUS_COMPLETED, null, false, false)
			.task("t", ResearchQuestionReader.ENUM_STATUS_OPEN, ConfidenceReader.ENUM_HIGH, "2020-01-01", "q")
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
