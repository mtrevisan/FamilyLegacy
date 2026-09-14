package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree;

import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.io.Serial;


/**
 * Installs the four keyboard shortcuts used by the individual tree.
 * <p>
 * Each shortcut is bound on the given component with
 * {@link JComponent#WHEN_IN_FOCUSED_WINDOW}, so it fires whenever the
 * enclosing window has focus and no closer component claims the key.
 * The actions are supplied as {@link Runnable}s, so the installer does
 * not need a reference to the panel or to any of its collaborators.
 */
public final class TreeShortcutInstaller{

	private TreeShortcutInstaller(){
	}


	/**
	 * Installs the four tree shortcuts on the given component.
	 *
	 * @param component      the component that receives the bindings
	 * @param toggleLayout   called by {@code Ctrl+L}
	 * @param openKinship    called by {@code Ctrl+K}
	 * @param showCollapse   called by {@code Ctrl+P}
	 * @param toggleStrip    called by {@code Ctrl+T}
	 */
	public static void installAll(final JComponent component,
		final Runnable toggleLayout,
		final Runnable openKinship,
		final Runnable showCollapse,
		final Runnable toggleStrip){
		install(component, GUIHelper.CTRL_L_STROKE, "toggleTreeLayout", toggleLayout);
		install(component, GUIHelper.CTRL_K_STROKE, "openKinshipDialog", openKinship);
		install(component, GUIHelper.CTRL_P_STROKE, "showPedigreeCollapse", showCollapse);
		install(component, GUIHelper.CTRL_T_STROKE, "toggleLifespansStrip", toggleStrip);
	}


	private static void install(final JComponent component, final KeyStroke stroke,
		final String actionKey, final Runnable action){
		final InputMap inputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		final ActionMap actionMap = component.getActionMap();
		inputMap.put(stroke, actionKey);
		actionMap.put(actionKey, new AbstractAction(){
			@Serial
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(final ActionEvent e){
				action.run();
			}
		});
	}

}
