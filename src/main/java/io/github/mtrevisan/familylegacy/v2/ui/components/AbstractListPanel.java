package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import java.awt.Dialog;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;


/**
 * Abstract panel for managing a list of items with add/edit/remove operations.
 * <p>
 * This panel provides a common UI pattern:
 * <ul>
 *   <li>A {@link JList} with a {@link DefaultListModel}</li>
 *   <li>Context menu (right-click) with Add, Edit, Remove options</li>
 *   <li>Double-click to edit</li>
 *   <li>INSERT key to add, DELETE key to remove</li>
 * </ul>
 *
 * @param <T> the type of items managed by this panel
 */
public abstract class AbstractListPanel<T> extends JPanel{

	@Serial
	private static final long serialVersionUID = -2135553287905371181L;


	protected final Dialog parentDialog;
	private final String title;

	protected final FLEFModel model;

	protected final DefaultListModel<String> listModel = new DefaultListModel<>();
	protected final JList<String> list = new JList<>(listModel);
	protected final List<T> items = new ArrayList<>();


	/**
	 * Constructs an AbstractListPanel.
	 *
	 * @param parentDialog the parent dialog (for showing modal dialogs)
	 * @param title        the title for the TitledBorder, or {@code null} for no border
	 * @param model        the FLEF model
	 */
	protected AbstractListPanel(final Dialog parentDialog, final String title, final FLEFModel model){
		this.parentDialog = parentDialog;
		this.title = title;

		this.model = model;

		initComponents();
	}

	/**
	 * Constructs an AbstractListPanel without a border.
	 */
	protected AbstractListPanel(final FLEFModel model, final Dialog parentDialog){
		this(parentDialog, null, model);
	}


	void initComponents(){
		setLayout(new MigLayout("fillx,wrap 1", "[grow]"));
		if(title != null)
			setBorder(new TitledBorder(title));

		list.setVisibleRowCount(4);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		GUIHelper.installBehavior(list,
			() -> list.getSelectedIndex() >= 0,
			this::editItem,
			this::addItem,
			this::removeItem,
			builder -> {
				builder.item("Add...", this::addItem);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editItem);
				builder.selectionSensitiveItem("Remove", this::removeItem);
			});

		add(GUIHelper.createScrollPane(list), "growx");
	}

	/**
	 * Adds a new item. Called by the "Add" action.
	 * Delegates to {@link #showAddDialog()} and adds the result.
	 */
	public final void addItem(){
		final T newItem = showAddDialog();
		if(newItem != null){
			items.add(newItem);
			listModel.addElement(getDisplay(newItem));
		}
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

		final int confirm = JOptionPane.showConfirmDialog(parentDialog,
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
	 * @param item the item
	 * @return the display string
	 */
	protected abstract String getDisplay(T item);

	/**
	 * Shows a dialog to create a new item.
	 *
	 * @return the new item, or {@code null} if canceled
	 */
	protected abstract T showAddDialog();

	/**
	 * Shows a dialog to edit an existing item.
	 *
	 * @param existing the item to edit
	 * @return the updated item, or {@code null} if canceled
	 */
	protected abstract T showEditDialog(T existing);

	/**
	 * Validates an item. Default implementation returns {@code true}.
	 * Override to add custom validation.
	 *
	 * @param item the item to validate
	 * @return {@code true} if valid, {@code false} otherwise
	 */
	protected boolean validateItem(T item){
		return true;
	}


	/**
	 * Clears all items from the list.
	 */
	public void clear(){
		items.clear();
		listModel.clear();
	}

	/**
	 * Returns the number of items in the list.
	 *
	 * @return the item count
	 */
	public int getItemCount(){
		return items.size();
	}

	/**
	 * Returns whether the list is empty.
	 *
	 * @return {@code true} if empty, {@code false} otherwise
	 */
	public boolean isEmpty(){
		return items.isEmpty();
	}

	/**
	 * Returns the list of items.
	 *
	 * @return the items
	 */
	public List<T> getItems(){
		return new ArrayList<>(items);
	}

	/**
	 * Replaces all items with the given list.
	 *
	 * @param newItems the new items
	 */
	public void setItems(final List<T> newItems){
		clear();

		if(newItems != null)
			for(final T item : newItems){
				items.add(item);
				listModel.addElement(getDisplay(item));
			}
	}

	/**
	 * Adds a single item directly (without showing a dialog).
	 *
	 * @param item the item to add
	 */
	protected void addItemDirectly(final T item){
		if(item != null){
			items.add(item);
			listModel.addElement(getDisplay(item));
		}
	}

}
