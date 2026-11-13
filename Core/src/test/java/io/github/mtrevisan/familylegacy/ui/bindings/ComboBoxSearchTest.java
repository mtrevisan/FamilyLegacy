package io.github.mtrevisan.familylegacy.ui.bindings;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


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
		final java.awt.Component face = combo.getRenderer().getListCellRendererComponent(new javax.swing.JList<>(), "x", -1, false, false);
		Assertions.assertEquals("→ beta", ((javax.swing.JLabel)face).getText());

		type(combo, '');

		Assertions.assertEquals("bet", model.getSearchText());
	}

	private static void type(final javax.swing.JComboBox<String> combo, final char c){
		final java.awt.event.KeyEvent event = new java.awt.event.KeyEvent(combo, java.awt.event.KeyEvent.KEY_TYPED, 0L, 0,
			java.awt.event.KeyEvent.VK_UNDEFINED, c);
		for(final java.awt.event.KeyListener listener : combo.getKeyListeners())
			listener.keyTyped(event);
	}

}
