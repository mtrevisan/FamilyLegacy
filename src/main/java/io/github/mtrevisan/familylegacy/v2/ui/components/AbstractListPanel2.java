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
package io.github.mtrevisan.familylegacy.v2.ui.components;

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


/* DONE */

/**
 * Abstract panel for managing a list of items with add/edit/remove operations.
 * <p>
 * This panel provides a common UI pattern:
 * <ul>
 *   <li>A {@link javax.swing.JList} with a {@link javax.swing.DefaultListModel}</li>
 *   <li>Context menu (right-click) with Add, Edit, Remove options</li>
 *   <li>Double-click to edit</li>
 *   <li>INSERT key to add, DELETE key to remove</li>
 * </ul>
 */
public abstract class AbstractListPanel2 extends JPanel{

	@Serial
	private static final long serialVersionUID = -2135553287905371181L;


	protected final Dialog parent;
	private final String title;

	protected final FLEFModel model;

	protected final DefaultListModel<String> listModel = new DefaultListModel<>();
	protected final JList<String> list = new JList<>(listModel);
	protected final List<FLEFRecord> items = new ArrayList<>();


	/**
	 * Constructs an AbstractListPanel.
	 *
	 * @param parent the parent dialog (for showing modal dialogs)
	 * @param title        the title for the TitledBorder, or {@code null} for no border
	 * @param model        the FLEF model
	 */
	protected AbstractListPanel2(final Dialog parent, final String title, final FLEFModel model){
		this.parent = parent;
		this.title = title;

		this.model = model;

		initComponents();
	}

	/**
	 * Constructs an AbstractListPanel without a border.
	 */
	protected AbstractListPanel2(final Dialog parent, final FLEFModel model){
		this(parent, null, model);
	}


	void initComponents(){
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
			@Override
			public void actionPerformed(final ActionEvent e){
				moveSelectedItemUp();
			}
		});

		actionMap.put("moveDown", new AbstractAction(){
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
		final FLEFRecord tempRecord = items.get(index1);
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
		final FLEFRecord newItem = showCreateNewDialog();
		if(newItem != null){
			addElement(newItem);

			GUIHelper.updatePlaceholder(list);
		}
	}

	/**
	 * Adds a new item from a list. Called by the "Add" action.
	 * Delegates to {@link #showAddDialog()} and adds the result.
	 */
	public final void addItem(){
		final FLEFRecord newItem = showAddDialog();
		if(newItem != null){
			addElement(newItem);

			GUIHelper.updatePlaceholder(list);
		}
	}

	/**
	 * Adds a single item directly (without showing a dialog).
	 *
	 * @param item the item to add
	 */
	protected final void addItemDirectly(final FLEFRecord item){
		if(item != null){
			addElement(item);

			GUIHelper.updatePlaceholder(list);
		}
	}

	/**
	 * Edits the currently selected item. Called by the "Edit" action.
	 * Delegates to {@link #showEditDialog(FLEFRecord)} and updates the result.
	 */
	public final void editItem(){
		final int idx = list.getSelectedIndex();
		if(idx == -1)
			return;

		final FLEFRecord current = items.get(idx);
		final FLEFRecord updated = showEditDialog(current);
		if(updated != null){
			items.set(idx, updated);
			listModel.set(idx, getDisplay(updated));
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
			"Remove this item?", "Confirm",
			JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			items.remove(idx);
			listModel.remove(idx);

			GUIHelper.updatePlaceholder(list);
		}
	}


	/**
	 * Returns the display string for an item.
	 *
	 * @param item the item
	 * @return the display string
	 */
	protected abstract String getDisplay(FLEFRecord item);

	/**
	 * Shows a dialog to create a new item.
	 *
	 * @return the new item, or {@code null} if canceled
	 */
	protected abstract FLEFRecord showCreateNewDialog();

	/**
	 * Shows a dialog to add an existing item (e.g., from a selection list).
	 *
	 * @return the item to add, or {@code null} if canceled
	 */
	protected abstract FLEFRecord showAddDialog();

	/**
	 * Shows a dialog to edit an existing item.
	 *
	 * @param existing the item to edit
	 * @return the updated item, or {@code null} if canceled
	 */
	protected abstract FLEFRecord showEditDialog(FLEFRecord existing);

	/**
	 * Validates an item. Default implementation returns {@code true}.
	 * Override to add custom validation.
	 *
	 * @param item the item to validate
	 * @return {@code true} if valid, {@code false} otherwise
	 */
	protected boolean validateItem(final FLEFRecord item){
		return true;
	}


	/**
	 * Clears all items from the list.
	 */
	public final void clear(){
		items.clear();
		listModel.clear();

		GUIHelper.updatePlaceholder(list);
	}

	/**
	 * Returns the number of items in the list.
	 *
	 * @return the item count
	 */
	public final int getItemCount(){
		return items.size();
	}

	/**
	 * Returns whether the list is empty.
	 *
	 * @return {@code true} if empty, {@code false} otherwise
	 */
	public final boolean isEmpty(){
		return items.isEmpty();
	}

	/**
	 * Returns the list of items.
	 *
	 * @return the items
	 */
	public final List<FLEFRecord> getItems(){
		return items;
	}

	/**
	 * Replaces all items with the given list.
	 *
	 * @param newItems the new items
	 */
	public final void setItems(final List<FLEFRecord> newItems){
		clear();

		if(newItems != null)
			for(final FLEFRecord item : newItems)
				if(item != null)
					addElement(item);

		GUIHelper.updatePlaceholder(list);
	}

	private final void addElement(final FLEFRecord newItem){
		items.add(newItem);
		listModel.addElement(getDisplay(newItem));
	}

	public final void save(final FLEFRecord record, final String path){
		FLEFRecordHelper.removeChildren(record, path);

		if(StringUtils.isEmpty(path)){
			record.addChildren(items);

			return;
		}

		final int lastDotIndex = path.lastIndexOf('.');
		final String parentPath = (lastDotIndex >= 0? path.substring(0, lastDotIndex): null);
		final String lastChildTag = (lastDotIndex >= 0? path.substring(lastDotIndex + 1): path);

		final FLEFRecord parent = FLEFRecordHelper.getOrCreateTargetNode(record, parentPath);
		parent.addChildrenWithTag(lastChildTag, items);
	}

}
