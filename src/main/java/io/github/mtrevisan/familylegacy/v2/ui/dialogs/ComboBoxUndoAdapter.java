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
