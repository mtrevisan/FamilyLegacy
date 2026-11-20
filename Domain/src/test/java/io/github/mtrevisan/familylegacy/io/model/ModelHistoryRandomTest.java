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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * Whatever is done to the records and to the model, in any of the ways there are, the history finds it: what it says about the model (modified
 * or not, what an undo gives back) is what a comparison of every record says.
 */
class ModelHistoryRandomTest{

	private static final int RECORDS = 25;


	/**
	 * A record seen in two lists (what the last test does, and any other test of the program may do) switches off, for the rest of the run, what
	 * these tests are about: it is switched on again.
	 */
	@BeforeEach
	void localizingChangesIsOn() throws ReflectiveOperationException{
		final var field = Modifications.class.getDeclaredField("shared");
		field.setAccessible(true);
		field.set(null, false);
	}


	private static FLEFRecord record(final int number, final Random random){
		final FLEFRecord record = FLEFRecord.createMainRecord("I" + number, "INDIVIDUAL");
		for(int i = 0; i < 3; i ++){
			final FLEFRecord child = FLEFRecord.createChildWithTagAndValue("t" + i, "v" + random.nextInt(5));
			child.addChild(FLEFRecord.createChildWithTagAndValue("n", "x" + random.nextInt(3)));
			record.addChild(child);
		}
		return record;
	}

	private static FLEFModel model(final Random random){
		final FLEFModel model = new FLEFModel();
		for(int i = 0; i < RECORDS; i ++)
			model.addRecord(record(i, random));
		return model;
	}

	private static FLEFRecord copy(final FLEFRecord record){
		final FLEFRecord copy = FLEFRecord.createEmpty();
		record.deepCopyTo(copy);
		return copy;
	}

	private static List<FLEFRecord> copyOf(final FLEFModel model){
		final List<FLEFRecord> copy = new ArrayList<>();
		for(final FLEFRecord record : model.getRecords())
			copy.add(copy(record));
		return copy;
	}

	private static boolean same(final FLEFRecord a, final FLEFRecord b){
		if(!java.util.Objects.equals(a.getTag(), b.getTag()) || !java.util.Objects.equals(a.getId(), b.getId())
				|| !java.util.Objects.equals(a.getValue(), b.getValue()) || a.getChildren().size() != b.getChildren().size())
			return false;
		for(int i = 0; i < a.getChildren().size(); i ++)
			if(!same(a.getChildren().get(i), b.getChildren().get(i)))
				return false;
		return true;
	}

	private static boolean same(final List<FLEFRecord> expected, final FLEFModel model){
		if(expected.size() != model.getRecords().size())
			return false;
		for(int i = 0; i < expected.size(); i ++)
			if(!same(expected.get(i), model.getRecords().get(i)))
				return false;
		return true;
	}

	/** A record of the model or a node below it, chosen at random. */
	private static FLEFRecord anyNode(final FLEFModel model, final Random random){
		FLEFRecord node = model.getRecords().get(random.nextInt(model.getRecords().size()));
		while(!node.getChildren().isEmpty() && random.nextInt(3) > 0)
			node = node.getChildren().get(random.nextInt(node.getChildren().size()));
		return node;
	}

	/** Changes the model in one of the ways there are. */
	private static int lastCase;

	private static void change(final FLEFModel model, final Random random, final int serial){
		final List<FLEFRecord> records = model.getRecords();
		final FLEFRecord node = anyNode(model, random);
		lastCase = random.nextInt(18);
		switch(lastCase){
			case 0 -> node.setValue("changed" + serial);
			case 1 -> node.setTag("tag" + serial);
			case 2 -> node.addChild(FLEFRecord.createChildWithTagAndValue("added", "a" + serial));
			case 3 -> {
				if(!node.getChildren().isEmpty())
					node.getChildren().removeFirst();
			}
			case 4 -> node.getChildren().removeIf(child -> child.getValue() != null && child.getValue().endsWith("1"));
			case 5 -> node.getChildren().sort((a, b) -> String.valueOf(b.getTag()).compareTo(String.valueOf(a.getTag())));
			case 6 -> {
				if(!node.getChildren().isEmpty())
					node.getChildren().set(0, FLEFRecord.createChildWithTagAndValue("set", "s" + serial));
			}
			case 7 -> {
				final var iterator = node.getChildren().iterator();
				if(iterator.hasNext()){
					iterator.next();
					iterator.remove();
				}
			}
			case 8 -> {
				if(node.getChildren().size() > 1)
					node.getChildren().subList(0, 1).clear();
			}
			case 9 -> {
				//a child moves from a record to another one: it is not shared, it changes parent
				final FLEFRecord from = records.get(random.nextInt(records.size()));
				final FLEFRecord to = records.get(random.nextInt(records.size()));
				if(from != to && !from.getChildren().isEmpty()){
					final FLEFRecord moved = from.getChildren().getFirst();
					from.removeChild(moved);
					to.addChild(moved);
				}
			}
			case 10 -> {
				//a record is replaced by another one with the same id
				final int index = random.nextInt(records.size());
				final FLEFRecord replacement = FLEFRecord.createMainRecord(records.get(index).getId(), "INDIVIDUAL")
					.addChild(FLEFRecord.createChildWithTagAndValue("new", "n" + serial));
				model.addRecord(replacement);
			}
			case 11 -> {
				if(records.size() > 5)
					model.removeRecord(records.get(random.nextInt(records.size())).getId());
			}
			case 12 -> model.addRecord(record(100 + serial, random));
			case 13 -> node.clear();
			case 14 -> {
				//the moved child is changed afterwards
				final FLEFRecord from = records.get(random.nextInt(records.size()));
				final FLEFRecord to = records.get(random.nextInt(records.size()));
				if(from != to && !from.getChildren().isEmpty()){
					final FLEFRecord moved = from.getChildren().removeLast();
					to.getChildren().add(moved);
					moved.setValue("moved" + serial);
				}
			}
			case 15 -> {
				if(records.size() > 5)
					records.remove(random.nextInt(records.size()));
			}
			case 16 -> node.getChildren().addAll(List.of(FLEFRecord.createChildWithTagAndValue("p", "1"), FLEFRecord.createChildWithTagAndValue("q", "2")));
			default -> {
				//a record that is put at another position of the model
				if(records.size() > 1){
					final FLEFRecord first = records.removeFirst();
					records.add(first);
				}
			}
		}
	}


	@Test
	void theHistoryFindsEveryChangeAndUndoesIt(){
		for(int seed = 0; seed < 1000; seed ++){
			final Random random = new Random(seed);
			final FLEFModel model = model(random);
			final ModelHistory history = new ModelHistory(model);
			final List<FLEFRecord> initial = copyOf(model);
			Assertions.assertFalse(history.isModified(), "seed " + seed);

			final int operations = 1 + random.nextInt(25);
			for(int i = 0; i < operations; i ++){
				change(model, random, i);

				final boolean equal = same(initial, model);
				Assertions.assertEquals(!equal, history.isModified(), "seed " + seed + ", operation " + i + ", case " + lastCase);
				if(random.nextInt(3) == 0)
					history.checkpoint();
			}
			history.checkpoint();
			final List<FLEFRecord> last = copyOf(model);

			final int undone = undoAll(history);
			Assertions.assertTrue(same(initial, model), "seed " + seed + " after " + undone + " undos");
			Assertions.assertFalse(history.isModified(), "seed " + seed);

			for(int i = 0; i < undone; i ++)
				Assertions.assertTrue(history.redo(), "seed " + seed);
			Assertions.assertTrue(same(last, model), "seed " + seed + " after the redos");

			//the view for a save is a copy of the model that does not change when the model does
			final FLEFModel view = history.readOnlyView();
			final List<FLEFRecord> viewed = copyOf(view);
			change(model, random, 99);
			Assertions.assertTrue(same(viewed, view), "seed " + seed + ": the view");
		}
	}

	private static int undoAll(final ModelHistory history){
		int undone = 0;
		while(history.canUndo()){
			Assertions.assertTrue(history.undo());
			undone ++;
		}
		return undone;
	}

	@Test
	void moreModifiedRecordsThanAreRememberedAreAllFound(){
		final Random random = new Random(3);
		final FLEFModel model = new FLEFModel();
		for(int i = 0; i < 12_000; i ++)
			model.addRecord(record(i, random));
		final ModelHistory history = new ModelHistory(model);
		Assertions.assertFalse(history.isModified());
		final List<FLEFRecord> initial = copyOf(model);

		//(the first one and the last one are the ones that a list of the last records modified would forget or keep)
		for(final FLEFRecord record : model.getRecords())
			record.getChildren().getFirst().setValue("changed");

		Assertions.assertTrue(history.isModified());
		history.checkpoint();
		Assertions.assertTrue(history.undo());
		Assertions.assertTrue(same(initial, model));
		Assertions.assertFalse(history.isModified());
	}

	@Test
	void aFewRecordsModifiedAfterTheOnesThatWereForgottenAreFoundToo(){
		final Random random = new Random(4);
		final FLEFModel model = new FLEFModel();
		for(int i = 0; i < 12_000; i ++)
			model.addRecord(record(i, random));
		final ModelHistory history = new ModelHistory(model);
		for(final FLEFRecord record : model.getRecords())
			record.setValue("x");
		history.checkpoint();

		model.getRecords().get(7).getChildren().getFirst().setValue("again");
		Assertions.assertTrue(history.isModified());
		history.checkpoint();
		Assertions.assertTrue(history.undo());
		Assertions.assertEquals("x", model.getRecords().get(0).getValue());
		Assertions.assertNotEquals("again", model.getRecords().get(7).getChildren().getFirst().getValue());
		Assertions.assertTrue(history.undo());
		Assertions.assertNull(model.getRecords().get(0).getValue());
	}

	@Test
	void aSharedNodeIsStillFound(){
		final Random random = new Random(1);
		final FLEFModel model = model(random);
		final ModelHistory history = new ModelHistory(model);
		final List<FLEFRecord> initial = copyOf(model);

		//the same node below two records: a change of it is a change of both
		final FLEFRecord shared = FLEFRecord.createChildWithTagAndValue("shared", "1");
		model.getRecords().get(0).addChild(shared);
		model.getRecords().get(1).addChild(shared);
		history.checkpoint();
		shared.setValue("2");

		Assertions.assertTrue(history.isModified());
		history.checkpoint();
		Assertions.assertEquals(2, undoAll(history));
		Assertions.assertTrue(same(initial, model));
	}

}
