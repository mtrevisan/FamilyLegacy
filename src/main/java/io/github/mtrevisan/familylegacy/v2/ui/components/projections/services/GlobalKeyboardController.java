package io.github.mtrevisan.familylegacy.v2.ui.components.projections.services;

import io.github.mtrevisan.familylegacy.v2.ui.components.projections.ProjectionSwitcherPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.SpatialNavigation;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.help.ShortcutRegistry;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.Serial;


/**
 * Handles global key bindings (Navigation, Edit Selection, Spatial Arrow Navigation).
 */
public final class GlobalKeyboardController{

	private static final String ACTION_NAVIGATE_BACK = "navigateBack";
	private static final String ACTION_NAVIGATE_FORWARD = "navigateForward";
	private static final String ACTION_EDIT_SELECTION = "editSelection";


	public static void install(final JFrame frame, final ProjectionSwitcherPanel switcher){
		final InputMap inputMap = frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		final ActionMap actionMap = frame.getRootPane().getActionMap();

		// Navigation Back / Forward
		inputMap.put(ShortcutRegistry.NAV_BACK.keyStroke(), ACTION_NAVIGATE_BACK);
		actionMap.put(ACTION_NAVIGATE_BACK, new AbstractAction(){
			@Serial
			private static final long serialVersionUID = -2472917274670701129L;

			@Override
			public void actionPerformed(final ActionEvent e){
				if(switcher.canGoBack())
					switcher.navigateBack();
			}
		});

		inputMap.put(ShortcutRegistry.NAV_FORWARD.keyStroke(), ACTION_NAVIGATE_FORWARD);
		actionMap.put(ACTION_NAVIGATE_FORWARD, new AbstractAction(){
			@Serial
			private static final long serialVersionUID = -7736620353955995616L;

			@Override
			public void actionPerformed(final ActionEvent e){
				if(switcher.canGoForward())
					switcher.navigateForward();
			}
		});

		// Edit Selection (F2)
		inputMap.put(ShortcutRegistry.EDIT_SELECTION.keyStroke(), ACTION_EDIT_SELECTION);
		actionMap.put(ACTION_EDIT_SELECTION, new AbstractAction(){
			@Serial
			private static final long serialVersionUID = -6055994878369180523L;

			@Override
			public void actionPerformed(final ActionEvent e){
				switcher.editCurrentSelection();
			}
		});

		// Spatial Arrow Navigation
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
			if(e.getID() != KeyEvent.KEY_PRESSED || !frame.isFocused())
				return false;

			final int modifiers = e.getModifiersEx();
			if((modifiers & (InputEvent.CTRL_DOWN_MASK | InputEvent.META_DOWN_MASK | InputEvent.ALT_DOWN_MASK
					| InputEvent.SHIFT_DOWN_MASK)) != 0)
				return false;

			final Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
			if(focusOwner instanceof JTextComponent)
				return false;

			return switch(e.getKeyCode()){
				case KeyEvent.VK_UP -> {
					switcher.moveSelection(SpatialNavigation.Direction.UP);
					yield true;
				}
				case KeyEvent.VK_DOWN -> {
					switcher.moveSelection(SpatialNavigation.Direction.DOWN);
					yield true;
				}
				case KeyEvent.VK_LEFT -> {
					switcher.moveSelection(SpatialNavigation.Direction.LEFT);
					yield true;
				}
				case KeyEvent.VK_RIGHT -> {
					switcher.moveSelection(SpatialNavigation.Direction.RIGHT);
					yield true;
				}
				case KeyEvent.VK_ENTER -> {
					switcher.confirmSelection();
					yield true;
				}
				default -> false;
			};
		});
	}

}
