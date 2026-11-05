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
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.bookmarks.Bookmark;
import io.github.mtrevisan.familylegacy.ui.components.projections.bookmarks.BookmarkMenu;
import io.github.mtrevisan.familylegacy.ui.components.projections.bookmarks.BookmarkStore;
import io.github.mtrevisan.familylegacy.ui.components.projections.individualtree.TreeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;


/** A bookmark, from the moment it is made to the moment it is applied, in the window of the application. */
class BookmarksEndToEndTest{

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
		individual I2 {
			name {
				part {
					type given
					value Bruno
				}
			}
		}
		""";


	@BeforeEach
	void useABookmarksFileOfItsOwn(@TempDir final Path directory){
		//the bookmarks of the user are left alone
		System.setProperty(BookmarkStore.FILE_PROPERTY_NAME, directory.resolve("bookmarks.properties").toString());
	}

	@AfterEach
	void restoreTheBookmarksFile(){
		System.clearProperty(BookmarkStore.FILE_PROPERTY_NAME);
	}


	private static File write(final Path path) throws IOException{
		Files.writeString(path, FLEF, StandardCharsets.UTF_8);
		return path.toFile();
	}

	private static void waitFor(final BooleanSupplier condition) throws InterruptedException{
		final long deadline = System.currentTimeMillis() + 20_000;
		while(!condition.getAsBoolean() && System.currentTimeMillis() < deadline)
			Thread.sleep(50);
	}

	private static FamilyLegacyFrame frame(final File toOpen) throws Exception{
		return frame(toOpen, new FLEFModel());
	}

	private static FamilyLegacyFrame frame(final File toOpen, final FLEFModel model) throws Exception{
		final FamilyLegacyFrame[] frame = new FamilyLegacyFrame[1];
		SwingUtilities.invokeAndWait(() -> {
			frame[0] = new FamilyLegacyFrame(TreeType.FAMILY, model);
			frame[0].setTerminator(() -> {});
		});
		if(toOpen != null){
			SwingUtilities.invokeLater(() -> frame[0].openFile(toOpen));
			waitFor(() -> toOpen.equals(frame[0].fileController().currentFile()));
			SwingUtilities.invokeAndWait(() -> {});
		}
		return frame[0];
	}

	private static BookmarkMenu bookmarkMenu(final FamilyLegacyFrame frame){
		for(int i = 0; i < frame.getJMenuBar().getMenuCount(); i ++)
			if(frame.getJMenuBar().getMenu(i) instanceof BookmarkMenu menu)
				return menu;
		throw new AssertionError("no Bookmarks menu");
	}

	/** The entries of the menu as the user sees them when it is opened. */
	private static List<JMenuItem> openMenu(final BookmarkMenu menu) throws Exception{
		final List<JMenuItem> items = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			for(final MenuListener listener : menu.getMenuListeners())
				listener.menuSelected(new MenuEvent(menu));
			for(int i = 0; i < menu.getItemCount(); i ++)
				if(menu.getItem(i) != null)
					items.add(menu.getItem(i));
		});
		return items;
	}

	private static JMenuItem find(final List<JMenuItem> items, final String text){
		return items.stream()
			.filter(item -> text.equals(item.getText()))
			.findFirst()
			.orElse(null);
	}

	private static String rootOf(final FamilyLegacyFrame frame) throws Exception{
		final String[] root = new String[1];
		SwingUtilities.invokeAndWait(() -> root[0] = frame.switcher().getCurrentRootId());
		return root[0];
	}

	private static void loadRoot(final FamilyLegacyFrame frame, final String id) throws Exception{
		SwingUtilities.invokeAndWait(() -> frame.switcher().loadRoot(id));
	}

	private static void close(final FamilyLegacyFrame frame) throws Exception{
		SwingUtilities.invokeAndWait(frame::dispose);
	}


	@Test
	void aBookmarkIsMadeListedAndAppliedInTheFileItBelongsTo(@TempDir final Path directory) throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The window needs a display");
		final File file = write(directory.resolve("tree.flef"));
		final File other = write(directory.resolve("other.flef"));
		final FamilyLegacyFrame frame = frame(file);
		try{
			loadRoot(frame, "I2");
			final Bookmark[] made = new Bookmark[1];
			SwingUtilities.invokeAndWait(() -> made[0] = frame.captureCurrentState("Bruno's view"));

			Assertions.assertNotNull(made[0], "nothing was captured");
			Assertions.assertEquals("I2", made[0].rootId());
			Assertions.assertNotNull(made[0].property(BookmarkStore.FILE_PROPERTY), "the bookmark is not tied to the file");
			SwingUtilities.invokeAndWait(() -> frame.bookmarkStore().add(made[0]));

			//it is in the menu, with the name of the person and not its identifier
			List<JMenuItem> items = openMenu(bookmarkMenu(frame));
			final JMenuItem item = find(items, "Bruno's view");
			Assertions.assertNotNull(item, items.stream().map(JMenuItem::getText).toList().toString());
			Assertions.assertTrue(item.getToolTipText().contains("Bruno"), item.getToolTipText());

			//and it takes the view back to the person
			loadRoot(frame, "I1");
			Assertions.assertEquals("I1", rootOf(frame));
			SwingUtilities.invokeAndWait(item::doClick);
			Assertions.assertEquals("I2", rootOf(frame));

			//the bookmark survives the window: the store is read again from its file
			Assertions.assertEquals(1, new BookmarkStore(Path.of(System.getProperty(BookmarkStore.FILE_PROPERTY_NAME))).all().size());

			//in another file I2 is another person: the bookmark is not there
			SwingUtilities.invokeLater(() -> frame.fileController().openRecent(other));
			waitFor(() -> RecentNormal.same(other, frame.fileController().currentFile()));
			SwingUtilities.invokeAndWait(() -> {});
			items = openMenu(bookmarkMenu(frame));
			Assertions.assertNull(find(items, "Bruno's view"), "a bookmark of another file is shown");
			Assertions.assertNotNull(find(items, I18N.t("menu.item.no_bookmarks")));
		}
		finally{
			frame.fileController().recentFiles().remove(file);
			frame.fileController().recentFiles().remove(other);
			close(frame);
		}
	}

	@Test
	void aDocumentWithNoFileCannotHaveBookmarks() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The window needs a display");
		//a document with people in it, that is on no file (the user did not choose one)
		final FamilyLegacyFrame frame = frame(null, new io.github.mtrevisan.familylegacy.io.FLEFParser().parse(FLEF));
		try{
			loadRoot(frame, "I2");
			Assertions.assertNull(frame.fileController().currentFile());
			final Bookmark[] made = new Bookmark[1];
			SwingUtilities.invokeAndWait(() -> made[0] = frame.captureCurrentState("nowhere"));

			//a bookmark made here would belong to no file, and would never be listed: it is not made
			Assertions.assertNull(made[0]);
		}
		finally{
			close(frame);
		}
	}


	/** The same file, however its path is written. */
	private static final class RecentNormal{

		static boolean same(final File a, final File b){
			return (b != null && io.github.mtrevisan.familylegacy.ui.tools.files.RecentFilesManager.normalized(a)
				.equals(io.github.mtrevisan.familylegacy.ui.tools.files.RecentFilesManager.normalized(b)));
		}

	}

	@SuppressWarnings("unused")
	private static void unused(final JMenu menu){}

}
