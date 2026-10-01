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
package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import javax.swing.JComboBox;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


class ComboBoxUndoAdapter<E> implements ActionListener{

	private final JComboBox<E> comboBox;
	private final UndoController undoController;
	private Object lastSelectedValue;


	public ComboBoxUndoAdapter(final JComboBox<E> comboBox, final UndoController undoController){
		this.comboBox = comboBox;
		this.undoController = undoController;
		this.lastSelectedValue = comboBox.getSelectedItem();
	}


	@Override
	public void actionPerformed(final ActionEvent e){
		final Object currentValue = comboBox.getSelectedItem();
		if(lastSelectedValue == null && currentValue != null
				|| lastSelectedValue != null && !lastSelectedValue.equals(currentValue)){
			final Object oldVal = lastSelectedValue;
			final Object newVal = currentValue;

			undoController.addEdit(new ComboBoxSelectionEdit<>(comboBox, oldVal, newVal, this));
			this.lastSelectedValue = currentValue;
		}
	}

	public void updateLastValueWithoutEdit(final Object value){
		this.lastSelectedValue = value;
	}

}
