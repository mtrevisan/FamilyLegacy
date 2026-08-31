package io.github.mtrevisan.familylegacy.v2.ui.bindings;

import io.github.mtrevisan.familylegacy.v2.ui.dialogs.ComponentUndoableEdit;
import org.apache.commons.lang3.StringUtils;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.MutableComboBoxModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoableEditSupport;
import java.awt.Component;
import java.util.List;
import java.util.Objects;


public class BoundComboBox<E> extends JComboBox<E> implements PathBound{

	private String path;

	private final boolean readOnly;

	private boolean isUpdatingItems;
	private Object lastSelectedValue;
	private final UndoableEditSupport undoSupport = new UndoableEditSupport();


	public BoundComboBox(final String path){
		super();

		clear();

		this.path = path;

		readOnly = false;

		clear();
		initUndoListener();
	}

	public BoundComboBox(final String path, final E[] items){
		super(items);

		this.path = path;

		readOnly = false;

		clear();
		initUndoListener();
	}

	public BoundComboBox(final String path, final E[] items, final E readOnlyItem){
		super(items);

		this.path = path;
		readOnly = true;

		if(readOnlyItem != null)
			super.setSelectedItem(readOnlyItem);
		initUndoListener();
	}


	private void initUndoListener(){
		lastSelectedValue = getSelectedItem();

		// Listener for non-editable selection changes
		addActionListener(e -> handleSelectionChange());

		// Listener for editable text editor changes
		setupEditorUndoListener();
	}

	private void setupEditorUndoListener(){
		final Component editorComp = getEditor().getEditorComponent();
		if(editorComp instanceof JTextComponent textComp){
			textComp.getDocument().addDocumentListener(new DocumentListener(){
				@Override
				public void insertUpdate(final DocumentEvent e){
					if(isEditable())
						handleSelectionChange();
				}

				@Override
				public void removeUpdate(final DocumentEvent e){
					if(isEditable())
						handleSelectionChange();
				}

				@Override
				public void changedUpdate(final DocumentEvent e){
					if(isEditable())
						handleSelectionChange();
				}
			});
		}
	}

	private void handleSelectionChange(){
		if(isUpdatingItems || readOnly)
			return;

		final Object newValue = getSelectedItem();
		final Object previousValue = lastSelectedValue;
		if(!Objects.equals(previousValue, newValue)){
			lastSelectedValue = newValue;

			undoSupport.postEdit(new ComponentUndoableEdit<>(this, previousValue, newValue,
				this::setSelectedItemWithoutUndo));
		}
	}

	public UndoableEditSupport getUndoSupport(){
		return undoSupport;
	}

	public void setSelectedItemWithoutUndo(final Object item){
		final boolean prevUpdating = isUpdatingItems;
		isUpdatingItems = true;
		try{
			setSelectedItem(item);

			lastSelectedValue = getSelectedItem();
		}
		finally{
			isUpdatingItems = prevUpdating;
		}
	}

	@Override
	public String getPath(){
		return path;
	}

	@Override
	public void setPath(final String path){
		this.path = path;
	}

	@Override
	public String getText(){
		final Object selectedItem = getSelectedItem();
		return (selectedItem != null? selectedItem.toString(): null);
	}

	/**
	 * Selects the item whose string representation equals the given text.
	 * If no item matches and the combo is editable, sets the typed text value.
	 *
	 * @param value The display text to search for.
	 */
	@Override
	public void setText(final String value){
		if(readOnly)
			throw new IllegalStateException("Cannot set item on a read-only BoundComboBox");

		if(value == null){
			setSelectedItem(null);

			return;
		}

		for(int i = 0; i < getItemCount(); i ++){
			final E item = getItemAt(i);

			final String display = (item != null? item.toString(): StringUtils.EMPTY);
			if(display.equals(value)){
				setSelectedIndex(i);

				return;
			}
		}

		// no match: if editable, set the typed value
		if(isEditable())
			setSelectedItem(value);
	}

	@Override
	public void setSelectedItem(final Object item){
		if(readOnly)
			throw new IllegalStateException("Cannot set item on a read-only BoundComboBox");

		super.setSelectedItem(item);
	}

	/**
	 * Clears the current selection.
	 * In read-only mode, it only clears the selection without throwing an exception.
	 */
	@Override
	public void clear(){
		if(readOnly)
			return;

		final boolean prevUpdating = isUpdatingItems;
		isUpdatingItems = true;
		try{
			setText(null);
			setSelectedIndex(-1);

			lastSelectedValue = getSelectedItem();
		}
		finally{
			isUpdatingItems = prevUpdating;
		}
	}

	@Override
	public boolean isReadOnly(){
		return readOnly;
	}

	public boolean isValued(){
		final Object item = getSelectedItem();
		return ((isEditable() || getSelectedIndex() >= 0)
			&& (item instanceof String str? StringUtils.isNotEmpty(str): item != null));
	}


	/**
	 * Updates the combo box items while preserving the empty element (if present)
	 * and the current selection when possible.
	 *
	 * @param newItems	The new list of items.
	 */
	public void updateItems(final List<E> newItems){
		final boolean prevUpdating = isUpdatingItems;

		isUpdatingItems = true;
		try{
			// Check if the empty element was present
			final E emptyElement = isEmptyItemPresent();

			// Save the current selection
			@SuppressWarnings("unchecked")
			final E selectedItem = (E)getSelectedItem();
			final ComboBoxModel<E> rawModel = getModel();
			if(rawModel instanceof MutableComboBoxModel<E> model){
				// If model supports dynamic updates, clear and rebuild
				if(model instanceof DefaultComboBoxModel<E> defaultModel)
					defaultModel.removeAllElements();
				else
					while(model.getSize() > 0)
						model.removeElementAt(0);

				if(emptyElement != null)
					model.addElement(emptyElement);
				if(newItems != null)
					for(final E element : newItems)
						model.addElement(element);

				model.setSelectedItem(selectedItem != null
						&& (newItems != null && newItems.contains(selectedItem) || isEditable())
					? selectedItem
					: emptyElement);
			}

			lastSelectedValue = getSelectedItem();
		}
		finally{
			isUpdatingItems = prevUpdating;
		}
	}

	/**
	 * Returns the empty element if present in the current model.
	 *
	 * @return The empty element, or {@code null} if not found.
	 */
	private E isEmptyItemPresent(){
		final ComboBoxModel<E> model = getModel();
		for(int i = 0; i < model.getSize(); i ++){
			final E element = model.getElementAt(i);
			if(element != null && StringUtils.EMPTY.equals(element.toString()))
				return element;
		}
		return null;
	}


	@Override
	public String toString(){
		final StringBuilder sb = new StringBuilder();
		sb.append("value: ");
		final String text = getText();
		sb.append(text != null? (text.isEmpty()? "''": text): "<null>")
			.append(", path: ")
			.append(path);
		return sb.toString();
	}

}
