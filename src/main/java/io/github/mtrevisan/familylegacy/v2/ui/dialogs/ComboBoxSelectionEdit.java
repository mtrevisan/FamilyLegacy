package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

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
