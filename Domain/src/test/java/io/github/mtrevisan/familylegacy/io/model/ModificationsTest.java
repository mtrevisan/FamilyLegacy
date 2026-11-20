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
import io.github.mtrevisan.familylegacy.io.model.record.Modifications;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;


/** Every way of changing a record or a model is seen by the history, also the ones that go through the lists the records give away. */
class ModificationsTest{

	private static FLEFModel model(){
		final FLEFModel model = new FLEFModel();
		for(int i = 1; i <= 3; i ++)
			model.addRecord(FLEFRecord.createMainRecord("I" + i, "INDIVIDUAL")
				.addChild(FLEFRecord.createChildWithTagAndValue("a", "1"))
				.addChild(FLEFRecord.createChildWithTagAndValue("b", "2"))
				.addChild(FLEFRecord.createChildWithTagAndValue("c", "3")));
		return model;
	}

	private static void assertSeen(final String what, final Consumer<FLEFModel> change){
		final FLEFModel model = model();
		final ModelHistory history = new ModelHistory(model);
		//the history knows the model is as it was: no record is looked at while nothing changes
		Assertions.assertFalse(history.isModified(), what);
		Assertions.assertFalse(history.canUndo(), what);

		change.accept(model);

		Assertions.assertTrue(history.isModified(), what);
		Assertions.assertTrue(history.canUndo(), what);
		Assertions.assertTrue(history.undo(), what);
		Assertions.assertFalse(history.isModified(), what);
	}


	@Test
	void theCounterMovesAtEveryChange(){
		final FLEFRecord record = FLEFRecord.createMainRecord("I1", "INDIVIDUAL");
		long before = Modifications.current();

		record.setValue("x");
		Assertions.assertNotEquals(before, Modifications.current());
		before = Modifications.current();

		Assertions.assertEquals(before, Modifications.current());
	}

	@Test
	void changesThroughTheMethodsOfTheRecordAreSeen(){
		assertSeen("value", model -> model.getRecordById("I1").getChildren().getFirst().setValue("changed"));
		assertSeen("tag", model -> model.getRecordById("I1").getChildren().getFirst().setTag("z"));
		assertSeen("add", model -> model.getRecordById("I2").addChild(FLEFRecord.createChildWithTagAndValue("d", "4")));
		assertSeen("remove", model -> model.getRecordById("I2").removeChild(model.getRecordById("I2").getChildren().getFirst()));
		assertSeen("clear", model -> model.getRecordById("I3").clear());
	}

	@Test
	void changesThroughTheListsTheyGiveAwayAreSeen(){
		assertSeen("removeIf", model -> model.getRecordById("I1").getChildren().removeIf(child -> "b".equals(child.getTag())));
		assertSeen("sort", model -> model.getRecordById("I1").getChildren().sort((x, y) -> y.getTag().compareTo(x.getTag())));
		assertSeen("set", model -> model.getRecordById("I1").getChildren().set(0, FLEFRecord.createChildWithTagAndValue("q", "9")));
		assertSeen("iterator", model -> {
			final Iterator<FLEFRecord> iterator = model.getRecordById("I1").getChildren().iterator();
			iterator.next();
			iterator.remove();
		});
		assertSeen("listIterator", model -> {
			final var iterator = model.getRecordById("I1").getChildren().listIterator();
			iterator.next();
			iterator.set(FLEFRecord.createChildWithTagAndValue("w", "8"));
		});
		assertSeen("subList", model -> model.getRecordById("I1").getChildren().subList(0, 2).clear());
		assertSeen("nested", model -> model.getRecordById("I1").getChildren().getFirst()
			.addChild(FLEFRecord.createChildWithTagAndValue("deep", "1")));
	}

	@Test
	void changesToTheModelAreSeen(){
		assertSeen("add", model -> model.addRecord(FLEFRecord.createMainRecord("I9", "INDIVIDUAL")
			.addChild(FLEFRecord.createChildWithTagAndValue("a", "1"))));
		assertSeen("remove", model -> model.removeRecord("I2"));
		assertSeen("list", model -> model.getRecords().removeIf(record -> "I3".equals(record.getId())));
		assertSeen("header", model -> model.getHeader().addChild(FLEFRecord.createChildWithTagAndValue("date", "2026")));
	}

	@Test
	void anEditThatIsUndoneInPlaceLeavesTheModelUnmodified(){
		final FLEFModel model = model();
		final ModelHistory history = new ModelHistory(model);
		final FLEFRecord first = model.getRecordById("I1").getChildren().getFirst();

		first.setValue("changed");
		Assertions.assertTrue(history.isModified());
		first.setValue("1");

		Assertions.assertFalse(history.isModified());
		Assertions.assertFalse(history.canUndo());
		Assertions.assertEquals(List.of("a", "b", "c"), model.getRecordById("I1").getChildren().stream().map(FLEFRecord::getTag).toList());
	}

}
