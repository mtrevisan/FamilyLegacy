/**
 * Copyright (c) 2020 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.ui.utilities;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.Component;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.Deque;
import java.util.LinkedList;
import java.util.function.Consumer;


public final class GUIHelper{

	private static final UndoManager UNDO_MANAGER = new UndoManager();
	private static final String ACTION_MAP_KEY_UNDO = "undo";
	private static final String ACTION_MAP_KEY_REDO = "redo";


	private GUIHelper(){}

	public static void executeOnEventDispatchThread(final Runnable runnable){
		if(SwingUtilities.isEventDispatchThread())
			runnable.run();
		else
			SwingUtilities.invokeLater(runnable);
	}


	public static void setEnabled(final JLabel label, final boolean enabled){
		label.setEnabled(enabled);

		final Component component = label.getLabelFor();
		if(component != null)
			component.setEnabled(enabled);
	}

	public static void setEnabled(final JComponent component, final boolean enabled){
		final Deque<Component> stack = new LinkedList<>();
		for(final Component comp : component.getComponents())
			stack.add(comp);
		while(!stack.isEmpty()){
			final Component comp = stack.pop();
			if(comp instanceof Container container)
				for(final Component subComp : container.getComponents())
					stack.add(subComp);

			comp.setEnabled(enabled);
		}
		component.setEnabled(enabled);
	}

	public static void bindLabelTextChangeUndo(final JLabel label, final JTextComponent field, final Consumer<DocumentEvent> onTextChange){
		if(label != null)
			label.setLabelFor(field);

		addUndoCapability(field);

		if(onTextChange != null)
			field.getDocument().addDocumentListener(new DocumentListener(){
				@Override
				public void changedUpdate(final DocumentEvent evt){
					onTextChange.accept(evt);
				}

				@Override
				public void removeUpdate(final DocumentEvent evt){
					onTextChange.accept(evt);
				}

				@Override
				public void insertUpdate(final DocumentEvent evt){
					onTextChange.accept(evt);
				}
			});
	}

	public static void addUndoCapability(final JTextComponent component){
		final Document doc = component.getDocument();
		doc.addUndoableEditListener(event -> UNDO_MANAGER.addEdit(event.getEdit()));
		final InputMap textInputMap = component.getInputMap(JComponent.WHEN_FOCUSED);
		textInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), ACTION_MAP_KEY_UNDO);
		textInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), ACTION_MAP_KEY_REDO);
		final ActionMap textActionMap = component.getActionMap();
		textActionMap.put(ACTION_MAP_KEY_UNDO, new UndoAction());
		textActionMap.put(ACTION_MAP_KEY_REDO, new RedoAction());
	}


	private static class UndoAction extends AbstractAction{
		@Serial
		private static final long serialVersionUID = -3974682914632160277L;

		@Override
		public void actionPerformed(final ActionEvent event){
			try{
				if(UNDO_MANAGER.canUndo())
					UNDO_MANAGER.undo();
			}
			catch(final CannotUndoException e){
				e.printStackTrace();
			}
		}


		@SuppressWarnings("unused")
		@Serial
		private void writeObject(final ObjectOutputStream os) throws NotSerializableException{
			throw new NotSerializableException(getClass().getName());
		}

		@SuppressWarnings("unused")
		@Serial
		private void readObject(final ObjectInputStream is) throws NotSerializableException{
			throw new NotSerializableException(getClass().getName());
		}
	}

	private static class RedoAction extends AbstractAction{
		@Serial
		private static final long serialVersionUID = -4415532769601693910L;

		@Override
		public void actionPerformed(final ActionEvent event){
			try{
				if(UNDO_MANAGER.canRedo())
					UNDO_MANAGER.redo();
			}
			catch(final CannotUndoException e){
				e.printStackTrace();
			}
		}


		@SuppressWarnings("unused")
		@Serial
		private void writeObject(final ObjectOutputStream os) throws NotSerializableException{
			throw new NotSerializableException(getClass().getName());
		}

		@SuppressWarnings("unused")
		@Serial
		private void readObject(final ObjectInputStream is) throws NotSerializableException{
			throw new NotSerializableException(getClass().getName());
		}
	}

}
