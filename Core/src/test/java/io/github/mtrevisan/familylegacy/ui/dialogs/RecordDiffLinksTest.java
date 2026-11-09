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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JDialog;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


/** The identifiers of the records in the comparison of two records are links to the editors of those records. */
class RecordDiffLinksTest{

	/** Two individuals that cite the same source, and a note that mentions an identifier that is not a record. */
	private static FLEFModel model(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("S1", SourceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(SourceReader.TAG_TITLE)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, "Parish register"))));
		for(final String id : List.of("I1", "I2"))
			model.addRecord(FLEFRecord.createMainRecord(id, IndividualReader.TYPE)
				.addChild(FLEFRecord.createChildWithTag(SourceCitationReader.TAG_SOURCE)
					.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_SOURCE, "S1"))));
		return model;
	}

	private static List<JTextArea> areas(final Container container){
		final List<JTextArea> found = new ArrayList<>();
		for(final Component child : container.getComponents()){
			if(child instanceof JTextArea area)
				found.add(area);
			if(child instanceof Container nested)
				found.addAll(areas(nested));
		}
		return found;
	}

	private static void click(final JTextArea area, final int offset){
		try{
			final Rectangle2D box = area.modelToView2D(offset);
			//a few pixels into the character at the offset, not on the boundary between two
			final Point point = new Point((int)box.getX() + 3, (int)box.getCenterY());
			area.dispatchEvent(new MouseEvent(area, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, point.x, point.y, 1, false,
				MouseEvent.BUTTON1));
		}
		catch(final BadLocationException e){
			throw new IllegalStateException(e);
		}
	}


	@Test
	void onlyTheIdentifiersOfTheRecordsOfTheModelAreLinks(){
		final FLEFModel model = model();

		final List<int[]> ranges = RecordDiffDialog.linkRanges("individual: I1\nsource: S1\nnote: see X99 and S1\nsex: M", model);

		//I1, S1 and the S1 of the note; X99 is not a record
		Assertions.assertEquals(3, ranges.size());
		Assertions.assertEquals("I1", "individual: I1\nsource: S1".substring(ranges.getFirst()[0], ranges.getFirst()[1]));
	}

	@Test
	void withoutAModelThereAreNoLinks(){
		Assertions.assertTrue(RecordDiffDialog.linkRanges("individual: I1", null).isEmpty());
		Assertions.assertTrue(RecordDiffDialog.linkRanges("individual: I1", new FLEFModel()).isEmpty());
	}

	@Test
	void aClickOnAnIdentifierOpensTheEditorOfThatRecord() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialog needs a display");
		final FLEFModel model = model();
		final AtomicReference<RecordDiffDialog> created = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> created.set(new RecordDiffDialog(null, "compare", model.getRecordById("I1"),
			model.getRecordById("I2"), true, model)));
		final RecordDiffDialog dialog = created.get();
		final JTextArea left = areas(dialog).getFirst();
		final String text = left.getText();
		final AtomicReference<String> opened = new AtomicReference<>();

		//the editor is modal: a timer notes which dialog is open over the comparison, and closes it
		final Timer closer = new Timer(100, e -> {
			for(final Window window : Window.getWindows())
				if(window instanceof JDialog other && other != dialog && other.isVisible()){
					opened.set(other.getClass().getSimpleName());
					other.dispose();
				}
		});
		closer.start();
		try{
			SwingUtilities.invokeAndWait(() -> click(left, text.indexOf("S1")));
			Assertions.assertEquals("SourceRecordDialog", opened.get());

			//a click elsewhere in the text opens nothing
			opened.set(null);
			SwingUtilities.invokeAndWait(() -> click(left, text.indexOf("source")));
			Thread.sleep(400);
			Assertions.assertNull(opened.get());
		}
		finally{
			closer.stop();
			SwingUtilities.invokeAndWait(dialog::dispose);
		}
	}

	@Test
	void withoutAModelAClickOpensNothing() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialog needs a display");
		final FLEFModel model = model();
		final AtomicReference<RecordDiffDialog> created = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> created.set(new RecordDiffDialog(null, "compare", model.getRecordById("I1"),
			model.getRecordById("I2"), true)));
		final RecordDiffDialog dialog = created.get();
		final JTextArea left = areas(dialog).getFirst();
		final String text = left.getText();
		final AtomicReference<String> opened = new AtomicReference<>();
		final Timer closer = new Timer(100, e -> {
			for(final Window window : Window.getWindows())
				if(window instanceof JDialog other && other != dialog && other.isVisible()){
					opened.set(other.getClass().getSimpleName());
					other.dispose();
				}
		});
		closer.start();
		try{
			SwingUtilities.invokeAndWait(() -> click(left, text.indexOf("S1")));
			Thread.sleep(400);
			Assertions.assertNull(opened.get());
		}
		finally{
			closer.stop();
			SwingUtilities.invokeAndWait(dialog::dispose);
		}
	}

}
