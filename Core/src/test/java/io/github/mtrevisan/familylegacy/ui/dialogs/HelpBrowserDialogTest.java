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

import io.github.mtrevisan.familylegacy.ui.dialogs.help.HelpBrowserDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.help.HelpTopics;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;


class HelpBrowserDialogTest{

	@Test
	void theHelpIsOneWindowThatClosesWithEscAndOnlyItself() throws InvocationTargetException, InterruptedException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The help needs a display");

		final JDialog below = new JDialog((Window)null, "below");
		below.pack();
		SwingUtilities.invokeAndWait(() -> {
			HelpBrowserDialog.show(below, HelpTopics.INDEX);
			//opening it again must not open a second one
			HelpBrowserDialog.show(below, HelpTopics.GETTING_STARTED);
		});

		try{
			final List<Window> helps = helpWindows();
			Assertions.assertEquals(1, helps.size());
			Assertions.assertTrue(helps.getFirst().isShowing());

			SwingUtilities.invokeAndWait(() -> DialogsEscapeTest.pressEscape((JDialog)helps.getFirst()));

			Assertions.assertTrue(helpWindows().isEmpty(), "ESC did not close the help");
			Assertions.assertTrue(below.isDisplayable(), "ESC in the help closed the window below it");

			//closed, it can be opened again
			SwingUtilities.invokeAndWait(() -> HelpBrowserDialog.show(below, HelpTopics.INDEX));
			Assertions.assertEquals(1, helpWindows().size());
		}
		finally{
			SwingUtilities.invokeAndWait(() -> {
				for(final Window help : helpWindows())
					help.dispose();
				below.dispose();
			});
		}
	}

	/** Paints the pages to PNG files (to look at them, set HELP_VIEW_IMAGES to a folder: otherwise a temporary one is used). */
	@Test
	void paintThePages(@TempDir final Path temporaryFolder) throws InvocationTargetException, InterruptedException, IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The help needs a display");

		final String requestedFolder = System.getenv("HELP_VIEW_IMAGES");
		final File folder = (requestedFolder != null? new File(requestedFolder): temporaryFolder.toFile());

		for(final String page : List.of(HelpTopics.INDEX, "editing.md", "shortcuts.md", "assistant.md")){
			SwingUtilities.invokeAndWait(() -> HelpBrowserDialog.show(null, page));
			Thread.sleep(400);
			SwingUtilities.invokeAndWait(() -> {
			});

			final Window help = helpWindows().getFirst();
			final BufferedImage image = new BufferedImage(help.getWidth(), help.getHeight(), BufferedImage.TYPE_INT_RGB);
			SwingUtilities.invokeAndWait(() -> help.paint(image.getGraphics()));
			final File png = new File(folder, "help-" + page.replace(".md", "") + ".png");
			Assertions.assertTrue(ImageIO.write(image, "png", png));
			Assertions.assertTrue(Files.size(png.toPath()) > 0, png.getName());

			SwingUtilities.invokeAndWait(help::dispose);
		}
	}


	private static List<Window> helpWindows(){
		return Arrays.stream(Window.getWindows())
			.filter(window -> window instanceof HelpBrowserDialog && window.isDisplayable())
			.map(Component.class::cast)
			.map(Window.class::cast)
			.toList();
	}

}
