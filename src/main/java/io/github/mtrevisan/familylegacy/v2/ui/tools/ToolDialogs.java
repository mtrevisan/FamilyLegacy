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
