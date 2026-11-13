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
package io.github.mtrevisan.familylegacy.ui.tools.reports;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;


/** The progress dialog of the reports: the bar has the whole height it asks for, and the task is told at once when the user cancels. */
class ReportProgressDialogLayoutTest{

	private static JProgressBar barOf(final Container container){
		for(final Component child : container.getComponents()){
			if(child instanceof final JProgressBar bar)
				return bar;
			if(child instanceof final Container inner){
				final JProgressBar found = barOf(inner);
				if(found != null)
					return found;
			}
		}
		return null;
	}


	@Test
	void theBarIsNotSqueezedByTheHeightOfTheWindow() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

		final int[] heights = new int[3];
		SwingUtilities.invokeAndWait(() -> {
			final ProgressDialog dialog = new ProgressDialog(null, "Report");
			dialog.validate();
			final JProgressBar bar = barOf(dialog);
			heights[0] = bar.getHeight();
			heights[1] = bar.getPreferredSize().height;
			//the part of the bar that is inside the panel that holds it: what is outside is not painted
			heights[2] = Math.min(bar.getHeight(), bar.getParent().getHeight() - bar.getY());
			dialog.dispose();
		});

		Assertions.assertTrue(heights[0] >= heights[1], "bar " + heights[0] + " of " + heights[1]);
		Assertions.assertEquals(heights[0], heights[2], "the bar is cut by its panel");
	}

	@Test
	void aLongMessageDoesNotResizeTheDialogNorTheBar() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

		final int[] widths = new int[4];
		SwingUtilities.invokeAndWait(() -> {
			final ProgressDialog dialog = new ProgressDialog(null, "Report");
			dialog.validate();
			final JProgressBar bar = barOf(dialog);
			widths[0] = dialog.getWidth();
			widths[1] = bar.getWidth();
			dialog.update(10, "x".repeat(2000));
		});
		//update() is queued on the EDT: wait for it
		SwingUtilities.invokeAndWait(() -> {});
		SwingUtilities.invokeAndWait(() -> {
			for(final java.awt.Window window : java.awt.Window.getWindows())
				if(window instanceof final ProgressDialog dialog){
					dialog.validate();
					widths[2] = dialog.getWidth();
					widths[3] = barOf(dialog).getWidth();
					dialog.dispose();
				}
		});

		Assertions.assertEquals(widths[0], widths[2], "the dialog changed its width");
		Assertions.assertEquals(widths[1], widths[3], "the bar changed its width");
	}

	@Test
	void theTaskIsToldOnceWhenTheUserCancelsWhicheverTheWay() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

		final int[] told = new int[3];
		SwingUtilities.invokeAndWait(() -> {
			for(int way = 0; way < 3; way ++){
				final ProgressDialog dialog = new ProgressDialog(null, "Report");
				final int index = way;
				dialog.onCancel(() -> told[index] ++);
				dialog.validate();
				//ESC is the only key, and there is no button: the dialog is cancelled with it or by closing the window
				final ActionListener escape = dialog.getRootPane()
					.getActionForKeyStroke(io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper.ESCAPE_STROKE);
				Assertions.assertNotNull(escape, "ESC is not bound");
				final Runnable close = () -> {
					for(final WindowListener listener : dialog.getWindowListeners())
						listener.windowClosing(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
				};
				final ActionEvent key = new ActionEvent(dialog, ActionEvent.ACTION_PERFORMED, "esc");
				if(way == 0)
					escape.actionPerformed(key);
				else if(way == 1)
					//the window is closed
					close.run();
				else{
					//twice: told once
					escape.actionPerformed(key);
					close.run();
				}
				Assertions.assertTrue(dialog.isCancelled());
				dialog.dispose();
			}
		});

		Assertions.assertArrayEquals(new int[]{1, 1, 1}, told);
	}

}
