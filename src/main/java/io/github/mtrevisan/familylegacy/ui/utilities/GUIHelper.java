/**
 * Copyright (c) 2020-2022 Mauro Trevisan
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
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.Serial;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.function.Consumer;


public final class GUIHelper{

	private static final UndoManager UNDO_MANAGER = new UndoManager();
	private static final String ACTION_MAP_KEY_UNDO = "undo";
	private static final String ACTION_MAP_KEY_REDO = "redo";

	public static final KeyStroke ESCAPE_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
	public static final KeyStroke INSERT_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0);
	public static final KeyStroke DELETE_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);


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
		stack.add(component);
		while(!stack.isEmpty()){
			final Component comp = stack.pop();
			if(comp instanceof Container container)
				stack.addAll(Arrays.asList(container.getComponents()));

			comp.setEnabled(enabled);
		}
	}


	public static String readTextTrimmed(final JTextField field){
		final String text = field.getText();
		return (text != null? text.trim(): null);
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

		final InputMap inputMap = component.getInputMap(JComponent.WHEN_FOCUSED);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), ACTION_MAP_KEY_UNDO);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), ACTION_MAP_KEY_REDO);

		final ActionMap actionMap = component.getActionMap();
		actionMap.put(ACTION_MAP_KEY_UNDO, new UndoAction());
		actionMap.put(ACTION_MAP_KEY_REDO, new RedoAction());
	}

	public static void addUndoCapability(final JComboBox<?> comboBox){
		if(!comboBox.isEditable())
			throw new IllegalArgumentException("JComboBox must be editable to add undo capability");

		final JTextField textField = (JTextField)comboBox.getEditor().getEditorComponent();
		addUndoCapability(textField);
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
			catch(final CannotUndoException cue){
				cue.printStackTrace();
			}
		}
	}

	public static void addBorder(final JButton button, final boolean dataPresent, final Color borderColor){
		if(dataPresent)
			addBorder(button, borderColor);
		else
			setDefaultBorder(button);
	}

	public static void setDefaultBorder(final JButton button){
		final Border border = UIManager.getBorder("Button.border");
		button.setBorder(border);
	}

	public static void addBorder(final JButton button, final Color borderColor){
		final Insets insets = button.getInsets();
		final Border outsideBorder = BorderFactory.createLineBorder(borderColor);
		final Border insideBorder = BorderFactory.createEmptyBorder(insets.top - 1, insets.left - 1,
			insets.bottom - 1, insets.right - 1);
		final Border border = BorderFactory.createCompoundBorder(outsideBorder, insideBorder);
		button.setBorder(border);
	}

	public static void setBackgroundColor(final JTextComponent component, final Color backgroundColor){
		final Document doc = component.getDocument();
		doc.addDocumentListener(new DocumentListener(){
			@Override
			public void insertUpdate(final DocumentEvent de){
				updateBackground();
			}

			@Override
			public void removeUpdate(final DocumentEvent de){
				updateBackground();
			}

			@Override
			public void changedUpdate(final DocumentEvent de){
				updateBackground();
			}

			private void updateBackground(){
				if(component.getText().trim().isEmpty())
					component.setBackground(backgroundColor);
				else
					component.setBackground(Color.WHITE);
			}
		});
	}

}
