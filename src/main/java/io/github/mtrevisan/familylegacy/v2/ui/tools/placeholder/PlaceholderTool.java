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
package io.github.mtrevisan.familylegacy.v2.ui.tools.placeholder;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;

import javax.swing.JOptionPane;
import java.util.Objects;


/**
 * Tool that reserves a menu entry for a planned feature and informs the
 * user when it is invoked.
 * <p>
 * Using a real {@link ToolOperation} instead of a bare {@code JMenuItem}
 * keeps the menu structure uniform: every entry in the {@code Tools}
 * menu is a tool, and enabling one consists in replacing its placeholder
 * with the actual implementation, without touching the menu factory.
 */
public final class PlaceholderTool implements ToolOperation{

	private final String name;


	public PlaceholderTool(final String name){
		this.name = Objects.requireNonNull(name, "name must not be null");
	}


	@Override
	public String getName(){
		return name;
	}

	@Override
	public void run(final ToolContext context){
		JOptionPane.showMessageDialog(context.owner(),
			"\"" + name.replace("…", "").trim() + "\" is not implemented yet.",
			"Feature Not Available",
			JOptionPane.INFORMATION_MESSAGE);
	}

}
