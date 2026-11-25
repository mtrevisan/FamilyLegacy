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
package io.github.mtrevisan.familylegacy.ui.bindings;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;


class ComboBoxSearchTest{

	private static String[] items(final int count){
		final String[] items = new String[count];
		for(int i = 0; i < count; i ++)
			items[i] = "item_" + i;
		items[3] = "alpha_beta";
		return items;
	}


	@Test
	void filtersByTypedText(){
		final BoundComboBox<String> combo = new BoundComboBox<>("x", items(ComboBoxSearch.THRESHOLD + 8));
		final SearchableComboBoxModel<String> model = (SearchableComboBoxModel<String>)combo.getModel();

		model.setSearchText("beta");

		Assertions.assertEquals(1, combo.getItemCount());
		Assertions.assertEquals("alpha_beta", combo.getItemAt(0));
		Assertions.assertEquals(ComboBoxSearch.THRESHOLD + 8, model.getTotalSize());

		model.clearSearch();

		Assertions.assertEquals(ComboBoxSearch.THRESHOLD + 8, combo.getItemCount());
	}

	@Test
	void selectingAnItemFilteredOutClearsTheSearch(){
		final BoundComboBox<String> combo = new BoundComboBox<>("x", items(ComboBoxSearch.THRESHOLD + 8));
		final SearchableComboBoxModel<String> model = (SearchableComboBoxModel<String>)combo.getModel();
		model.setSearchText("beta");

		combo.setText("item_7");

		Assertions.assertEquals("item_7", combo.getText());
		Assertions.assertFalse(model.isFiltered());
	}

	@Test
	void keepsTheValueWhenTheSearchIsCleared(){
		final BoundComboBox<String> combo = new BoundComboBox<>("x", items(ComboBoxSearch.THRESHOLD + 8));
		combo.setText("item_5");
		final SearchableComboBoxModel<String> model = (SearchableComboBoxModel<String>)combo.getModel();

		model.setSearchText("beta");
		model.clearSearch();

		Assertions.assertEquals("item_5", combo.getText());
	}

	@Test
	void typingFiltersAndShowsTheSearchOnTheFace(){
		final javax.swing.JComboBox<String> combo = ComboBoxSearch.searchable(new javax.swing.JComboBox<>(items(ComboBoxSearch.THRESHOLD + 8)));
		final SearchableComboBoxModel<String> model = (SearchableComboBoxModel<String>)combo.getModel();

		for(final char c : "beta".toCharArray())
			type(combo, c);

		Assertions.assertEquals("beta", model.getSearchText());
		Assertions.assertEquals(1, combo.getItemCount());
		final Component face = combo.getRenderer().getListCellRendererComponent(new JList<>(), "x", -1, false, false);
		Assertions.assertEquals("→ beta", ((JLabel)face).getText());

		type(combo, '');

		Assertions.assertEquals("bet", model.getSearchText());
	}

	@Test
	void anEditableComboIsFilteredByWhatIsTypedInItsEditorAndTheTextIsLeftAsTyped(){
		final BoundComboBox<String> combo = new BoundComboBox<>("x", items(ComboBoxSearch.THRESHOLD + 8));
		combo.setEditable(true);
		final SearchableComboBoxModel<String> model = (SearchableComboBoxModel<String>)combo.getModel();
		final javax.swing.text.JTextComponent editor = (javax.swing.text.JTextComponent)combo.getEditor().getEditorComponent();

		editor.setText("beta");
		release(editor, 'a');

		Assertions.assertEquals("beta", model.getSearchText());
		Assertions.assertEquals(1, combo.getItemCount());
		Assertions.assertEquals("beta", editor.getText());

		//a text that is not an item is accepted as it is, and the whole list is back
		editor.setText("something_else");
		release(editor, 'e');
		Assertions.assertEquals(0, combo.getItemCount());
		combo.setSelectedItem("something_else");
		Assertions.assertFalse(model.isFiltered());
		Assertions.assertEquals(ComboBoxSearch.THRESHOLD + 8, combo.getItemCount());
	}

	@Test
	void anEditableComboWithFewItemsIsNotFiltered(){
		final BoundComboBox<String> combo = new BoundComboBox<>("x", items(5));
		combo.setEditable(true);
		final javax.swing.text.JTextComponent editor = (javax.swing.text.JTextComponent)combo.getEditor().getEditorComponent();

		editor.setText("beta");
		release(editor, 'a');

		Assertions.assertEquals(5, combo.getItemCount());
	}

	private static void release(final Component component, final char c){
		final KeyEvent event = new KeyEvent(component, KeyEvent.KEY_RELEASED, 0L, 0, KeyEvent.VK_A, c);
		for(final KeyListener listener : component.getKeyListeners())
			listener.keyReleased(event);
	}

	private static void type(final JComboBox<String> combo, final char c){
		final KeyEvent event = new KeyEvent(combo, KeyEvent.KEY_TYPED, 0L, 0, KeyEvent.VK_UNDEFINED, c);
		for(final KeyListener listener : combo.getKeyListeners())
			listener.keyTyped(event);
	}

}
