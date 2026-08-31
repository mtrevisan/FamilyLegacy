package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;


/**
 * Controller to manage undo/redo history without triggering nested edit events.
 */
public class UndoController{

	private final UndoManager undoManager = new UndoManager();
	private boolean isExecuting = false;


	public boolean addEdit(final UndoableEdit edit){
		if(isExecuting || edit == null)
			return false;

		return undoManager.addEdit(edit);
	}

	public void undo() throws CannotUndoException{
		if(canUndo()){
			try{
				isExecuting = true;

				undoManager.undo();
			}
			finally{
				isExecuting = false;
			}
		}
	}

	public void redo() throws CannotRedoException{
		if(canRedo()){
			try{
				isExecuting = true;

				undoManager.redo();
			}
			finally{
				isExecuting = false;
			}
		}
	}

	public boolean canUndo(){
		return undoManager.canUndo();
	}

	public boolean canRedo(){
		return undoManager.canRedo();
	}

	public void discardAllEdits(){
		undoManager.discardAllEdits();
	}

}
