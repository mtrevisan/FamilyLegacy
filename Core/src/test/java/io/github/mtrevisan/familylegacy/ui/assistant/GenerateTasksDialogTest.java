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
import io.github.mtrevisan.familylegacy.assistant.task.TaskProposal;
import io.github.mtrevisan.familylegacy.assistant.task.TaskProposals;
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


/** The tasks proposed by the assistant, to review: nothing exists until the researcher confirms, and only what is ticked is created. */
class GenerateTasksDialogTest{

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-24T10:00:00Z"), ZoneOffset.UTC);


	private static FLEFModel model(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("I2", IndividualReader.TYPE));
		return model;
	}

	private static List<Diagnostic> findings(){
		return List.of(
			new Diagnostic(Diagnostic.Severity.INFORMATION, AssistantCategory.RESEARCH, "No birth for I1", "Nobody knows", List.of("I1"),
				null, DiagnosticKeys.of("gap.no_birth", List.of("I1")), List.of(), 70),
			new Diagnostic(Diagnostic.Severity.WARNING, AssistantCategory.IDENTITY, "I1 and I2", "Same name", List.of("I1", "I2"),
				null, DiagnosticKeys.of("identity.possible_duplicate", List.of("I1", "I2")), List.of(), 0));
	}


	@Test
	void onlyTheTickedTasksAreCreated() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialog needs a display");
		final FLEFModel model = model();
		final List<TaskProposal> proposals = TaskProposals.propose(findings(), model, LocalDate.of(2026, 11, 24));
		final List<Integer> created = new ArrayList<>();

		SwingUtilities.invokeAndWait(() -> {
			final GenerateTasksDialog dialog = new GenerateTasksDialog(null, model, CLOCK);
			dialog.setProposals(proposals);
			//nothing is ticked at first: creating the ticked ones does nothing
			dialog.create();
			created.add(dialog.created());
			//the researcher ticks the first one
			dialog.setChecked(0, true);
			dialog.create();
			created.add(dialog.created());
			//the row stays, as the task it became, so that it can be opened
			created.add(dialog.pending());
			created.add(dialog.taskIds().size());
		});

		Assertions.assertEquals(List.of(0, 1, 1, 1), created);
		final List<TaskBoard.Entry> tasks = TaskBoard.list(model);
		Assertions.assertEquals(1, tasks.size());
		Assertions.assertEquals(proposals.getFirst().origin(), tasks.getFirst().origin());
		Assertions.assertTrue(tasks.getFirst().isInInbox());
	}

	@Test
	void theSelectedTaskIsCreatedAndLeavesTheList() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialog needs a display");
		final FLEFModel model = model();
		final List<TaskProposal> proposals = TaskProposals.propose(findings(), model, LocalDate.of(2026, 11, 24));
		final List<Integer> counts = new ArrayList<>();

		SwingUtilities.invokeAndWait(() -> {
			final GenerateTasksDialog dialog = new GenerateTasksDialog(null, model, CLOCK);
			dialog.setProposals(proposals);
			//the first row is selected at first
			dialog.createSelected();
			counts.add(dialog.created());
			counts.add(dialog.pending());
			dialog.createSelected();
			//a row that already is a task creates nothing
			dialog.createSelected();
			counts.add(dialog.created());
			counts.add(dialog.pending());
			counts.add(dialog.proposals().size());
		});

		Assertions.assertEquals(List.of(1, 1, 2, 0, 2), counts);
		Assertions.assertEquals(2, TaskBoard.list(model).size());
	}

	@Test
	void cancellingLeavesTheFileUntouched() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialog needs a display");
		final FLEFModel model = model();
		final List<TaskProposal> proposals = TaskProposals.propose(findings(), model, LocalDate.of(2026, 11, 24));
		final List<Integer> created = new ArrayList<>();

		SwingUtilities.invokeAndWait(() -> {
			final GenerateTasksDialog dialog = new GenerateTasksDialog(null, model, CLOCK);
			dialog.setProposals(proposals);
			dialog.dispose();
			created.add(dialog.created());
		});

		Assertions.assertEquals(List.of(0), created);
		Assertions.assertTrue(model.getRecordsByType(ResearchTaskReader.TYPE).isEmpty());
	}

	@Test
	void theActionIsOfferedOnlyWithAFile(){
		final AssistantAction action = new TaskAssistantContributor(context -> List.of()).actions().getFirst();

		Assertions.assertEquals("tasks.generate", action.id());
		Assertions.assertEquals(AssistantCategory.RESEARCH, action.category());
		Assertions.assertTrue(action.isApplicable(new SwingAssistantContext(null, null, model())));
		Assertions.assertFalse(action.isApplicable(new SwingAssistantContext(null, null, null)));
	}

	@Test
	void aFindingThatAsksForWorkOffersToCreateItsTaskAfterTheOtherActions(){
		final List<io.github.mtrevisan.familylegacy.assistant.DiagnosticAction> actions = DiagnosticActions.of(findings().getFirst());

		//opening its records is Ctrl+Alt+Enter and stays what Enter does; the task takes the first key that is left
		Assertions.assertEquals(2, actions.size());
		Assertions.assertEquals(3, actions.getFirst().keySlot());
		Assertions.assertEquals(1, actions.getLast().keySlot());
		Assertions.assertSame(actions.getLast(), DiagnosticActions.forSlot(actions, 1));
		Assertions.assertSame(actions.getFirst(), DiagnosticActions.forSlot(actions, 0));
	}

	@Test
	void aFindingThatIsNotWorkOffersNoTask(){
		final Diagnostic hint = new Diagnostic(Diagnostic.Severity.INFORMATION, AssistantCategory.CONTEXT, "Hint", "x", List.of("I1"),
			null, DiagnosticKeys.of("context.candidate", List.of("I1")), List.of(), 0);

		Assertions.assertEquals(1, DiagnosticActions.of(hint).size());
	}

}
