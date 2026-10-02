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
package io.github.mtrevisan.familylegacy.ui.dialogs;

import javax.swing.JComboBox;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;


class ComboBoxSelectionEdit<E> extends AbstractUndoableEdit{

	private final JComboBox<E> comboBox;
	private final Object oldValue;
	private final Object newValue;
	private final ComboBoxUndoAdapter<E> adapter;


	public ComboBoxSelectionEdit(final JComboBox<E> comboBox, final Object oldValue, final Object newValue,
			final ComboBoxUndoAdapter<E> adapter){
		this.comboBox = comboBox;
		this.oldValue = oldValue;
		this.newValue = newValue;
		this.adapter = adapter;
	}


	@Override
	public void undo() throws CannotUndoException{
		super.undo();

		adapter.updateLastValueWithoutEdit(oldValue);
		comboBox.setSelectedItem(oldValue);
	}

	@Override
	public void redo() throws CannotRedoException{
		super.redo();

		adapter.updateLastValueWithoutEdit(newValue);
		comboBox.setSelectedItem(newValue);
	}

}
