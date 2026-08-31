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
		this.baseValue = getCurrentValue();

		this.undoDebouncer = new Debouncer<>(key -> commitEdit(), DEBOUNCE_TIME);
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
			this.baseValue = getCurrentValue();
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
			this.baseValue = currentValue;
		}
		else if(baseValue == null && currentValue != null)
			this.baseValue = currentValue;
	}

	public void applyValue(final Object value){
		this.isExecuting = true;
		try{
			undoDebouncer.terminate(DEBOUNCE_KEY);

			this.baseValue = value;

			final String textRepresentation = (value != null? value.toString(): StringUtils.EMPTY);

			this.comboBox.getEditor().setItem(value);
			this.comboBox.setSelectedItem(value);

			if(!this.textComp.getText().equals(textRepresentation))
				this.textComp.setText(textRepresentation);
		}
		finally{
			this.isExecuting = false;
		}
	}

}
