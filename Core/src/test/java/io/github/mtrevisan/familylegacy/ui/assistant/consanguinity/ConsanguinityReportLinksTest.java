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
package io.github.mtrevisan.familylegacy.ui.assistant.consanguinity;

import io.github.mtrevisan.familylegacy.consanguinity.ConsanguinityCalculator;
import io.github.mtrevisan.familylegacy.consanguinity.ConsanguinityReport;
import io.github.mtrevisan.familylegacy.consanguinity.ConsanguinityReportText;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.tools.consanguinity.ConsanguinityReportDialog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JDialog;
import javax.swing.JTextPane;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


/** Every reference of the consanguinity report is a link to the editor of the record. */
class ConsanguinityReportLinksTest{

	private static final List<ConsanguinityCalculator.CommonAncestor> ANCESTORS = List.of(
		new ConsanguinityCalculator.CommonAncestor("A1", 2, 2), new ConsanguinityCalculator.CommonAncestor("A2", 2, 2));

	/** A couple (with a union and a child) and an inbred individual. */
	private static ConsanguinityReport report(){
		final ConsanguinityReport.ConsanguineousUnion union = new ConsanguinityReport.ConsanguineousUnion("I1", "Mario Rossi", "I2",
			"Anna Rossi", List.of("R1", "R2"), List.of("I3", "I4"), 0.0625, 0.125, ANCESTORS);
		final ConsanguinityReport.InbredIndividual inbred = new ConsanguinityReport.InbredIndividual("I3", "Pietro Rossi", 0.0625,
			List.of("I1", "I2"), ANCESTORS);
		return new ConsanguinityReport(List.of(union), List.of(inbred), 1. / 256., 1. / 64.);
	}

	private static FLEFModel model(){
		final FLEFModel model = new FLEFModel();
		for(final String id : List.of("I1", "I2", "I3", "I4", "A1", "A2"))
			model.addRecord(FLEFRecord.createMainRecord(id, IndividualReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("R1", RelationshipReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("R2", RelationshipReader.TYPE));
		return model;
	}


	@Test
	void theSegmentsAreTheTextOfTheReport(){
		final ConsanguinityReport report = report();
		final StringBuilder sb = new StringBuilder();
		for(final ConsanguinityReportText.Segment segment : ConsanguinityReportText.segments(report, id -> "Name of " + id))
			sb.append(segment.text());

		Assertions.assertEquals(ConsanguinityReportText.format(report, id -> "Name of " + id), sb.toString());
	}

	@Test
	void everyReferenceIsALinkToItsRecord(){
		final List<String> linked = ConsanguinityReportText.segments(report(), id -> "Name of " + id).stream()
			.filter(ConsanguinityReportText.Segment::isReference)
			.map(ConsanguinityReportText.Segment::recordId)
			.toList();

		//the partners, the common ancestors, the relationships, the children; then the inbred one (name and identifier) and the ancestors
		Assertions.assertEquals(List.of("I1", "I2", "A1", "A2", "R1", "R2", "I3", "I4", "I3", "I3", "A1", "A2"), linked);
	}

	@Test
	void theNameOfAnAncestorIsTheLinkAndNotTheGenerations(){
		final List<ConsanguinityReportText.Segment> segments = ConsanguinityReportText.ancestorSegments(ANCESTORS, id -> "Name of " + id);

		Assertions.assertEquals("Name of A1", segments.getFirst().text());
		Assertions.assertEquals("A1", segments.getFirst().recordId());
		Assertions.assertFalse(segments.get(1).isReference(), segments.get(1).text());
		Assertions.assertTrue(segments.get(1).text().startsWith(" ("), segments.get(1).text());
	}

	@Test
	void aLongListOfAncestorsKeepsOnlyTheFourThatItShows(){
		final List<ConsanguinityCalculator.CommonAncestor> many = List.of(
			new ConsanguinityCalculator.CommonAncestor("A1", 1, 1), new ConsanguinityCalculator.CommonAncestor("A2", 1, 1),
			new ConsanguinityCalculator.CommonAncestor("A3", 1, 1), new ConsanguinityCalculator.CommonAncestor("A4", 1, 1),
			new ConsanguinityCalculator.CommonAncestor("A5", 1, 1));

		final List<String> linked = ConsanguinityReportText.ancestorSegments(many, id -> id).stream()
			.filter(ConsanguinityReportText.Segment::isReference)
			.map(ConsanguinityReportText.Segment::recordId)
			.toList();

		Assertions.assertEquals(List.of("A1", "A2", "A3", "A4"), linked);
		Assertions.assertTrue(ConsanguinityReportText.ancestors(many, id -> id).endsWith("and 1 more"));
	}

	@Test
	void theDialogLinksTheTextOfTheReferences() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialog needs a display");
		final AtomicReference<ConsanguinityReportDialog> created = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> created.set(new ConsanguinityReportDialog(null, model(), report(), id -> "Name of " + id)));
		final ConsanguinityReportDialog dialog = created.get();
		final String text = dialog.reportText();

		Assertions.assertEquals("I1", dialog.recordIdAt(text.indexOf("Mario Rossi") + 2));
		Assertions.assertEquals("I2", dialog.recordIdAt(text.indexOf("Anna Rossi")));
		Assertions.assertEquals("A1", dialog.recordIdAt(text.indexOf("Name of A1") + 3));
		Assertions.assertEquals("R2", dialog.recordIdAt(text.indexOf("R2")));
		Assertions.assertEquals("I4", dialog.recordIdAt(text.indexOf("I4")));
		//the text around the references, and a position out of the text, are not links
		Assertions.assertNull(dialog.recordIdAt(text.indexOf(" + ")));
		Assertions.assertNull(dialog.recordIdAt(0));
		Assertions.assertNull(dialog.recordIdAt(text.length() + 10));
		SwingUtilities.invokeAndWait(dialog::dispose);
	}

	@Test
	void aReferenceToARecordThatIsNotInTheModelIsWrittenNotLinked() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialog needs a display");
		final FLEFModel withoutI2 = model();
		withoutI2.removeRecord("I2");
		final AtomicReference<ConsanguinityReportDialog> created = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> created.set(new ConsanguinityReportDialog(null, withoutI2, report(), id -> id)));
		final ConsanguinityReportDialog dialog = created.get();
		final String text = dialog.reportText();

		Assertions.assertNull(dialog.recordIdAt(text.indexOf("Anna Rossi")));
		Assertions.assertEquals("I1", dialog.recordIdAt(text.indexOf("Mario Rossi")));
		SwingUtilities.invokeAndWait(dialog::dispose);
	}

	@Test
	void aClickOnALinkOpensTheEditorOfTheRecordAndAClickElsewhereDoesNot() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialog needs a display");
		final FLEFModel model = model();
		final AtomicReference<ConsanguinityReportDialog> created = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> created.set(new ConsanguinityReportDialog(null, model, report(), id -> id)));
		final ConsanguinityReportDialog dialog = created.get();
		final JTextPane area = findTextPane(dialog);
		final String text = dialog.reportText();
		final AtomicReference<String> opened = new AtomicReference<>();

		//the editor is modal: a timer notes the title of the dialog that is open over the report, and closes it
		final Timer closer = new Timer(100, e -> {
			for(final Window window : Window.getWindows())
				if(window instanceof JDialog other && other != dialog && other.isVisible()){
					opened.set(other.getClass().getSimpleName());
					other.dispose();
				}
		});
		closer.start();
		try{
			SwingUtilities.invokeAndWait(() -> click(area, text.indexOf("Mario Rossi") + 2));
			Assertions.assertEquals("IndividualRecordDialog", opened.get());

			opened.set(null);
			SwingUtilities.invokeAndWait(() -> click(area, text.indexOf(" + ")));
			Thread.sleep(400);
			Assertions.assertNull(opened.get());
		}
		finally{
			closer.stop();
			SwingUtilities.invokeAndWait(dialog::dispose);
		}
	}

	private static void click(final JTextPane area, final int offset){
		try{
			final Rectangle2D box = area.modelToView2D(offset);
			final Point point = new Point((int)box.getCenterX(), (int)box.getCenterY());
			area.dispatchEvent(new MouseEvent(area, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, point.x, point.y, 1, false,
				MouseEvent.BUTTON1));
		}
		catch(final BadLocationException e){
			throw new IllegalStateException(e);
		}
	}

	private static JTextPane findTextPane(final Container container){
		for(final Component child : container.getComponents()){
			if(child instanceof JTextPane pane)
				return pane;
			if(child instanceof Container nested){
				final JTextPane found = findTextPane(nested);
				if(found != null)
					return found;
			}
		}
		return null;
	}

}
