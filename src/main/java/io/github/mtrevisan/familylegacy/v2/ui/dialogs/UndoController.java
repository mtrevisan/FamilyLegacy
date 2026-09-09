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
