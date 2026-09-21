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
package io.github.mtrevisan.familylegacy.v2.ui.tools.files;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;

import javax.swing.JOptionPane;


/**
 * Base class for the import/export tools that are not yet implemented.
 * <p>
 * Every subclass declares its own name; the {@link #run(ToolContext)}
 * method is shared and shows a placeholder dialog with a clear
 * description of the missing feature. When the actual implementation
 * lands, the subclass overrides {@code run} and the placeholder
 * disappears.
 * <p>
 * Keeping the placeholder behaviour in one place avoids ten copies of
 * the same dialog.
 */
public abstract class PlaceholderFileTool implements ToolOperation{


	@Override
	public void run(final ToolContext context){
		JOptionPane.showMessageDialog(context.owner(),
			"\"" + getName().replace("…", "").trim() + "\" is not implemented yet.",
			"Feature Not Available", JOptionPane.INFORMATION_MESSAGE);
	}

}
