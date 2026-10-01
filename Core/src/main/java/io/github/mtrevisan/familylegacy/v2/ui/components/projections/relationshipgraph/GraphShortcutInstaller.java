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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.relationshipgraph;

import io.github.mtrevisan.familylegacy.v2.ui.dialogs.help.ShortcutRegistry;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;


/**
 * Installs the four keyboard shortcuts used by the individual tree.
 * <p>
 * Each shortcut is bound on the given component with
 * {@link JComponent#WHEN_IN_FOCUSED_WINDOW}, so it fires whenever the
 * enclosing window has focus and no closer component claims the key.
 * The actions are supplied as {@link Runnable}s, so the installer does
 * not need a reference to the panel or to any of its collaborators.
 */
public final class GraphShortcutInstaller{

	private GraphShortcutInstaller(){}


	/**
	 * Installs the four tree shortcuts on the given component.
	 *
	 * @param component      the component that receives the bindings
	 * @param toggleLayout   called by {@code Ctrl+L}
	 * @param toggleStrip    called by {@code Ctrl+T}
	 */
	public static void installAll(final JComponent component, final Runnable toggleLayout, final Runnable toggleStrip){
		install(component, ShortcutRegistry.TREE_TOGGLE_LAYOUT.keyStroke(), "toggleTreeLayout", toggleLayout);
		install(component, ShortcutRegistry.TREE_TOGGLE_LIFESPAN_EVENTS_STRIP.keyStroke(), "toggleEventsStrip", toggleStrip);
	}


	private static void install(final JComponent component, final KeyStroke stroke, final String actionKey,
			final Runnable action){
		final InputMap inputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		final ActionMap actionMap = component.getActionMap();
		inputMap.put(stroke, actionKey);
		actionMap.put(actionKey, new AbstractAction(){
			@Override
			public void actionPerformed(final ActionEvent e){
				action.run();
			}
		});
	}

}
