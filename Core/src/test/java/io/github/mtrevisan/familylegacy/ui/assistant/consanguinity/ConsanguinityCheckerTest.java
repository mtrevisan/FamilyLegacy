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
package io.github.mtrevisan.familylegacy.ui.assistant.consanguinity;

import io.github.mtrevisan.familylegacy.consanguinity.ConsanguinityCalculator;
import io.github.mtrevisan.familylegacy.consanguinity.ConsanguinityReport;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;


/** What the consanguinity report becomes in the Research Assistant. */
class ConsanguinityCheckerTest{

	private static ConsanguinityReport.ConsanguineousUnion union(final String first, final String second, final double kinship,
			final int children){
		final List<String> childIds = new ArrayList<>();
		for(int i = 0; i < children; i ++)
			childIds.add("C" + i);
		return new ConsanguinityReport.ConsanguineousUnion(first, "Name " + first, second, "Name " + second, List.of("R-" + first),
			childIds, kinship, 2. * kinship, List.of(new ConsanguinityCalculator.CommonAncestor("G1", 2, 2)));
	}

	private static ConsanguinityReport report(final List<ConsanguinityReport.ConsanguineousUnion> unions,
			final List<ConsanguinityReport.InbredIndividual> inbred){
		return new ConsanguinityReport(unions, inbred, 1. / 256., 1. / 64.);
	}


	@Test
	void firstCousinsAreAWarningAndTheMoreDistantOnesInformation(){
		final List<Diagnostic> findings = ConsanguinityChecker.findings(report(List.of(
			union("A", "B", 1. / 16., 1),
			union("C", "D", 1. / 64., 0)), List.of()), id -> id);

		Assertions.assertEquals(2, findings.size());
		Assertions.assertEquals(Diagnostic.Severity.WARNING, findings.get(0).severity());
		Assertions.assertEquals(Diagnostic.Severity.INFORMATION, findings.get(1).severity());
	}

	@Test
	void aFindingNamesThePartnersAndOpensTheirRecordsAndTheUnion(){
		final Diagnostic finding = ConsanguinityChecker.findings(report(List.of(union("A", "B", 1. / 16., 2)), List.of()), id -> "Gino")
			.getFirst();

		Assertions.assertTrue(finding.title().contains("Name A") && finding.title().contains("Name B"), finding.title());
		Assertions.assertTrue(finding.explanation().contains("first cousins"), finding.explanation());
		Assertions.assertTrue(finding.explanation().contains("6.25%"), finding.explanation());
		Assertions.assertTrue(finding.explanation().contains("Gino"), finding.explanation());
		Assertions.assertTrue(finding.explanation().contains("not a conclusion"), "it never says more than a possibility");
		Assertions.assertEquals(List.of("A", "B", "R-A"), finding.recordIds());
		Assertions.assertNull(finding.action(), "nothing to create: the assistant only reports");
	}

	@Test
	void theFindingsOfTheClosestRelativesComeFirst(){
		final List<Diagnostic> findings = ConsanguinityChecker.findings(report(List.of(
			union("A", "B", 1. / 4., 0),
			union("C", "D", 1. / 16., 0)), List.of()), id -> id);

		Assertions.assertTrue(findings.get(0).rank() > findings.get(1).rank());
	}

	@Test
	void aFindingCanBeIgnoredAndComesBackWhenTheChildrenChange(){
		final String withoutChildren = ConsanguinityChecker.findings(report(List.of(union("A", "B", 1. / 16., 0)), List.of()), id -> id)
			.getFirst().key();
		final String withChildren = ConsanguinityChecker.findings(report(List.of(union("A", "B", 1. / 16., 1)), List.of()), id -> id)
			.getFirst().key();

		Assertions.assertNotNull(withoutChildren);
		Assertions.assertNotEquals(withoutChildren, withChildren);
	}

	@Test
	void aVillageOfRelativesDoesNotFillThePalette(){
		final List<ConsanguinityReport.ConsanguineousUnion> unions = new ArrayList<>();
		for(int i = 0; i < 40; i ++)
			unions.add(union("A" + i, "B" + i, 1. / 256., 0));

		final List<Diagnostic> findings = ConsanguinityChecker.findings(report(unions, List.of()), id -> id);

		Assertions.assertEquals(ConsanguinityChecker.MAX_UNIONS + 1, findings.size());
		final Diagnostic others = findings.getLast();
		Assertions.assertTrue(others.title().contains("15"), others.title());
		Assertions.assertEquals(30, others.recordIds().size());
	}

	@Test
	void theInbredIndividualsAreOneFindingWithTheMostInbredNamed(){
		final List<ConsanguinityReport.InbredIndividual> inbred = List.of(
			new ConsanguinityReport.InbredIndividual("Z1", "Zeno", 0.25, List.of("X", "Y"),
				List.of(new ConsanguinityCalculator.CommonAncestor("G1", 1, 1))),
			new ConsanguinityReport.InbredIndividual("Z2", "Zita", 1. / 16., List.of("X2", "Y2"),
				List.of(new ConsanguinityCalculator.CommonAncestor("G2", 2, 2))));

		final List<Diagnostic> findings = ConsanguinityChecker.findings(report(List.of(), inbred), id -> id);

		Assertions.assertEquals(1, findings.size());
		final Diagnostic finding = findings.getFirst();
		Assertions.assertEquals(Diagnostic.Severity.INFORMATION, finding.severity());
		Assertions.assertTrue(finding.title().contains("2"), finding.title());
		Assertions.assertTrue(finding.explanation().contains("Zeno (25.00%)"), finding.explanation());
		Assertions.assertEquals(List.of("Z1", "Z2"), finding.recordIds());
	}

	@Test
	void nothingToReportNoFindings(){
		Assertions.assertTrue(ConsanguinityChecker.findings(report(List.of(), List.of()), id -> id).isEmpty());
	}

}
