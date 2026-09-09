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

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.Component;


public class ComponentUndoableEdit<T> extends AbstractUndoableEdit{

	private final Component component;
	private final T oldValue;
	private final T newValue;
	private final ValueConsumer<T> setter;


	@FunctionalInterface
	public interface ValueConsumer<T>{
		void accept(T value);
	}


	public ComponentUndoableEdit(final Component component, final T oldValue, final T newValue,
			final ValueConsumer<T> setter){
		this.component = component;
		this.oldValue = oldValue;
		this.newValue = newValue;
		this.setter = setter;
	}

	@Override
	public void undo() throws CannotUndoException{
		super.undo();

		setter.accept(oldValue);

		requestFocus();
	}

	@Override
	public void redo() throws CannotRedoException{
		super.redo();

		setter.accept(newValue);

		requestFocus();
	}

	private void requestFocus(){
		if(component != null && component.isShowing())
			component.requestFocusInWindow();
	}

}
