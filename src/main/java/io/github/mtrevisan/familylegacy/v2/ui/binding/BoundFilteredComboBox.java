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
package io.github.mtrevisan.familylegacy.v2.ui.binding;

import org.apache.commons.lang3.StringUtils;

import java.util.List;


public class BoundFilteredComboBox<E> extends FilteredComboBox<E> implements PathBound{

	private String path;

	private final boolean readOnly;


	public BoundFilteredComboBox(final String path, final List<E> items){
		super(items);

		clear();

		this.path = path;

		readOnly = false;
	}

	public BoundFilteredComboBox(final String path, final List<E> items, final E readOnlyItem){
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
		// no match: do not change selection
	}

	@Override
	public void setSelectedItem(final Object item){
		if(readOnly)
			throw new IllegalStateException("Cannot set item on a read-only BoundFilteredComboBox");

		super.setSelectedItem(item);
	}

	@Override
	public void clear(){
		setSelectedIndex(-1);
	}

	@Override
	public boolean isReadOnly(){
		return readOnly;
	}

	public boolean isSelected(){
		final Object item = getSelectedItem();
		return (getSelectedIndex() >= 0 && (item instanceof String str? StringUtils.isNotEmpty(str): item != null));
	}

}
