package io.github.mtrevisan.familylegacy.v2.ui.bindings;

import org.apache.commons.lang3.StringUtils;

import java.util.List;


/**
 * Filtered combo box supporting property binding via {@link PathBound}.
 *
 * @param <E> the element type
 */
public class BoundFilteredComboBox<E> extends FilteredComboBox<E> implements PathBound{

	private String path;

	private final boolean readOnly;


	public BoundFilteredComboBox(final String path){
		super();

		this.path = path;

		this.readOnly = false;

		clear();
	}

	public BoundFilteredComboBox(final String path, final List<E> items){
		super(items);

		this.path = path;

		this.readOnly = false;

		clear();
	}

	public BoundFilteredComboBox(final String path, final List<E> items, final E readOnlyItem){
		super(items);

		this.path = path;

		this.readOnly = true;
		if(readOnlyItem != null)
			super.setSelectedItem(readOnlyItem);
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
	 * If no item matches and the combo box is editable, sets the typed text value.
	 *
	 * @param value the display text to search for
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

		// no match: change selection if combo box is editable
		if(isEditable)
			setSelectedItem(value);
	}

	@Override
	public void setSelectedItem(final Object item){
		if(readOnly)
			throw new IllegalStateException("Cannot set item on a read-only BoundFilteredComboBox");

		super.setSelectedItem(item);
	}

	/**
	 * Clears the current selection if the component is not read-only.
	 */
	@Override
	public void clear(){
		if(readOnly)
			return;

		setSelectedIndex(-1);
	}

	@Override
	public boolean isReadOnly(){
		return readOnly;
	}

	public boolean isSelected(){
		final Object item = getSelectedItem();
		return ((isEditable() || getSelectedIndex() >= 0)
			&& (item instanceof String str? StringUtils.isNotEmpty(str): item != null));
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
