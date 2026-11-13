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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;


/** The progress dialog is closed when it is asked to, not later: what follows may be a window that must not have it over or under it. */
class ProgressDialogTest{

	@Test
	void closingOnTheEventThreadClosesAtOnce() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
		final boolean[] displayable = new boolean[]{true};
		SwingUtilities.invokeAndWait(() -> {
			final ProgressDialog dialog = new ProgressDialog(null, "title", "message");
			dialog.addNotify();
			dialog.close();
			//not queued: what is done next in the same event already finds it closed
			displayable[0] = dialog.isDisplayable();
		});

		Assertions.assertFalse(displayable[0]);
	}

	@Test
	void closingFromAnotherThreadClosesOnTheEventThread() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
		final ProgressDialog[] holder = new ProgressDialog[1];
		SwingUtilities.invokeAndWait(() -> {
			holder[0] = new ProgressDialog(null, "title", "message");
			holder[0].addNotify();
		});

		holder[0].close();
		SwingUtilities.invokeAndWait(() -> {});

		Assertions.assertFalse(holder[0].isDisplayable());
	}

}
