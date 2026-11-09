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
package io.github.mtrevisan.familylegacy.ui.shell;

import io.github.mtrevisan.familylegacy.reports.ReportType;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolOperation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


/** The Reports menu is in groups by what each report starts from, and has every report. */
class ReportMenuGroupsTest{

	@Test
	void theReportsAreInFourGroupsInTheOrderOfWhatTheyStartFrom(){
		final List<List<ToolOperation>> groups = ApplicationMenuBar.reportOperationGroups();

		//people and groups, places and sources, the bibliography and the research progress, the whole file
		Assertions.assertEquals(List.of(5, 3, 2, 4), groups.stream().map(List::size).toList());
		Assertions.assertEquals(List.of("AncestorReportOperation", "DescendantReportOperation", "IndividualSummaryOperation",
				"RelationshipReportOperation", "GroupSheetOperation"),
			groups.getFirst().stream().map(operation -> operation.getClass().getSimpleName()).toList());
		Assertions.assertEquals(List.of("FamilyBookOperation", "FullBibliographyOperation", "ToVerifyOperation", "SurnamesOperation"),
			groups.getLast().stream().map(operation -> operation.getClass().getSimpleName()).toList());
	}

	@Test
	void everyTypeOfReportHasItsCommandOnceAndOnlyOnce(){
		final List<ToolOperation> operations = ApplicationMenuBar.reportOperationGroups().stream()
			.flatMap(List::stream)
			.toList();

		//a new type of report must be put in a group of the menu
		Assertions.assertEquals(ReportType.values().length, operations.size());
		final Set<Class<?>> classes = new HashSet<>();
		final Set<String> names = new HashSet<>();
		for(final ToolOperation operation : operations){
			Assertions.assertTrue(classes.add(operation.getClass()));
			Assertions.assertTrue(names.add(operation.getName()), operation.getName());
		}
	}

}
