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
package io.github.mtrevisan.familylegacy.research;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.AuditReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;


class OpenTasksTest{

	private static FLEFRecord task(final String id, final String status, final String priority, final String due, final String... questionIds){
		final FLEFRecord task = FLEFRecord.createMainRecord(id, ResearchTaskReader.TYPE);
		task.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_DESCRIPTION, "Task " + id));
		for(final String questionId : questionIds)
			task.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_QUESTION, questionId));
		if(status != null)
			task.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_STATUS, status));
		if(priority != null)
			task.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_PRIORITY, priority));
		if(due != null)
			task.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_DUE_DATE, due));
		task.addChild(FLEFRecord.createChildWithTag(AuditReader.TAG_AUDIT)
			.addChild(FLEFRecord.createChildWithTag(AuditReader.TAG_CREATION)
				.addChild(FLEFRecord.createChildWithTagAndValue(AuditReader.TAG_DATE, "2026-01-01T00:00:00Z"))));
		return task;
	}

	private static FLEFRecord question(final String id, final String targetId){
		final FLEFRecord question = FLEFRecord.createMainRecord(id, ResearchQuestionReader.TYPE);
		question.addChild(FLEFRecord.createChildWithTagAndValue(ResearchQuestionReader.TAG_TITLE, "Question " + id));
		question.addChild(FLEFRecord.createChildWithTag(ResearchQuestionReader.TAG_TARGET)
			.addChild(FLEFRecord.createChildWithTagAndValue("individual", targetId)));
		return question;
	}

	private static List<String> ids(final List<OpenTasks.Task> tasks){
		return tasks.stream()
			.map(task -> task.record().getId())
			.toList();
	}


	@Test
	void onlyTheTasksStillToBeDoneAreListed(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(task("T1", ResearchTaskReader.ENUM_STATUS_OPEN, null, null));
		model.addRecord(task("T2", ResearchTaskReader.ENUM_STATUS_IN_PROGRESS, null, null));
		model.addRecord(task("T3", ResearchTaskReader.ENUM_STATUS_COMPLETED, null, null));
		model.addRecord(task("T4", ResearchTaskReader.ENUM_STATUS_ABANDONED, null, null));
		model.addRecord(task("T5", null, null, null));

		Assertions.assertEquals(List.of("T1", "T2", "T5"), ids(OpenTasks.list(model)));
	}

	@Test
	void theEarliestDueDateComesFirstThenTheHighestPriority(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(task("T1", ResearchTaskReader.ENUM_STATUS_OPEN, ResearchTaskReader.ENUM_PRIORITY_HIGH, null));
		model.addRecord(task("T2", ResearchTaskReader.ENUM_STATUS_OPEN, ResearchTaskReader.ENUM_PRIORITY_LOW, "2026-03-01"));
		model.addRecord(task("T3", ResearchTaskReader.ENUM_STATUS_OPEN, ResearchTaskReader.ENUM_PRIORITY_LOW, "2026-02-01"));
		model.addRecord(task("T4", ResearchTaskReader.ENUM_STATUS_OPEN, ResearchTaskReader.ENUM_PRIORITY_HIGH, "2026-03-01"));
		model.addRecord(task("T5", ResearchTaskReader.ENUM_STATUS_OPEN, null, null));
		model.addRecord(task("T6", ResearchTaskReader.ENUM_STATUS_OPEN, ResearchTaskReader.ENUM_PRIORITY_LOW, null));

		//without a due date: high, none (as normal), low
		Assertions.assertEquals(List.of("T3", "T4", "T2", "T1", "T5", "T6"), ids(OpenTasks.list(model)));
	}

	@Test
	void aTaskIsOverdueTheDayAfterItsDueDate(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(task("T1", ResearchTaskReader.ENUM_STATUS_OPEN, null, "2026-05-10"));
		model.addRecord(task("T2", ResearchTaskReader.ENUM_STATUS_OPEN, null, "not a date"));

		final List<OpenTasks.Task> tasks = OpenTasks.list(model);

		Assertions.assertFalse(tasks.getFirst().isOverdue(LocalDate.of(2026, 5, 10)));
		Assertions.assertTrue(tasks.getFirst().isOverdue(LocalDate.of(2026, 5, 11)));
		Assertions.assertNull(tasks.get(1).due());
		Assertions.assertFalse(tasks.get(1).isOverdue(LocalDate.of(2026, 5, 11)));
	}

	@Test
	void aTaskConcernsTheTargetsOfItsQuestions(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(question("Q1", "I1"));
		model.addRecord(question("Q2", "I2"));
		model.addRecord(task("T1", ResearchTaskReader.ENUM_STATUS_OPEN, null, null, "Q1"));
		model.addRecord(task("T2", ResearchTaskReader.ENUM_STATUS_OPEN, null, null, "Q1", "Q2"));
		model.addRecord(task("T3", ResearchTaskReader.ENUM_STATUS_OPEN, null, null, "Q9"));

		final List<OpenTasks.Task> tasks = OpenTasks.list(model);

		Assertions.assertEquals(List.of("T1", "T2"), ids(tasks.stream().filter(task -> task.concerns("I1")).toList()));
		Assertions.assertEquals(List.of("T2"), ids(tasks.stream().filter(task -> task.concerns("I2")).toList()));
		Assertions.assertEquals(2, tasks.get(1).questions().size());
		//a question that does not exist is not listed
		Assertions.assertTrue(tasks.get(2).questions().isEmpty());
		Assertions.assertFalse(tasks.get(2).concerns("I1"));
	}

	@Test
	void aNewTaskCanServeTheQuestionsUnderWayAboutTheRecord(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(question("Q1", "I1"));
		final FLEFRecord resolved = question("Q2", "I1");
		resolved.addChild(FLEFRecord.createChildWithTagAndValue(ResearchQuestionReader.TAG_STATUS, ResearchQuestionReader.ENUM_STATUS_RESOLVED));
		model.addRecord(resolved);
		final FLEFRecord onHold = question("Q0", "I1");
		onHold.addChild(FLEFRecord.createChildWithTagAndValue(ResearchQuestionReader.TAG_STATUS, ResearchQuestionReader.ENUM_STATUS_ON_HOLD));
		model.addRecord(onHold);
		model.addRecord(question("Q3", "I2"));

		//by title: "Question Q0" before "Question Q1"; the resolved one is left out
		Assertions.assertEquals(List.of("Q0", "Q1"), OpenTasks.questionsAbout(model, "I1").stream().map(FLEFRecord::getId).toList());
		Assertions.assertEquals(List.of("Q3"), OpenTasks.questionsAbout(model, "I2").stream().map(FLEFRecord::getId).toList());
		Assertions.assertTrue(OpenTasks.questionsAbout(model, "I9").isEmpty());
		Assertions.assertTrue(OpenTasks.questionsAbout(model, null).isEmpty());
	}

	@Test
	void changingTheStatusIsRecordedInTheAudit(){
		final FLEFRecord task = task("T1", ResearchTaskReader.ENUM_STATUS_OPEN, null, null);

		Assertions.assertTrue(OpenTasks.changeStatus(task, ResearchTaskReader.ENUM_STATUS_COMPLETED));

		Assertions.assertEquals(ResearchTaskReader.ENUM_STATUS_COMPLETED, FLEFRecordHelper.getChildValue(task, ResearchTaskReader.TAG_STATUS));
		final FLEFRecord audit = FLEFRecordHelper.findChild(task, AuditReader.TAG_AUDIT);
		final List<FLEFRecord> updates = FLEFRecordHelper.findChildren(audit, AuditReader.TAG_UPDATE);
		Assertions.assertEquals(1, updates.size());
		Assertions.assertTrue(FLEFRecordHelper.getChildValue(updates.getFirst(), AuditReader.TAG_COMMENT).contains("status"));

		//the same status again changes nothing
		Assertions.assertFalse(OpenTasks.changeStatus(task, ResearchTaskReader.ENUM_STATUS_COMPLETED));
		Assertions.assertEquals(1, FLEFRecordHelper.findChildren(audit, AuditReader.TAG_UPDATE).size());
	}

	@Test
	void aTaskWithoutStatusGetsOne(){
		final FLEFRecord task = task("T1", null, null, null);

		Assertions.assertTrue(OpenTasks.changeStatus(task, ResearchTaskReader.ENUM_STATUS_IN_PROGRESS));

		Assertions.assertEquals(ResearchTaskReader.ENUM_STATUS_IN_PROGRESS, FLEFRecordHelper.getChildValue(task, ResearchTaskReader.TAG_STATUS));
	}

}
