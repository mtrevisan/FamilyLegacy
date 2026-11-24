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
package io.github.mtrevisan.familylegacy.ui.tools.research;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.AuditReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.research.TaskBoard;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContexts;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;


/** The task dashboard, driven: the views, the filters, the counts, the inbox, the change of status. */
class TaskDashboardDialogTest{

	private static final LocalDate TODAY = LocalDate.of(2026, 6, 1);


	private static FLEFRecord task(final String id, final String status, final String priority, final String due, final String origin,
			final String related){
		final FLEFRecord task = FLEFRecord.createMainRecord(id, ResearchTaskReader.TYPE);
		task.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_DESCRIPTION, "Task " + id));
		task.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_STATUS, status));
		task.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_PRIORITY, priority));
		if(due != null)
			task.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_DUE_DATE, due));
		if(related != null)
			task.addChild(FLEFRecord.createChildWithTag(ResearchTaskReader.TAG_RELATED)
				.addChild(FLEFRecord.createChildWithTagAndValue("individual", related)));
		if(origin != null){
			task.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_ASSISTANT_FINDING_ID, origin));
			task.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_RATIONALE, "Why " + id));
		}
		task.addChild(FLEFRecord.createChildWithTag(AuditReader.TAG_AUDIT)
			.addChild(FLEFRecord.createChildWithTag(AuditReader.TAG_CREATION)
				.addChild(FLEFRecord.createChildWithTagAndValue(AuditReader.TAG_DATE, "2026-01-01T00:00:00Z"))));
		return task;
	}

	private static FLEFModel model(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE));
		model.addRecord(task("T1", "open", "high", "2026-05-20", "gap.no_birth|I1|", "I1"));
		model.addRecord(task("T2", "open", "low", null, "gap.no_death|I1|", "I1"));
		model.addRecord(task("T3", "in_progress", "normal", "2026-07-01", null, null));
		model.addRecord(task("T4", "completed", "normal", null, null, null));
		return model;
	}

	private static List<String> ids(final List<TaskBoard.Entry> entries){
		return entries.stream()
			.map(entry -> entry.record().getId())
			.toList();
	}

	private static TaskDashboardDialog open(final FLEFModel model, final TaskBoard.View view){
		final TaskDashboardDialog dialog = new TaskDashboardDialog(ToolContexts.custom(model, null, null, null), view, () -> TODAY);
		//a modal question cannot be answered in a test: no outcome
		dialog.setOutcomeAsker(entry -> Optional.of(""));
		return dialog;
	}


	@Test
	void theInboxHoldsTheGeneratedTasksNotYetTakenInCharge() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialog needs a display");
		final List<String> listed = new ArrayList<>();
		final AtomicReference<String> summary = new AtomicReference<>();
		final AtomicReference<Integer> completion = new AtomicReference<>();

		SwingUtilities.invokeAndWait(() -> {
			final TaskDashboardDialog dialog = open(model(), TaskBoard.View.INBOX);
			listed.addAll(ids(dialog.shownEntries()));
			summary.set(dialog.summaryText());
			completion.set(dialog.completionPercent());
			dialog.dispose();
		});

		Assertions.assertEquals(List.of("T1", "T2"), listed);
		Assertions.assertEquals("4 tasks: 2 open, 1 in progress, 1 completed, 0 abandoned, 1 overdue, 2 in the inbox", summary.get());
		//one completed out of four
		Assertions.assertEquals(25, completion.get());
	}

	@Test
	void theViewsAndTheFiltersNarrowTheList() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialog needs a display");
		final List<List<String>> listed = new ArrayList<>();

		SwingUtilities.invokeAndWait(() -> {
			final TaskDashboardDialog dialog = open(model(), TaskBoard.View.TO_DO);
			listed.add(ids(dialog.shownEntries()));
			dialog.setView(TaskBoard.View.DONE);
			listed.add(ids(dialog.shownEntries()));
			dialog.setView(TaskBoard.View.ALL);
			listed.add(ids(dialog.shownEntries()));
			dialog.setPriority("high");
			listed.add(ids(dialog.shownEntries()));
			dialog.setPriority(null);
			dialog.setOrigin(TaskBoard.Origin.MANUAL);
			listed.add(ids(dialog.shownEntries()));
			dialog.setOrigin(TaskBoard.Origin.ANY);
			dialog.setRelatedType("INDIVIDUAL");
			listed.add(ids(dialog.shownEntries()));
			dialog.setRelatedType(null);
			dialog.setOverdueOnly(true);
			listed.add(ids(dialog.shownEntries()));
			dialog.dispose();
		});

		Assertions.assertEquals(List.of("T1", "T3", "T2"), listed.get(0));
		Assertions.assertEquals(List.of("T4"), listed.get(1));
		Assertions.assertEquals(List.of("T1", "T3", "T2", "T4"), listed.get(2));
		Assertions.assertEquals(List.of("T1"), listed.get(3));
		Assertions.assertEquals(List.of("T3", "T4"), listed.get(4));
		Assertions.assertEquals(List.of("T1", "T2"), listed.get(5));
		Assertions.assertEquals(List.of("T1"), listed.get(6));
	}

	@Test
	void acceptingOrDismissingATaskTakesItOutOfTheInboxAndKeepsTheCount() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialog needs a display");
		final FLEFModel model = model();
		final List<List<String>> listed = new ArrayList<>();
		final List<String> statuses = new ArrayList<>();
		final List<Boolean> enabled = new ArrayList<>();
		final List<Integer> completion = new ArrayList<>();

		SwingUtilities.invokeAndWait(() -> {
			final TaskDashboardDialog dialog = open(model, TaskBoard.View.INBOX);
			dialog.select("T1");
			enabled.add(dialog.isInboxActionEnabled());
			Assertions.assertTrue(dialog.detailsText().contains("Why T1"), dialog.detailsText());
			dialog.accept();
			listed.add(ids(dialog.shownEntries()));
			statuses.add(ResearchTaskReader.extractStatus(model.getRecordById("T1")));

			dialog.select("T2");
			dialog.dismissSelected();
			listed.add(ids(dialog.shownEntries()));
			statuses.add(ResearchTaskReader.extractStatus(model.getRecordById("T2")));
			completion.add(dialog.completionPercent());

			//a task that is not in the inbox cannot be accepted or dismissed from it
			dialog.setView(TaskBoard.View.ALL);
			dialog.select("T3");
			enabled.add(dialog.isInboxActionEnabled());

			//completion tracking: completing a task is counted
			dialog.changeStatus(TaskBoard.list(model).get(2), ResearchTaskReader.ENUM_STATUS_COMPLETED);
			completion.add(dialog.completionPercent());
			dialog.dispose();
		});

		Assertions.assertEquals(List.of(true, false), enabled);
		Assertions.assertEquals(List.of(List.of("T2"), List.of()), listed);
		Assertions.assertEquals(List.of("in_progress", "abandoned"), statuses);
		//one completed out of the three that were not given up, then two
		Assertions.assertEquals(List.of(33, 67), completion);
		//the changes are noted in the audit, as a save from the editor is
		Assertions.assertNotNull(model.getRecordById("T1").getChildren().stream()
			.filter(child -> AuditReader.TAG_AUDIT.equals(child.getTag()))
			.findFirst()
			.orElseThrow()
			.getChildren().stream()
			.filter(child -> AuditReader.TAG_UPDATE.equals(child.getTag()))
			.findFirst()
			.orElse(null));
	}

	@Test
	void anOutcomeIsAskedWhenATaskIsOverAndWrittenInTheTask() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialog needs a display");
		final FLEFModel model = model();
		final List<String> outcomes = new ArrayList<>();

		SwingUtilities.invokeAndWait(() -> {
			final TaskDashboardDialog dialog = open(model, TaskBoard.View.ALL);
			dialog.setOutcomeAsker(entry -> Optional.of("Found in the census"));
			dialog.changeStatus(TaskBoard.list(model).get(2), ResearchTaskReader.ENUM_STATUS_COMPLETED);
			//cancelling the question leaves the task as it is
			dialog.setOutcomeAsker(entry -> Optional.empty());
			dialog.changeStatus(TaskBoard.list(model).get(0), ResearchTaskReader.ENUM_STATUS_ABANDONED);
			outcomes.add(ResearchTaskReader.extractOutcome(model.getRecordById("T3")));
			outcomes.add(ResearchTaskReader.extractStatus(model.getRecordById("T1")));
			dialog.dispose();
		});

		Assertions.assertEquals(List.of("Found in the census", "open"), outcomes);
	}

	@Test
	void aHeaderSortsTheListByUrgencyAndDueDate() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialog needs a display");
		final List<String> first = new ArrayList<>();

		SwingUtilities.invokeAndWait(() -> {
			final TaskDashboardDialog dialog = open(model(), TaskBoard.View.ALL);
			first.addAll(dialog.sortedBy(1));
			dialog.dispose();
		});

		//high, then normal (T3, T4), then low
		Assertions.assertEquals(List.of("T1", "T3", "T4", "T2"), first);
	}

}
