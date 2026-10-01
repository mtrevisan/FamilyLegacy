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
package io.github.mtrevisan.familylegacy.v2.ui.bindings;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;


/**
 * Builder that collects menu entries.
 * <p>
 * You can call {@link #item(String, Runnable)} to add a plain item,
 * {@link #selectionSensitiveItem(String, Runnable)} for an item that is enabled only when there is a selection, or
 * {@link #item(String, Runnable, Supplier)} for a fully custom enable condition.
 * {@link #separator()} inserts a separator.
 */
public final class MenuBuilder{

	/**
	 * Immutable record representing a menu entry.
	 */
	record MenuEntry(String label, Runnable action, Supplier<Boolean> enabledCondition, boolean isSeparator){
		private static MenuEntry createEntry(final String label, final Runnable action,
			final Supplier<Boolean> enabledCondition){
			return new MenuEntry(label, action, enabledCondition, false);
		}

		private static MenuEntry createSeparator(){
			return new MenuEntry(null, null, null, true);
		}
	}


	private final Supplier<Boolean> hasSelection;
	private final List<MenuEntry> entries = new ArrayList<>();


	MenuBuilder(final Supplier<Boolean> hasSelection){
		this.hasSelection = hasSelection;
	}

	/**
	 * Adds a menu item that is always enabled.
	 *
	 * @param label  The item label.
	 * @param action The action to run when clicked.
	 * @return   This builder.
	 */
	public MenuBuilder item(final String label, final Runnable action){
		entries.add(MenuEntry.createEntry(label, action, () -> true));

		return this;
	}

	/**
	 * Adds a menu item that is enabled only when {@link #hasSelection} is {@code true}.
	 *
	 * @param label  The item label.
	 * @param action The action to run when clicked.
	 * @return   This builder.
	 */
	public MenuBuilder selectionSensitiveItem(final String label, final Runnable action){
		entries.add(MenuEntry.createEntry(label, action, hasSelection));

		return this;
	}

	/**
	 * Adds a menu item with a custom enable condition.
	 *
	 * @param label            The item label.
	 * @param action           The action to run when clicked.
	 * @param enabledCondition Supplier that returns {@code true} when the item should be enabled.
	 * @return   This builder.
	 */
	public MenuBuilder item(final String label, final Runnable action, final Supplier<Boolean> enabledCondition){
		entries.add(MenuEntry.createEntry(label, action, enabledCondition));

		return this;
	}

	/**
	 * Adds a separator.
	 *
	 * @return   This builder.
	 */
	public MenuBuilder separator(){
		entries.add(MenuEntry.createSeparator());

		return this;
	}

	List<MenuEntry> getEntries(){
		return entries;
	}

}
