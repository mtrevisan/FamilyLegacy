package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;


class DirectComboEdit extends AbstractUndoableEdit{

	private final ComboBoxUndoSelectionListener listener;
	private final Object oldValue;
	private final Object newValue;


	public DirectComboEdit(final ComboBoxUndoSelectionListener listener, final Object oldValue, final Object newValue){
		this.listener = listener;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}


	@Override
	public void undo() throws CannotUndoException{
		super.undo();

		listener.applyUndoRedo(oldValue);
	}

	@Override
	public void redo() throws CannotRedoException{
		super.redo();

		listener.applyUndoRedo(newValue);
	}

}
