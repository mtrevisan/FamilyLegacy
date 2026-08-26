package io.github.mtrevisan.familylegacy.v2.ui.helpers;

import javax.swing.JComponent;
import java.awt.Cursor;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


/**
 * Base class for adding dual‑click behavior to Swing components:
 * <ul>
 *   <li>Double‑click → primary action</li>
 *   <li>Shift+Double‑click → secondary action</li>
 * </ul>
 * <p>
 * Subclasses must implement {@link #isOverValidTarget()} to detect
 * whether the mouse is over a valid "clickable" area of the component.
 */
public abstract class AbstractDualActionEnhancer{

	private static final Cursor HAND_CURSOR = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
	private static final Cursor DEFAULT_CURSOR = Cursor.getDefaultCursor();


	// Static state for Shift key (updated by the dispatcher)
	private static volatile boolean shiftHeld;
	static{
		// Register a global dispatcher to update the shift state
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
			if(e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_SHIFT)
				shiftHeld = true;
			else if(e.getID() == KeyEvent.KEY_RELEASED && e.getKeyCode() == KeyEvent.VK_SHIFT)
				shiftHeld = false;

			return false;
		});
	}


	protected final JComponent component;

	protected boolean mouseInside;


	/**
	 * Constructor.
	 *
	 * @param component   the component to enhance
	 */
	protected AbstractDualActionEnhancer(final JComponent component){
		this.component = component;

		installCommonBehavior();
		installComponentSpecificBehavior();
	}

	/**
	 * Installs the common behavior (cursor, tooltip, key dispatcher).
	 */
	private void installCommonBehavior(){
		// Mouse listener to track presence inside the component
		component.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseEntered(final MouseEvent e){
				mouseInside = true;

				updateCursor();
			}

			@Override
			public void mouseExited(final MouseEvent e){
				mouseInside = false;

				component.setCursor(DEFAULT_CURSOR);
			}
		});

		// Global Shift listener (updates cursor if mouse is inside)
		final KeyboardFocusManager currentKeyboardFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		currentKeyboardFocusManager.addKeyEventDispatcher(e -> {
			if(e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_SHIFT)
				updateCursor();
			else if(e.getID() == KeyEvent.KEY_RELEASED && e.getKeyCode() == KeyEvent.VK_SHIFT)
				component.setCursor(DEFAULT_CURSOR);

			return false;
		});

		// Reset cursor when component is hidden
		component.addHierarchyListener(e -> {
			if(!component.isShowing())
				component.setCursor(DEFAULT_CURSOR);
		});
	}

	/**
	 * Updates the cursor based on the Shift state and whether the mouse is
	 * inside the component AND over a valid target (delegated to subclass).
	 */
	protected void updateCursor(){
		if(!component.isShowing()){
			component.setCursor(DEFAULT_CURSOR);

			return;
		}

		final boolean shift = shiftHeld;
		final boolean overTarget = (mouseInside && isOverValidTarget());
		component.setCursor(shift && overTarget? HAND_CURSOR: DEFAULT_CURSOR);
	}

	/**
	 * Checks whether the mouse is currently over a valid target
	 * (e.g., a list item or the text field itself).
	 * This is called from the mouse motion listener (or on demand).
	 *
	 * @return true if the mouse is over a clickable area
	 */
	protected abstract boolean isOverValidTarget();

	/**
	 * Installs the component‑specific behavior:
	 * - Mouse motion listener to update {@link #mouseInside} and cursor.
	 * - Mouse listener for double‑click.
	 * - Optional context menu.
	 */
	protected abstract void installComponentSpecificBehavior();

	/**
	 * Hook for subclasses to safely get the mouse position relative to the component.
	 */
	protected final Point getMousePosition(){
		return component.getMousePosition();
	}

}
