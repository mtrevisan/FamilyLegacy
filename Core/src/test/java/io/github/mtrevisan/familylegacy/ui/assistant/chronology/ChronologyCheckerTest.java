package io.github.mtrevisan.familylegacy.ui.assistant.chronology;

import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DatePrecision;
import io.github.mtrevisan.familylegacy.io.model.readers.date.NormalizedDate;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


class ChronologyCheckerTest{

	private static long jdn(final String gregorianDate){
		return CalendarType.GREGORIAN.parseToJdn(gregorianDate);
	}

	private static DateRange day(final String gregorianDate){
		return DateRange.day(jdn(gregorianDate));
	}

	private static DateRange year(final int year){
		return DateRange.of(TemporalSpan.point(NormalizedDate.exact(jdn("1 January " + year), DatePrecision.YEAR)));
	}

	private static ChronologyData.Person person(final String id, final SexType sex, final DateRange birth, final DateRange death){
		return new ChronologyData.Person(id, "Person " + id, sex, birth, death, (birth != null? "birth of " + id: null),
			(death != null? "death of " + id: null));
	}

	/** A data set built step by step. */
	private static final class Builder{

		private final Map<String, ChronologyData.Person> persons = new LinkedHashMap<>();
		private final List<ChronologyData.LifeEvent> events = new ArrayList<>();
		private final List<ChronologyData.ParentLink> parents = new ArrayList<>();
		private final List<ChronologyData.Union> unions = new ArrayList<>();

		Builder person(final ChronologyData.Person person){
			persons.put(person.id(), person);
			return this;
		}

		Builder event(final String id, final String personId, final String type, final DateRange date){
			events.add(new ChronologyData.LifeEvent(id, personId, type, date, "date of " + id, true));
			return this;
		}

		Builder parent(final String childId, final String parentId, final boolean biological){
			parents.add(new ChronologyData.ParentLink("rel-" + childId + "-" + parentId, childId, parentId, biological));
			return this;
		}

		Builder union(final String id, final String first, final String second, final boolean marriage, final DateRange from,
				final DateRange to, final boolean endedUnknown){
			unions.add(new ChronologyData.Union(id, first, second, marriage, from, to, endedUnknown, "begin of " + id));
			return this;
		}

		List<Diagnostic> check(){
			return ChronologyChecker.check(new ChronologyData(persons, events, parents, unions));
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


	@Test
	void aConsistentLifeHasNoFindings(){
		final List<Diagnostic> findings = new Builder()
			.person(person("m", SexType.FEMALE, day("1 March 1800"), day("1 May 1870")))
			.person(person("c", SexType.MALE, day("1 June 1825"), day("1 June 1890")))
			.parent("c", "m", true)
			.event("e1", "c", "marriage", day("1 June 1850"))
			.event("e2", "c", "burial", day("5 June 1890"))
			.check();

		Assertions.assertTrue(findings.isEmpty(), titles(findings).toString());
	}

	@Test
	void theKeyOfAFindingTellsItFromTheOthersAndFollowsItsData(){
		final Diagnostic finding = only(new Builder()
			.person(person("a", SexType.MALE, day("1 June 1800"), day("1 June 1799")))
			.check());

		Assertions.assertNotNull(finding.key());
		Assertions.assertTrue(finding.isIgnorable());
		//the same data, the same key
		Assertions.assertEquals(finding.key(), only(new Builder()
			.person(person("a", SexType.MALE, day("1 June 1800"), day("1 June 1799")))
			.check()).key());
		//another death date (as written), still before the birth: a finding to look at again
		final ChronologyData.Person corrected = new ChronologyData.Person("a", "Person a", SexType.MALE, day("1 June 1800"),
			day("1 June 1798"), "birth of a", "a corrected death");
		Assertions.assertNotEquals(finding.key(), only(new Builder().person(corrected).check()).key());
		//another record, another finding
		Assertions.assertNotEquals(finding.key(), only(new Builder()
			.person(person("b", SexType.MALE, day("1 June 1800"), day("1 June 1799")))
			.check()).key());
	}

	@Test
	void theKeysOfTheFindingsOfAModelAreDistinct(){
		final List<Diagnostic> findings = new Builder()
			.person(person("a", SexType.MALE, day("1 June 1800"), day("1 June 1860")))
			.event("e1", "a", "census", day("1 June 1799"))
			.event("e2", "a", "census", day("1 June 1861"))
			.check();

		Assertions.assertEquals(2, findings.size());
		Assertions.assertEquals(2, findings.stream().map(Diagnostic::key).distinct().count());
	}

	@Test
	void deathBeforeBirth(){
		final Diagnostic finding = only(new Builder()
			.person(person("a", SexType.MALE, day("1 June 1800"), day("1 June 1799")))
			.check());

		Assertions.assertEquals(Diagnostic.Severity.ERROR, finding.severity());
		Assertions.assertTrue(finding.title().startsWith("Death before birth"));
		Assertions.assertEquals(List.of("a"), finding.recordIds());
		Assertions.assertTrue(finding.explanation().contains("death of a"));
	}

	@Test
	void uncertainDatesNeverRaiseAnAlarm(){
		//both in 1800: the death can be after the birth
		Assertions.assertTrue(new Builder()
			.person(person("a", SexType.MALE, year(1800), year(1800)))
			.check().isEmpty());
		//the child can be born after the parent within the same year
		Assertions.assertTrue(new Builder()
			.person(person("p", SexType.FEMALE, year(1800), null))
			.person(person("c", SexType.MALE, year(1825), null))
			.parent("c", "p", true)
			.check().isEmpty());
		//an event in the same year of the death can be before it
		Assertions.assertTrue(new Builder()
			.person(person("a", SexType.MALE, year(1800), year(1850)))
			.event("e", "a", "census", year(1850))
			.check().isEmpty());
	}

	@Test
	void implausibleLifespan(){
		final Diagnostic finding = only(new Builder()
			.person(person("a", SexType.MALE, day("1 June 1700"), day("1 June 1830")))
			.check());

		Assertions.assertEquals(Diagnostic.Severity.WARNING, finding.severity());
		Assertions.assertTrue(finding.title().startsWith("Implausible lifespan"));
		Assertions.assertTrue(new Builder()
			.person(person("a", SexType.MALE, day("1 June 1700"), day("1 June 1810")))
			.check().isEmpty());
	}

	@Test
	void eventBeforeBirthAndAfterDeath(){
		final List<Diagnostic> findings = new Builder()
			.person(person("a", SexType.MALE, day("1 June 1800"), day("1 June 1860")))
			.event("e1", "a", "census", day("1 June 1799"))
			.event("e2", "a", "marriage", day("1 June 1799"))
			.event("e3", "a", "census", day("1 June 1861"))
			.event("e4", "a", "burial", day("5 June 1860"))
			.event("e5", "a", "probate", day("1 June 1870"))
			.event("e6", "a", "birth", day("1 June 1800"))
			.check();

		Assertions.assertEquals(3, findings.size(), titles(findings).toString());
		Assertions.assertTrue(titles(findings).get(0).startsWith("Event before birth"));
		Assertions.assertTrue(titles(findings).get(1).startsWith("Marriage before birth"));
		Assertions.assertTrue(titles(findings).get(2).startsWith("Event after death"));
		Assertions.assertEquals(List.of("a", "e1"), findings.getFirst().recordIds());
	}

	@Test
	void childBornBeforeTheParent(){
		final Diagnostic finding = only(new Builder()
			.person(person("p", SexType.MALE, day("1 June 1800"), null))
			.person(person("c", SexType.MALE, day("1 June 1790"), null))
			.parent("c", "p", true)
			.check());

		Assertions.assertEquals(Diagnostic.Severity.ERROR, finding.severity());
		Assertions.assertTrue(finding.title().startsWith("Child born before the parent"));
		Assertions.assertTrue(finding.recordIds().containsAll(List.of("c", "p", "rel-c-p")));
	}

	@Test
	void anAdoptedChildCanBeOlderThanTheParent(){
		Assertions.assertTrue(new Builder()
			.person(person("p", SexType.MALE, day("1 June 1800"), null))
			.person(person("c", SexType.MALE, day("1 June 1790"), null))
			.parent("c", "p", false)
			.check().isEmpty());
	}

	@Test
	void parentTooYoungAndTooOld(){
		final Diagnostic young = only(new Builder()
			.person(person("m", SexType.FEMALE, day("1 June 1800"), null))
			.person(person("c", SexType.MALE, day("1 June 1810"), null))
			.parent("c", "m", true)
			.check());
		Assertions.assertEquals(Diagnostic.Severity.WARNING, young.severity());
		Assertions.assertTrue(young.title().startsWith("Parent too young"));

		//13 is old enough for a mother, not for a father
		Assertions.assertTrue(new Builder()
			.person(person("m", SexType.FEMALE, day("1 June 1800"), null))
			.person(person("c", SexType.MALE, day("1 June 1813"), null))
			.parent("c", "m", true)
			.check().isEmpty());
		Assertions.assertEquals(1, new Builder()
			.person(person("f", SexType.MALE, day("1 June 1800"), null))
			.person(person("c", SexType.MALE, day("1 June 1813"), null))
			.parent("c", "f", true)
			.check().size());

		final Diagnostic old = only(new Builder()
			.person(person("m", SexType.FEMALE, day("1 June 1800"), null))
			.person(person("c", SexType.MALE, day("1 June 1857"), null))
			.parent("c", "m", true)
			.check());
		Assertions.assertTrue(old.title().startsWith("Parent too old"));
		//the same age is fine for a father
		Assertions.assertTrue(new Builder()
			.person(person("f", SexType.MALE, day("1 June 1800"), null))
			.person(person("c", SexType.MALE, day("1 June 1857"), null))
			.parent("c", "f", true)
			.check().isEmpty());
	}

	@Test
	void ageLimitsNeedCertainDates(){
		//a mother born in 1800 and a child born in 1812: the mother can be 12 or older
		Assertions.assertTrue(new Builder()
			.person(person("m", SexType.FEMALE, year(1800), null))
			.person(person("c", SexType.MALE, year(1812), null))
			.parent("c", "m", true)
			.check().isEmpty());
	}

	@Test
	void bornAfterTheDeathOfAParent(){
		final Diagnostic mother = only(new Builder()
			.person(person("m", SexType.FEMALE, day("1 June 1800"), day("1 June 1830")))
			.person(person("c", SexType.MALE, day("2 June 1830"), null))
			.parent("c", "m", true)
			.check());
		Assertions.assertTrue(mother.title().startsWith("Child born after the death of the mother"));

		//a child born within the gestation after the death of the father is possible
		Assertions.assertTrue(new Builder()
			.person(person("f", SexType.MALE, day("1 June 1800"), day("1 June 1830")))
			.person(person("c", SexType.MALE, day("1 January 1831"), null))
			.parent("c", "f", true)
			.check().isEmpty());
		final Diagnostic father = only(new Builder()
			.person(person("f", SexType.MALE, day("1 June 1800"), day("1 June 1830")))
			.person(person("c", SexType.MALE, day("1 June 1831"), null))
			.parent("c", "f", true)
			.check());
		Assertions.assertTrue(father.title().startsWith("Child born after the death of the father"));
	}

	@Test
	void unionBeforeBirthAndAfterDeath(){
		final List<Diagnostic> findings = new Builder()
			.person(person("a", SexType.MALE, day("1 June 1800"), day("1 June 1850")))
			.person(person("b", SexType.FEMALE, day("1 June 1810"), null))
			.union("u1", "a", "b", true, day("1 June 1805"), null, false)
			.union("u2", "a", "b", true, day("1 June 1851"), null, false)
			.check();

		Assertions.assertEquals(2, findings.size(), titles(findings).toString());
		Assertions.assertTrue(titles(findings).get(0).startsWith("Union before birth: Person b"));
		Assertions.assertTrue(titles(findings).get(1).startsWith("Union after death: Person a"));
	}

	@Test
	void overlappingMarriages(){
		final Builder overlapping = new Builder()
			.person(person("a", SexType.MALE, day("1 June 1800"), null))
			.person(person("b", SexType.FEMALE, day("1 June 1805"), null))
			.person(person("c", SexType.FEMALE, day("1 June 1806"), null))
			.union("u1", "a", "b", true, day("1 June 1825"), null, false)
			.union("u2", "a", "c", true, day("1 June 1830"), null, false);
		final Diagnostic finding = only(overlapping.check());
		Assertions.assertEquals(Diagnostic.Severity.INFORMATION, finding.severity());
		Assertions.assertTrue(finding.title().startsWith("Overlapping marriages: Person a"));

		//the first marriage ended before the second began
		Assertions.assertTrue(new Builder()
			.person(person("a", SexType.MALE, day("1 June 1800"), null))
			.union("u1", "a", "b", true, day("1 June 1825"), day("1 June 1828"), false)
			.union("u2", "a", "c", true, day("1 June 1830"), null, false)
			.check().isEmpty());
		//the first wife died before the second marriage
		Assertions.assertTrue(new Builder()
			.person(person("a", SexType.MALE, day("1 June 1800"), null))
			.person(person("b", SexType.FEMALE, day("1 June 1805"), day("1 June 1828")))
			.union("u1", "a", "b", true, day("1 June 1825"), null, false)
			.union("u2", "a", "c", true, day("1 June 1830"), null, false)
			.check().isEmpty());
		//an end that is known without a date is not a claim of overlap
		Assertions.assertTrue(new Builder()
			.person(person("a", SexType.MALE, day("1 June 1800"), null))
			.union("u1", "a", "b", true, day("1 June 1825"), null, true)
			.union("u2", "a", "c", true, day("1 June 1830"), null, false)
			.check().isEmpty());
		//a civil and a religious marriage of the same couple are one marriage
		Assertions.assertTrue(new Builder()
			.person(person("a", SexType.MALE, day("1 June 1800"), null))
			.union("u1", "a", "b", true, day("1 June 1825"), null, false)
			.union("u2", "b", "a", true, day("1 June 1825"), null, false)
			.check().isEmpty());
		//a cohabitation is not a marriage
		Assertions.assertTrue(new Builder()
			.person(person("a", SexType.MALE, day("1 June 1800"), null))
			.union("u1", "a", "b", true, day("1 June 1825"), null, false)
			.union("u2", "a", "c", false, day("1 June 1830"), null, false)
			.check().isEmpty());
	}

	@Test
	void dateRangeOfADate(){
		final long start = jdn("1 January 1800");

		final DateRange exact = DateRange.of(TemporalSpan.point(NormalizedDate.exact(start, DatePrecision.DAY)));
		Assertions.assertEquals(start, exact.min());
		Assertions.assertEquals(start, exact.max());

		final DateRange yearRange = DateRange.of(TemporalSpan.point(NormalizedDate.exact(start, DatePrecision.YEAR)));
		Assertions.assertEquals(start, yearRange.min());
		Assertions.assertEquals(start + 365, yearRange.max());

		final DateRange approximate = DateRange.of(TemporalSpan.point(NormalizedDate.approximated(start, DatePrecision.DAY,
			"gregorian", "stated", "P1Y")));
		Assertions.assertEquals(start - 366, approximate.min());
		Assertions.assertEquals(start + 366, approximate.max());

		final DateRange noMargin = DateRange.of(TemporalSpan.point(NormalizedDate.approximated(start, DatePrecision.DAY,
			"gregorian", "stated", null)));
		Assertions.assertEquals(start - DateRange.DEFAULT_APPROXIMATE_MARGIN_DAYS, noMargin.min());

		Assertions.assertNull(DateRange.of(null));
	}

}
