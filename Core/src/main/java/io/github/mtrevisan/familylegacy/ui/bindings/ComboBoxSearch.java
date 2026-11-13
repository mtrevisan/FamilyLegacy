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

import javax.accessibility.Accessible;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.Function;


/**
 * Makes a (non editable) combo box searchable when it has many items: typing while the combo has the focus opens the
 * popup and shows only the items whose text matches what was typed (word by word, case insensitive); backspace removes the
 * last character, and the search is cleared when the popup closes.
 * <p>
 * Combo boxes with few items keep the standard behavior (typing a letter jumps to the item starting with it).
 * </p>
 */
public final class ComboBoxSearch{

	/** The combo boxes with more items than this are searchable. */
	public static final int THRESHOLD = 12;


	private ComboBoxSearch(){}


	/**
	 * Makes the combo box searchable, the items being searched as the renderer of the combo box writes them.
	 *
	 * @param combo	The combo box (not editable).
	 * @param <E>	The type of the elements.
	 * @return	The same combo box.
	 */
	public static <E> JComboBox<E> searchable(final JComboBox<E> combo){
		return searchable(combo, rendered(combo));
	}

	/**
	 * Makes the combo box searchable.
	 *
	 * @param combo	The combo box (not editable).
	 * @param label	The text of an item as the user sees it.
	 * @param <E>	The type of the elements.
	 * @return	The same combo box.
	 */
	public static <E> JComboBox<E> searchable(final JComboBox<E> combo, final Function<Object, String> label){
		install(combo, label);

		return combo;
	}

	/**
	 * Installs the search on the combo box.
	 *
	 * @param combo	The combo box (not editable).
	 * @param label	The text of an item as the user sees it.
	 * @param <E>	The type of the elements.
	 * @return	The model that now backs the combo box.
	 */
	public static <E> SearchableComboBoxModel<E> install(final JComboBox<E> combo, final Function<Object, String> label){
		final ComboBoxModel<E> old = combo.getModel();
		final SearchableComboBoxModel<E> model = new SearchableComboBoxModel<>();
		for(int i = 0, count = old.getSize(); i < count; i ++)
			model.addElement(old.getElementAt(i));
		model.setSelectedItem(old.getSelectedItem());
		model.setLabelProvider(label);
		combo.setModel(model);

		//the standard "jump to the item starting with the typed letter" is kept for the combos that are not searchable
		final JComboBox.KeySelectionManager standard = combo.getKeySelectionManager();
		combo.setKeySelectionManager((key, m) -> (model.getTotalSize() > THRESHOLD? -1: standard.selectionForKey(key, m)));

		showSearchOnFace(combo, model);

		final boolean[] restarting = {false};
		combo.addKeyListener(new KeyAdapter(){
			@Override
			public void keyPressed(final KeyEvent e){
				//the space is part of the search (instead of toggling the popup) once the search has started
				if(e.getKeyCode() == KeyEvent.VK_SPACE && searching(combo, model) && !model.getSearchText().isEmpty())
					e.consume();
			}

			@Override
			public void keyTyped(final KeyEvent e){
				if(!searching(combo, model) || e.isControlDown() || e.isAltDown() || e.isMetaDown())
					return;

				final String current = model.getSearchText();
				final char c = e.getKeyChar();
				final String next;
				if(c == KeyEvent.VK_BACK_SPACE){
					if(current.isEmpty())
						return;

					next = current.substring(0, current.length() - 1);
				}
				else if(Character.isISOControl(c) || (c == ' ' && current.isEmpty()))
					return;
				else
					next = current + c;

				e.consume();
				model.setSearchText(next);
				combo.repaint();
				if(!combo.isShowing())
					return;

				if(model.getSize() == 0)
					combo.setPopupVisible(false);
				else{
					restarting[0] = true;
					try{
						//the popup is reopened to fit the number of matching items
						combo.setPopupVisible(false);
						combo.setPopupVisible(true);
					}
					finally{
						restarting[0] = false;
					}
					highlightFirst(combo);
				}
			}
		});

		combo.addPopupMenuListener(new PopupMenuListener(){
			@Override
			public void popupMenuWillBecomeVisible(final PopupMenuEvent e){}

			@Override
			public void popupMenuWillBecomeInvisible(final PopupMenuEvent e){
				if(!restarting[0])
					//deferred: the selection of the clicked row is done after the popup closes
					SwingUtilities.invokeLater(() -> clear(combo, model));
			}

			@Override
			public void popupMenuCanceled(final PopupMenuEvent e){
				if(!restarting[0])
					SwingUtilities.invokeLater(() -> clear(combo, model));
			}
		});
		return model;
	}

	/** @return	The text of an item as the renderer of the combo box writes it (read when searching, so a later renderer is honored). */
	private static <E> Function<Object, String> rendered(final JComboBox<E> combo){
		final JList<E> list = new JList<>();
		return item -> {
			@SuppressWarnings("unchecked")
			final Component component = combo.getRenderer().getListCellRendererComponent(list, (E)item, 0, false, false);
			return (component instanceof JLabel label && label.getText() != null? label.getText(): String.valueOf(item));
		};
	}

	/**
	 * While searching, the face of the combo box shows what was typed (red when nothing matches), whatever the renderer.
	 * A renderer set later is wrapped too.
	 */
	private static <E> void showSearchOnFace(final JComboBox<E> combo, final SearchableComboBoxModel<E> model){
		final boolean[] wrapping = {false};
		final Runnable wrap = () -> {
			final ListCellRenderer<? super E> delegate = combo.getRenderer();
			if(delegate instanceof FaceRenderer)
				return;

			wrapping[0] = true;
			try{
				combo.setRenderer(new FaceRenderer<>(delegate, model));
			}
			finally{
				wrapping[0] = false;
			}
		};
		wrap.run();
		combo.addPropertyChangeListener("renderer", e -> {
			if(!wrapping[0])
				wrap.run();
		});
	}

	private record FaceRenderer<E>(ListCellRenderer<? super E> delegate, SearchableComboBoxModel<E> model) implements ListCellRenderer<E>{
		@Override
		public Component getListCellRendererComponent(final JList<? extends E> list, final E value, final int index,
				final boolean isSelected, final boolean cellHasFocus){
			final Component component = delegate.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if(index < 0 && !model.getSearchText().isEmpty() && component instanceof JLabel label){
				label.setText("→ " + model.getSearchText());
				if(model.getSize() == 0)
					label.setForeground(Color.RED);
			}
			return component;
		}
	}

	private static boolean searching(final JComboBox<?> combo, final SearchableComboBoxModel<?> model){
		return (combo.isEnabled() && !combo.isEditable() && model.getTotalSize() > THRESHOLD);
	}

	private static void clear(final JComboBox<?> combo, final SearchableComboBoxModel<?> model){
		if(model.isFiltered()){
			model.clearSearch();
			combo.repaint();
		}
	}

	private static void highlightFirst(final JComboBox<?> combo){
		final Accessible popup = combo.getAccessibleContext().getAccessibleChild(0);
		if(popup instanceof ComboPopup comboPopup){
			comboPopup.getList().setSelectedIndex(0);
			comboPopup.getList().ensureIndexIsVisible(0);
		}
	}

}
