package io.github.mtrevisan.familylegacy.ui.assistant.planner;

import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.assistant.gap.GapData;
import io.github.mtrevisan.familylegacy.ui.assistant.gap.GapDraft;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


class PlannerCheckerTest{

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 28);

	private static final class Builder{

		private final Map<String, GapData.Person> persons = new LinkedHashMap<>();
		private final List<GapData.Event> events = new ArrayList<>();
		private final List<GapData.ParentLink> parents = new ArrayList<>();
		private final List<GapData.Union> unions = new ArrayList<>();
		private final List<GapData.Question> questions = new ArrayList<>();
		private final List<GapData.Activity> activities = new ArrayList<>();
		private final List<GapData.Task> tasks = new ArrayList<>();

		Builder person(final String id, final Integer birthYear){
			return person(id, birthYear, false, false, 0);
		}

		Builder person(final String id, final Integer birthYear, final boolean hasBirth, final boolean hasCensus, final int events){
			persons.put(id, new GapData.Person(id, "Person " + id, birthYear, hasBirth, false, false, hasCensus, events));
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

		Builder question(final String id, final String status, final boolean conclusion, final String... targets){
			questions.add(new GapData.Question(id, "Title of " + id, status, conclusion, List.of(targets)));
			return this;
		}

		Builder activity(final String id, final String action, final String status, final String result, final String... questions){
			activities.add(new GapData.Activity(id, action, status, result, List.of(questions), false, false));
			return this;
		}

		Builder task(final String id, final String description, final String status, final String... questions){
			tasks.add(new GapData.Task(id, description, status, null, null, List.of(questions)));
			return this;
		}

		List<PlannerFinding> findings(){
			return PlannerChecker.findings(new GapData(TODAY, persons, events, parents, unions, questions, activities, tasks));
		}

	}

	private static List<PlannerFinding> ofKind(final List<PlannerFinding> findings, final String kind){
		return findings.stream().filter(f -> f.diagnostic().key().startsWith(kind + "|")).toList();
	}

	private static List<String> tasks(final List<PlannerFinding> findings){
		return findings.stream().map(f -> f.draft().text()).toList();
	}


	@Test
	void aQuestionAboutAPersonWithNothingRecordedHasTheNextSteps(){
		final List<PlannerFinding> found = ofKind(new Builder()
			.person("a", 1850)
			.question("q1", "open", false, "a")
			.findings(), "planner.next_step");

		//the birth, the marriage record that names the parents, and the census or the death are possible: the best three are proposed
		Assertions.assertEquals(PlannerChecker.MAX_STEPS_PER_QUESTION, found.size());
		final List<String> tasks = tasks(found);
		Assertions.assertTrue(tasks.stream().anyMatch(t -> t.contains("birth or baptism")), tasks.toString());
		Assertions.assertTrue(tasks.stream().anyMatch(t -> t.contains("marriage record")), tasks.toString());
	}

	@Test
	void aStepIsATaskDraftThatSupportsTheQuestion(){
		final PlannerFinding finding = ofKind(new Builder()
			.person("a", 1850)
			.question("q1", "open", false, "a")
			.findings(), "planner.next_step").getFirst();

		Assertions.assertEquals(GapDraft.Kind.TASK, finding.draft().kind());
		Assertions.assertEquals(List.of("q1"), finding.draft().records());
		Assertions.assertEquals("high", finding.draft().priority());
		Assertions.assertEquals(List.of("q1", "a"), finding.diagnostic().recordIds());
		Assertions.assertEquals(Diagnostic.Severity.INFORMATION, finding.diagnostic().severity());
		//the reason is given, with the usefulness
		Assertions.assertTrue(finding.diagnostic().explanation().contains("Title of q1"), finding.diagnostic().explanation());
		Assertions.assertTrue(finding.diagnostic().explanation().contains("100"), finding.diagnostic().explanation());
	}

	@Test
	void theStepsAreOrderedByUsefulness(){
		final List<PlannerFinding> found = ofKind(new Builder()
			.person("a", 1850)
			.question("q1", "open", false, "a")
			.findings(), "planner.next_step");

		final double first = found.get(0).diagnostic().rank();
		final double last = found.get(found.size() - 1).diagnostic().rank();
		Assertions.assertTrue(first >= last);
		//the birth is the first record to look for: nothing is known of it
		Assertions.assertTrue(found.stream().max((x, y) -> Double.compare(x.diagnostic().rank(), y.diagnostic().rank())).orElseThrow()
			.draft().text().contains("birth"));
	}

	@Test
	void whatIsAlreadyDoneIsNotProposed(){
		//a birth that a source cites, parents known, a marriage cited, a death cited, a census: nothing is left to search
		final List<PlannerFinding> found = ofKind(new Builder()
			.person("a", 1850, true, true, 3).person("p", 1820)
			.event("b", "birth", 1, "a").event("m", "marriage", 1, "a").event("d", "death", 1, "a")
			.parent("a", "p").union("a", "p")
			.question("q1", "open", false, "a")
			.findings(), "planner.next_step");

		Assertions.assertTrue(found.isEmpty(), tasks(found).toString());
	}

	@Test
	void aBirthThatNoSourceCitesIsProposedWithADifferentReason(){
		final List<PlannerFinding> found = ofKind(new Builder()
			.person("a", 1990, true, true, 1).person("p", 1960)
			.event("b", "birth", 0, "a").parent("a", "p")
			.question("q1", "open", false, "a")
			.findings(), "planner.next_step");

		Assertions.assertEquals(1, found.size(), tasks(found).toString());
		Assertions.assertTrue(found.getFirst().draft().text().contains("to cite it"));
	}

	@Test
	void aTaskOrAnActivityThatCoversAStepMakesItNotProposed(){
		final Builder builder = new Builder()
			.person("a", 1850)
			.question("q1", "open", false, "a")
			.task("t1", "Cerca l'atto di nascita (battesimo) di Anna", "open", "q1")
			.activity("r1", "Read the marriage registers of the parish", "completed", "negative", "q1");

		final List<String> tasks = tasks(ofKind(builder.findings(), "planner.next_step"));

		Assertions.assertTrue(tasks.stream().noneMatch(t -> t.contains("birth or baptism")), tasks.toString());
		Assertions.assertTrue(tasks.stream().noneMatch(t -> t.contains("marriage record")), tasks.toString());
		//and an abandoned task does not cover anything
		final List<String> withAbandoned = tasks(ofKind(new Builder().person("a", 1850).question("q1", "open", false, "a")
			.task("t1", "Search the birth record", "abandoned", "q1").findings(), "planner.next_step"));
		Assertions.assertTrue(withAbandoned.stream().anyMatch(t -> t.contains("birth or baptism")), withAbandoned.toString());
	}

	@Test
	void aQuestionThatIsNotOpenOrIsAnsweredIsNotPlanned(){
		final Builder builder = new Builder().person("a", 1850)
			.question("q1", "resolved", false, "a").question("q2", "open", true, "a").question("q3", "on_hold", false, "a");

		Assertions.assertTrue(ofKind(builder.findings(), "planner.next_step").isEmpty());
	}

	@Test
	void aQuestionAboutAnEventIsAboutItsPrincipals(){
		final List<PlannerFinding> found = ofKind(new Builder()
			.person("a", 1850, true, true, 1).person("p", 1820)
			.event("b", "birth", 0, "a").parent("a", "p")
			.question("q1", "open", false, "b")
			.findings(), "planner.next_step");

		//the birth that no source cites, and the death of a person born in 1850
		Assertions.assertEquals(2, found.size(), tasks(found).toString());
		Assertions.assertTrue(found.stream().allMatch(f -> f.diagnostic().recordIds().equals(List.of("q1", "a"))));
	}

	@Test
	void aQuestionWithNoPersonCannotBePlanned(){
		Assertions.assertTrue(ofKind(new Builder().question("q1", "open", false, "X9").question("q2", "open", false).findings(),
			"planner.next_step").isEmpty());
	}

	@Test
	void fruitlessSearchesSuggestContactingTheArchive(){
		final List<PlannerFinding> found = ofKind(new Builder()
			.person("a", 1990, true, true, 1).person("p", 1960)
			.event("b", "birth", 1, "a").parent("a", "p")
			.question("q1", "open", false, "a")
			.activity("r1", "Looked in the parish", "completed", "negative", "q1")
			.activity("r2", "Looked in the civil registry", "completed", "inconclusive", "q1")
			.findings(), "planner.next_step");

		Assertions.assertEquals(1, found.size(), tasks(found).toString());
		Assertions.assertTrue(found.getFirst().draft().text().contains("archive"));
		Assertions.assertTrue(found.getFirst().diagnostic().explanation().contains("2"), found.getFirst().diagnostic().explanation());
	}

	@Test
	void oneFruitlessSearchIsNotEnough(){
		Assertions.assertTrue(ofKind(new Builder()
			.person("a", 1990, true, true, 1).person("p", 1960)
			.event("b", "birth", 1, "a").parent("a", "p")
			.question("q1", "open", false, "a")
			.activity("r1", "Looked in the parish", "completed", "negative", "q1")
			.findings(), "planner.next_step").isEmpty());
	}

	@Test
	void aQuestionNobodyStartedIsMoreUrgent(){
		final double fresh = ofKind(new Builder().person("a", 1850).question("q1", "open", false, "a").findings(), "planner.next_step")
			.stream().mapToDouble(f -> f.diagnostic().rank()).max().orElseThrow();
		final double started = ofKind(new Builder().person("a", 1850).question("q1", "open", false, "a")
			.task("t1", "Think about it", "open", "q1").findings(), "planner.next_step")
			.stream().mapToDouble(f -> f.diagnostic().rank()).max().orElseThrow();

		Assertions.assertEquals(5., fresh - started);
	}

	@Test
	void theStepsOfAllQuestionsAreSummedUpBeyondTheLimit(){
		final Builder builder = new Builder();
		for(int i = 0; i < 12; i ++)
			builder.person("p" + i, 1850).question("q" + i, "open", false, "p" + i);

		final List<PlannerFinding> found = builder.findings();

		Assertions.assertEquals(PlannerChecker.MAX_STEPS, ofKind(found, "planner.next_step").size());
		final List<PlannerFinding> more = ofKind(found, "planner.next_step.more");
		Assertions.assertEquals(1, more.size());
		Assertions.assertNull(more.getFirst().draft());
		Assertions.assertTrue(more.getFirst().diagnostic().recordIds().isEmpty());
	}


	@Test
	void anAncestorWithUnknownParentsIsABrickWall(){
		final List<PlannerFinding> found = ofKind(new Builder()
			.person("a", 1820).person("b", 1850).person("c", 1880)
			.parent("b", "a").parent("c", "b")
			.findings(), "planner.brick_wall");

		Assertions.assertEquals(1, found.size());
		Assertions.assertEquals(List.of("a"), found.getFirst().diagnostic().recordIds());
		Assertions.assertEquals(GapDraft.Kind.QUESTION, found.getFirst().draft().kind());
		Assertions.assertEquals(List.of("a"), found.getFirst().draft().records());
		Assertions.assertTrue(found.getFirst().draft().text().contains("1820"), found.getFirst().draft().text());
		//two descendants: 40 + 6 + 10 for the year
		Assertions.assertEquals(56., found.getFirst().diagnostic().rank());
	}

	@Test
	void aPersonWithNoDescendantsIsNotABrickWall(){
		Assertions.assertTrue(ofKind(new Builder().person("a", 1820).person("b", 1850).findings(), "planner.brick_wall").isEmpty());
	}

	@Test
	void aBrickWallThatAQuestionAlreadyWorksOnIsNotProposed(){
		final Builder builder = new Builder().person("a", 1820).person("b", 1850).parent("b", "a")
			.question("q1", "open", false, "a");

		Assertions.assertTrue(ofKind(builder.findings(), "planner.brick_wall").isEmpty());
		//a question that is over does not count
		Assertions.assertEquals(1, ofKind(new Builder().person("a", 1820).person("b", 1850).parent("b", "a")
			.question("q1", "resolved", false, "a").findings(), "planner.brick_wall").size());
	}

	@Test
	void theBrickWallThatMattersMostComesFirst(){
		final List<PlannerFinding> found = ofKind(new Builder()
			.person("a", 1820).person("b", 1850).person("c", 1880).person("d", 1880)
			.person("x", 1830).person("y", 1860)
			.parent("b", "a").parent("c", "b").parent("d", "b").parent("y", "x")
			.findings(), "planner.brick_wall");

		Assertions.assertEquals(2, found.size());
		final PlannerFinding best = found.stream().max((l, r) -> Double.compare(l.diagnostic().rank(), r.diagnostic().rank())).orElseThrow();
		Assertions.assertEquals(List.of("a"), best.diagnostic().recordIds());
	}

	@Test
	void aCycleOfDescentDoesNotLoopForever(){
		final List<PlannerFinding> found = new Builder().person("a", 1820).person("b", 1850)
			.parent("a", "b").parent("b", "a").findings();

		//both have parents (each other): there is no brick wall, and the checks end
		Assertions.assertTrue(ofKind(found, "planner.brick_wall").isEmpty());
	}

	@Test
	void manyBrickWallsAreSummedUp(){
		final Builder builder = new Builder();
		for(int i = 0; i < 14; i ++)
			builder.person("p" + i, 1800).person("c" + i, 1830).parent("c" + i, "p" + i);

		final List<PlannerFinding> found = builder.findings();

		Assertions.assertEquals(PlannerChecker.MAX_BRICK_WALLS, ofKind(found, "planner.brick_wall").size());
		Assertions.assertEquals(1, ofKind(found, "planner.brick_wall.more").size());
	}

	@Test
	void theUsefulnessIsShownAsAPriority(){
		Assertions.assertEquals("high", PlannerChecker.priority(70));
		Assertions.assertEquals("normal", PlannerChecker.priority(45));
		Assertions.assertEquals("low", PlannerChecker.priority(44));
		Assertions.assertEquals("medium", PlannerChecker.level(60));
	}

	@Test
	void anEmptyResearchHasNothing(){
		Assertions.assertTrue(new Builder().findings().isEmpty());
	}

}
