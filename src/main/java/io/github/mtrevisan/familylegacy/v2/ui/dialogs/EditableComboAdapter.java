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

import io.github.mtrevisan.familylegacy.v2.ui.helpers.Debouncer;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JComboBox;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;


class EditableComboAdapter extends FocusAdapter implements ActionListener, DocumentListener{

	private static final String DEBOUNCE_KEY = "combo_undo_debounce";

	/**
	 * [ms]
	 */
	private static final int DEBOUNCE_TIME = 400;

	private final JComboBox<?> comboBox;
	private final JTextComponent textComp;
	private final UndoController undoController;
	private final Debouncer<String> undoDebouncer;

	private Object baseValue;
	private boolean isExecuting;


	public EditableComboAdapter(final JComboBox<?> comboBox, final JTextComponent textComp,
			final UndoController undoController){
		this.comboBox = comboBox;
		this.textComp = textComp;
		this.undoController = undoController;
		baseValue = getCurrentValue();

		undoDebouncer = new Debouncer<>(key -> commitEdit(), DEBOUNCE_TIME);
	}


	private Object getCurrentValue(){
		final String text = textComp.getText();
		if(text != null && !text.isEmpty())
			return text;

		return comboBox.getSelectedItem();
	}

	@Override
	public void focusGained(final FocusEvent e){
		if(!isExecuting)
			baseValue = getCurrentValue();
	}

	@Override
	public void focusLost(final FocusEvent e){
		undoDebouncer.terminate(DEBOUNCE_KEY);

		commitEdit();
	}

	@Override
	public void actionPerformed(final ActionEvent e){
		if(!isExecuting){
			undoDebouncer.terminate(DEBOUNCE_KEY);

			commitEdit();
		}
	}

	@Override
	public void insertUpdate(final DocumentEvent e){
		onTextChange();
	}

	@Override
	public void removeUpdate(final DocumentEvent e){
		onTextChange();
	}

	@Override
	public void changedUpdate(final DocumentEvent e){
		onTextChange();
	}

	private void onTextChange(){
		if(!isExecuting)
			undoDebouncer.call(DEBOUNCE_KEY);
	}

	private void commitEdit(){
		if(isExecuting)
			return;

		final Object currentValue = getCurrentValue();

		if(baseValue != null && !baseValue.equals(currentValue)){
			final Object oldVal = baseValue;
			final Object newVal = currentValue;

			undoController.addEdit(new EditableComboEdit(this, oldVal, newVal));
			baseValue = currentValue;
		}
		else if(baseValue == null && currentValue != null)
			baseValue = currentValue;
	}

	public void applyValue(final Object value){
		isExecuting = true;
		try{
			undoDebouncer.terminate(DEBOUNCE_KEY);

			baseValue = value;

			final String textRepresentation = (value != null? value.toString(): StringUtils.EMPTY);

			comboBox.getEditor().setItem(value);
			comboBox.setSelectedItem(value);

			if(!textComp.getText().equals(textRepresentation))
				textComp.setText(textRepresentation);
		}
		finally{
			isExecuting = false;
		}
	}

}
