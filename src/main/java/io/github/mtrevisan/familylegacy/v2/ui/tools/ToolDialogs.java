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
package io.github.mtrevisan.familylegacy.v2.ui.tools;

import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import java.awt.event.ActionEvent;
import java.io.Serial;


/**
 * Small helpers shared by the tool dialogs.
 * <p>
 * The class exists so that the escape-to-close behaviour is defined in
 * one place and applied uniformly to every dialog in the {@code tools}
 * packages. Without it, each dialog would carry its own copy of the
 * binding, and a change to the behaviour would have to be replicated
 * everywhere.
 */
public final class ToolDialogs{

	private static final String ACTION_CLOSE = "closeDialog";


	private ToolDialogs(){
	}


	/**
	 * Installs an escape-to-close binding on the given dialog.
	 * <p>
	 * The binding is registered on the dialog's root pane with
	 * {@link JComponent#WHEN_IN_FOCUSED_WINDOW}, so it fires regardless
	 * of which child component has the focus. When the user presses
	 * escape, the dialog is disposed exactly as if the Cancel button had
	 * been clicked: any unsaved change is discarded, and the code that
	 * opened the dialog resumes as if the user had cancelled.
	 * <p>
	 * The call is idempotent: calling it twice on the same dialog
	 * replaces the binding with an equivalent one, without side effects.
	 *
	 * @param dialog the dialog to install the binding on; must not be
	 *               {@code null}
	 */
	public static void installEscapeToClose(final JDialog dialog){
		final InputMap inputMap = dialog.getRootPane()
			.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		final ActionMap actionMap = dialog.getRootPane()
			.getActionMap();
		inputMap.put(GUIHelper.ESCAPE_STROKE, ACTION_CLOSE);
		actionMap.put(ACTION_CLOSE, new AbstractAction(){
			@Serial
			private static final long serialVersionUID = 8512390128374019283L;

			@Override
			public void actionPerformed(final ActionEvent e){
				dialog.dispose();
			}
		});
	}

}
