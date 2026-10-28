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
package io.github.mtrevisan.familylegacy.io.model;

import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;


class ModelHistoryTest{

	private static FLEFModel model(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("P1", "place")
			.addChild(FLEFRecord.createChildWithTagAndValue("name.value", "Treviso")));
		return model;
	}


	private static java.util.List<String> summarize(final ModelHistory.Entry entry){
		return entry.changes().stream()
			.map(change -> change.id() + ":" + change.kind())
			.toList();
	}


	@Test
	void undoAndRedoRestoreTheModel(){
		final FLEFModel model = model();
		final ModelHistory history = new ModelHistory(model);

		model.getRecordById("P1").getChildren().getFirst().setValue("Venezia");
		model.addRecord(FLEFRecord.createMainRecord("P2", "place"));

		Assertions.assertTrue(history.undo());
		Assertions.assertEquals(1, model.getRecordCount());
		Assertions.assertEquals("Treviso", model.getRecordById("P1").getChildren().getFirst().getValue());
		Assertions.assertFalse(model.hasRecord("P2"));
		Assertions.assertFalse(history.canUndo());

		Assertions.assertTrue(history.redo());
		Assertions.assertEquals(2, model.getRecordCount());
		Assertions.assertEquals("Venezia", model.getRecordById("P1").getChildren().getFirst().getValue());
		Assertions.assertTrue(model.hasRecord("P2"));
		Assertions.assertFalse(history.canRedo());
	}

	@Test
	void eachCheckpointIsAStepAndANewChangeClearsRedo(){
		final FLEFModel model = model();
		final ModelHistory history = new ModelHistory(model);

		model.addRecord(FLEFRecord.createMainRecord("P2", "place"));
		history.checkpoint();
		model.addRecord(FLEFRecord.createMainRecord("P3", "place"));
		history.checkpoint();

		history.undo();
		Assertions.assertTrue(model.hasRecord("P2"));
		Assertions.assertFalse(model.hasRecord("P3"));

		model.addRecord(FLEFRecord.createMainRecord("P4", "place"));
		Assertions.assertFalse(history.canRedo());
		history.undo();
		Assertions.assertTrue(model.hasRecord("P2"));
		Assertions.assertFalse(model.hasRecord("P4"));
	}

	@Test
	void childrenOrderIsAChange(){
		final FLEFModel model = model();
		final FLEFRecord record = model.getRecordById("P1");
		record.addChild(FLEFRecord.createChildWithTagAndValue("note", "a"));
		final ModelHistory history = new ModelHistory(model);

		Collections.reverse(record.getChildren());

		Assertions.assertTrue(history.canUndo());
		history.undo();
		Assertions.assertEquals("name.value", model.getRecordById("P1").getChildren().getFirst().getTag());
	}

	@Test
	void theOriginOfTheChangeIsReported(){
		final FLEFModel model = model();
		final ModelHistory history = new ModelHistory(model);
		Assertions.assertNull(history.peekUndoOrigin());

		model.addRecord(FLEFRecord.createMainRecord("P2", "place"));
		history.checkpoint(ModelHistory.Origin.DIALOG);

		Assertions.assertEquals(ModelHistory.Origin.DIALOG, history.peekUndoOrigin());
	}

	@Test
	void recentChangesListTouchedRecordsNewestFirstAndFlagUndoneSteps(){
		final FLEFModel model = model();
		final ModelHistory history = new ModelHistory(model);

		model.getRecordById("P1").getChildren().getFirst().setValue("Venezia");
		model.addRecord(FLEFRecord.createMainRecord("P2", "place"));
		history.checkpoint();
		model.getRecords().removeIf(record -> "P2".equals(record.getId()));
		history.checkpoint();

		java.util.List<ModelHistory.Entry> entries = history.recentChanges();
		Assertions.assertEquals(2, entries.size());
		Assertions.assertEquals(java.util.List.of("P2:REMOVED"), summarize(entries.get(0)));
		Assertions.assertEquals(java.util.List.of("P1:MODIFIED", "P2:ADDED"), summarize(entries.get(1)));
		//the record of a removed change is still readable
		Assertions.assertEquals("P2", entries.get(0).changes().getFirst().record().getId());
		Assertions.assertNotNull(entries.get(0).changes().getFirst().before());
		Assertions.assertNull(entries.get(0).changes().getFirst().after());
		//a modified change keeps both states
		Assertions.assertEquals("Treviso", entries.get(1).changes().getFirst().before().getChildren().getFirst().getValue());
		Assertions.assertEquals("Venezia", entries.get(1).changes().getFirst().after().getChildren().getFirst().getValue());
		//the record of a modified change is the one after the change
		Assertions.assertEquals("Venezia", entries.get(1).changes().getFirst().record().getChildren().getFirst().getValue());
		Assertions.assertFalse(entries.get(0).undone());

		history.undo();
		entries = history.recentChanges();
		Assertions.assertEquals(2, entries.size());
		Assertions.assertTrue(entries.get(0).undone());
		Assertions.assertFalse(entries.get(1).undone());
	}

}
