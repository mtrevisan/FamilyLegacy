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
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;


/** The dashboard of the research tasks: what is listed, the filters, the views, the counts. */
class TaskBoardTest{

	private static final LocalDate TODAY = LocalDate.of(2026, 6, 1);


	private static FLEFRecord task(final String id, final String status, final String priority, final String due, final String origin,
			final String... related){
		final FLEFRecord task = FLEFRecord.createMainRecord(id, ResearchTaskReader.TYPE);
		task.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_DESCRIPTION, "Task " + id));
		if(status != null)
			task.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_STATUS, status));
		if(priority != null)
			task.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_PRIORITY, priority));
		if(due != null)
			task.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_DUE_DATE, due));
		for(final String target : related)
			task.addChild(FLEFRecord.createChildWithTag(ResearchTaskReader.TAG_RELATED)
				.addChild(FLEFRecord.createChildWithTagAndValue(target.startsWith("P")? "place": "individual", target)));
		if(origin != null){
			task.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_ASSISTANT_FINDING_ID, origin));
			task.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_RATIONALE, "Because " + origin));
		}
		task.addChild(FLEFRecord.createChildWithTag(AuditReader.TAG_AUDIT)
			.addChild(FLEFRecord.createChildWithTag(AuditReader.TAG_CREATION)
				.addChild(FLEFRecord.createChildWithTagAndValue(AuditReader.TAG_DATE, "2026-01-01T00:00:00Z"))));
		return task;
	}

	private static FLEFModel model(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("I2", IndividualReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("P1", PlaceReader.TYPE));
		final FLEFRecord question = FLEFRecord.createMainRecord("Q1", ResearchQuestionReader.TYPE);
		question.addChild(FLEFRecord.createChildWithTagAndValue(ResearchQuestionReader.TAG_TITLE, "Parents of I2"));
		question.addChild(FLEFRecord.createChildWithTag(ResearchQuestionReader.TAG_TARGET)
			.addChild(FLEFRecord.createChildWithTagAndValue("individual", "I2")));
		model.addRecord(question);

		//T1 generated, in the inbox, overdue, about a person and a place; T2 generated and taken in charge; T3 by hand, no status
		//and no priority; T4 completed; T5 abandoned, high; T6 by hand, of the question about I2
		model.addRecord(task("T1", "open", "high", "2026-05-20", "gap.no_birth|I1|", "I1", "P1"));
		model.addRecord(task("T2", "in_progress", "normal", "2026-07-01", "gap.no_birth|I2|"));
		model.addRecord(task("T3", null, null, null, null));
		model.addRecord(task("T4", "completed", "low", "2026-01-01", null));
		model.addRecord(task("T5", "abandoned", "high", null, null));
		final FLEFRecord t6 = task("T6", "open", "low", "2026-08-01", null);
		t6.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_QUESTION, "Q1"));
		model.addRecord(t6);
		return model;
	}

	private static List<String> ids(final List<TaskBoard.Entry> entries){
		return entries.stream()
			.map(entry -> entry.record().getId())
			.toList();
	}


	@Test
	void everyTaskIsListedWithItsDefaults(){
		final List<TaskBoard.Entry> entries = TaskBoard.list(model());

		Assertions.assertEquals(List.of("T1", "T2", "T3", "T4", "T5", "T6"), ids(entries));
		final TaskBoard.Entry t3 = entries.get(2);
		Assertions.assertEquals("open", t3.status());
		Assertions.assertEquals("normal", t3.priority());
		Assertions.assertNull(t3.due());
		Assertions.assertFalse(t3.isGenerated());
		Assertions.assertTrue(entries.get(0).isGenerated());
		Assertions.assertEquals("Because gap.no_birth|I1|", entries.get(0).rationale());
	}

	@Test
	void aTaskIsAboutWhatItNamesAndWhatItsQuestionsAreAbout(){
		final FLEFModel model = model();
		final List<TaskBoard.Entry> entries = TaskBoard.list(model);

		Assertions.assertEquals(Set.of("I1", "P1"), entries.get(0).relatedIds());
		Assertions.assertEquals(Set.of("I2"), entries.get(5).relatedIds());

		//the "only the selected record" filter of the panel finds the tasks that name the record too
		final List<OpenTasks.Task> open = OpenTasks.list(model);
		Assertions.assertTrue(open.stream().anyMatch(task -> task.record().getId().equals("T1") && task.concerns("P1")));
	}

	@Test
	void theViewsSplitTheInboxWhatIsToDoAndWhatIsOver(){
		final FLEFModel model = model();
		final List<TaskBoard.Entry> entries = TaskBoard.list(model);

		Assertions.assertEquals(List.of("T1"), ids(TaskBoard.filter(entries, TaskBoard.View.INBOX.filter(), model, TODAY)));
		Assertions.assertEquals(List.of("T1", "T2", "T6", "T3"), ids(TaskBoard.filter(entries, TaskBoard.View.TO_DO.filter(), model, TODAY)));
		Assertions.assertEquals(List.of("T4", "T5"), ids(TaskBoard.filter(entries, TaskBoard.View.DONE.filter(), model, TODAY)));
		Assertions.assertEquals(6, TaskBoard.filter(entries, TaskBoard.View.ALL.filter(), model, TODAY).size());
	}

	@Test
	void tasksStillToBeDoneComeFirstByDueDateThenPriority(){
		final FLEFModel model = model();

		Assertions.assertEquals(List.of("T1", "T2", "T6", "T3", "T4", "T5"),
			ids(TaskBoard.filter(TaskBoard.list(model), TaskBoard.Filter.ALL, model, TODAY)));
	}

	@Test
	void theFiltersNarrowTheList(){
		final FLEFModel model = model();
		final List<TaskBoard.Entry> entries = TaskBoard.list(model);

		Assertions.assertEquals(List.of("T1", "T5"), ids(TaskBoard.filter(entries, TaskBoard.Filter.ALL.withPriority("high"), model, TODAY)));
		Assertions.assertEquals(List.of("T1", "T2"), ids(TaskBoard.filter(entries,
			TaskBoard.Filter.ALL.withOrigin(TaskBoard.Origin.GENERATED), model, TODAY)));
		Assertions.assertEquals(List.of("T6", "T3", "T4", "T5"), ids(TaskBoard.filter(entries,
			TaskBoard.Filter.ALL.withOrigin(TaskBoard.Origin.MANUAL), model, TODAY)));
		//only what is still to be done and late: T4 is late too, but it is completed
		Assertions.assertEquals(List.of("T1"), ids(TaskBoard.filter(entries, TaskBoard.Filter.ALL.withOverdueOnly(true), model, TODAY)));
		Assertions.assertEquals(List.of("T1"), ids(TaskBoard.filter(entries, TaskBoard.Filter.ALL.withRelatedType("PLACE"), model, TODAY)));
		//T6 is about I2 through its question
		Assertions.assertEquals(List.of("T1", "T6"), ids(TaskBoard.filter(entries, TaskBoard.Filter.ALL.withRelatedType("INDIVIDUAL"), model, TODAY)));
		Assertions.assertEquals(List.of("T2"), ids(TaskBoard.filter(entries, TaskBoard.Filter.ALL.withText("task t2"), model, TODAY)));
		//the text is looked for in the rationale too
		Assertions.assertEquals(List.of("T1"), ids(TaskBoard.filter(entries, TaskBoard.Filter.ALL.withText("BECAUSE gap.no_birth|I1"), model, TODAY)));
	}

	@Test
	void theStatisticsTellHowFarTheWorkIs(){
		final TaskBoard.Statistics statistics = TaskBoard.statistics(TaskBoard.list(model()), TODAY);

		Assertions.assertEquals(6, statistics.total());
		Assertions.assertEquals(3, statistics.open());
		Assertions.assertEquals(1, statistics.inProgress());
		Assertions.assertEquals(1, statistics.completed());
		Assertions.assertEquals(1, statistics.abandoned());
		Assertions.assertEquals(1, statistics.overdue());
		Assertions.assertEquals(1, statistics.inbox());
		//one completed out of the five tasks that were not given up
		Assertions.assertEquals(0.2, statistics.completion(), 1e-9);
		Assertions.assertEquals(0., TaskBoard.statistics(List.of(), TODAY).completion());
	}

	@Test
	void theOriginsOfTheTasksAreKnownWhateverTheirState(){
		final FLEFModel model = model();
		OpenTasks.changeStatus(model.getRecordById("T1"), ResearchTaskReader.ENUM_STATUS_ABANDONED);

		Assertions.assertEquals(Set.of("gap.no_birth|I1|", "gap.no_birth|I2|"), TaskBoard.origins(model));
		Assertions.assertFalse(TaskBoard.list(model).getFirst().isInInbox());
	}

}
