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

import io.github.mtrevisan.familylegacy.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.assistant.DiagnosticKeys;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/** The gaps of the research gathered by kind, with the searches and the repositories that go with them. */
public class GapAnalysisTest{

	private static final LocalDate TODAY = LocalDate.of(2026, 11, 25);


	private static Diagnostic finding(final String kind, final String... ids){
		return new Diagnostic(Diagnostic.Severity.INFORMATION, AssistantCategory.RESEARCH, "Title of " + kind, "Explanation of " + kind,
			List.of(ids), null, DiagnosticKeys.of(kind, List.of(ids)));
	}

	private static GapData data(final List<GapData.Person> persons, final List<GapData.Event> events, final List<GapData.ParentLink> parents){
		final Map<String, GapData.Person> map = new LinkedHashMap<>();
		persons.forEach(person -> map.put(person.id(), person));
		return new GapData(TODAY, map, events, parents, List.of(), List.of(), List.of(), List.of());
	}

	private static GapData.Person person(final String id, final Integer birthYear, final List<String> places, final List<String> repositories){
		return new GapData.Person(id, "Person " + id, birthYear, false, false, false, false, 0, places, null, repositories);
	}


	@Test
	void everyKindOfGapHasItsFindings(){
		Assertions.assertEquals(GapAnalysis.Gap.MISSING_BIRTH, GapAnalysis.Gap.of("gap.no_birth"));
		Assertions.assertEquals(GapAnalysis.Gap.MISSING_BIRTH, GapAnalysis.Gap.of("gap.no_birth_place"));
		Assertions.assertEquals(GapAnalysis.Gap.MISSING_DEATH, GapAnalysis.Gap.of("gap.no_death_probable_relatives"));
		Assertions.assertEquals(GapAnalysis.Gap.MISSING_SOURCE, GapAnalysis.Gap.of("gap.unsourced_vital_event"));
		Assertions.assertEquals(GapAnalysis.Gap.INCOMPLETE_CHRONOLOGY, GapAnalysis.Gap.of("gap.children_without_marriage"));
		Assertions.assertEquals(GapAnalysis.Gap.INCOMPLETE_CHRONOLOGY, GapAnalysis.Gap.of("gap.census_without_residence"));
		Assertions.assertEquals(GapAnalysis.Gap.IDENTITY_PROBLEM, GapAnalysis.Gap.of("identity.possible_duplicate"));
		Assertions.assertEquals(GapAnalysis.Gap.IDENTITY_PROBLEM, GapAnalysis.Gap.of("conclusion.unresolved_identity"));
		//what is not a gap
		Assertions.assertNull(GapAnalysis.Gap.of("gap.overdue_task"));
		Assertions.assertNull(GapAnalysis.Gap.of("chronology.child_before_parent"));
		Assertions.assertNull(GapAnalysis.Gap.of(null));
	}

	@Test
	void theGapsAreGroupedByKindAndOtherFindingsAreLeftOut(){
		final GapData data = data(List.of(person("I1", 1850, List.of(), List.of()), person("I2", 1860, List.of(), List.of())), List.of(), List.of());

		final List<GapAnalysis.Entry> entries = GapAnalysis.analyse(List.of(
			finding("identity.possible_duplicate", "I1", "I2"),
			finding("gap.no_birth", "I1"),
			finding("chronology.child_before_parent", "I1"),
			finding("gap.no_death_certain", "I2"),
			finding("gap.no_birth", "I2")), data);

		Assertions.assertEquals(List.of(GapAnalysis.Gap.MISSING_BIRTH, GapAnalysis.Gap.MISSING_BIRTH, GapAnalysis.Gap.MISSING_DEATH,
			GapAnalysis.Gap.IDENTITY_PROBLEM), entries.stream().map(GapAnalysis.Entry::gap).toList());
	}

	@Test
	void aMissingBirthIsSearchedWhereTheIndividualIsKnownAndInTheYearsTheChildrenAllow(){
		final GapData.Person father = person("I1", null, List.of("Asolo"), List.of("Archivio Parrocchiale di Asolo"));
		final GapData.Person child = new GapData.Person("I2", "Person I2", 1880, true, false, false, false, 0);
		final GapData data = data(List.of(father, child), List.of(), List.of(new GapData.ParentLink("I2", "I1")));

		final GapAnalysis.Entry entry = GapAnalysis.analyse(List.of(finding("gap.no_birth_place", "I1")), data).getFirst();

		Assertions.assertEquals(List.of("Person I1"), entry.persons());
		//the child was born in 1880: the father between 16 and 50 years earlier
		Assertions.assertTrue(entry.searches().getFirst().contains("Asolo"), entry.searches().getFirst());
		Assertions.assertTrue(entry.searches().getFirst().contains("1830–1864"), entry.searches().getFirst());
		//the repository of the tree comes before the kinds of archive
		Assertions.assertEquals("Archivio Parrocchiale di Asolo", entry.repositories().getFirst());
		Assertions.assertTrue(entry.repositories().size() > 1);
	}

	@Test
	void theWindowOfABirthComesFromTheRelatives(){
		final GapData.Person father = new GapData.Person("I1", "Father", 1800, true, false, false, false, 0);
		final GapData.Person child = person("I2", null, List.of(), List.of());
		final GapData data = data(List.of(father, child), List.of(), List.of(new GapData.ParentLink("I2", "I1")));

		Assertions.assertArrayEquals(new int[]{1816, 1850}, GapAnalysis.birthWindow(child, data));
		Assertions.assertNull(GapAnalysis.birthWindow(person("I3", null, List.of(), List.of()), data));
		Assertions.assertArrayEquals(new int[]{1800, 1800}, GapAnalysis.birthWindow(father, data));
	}

	@Test
	void aMissingDeathIsSearchedFromTheBirthToTheLongestLife(){
		final GapData.Person old = person("I1", 1850, List.of(), List.of());
		final GapData data = data(List.of(old), List.of(), List.of());

		Assertions.assertArrayEquals(new int[]{1850, 1960}, GapAnalysis.deathWindow(old, data));
		Assertions.assertNull(GapAnalysis.deathWindow(person("I2", null, List.of(), List.of()), data));
		//never beyond today
		Assertions.assertArrayEquals(new int[]{1990, 2026}, GapAnalysis.deathWindow(person("I3", 1990, List.of(), List.of()), data));
	}

	@Test
	void aMissingSourceIsSearchedInTheRegisterOfItsEvent(){
		final GapData.Person person = person("I1", 1850, List.of("Treviso"), List.of());
		final GapData data = data(List.of(person), List.of(new GapData.Event("E1", EventReader.ENUM_TYPE_MARRIAGE, 0, List.of("I1"))), List.of());

		final GapAnalysis.Entry entry = GapAnalysis.analyse(List.of(finding("gap.unsourced_vital_event", "E1", "I1")), data).getFirst();

		Assertions.assertEquals(GapAnalysis.Gap.MISSING_SOURCE, entry.gap());
		Assertions.assertTrue(entry.searches().getFirst().toLowerCase().contains("marriage"), entry.searches().getFirst());
		Assertions.assertTrue(entry.searches().getFirst().contains("Treviso"), entry.searches().getFirst());
	}

	@Test
	void anIdentityProblemIsCompared(){
		final GapData data = data(List.of(person("I1", 1850, List.of("Asolo"), List.of()), person("I2", 1851, List.of("Treviso"), List.of())),
			List.of(), List.of());

		final GapAnalysis.Entry entry = GapAnalysis.analyse(List.of(finding("identity.possible_duplicate", "I1", "I2")), data).getFirst();

		Assertions.assertEquals(List.of("Person I1", "Person I2"), entry.persons());
		Assertions.assertEquals(3, entry.searches().size());
		Assertions.assertTrue(entry.searches().getFirst().contains("Person I1"));
		Assertions.assertTrue(entry.searches().getLast().contains("Asolo"));
	}

	@Test
	void theMarriageOfTheParentsIsSearchedBeforeTheFirstChild(){
		final GapData data = data(List.of(person("I1", 1820, List.of(), List.of()), person("I2", 1825, List.of(), List.of()),
			new GapData.Person("I3", "Child", 1850, true, false, false, false, 0)), List.of(), List.of());

		final GapAnalysis.Entry entry = GapAnalysis.analyse(List.of(finding("gap.children_without_marriage", "I1", "I2", "I3")), data).getFirst();

		Assertions.assertEquals(GapAnalysis.Gap.INCOMPLETE_CHRONOLOGY, entry.gap());
		Assertions.assertTrue(entry.searches().getFirst().contains("1840–1850"), entry.searches().getFirst());
	}

	@Test
	void everyGapHasItsName(){
		for(final GapAnalysis.Gap gap : GapAnalysis.Gap.values())
			Assertions.assertFalse(gap.label().contains("gap.analysis"), gap.name());
	}

}
