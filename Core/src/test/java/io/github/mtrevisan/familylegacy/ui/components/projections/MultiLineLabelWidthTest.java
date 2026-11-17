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
package io.github.mtrevisan.familylegacy.ui.components.projections;

import io.github.mtrevisan.familylegacy.ui.components.MultiLineLabel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.Font;


/** A name is never clipped: when the layout gives the label less than its maximum width, the name wraps on that width. */
class MultiLineLabelWidthTest{

	@Test
	void aNameNarrowerThanMaxWidthButWiderThanTheBoundsWraps(){
		final MultiLineLabel label = new MultiLineLabel(2);
		label.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
		final String name = "Antonio Trivisan";
		final int nameWidth = label.getFontMetrics(label.getFont()).stringWidth(name);

		label.setMaxWidth(nameWidth + 2);
		label.setFormattedText(name);
		Assertions.assertEquals(name, label.getText(), "before layout the name fits the maximum width");

		label.setBounds(0, 0, nameWidth - 2, label.getPreferredSize().height);
		Assertions.assertEquals("Antonio\nTrivisan", label.getText(), "the layout gave less room: the name must wrap");
	}

	@Test
	void aWiderBoundsDoesNotExceedMaxWidth(){
		final MultiLineLabel label = new MultiLineLabel(2);
		label.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
		final String name = "Antonio Trivisan";
		final int nameWidth = label.getFontMetrics(label.getFont()).stringWidth(name);

		label.setMaxWidth(nameWidth - 2);
		label.setFormattedText(name);
		label.setBounds(0, 0, nameWidth + 50, label.getPreferredSize().height);
		Assertions.assertEquals("Antonio\nTrivisan", label.getText(), "the maximum width still wins over wider bounds");
	}

}
