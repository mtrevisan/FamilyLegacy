/**
 * Copyright (c) 2020-2022 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.ui.helpers;

import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
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
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.io.Serial;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.function.Supplier;


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

	public static void setEnabled(final Component component, final boolean enabled){
		final Deque<Component> stack = new LinkedList<>();
		stack.add(component);
		while(!stack.isEmpty()){
			final Component comp = stack.pop();
			if(comp instanceof Container container)
				stack.addAll(Arrays.asList(container.getComponents()));

			comp.setEnabled(enabled);
		}
	}


	public static String getTextTrimmed(final JTextComponent field){
		String text = field.getText();
		if(text != null)
			text = text.trim();
		return (text != null && !text.isEmpty()? text: null);
	}

	public static String getTextTrimmed(final JComboBox<String> comboBox){
		String text = (String)comboBox.getSelectedItem();
		if(text != null)
			text = text.trim();
		return (text != null && !text.isEmpty()? text: null);
	}

	public static void bindLabelTextChange(final JLabel label, final TextPreviewPane field, final Runnable onEdit){
		if(label != null)
			label.setLabelFor(field);

		if(onEdit != null)
			field.addDocumentListener(new DocumentListener(){
				@Override
				public void changedUpdate(final DocumentEvent evt){
					onEdit.run();
				}

				@Override
				public void removeUpdate(final DocumentEvent evt){
					onEdit.run();
				}

				@Override
				public void insertUpdate(final DocumentEvent evt){
					onEdit.run();
				}
			});
	}

	public static void bindLabelTextChangeUndo(final JLabel label, final JTextComponent field, final Runnable onEdit){
		if(label != null)
			label.setLabelFor(field);

		addUndoCapability(field);

		if(onEdit != null)
			field.getDocument().addDocumentListener(new DocumentListener(){
				@Override
				public void changedUpdate(final DocumentEvent evt){
					onEdit.run();
				}

				@Override
				public void removeUpdate(final DocumentEvent evt){
					onEdit.run();
				}

				@Override
				public void insertUpdate(final DocumentEvent evt){
					onEdit.run();
				}
			});
	}

	public static void bindLabelSelectionAutoCompleteChange(final JLabel label, final JComboBox<?> comboBox,
			final Consumer<ActionEvent> onSelection){
		if(label != null)
			label.setLabelFor(comboBox);

		AutoCompleteDecorator.decorate(comboBox);

		if(onSelection != null)
			comboBox.addActionListener(evt -> {
				if(comboBox.getSelectedItem() != null)
					onSelection.accept(evt);
			});
	}

	public static void bindLabelUndoSelectionAutoCompleteChange(final JLabel label, final JComboBox<?> comboBox, final Runnable onEdit){
		if(label != null)
			label.setLabelFor(comboBox);

		comboBox.setEditable(true);

		addUndoCapability(comboBox);

		AutoCompleteDecorator.decorate(comboBox);

		if(onEdit != null)
			comboBox.addItemListener(evt -> {
				if(evt.getStateChange() == ItemEvent.SELECTED)
					onEdit.run();
			});
	}

	public static void bindLabelSelectionChange(final JLabel label, final JComboBox<?> comboBox, final Consumer<ActionEvent> onSelection){
		if(label != null)
			label.setLabelFor(comboBox);

		//if not editable:
		if(onSelection != null)
			comboBox.addActionListener(evt -> {
				if(comboBox.getSelectedItem() != null)
					onSelection.accept(evt);
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

	public static void setEnabled(final Supplier<Boolean> funEnabled, final JTextComponent... components){
		for(final JTextComponent component : components){
			final Document doc = component.getDocument();
			doc.addDocumentListener(new DocumentListener(){
				@Override
				public void insertUpdate(final DocumentEvent de){
					updateEnabled(funEnabled, components);
				}

				@Override
				public void removeUpdate(final DocumentEvent de){
					updateEnabled(funEnabled, components);
				}

				@Override
				public void changedUpdate(final DocumentEvent de){
					updateEnabled(funEnabled, components);
				}
			});
		}

		updateEnabled(funEnabled, components);
	}

	public static void updateEnabled(final Supplier<Boolean> funEnabled, final JComponent... components){
		final boolean enabled = funEnabled.get();
		for(int i = 0, length = components.length; i < length; i ++)
			components[i].setEnabled(enabled);
	}

	public static void addValidDataListener(final ValidDataListenerInterface validDataInterface, final Color mandatoryBackgroundColor,
			final Color defaultBackgroundColor, final JTextComponent... components){
		for(int i = 0, length = components.length; i < length; i ++){
			JTextComponent component = components[i];
			final Document doc = component.getDocument();
			doc.addDocumentListener(new DocumentListener(){
				@Override
				public void insertUpdate(final DocumentEvent de){
					final boolean valid = checkValidData(validDataInterface, components);
					updateBackground(valid, mandatoryBackgroundColor, defaultBackgroundColor, components);
				}

				@Override
				public void removeUpdate(final DocumentEvent de){
					final boolean valid = checkValidData(validDataInterface, components);
					updateBackground(valid, mandatoryBackgroundColor, defaultBackgroundColor, components);
				}

				@Override
				public void changedUpdate(final DocumentEvent de){
					final boolean valid = checkValidData(validDataInterface, components);
					updateBackground(valid, mandatoryBackgroundColor, defaultBackgroundColor, components);
				}
			});
		}

		final boolean valid = checkValidData(validDataInterface, components);
		updateBackground(valid, mandatoryBackgroundColor, defaultBackgroundColor, components);
	}

	public static boolean checkValidData(final ValidDataListenerInterface validDataInterface, final JTextComponent... components){
		boolean valid = false;
		for(int i = 0, length = components.length; i < length; i ++)
			if(getTextTrimmed(components[i]) != null){
				valid = true;

				break;
			}
		validDataInterface.onValidationChange(valid);

		return valid;
	}

	public static void updateBackground(final boolean valid, final Color mandatoryBackgroundColor, final Color defaultBackgroundColor,
			final JTextComponent... components){
		final int length = components.length;
		if(valid){
			for(int j = 0; j < length; j ++)
				components[j].setBackground(defaultBackgroundColor);
		}
		else{
			for(int j = 0; j < length; j ++){
				final JTextComponent component = components[j];
				component.setBackground(component.isEnabled()? mandatoryBackgroundColor: defaultBackgroundColor);
			}
		}
	}


	public static void enableTabByTitle(final JTabbedPane tabbedPane, final String title, final boolean enable){
		final int index = tabbedPane.indexOfTab(title);
		if(index >= 0)
			tabbedPane.setEnabledAt(index, enable);
	}

}
