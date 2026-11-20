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

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import java.awt.Point;


/** A text that is cut shows in full as a tooltip; one that fits has none (or keeps its own). */
class ClippedTextTooltipsTest{

	private static final String TEXT = "A very long label that surely does not fit in a narrow column";


	@Test
	void aCutLabelHasItsWholeTextAsTooltip(){
		final JLabel label = new JLabel(TEXT);
		label.setSize(40, 20);

		ClippedTextTooltips.update(label);

		Assertions.assertEquals("<html>" + TEXT + "</html>", label.getToolTipText());
	}

	@Test
	void aLabelThatFitsHasNoTooltipAndLosesTheOneItGotWhenItGrows(){
		final JLabel label = new JLabel(TEXT);
		label.setSize(40, 20);
		ClippedTextTooltips.update(label);

		label.setSize(label.getPreferredSize());
		ClippedTextTooltips.update(label);

		Assertions.assertNull(label.getToolTipText());
	}

	@Test
	void theTooltipOfTheComponentIsKeptUnderTheText(){
		final JCheckBox box = new JCheckBox(TEXT);
		box.setToolTipText("What it does");
		box.setSize(40, 20);

		ClippedTextTooltips.update(box);
		Assertions.assertEquals("<html><b>" + TEXT + "</b><br>What it does</html>", box.getToolTipText());

		box.setSize(box.getPreferredSize());
		ClippedTextTooltips.update(box);
		Assertions.assertEquals("What it does", box.getToolTipText());
	}

	@Test
	void theCharactersOfMarkupAreEscaped(){
		Assertions.assertEquals("<html>a &lt; b &amp; c</html>", ClippedTextTooltips.withText("a < b & c", null));
	}

	@Test
	void aComponentNotLaidOutYetIsNotCut(){
		final JLabel label = new JLabel(TEXT);

		ClippedTextTooltips.update(label);

		Assertions.assertNull(label.getToolTipText());
	}

	@Test
	void aCutItemOfAComboBoxHasItsWholeTextAsTooltip(){
		final JComboBox<String> combo = new JComboBox<>(new String[]{TEXT, "short"});
		combo.setSize(60, 20);

		ClippedTextTooltips.update(combo);
		Assertions.assertEquals("<html>" + TEXT + "</html>", combo.getToolTipText());

		combo.setSelectedIndex(1);
		ClippedTextTooltips.update(combo);
		//(the item that fits has none)
		Assertions.assertNull(combo.getToolTipText());
	}

	@Test
	void theCutItemUnderTheMouseInAListHasItsWholeTextAsTooltip(){
		final JList<String> list = new JList<>(new String[]{TEXT, "short"});
		list.setSize(60, 60);

		ClippedTextTooltips.updateList(list, new Point(5, 5));
		Assertions.assertEquals("<html>" + TEXT + "</html>", list.getToolTipText());

		final int secondRow = list.getCellBounds(1, 1).y + 2;
		ClippedTextTooltips.updateList(list, new Point(5, secondRow));
		Assertions.assertNull(list.getToolTipText());
	}

}
