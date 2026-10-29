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

import javax.swing.JComponent;
import java.awt.Cursor;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.HierarchyEvent;
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
		final KeyboardFocusManager currentKeyboardFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		currentKeyboardFocusManager.addKeyEventDispatcher(e -> {
			if(e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_SHIFT)
				shiftHeld = true;
			else if(e.getID() == KeyEvent.KEY_RELEASED && e.getKeyCode() == KeyEvent.VK_SHIFT)
				shiftHeld = false;

			return false;
		});
	}


	protected final JComponent component;

	protected boolean mouseInside;

	private final KeyEventDispatcher shiftDispatcher = this::onShiftEvent;


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

		// Reset cursor when component is hidden; the global Shift listener lives as long as the component is displayable
		component.addHierarchyListener(e -> {
			if((e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0)
				updateShiftDispatcher();

			if(!component.isShowing())
				component.setCursor(DEFAULT_CURSOR);
		});
		updateShiftDispatcher();
	}

	/**
	 * The Shift listener (updates cursor if mouse is inside) is global: it is registered only while the component is
	 * displayable, otherwise it would keep the component (and its window) reachable for ever.
	 */
	private void updateShiftDispatcher(){
		final KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		manager.removeKeyEventDispatcher(shiftDispatcher);
		if(component.isDisplayable())
			manager.addKeyEventDispatcher(shiftDispatcher);
	}

	private boolean onShiftEvent(final KeyEvent e){
		if(e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_SHIFT)
			updateCursor();
		else if(e.getID() == KeyEvent.KEY_RELEASED && e.getKeyCode() == KeyEvent.VK_SHIFT)
			component.setCursor(DEFAULT_CURSOR);

		return false;
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
