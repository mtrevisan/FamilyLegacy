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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.KeyStroke;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


/** A key has one meaning: the shortcuts of the menus do not share their keys, and the ones of a view do not take the keys of the menus. */
class ShortcutRegistryTest{

	/** The menus whose commands work on the selected record, so that the same key means the same thing on an individual, a group or a place. */
	private static final Set<String> SELECTION_MENUS = Set.of(I18N.t("menu.individual"), I18N.t("menu.group"), I18N.t("menu.place"));


	@Test
	void aKeyOfTheMenusHasOneMeaning(){
		final Map<KeyStroke, List<ShortcutRegistry.ShortcutDefinition>> byKey = new TreeMap<>((a, b) -> a.toString().compareTo(b.toString()));
		for(final ShortcutRegistry.ShortcutDefinition shortcut : ShortcutRegistry.getAllShortcuts())
			if(shortcut.context() == null)
				byKey.computeIfAbsent(shortcut.keyStroke(), key -> new ArrayList<>()).add(shortcut);

		final List<String> clashes = new ArrayList<>();
		for(final Map.Entry<KeyStroke, List<ShortcutRegistry.ShortcutDefinition>> entry : byKey.entrySet()){
			final List<ShortcutRegistry.ShortcutDefinition> sharing = entry.getValue();
			if(sharing.size() < 2)
				continue;

			//the command of the selected record (edit, delete) is the same key in the menus of the individual, the group and the place;
			//the jump to an individual and to an individual or group are one entry, of two views
			final boolean selection = sharing.stream().allMatch(shortcut -> SELECTION_MENUS.contains(shortcut.category()));
			final boolean jump = sharing.stream()
				.allMatch(shortcut -> shortcut == ShortcutRegistry.NAV_JUMP_TO_INDIVIDUAL
					|| shortcut == ShortcutRegistry.NAV_JUMP_TO_INDIVIDUAL_OR_GROUP);
			if(!selection && !jump)
				clashes.add(entry.getKey() + " " + sharing.stream().map(ShortcutRegistry.ShortcutDefinition::action).toList());
		}

		Assertions.assertTrue(clashes.isEmpty(), "keys with more than one meaning: " + clashes);
	}

	@Test
	void theKeysOfTheChronomapAreNotTheOnesOfTheMenus(){
		final List<String> clashes = new ArrayList<>();
		for(final ShortcutRegistry.ShortcutDefinition chronomap : ShortcutRegistry.getAllShortcuts())
			if(I18N.t("shortcut.context.chronomap").equals(chronomap.context()))
				for(final ShortcutRegistry.ShortcutDefinition other : ShortcutRegistry.getAllShortcuts())
					if(other != chronomap && other.context() == null && other.keyStroke().equals(chronomap.keyStroke()))
						clashes.add(chronomap.action() + " / " + other.action() + " on " + chronomap.keyStroke());

		Assertions.assertTrue(clashes.isEmpty(), "keys of the chronomap that are the ones of a menu: " + clashes);
	}

	@Test
	void relocateIsNotTheCutOfTheClipboard(){
		Assertions.assertNotEquals(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X,
			java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), ShortcutRegistry.EDIT_RELOCATE_INDIVIDUAL.keyStroke());
		Assertions.assertEquals(ShortcutRegistry.EDIT_RELOCATE_INDIVIDUAL.keyStroke(), ShortcutRegistry.EDIT_RELOCATE_GROUP.keyStroke());
	}

}
