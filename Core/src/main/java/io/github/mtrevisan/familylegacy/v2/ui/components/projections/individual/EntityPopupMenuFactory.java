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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;


/**
 * Strategy interface for creating dynamic context popup menus for an {@link IndividualPanel}.
 */
@FunctionalInterface
public interface EntityPopupMenuFactory<T extends JPanel, L extends EntityListener>{

	/**
	 * Creates and configures a context-specific {@link JPopupMenu}.
	 *
	 * @param panel    The source panel invoking the menu.
	 * @param listener The listener handling user actions.
	 * @param model    The FLEF model used for evaluating dynamic states and menu items.
	 * @return The fully configured popup menu.
	 */
	JPopupMenu createPopupMenu(T panel, L listener, FLEFModel model);

}
