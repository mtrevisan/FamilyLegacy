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
