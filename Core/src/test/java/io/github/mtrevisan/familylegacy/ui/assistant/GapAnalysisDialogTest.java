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
package io.github.mtrevisan.familylegacy.ui.assistant;

import io.github.mtrevisan.familylegacy.assistant.AssistantAction;
import io.github.mtrevisan.familylegacy.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.assistant.DiagnosticKeys;
import io.github.mtrevisan.familylegacy.assistant.gap.GapAnalysis;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.research.TaskBoard;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;


/** The gap analysis: the gaps by kind, the task each one proposes, and nothing created until the researcher confirms. */
class GapAnalysisDialogTest{

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-25T10:00:00Z"), ZoneOffset.UTC);
	private static final LocalDate TODAY = LocalDate.of(2026, 11, 25);


	private static FLEFModel model(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("I2", IndividualReader.TYPE));
		return model;
	}

	private static Diagnostic finding(final String kind, final String... ids){
		return new Diagnostic(Diagnostic.Severity.INFORMATION, AssistantCategory.RESEARCH, "Title of " + kind, "Explanation of " + kind,
			List.of(ids), null, DiagnosticKeys.of(kind, List.of(ids)), List.of(), 70);
	}

	private static List<GapAnalysisDialog.Row> rows(final FLEFModel model){
		return GapAnalysisContributor.rows(List.of(
			finding("gap.no_birth", "I1"),
			finding("gap.no_death_certain", "I2"),
			finding("chronology.child_before_parent", "I1")), model, TODAY);
	}


	@Test
	void onlyTheGapsAreListedAndEachOneHasItsTask(){
		final List<GapAnalysisDialog.Row> rows = rows(model());

		Assertions.assertEquals(2, rows.size());
		Assertions.assertEquals(GapAnalysis.Gap.MISSING_BIRTH, rows.getFirst().entry().gap());
		Assertions.assertNotNull(rows.getFirst().task());
		Assertions.assertFalse(rows.getFirst().exists());
		Assertions.assertEquals(GapAnalysis.Gap.MISSING_DEATH, rows.getLast().entry().gap());
	}

	@Test
	void aGapThatIsAlreadyATaskIsNotProposedAgain(){
		final FLEFModel model = model();
		final List<GapAnalysisDialog.Row> first = rows(model);
		io.github.mtrevisan.familylegacy.assistant.task.TaskProposals.create(model, first.getFirst().task(), CLOCK);

		final List<GapAnalysisDialog.Row> second = rows(model);

		Assertions.assertEquals(2, second.size());
		Assertions.assertNull(second.getFirst().task());
		Assertions.assertTrue(second.getFirst().exists());
		Assertions.assertNotNull(second.getLast().task());
	}

	@Test
	void theDetailsHaveTheSearchesAndTheRepositories(){
		final String text = GapAnalysisDialog.describe(rows(model()).getFirst());

		Assertions.assertTrue(text.contains("Explanation of gap.no_birth"), text);
		Assertions.assertTrue(text.contains("•"), text);
		Assertions.assertTrue(text.split("\n\n").length >= 4, text);
	}

	@Test
	void onlyTheTickedTasksAreCreated() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialog needs a display");
		final FLEFModel model = model();
		final List<GapAnalysisDialog.Row> rows = rows(model);
		final List<Integer> created = new ArrayList<>();
		final List<Integer> shown = new ArrayList<>();

		SwingUtilities.invokeAndWait(() -> {
			final GapAnalysisDialog dialog = new GapAnalysisDialog(null, model, CLOCK);
			dialog.setRows(rows);
			dialog.showOnly(GapAnalysis.Gap.MISSING_DEATH);
			shown.add(dialog.shown().size());
			dialog.showOnly(null);
			shown.add(dialog.shown().size());
			//nothing is ticked at first; the researcher ticks the first one
			dialog.create();
			created.add(dialog.created());
			dialog.setChecked(rows.getFirst(), true);
			dialog.create();
			created.add(dialog.created());
			//the gap stays, as a task that exists
			created.add(dialog.rows().size());
			created.add(dialog.rows().getFirst().exists()? 1: 0);
			//and says which task it is, to open it
			created.add(dialog.rows().getFirst().taskId() != null? 1: 0);
		});

		Assertions.assertEquals(List.of(1, 2), shown);
		Assertions.assertEquals(List.of(0, 1, 2, 1, 1), created);
		final List<TaskBoard.Entry> tasks = TaskBoard.list(model);
		Assertions.assertEquals(1, tasks.size());
		Assertions.assertEquals(rows.getFirst().task().origin(), tasks.getFirst().origin());
	}

	@Test
	void theSelectedGapGetsItsTaskAndTheNextOneIsSelected() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialog needs a display");
		final FLEFModel model = model();
		final List<GapAnalysisDialog.Row> rows = rows(model);
		final List<Integer> counts = new ArrayList<>();

		SwingUtilities.invokeAndWait(() -> {
			final GapAnalysisDialog dialog = new GapAnalysisDialog(null, model, CLOCK);
			dialog.setRows(rows);
			dialog.createSelected();
			dialog.createSelected();
			//a gap that already is a task creates nothing
			dialog.createSelected();
			counts.add(dialog.created());
		});

		Assertions.assertEquals(List.of(2), counts);
		Assertions.assertEquals(2, TaskBoard.list(model).size());
	}

	@Test
	void cancellingLeavesTheFileUntouched() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialog needs a display");
		final FLEFModel model = model();
		final List<GapAnalysisDialog.Row> rows = rows(model);

		SwingUtilities.invokeAndWait(() -> {
			final GapAnalysisDialog dialog = new GapAnalysisDialog(null, model, CLOCK);
			dialog.setRows(rows);
			dialog.dispose();
		});

		Assertions.assertTrue(model.getRecordsByType(ResearchTaskReader.TYPE).isEmpty());
	}

	@Test
	void theActionIsOfferedOnlyWithAFile(){
		final AssistantAction action = new GapAnalysisContributor(context -> List.of()).actions().getFirst();

		Assertions.assertEquals("gaps.analysis", action.id());
		Assertions.assertEquals(AssistantCategory.RESEARCH, action.category());
		Assertions.assertTrue(action.isApplicable(new SwingAssistantContext(null, null, model())));
		Assertions.assertFalse(action.isApplicable(new SwingAssistantContext(null, null, null)));
	}

}
