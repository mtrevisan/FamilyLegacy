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
package io.github.mtrevisan.familylegacy.ui.helpers;

import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import java.awt.Component;


/**
 * Message dialogs with no button: every dialog of the application closes with Esc (or with the close button of the window), so an Ok that
 * only dismisses a message would add nothing. A dialog where the user decides something is still made with the buttons of the choices
 * ({@link JOptionPane#showConfirmDialog}, {@link JOptionPane#showOptionDialog}).
 */
public final class MessageDialogs{

	/** The longest line of a text message, in characters: a longer one is wrapped, so the dialog does not grow as wide as the screen. */
	private static final int MAX_CHARACTERS_PER_LINE = 80;


	private MessageDialogs(){}


	/**
	 * Tells something to the user, in a modal dialog that closes with Esc.
	 *
	 * @param parent	The component the dialog is over ({@code null} for the screen).
	 * @param message	What to say.
	 */
	public static void show(final Component parent, final Object message){
		show(parent, message, UIManager.getString("OptionPane.messageDialogTitle"), JOptionPane.INFORMATION_MESSAGE, null);
	}

	/**
	 * @param parent	The component the dialog is over ({@code null} for the screen).
	 * @param message	What to say.
	 * @param title	The title of the dialog.
	 * @param messageType	One of the message types of {@link JOptionPane}: it decides the icon.
	 */
	public static void show(final Component parent, final Object message, final String title, final int messageType){
		show(parent, message, title, messageType, null);
	}

	/**
	 * @param parent	The component the dialog is over ({@code null} for the screen).
	 * @param message	What to say.
	 * @param title	The title of the dialog.
	 * @param messageType	One of the message types of {@link JOptionPane}: it decides the icon.
	 * @param icon	The icon, {@code null} for the one of the message type.
	 */
	public static void show(final Component parent, final Object message, final String title, final int messageType, final Icon icon){
		//no options: the pane has no button
		final JOptionPane pane = new WrappingOptionPane(message, messageType, JOptionPane.DEFAULT_OPTION, icon, new Object[0]);
		final JDialog dialog = pane.createDialog(parent, title);
		GUIHelper.installEscapeToClose(dialog);
		dialog.setVisible(true);
		dialog.dispose();
	}

	/**
	 * Asks the user to decide, like {@link JOptionPane#showConfirmDialog(Component, Object, String, int)} but wrapping a long text message.
	 *
	 * @param parent	The component the dialog is over ({@code null} for the screen).
	 * @param message	What to ask.
	 * @param title	The title of the dialog.
	 * @param optionType	One of the option types of {@link JOptionPane}: it decides the buttons.
	 * @return	The button chosen, or {@link JOptionPane#CLOSED_OPTION}.
	 */
	public static int confirm(final Component parent, final Object message, final String title, final int optionType){
		return confirm(parent, message, title, optionType, JOptionPane.QUESTION_MESSAGE);
	}

	/**
	 * @param parent	The component the dialog is over ({@code null} for the screen).
	 * @param message	What to ask.
	 * @param title	The title of the dialog.
	 * @param optionType	One of the option types of {@link JOptionPane}: it decides the buttons.
	 * @param messageType	One of the message types of {@link JOptionPane}: it decides the icon.
	 * @return	The button chosen, or {@link JOptionPane#CLOSED_OPTION}.
	 */
	public static int confirm(final Component parent, final Object message, final String title, final int optionType,
			final int messageType){
		final JOptionPane pane = new WrappingOptionPane(message, messageType, optionType, null, null);
		pane.setInitialValue(null);
		final JDialog dialog = pane.createDialog(parent, title);
		pane.selectInitialValue();
		dialog.setVisible(true);
		dialog.dispose();
		return (pane.getValue() instanceof Integer value? value: JOptionPane.CLOSED_OPTION);
	}

	/**
	 * Asks the user to choose among some options, like
	 * {@link JOptionPane#showOptionDialog(Component, Object, String, int, int, Icon, Object[], Object)} but wrapping a long text message.
	 *
	 * @param parent	The component the dialog is over ({@code null} for the screen).
	 * @param message	What to ask.
	 * @param title	The title of the dialog.
	 * @param optionType	One of the option types of {@link JOptionPane}.
	 * @param messageType	One of the message types of {@link JOptionPane}: it decides the icon.
	 * @param icon	The icon, {@code null} for the one of the message type.
	 * @param options	The choices.
	 * @param initialValue	The choice that is selected at first.
	 * @return	The index of the choice, or {@link JOptionPane#CLOSED_OPTION}.
	 */
	public static int option(final Component parent, final Object message, final String title, final int optionType, final int messageType,
			final Icon icon, final Object[] options, final Object initialValue){
		final JOptionPane pane = new WrappingOptionPane(message, messageType, optionType, icon, options);
		pane.setInitialValue(initialValue);
		final JDialog dialog = pane.createDialog(parent, title);
		pane.selectInitialValue();
		dialog.setVisible(true);
		dialog.dispose();
		final Object selected = pane.getValue();
		if(options == null)
			return (selected instanceof Integer value? value: JOptionPane.CLOSED_OPTION);
		for(int i = 0; i < options.length; i ++)
			if(options[i].equals(selected))
				return i;
		return JOptionPane.CLOSED_OPTION;
	}


	/** An option pane that wraps a long text message. */
	private static final class WrappingOptionPane extends JOptionPane{

		private WrappingOptionPane(final Object message, final int messageType, final int optionType, final Icon icon,
				final Object[] options){
			super(message, messageType, optionType, icon, options);
		}

		@Override
		public int getMaxCharactersPerLineCount(){
			return MAX_CHARACTERS_PER_LINE;
		}

	}

}
