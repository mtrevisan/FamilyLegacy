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
package io.github.mtrevisan.familylegacy.v2.ui.dialogs.help;

import javax.swing.KeyStroke;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


/**
 * Single source of truth for all application keyboard shortcuts,
 * supporting multiple KeyStrokes per action (e.g., Undo / Redo OS variations).
 */
public final class ShortcutRegistry{

	public record ShortcutDefinition(
		String category,
		String action,
		KeyStroke defaultStroke,
		KeyStroke macStroke,
		String displayKeys
	){
		/**
		 * Returns the appropriate KeyStroke for the current operating system.
		 */
		public KeyStroke keyStroke(){
			return (IS_MAC && macStroke != null? macStroke: defaultStroke);
		}

		public int keyStrokeCode(){
			return (IS_MAC && macStroke != null? macStroke.getKeyCode(): defaultStroke.getKeyCode());
		}
	}

	public static final boolean IS_MAC = System.getProperty("os.name")
		.toLowerCase(Locale.ROOT)
		.contains("mac");

	private static final int MENU_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
	private static final List<ShortcutDefinition> REGISTRY = new ArrayList<>();

	// Global Definitions
	public static final ShortcutDefinition FILE_NEW = register("File", "New file…", KeyStroke.getKeyStroke(KeyEvent.VK_N, MENU_MASK), "Ctrl+N");
	public static final ShortcutDefinition FILE_OPEN = register("File", "Open file…", KeyStroke.getKeyStroke(KeyEvent.VK_O, MENU_MASK), "Ctrl+O");
	public static final ShortcutDefinition FILE_SAVE = register("File", "Save", KeyStroke.getKeyStroke(KeyEvent.VK_S, MENU_MASK), "Ctrl+S");
	public static final ShortcutDefinition FILE_SAVE_AS = register("File", "Save as…", KeyStroke.getKeyStroke(KeyEvent.VK_S, MENU_MASK | InputEvent.SHIFT_DOWN_MASK), "Ctrl+Shift+S");
	public static final ShortcutDefinition FILE_EXIT = register("File", "Exit", KeyStroke.getKeyStroke(KeyEvent.VK_Q, MENU_MASK), "Ctrl+Q");

	//TODO connect relocate, paste, delete to popup menu
	public static final ShortcutDefinition EDIT_UNDO = register("Edit", "Undo", KeyStroke.getKeyStroke(KeyEvent.VK_Z, MENU_MASK), (IS_MAC? "⌘Z": "Ctrl+Z"));
	public static final ShortcutDefinition EDIT_REDO = register("Edit", "Redo", KeyStroke.getKeyStroke(KeyEvent.VK_Y, MENU_MASK), KeyStroke.getKeyStroke(KeyEvent.VK_Z, MENU_MASK | InputEvent.SHIFT_DOWN_MASK), (IS_MAC? "⌘⇧Z": "Ctrl+Y"));
	public static final ShortcutDefinition EDIT_RELOCATE = register("Edit", "Relocate", KeyStroke.getKeyStroke(KeyEvent.VK_X, MENU_MASK), "Ctrl+X");
	public static final ShortcutDefinition EDIT_PASTE = register("Edit", "Paste", KeyStroke.getKeyStroke(KeyEvent.VK_V, MENU_MASK), "Ctrl+V");
	public static final ShortcutDefinition EDIT_DELETE = register("Edit", "Delete", KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "Del");
	public static final ShortcutDefinition EDIT_SELECTION = register("Edit", "Edit current selection", KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "F2");

	public static final ShortcutDefinition VIEW_ANCESTOR_TREE = register("View", "Ancestor tree", KeyStroke.getKeyStroke(KeyEvent.VK_1, MENU_MASK), "Ctrl+1");
	public static final ShortcutDefinition VIEW_SUGIYAMA_GRAPH = register("View", "Sugiyama graph", KeyStroke.getKeyStroke(KeyEvent.VK_2, MENU_MASK), "Ctrl+2");
	public static final ShortcutDefinition VIEW_EGO_NETWORK = register("View", "Ego network", KeyStroke.getKeyStroke(KeyEvent.VK_3, MENU_MASK), "Ctrl+3");
	public static final ShortcutDefinition VIEW_FULLSCREEN = register("View", "Full screen", KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "F11");

	public static final ShortcutDefinition NAV_BACK = register("Navigate", "Back", KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, MENU_MASK), "Ctrl+Left");
	public static final ShortcutDefinition NAV_FORWARD = register("Navigate", "Forward", KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, MENU_MASK), "Ctrl+Right");
	public static final ShortcutDefinition NAV_JUMP_TO_INDIVIDUAL = register("Navigate", "Jump to individual…", KeyStroke.getKeyStroke(KeyEvent.VK_J, MENU_MASK), "Ctrl+J");
	public static final ShortcutDefinition NAV_JUMP_TO_INDIVIDUAL_OR_GROUP = register("Navigate", "Jump to individual/Group…", KeyStroke.getKeyStroke(KeyEvent.VK_J, MENU_MASK), "Ctrl+J");

	public static final ShortcutDefinition TREE_TOGGLE_LAYOUT = register("Ancestor Tree", "Toggle layout (vertical / horizontal)", KeyStroke.getKeyStroke(KeyEvent.VK_L, MENU_MASK), "Ctrl+L");
	public static final ShortcutDefinition TREE_TOGGLE_LIFESPAN = register("Ancestor Tree", "Toggle lifespan strip", KeyStroke.getKeyStroke(KeyEvent.VK_T, MENU_MASK), "Ctrl+T");

	public static final ShortcutDefinition HELP_CONTENTS = register("Help", "Help contents", KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "F1");


	private ShortcutRegistry(){}


	public static ShortcutDefinition register(final String category, final String action,
			final KeyStroke defaultStroke, final String displayKeys) {
		return register(category, action, defaultStroke, null, displayKeys);
	}

	public static ShortcutDefinition register(final String category, final String action,
			final KeyStroke defaultStroke, final KeyStroke macStroke, final String displayKeys) {
		final ShortcutDefinition def = new ShortcutDefinition(category, action, defaultStroke, macStroke, displayKeys);
		REGISTRY.add(def);
		return def;
	}

	public static List<ShortcutDefinition> getAllShortcuts(){
		return Collections.unmodifiableList(REGISTRY);
	}

}
