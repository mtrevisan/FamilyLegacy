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
package io.github.mtrevisan.familylegacy.reports;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


/** The dashboard of the conflicts: what it lists, how it filters and what it counts. */
class ConflictDashboardTest{

	private static FLEFRecord event(final String id, final String confidence){
		final FLEFRecord event = FLEFRecord.createMainRecord(id, EventReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_TYPE, EventReader.ENUM_TYPE_BIRTH));
		if(confidence != null)
			event.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_CONFIDENCE, confidence));
		event.addChild(FLEFRecord.createChildWithTag(SourceCitationReader.TAG_SOURCE)
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_SOURCE, "S1"))
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_STANCE, SourceCitationReader.STANCE_CONTRADICTS)));
		return event;
	}

	private static FLEFRecord conclusion(final String id, final String preferred, final String proofStatus, final String... resolves){
		final FLEFRecord conclusion = FLEFRecord.createMainRecord(id, ConclusionReader.TYPE);
		for(final String target : resolves)
			conclusion.addChild(FLEFRecord.createChildWithTag(ConclusionReader.TAG_RESOLVES)
				.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, target)));
		if(preferred != null)
			conclusion.addChild(FLEFRecord.createChildWithTag(ConclusionReader.TAG_PREFERRED)
				.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, preferred)));
		if(proofStatus != null)
			conclusion.addChild(FLEFRecord.createChildWithTagAndValue(ConclusionReader.TAG_PROOF_STATUS, proofStatus));
		return conclusion;
	}

	private static ReportConflict conflict(final ReportConflict.Kind kind, final ReportConflict.Problem problem,
			final ReportConflict.Settlement settlement, final String... eventIds){
		return new ReportConflict(kind, problem, List.of("I1"), List.of(
			new ReportConflict.Assertion(eventIds[0], "1850", null, "low"),
			new ReportConflict.Assertion(eventIds[1], "1860", null, null)), settlement);
	}

	private static FLEFModel model(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE));
		model.addRecord(event("E1", "low"));
		model.addRecord(event("E2", null));
		model.addRecord(event("E3", null));
		model.addRecord(event("E4", null));
		return model;
	}

	private static ConflictFinder finder(final ReportConflict... conflicts){
		return model -> List.of(conflicts);
	}


	@Test
	void everythingInConflictIsInOneListWithTheUnresolvedFirst(){
		final FLEFModel model = model();
		final ConflictDashboard dashboard = ConflictDashboard.analyze(model, finder(
			conflict(ReportConflict.Kind.BIRTH, ReportConflict.Problem.DIFFERENT_DATES, new ReportConflict.Settlement("C1", "E1", null), "E1", "E2"),
			conflict(ReportConflict.Kind.DEATH, ReportConflict.Problem.DIFFERENT_PLACES, null, "E3", "E4"),
			new ReportConflict(ReportConflict.Kind.PARENTAGE, ReportConflict.Problem.TOO_MANY_PARENTS, List.of("I1"),
				List.of(new ReportConflict.Assertion("R1", "A", null, null), new ReportConflict.Assertion("R2", "B", null, null)), null),
			new ReportConflict(ReportConflict.Kind.IDENTITY, ReportConflict.Problem.OPEN_HYPOTHESIS, List.of("I1"),
				List.of(new ReportConflict.Assertion("H1", "A / B", null, null)), null)));

		Assertions.assertEquals(4, dashboard.entries().size());
		Assertions.assertEquals(3, dashboard.unresolved());
		Assertions.assertTrue(!dashboard.entries().getFirst().resolved() && dashboard.entries().getLast().resolved());
		Assertions.assertEquals(1, dashboard.count(ConflictDashboard.Category.DATES));
		Assertions.assertEquals(1, dashboard.count(ConflictDashboard.Category.PLACES));
		Assertions.assertEquals(1, dashboard.count(ConflictDashboard.Category.RELATIONSHIPS));
		Assertions.assertEquals(1, dashboard.count(ConflictDashboard.Category.IDENTITIES));
	}

	@Test
	void aConclusionThatDecidesNothingIsListedToo(){
		final FLEFModel model = model();
		model.addRecord(conclusion("C1", null, null, "E1", "E2"));
		model.addRecord(conclusion("C2", "E1", ConclusionReader.ENUM_PROOF_STATUS_SUPPORTED, "E1", "E2"));
		model.addRecord(conclusion("C3", null, ConclusionReader.ENUM_PROOF_STATUS_CONFLICTING_EVIDENCE, "E1", "E2"));

		final ConflictDashboard dashboard = ConflictDashboard.analyze(model, null);

		Assertions.assertEquals(1, dashboard.entries().size());
		final ConflictDashboard.Entry entry = dashboard.entries().getFirst();
		Assertions.assertEquals(ConflictDashboard.Category.CONCLUSIONS, entry.category());
		Assertions.assertEquals("C1", entry.conclusionId());
		Assertions.assertFalse(entry.resolved());
		Assertions.assertEquals(2, entry.assertions().size());
		Assertions.assertEquals("low", entry.assertions().getFirst().confidence());
		Assertions.assertFalse(entry.canBeConcluded());
	}

	@Test
	void theFiltersKeepWhatTheyAreAskedFor(){
		final FLEFModel model = model();
		model.addRecord(conclusion("C1", null, null, "E3", "E4"));
		final ConflictDashboard dashboard = ConflictDashboard.analyze(model, finder(
			conflict(ReportConflict.Kind.BIRTH, ReportConflict.Problem.DIFFERENT_DATES, new ReportConflict.Settlement("C9", "E1", null), "E1", "E2"),
			new ReportConflict(ReportConflict.Kind.ATTRIBUTE, ReportConflict.Problem.DIFFERENT_VALUES, List.of("I1"),
				List.of(new ReportConflict.Assertion("A1", "sex: M", null, "high"), new ReportConflict.Assertion("A2", "sex: F", null, null)), null)));

		Assertions.assertEquals(3, dashboard.filter(ConflictFilter.ALL, ConflictConfidence.ALL, ConflictRecordType.ALL).size());
		Assertions.assertEquals(2, dashboard.filter(ConflictFilter.UNRESOLVED, ConflictConfidence.ALL, ConflictRecordType.ALL).size());
		Assertions.assertEquals(1, dashboard.filter(ConflictFilter.RESOLVED, ConflictConfidence.ALL, ConflictRecordType.ALL).size());

		Assertions.assertEquals(1, dashboard.filter(ConflictFilter.ALL, ConflictConfidence.LOW, ConflictRecordType.ALL).size());
		Assertions.assertEquals(1, dashboard.filter(ConflictFilter.ALL, ConflictConfidence.HIGH, ConflictRecordType.ALL).size());
		Assertions.assertEquals(3, dashboard.filter(ConflictFilter.ALL, ConflictConfidence.NOT_STATED, ConflictRecordType.ALL).size());

		//the open conclusion evaluates two events: it is listed with the events
		Assertions.assertEquals(2, dashboard.filter(ConflictFilter.ALL, ConflictConfidence.ALL, ConflictRecordType.EVENT).size());
		Assertions.assertEquals(1, dashboard.filter(ConflictFilter.ALL, ConflictConfidence.ALL, ConflictRecordType.ATTRIBUTE).size());
		Assertions.assertTrue(dashboard.filter(ConflictFilter.ALL, ConflictConfidence.ALL, ConflictRecordType.NAME).isEmpty());
	}

	@Test
	void theIssueOfTheConclusionThatWouldSettleAConflict(){
		final ConflictDashboard dashboard = ConflictDashboard.analyze(model(), finder(
			conflict(ReportConflict.Kind.BIRTH, ReportConflict.Problem.DIFFERENT_DATES, null, "E1", "E2"),
			conflict(ReportConflict.Kind.DEATH, ReportConflict.Problem.SEVERAL_RECORDS, null, "E3", "E4"),
			new ReportConflict(ReportConflict.Kind.ATTRIBUTE, ReportConflict.Problem.DIFFERENT_VALUES, List.of("I1"),
				List.of(new ReportConflict.Assertion("A1", "national_id: 1", null, null), new ReportConflict.Assertion("A2", "national_id: 2", null, null)), null),
			new ReportConflict(ReportConflict.Kind.NAME, ReportConflict.Problem.DIFFERENT_NAMES, List.of("I1"),
				List.of(new ReportConflict.Assertion("I1", "birth: A", null, null), new ReportConflict.Assertion("I1", "birth: B", null, null)), null)));

		final List<String> issues = dashboard.entries().stream()
			.map(ConflictDashboard.Entry::issue)
			.toList();

		Assertions.assertTrue(issues.containsAll(List.of("birth_date", "death", "national_id", "")), issues.toString());
		Assertions.assertEquals(1, dashboard.entries().stream()
			.filter(entry -> !entry.canBeConcluded())
			.count());
	}

	@Test
	void theCitationsAreCountedByStance(){
		final FLEFModel model = model();

		final int[] counted = ConflictDashboard.citations(model, new ReportConflict.Assertion("E1", "1850", null, null));
		final int[] given = ConflictDashboard.citations(model, new ReportConflict.Assertion("E1", "1850", null, null, 3, 1));

		Assertions.assertArrayEquals(new int[]{0, 1}, counted);
		Assertions.assertArrayEquals(new int[]{3, 1}, given);
	}

	@Test
	void withoutFinderOnlyTheConclusionsAreThere(){
		Assertions.assertTrue(ConflictDashboard.analyze(model(), null).entries().isEmpty());
	}

}
