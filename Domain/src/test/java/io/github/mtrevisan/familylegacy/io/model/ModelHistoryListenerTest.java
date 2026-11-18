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

import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;


/** The history tells when the model changes, so that what depends on the model (the indicator of the assistant) is not polling it. */
class ModelHistoryListenerTest{

	private static FLEFRecord individual(final String id){
		final FLEFRecord record = FLEFRecord.createMainRecord(id, IndividualReader.TYPE);
		record.addChild(FLEFRecord.createChildWithTagAndValue(IndividualReader.TAG_NOTE, "x"));
		return record;
	}


	@Test
	void aStepRecordedTellsTheListener(){
		final FLEFModel model = new FLEFModel();
		final ModelHistory history = new ModelHistory(model);
		final AtomicInteger changes = new AtomicInteger();
		history.addChangeListener(changes::incrementAndGet);

		model.addRecord(individual("I1"));
		history.checkpoint();

		Assertions.assertEquals(1, changes.get());
	}

	@Test
	void aCheckpointWithoutAChangeIsSilent(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(individual("I1"));
		final ModelHistory history = new ModelHistory(model);
		final AtomicInteger changes = new AtomicInteger();
		history.addChangeListener(changes::incrementAndGet);

		//a checkpoint is taken before every action of the user: most of them change nothing
		for(int i = 0; i < 5; i ++)
			history.checkpoint();

		Assertions.assertEquals(0, changes.get());
	}

	@Test
	void anUndoAndARedoTellTheListener(){
		final FLEFModel model = new FLEFModel();
		final ModelHistory history = new ModelHistory(model);
		model.addRecord(individual("I1"));
		history.checkpoint();
		final AtomicInteger changes = new AtomicInteger();
		history.addChangeListener(changes::incrementAndGet);

		Assertions.assertTrue(history.undo());
		Assertions.assertEquals(1, changes.get());
		Assertions.assertTrue(history.redo());
		Assertions.assertEquals(2, changes.get());
	}

	@Test
	void anUndoThatDoesNothingIsSilent(){
		final FLEFModel model = new FLEFModel();
		final ModelHistory history = new ModelHistory(model);
		final AtomicInteger changes = new AtomicInteger();
		history.addChangeListener(changes::incrementAndGet);

		Assertions.assertFalse(history.undo());
		Assertions.assertFalse(history.redo());

		Assertions.assertEquals(0, changes.get());
	}

	@Test
	void replacingTheModelTellsTheListener(){
		final ModelHistory history = new ModelHistory(new FLEFModel());
		final AtomicInteger changes = new AtomicInteger();
		history.addChangeListener(changes::incrementAndGet);

		history.reset(new FLEFModel());

		Assertions.assertEquals(1, changes.get());
	}

	@Test
	void everyListenerIsTold(){
		final FLEFModel model = new FLEFModel();
		final ModelHistory history = new ModelHistory(model);
		final AtomicInteger first = new AtomicInteger();
		final AtomicInteger second = new AtomicInteger();
		history.addChangeListener(first::incrementAndGet);
		history.addChangeListener(second::incrementAndGet);

		model.addRecord(individual("I1"));
		history.checkpoint();

		Assertions.assertEquals(1, first.get());
		Assertions.assertEquals(1, second.get());
	}

}
