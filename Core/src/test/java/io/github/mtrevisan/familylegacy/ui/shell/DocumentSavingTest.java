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
package io.github.mtrevisan.familylegacy.ui.shell;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.FLEFWriter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.ModelHistory;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.components.projections.individualtree.TreeType;
import io.github.mtrevisan.familylegacy.ui.helpers.ReportText;
import io.github.mtrevisan.familylegacy.ui.tools.files.FileMenuController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.AbstractButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;


/**
 * There are no unsaved changes: every change is written to the file of the document by itself, closing the window (and every other
 * way of leaving the document) writes what is pending without asking, and the only question is for a document that has no file.
 */
class DocumentSavingTest{

	private static final String FLEF = """
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
		}
		""";


	/** Presses the first of the buttons, whichever is in the dialog that is showing, as the user would: a modal dialog blocks the caller. */
	private static Thread pressOneOf(final String... texts){
		final Thread thread = new Thread(() -> {
			final long deadline = System.currentTimeMillis() + 15_000;
			while(System.currentTimeMillis() < deadline){
				final AtomicBoolean pressed = new AtomicBoolean();
				try{
					SwingUtilities.invokeAndWait(() -> {
						for(final Window window : Window.getWindows())
							if(window instanceof JDialog dialog && dialog.isShowing()){
								final AbstractButton button = find(dialog, texts);
								if(button != null){
									button.doClick();
									pressed.set(true);
									return;
								}
							}
					});
				}
				catch(final Exception ignored){
					return;
				}
				if(pressed.get())
					return;

				try{
					Thread.sleep(50);
				}
				catch(final InterruptedException ignored){
					return;
				}
			}
		}, "button-presser");
		thread.setDaemon(true);
		thread.start();
		return thread;
	}

	private static AbstractButton find(final Container container, final String[] texts){
		for(final Component child : container.getComponents()){
			if(child instanceof AbstractButton button)
				for(final String text : texts)
					if(text.equals(button.getText()))
						return button;
			if(child instanceof Container nested){
				final AbstractButton found = find(nested, texts);
				if(found != null)
					return found;
			}
		}
		return null;
	}

	private static void flushEventQueue() throws Exception{
		SwingUtilities.invokeAndWait(() -> {});
	}

	private static void waitFor(final BooleanSupplier condition) throws InterruptedException{
		final long deadline = System.currentTimeMillis() + 20_000;
		while(!condition.getAsBoolean() && System.currentTimeMillis() < deadline)
			Thread.sleep(50);
	}

	private static File write(final Path path) throws IOException{
		Files.writeString(path, FLEF, StandardCharsets.UTF_8);
		return path.toFile();
	}

	private static boolean contains(final File file, final String text){
		try{
			return Files.readString(file.toPath(), StandardCharsets.UTF_8).contains(text);
		}
		catch(final IOException e){
			return false;
		}
	}

	private static FamilyLegacyFrame frame(final AtomicInteger terminated, final File toOpen) throws Exception{
		final FamilyLegacyFrame[] frame = new FamilyLegacyFrame[1];
		SwingUtilities.invokeAndWait(() -> {
			frame[0] = new FamilyLegacyFrame(TreeType.FAMILY, new FLEFModel());
			frame[0].setTerminator(terminated::incrementAndGet);
		});
		if(toOpen != null){
			SwingUtilities.invokeLater(() -> frame[0].openFile(toOpen));
			waitFor(() -> toOpen.equals(frame[0].fileController().currentFile()));
			flushEventQueue();
			Assertions.assertEquals(toOpen, frame[0].fileController().currentFile());
		}
		return frame[0];
	}

	private static ModelHistory history(final FamilyLegacyFrame frame) throws Exception{
		final Field field = FamilyLegacyFrame.class.getDeclaredField("history");
		field.setAccessible(true);
		return (ModelHistory)field.get(frame);
	}

	private static boolean modified(final FamilyLegacyFrame frame){
		try{
			return history(frame).isModified();
		}
		catch(final Exception e){
			throw new IllegalStateException(e);
		}
	}

	/** Adds a place to the model, as an editor would, and records the step, as the next event of the user does. */
	private static void edit(final FamilyLegacyFrame frame, final String id) throws Exception{
		SwingUtilities.invokeAndWait(() -> {
			frame.model().addRecord(FLEFRecord.createMainRecord(id, "place"));
			try{
				history(frame).checkpoint();
			}
			catch(final Exception e){
				throw new IllegalStateException(e);
			}
		});
	}

	private static void pressClose(final FamilyLegacyFrame frame){
		SwingUtilities.invokeLater(() -> frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)));
	}

	private static void close(final FamilyLegacyFrame frame) throws Exception{
		SwingUtilities.invokeAndWait(frame::dispose);
	}


	@Test
	void aChangeIsWrittenToTheFileByItself(@TempDir final Path directory) throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The window needs a display");
		final File file = write(directory.resolve("tree.flef"));
		final FamilyLegacyFrame frame = frame(new AtomicInteger(), file);
		try{
			Assertions.assertFalse(modified(frame));

			edit(frame, "P9");

			waitFor(() -> contains(file, "P9"));
			Assertions.assertTrue(contains(file, "P9"), "the change was not written");
			waitFor(() -> !modified(frame));
			Assertions.assertFalse(history(frame).isModified());
		}
		finally{
			close(frame);
		}
	}

	@Test
	void theCloseButtonWritesWhatIsPendingAndClosesWithoutAskingAnything(@TempDir final Path directory) throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The window needs a display");
		final File file = write(directory.resolve("tree.flef"));
		final AtomicInteger terminated = new AtomicInteger();
		final FamilyLegacyFrame frame = frame(terminated, file);
		try{
			Assertions.assertEquals(JFrame.DO_NOTHING_ON_CLOSE, frame.getDefaultCloseOperation());
			edit(frame, "P9");

			//right away, before the delay of the automatic save is over: nobody presses a button
			pressClose(frame);
			flushEventQueue();

			Assertions.assertTrue(contains(file, "P9"));
			Assertions.assertEquals(1, terminated.get());
			Assertions.assertFalse(frame.isDisplayable());
		}
		finally{
			close(frame);
		}
	}

	@Test
	void openingAnotherFileWritesTheChangesOfTheOpenOneFirst(@TempDir final Path directory) throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The window needs a display");
		final File first = write(directory.resolve("first.flef"));
		final File second = write(directory.resolve("second.flef"));
		final FamilyLegacyFrame frame = frame(new AtomicInteger(), first);
		try{
			edit(frame, "P9");

			SwingUtilities.invokeLater(() -> frame.fileController().openRecent(second));
			waitFor(() -> second.equals(frame.fileController().currentFile()));
			flushEventQueue();

			Assertions.assertEquals(second, frame.fileController().currentFile());
			Assertions.assertTrue(contains(first, "P9"));
			Assertions.assertFalse(contains(second, "P9"));
		}
		finally{
			frame.fileController().recentFiles().remove(first);
			frame.fileController().recentFiles().remove(second);
			close(frame);
		}
	}

	@Test
	void aDocumentWithNoFileAsksWhereToWriteAtTheFirstChangeAndAtTheEnd() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The window needs a display");
		final AtomicInteger terminated = new AtomicInteger();
		final FamilyLegacyFrame frame = frame(terminated, null);
		try{
			//an empty document has nothing to write
			pressClose(frame);
			flushEventQueue();
			Assertions.assertEquals(1, terminated.get());
			SwingUtilities.invokeAndWait(() -> frame.setVisible(false));
		}
		finally{
			close(frame);
		}

		final AtomicInteger again = new AtomicInteger();
		final FamilyLegacyFrame second = frame(again, null);
		try{
			//the first change: the file is asked for, and the user does not choose one
			final Thread chooser = pressOneOf(UIManager.getString("FileChooser.cancelButtonText"));
			edit(second, "P9");
			chooser.join(20_000);
			flushEventQueue();
			Assertions.assertNull(second.fileController().currentFile());
			Assertions.assertTrue(history(second).isModified());

			//closing: the changes would be lost, and the user is told; cancel keeps the window
			Thread user = pressOneOf(I18N.t("button.cancel"));
			pressClose(second);
			user.join(20_000);
			flushEventQueue();
			Assertions.assertEquals(0, again.get());
			Assertions.assertTrue(second.isDisplayable());

			//discard: the window is closed
			user = pressOneOf(I18N.t("button.discard"));
			pressClose(second);
			user.join(20_000);
			flushEventQueue();
			Assertions.assertEquals(1, again.get());
		}
		finally{
			close(second);
		}
	}


	@Test
	void aBookmarkOfARecordThatIsNotInTheFileIsNotApplied() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The window needs a display");
		final FamilyLegacyFrame frame = frame(new AtomicInteger(), null);
		try{
			final String root = frame.switcher().getCurrentRootId();
			final io.github.mtrevisan.familylegacy.ui.components.projections.bookmarks.Bookmark bookmark =
				new io.github.mtrevisan.familylegacy.ui.components.projections.bookmarks.Bookmark("b", "gone",
					io.github.mtrevisan.familylegacy.ui.components.projections.bookmarks.BookmarkType.BIOLOGICAL_TREE, "I999",
					java.util.Map.of(), 0);

			//the user is told, and presses OK
			final Thread user = pressOneOf(UIManager.getString("OptionPane.okButtonText"));
			SwingUtilities.invokeLater(() -> frame.applyBookmark(bookmark));
			user.join(20_000);
			flushEventQueue();

			Assertions.assertFalse(user.isAlive(), "the user was not told");
			Assertions.assertEquals(root, frame.switcher().getCurrentRootId());
		}
		finally{
			close(frame);
		}
	}

	private static FileMenuController controller(final JFrame owner, final AtomicInteger replaced){
		return new FileMenuController(owner, FLEFModel::new, model -> replaced.incrementAndGet(), new ModelHistory(new FLEFModel()));
	}

	@Test
	void aRecentFileThatIsNotThereIsRemovedFromTheListIfTheUserAgrees(@TempDir final Path directory) throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The window needs a display");
		final JFrame owner = new JFrame();
		final AtomicInteger replaced = new AtomicInteger();
		final FileMenuController controller = controller(owner, replaced);
		final File missing = directory.resolve("moved-away.flef").toFile();
		final File missingToo = directory.resolve("deleted.flef").toFile();
		try{
			controller.recentFiles().add(missing);
			controller.recentFiles().add(missingToo);

			//"No": the list is left as it is
			Thread user = pressOneOf(UIManager.getString("OptionPane.noButtonText"));
			SwingUtilities.invokeLater(() -> controller.openRecent(missing));
			user.join(20_000);
			flushEventQueue();
			Assertions.assertTrue(controller.recentFiles().list().contains(missing));

			//"Yes": the file is removed, the other one stays
			user = pressOneOf(UIManager.getString("OptionPane.yesButtonText"));
			SwingUtilities.invokeLater(() -> controller.openRecent(missing));
			user.join(20_000);
			flushEventQueue();
			Assertions.assertFalse(controller.recentFiles().list().contains(missing));
			Assertions.assertTrue(controller.recentFiles().list().contains(missingToo));
			Assertions.assertEquals(0, replaced.get());
		}
		finally{
			controller.recentFiles().remove(missing);
			controller.recentFiles().remove(missingToo);
			SwingUtilities.invokeAndWait(owner::dispose);
		}
	}

	@Test
	void anEmptyDocumentCanBeWrittenAndReadBack(){
		//a new document is created on its file: the empty model has to be a valid file
		final String text = FLEFWriter.createCompact().writeToString(new FLEFModel());

		final FLEFModel read = Assertions.assertDoesNotThrow(() -> new FLEFParser().parse(text), text);
		Assertions.assertEquals(0, read.getRecordCount());
	}

	@Test
	void backupAndRestoreAreInTheFileMenuAndEnabledWhenThereIsAModel() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The window needs a display");
		final FamilyLegacyFrame frame = frame(new AtomicInteger(), null);
		try{
			final JMenu file = frame.getJMenuBar().getMenu(0);
			boolean backup = false;
			boolean restore = false;
			for(int i = 0; i < file.getItemCount(); i ++){
				final JMenuItem item = file.getItem(i);
				if(item == null)
					continue;
				if(I18N.t("menu.file.backup").equals(item.getText())){
					backup = true;
					Assertions.assertTrue(item.isEnabled());
				}
				if(I18N.t("menu.file.restore").equals(item.getText())){
					restore = true;
					Assertions.assertTrue(item.isEnabled());
				}
			}
			Assertions.assertTrue(backup && restore);
		}
		finally{
			close(frame);
		}
	}

	@Test
	void theReportsAlignTheValuesWhateverTheLengthOfTheLabels(){
		final StringBuilder sb = new StringBuilder();
		ReportText.line(sb, "text.report.name", "a");
		ReportText.line(sb, "text.diagnostics.heap_allocated", "b");
		ReportText.modelStatistics(sb, new FLEFParser().parse(FLEF));
		ReportText.modelStatistics(sb, null);

		final String text = sb.toString();
		final String[] lines = text.split("\n");
		Assertions.assertEquals(I18N.t("text.report.name") + ":", lines[0].substring(0, I18N.t("text.report.name").length() + 1));
		Assertions.assertEquals(lines[0].lastIndexOf('a'), lines[1].lastIndexOf('b'), text);
		Assertions.assertTrue(text.contains(I18N.t("text.report.total_records") + ":"), text);
		Assertions.assertTrue(text.contains("individual"), text);
		Assertions.assertTrue(text.contains(I18N.t("text.report.no_model")), text);
	}

}
