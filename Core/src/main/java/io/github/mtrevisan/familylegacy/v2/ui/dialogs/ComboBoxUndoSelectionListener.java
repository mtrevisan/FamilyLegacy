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


class ComboBoxUndoSelectionListener implements ActionListener{

	private final JComboBox<?> comboBox;
	private final UndoController undoController;
	private Object lastValidValue;
	private boolean isExecuting;


	public ComboBoxUndoSelectionListener(final JComboBox<?> comboBox, final UndoController undoController){
		this.comboBox = comboBox;
		this.undoController = undoController;
		this.lastValidValue = comboBox.getSelectedItem();
	}


	@Override
	public void actionPerformed(final ActionEvent e){
		if(isExecuting){
			return;
		}

		final Object currentValue = comboBox.getSelectedItem();

		if(currentValue == null){
			return;
		}

		if(lastValidValue != null && !lastValidValue.equals(currentValue)){
			final Object oldVal = lastValidValue;
			final Object newVal = currentValue;

			undoController.addEdit(new DirectComboEdit(this, oldVal, newVal));
			this.lastValidValue = currentValue;
		}
		else if(lastValidValue == null){
			this.lastValidValue = currentValue;
		}
	}

	public void applyUndoRedo(final Object targetValue){
		if(targetValue == null){
			return;
		}
		this.isExecuting = true;
		try{
			this.lastValidValue = targetValue;
			this.comboBox.setSelectedItem(targetValue);
		}
		finally{
			this.isExecuting = false;
		}
	}

}
