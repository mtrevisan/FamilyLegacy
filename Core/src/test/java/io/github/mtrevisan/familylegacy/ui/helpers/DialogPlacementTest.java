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
package io.github.mtrevisan.familylegacy.ui.helpers;

import io.github.mtrevisan.familylegacy.consanguinity.ConsanguinityCalculator;
import io.github.mtrevisan.familylegacy.consanguinity.ConsanguinityReport;
import io.github.mtrevisan.familylegacy.ui.tools.consanguinity.ConsanguinityReportDialog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


/** The modal dialogs are centered, and not taller than the screen allows. */
class DialogPlacementTest{

	private static final Rectangle SCREEN = new Rectangle(0, 0, 1920, 1080);


	@Test
	void aDialogIsCenteredOverItsOwner(){
		final Rectangle placed = DialogPlacement.place(new Rectangle(5, 5, 400, 300), new Rectangle(100, 100, 1000, 600), SCREEN);

		Assertions.assertEquals(new Rectangle(400, 250, 400, 300), placed);
	}

	@Test
	void aDialogWithoutOwnerIsCenteredOnTheScreen(){
		final Rectangle placed = DialogPlacement.place(new Rectangle(0, 0, 800, 600), SCREEN, SCREEN);

		Assertions.assertEquals(new Rectangle(560, 240, 800, 600), placed);
	}

	@Test
	void aDialogTooTallIsBroughtToTheShareOfTheScreen(){
		final Rectangle placed = DialogPlacement.place(new Rectangle(0, 0, 800, 5_000), SCREEN, SCREEN);

		Assertions.assertEquals(1080 * DialogPlacement.MAX_HEIGHT_PERCENT / 100, placed.height);
		//and it is still centered
		Assertions.assertEquals((1080 - placed.height) / 2, placed.y);
		Assertions.assertEquals(800, placed.width);
	}

	@Test
	void aDialogTooWideIsBroughtToTheShareOfTheScreen(){
		final Rectangle placed = DialogPlacement.place(new Rectangle(0, 0, 9_000, 400), SCREEN, SCREEN);

		Assertions.assertEquals(1920 * DialogPlacement.MAX_WIDTH_PERCENT / 100, placed.width);
		Assertions.assertTrue(placed.x >= 0);
	}

	@Test
	void anOwnerNearTheEdgeDoesNotTakeTheDialogOutOfTheScreen(){
		final Rectangle placed = DialogPlacement.place(new Rectangle(0, 0, 600, 400), new Rectangle(1700, 900, 220, 180), SCREEN);

		Assertions.assertTrue(SCREEN.contains(placed), placed.toString());
	}

	@Test
	void theBarsOfTheSystemAreNotUsed(){
		//a screen with a bar of 40 pixels at the bottom
		final Rectangle usable = new Rectangle(0, 0, 1920, 1040);

		final Rectangle placed = DialogPlacement.place(new Rectangle(0, 0, 400, 5_000), usable, usable);

		Assertions.assertTrue(usable.contains(placed), placed.toString());
	}


	/** @return	The bounds that a dialog has once it is open, and the usable area of the screen it is on. */
	private static Rectangle[] open(final JDialog dialog) throws Exception{
		DialogPlacement.install();
		final AtomicReference<Rectangle> bounds = new AtomicReference<>();
		final Timer closer = new Timer(500, e -> {
			bounds.set(dialog.getBounds());
			dialog.dispose();
		});
		closer.setRepeats(false);
		closer.start();
		SwingUtilities.invokeAndWait(() -> dialog.setVisible(true));
		final GraphicsConfiguration configuration = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
			.getDefaultConfiguration();
		final Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
		final Rectangle screen = configuration.getBounds();
		return new Rectangle[]{bounds.get(), new Rectangle(screen.x + insets.left, screen.y + insets.top,
			screen.width - insets.left - insets.right, screen.height - insets.top - insets.bottom)};
	}

	@Test
	void aRealModalDialogOpensCenteredAndNotTallerThanTheScreen() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialog needs a display");
		final AtomicReference<JDialog> created = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			final JDialog dialog = new JDialog((Window)null, "test", JDialog.ModalityType.APPLICATION_MODAL);
			final JPanel content = new JPanel();
			//the height of a report that has all its text in a text area
			content.setPreferredSize(new Dimension(600, 6_000));
			dialog.add(content);
			dialog.pack();
			//the corner where a dialog that was centered before it was packed would end
			dialog.setLocation(3, 3);
			created.set(dialog);
		});

		final Rectangle[] result = open(created.get());

		final Rectangle bounds = result[0];
		final Rectangle usable = result[1];
		Assertions.assertTrue(bounds.height <= usable.height * DialogPlacement.MAX_HEIGHT_PERCENT / 100, bounds.toString());
		Assertions.assertTrue(Math.abs(bounds.getCenterX() - usable.getCenterX()) <= 2, bounds + " in " + usable);
		Assertions.assertTrue(Math.abs(bounds.getCenterY() - usable.getCenterY()) <= 2, bounds + " in " + usable);
	}

	@Test
	void theReportOfConsanguinityIsContainedInHeightWhateverItsLength() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialog needs a display");
		final List<ConsanguinityReport.ConsanguineousUnion> unions = new ArrayList<>();
		for(int i = 0; i < 300; i ++)
			unions.add(new ConsanguinityReport.ConsanguineousUnion("I" + i, "Person " + i, "J" + i, "Partner " + i, List.of(), List.of(),
				0.0625, 0.125, List.of(new ConsanguinityCalculator.CommonAncestor("A" + i, 2, 2))));
		final ConsanguinityReport report = new ConsanguinityReport(unions, List.of(), 1. / 256., 1. / 64.);
		final AtomicReference<ConsanguinityReportDialog> created = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> created.set(new ConsanguinityReportDialog(null, new io.github.mtrevisan.familylegacy.io.model.FLEFModel(), report, id -> id)));

		final Rectangle[] result = open(created.get());

		Assertions.assertTrue(result[0].height <= result[1].height * DialogPlacement.MAX_HEIGHT_PERCENT / 100, result[0].toString());
		Assertions.assertTrue(result[0].height < 700, result[0].toString());
		Assertions.assertTrue(Math.abs(result[0].getCenterX() - result[1].getCenterX()) <= 2, result[0] + " in " + result[1]);
		Assertions.assertTrue(Math.abs(result[0].getCenterY() - result[1].getCenterY()) <= 2, result[0] + " in " + result[1]);
	}

}
