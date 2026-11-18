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
package io.github.mtrevisan.familylegacy.ui.components.projections.actions;


import io.github.mtrevisan.familylegacy.preferences.UxLevel;

import javax.swing.KeyStroke;


/**
 * A single operation exposed by the {@code Tools} menu.
 * <p>
 * Implementations are stateless and self-contained: they receive a
 * {@link ToolContext} with the model and the owning frame, perform
 * their work, and show any UI they need (typically a
 * {@link ReportDialog}). Keeping each tool behind this interface lets
 * the menu bar enumerate them without knowing their internals, which is
 * what prevents the menu code from growing into a god class.
 */
public interface ToolOperation{

	/** Menu label, including the trailing ellipsis when a dialog follows. */
	String getName();

	default KeyStroke getAccelerator(){
		return null;
	}

	/**
	 * @return	The lowest user level the operation is shown at in the menus (progressive disclosure, see
	 * 	{@code FeatureVisibilityService}): the operation is always usable, the level only decides whether it is offered by default.
	 */
	default UxLevel minimumLevel(){
		return UxLevel.BASIC;
	}

	/** Runs the operation. */
	void run(ToolContext context);

	/**
	 * Determines if the tool should be enabled based on the current context.
	 * Defaults to true if not overridden.
	 */
	default boolean isEnabled(final ToolContext context){
		return true;
	}

}
