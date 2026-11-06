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

import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;


/** The copy of the model that a thread away from the one that edits can read: it never changes, and it costs what changed. */
class ModelHistoryReadOnlyViewTest{

	private static FLEFModel model(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("P1", PlaceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue("name.value", "Treviso")));
		model.addRecord(FLEFRecord.createMainRecord("P2", PlaceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue("name.value", "Padova")));
		return model;
	}

	private static String nameOf(final FLEFModel model, final String id){
		return model.getRecordById(id).getChildren().getFirst().getValue();
	}


	@Test
	void theViewHasTheContentOfTheModel(){
		final FLEFModel model = model();
		final FLEFModel view = new ModelHistory(model).readOnlyView();

		Assertions.assertEquals(2, view.getRecordCount());
		Assertions.assertEquals("Treviso", nameOf(view, "P1"));
		Assertions.assertEquals("Padova", nameOf(view, "P2"));
		Assertions.assertEquals(2, view.getRecordsByType(PlaceReader.TYPE).size(), "the lookups by type work");
		Assertions.assertEquals(model.getHeader(), view.getHeader());
	}

	@Test
	void theViewIsNotAffectedByTheEditsThatFollow(){
		final FLEFModel model = model();
		final ModelHistory history = new ModelHistory(model);
		final FLEFModel view = history.readOnlyView();

		//the model is edited in place, from anywhere: changed, added, removed
		model.getRecordById("P1").getChildren().getFirst().setValue("Venezia");
		model.addRecord(FLEFRecord.createMainRecord("P3", PlaceReader.TYPE));
		model.removeRecord("P2");

		Assertions.assertEquals(2, view.getRecordCount());
		Assertions.assertEquals("Treviso", nameOf(view, "P1"));
		Assertions.assertEquals("Padova", nameOf(view, "P2"));
		Assertions.assertFalse(view.hasRecord("P3"));
	}

	@Test
	void theViewDoesNotShareTheRecordsThatTheEditorsChange(){
		final FLEFModel model = model();
		final FLEFModel view = new ModelHistory(model).readOnlyView();

		Assertions.assertNotSame(model.getRecordById("P1"), view.getRecordById("P1"));
		Assertions.assertNotSame(model.getRecordById("P1").getChildren().getFirst(), view.getRecordById("P1").getChildren().getFirst());
	}

	@Test
	void theSameViewIsGivenWhileTheModelDoesNotChange(){
		final FLEFModel model = model();
		final ModelHistory history = new ModelHistory(model);

		final FLEFModel first = history.readOnlyView();
		Assertions.assertSame(first, history.readOnlyView());

		model.getRecordById("P1").getChildren().getFirst().setValue("Venezia");
		final FLEFModel second = history.readOnlyView();
		Assertions.assertNotSame(first, second);
		Assertions.assertEquals("Venezia", nameOf(second, "P1"));
		Assertions.assertEquals("Treviso", nameOf(first, "P1"), "the old view is as it was");
		Assertions.assertSame(second, history.readOnlyView());
	}

	@Test
	void theRecordsThatDidNotChangeAreSharedBetweenViews(){
		final FLEFModel model = model();
		final ModelHistory history = new ModelHistory(model);
		final FLEFModel first = history.readOnlyView();

		model.getRecordById("P1").getChildren().getFirst().setValue("Venezia");
		final FLEFModel second = history.readOnlyView();

		Assertions.assertSame(first.getRecordById("P2"), second.getRecordById("P2"), "what did not change is not copied again");
		Assertions.assertNotSame(first.getRecordById("P1"), second.getRecordById("P1"));
	}

	@Test
	void askingForTheViewDoesNotRecordAStepOfTheHistory(){
		final FLEFModel model = model();
		final ModelHistory history = new ModelHistory(model);
		final AtomicInteger changes = new AtomicInteger();
		history.addChangeListener(changes::incrementAndGet);

		model.getRecordById("P1").getChildren().getFirst().setValue("Venezia");
		history.readOnlyView();

		Assertions.assertEquals(0, changes.get(), "an edit under way does not become a step because someone looks at it");
		//the edit is still pending, so it is the one undone: nothing else was recorded
		Assertions.assertTrue(history.isModified());
		Assertions.assertTrue(history.undo());
		Assertions.assertEquals("Treviso", nameOf(model, "P1"));
		Assertions.assertFalse(history.canUndo());
	}

	@Test
	void theViewFollowsUndoAndRedo(){
		final FLEFModel model = model();
		final ModelHistory history = new ModelHistory(model);
		model.getRecordById("P1").getChildren().getFirst().setValue("Venezia");
		history.checkpoint();
		Assertions.assertEquals("Venezia", nameOf(history.readOnlyView(), "P1"));

		history.undo();
		Assertions.assertEquals("Treviso", nameOf(history.readOnlyView(), "P1"));
		history.redo();
		Assertions.assertEquals("Venezia", nameOf(history.readOnlyView(), "P1"));
	}

	@Test
	void theViewIsDroppedWhenTheHistoryIsBoundToAnotherModel(){
		final ModelHistory history = new ModelHistory(model());
		final FLEFModel before = history.readOnlyView();

		final FLEFModel other = new FLEFModel();
		other.addRecord(FLEFRecord.createMainRecord("P9", PlaceReader.TYPE));
		history.reset(other);

		final FLEFModel after = history.readOnlyView();
		Assertions.assertNotSame(before, after);
		Assertions.assertTrue(after.hasRecord("P9"));
		Assertions.assertFalse(after.hasRecord("P1"));
	}

}
