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

import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.MutableComboBoxModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoableEditSupport;
import java.awt.Component;
import java.util.List;
import java.util.Objects;


public class BoundComboBox<E> extends JComboBox<E> implements PathBound{

	private static final String NONE = "none";

	private static final String NON_BREAKING_SPACE = "\u00A0";


	private String path;

	/**
	 * Optional prefix for the i18n keys of the combo's values. When set,
	 * the renderer displays the localized label resolved from
	 * {@code <prefix>.<code>} instead of the prettified code. The value
	 * returned by {@link #getSelectedItem()} and {@link #getText()} is
	 * always the raw code, so persistence and undo are unaffected.
	 */
	private String i18nPrefix;

	private boolean readOnly;

	private boolean isUpdatingItems;
	private Object lastSelectedValue;
	private final UndoableEditSupport undoSupport = new UndoableEditSupport();


	public BoundComboBox(final String path){
		super();

		init(path, null);
	}

	public BoundComboBox(final String path, final E[] items){
		super(items);

		init(path, null);
	}

	public BoundComboBox(final String path, final E[] items, final E readOnlyItem){
		super(items);

		init(path, readOnlyItem);
	}

	private void init(final String path, final E readOnlyItem){
		this.path = path;

		readOnly = (readOnlyItem != null);
		if(readOnly)
			super.setSelectedItem(readOnlyItem);

		if(!readOnly)
			clear();

		installRenderer();

		initUndoListener();
	}


	/**
	 * Configures the i18n prefix used by the renderer. The full key for a
	 * value {@code code} is {@code <prefix>.<code>}; the key for the empty
	 * value (if the combo has one) is {@code <prefix>.none}.
	 *
	 * <p>Passing {@code null} restores the default behaviour: the renderer
	 * prettifies the code by replacing underscores with spaces and
	 * capitalizing each word.</p>
	 *
	 * @param i18nPrefix the key prefix, e.g. {@code "enum.proof_status"};
	 *                   may be {@code null}
	 */
	public void setI18NPrefix(final String i18nPrefix){
		this.i18nPrefix = (StringUtils.isBlank(i18nPrefix)? null: i18nPrefix);

		installRenderer();
	}

	/**
	 * Installs the renderer that displays the localized label for each
	 * value. Called on construction and whenever the i18n prefix changes.
	 */
	private void installRenderer(){
		setRenderer(new DefaultListCellRenderer(){
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus){
				final Object displayValue = manageEmptyValue(value);
				super.getListCellRendererComponent(list, displayValue, index, isSelected, cellHasFocus);

				if(value instanceof String str)
					setText(localize(str));
				return this;
			}
		});
	}

	private static Object manageEmptyValue(final Object value){
		return (value == null || (value instanceof String str && str.trim().isEmpty())
			? NON_BREAKING_SPACE
			: value);
	}

	/**
	 * Returns the label to display for the given code. When no i18n prefix
	 * is configured, falls back to the prettified code. When a prefix is
	 * configured but the translation is missing, the prettified code is
	 * used instead of a raw token.
	 */
	private String localize(final String code){
		if(i18nPrefix == null)
			return prettify(code);

		if(code.isEmpty())
			return I18N.t(FLEFRecordHelper.composePath(i18nPrefix, NONE), "\u2014");

		return I18N.t(FLEFRecordHelper.composePath(i18nPrefix, code), prettify(code));
	}

	/**
	 * Fallback label for a code with no translation: underscores are
	 * replaced by spaces and each word is capitalized.
	 */
	private static String prettify(final String code){
		return WordUtils.capitalizeFully(code.replace('_', ' '));
	}


	/* ======================================================================
	 *                          Undo
	 * ====================================================================== */

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
	 * When an i18n prefix is configured, also matches the item whose
	 * localized label equals the given text. If no item matches and the
	 * combo is editable, sets the typed text value.
	 *
	 * @param value The value to search for, either the raw code or the localized label.
	 */
	@Override
	public void setText(final String value){
		if(readOnly)
			throw new IllegalStateException("Cannot set item on a read-only BoundComboBox");

		if(value == null){
			setSelectedItem(null);

			return;
		}

		// 1. Match by code (the item's toString)
		for(int i = 0, count = getItemCount(); i < count; i ++){
			final E item = getItemAt(i);
			if(item != null && value.equals(item.toString())){
				setSelectedIndex(i);

				return;
			}
		}

		// 2. Match by localized label (only when an i18n prefix is set)
		if(i18nPrefix != null)
			for(int i = 0, count = getItemCount(); i < count; i ++){
				final E item = getItemAt(i);
				if(item instanceof String str && value.equals(localize(str))){
					setSelectedIndex(i);

					return;
				}
			}

		// 3. No match: if editable, set the typed value
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
		for(int i = 0, size = model.getSize(); i < size; i ++){
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
		sb.append(text != null? (text.isEmpty()? "''": text): "<null>");
		if(path != null)
			sb.append(", path: ")
				.append(path);
		return sb.toString();
	}

}
