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

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.DefaultListModel;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
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


	protected final Dialog parent;
	private final String title;

	protected final FLEFModel model;

	protected final DefaultListModel<String> listModel = new DefaultListModel<>();
	protected final JList<String> list = GUIHelper.createList(listModel);
	protected final List<T> items = new ArrayList<>();

	protected boolean isReference;


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

		list.setVisibleRowCount(4);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		add(GUIHelper.createScrollPane(list), "growx");

		setupReorderingShortcuts();
	}

	private void setupReorderingShortcuts(){
		final InputMap inputMap = list.getInputMap(JComponent.WHEN_FOCUSED);
		final ActionMap actionMap = list.getActionMap();

		// Keybindings: CTRL + UP / CTRL + DOWN
		inputMap.put(GUIHelper.CTRL_UP_STROKE, "moveUp");
		inputMap.put(GUIHelper.CTRL_DOWN_STROKE, "moveDown");

		actionMap.put("moveUp", new AbstractAction(){
			@Serial
			private static final long serialVersionUID = 1557398991645410075L;

			@Override
			public void actionPerformed(final ActionEvent e){
				moveSelectedItemUp();
			}
		});

		actionMap.put("moveDown", new AbstractAction(){
			@Serial
			private static final long serialVersionUID = -8473405355379714143L;

			@Override
			public void actionPerformed(final ActionEvent e){
				moveSelectedItemDown();
			}
		});
	}

	/**
	 * Moves the currently selected item up by one position.
	 */
	public final void moveSelectedItemUp(){
		final int idx = list.getSelectedIndex();
		if(idx > 0)
			swapItems(idx, idx - 1);
	}

	/**
	 * Moves the currently selected item down by one position.
	 */
	public final void moveSelectedItemDown(){
		final int idx = list.getSelectedIndex();
		if(idx >= 0 && idx < items.size() - 1)
			swapItems(idx, idx + 1);
	}

	private void swapItems(final int index1, final int index2){
		// Swap in underlying items list
		final T tempRecord = items.get(index1);
		items.set(index1, items.get(index2));
		items.set(index2, tempRecord);

		// Swap in GUI model
		final String tempDisplay = listModel.get(index1);
		listModel.set(index1, listModel.get(index2));
		listModel.set(index2, tempDisplay);

		// Keep the moved item selected and visible
		list.setSelectedIndex(index2);
		list.ensureIndexIsVisible(index2);
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

		final T current = items.get(idx);
		final T updated = showEditDialog(current);
		if(updated != null){
			// find the new index (if changed)
			final int newIdx = items.indexOf(updated);
			if(newIdx >= 0){
				items.set(newIdx, updated);
				listModel.set(newIdx, getDisplayText(updated));
			}
			else{
				// add as last item if not found
				items.add(updated);
				listModel.addElement(getDisplayText(updated));
			}
		}
		else{
			// The item has been removed; we remove it from the list
			items.remove(current);
			listModel.removeElement(getDisplayText(current));

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

		//TODO instead of "Remove this item?", show displayText of record
		final int confirm = JOptionPane.showConfirmDialog(parent,
			"Remove this item?", "Confirm",
			JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			items.remove(idx);
			listModel.remove(idx);
		}
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
	 * Validates an item. Default implementation returns {@code true}.
	 * Override to add custom validation.
	 *
	 * @param item	The item to validate.
	 * @return	Whether it is valid.
	 */
	protected boolean validateItem(final T item){
		return true;
	}


	/**
	 * Clears all items from the list.
	 */
	public final void clear(){
		items.clear();
		listModel.clear();
	}

	/**
	 * Returns the number of items in the list.
	 *
	 * @return	The item count.
	 */
	public final int getItemCount(){
		return items.size();
	}

	/**
	 * Returns whether the list is empty.
	 *
	 * @return	Whether it is empty.
	 */
	public final boolean isEmpty(){
		return items.isEmpty();
	}

	/**
	 * Returns the list of items.
	 *
	 * @return	The items.
	 */
	public final List<T> getItems(){
		return items;
	}

	/**
	 * Replaces all items with the given list.
	 *
	 * @param newItems	The new items.
	 */
	public final void setItems(final List<T> newItems){
		clear();

		if(newItems != null)
			for(final T item : newItems)
				if(item != null)
					addElement(item);
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
		if(!items.contains(newItem)){
			items.add(newItem);
			listModel.addElement(getDisplayText(newItem));
		}
	}


	/**
	 * Saves list elements into a target FLEFRecord path when {@code T} is {@code FLEFRecord}.
	 */
	public final void save(final FLEFRecord record, final String path){
		if(items.isEmpty())
			return;

		@SuppressWarnings("unchecked")
		final List<FLEFRecord> recordItems = (List<FLEFRecord>)items;
		if(StringUtils.isEmpty(path)){
			record.addChildren(recordItems);

			return;
		}

		final int lastDotIndex = path.lastIndexOf('.');
		final String parentPath = (lastDotIndex >= 0? path.substring(0, lastDotIndex): null);
		final String lastChildTag = (lastDotIndex >= 0? path.substring(lastDotIndex + 1): path);

		final FLEFRecord parent = FLEFRecordHelper.getOrCreateTargetNode(record, parentPath);
		parent.addChildrenWithTag(lastChildTag, recordItems);
	}

}
