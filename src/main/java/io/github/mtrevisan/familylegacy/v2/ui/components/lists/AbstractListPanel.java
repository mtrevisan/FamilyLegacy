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
package io.github.mtrevisan.familylegacy.v2.ui.components.lists;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import java.awt.Component;
import java.awt.Dialog;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;


/**
 * Abstract base panel managing lists of items of type {@code T}.
 * <p>
 * This panel provides a standardized UI pattern for list management:
 * <ul>
 *   <li>A {@link JList} backed by a {@link DefaultListModel}</li>
 *   <li>Keyboard shortcuts for reordering items ({@code CTRL + UP} / {@code CTRL + DOWN})</li>
 *   <li>Hooks for subclass management via {@link #showCreateNewDialog()}, {@link #showAddDialog()}, and {@link #showEditDialog(Object)}</li>
 * </ul>
 * Standard actions (such as context menus, double-click to edit, or {@code INSERT}/{@code DELETE} keybindings)
 * are typically attached in subclass implementations (e.g., via {@code GUIHelper.installBehavior}).
 *
 * @param <T>	The type of elements managed in this list.
 */
public abstract class AbstractListPanel<T> extends JPanel{

	@Serial
	private static final long serialVersionUID = -2135553287905371181L;


	private class ItemCellRenderer extends DefaultListCellRenderer{
		@Override
		public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
				final boolean isSelected, final boolean cellHasFocus){
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			if(value != null){
				@SuppressWarnings("unchecked")
				final T item = (T)value;
				setText(getDisplayText(item));
			}
			else
				setText("--");
			return this;
		}

	}


	protected final Dialog parent;
	private final String title;

	protected final FLEFModel model;

	protected JList<T> list;
	protected final DefaultListModel<T> listModel = new DefaultListModel<>();


	/**
	 * Constructs an {@code AbstractListPanel} with a title.
	 *
	 * @param parent	The parent dialog.
	 * @param title	The border title, or {@code null} for no border.
	 * @param model	The FLEF model.
	 */
	protected AbstractListPanel(final Dialog parent, final String title, final FLEFModel model){
		this.parent = parent;
		this.title = title;

		this.model = model;
	}

	/**
	 * Constructs an AbstractListPanel without a border.
	 *
	 * @param parent	The parent dialog.
	 * @param model	The FLEF model.
	 */
	protected AbstractListPanel(final Dialog parent, final FLEFModel model){
		this(parent, null, model);
	}


	protected void initComponents(){
		setLayout(new MigLayout("fillx,wrap 1", "[grow]"));
		if(title != null)
			setBorder(new TitledBorder(title));

		list = GUIHelper.createList(listModel);
		list.setVisibleRowCount(getListVisibleRowCount());
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		list.setCellRenderer(new ItemCellRenderer());

		add(GUIHelper.createScrollPane(list), "growx");

		GUIHelper.setupReorderingShortcuts(list, listModel);
		GUIHelper.setupDragAndDrop(list, listModel);
	}

	protected int getListVisibleRowCount(){
		return 4;
	}


	/**
	 * Adds a new item. Called by the "Add" action.
	 * Delegates to {@link #showCreateNewDialog()} and adds the result.
	 */
	public final void createNewItem(){
		final T newItem = showCreateNewDialog();
		if(newItem != null)
			addElement(newItem);
	}

	/**
	 * Adds a new item from a list. Called by the "Add" action.
	 * Delegates to {@link #showAddDialog()} and adds the result.
	 */
	public final void addItem(){
		final T newItem = showAddDialog();
		if(newItem != null)
			addElement(newItem);
	}

	/**
	 * Adds a single item directly (without showing a dialog).
	 *
	 * @param item	The item to add.
	 */
	public final void addItemDirectly(final T item){
		if(item != null)
			addElement(item);
	}

	/**
	 * Edits the currently selected item. Called by the "Edit" action.
	 * Delegates to {@link #showEditDialog(Object)} and updates the result.
	 */
	public final void editItem(){
		final int idx = list.getSelectedIndex();
		if(idx == -1)
			return;

		final T current = listModel.get(idx);
		final T updated = showEditDialog(current);
		if(updated != null){
			// find the new index (if changed)
			final int newIdx = listModel.indexOf(updated);
			if(newIdx >= 0)
				listModel.set(newIdx, updated);
			else
				// add as last item if not found
				listModel.addElement(updated);
		}
		else{
			// The item has been removed; we remove it from the list
			listModel.removeElement(current);

			list.clearSelection();
		}
	}

	/**
	 * Removes the currently selected item after confirmation.
	 * Called by the "Remove" action.
	 */
	public final void removeItem(){
		final int idx = list.getSelectedIndex();
		if(idx == -1)
			return;

		final int confirm = JOptionPane.showConfirmDialog(parent,
			"Are you sure you want to remove this item?"
				+ StringUtils.LF + listModel.get(idx),
			"Confirm Removal",
			JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION)
			listModel.remove(idx);
	}


	/**
	 * Returns the display string for an item.
	 *
	 * @param item	The item.
	 * @return	The display string.
	 */
	protected abstract String getDisplayText(T item);

	/**
	 * Shows a dialog to create a new item.
	 *
	 * @return	The new item, or {@code null} if canceled.
	 */
	protected abstract T showCreateNewDialog();

	/**
	 * Shows a dialog to add an existing item (e.g., from a selection list).
	 *
	 * @return	The item to add, or {@code null} if canceled.
	 */
	protected abstract T showAddDialog();

	/**
	 * Shows a dialog to edit an existing item.
	 *
	 * @param record	The item to edit.
	 * @return	The updated item, or {@code null} if canceled.
	 */
	protected abstract T showEditDialog(T record);



	/**
	 * Clears all items from the list.
	 */
	public final void clear(){
		listModel.clear();
	}

	public JList<T> getList(){
		return list;
	}

	public T getSelectedItem(){
		final int idx = list.getSelectedIndex();
		return (idx != -1? listModel.get(idx): null);

	}

	/**
	 * Returns the number of items in the list.
	 *
	 * @return	The item count.
	 */
	public final int getItemCount(){
		return listModel.size();
	}

	/**
	 * Returns whether the list is empty.
	 *
	 * @return	Whether it is empty.
	 */
	public final boolean isEmpty(){
		return listModel.isEmpty();
	}

	/**
	 * Returns the list of items.
	 *
	 * @return	The items.
	 */
	public final List<T> getItems(){
		final int size = listModel.getSize();
		final List<T> items = new ArrayList<>();
		for(int i = 0; i < size; i ++)
			items.add(listModel.elementAt(i));
		return items;
	}

	/**
	 * Replaces all items with the given list.
	 *
	 * @param newItems	The new items.
	 */
	public final void setItems(final List<T> newItems){
		clear();

		if(newItems != null && !newItems.isEmpty())
			listModel.addAll(newItems);
	}

	/**
	 * Checks if the list contains any items.
	 *
	 * @return	Whether there is at least one item.
	 */
	public boolean hasData(){
		return !isEmpty();
	}

	private void addElement(final T newItem){
		if(!listModel.contains(newItem))
			listModel.addElement(newItem);
	}


	/**
	 * Saves list elements into a target FLEFRecord path when {@code T} is {@code FLEFRecord}.
	 */
	public final void save(final FLEFRecord record, final String path){
		if(listModel.isEmpty())
			return;

		final List<T> items = getItems();
		if(StringUtils.isEmpty(path)){
			for(final T item : items)
				record.addChild((FLEFRecord)item);

			return;
		}

		final int lastDotIndex = path.lastIndexOf('.');
		final String parentPath = (lastDotIndex >= 0? path.substring(0, lastDotIndex): null);
		final String lastChildTag = (lastDotIndex >= 0? path.substring(lastDotIndex + 1): path);

		final FLEFRecord parent = FLEFRecordHelper.getOrCreateTargetNode(record, parentPath);
		for(final T item : items)
			parent.addChildWithTag(lastChildTag, (FLEFRecord)item);
	}

}
