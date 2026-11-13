package io.github.mtrevisan.familylegacy.ui.tools.maintenance.merge;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.merger.manual.MergeGrammar;
import io.github.mtrevisan.familylegacy.io.merger.manual.MergePlan;
import io.github.mtrevisan.familylegacy.io.merger.manual.RecordMerger;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


/** The review of the whole record: the middle column is what the kept record gets, and it is read back on Merge. */
class MergeRecordReviewDialogTest{

	private static final String TEXT = """
		header {
			protocol {
				name Family LEgacy Format
				version 0.2.0
			}
			date 2026-01-01T00:00:00Z
		}
		individual I1 {
			name.part {
				type given
				value Giovanni
			}
			audit.creation.date 2026-01-01T00:00:00Z
		}
		individual I2 {
			name.part {
				type given
				value Giuseppe
			}
			audit.creation.date 2026-01-01T00:00:00Z
		}
		""";

	private static final String TWO_CONFLICTS = """
		header {
			protocol {
				name Family LEgacy Format
				version 0.2.0
			}
			date 2026-01-01T00:00:00Z
		}
		individual I1 {
			name {
				part {
					type given
					value Anna
				}
			}
			name {
				part {
					type given
					value Bianca
				}
			}
			audit.creation.date 2026-01-01T00:00:00Z
		}
		individual I2 {
			name {
				part {
					type given
					value Carla
				}
			}
			name {
				part {
					type given
					value Dora
				}
			}
			audit.creation.date 2026-01-01T00:00:00Z
		}
		""";

	@Test
	void theNavigationGoesFromConflictToConflictAndNoFurther() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
		Assumptions.assumeTrue(MergeGrammar.instance() != null, "needs the grammar");

		final FLEFModel model = new FLEFParser().parse(TWO_CONFLICTS);
		final MergePlan plan = new RecordMerger(model, MergeGrammar.instance()).planAllFields("I2", "I1");
		final MergeRecordReviewDialog[] holder = new MergeRecordReviewDialog[1];
		SwingUtilities.invokeAndWait(() -> holder[0] = new MergeRecordReviewDialog(null, plan, model, "I2", "I1"));
		final MergeRecordReviewDialog dialog = holder[0];

		//two names against two names, paired in order: two conflicts, none decided
		Assertions.assertEquals(2, dialog.conflictCount());
		Assertions.assertEquals(2, dialog.undecidedConflictCount());
		Assertions.assertEquals(0, dialog.currentConflictNumber());

		SwingUtilities.invokeAndWait(dialog::goToFirst);
		Assertions.assertEquals(1, dialog.currentConflictNumber());
		Assertions.assertTrue(dialog.currentResultText().contains("Anna") && dialog.currentResultText().contains("Carla"),
			dialog.currentResultText());

		SwingUtilities.invokeAndWait(() -> dialog.goToNext(true));
		Assertions.assertEquals(2, dialog.currentConflictNumber());
		Assertions.assertTrue(dialog.currentResultText().contains("Bianca") && dialog.currentResultText().contains("Dora"));

		//there is no third one: it stays where it is
		SwingUtilities.invokeAndWait(() -> dialog.goToNext(true));
		Assertions.assertEquals(2, dialog.currentConflictNumber());

		SwingUtilities.invokeAndWait(() -> dialog.goToPrevious(true));
		Assertions.assertEquals(1, dialog.currentConflictNumber());
		SwingUtilities.invokeAndWait(() -> dialog.goToPrevious(true));
		Assertions.assertEquals(1, dialog.currentConflictNumber());

		//deciding a conflict (here by editing the result of the first) leaves one to decide
		final List<JTextArea> areas = new ArrayList<>();
		collect(dialog.getContentPane(), areas);
		SwingUtilities.invokeAndWait(() -> areas.get(1).setText(areas.get(0).getText()));
		Assertions.assertEquals(1, dialog.undecidedConflictCount());
	}

	private static int lineOf(final String[] lines, final String fragment){
		for(int i = 0; i < lines.length; i ++)
			if(lines[i].contains(fragment))
				return i;
		return -1;
	}

	private static void collect(final Container container, final List<JTextArea> into){
		for(final Component child : container.getComponents()){
			if(child instanceof JTextArea area)
				into.add(area);
			else if(child instanceof Container nested)
				collect(nested, into);
		}
	}

	/** An error box is modal: if the dialog shows one the test must fail, not wait for a click that never comes. */
	private static Timer failOnBoxes(final List<String> boxes, final Window dialog){
		final Timer closer = new Timer(100, e -> {
			for(final Window window : Window.getWindows())
				if(window.isVisible() && window != dialog){
					boxes.add(window.getClass().getName());
					window.dispose();
				}
		});
		closer.start();
		return closer;
	}

	private static void pressWithoutBoxes(final MergeRecordReviewDialog dialog) throws Exception{
		final List<String> boxes = new ArrayList<>();
		final Timer closer = failOnBoxes(boxes, dialog);
		try{
			SwingUtilities.invokeAndWait(() -> {
				try{
					press(dialog);
				}
				catch(final Exception e){
					throw new RuntimeException(e);
				}
			});
		}
		finally{
			closer.stop();
		}
		Assertions.assertTrue(boxes.isEmpty(), "the dialog showed " + boxes);
	}

	private static void press(final MergeRecordReviewDialog dialog) throws Exception{
		final Method merge = MergeRecordReviewDialog.class.getDeclaredMethod("onMerge");
		merge.setAccessible(true);
		merge.invoke(dialog);
	}

	@Test
	void theResultColumnIsWhatTheTargetGets() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
		Assumptions.assumeTrue(MergeGrammar.instance() != null, "needs the grammar");

		final FLEFModel model = new FLEFParser().parse(TEXT);
		final RecordMerger merger = new RecordMerger(model, MergeGrammar.instance());
		final MergePlan plan = merger.planAllFields("I2", "I1");

		final MergeRecordReviewDialog[] holder = new MergeRecordReviewDialog[1];
		SwingUtilities.invokeAndWait(() -> holder[0] = new MergeRecordReviewDialog(null, plan, model, "I2", "I1"));
		final MergeRecordReviewDialog dialog = holder[0];

		final List<JTextArea> areas = new ArrayList<>();
		collect(dialog.getContentPane(), areas);
		//source, result, target of the only field that is visible (the name): left to right
		Assertions.assertEquals(3, areas.size(), "one row of three columns");
		final JTextArea source = areas.get(0);
		final JTextArea result = areas.get(1);
		final JTextArea target = areas.get(2);
		Assertions.assertTrue(source.getText().contains("Giuseppe"));
		Assertions.assertTrue(target.getText().contains("Giovanni"));
		//the names are repeatable: the proposal has both
		Assertions.assertTrue(result.getText().contains("Giuseppe") && result.getText().contains("Giovanni"));

		//the three columns have the same number of lines, and what collides is on one row: the two given names side by side
		final String[] sourceLines = source.getText().split("\n", -1);
		final String[] targetLines = target.getText().split("\n", -1);
		Assertions.assertEquals(sourceLines.length, result.getText().split("\n", -1).length);
		Assertions.assertEquals(sourceLines.length, targetLines.length);
		Assertions.assertEquals(lineOf(sourceLines, "Giuseppe"), lineOf(targetLines, "Giovanni"));

		//the user takes only the source's name
		SwingUtilities.invokeAndWait(() -> result.setText(source.getText()));
		pressWithoutBoxes(dialog);
		Assertions.assertTrue(dialog.isConfirmed());

		merger.apply(plan);

		final FLEFRecord kept = model.getRecordById("I1");
		Assertions.assertNull(model.getRecordById("I2"));
		Assertions.assertEquals(1, kept.countChildrenWithTag("name"));
		Assertions.assertTrue(hasValue(kept, "Giuseppe"));
		Assertions.assertFalse(hasValue(kept, "Giovanni"));
	}

	private static final String ROSSI_AND_ROSSA = """
		header {
			protocol {
				name Family LEgacy Format
				version 0.2.0
			}
			date 2026-01-01T00:00:00Z
		}
		individual I1 {
			name {
				part {
					type given
					value Giovanni
				}
				part {
					type family
					value Rossi
				}
			}
			audit.creation.date 2026-01-01T00:00:00Z
		}
		individual I2 {
			name {
				part {
					type given
					value Giovanni
				}
				part {
					type family
					value Rossa
				}
			}
			audit.creation.date 2026-01-01T00:00:00Z
		}
		""";

	private static MergeRecordReviewDialog open(final String text) throws Exception{
		final FLEFModel model = new FLEFParser().parse(text);
		final MergePlan plan = new RecordMerger(model, MergeGrammar.instance()).planAllFields("I2", "I1");
		final MergeRecordReviewDialog[] holder = new MergeRecordReviewDialog[1];
		SwingUtilities.invokeAndWait(() -> holder[0] = new MergeRecordReviewDialog(null, plan, model, "I2", "I1"));
		return holder[0];
	}

	private static int occurrences(final String text, final String fragment){
		int count = 0;
		for(int at = text.indexOf(fragment); at >= 0; at = text.indexOf(fragment, at + fragment.length()))
			count ++;
		return count;
	}

	@Test
	void aBlockIsTakenFromASideWithoutTouchingTheRest() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
		Assumptions.assumeTrue(MergeGrammar.instance() != null, "needs the grammar");

		final int blocks = open(ROSSI_AND_ROSSA).blockCount(0);
		//the family part, and the line of its value: the given part is the same in both, there is nothing to decide
		Assertions.assertEquals(2, blocks);
		for(int block = 0; block < blocks; block ++){
			final int chosen = block;

			//from the source: the copy of the target that the result starts with has the value of the source
			final MergeRecordReviewDialog fromSource = open(ROSSI_AND_ROSSA);
			SwingUtilities.invokeAndWait(() -> fromSource.takeBlock(0, chosen, 0));
			Assertions.assertEquals(0, occurrences(fromSource.resultText(0), "Rossi"), fromSource.resultText(0));
			Assertions.assertEquals(2, occurrences(fromSource.resultText(0), "Rossa"), fromSource.resultText(0));
			Assertions.assertEquals(2, occurrences(fromSource.resultText(0), "Giovanni"), fromSource.resultText(0));

			//from the target: it was already so
			final MergeRecordReviewDialog fromTarget = open(ROSSI_AND_ROSSA);
			SwingUtilities.invokeAndWait(() -> fromTarget.takeBlock(0, chosen, 1));
			Assertions.assertEquals(1, occurrences(fromTarget.resultText(0), "Rossi"), fromTarget.resultText(0));
			Assertions.assertEquals(1, occurrences(fromTarget.resultText(0), "Rossa"), fromTarget.resultText(0));

			//none: the block is gone from the first copy
			final MergeRecordReviewDialog none = open(ROSSI_AND_ROSSA);
			SwingUtilities.invokeAndWait(() -> none.takeBlock(0, chosen, 2));
			Assertions.assertEquals(0, occurrences(none.resultText(0), "Rossi"), none.resultText(0));
			Assertions.assertEquals(1, occurrences(none.resultText(0), "Rossa"), none.resultText(0));
		}
	}

	private static void waitFor(final java.util.function.BooleanSupplier condition) throws Exception{
		for(int i = 0; i < 200 && !condition.getAsBoolean(); i ++)
			Thread.sleep(50);
	}

	private static String manyNames(){
		final StringBuilder text = new StringBuilder("""
			header {
				protocol {
					name Family LEgacy Format
					version 0.2.0
				}
				date 2026-01-01T00:00:00Z
			}
			""");
		for(final String id : List.of("I1", "I2")){
			text.append("individual ").append(id).append(" {\n");
			for(int k = 0; k < 30; k ++)
				text.append("name {\npart {\ntype given\nvalue ").append(id).append("N").append(k).append("\n}\n}\n");
			text.append("}\n");
		}
		return text.toString();
	}

	@Test
	void theWindowOpensAtTheTop() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
		Assumptions.assumeTrue(MergeGrammar.instance() != null, "needs the grammar");

		final MergeRecordReviewDialog dialog = open(manyNames());
		SwingUtilities.invokeAndWait(() -> {
			dialog.setModalityType(java.awt.Dialog.ModalityType.MODELESS);
			dialog.setVisible(true);
		});
		try{
			//the record is longer than the window, and the first line is the one that is shown, however long it takes to lay it out
			for(int i = 0; i < 10; i ++){
				Thread.sleep(100);
				Assertions.assertEquals(0, dialog.verticalScrollPosition(), "after " + (i + 1) * 100 + " ms");
			}
		}
		finally{
			SwingUtilities.invokeAndWait(dialog::dispose);
		}
	}

	private static final String PREFERRED_IMAGES = """
		header {
			protocol {
				name Family LEgacy Format
				version 0.2.0
			}
			date 2026-01-01T00:00:00Z
		}
		media M1 {
			title A
		}
		media M2 {
			title B
		}
		individual I1 {
			preferred_image {
				media M2
				crop {
					x 1
					y 2
				}
			}
			audit.creation.date 2026-01-01T00:00:00Z
		}
		individual I2 {
			preferred_image {
				media M1
				crop {
					x 3
					y 4
				}
			}
			audit.creation.date 2026-01-01T00:00:00Z
		}
		""";

	@Test
	void theSingleValuedFieldsOfTwoRecordsFaceEachOtherWhateverTheyCite() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
		Assumptions.assumeTrue(MergeGrammar.instance() != null, "needs the grammar");

		//one preferred image on each side, of two different media: they are the two versions of one, not two things
		final MergeRecordReviewDialog dialog = open(PREFERRED_IMAGES);
		Assertions.assertEquals(1, dialog.conflictCount());
	}

	/** Whether a window with the list of the suggestions is shown. */
	private static boolean suggestionsShown(){
		for(final Window window : Window.getWindows())
			if(window.isVisible() && window instanceof javax.swing.JWindow)
				return true;
		return false;
	}

	@Test
	void theTagsAreSuggestedWhileTheyAreTyped() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
		Assumptions.assumeTrue(MergeGrammar.instance() != null, "needs the grammar");

		final MergeRecordReviewDialog dialog = open(ROSSI_AND_ROSSA);
		SwingUtilities.invokeAndWait(() -> {
			dialog.setModalityType(java.awt.Dialog.ModalityType.MODELESS);
			dialog.setVisible(true);
		});
		try{
			final List<JTextArea> areas = new ArrayList<>();
			collect(dialog.getContentPane(), areas);
			final JTextArea result = areas.get(1);
			SwingUtilities.invokeAndWait(result::requestFocusInWindow);
			waitFor(result::isFocusOwner);
			Assumptions.assumeTrue(result.isFocusOwner(), "the window does not get the focus here");

			//a new line inside the block, and the first letters of a tag that the block allows: the tags are offered
			SwingUtilities.invokeAndWait(() -> {
				try{
					final int at = result.getText().indexOf("type given");
					result.getDocument().insertString(at, "va", null);
					result.setCaretPosition(at + 2);
				}
				catch(final javax.swing.text.BadLocationException e){
					throw new RuntimeException(e);
				}
			});
			waitFor(MergeRecordReviewDialogTest::suggestionsShown);
			Assertions.assertTrue(suggestionsShown(), "no suggestion was offered");
		}
		finally{
			SwingUtilities.invokeAndWait(dialog::dispose);
		}
	}

	@Test
	void theBarOfTheValidityIsSteadyWhileItChecksAgain() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
		Assumptions.assumeTrue(MergeGrammar.instance() != null, "needs the grammar");

		final MergeRecordReviewDialog dialog = open(ROSSI_AND_ROSSA);
		//the window is not shown: it has a peer, so that what a check finds is written
		SwingUtilities.invokeAndWait(dialog::addNotify);
		Assertions.assertTrue(dialog.validityText().isBlank());

		//the closing brace is missing: the text cannot be read
		SwingUtilities.invokeAndWait(() -> {
			dialog.writeResult(0, "name {\npart {\ntype given\nvalue Giovanni\n}\n");
			dialog.checkValidityNow();
		});
		waitFor(() -> dialog.validityText().startsWith("\u2717"));
		final String said = dialog.validityText();
		Assertions.assertTrue(said.startsWith("\u2717"), said);

		//the same text again: nothing to check, nothing to say
		SwingUtilities.invokeAndWait(() -> {
			dialog.checkValidityNow();
			Assertions.assertEquals(said, dialog.validityText());
		});

		//another text that cannot be read: while it is checked the bar still says what it said, it does not go blank or say it checks
		final List<String> seen = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			dialog.writeResult(0, "name {\npart {\ntype given\nvalue Giuseppe\n}\n");
			dialog.checkValidityNow();
			seen.add(dialog.validityText());
		});
		Assertions.assertEquals(List.of(said), seen);
		Thread.sleep(500);
		Assertions.assertEquals(said, dialog.validityText());
	}

	@Test
	void everyConflictCanBeTakenFromASideAtOnce() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
		Assumptions.assumeTrue(MergeGrammar.instance() != null, "needs the grammar");

		final MergeRecordReviewDialog dialog = open(TWO_CONFLICTS);
		Assertions.assertEquals(2, dialog.undecidedConflictCount());

		SwingUtilities.invokeAndWait(() -> dialog.takeAllConflicts(true));

		Assertions.assertEquals(0, dialog.undecidedConflictCount());
		//each of the two conflicts has the name of the source, and not the one of the target
		Assertions.assertTrue(dialog.resultText(0).contains("Carla"), dialog.resultText(0));
		Assertions.assertFalse(dialog.resultText(0).contains("Anna"), dialog.resultText(0));
		Assertions.assertTrue(dialog.resultText(1).contains("Dora"), dialog.resultText(1));
		Assertions.assertFalse(dialog.resultText(1).contains("Bianca"), dialog.resultText(1));
	}

	@Test
	void aRowLeftAsProposedIsLeftToTheRules() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
		Assumptions.assumeTrue(MergeGrammar.instance() != null, "needs the grammar");

		final FLEFModel model = new FLEFParser().parse(TEXT);
		final RecordMerger merger = new RecordMerger(model, MergeGrammar.instance());
		final MergePlan plan = merger.planAllFields("I2", "I1");

		final MergeRecordReviewDialog[] holder = new MergeRecordReviewDialog[1];
		SwingUtilities.invokeAndWait(() -> holder[0] = new MergeRecordReviewDialog(null, plan, model, "I2", "I1"));
		pressWithoutBoxes(holder[0]);
		Assertions.assertTrue(holder[0].isConfirmed());

		merger.apply(plan);

		Assertions.assertEquals(2, model.getRecordById("I1").countChildrenWithTag("name"));
	}

	@Test
	void anInvalidResultKeepsTheDialogOpenAndTheFileUntouched() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
		Assumptions.assumeTrue(MergeGrammar.instance() != null, "needs the grammar");

		final FLEFModel model = new FLEFParser().parse(TEXT);
		final RecordMerger merger = new RecordMerger(model, MergeGrammar.instance());
		final MergePlan plan = merger.planAllFields("I2", "I1");

		final MergeRecordReviewDialog[] holder = new MergeRecordReviewDialog[1];
		SwingUtilities.invokeAndWait(() -> holder[0] = new MergeRecordReviewDialog(null, plan, model, "I2", "I1"));
		final List<JTextArea> areas = new ArrayList<>();
		collect(holder[0].getContentPane(), areas);

		//the error box is modal: it is closed by a timer, as a user would
		final Timer closer = new Timer(100, e -> {
			for(final Window window : Window.getWindows())
				if(window.isVisible() && window != holder[0])
					window.dispose();
		});
		closer.start();
		try{
			SwingUtilities.invokeAndWait(() -> areas.get(1).setText("name {{{ broken"));
			SwingUtilities.invokeAndWait(() -> {
				try{
					press(holder[0]);
				}
				catch(final Exception e){
					throw new RuntimeException(e);
				}
			});
		}
		finally{
			closer.stop();
		}

		Assertions.assertFalse(holder[0].isConfirmed());
		Assertions.assertFalse(plan.fields().stream().anyMatch(f -> f.isComposed()));
	}

	private static boolean hasValue(final FLEFRecord record, final String value){
		if(value.equals(record.getValue()))
			return true;
		for(final FLEFRecord child : record.getChildren())
			if(hasValue(child, value))
				return true;
		return false;
	}

	@SuppressWarnings("unused")
	private static Object field(final Object owner, final String name) throws Exception{
		final Field f = owner.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.get(owner);
	}

}
