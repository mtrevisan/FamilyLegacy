package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;


class EditableComboEdit extends AbstractUndoableEdit{

	private final EditableComboAdapter adapter;
	private final Object oldValue;
	private final Object newValue;


	public EditableComboEdit(final EditableComboAdapter adapter, final Object oldValue, final Object newValue){
		this.adapter = adapter;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}


	@Override
	public void undo() throws CannotUndoException{
		super.undo();

		adapter.applyValue(oldValue);
	}

	@Override
	public void redo() throws CannotRedoException{
		super.redo();

		adapter.applyValue(newValue);
	}

}
