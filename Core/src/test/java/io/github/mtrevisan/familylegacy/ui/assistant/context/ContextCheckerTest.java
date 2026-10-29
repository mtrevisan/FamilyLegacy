package io.github.mtrevisan.familylegacy.ui.assistant.context;

import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.assistant.chronology.DateRange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


class ContextCheckerTest{

	private static DateRange range(final String from, final String to){
		return new DateRange(CalendarType.GREGORIAN.parseToJdn(from), CalendarType.GREGORIAN.parseToJdn(to));
	}

	private static DateRange year(final int year){
		return range(year + "-01-01", year + "-12-31");
	}

	private static DateRange day(final String date){
		return DateRange.day(CalendarType.GREGORIAN.parseToJdn(date));
	}

	private static final class Builder{

		private final List<ContextData.Event> events = new ArrayList<>();
		private final List<ContextData.Context> contexts = new ArrayList<>();
		private final List<ContextData.Impact> impacts = new ArrayList<>();
		private final Map<String, Set<String>> ancestors = new HashMap<>();
		private final Map<String, String> places = new HashMap<>(Map.of("P1", "Treviso", "P2", "Veneto", "P3", "Padua"));

		Builder event(final String id, final String type, final DateRange range, final String place, final String... persons){
			events.add(new ContextData.Event(id, type, range, place, type + " of " + String.join("+", persons), List.of(persons)));
			return this;
		}

		Builder historic(final String id, final String type, final DateRange range, final String place){
			contexts.add(new ContextData.Context(id, true, type, "Title " + id, range, place));
			return this;
		}

		Builder norm(final String id, final String type, final DateRange range, final String place){
			contexts.add(new ContextData.Context(id, false, type, "Title " + id, range, place));
			return this;
		}

		Builder impact(final String context, final String target){
			impacts.add(new ContextData.Impact(context, target));
			return this;
		}

		Builder inside(final String place, final String container){
			ancestors.computeIfAbsent(place, k -> new java.util.HashSet<>()).add(container);
			return this;
		}

		List<ContextFinding> findings(){
			return ContextChecker.findings(new ContextData(events, contexts, impacts, ancestors, places));
		}

	}

	private static List<ContextFinding> ofKind(final List<ContextFinding> findings, final String kind){
		return findings.stream().filter(f -> f.diagnostic().key().startsWith(kind + "|")).toList();
	}


	@Test
	void aWarInThePlaceAndThePeriodMayHaveInfluencedADeath(){
		final List<ContextFinding> found = ofKind(new Builder()
			.event("E1", "death", day("1916-05-10"), "P1", "I1")
			.historic("H1", "war", range("1914-07-28", "1918-11-11"), "P1")
			.findings(), "context.candidate");

		Assertions.assertEquals(1, found.size());
		final Diagnostic diagnostic = found.getFirst().diagnostic();
		Assertions.assertEquals(AssistantCategory.CONTEXT, diagnostic.category());
		Assertions.assertEquals(Diagnostic.Severity.INFORMATION, diagnostic.severity());
		Assertions.assertEquals(List.of("H1", "E1"), diagnostic.recordIds());
		//the place and the period match, the event is inside the period
		Assertions.assertEquals(65., diagnostic.rank());
		final ContextFinding.ImpactDraft draft = found.getFirst().impact();
		Assertions.assertEquals(new ContextFinding.ImpactDraft("H1", "E1", "influences", draft.rationale()), draft);
		Assertions.assertNull(found.getFirst().historic());
		Assertions.assertTrue(diagnostic.explanation().contains("Title H1"), diagnostic.explanation());
		Assertions.assertTrue(draft.rationale().contains("Title H1"), draft.rationale());
	}

	@Test
	void withoutAPlaceOnlyThePeriodMatchesAndItCountsLess(){
		final List<ContextFinding> found = ofKind(new Builder()
			.event("E1", "death", day("1916-05-10"), "P1", "I1")
			.historic("H1", "war", range("1914-07-28", "1918-11-11"), null)
			.findings(), "context.candidate");

		Assertions.assertEquals(1, found.size());
		Assertions.assertEquals(45., found.getFirst().diagnostic().rank());
	}

	@Test
	void anotherPlaceIsNotAMatch(){
		final Builder builder = new Builder()
			.event("E1", "death", day("1916-05-10"), "P1", "I1")
			.event("E2", "death", day("1916-05-10"), null, "I2")
			.historic("H1", "war", range("1914-07-28", "1918-11-11"), "P3");

		Assertions.assertTrue(ofKind(builder.findings(), "context.candidate").isEmpty());
	}

	@Test
	void aPlaceInsideThePlaceOfTheContextIsAMatch(){
		final List<ContextFinding> found = ofKind(new Builder()
			.event("E1", "death", day("1916-05-10"), "P1", "I1")
			.historic("H1", "war", range("1914-07-28", "1918-11-11"), "P2")
			.inside("P1", "P2")
			.findings(), "context.candidate");

		Assertions.assertEquals(1, found.size());
	}

	@Test
	void anEventOfAnotherKindIsNotInfluenced(){
		//a war and a birth: nothing says the war touched the birth
		Assertions.assertTrue(ofKind(new Builder()
			.event("E1", "birth", day("1916-05-10"), "P1", "I1")
			.historic("H1", "war", range("1914-07-28", "1918-11-11"), "P1")
			.findings(), "context.candidate").isEmpty());
	}

	@Test
	void anEventOutsideThePeriodOrWithoutAPreciseDateIsLeftOut(){
		final Builder builder = new Builder()
			.event("E1", "death", day("1930-05-10"), "P1", "I1")
			.event("E2", "death", null, "P1", "I2")
			.event("E3", "death", range("1800-01-01", "1990-12-31"), "P1", "I3")
			.event("E4", "death", new DateRange(CalendarType.GREGORIAN.parseToJdn("1916-01-01"), Long.MAX_VALUE), "P1", "I4")
			.historic("H1", "war", range("1914-07-28", "1918-11-11"), "P1");

		Assertions.assertTrue(ofKind(builder.findings(), "context.candidate").isEmpty());
	}

	@Test
	void aHistoricEventWithNoDateIsNotUsed(){
		Assertions.assertTrue(ofKind(new Builder()
			.event("E1", "death", day("1916-05-10"), "P1", "I1")
			.historic("H1", "war", null, "P1")
			.historic("H2", "war", new DateRange(CalendarType.GREGORIAN.parseToJdn("1914-01-01"), Long.MAX_VALUE), "P1")
			.findings(), "context.candidate").isEmpty());
	}

	@Test
	void anImpactThatAlreadyExistsIsNotProposed(){
		Assertions.assertTrue(ofKind(new Builder()
			.event("E1", "death", day("1916-05-10"), "P1", "I1")
			.historic("H1", "war", range("1914-07-28", "1918-11-11"), "P1")
			.impact("H1", "E1")
			.findings(), "context.candidate").isEmpty());
	}

	@Test
	void aTypeThatIsNotKnownOrIsMissingIsNotUsed(){
		Assertions.assertTrue(ofKind(new Builder()
			.event("E1", "death", day("1916-05-10"), "P1", "I1")
			.historic("H1", "scientific_discovery", range("1914-07-28", "1918-11-11"), "P1")
			.historic("H2", null, range("1914-07-28", "1918-11-11"), "P1")
			.norm("N1", null, range("1914-07-28", "1918-11-11"), "P1")
			.findings(), "context.candidate").isEmpty());
	}


	@Test
	void aMarriageLawInThePlaceAndThePeriodMayHaveInfluencedAMarriage(){
		final List<ContextFinding> found = ofKind(new Builder()
			.event("E1", "marriage", day("1810-05-10"), "P1", "I1", "I2")
			.event("E2", "birth", day("1810-05-10"), "P1", "I3")
			.norm("N1", "marriage_minimum_age", range("1804-03-21", "1814-12-31"), "P2")
			.inside("P1", "P2")
			.findings(), "context.candidate");

		Assertions.assertEquals(1, found.size());
		Assertions.assertEquals("constrains", found.getFirst().impact().impactType());
		Assertions.assertEquals(List.of("N1", "E1"), found.getFirst().diagnostic().recordIds());
	}

	@Test
	void aNormOpenOnOneSideStillHasABasis(){
		final Builder builder = new Builder()
			.event("E1", "marriage", day("1900-05-10"), "P1", "I1")
			.event("E2", "marriage", day("1700-05-10"), "P1", "I2")
			.norm("N1", "marriage_practice", new DateRange(CalendarType.GREGORIAN.parseToJdn("1804-03-21"), Long.MAX_VALUE), null);

		final List<ContextFinding> found = ofKind(builder.findings(), "context.candidate");

		//from 1804: the marriage of 1900, not the one of 1700
		Assertions.assertEquals(1, found.size());
		Assertions.assertEquals(List.of("N1", "E1"), found.getFirst().diagnostic().recordIds());
	}

	@Test
	void aNormWithNeitherDatesNorPlaceWouldMatchEverythingSoItIsNotUsed(){
		Assertions.assertTrue(ofKind(new Builder()
			.event("E1", "marriage", day("1900-05-10"), "P1", "I1")
			.norm("N1", "marriage_practice", null, null)
			.findings(), "context.candidate").isEmpty());
	}

	@Test
	void aNormOfAPlaceWithNoDatesIsInEffectAllTheTime(){
		final List<ContextFinding> found = ofKind(new Builder()
			.event("E1", "birth", day("1900-05-10"), "P1", "I1")
			.norm("N1", "patronymic_system", null, "P1")
			.findings(), "context.candidate");

		Assertions.assertEquals(1, found.size());
		Assertions.assertEquals("explains", found.getFirst().impact().impactType());
		Assertions.assertEquals(55., found.getFirst().diagnostic().rank());
	}


	@Test
	void atMostThreeEventsAreProposedForAContext(){
		final Builder builder = new Builder()
			.historic("H1", "epidemic", range("1855-01-01", "1855-12-31"), "P1");
		for(int i = 1; i <= 6; i ++)
			builder.event("E" + i, "death", day("1855-0" + i + "-10"), "P1", "I" + i);

		final List<ContextFinding> found = ofKind(builder.findings(), "context.candidate");

		Assertions.assertEquals(ContextChecker.MAX_PER_CONTEXT, found.size());
	}

	@Test
	void manyImpactsAreSummedUp(){
		final Builder builder = new Builder();
		for(int i = 0; i < 12; i ++){
			builder.historic("H" + i, "epidemic", range("1855-01-01", "1855-12-31"), "P1");
			builder.event("E" + i, "death", day("1855-03-10"), "P1", "I" + i);
		}

		final List<ContextFinding> found = builder.findings();

		//12 contexts, each with 12 deaths: 36 candidates at most, 25 listed and one finding for the rest
		Assertions.assertEquals(ContextChecker.MAX_PER_KIND, ofKind(found, "context.candidate").size());
		Assertions.assertEquals(1, ofKind(found, "context.candidate.more").size());
	}


	@Test
	void severalDeathsInAPlaceInAShortTimeAreACrisisToLookFor(){
		final List<ContextFinding> found = ofKind(new Builder()
			.event("E1", "death", day("1855-03-10"), "P1", "I1")
			.event("E2", "death", day("1855-07-10"), "P1", "I2")
			.event("E3", "burial", day("1856-01-10"), "P1", "I3")
			.findings(), "context.cluster");

		Assertions.assertEquals(1, found.size());
		final ContextFinding finding = found.getFirst();
		Assertions.assertEquals(List.of("P1", "E1", "E2", "E3"), finding.diagnostic().recordIds());
		Assertions.assertEquals("epidemic", finding.historic().type());
		Assertions.assertTrue(finding.historic().title().contains("Treviso"), finding.historic().title());
		Assertions.assertTrue(finding.historic().title().contains("1855"), finding.historic().title());
		Assertions.assertNull(finding.impact());
		Assertions.assertEquals(55., finding.diagnostic().rank());
	}

	@Test
	void twoDeathsOrDeathsSpreadOverManyYearsAreNotACrisis(){
		Assertions.assertTrue(ofKind(new Builder()
			.event("E1", "death", day("1855-03-10"), "P1", "I1")
			.event("E2", "death", day("1855-07-10"), "P1", "I2")
			.findings(), "context.cluster").isEmpty());
		Assertions.assertTrue(ofKind(new Builder()
			.event("E1", "death", day("1850-03-10"), "P1", "I1")
			.event("E2", "death", day("1855-07-10"), "P1", "I2")
			.event("E3", "death", day("1860-01-10"), "P1", "I3")
			.findings(), "context.cluster").isEmpty());
	}

	@Test
	void theSamePersonIsCountedOnce(){
		//the death and the burial of one individual, and one more: two persons
		Assertions.assertTrue(ofKind(new Builder()
			.event("E1", "death", day("1855-03-10"), "P1", "I1")
			.event("E2", "burial", day("1855-03-12"), "P1", "I1")
			.event("E3", "death", day("1855-07-10"), "P1", "I2")
			.findings(), "context.cluster").isEmpty());
	}

	@Test
	void deathsInDifferentPlacesAreNotACluster(){
		Assertions.assertTrue(ofKind(new Builder()
			.event("E1", "death", day("1855-03-10"), "P1", "I1")
			.event("E2", "death", day("1855-07-10"), "P3", "I2")
			.event("E3", "death", day("1855-09-10"), "P2", "I3")
			.findings(), "context.cluster").isEmpty());
	}

	@Test
	void aHistoricEventThatCoversThePlaceAndThePeriodExplainsTheDeaths(){
		final Builder builder = new Builder()
			.event("E1", "death", day("1855-03-10"), "P1", "I1")
			.event("E2", "death", day("1855-07-10"), "P1", "I2")
			.event("E3", "death", day("1855-09-10"), "P1", "I3");

		Assertions.assertTrue(ofKind(builder.historic("H1", "epidemic", year(1855), null).findings(), "context.cluster").isEmpty());
		//in a place that contains the place of the deaths
		Assertions.assertTrue(ofKind(new Builder()
			.event("E1", "death", day("1855-03-10"), "P1", "I1")
			.event("E2", "death", day("1855-07-10"), "P1", "I2")
			.event("E3", "death", day("1855-09-10"), "P1", "I3")
			.historic("H1", "famine", year(1855), "P2").inside("P1", "P2").findings(), "context.cluster").isEmpty());
	}

	@Test
	void aHistoricEventOfAnotherPlaceOrPeriodDoesNotExplainTheDeaths(){
		final Builder elsewhere = new Builder()
			.event("E1", "death", day("1855-03-10"), "P1", "I1")
			.event("E2", "death", day("1855-07-10"), "P1", "I2")
			.event("E3", "death", day("1855-09-10"), "P1", "I3")
			.historic("H1", "epidemic", year(1855), "P3")
			//and a reform is not a crisis
			.historic("H2", "legal_reform", year(1855), null)
			.historic("H3", "epidemic", year(1900), null);

		Assertions.assertEquals(1, ofKind(elsewhere.findings(), "context.cluster").size());
	}

	@Test
	void theUsefulnessOfACrisisGrowsWithTheDeathsUpToALimit(){
		final Builder builder = new Builder();
		for(int i = 1; i <= 9; i ++)
			builder.event("E" + i, "death", day("1855-0" + i + "-10"), "P1", "I" + i);

		Assertions.assertEquals(70., ofKind(builder.findings(), "context.cluster").getFirst().diagnostic().rank());
	}

	@Test
	void anEmptyModelHasNothing(){
		Assertions.assertTrue(new Builder().findings().isEmpty());
		Assertions.assertEquals("high", ContextChecker.level(70));
		Assertions.assertEquals("medium", ContextChecker.level(45));
		Assertions.assertEquals("low", ContextChecker.level(44));
	}

}
