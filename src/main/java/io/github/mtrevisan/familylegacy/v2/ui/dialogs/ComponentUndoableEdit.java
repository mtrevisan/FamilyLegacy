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
