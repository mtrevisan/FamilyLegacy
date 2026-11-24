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
package io.github.mtrevisan.familylegacy.ui.dialogs;

import io.github.mtrevisan.familylegacy.assistant.task.TaskProposal;
import io.github.mtrevisan.familylegacy.assistant.task.TaskProposals;
import io.github.mtrevisan.familylegacy.assistant.task.TaskTopic;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.research.TaskBoard;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.ResearchTaskRecordDialog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;


/** The editor of a research task: the records it is about, the rationale and the origin survive an edit. */
class ResearchTaskEditorTest{

	@Test
	void aGeneratedTaskKeepsWhatItIsAboutItsRationaleAndItsOriginWhenSaved(){
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The editor needs a display");
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("P1", PlaceReader.TYPE));
		final FLEFRecord task = TaskProposals.create(model, new TaskProposal("gap.no_birth|I1|", TaskTopic.PARENTAGE, "Find the parents",
			"Nobody knows them", "normal", "2026-12-24", List.of("I1", "P1"), List.of(), null),
			Clock.fixed(Instant.parse("2026-11-24T10:00:00Z"), ZoneOffset.UTC));

		final ResearchTaskRecordDialog dialog = ResearchTaskRecordDialog.createEdit(null, model, task);
		dialog.save();

		Assertions.assertTrue(dialog.isSaved());
		final FLEFRecord saved = model.getRecordById(task.getId());
		Assertions.assertEquals("Find the parents", ResearchTaskReader.extractDescription(saved));
		Assertions.assertEquals("Nobody knows them", ResearchTaskReader.extractRationale(saved));
		Assertions.assertEquals("gap.no_birth|I1|", ResearchTaskReader.extractAssistantFindingId(saved));
		Assertions.assertEquals(2, FLEFRecordHelper.findChildren(saved, ResearchTaskReader.TAG_RELATED).size());
		Assertions.assertEquals(Set.of("I1", "P1"), TaskBoard.list(model).getFirst().relatedIds());
	}

}
