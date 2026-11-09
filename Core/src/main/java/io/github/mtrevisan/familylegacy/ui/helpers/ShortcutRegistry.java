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

import io.github.mtrevisan.familylegacy.i18n.I18N;

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
		String displayKeys,
		String context
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
	public static final ShortcutDefinition FILE_NEW = register(I18N.t("menu.file"), I18N.t("menu.file.new.file"), KeyStroke.getKeyStroke(KeyEvent.VK_N, MENU_MASK), "Ctrl+N");
	public static final ShortcutDefinition FILE_OPEN = register(I18N.t("menu.file"), I18N.t("menu.file.open.file"), KeyStroke.getKeyStroke(KeyEvent.VK_O, MENU_MASK), "Ctrl+O");
	public static final ShortcutDefinition FILE_SAVE_AS = register(I18N.t("menu.file"), I18N.t("menu.file.save_as"), KeyStroke.getKeyStroke(KeyEvent.VK_S, MENU_MASK | InputEvent.SHIFT_DOWN_MASK), "Ctrl+Shift+S");
	public static final ShortcutDefinition FILE_EXIT = register(I18N.t("menu.file"), I18N.t("menu.file.exit"), KeyStroke.getKeyStroke(KeyEvent.VK_Q, MENU_MASK), "Ctrl+Q");

	public static final ShortcutDefinition EDIT_UNDO = register(I18N.t("menu.edit"), I18N.t("menu.edit.undo"), KeyStroke.getKeyStroke(KeyEvent.VK_Z, MENU_MASK), (IS_MAC? "⌘Z": "Ctrl+Z"));
	public static final ShortcutDefinition EDIT_REDO = register(I18N.t("menu.edit"), I18N.t("menu.edit.redo"), KeyStroke.getKeyStroke(KeyEvent.VK_Y, MENU_MASK), KeyStroke.getKeyStroke(KeyEvent.VK_Z, MENU_MASK | InputEvent.SHIFT_DOWN_MASK), (IS_MAC? "⌘⇧Z": "Ctrl+Y"));

	public static final ShortcutDefinition EDIT_RELOCATE_INDIVIDUAL = register(I18N.t("menu.individual"), I18N.t("menu.individual.relocate"), KeyStroke.getKeyStroke(KeyEvent.VK_X, MENU_MASK), "Ctrl+X");
	public static final ShortcutDefinition TOOLS_RESEARCH_ASSISTANT = register(I18N.t("menu.tools"), I18N.t("menu.tools.research_assistant"), KeyStroke.getKeyStroke(KeyEvent.VK_A, MENU_MASK | InputEvent.SHIFT_DOWN_MASK), "Ctrl+Shift+A");
	public static final ShortcutDefinition EDIT_DELETE_INDIVIDUAL = register(I18N.t("menu.individual"), I18N.t("menu.individual.delete"), KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "Del");
	public static final ShortcutDefinition EDIT_SELECTION_INDIVIDUAL = register(I18N.t("menu.individual"), I18N.t("menu.individual.edit.individual"), KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "F2");

	public static final ShortcutDefinition EDIT_RELOCATE_GROUP = register(I18N.t("menu.group"), I18N.t("menu.group.relocate"), KeyStroke.getKeyStroke(KeyEvent.VK_X, MENU_MASK), "Ctrl+X");
	public static final ShortcutDefinition EDIT_DELETE_GROUP = register(I18N.t("menu.group"), I18N.t("menu.group.delete"), KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "Del");
	public static final ShortcutDefinition EDIT_SELECTION_GROUP = register(I18N.t("menu.group"), I18N.t("menu.group.edit.group"), KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "F2");

	public static final ShortcutDefinition EDIT_DELETE_PLACE = register(I18N.t("menu.place"), I18N.t("menu.place.delete"), KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "Del");

	public static final ShortcutDefinition VIEW_BIOLOGICAL_TREE = register(I18N.t("menu.view"), I18N.t("menu.view.biological_tree"), KeyStroke.getKeyStroke(KeyEvent.VK_1, MENU_MASK), "Ctrl+1");
	public static final ShortcutDefinition VIEW_SUGIYAMA_GRAPH = register(I18N.t("menu.view"), I18N.t("menu.view.sugiyama_graph"), KeyStroke.getKeyStroke(KeyEvent.VK_2, MENU_MASK), "Ctrl+2");
	public static final ShortcutDefinition VIEW_EGO_NETWORK = register(I18N.t("menu.view"), I18N.t("menu.view.ego_network"), KeyStroke.getKeyStroke(KeyEvent.VK_3, MENU_MASK), "Ctrl+3");
	public static final ShortcutDefinition VIEW_CHRONOMAP = register(I18N.t("menu.view"), I18N.t("menu.view.chronomap"), KeyStroke.getKeyStroke(KeyEvent.VK_4, MENU_MASK), "Ctrl+4");
	public static final ShortcutDefinition VIEW_FULLSCREEN = register(I18N.t("menu.view"), I18N.t("menu.view.full_screen"), KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "F11");

	public static final ShortcutDefinition NAV_BACK = register(I18N.t("menu.navigate"), I18N.t("menu.navigate.back"), KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, MENU_MASK), "Ctrl+Left");
	public static final ShortcutDefinition NAV_FORWARD = register(I18N.t("menu.navigate"), I18N.t("menu.navigate.forward"), KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, MENU_MASK), "Ctrl+Right");
	public static final ShortcutDefinition NAV_JUMP_TO_INDIVIDUAL = register(I18N.t("menu.navigate"), I18N.t("menu.navigate.jump.to.individual"), KeyStroke.getKeyStroke(KeyEvent.VK_J, MENU_MASK), "Ctrl+J");
	public static final ShortcutDefinition NAV_JUMP_TO_INDIVIDUAL_OR_GROUP = register(I18N.t("menu.navigate"), I18N.t("menu.navigate.jump.to.individual.or.group"), KeyStroke.getKeyStroke(KeyEvent.VK_J, MENU_MASK), "Ctrl+J");
	public static final ShortcutDefinition NAV_FIND_RECORD = register(I18N.t("menu.navigate"), I18N.t("menu.navigate.find.record"), KeyStroke.getKeyStroke(KeyEvent.VK_F, MENU_MASK), "Ctrl+F");

	public static final ShortcutDefinition TREE_TOGGLE_LAYOUT = register(I18N.t("menu.view.biological_tree"), I18N.t("menu.view.biological_tree.toggle.layout"), KeyStroke.getKeyStroke(KeyEvent.VK_L, MENU_MASK), "Ctrl+L");
	public static final ShortcutDefinition TREE_TOGGLE_LIFESPAN_EVENTS_STRIP = register(I18N.t("menu.view.biological_tree"), I18N.t("menu.view.biological_tree.toggle.lifespan.strip"), KeyStroke.getKeyStroke(KeyEvent.VK_T, MENU_MASK), "Ctrl+T");
	public static final ShortcutDefinition TREE_KINSHIP = register(I18N.t("menu.view.biological_tree"), I18N.t("menu.view.biological_tree.kinship"), KeyStroke.getKeyStroke(KeyEvent.VK_K, MENU_MASK), "Ctrl+K");
	public static final ShortcutDefinition TREE_PEDIGREE_COLLAPSE = register(I18N.t("menu.view.biological_tree"), I18N.t("menu.view.biological_tree.pedigree.collapse"), KeyStroke.getKeyStroke(KeyEvent.VK_P, MENU_MASK), "Ctrl+P");

	public static final ShortcutDefinition BOOKMARK_CURRENT_VIEW = register(I18N.t("menu.bookmarks"), I18N.t("menu.item.bookmark_current_view"), KeyStroke.getKeyStroke(KeyEvent.VK_D, MENU_MASK), "Ctrl+D");

	public static final ShortcutDefinition HELP_CONTENTS = register(I18N.t("menu.help"), I18N.t("menu.help.help.contents"), KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "F1");

	// Contextual shortcuts — active only inside a specific component
	public static final String CTX_CHRONOMAP = I18N.t("shortcut.context.chronomap");
	public static final String CTX_RESEARCH_PALETTE = I18N.t("shortcut.context.research_palette");
	public static final String CTX_LIST = I18N.t("shortcut.context.list");
	public static final String CTX_DIALOG = I18N.t("shortcut.context.dialog");

	public static final ShortcutDefinition CHRONOMAP_TOGGLE_3D = register(I18N.t("shortcut.category.chronomap"), I18N.t("shortcut.chronomap.toggle.3d"), KeyStroke.getKeyStroke(KeyEvent.VK_2, InputEvent.CTRL_DOWN_MASK), "Ctrl+2", CTX_CHRONOMAP);
	public static final ShortcutDefinition CHRONOMAP_TOGGLE_2D = register(I18N.t("shortcut.category.chronomap"), I18N.t("shortcut.chronomap.toggle.2d"), KeyStroke.getKeyStroke(KeyEvent.VK_3, InputEvent.CTRL_DOWN_MASK), "Ctrl+3", CTX_CHRONOMAP);
	public static final ShortcutDefinition CHRONOMAP_TIMELINE_PREV_MONTH = register(I18N.t("shortcut.category.chronomap"), I18N.t("shortcut.chronomap.timeline.prev.month"), KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "Left", CTX_CHRONOMAP);
	public static final ShortcutDefinition CHRONOMAP_TIMELINE_NEXT_MONTH = register(I18N.t("shortcut.category.chronomap"), I18N.t("shortcut.chronomap.timeline.next.month"), KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "Right", CTX_CHRONOMAP);
	public static final ShortcutDefinition CHRONOMAP_TIMELINE_PREV_YEAR = register(I18N.t("shortcut.category.chronomap"), I18N.t("shortcut.chronomap.timeline.prev.year"), KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.SHIFT_DOWN_MASK), "Shift+Left", CTX_CHRONOMAP);
	public static final ShortcutDefinition CHRONOMAP_TIMELINE_NEXT_YEAR = register(I18N.t("shortcut.category.chronomap"), I18N.t("shortcut.chronomap.timeline.next.year"), KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.SHIFT_DOWN_MASK), "Shift+Right", CTX_CHRONOMAP);

	public static final ShortcutDefinition RESEARCH_EXECUTE = register(I18N.t("shortcut.category.research"), I18N.t("shortcut.research.execute"), KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Enter", CTX_RESEARCH_PALETTE);
	public static final ShortcutDefinition RESEARCH_EXECUTE_ALT_1 = register(I18N.t("shortcut.category.research"), I18N.t("shortcut.research.execute.alt.context"), KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "Ctrl+Enter", CTX_RESEARCH_PALETTE);
	public static final ShortcutDefinition RESEARCH_EXECUTE_ALT_2 = register(I18N.t("shortcut.category.research"), I18N.t("shortcut.research.execute.alt.context.2"), KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "Ctrl+Shift+Enter", CTX_RESEARCH_PALETTE);
	public static final ShortcutDefinition RESEARCH_EXECUTE_ALT_3 = register(I18N.t("shortcut.category.research"), I18N.t("shortcut.research.execute.alt.context.3"), KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK), "Ctrl+Alt+Enter", CTX_RESEARCH_PALETTE);
	public static final ShortcutDefinition RESEARCH_IGNORE = register(I18N.t("shortcut.category.research"), I18N.t("shortcut.research.ignore"), KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK), "Ctrl+D", CTX_RESEARCH_PALETTE);
	public static final ShortcutDefinition RESEARCH_TOGGLE_IGNORED = register(I18N.t("shortcut.category.research"), I18N.t("shortcut.research.toggle.ignored"), KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK), "Ctrl+I", CTX_RESEARCH_PALETTE);
	public static final ShortcutDefinition RESEARCH_NAVIGATE_UP = register(I18N.t("shortcut.category.research"), I18N.t("shortcut.research.navigate.up"), KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "Up", CTX_RESEARCH_PALETTE);
	public static final ShortcutDefinition RESEARCH_NAVIGATE_DOWN = register(I18N.t("shortcut.category.research"), I18N.t("shortcut.research.navigate.down"), KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "Down", CTX_RESEARCH_PALETTE);

	public static final ShortcutDefinition LIST_MOVE_UP = register(I18N.t("shortcut.category.list"), I18N.t("shortcut.list.move.up"), KeyStroke.getKeyStroke(KeyEvent.VK_UP, MENU_MASK), "Ctrl+Up", CTX_LIST);
	public static final ShortcutDefinition LIST_MOVE_DOWN = register(I18N.t("shortcut.category.list"), I18N.t("shortcut.list.move.down"), KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, MENU_MASK), "Ctrl+Down", CTX_LIST);

	public static final ShortcutDefinition DIALOG_UNDO = register(I18N.t("shortcut.category.dialog"), I18N.t("shortcut.dialog.undo"), KeyStroke.getKeyStroke(KeyEvent.VK_Z, MENU_MASK), (IS_MAC? "⌘Z": "Ctrl+Z"), CTX_DIALOG);
	public static final ShortcutDefinition DIALOG_REDO = register(I18N.t("shortcut.category.dialog"), I18N.t("shortcut.dialog.redo"), KeyStroke.getKeyStroke(KeyEvent.VK_Y, MENU_MASK), KeyStroke.getKeyStroke(KeyEvent.VK_Z, MENU_MASK | InputEvent.SHIFT_DOWN_MASK), (IS_MAC? "⌘⇧Z": "Ctrl+Y"), CTX_DIALOG);
	public static final ShortcutDefinition DIALOG_CLOSE =register(I18N.t("shortcut.category.dialog"), I18N.t("shortcut.dialog.close"), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "Esc", CTX_DIALOG);


	private ShortcutRegistry(){}


	public static ShortcutDefinition register(final String category, final String action,
			final KeyStroke defaultStroke, final String displayKeys){
		return register(category, action, defaultStroke, null, displayKeys, null);
	}

	public static ShortcutDefinition register(final String category, final String action,
			final KeyStroke defaultStroke, final KeyStroke macStroke, final String displayKeys){
		return register(category, action, defaultStroke, macStroke, displayKeys, null);
	}

	public static ShortcutDefinition register(final String category, final String action,
			final KeyStroke defaultStroke, final String displayKeys, final String context){
		return register(category, action, defaultStroke, null, displayKeys, context);
	}

	public static ShortcutDefinition register(final String category, final String action,
			final KeyStroke defaultStroke, final KeyStroke macStroke, final String displayKeys, final String context){
		final ShortcutDefinition def = new ShortcutDefinition(category, action, defaultStroke, macStroke, displayKeys, context);
		REGISTRY.add(def);
		return def;
	}

	public static List<ShortcutDefinition> getAllShortcuts(){
		return Collections.unmodifiableList(REGISTRY);
	}

}
