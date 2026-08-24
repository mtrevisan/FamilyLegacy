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
package io.github.mtrevisan.familylegacy.v2.ui.bindings;

import org.apache.commons.lang3.StringUtils;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import java.util.List;


public class BoundComboBox<E> extends JComboBox<E> implements PathBound{

	private String path;

	private final boolean readOnly;


	public BoundComboBox(final String path){
		super();

		clear();

		this.path = path;

		readOnly = false;
	}

	public BoundComboBox(final String path, final E[] items){
		super(items);

		clear();

		this.path = path;

		readOnly = false;
	}

	public BoundComboBox(final String path, final E[] items, final E readOnlyItem){
		super(items);

		clear();

		this.path = path;

		setSelectedItem(readOnlyItem);

		readOnly = true;
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
	 * If no item matches, the selection is left unchanged.
	 *
	 * @param value	The display text to search for (case‑sensitive).
	 */
	@Override
	public void setText(final String value){
		if(readOnly)
			throw new IllegalStateException("Cannot set item on a read-only BoundFilteredComboBox");

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
		// In read-only mode, we just deselect (or keep the current item) but we shouldn't throw an exception
		if(!readOnly)
			setText(null);

		setSelectedIndex(-1);
	}

	@Override
	public boolean isReadOnly(){
		return readOnly;
	}

	public boolean isValued(){
		final Object item = getSelectedItem();
		return ((isEditable() || getSelectedIndex() >= 0) && (item instanceof String str? StringUtils.isNotEmpty(str): item != null));
	}


	/**
	 * Updates the combo box items while preserving the empty element (if present)
	 * and the current selection when possible.
	 *
	 * @param newItems	The new list of items.
	 */
	public void updateItems(final List<E> newItems){
		// Check if the empty element was present
		final E emptyElement = isEmptyItemPresent();

		// Save the current selection
		@SuppressWarnings("unchecked")
		final E selectedItem = (E)getSelectedItem();

		// Clear and repopulate the model
		final DefaultComboBoxModel<E> model = (DefaultComboBoxModel<E>)getModel();
		model.removeAllElements();
		if(emptyElement != null)
			model.addElement(emptyElement);
		for(final E element : newItems)
			model.addElement(element);

		// Restore the selection if it is still valid
		model.setSelectedItem(selectedItem != null && (newItems.contains(selectedItem) || isEditable())
			? selectedItem
			: emptyElement);
	}

	/**
	 * Returns the empty element if present in the current model.
	 * The empty element is defined as the first element whose string representation
	 * is equal to {@link StringUtils#EMPTY}.
	 *
	 * @return	The empty element, or {@code null} if not found.
	 */
	private E isEmptyItemPresent(){
		final DefaultComboBoxModel<E> model = (DefaultComboBoxModel<E>)getModel();
		for(int i = 0; i < model.getSize(); i ++){
			final E emptyElement = model.getElementAt(i);
			if(StringUtils.EMPTY.equals(emptyElement))
				return emptyElement;
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
