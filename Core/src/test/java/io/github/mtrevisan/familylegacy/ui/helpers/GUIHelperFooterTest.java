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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;


/** The bar at the bottom of a window with a table says what it shows at the left, and has the buttons at the right. */
class GUIHelperFooterTest{

	@Test
	void theStatusIsAtTheLeftAndTheButtonsAtTheRight(){
		final JLabel status = new JLabel("12 shown");
		final JButton first = new JButton("One");
		final JButton second = new JButton("Two");

		final JPanel footer = GUIHelper.createStatusFooter(status, first, second);
		final BorderLayout layout = (BorderLayout)footer.getLayout();

		Assertions.assertEquals(BorderLayout.WEST, layout.getConstraints(status));
		final JPanel buttons = (JPanel)layout.getLayoutComponent(BorderLayout.EAST);
		Assertions.assertTrue(buttons.getLayout() instanceof FlowLayout && ((FlowLayout)buttons.getLayout()).getAlignment() == FlowLayout.RIGHT);
		Assertions.assertArrayEquals(new java.awt.Component[]{first, second}, buttons.getComponents());
	}

}
