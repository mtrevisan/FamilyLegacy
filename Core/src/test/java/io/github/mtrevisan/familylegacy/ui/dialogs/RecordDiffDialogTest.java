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

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.Dialog;
import java.awt.GraphicsEnvironment;
import java.util.List;


/** The comparison of two records aligns them as the review of a merge does, and moves from difference to difference. */
class RecordDiffDialogTest{

	private static final String HEADER = """
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
		""";

	private static FLEFModel model(final String left, final String right){
		return new FLEFParser().parse(HEADER + "individual I1 {\n" + left + "\n}\nindividual I2 {\n" + right + "\n}\n");
	}

	private static String name(final String... parts){
		final StringBuilder sb = new StringBuilder("name {\n");
		for(final String part : parts)
			sb.append("part {\n").append(part).append("\n}\n");
		return sb.append("}").toString();
	}

	private static RecordDiffDialog open(final FLEFModel model) throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
		final FLEFRecord before = model.getRecordById("I1");
		final FLEFRecord after = model.getRecordById("I2");
		final RecordDiffDialog[] holder = new RecordDiffDialog[1];
		SwingUtilities.invokeAndWait(() -> holder[0] = new RecordDiffDialog(null, "compare", before, after, true, model));
		return holder[0];
	}

	private static String[] lines(final String text){
		return text.split("\n", -1);
	}

	private static int rowOf(final String[] lines, final String fragment){
		for(int row = 0; row < lines.length; row ++)
			if(lines[row].contains(fragment))
				return row;
		return -1;
	}

	@Test
	void theColumnsAreAlignedAndTheSubRecordsAreInTheSameOrder() throws Exception{
		//the right record lists the parts the other way round, and has a part more: the left follows the order of the right
		final RecordDiffDialog dialog = open(model(
			name("type given\nvalue Giovanni", "type family\nvalue Rossi"),
			name("type family\nvalue Rossa", "type nickname\nvalue Gianni", "type given\nvalue Giovanni")));
		final String[] left = lines(dialog.leftText());
		final String[] right = lines(dialog.rightText());

		Assertions.assertEquals(left.length, right.length);
		//the right record is the one in the second column: its order is the one of the screen, in both
		Assertions.assertTrue(rowOf(right, "value Rossa") < rowOf(right, "value Gianni"));
		Assertions.assertTrue(rowOf(right, "value Gianni") < rowOf(right, "value Giovanni"));
		//the same part is on the same row in both columns, and a part that only one side has faces empty lines
		Assertions.assertEquals(rowOf(right, "value Giovanni"), rowOf(left, "value Giovanni"));
		Assertions.assertEquals(rowOf(right, "value Rossa"), rowOf(left, "value Rossi"));
		Assertions.assertTrue(left[rowOf(right, "value Gianni")].isBlank(), left[rowOf(right, "value Gianni")]);
		SwingUtilities.invokeAndWait(dialog::dispose);
	}

	@Test
	void theButtonsGoFromDifferenceToDifference() throws Exception{
		final RecordDiffDialog dialog = open(model(
			name("type given\nvalue Giovanni", "type family\nvalue Rossi", "type title\nvalue Sir"),
			name("type given\nvalue Giuseppe", "type family\nvalue Rossi", "type title\nvalue Dr")));
		//two places differ (the given name and the title), with the family name that is the same between them
		Assertions.assertEquals(2, dialog.differenceCount());
		Assertions.assertEquals(-1, dialog.currentDifference());

		SwingUtilities.invokeAndWait(dialog::goToNext);
		Assertions.assertEquals(0, dialog.currentDifference());
		SwingUtilities.invokeAndWait(dialog::goToNext);
		Assertions.assertEquals(1, dialog.currentDifference());
		//there is no third one: it stays where it is
		SwingUtilities.invokeAndWait(dialog::goToNext);
		Assertions.assertEquals(1, dialog.currentDifference());
		SwingUtilities.invokeAndWait(dialog::goToPrevious);
		Assertions.assertEquals(0, dialog.currentDifference());
		SwingUtilities.invokeAndWait(dialog::goToPrevious);
		Assertions.assertEquals(0, dialog.currentDifference());
		SwingUtilities.invokeAndWait(dialog::dispose);
	}

	@Test
	void twoRecordsThatAreTheSameHaveNoDifference() throws Exception{
		final RecordDiffDialog dialog = open(model(name("type given\nvalue Giovanni"), name("type given\nvalue Giovanni")));

		Assertions.assertEquals(0, dialog.differenceCount());
		SwingUtilities.invokeAndWait(dialog::goToNext);
		Assertions.assertEquals(-1, dialog.currentDifference());
		SwingUtilities.invokeAndWait(dialog::dispose);
	}

	@Test
	void twoDifferentRecordsOfAKindFaceEachOtherAsOneDifference() throws Exception{
		//the same field citing two different media: one against the other, not one block and then the other
		final RecordDiffDialog dialog = open(model("preferred_image {\nmedia M1\ncrop {\nx 1\n}\n}", "preferred_image {\nmedia M2\ncrop {\nx 2\n}\n}"));
		final String[] left = lines(dialog.leftText());
		final String[] right = lines(dialog.rightText());

		Assertions.assertEquals(rowOf(left, "media M1"), rowOf(right, "media M2"));
		Assertions.assertEquals(rowOf(left, "x 1"), rowOf(right, "x 2"));
		SwingUtilities.invokeAndWait(dialog::dispose);
	}

	@Test
	void theWindowOpensAtTheFirstLine() throws Exception{
		final StringBuilder many = new StringBuilder();
		for(int k = 0; k < 60; k ++)
			many.append(name("type given\nvalue N" + k)).append("\n");
		final RecordDiffDialog dialog = open(model(many.toString(), many.toString().replace("N1", "M1")));
		SwingUtilities.invokeAndWait(() -> {
			dialog.setModalityType(Dialog.ModalityType.MODELESS);
			dialog.setVisible(true);
		});
		try{
			for(int i = 0; i < 10; i ++){
				Thread.sleep(100);
				Assertions.assertEquals(0, dialog.verticalScrollPosition(), "after " + (i + 1) * 100 + " ms");
			}
		}
		finally{
			SwingUtilities.invokeAndWait(dialog::dispose);
		}
	}

	@SuppressWarnings("unused")
	private static List<String> unused(){
		return List.of();
	}

}
