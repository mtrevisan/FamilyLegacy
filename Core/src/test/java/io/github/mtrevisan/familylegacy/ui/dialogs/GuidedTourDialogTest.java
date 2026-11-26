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

import io.github.mtrevisan.familylegacy.ui.dialogs.help.GuidedTourDialog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


class GuidedTourDialogTest{

	private static JDialog findTour(){
		for(final Window window : Window.getWindows())
			if(window instanceof JDialog dialog && window.isShowing() && dialog.getTitle().equals(
					io.github.mtrevisan.familylegacy.i18n.I18N.t("dialog.tour.title")))
				return dialog;
		return null;
	}

	private static JButton button(final Container container, final String text){
		for(final Component component : container.getComponents()){
			if(component instanceof JButton b && text.equals(b.getText()))
				return b;
			if(component instanceof Container child){
				final JButton found = button(child, text);
				if(found != null)
					return found;
			}
		}
		return null;
	}


	@Test
	void walksTheStepsAndRestoresTheGlassPaneWhenItEnds() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");

		final JFrame frame = new JFrame("main");
		final JMenuBar bar = new JMenuBar();
		final JMenu file = new JMenu("File");
		bar.add(file);
		frame.setJMenuBar(bar);
		frame.setSize(800, 600);
		frame.setVisible(true);
		final Component original = frame.getGlassPane();
		final AtomicInteger closed = new AtomicInteger();
		try{
			SwingUtilities.invokeAndWait(() -> GuidedTourDialog.show(frame, List.of(
				new GuidedTourDialog.Step("one", "<html>first</html>", () -> file),
				new GuidedTourDialog.Step("two", "<html>second</html>", null)), closed::incrementAndGet));
			DialogsEscapeTest.flushEventQueue();

			final JDialog tour = findTour();
			Assertions.assertNotNull(tour);
			Assertions.assertNotSame(original, frame.getGlassPane(), "the glass pane outlines the part of the step");

			//the first step has no previous one
			Assertions.assertFalse(button(tour, "Back").isEnabled());
			SwingUtilities.invokeAndWait(() -> button(tour, "Next").doClick());
			Assertions.assertTrue(button(tour, "Back").isEnabled());
			Assertions.assertNotNull(button(tour, "Finish"), "the last step finishes the tour");

			SwingUtilities.invokeAndWait(() -> button(tour, "Finish").doClick());
			DialogsEscapeTest.flushEventQueue();
			Assertions.assertFalse(tour.isDisplayable());
			Assertions.assertSame(original, frame.getGlassPane());
			Assertions.assertEquals(1, closed.get());
			Assertions.assertTrue(frame.isDisplayable(), "the application window stays");

			//Esc ends it too, and only it
			SwingUtilities.invokeAndWait(() -> GuidedTourDialog.show(frame, List.of(
				new GuidedTourDialog.Step("one", "<html>first</html>", () -> file)), closed::incrementAndGet));
			DialogsEscapeTest.flushEventQueue();
			final JDialog again = findTour();
			Assertions.assertNotNull(again);
			DialogsEscapeTest.typeEscape(again);
			Assertions.assertFalse(again.isDisplayable());
			Assertions.assertSame(original, frame.getGlassPane());
			Assertions.assertEquals(2, closed.get());
			Assertions.assertTrue(frame.isDisplayable());
		}
		finally{
			frame.dispose();
		}
	}

}
