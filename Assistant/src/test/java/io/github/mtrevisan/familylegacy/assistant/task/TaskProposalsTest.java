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
package io.github.mtrevisan.familylegacy.assistant.task;

import io.github.mtrevisan.familylegacy.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.assistant.DiagnosticKeys;
import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.FLEFValidator;
import io.github.mtrevisan.familylegacy.io.FLEFWriter;
import io.github.mtrevisan.familylegacy.io.merger.RecordDeleter;
import io.github.mtrevisan.familylegacy.io.merger.manual.MergeGrammar;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.AuditReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchActivityReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.research.OpenTasks;
import io.github.mtrevisan.familylegacy.research.TaskBoard;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;


/** The tasks that the findings ask for: which findings, what the task says, that it is proposed once, and that it is a good record. */
class TaskProposalsTest{

	private static final LocalDate TODAY = LocalDate.of(2026, 11, 24);
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-24T10:00:00Z"), ZoneOffset.UTC);


	private static FLEFModel model(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("I2", IndividualReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("S1", SourceReader.TYPE));
		final FLEFRecord question = FLEFRecord.createMainRecord("RQ1", ResearchQuestionReader.TYPE);
		question.addChild(FLEFRecord.createChildWithTagAndValue(ResearchQuestionReader.TAG_TITLE, "Parents of I1"));
		model.addRecord(question);
		model.addRecord(FLEFRecord.createMainRecord("RA1", ResearchActivityReader.TYPE));
		return model;
	}

	private static Diagnostic finding(final Diagnostic.Severity severity, final String kind, final double rank, final String... ids){
		return new Diagnostic(severity, AssistantCategory.RESEARCH, "Title of " + kind, "Why " + kind, List.of(ids), null,
			DiagnosticKeys.of(kind, List.of(ids)), List.of(), rank);
	}


	@Test
	void onlyTheFindingsThatAskForWorkBecomeTasks(){
		final List<Diagnostic> findings = List.of(
			finding(Diagnostic.Severity.INFORMATION, "gap.unsourced_vital_event", 50, "I1"),
			finding(Diagnostic.Severity.ERROR, "conclusion.conflicting_events", 0, "I1"),
			finding(Diagnostic.Severity.WARNING, "identity.possible_duplicate", 0, "I1", "I2"),
			finding(Diagnostic.Severity.INFORMATION, "gap.no_birth", 80, "I2"),
			finding(Diagnostic.Severity.INFORMATION, "planner.next_step", 10, "RQ1"),
			finding(Diagnostic.Severity.WARNING, "chronology.child_before_parent", 0, "I1"),
			finding(Diagnostic.Severity.INFORMATION, "relationship.marriage_without_relationship", 0, "I1", "I2"),
			//what is not work
			finding(Diagnostic.Severity.WARNING, "gap.overdue_task", 90, "I1"),
			finding(Diagnostic.Severity.INFORMATION, "context.candidate", 0, "I1"),
			finding(Diagnostic.Severity.WARNING, "relationship.generic_child", 0, "I1"),
			finding(Diagnostic.Severity.WARNING, "chronology.date_not_checked", 0, "I1"),
			new Diagnostic(Diagnostic.Severity.WARNING, AssistantCategory.RESEARCH, "No key", "x", List.of("I1")));

		final List<TaskProposal> proposals = TaskProposals.propose(findings, model(), TODAY);

		Assertions.assertEquals(List.of(TaskTopic.MISSING_SOURCE, TaskTopic.CONFLICT, TaskTopic.DUPLICATE, TaskTopic.PARENTAGE,
			TaskTopic.ARCHIVE_SEARCH, TaskTopic.CHRONOLOGY, TaskTopic.ARCHIVE_SEARCH), proposals.stream().map(TaskProposal::topic).toList());
	}

	@Test
	void aProposalSaysWhatToDoWhyAndAboutWhat(){
		final TaskProposal proposal = TaskProposals.propose(
			List.of(finding(Diagnostic.Severity.ERROR, "identity.possible_duplicate", 0, "I1", "I2")), model(), TODAY).getFirst();

		Assertions.assertTrue(proposal.description().contains("Title of identity.possible_duplicate"), proposal.description());
		Assertions.assertEquals("Why identity.possible_duplicate", proposal.rationale());
		Assertions.assertEquals("high", proposal.priority());
		Assertions.assertEquals("2026-12-08", proposal.dueDate());
		Assertions.assertEquals(List.of("I1", "I2"), proposal.relatedIds());
		Assertions.assertTrue(proposal.origin().startsWith("identity.possible_duplicate|I1,I2|"), proposal.origin());
	}

	@Test
	void thePriorityFollowsTheSeverityAndTheUsefulness(){
		final List<TaskProposal> proposals = TaskProposals.propose(List.of(
			finding(Diagnostic.Severity.ERROR, "conclusion.a", 0, "I1"),
			finding(Diagnostic.Severity.WARNING, "conclusion.b", 0, "I1"),
			finding(Diagnostic.Severity.INFORMATION, "gap.no_birth", 80, "I1"),
			finding(Diagnostic.Severity.INFORMATION, "gap.no_death", 20, "I1")), model(), TODAY);

		Assertions.assertEquals(List.of("high", "normal", "normal", "low"), proposals.stream().map(TaskProposal::priority).toList());
	}

	@Test
	void questionsAndActivitiesAreNotTargetsOfTheTask(){
		final TaskProposal proposal = TaskProposals.propose(List.of(
			finding(Diagnostic.Severity.INFORMATION, "gap.no_follow_up", 40, "RA1", "RQ1", "I1", "X9")), model(), TODAY).getFirst();

		//the question is served by the task, the only activity is the one it follows, a record that is gone is left out
		Assertions.assertEquals(List.of("I1"), proposal.relatedIds());
		Assertions.assertEquals(List.of("RQ1"), proposal.questionIds());
		Assertions.assertEquals("RA1", proposal.activityId());
	}

	@Test
	void aFindingIsProposedOnceWhateverBecameOfItsTask(){
		final FLEFModel model = model();
		final Diagnostic finding = finding(Diagnostic.Severity.WARNING, "conclusion.conflicting_events", 0, "I1");

		final TaskProposal proposal = TaskProposals.propose(List.of(finding, finding), model, TODAY).getFirst();
		Assertions.assertEquals(1, TaskProposals.propose(List.of(finding, finding), model, TODAY).size());

		final FLEFRecord task = TaskProposals.create(model, proposal, CLOCK);
		Assertions.assertTrue(TaskProposals.propose(List.of(finding), model, TODAY).isEmpty());

		OpenTasks.changeStatus(task, ResearchTaskReader.ENUM_STATUS_COMPLETED);
		Assertions.assertTrue(TaskProposals.propose(List.of(finding), model, TODAY).isEmpty());
		OpenTasks.changeStatus(task, ResearchTaskReader.ENUM_STATUS_ABANDONED);
		Assertions.assertTrue(TaskProposals.propose(List.of(finding), model, TODAY).isEmpty());
	}

	@Test
	void theCreatedTaskIsOpenAndInTheInbox(){
		final FLEFModel model = model();
		final TaskProposal proposal = TaskProposals.propose(
			List.of(finding(Diagnostic.Severity.WARNING, "gap.no_birth", 0, "I1", "RQ1")), model, TODAY).getFirst();

		final FLEFRecord task = TaskProposals.create(model, proposal, CLOCK);

		Assertions.assertSame(task, model.getRecordById(task.getId()));
		Assertions.assertTrue(task.getId().startsWith(ResearchTaskReader.ID_PREFIX));
		Assertions.assertEquals(proposal.description(), ResearchTaskReader.extractDescription(task));
		Assertions.assertEquals("open", ResearchTaskReader.extractStatus(task));
		Assertions.assertEquals("normal", ResearchTaskReader.extractPriority(task));
		Assertions.assertEquals(proposal.rationale(), ResearchTaskReader.extractRationale(task));
		Assertions.assertEquals(proposal.origin(), ResearchTaskReader.extractAssistantFindingId(task));
		Assertions.assertEquals("2026-11-24T10:00:00Z", FLEFRecordHelper.getChildValue(task, FLEFRecordHelper.composePath(AuditReader.TAG_AUDIT, AuditReader.TAG_CREATION_DATE)));

		final TaskBoard.Entry entry = TaskBoard.list(model).getFirst();
		Assertions.assertTrue(entry.isInInbox());
		Assertions.assertEquals(Set.of("I1"), entry.relatedIds());
		Assertions.assertEquals(List.of("RQ1"), entry.questions().stream().map(FLEFRecord::getId).toList());
		//the tasks panel finds it from the record it is about
		Assertions.assertTrue(OpenTasks.list(model).getFirst().concerns("I1"));
	}

	@Test
	void theCreatedTaskIsAValidRecordThatSurvivesAFileAndTheDeletionOfWhatItIsAbout(){
		final FLEFModel model = model();
		final TaskProposal proposal = TaskProposals.propose(
			List.of(finding(Diagnostic.Severity.WARNING, "identity.possible_duplicate", 0, "I1", "I2")), model, TODAY).getFirst();
		final String taskId = TaskProposals.create(model, proposal, CLOCK).getId();

		final String text = FLEFWriter.create().writeToString(model);
		final FLEFModel reread = new FLEFParser().parse(text);
		final FLEFRecord task = reread.getRecordById(taskId);
		Assertions.assertNotNull(task, text);
		Assertions.assertEquals(proposal.origin(), ResearchTaskReader.extractAssistantFindingId(task));
		Assertions.assertEquals(Set.of("I1", "I2"), TaskBoard.list(reread).getFirst().relatedIds());

		//the grammar knows the new fields
		final List<FLEFValidator.ValidationError> errors = new FLEFValidator(MergeGrammar.instance()).validateAllStructured(model).stream()
			.filter(error -> taskId.equals(error.recordId()))
			.toList();
		Assertions.assertTrue(errors.isEmpty(), errors.toString());

		//deleting a record the task is about removes it from the task, which stays
		new RecordDeleter(MergeGrammar.instance()).delete(model, "I1");
		Assertions.assertNotNull(model.getRecordById(taskId));
		Assertions.assertEquals(Set.of("I2"), TaskBoard.list(model).getFirst().relatedIds());
	}

}
