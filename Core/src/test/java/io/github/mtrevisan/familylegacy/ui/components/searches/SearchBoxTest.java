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
package io.github.mtrevisan.familylegacy.ui.components.searches;

import io.github.mtrevisan.familylegacy.search.SearchMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.EnumSet;
import java.util.Set;


class SearchBoxTest{

	private static final Set<SearchMode> PLAIN = EnumSet.noneOf(SearchMode.class);


	@Test
	void defaultOptionsAreOffAndFollowTheNotesFlag(){
		final SearchBox plain = new SearchBox(10, false);
		Assertions.assertEquals(2, plain.popupItemCount());
		Assertions.assertTrue(plain.modes().isEmpty());

		final SearchBox withNotes = new SearchBox(10, true);
		Assertions.assertEquals(4, withNotes.popupItemCount());
		Assertions.assertEquals(SearchMode.NOTES_AND_CITATIONS, withNotes.options().get(2).mode());
		Assertions.assertEquals(SearchMode.STORIES, withNotes.options().get(3).mode());
	}

	@Test
	void selectingAnOptionChangesTheModesAndTellsTheListeners(){
		final SearchBox box = new SearchBox(10, false);
		final int[] calls = {0};
		box.onChange(() -> calls[0] ++);

		box.setSelected(SearchMode.FUZZY, true);
		Assertions.assertEquals(EnumSet.of(SearchMode.FUZZY), box.modes());
		Assertions.assertTrue(box.isSelected(SearchMode.FUZZY));
		Assertions.assertEquals(1, calls[0]);

		//no change, no call
		box.setSelected(SearchMode.FUZZY, true);
		Assertions.assertEquals(1, calls[0]);

		box.setSelected(SearchMode.FUZZY, false);
		Assertions.assertTrue(box.modes().isEmpty());
		Assertions.assertEquals(2, calls[0]);
	}

	@Test
	void clickingAnItemOfThePopupSwitchesTheOptionAndTellsTheListenersEachTime() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
		final SearchBox box = new SearchBox(10, false);
		final int[] calls = {0};
		box.onChange(() -> calls[0] ++);
		final JFrame frame = new JFrame();
		try{
			SwingUtilities.invokeAndWait(() -> {
				frame.add(box);
				frame.pack();
				frame.setVisible(true);
				final java.lang.reflect.Method show;
				try{
					show = SearchBox.class.getDeclaredMethod("showOptions");
					show.setAccessible(true);
					show.invoke(box);
				}
				catch(final Exception e){
					throw new RuntimeException(e);
				}
			});
			final java.lang.reflect.Field popupField = SearchBox.class.getDeclaredField("popup");
			popupField.setAccessible(true);
			final JPopupMenu popup = (JPopupMenu)popupField.get(box);
			final JMenuItem fuzzy = (JMenuItem)popup.getComponent(1);

			for(int i = 0; i < 3; i ++){
				final boolean expected = (i % 2 == 0);
				SwingUtilities.invokeAndWait(() -> {
					final Point point = new Point(fuzzy.getWidth() / 2, fuzzy.getHeight() / 2);
					fuzzy.dispatchEvent(new MouseEvent(fuzzy, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), InputEvent.BUTTON1_DOWN_MASK,
						point.x, point.y, 1, false, MouseEvent.BUTTON1));
					fuzzy.dispatchEvent(new MouseEvent(fuzzy, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0,
						point.x, point.y, 1, false, MouseEvent.BUTTON1));
				});
				Assertions.assertEquals(expected, box.isSelected(SearchMode.FUZZY), "click " + i);
				Assertions.assertEquals(i + 1, calls[0], "click " + i);
			}
		}
		finally{
			SwingUtilities.invokeAndWait(frame::dispose);
		}
	}

	@Test
	void modesIsACopy(){
		final SearchBox box = new SearchBox(10, false);
		final Set<SearchMode> modes = box.modes();
		modes.add(SearchMode.WHOLE_WORD);
		Assertions.assertTrue(box.modes().isEmpty());
	}

	@Test
	void anOptionAddedLaterGrowsThePopup(){
		final SearchBox box = new SearchBox(10);
		Assertions.assertEquals(0, box.popupItemCount());

		box.addOption(SearchMode.NOTES_AND_CITATIONS, "Notes");
		Assertions.assertEquals(1, box.popupItemCount());
		box.setSelected(SearchMode.NOTES_AND_CITATIONS, true);
		Assertions.assertEquals(EnumSet.of(SearchMode.NOTES_AND_CITATIONS), box.modes());
	}

	@Test
	void anOptionIsRegisteredOnce(){
		final SearchBox box = new SearchBox(10, false);
		Assertions.assertThrows(IllegalArgumentException.class, () -> box.addOption(SearchMode.FUZZY, "Again"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> box.setSelected(SearchMode.NOTES_AND_CITATIONS, true));
	}

	@Test
	void blankQueryMatchesEverything(){
		Assertions.assertTrue(SearchBox.matches("", PLAIN, "anything"));
	}

	@Test
	void wordsOfTheQueryCanBeInDifferentFields(){
		Assertions.assertTrue(SearchBox.matches("anna padova", PLAIN, "Anna Rossi", null, "born in Padova"));
		Assertions.assertFalse(SearchBox.matches("venezia roma", PLAIN, "Anna Rossi", "Padova"));
	}

	@Test
	void wholeWordRefusesAPartOfAWord(){
		Assertions.assertTrue(SearchBox.matches("ross", PLAIN, "Anna Rossi"));
		Assertions.assertFalse(SearchBox.matches("ross", EnumSet.of(SearchMode.WHOLE_WORD), "Anna Rossi"));
	}

	@Test
	void fuzzyToleratesATypo(){
		Assertions.assertFalse(SearchBox.matches("rosi", PLAIN, "Anna Rossi"));
		Assertions.assertTrue(SearchBox.matches("rosi", EnumSet.of(SearchMode.FUZZY), "Anna Rossi"));
	}

}
