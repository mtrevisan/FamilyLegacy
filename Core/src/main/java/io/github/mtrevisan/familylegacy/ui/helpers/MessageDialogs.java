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
		final JOptionPane pane = new JOptionPane(message, messageType, JOptionPane.DEFAULT_OPTION, icon, new Object[0]);
		final JDialog dialog = pane.createDialog(parent, title);
		GUIHelper.installEscapeToClose(dialog);
		dialog.setVisible(true);
		dialog.dispose();
	}

}
