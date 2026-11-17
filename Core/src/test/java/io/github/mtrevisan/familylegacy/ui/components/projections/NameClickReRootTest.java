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

import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.components.MultiLineLabel;
import io.github.mtrevisan.familylegacy.ui.components.projections.individual.EntityMouseListeners;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;


/** Re-rooting happens only by clicking the drawn text of the name, even a lone "?", never the blank part of the name label. */
class NameClickReRootTest{

	private static MultiLineLabel label(final String text){
		final MultiLineLabel label = new MultiLineLabel(2);
		label.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
		label.setFormattedText(text);
		label.setSize(200, label.getPreferredSize().height);
		return label;
	}

	/** A point vertically centred on the first line, at the given x. */
	private static Point onFirstLine(final MultiLineLabel label, final int x){
		final FontMetrics fm = label.getFontMetrics(label.getFont());
		return new Point(x, label.getInsets().top + fm.getHeight() / 2);
	}

	private static int textWidth(final MultiLineLabel label, final String text){
		return label.getFontMetrics(label.getFont()).stringWidth(text);
	}

	@Test
	void aLoneQuestionMarkIsClickable(){
		final MultiLineLabel label = label("?");
		final int width = textWidth(label, "?");

		Assertions.assertTrue(label.isTextHit(onFirstLine(label, Math.max(1, width / 2))), "middle of \"?\"");
		Assertions.assertFalse(label.isTextHit(onFirstLine(label, width + 20)), "blank space right of \"?\"");
	}

	@Test
	void theLastGlyphOfANameIsClickable(){
		final MultiLineLabel label = label("Mario Rossi");
		final int width = textWidth(label, "Mario Rossi");

		Assertions.assertTrue(label.isTextHit(onFirstLine(label, width - 2)), "inside the last glyph");
		Assertions.assertFalse(label.isTextHit(onFirstLine(label, width + 20)), "blank space right of the name");
	}

	@Test
	void aWrappedNameIsClickableOnlyOnTheTextOfEachLine(){
		final MultiLineLabel label = label("Ugo Bartolomeo");
		final int shortWidth = textWidth(label, "Ugo");
		final int longWidth = textWidth(label, "Bartolomeo");
		label.setMaxWidth(longWidth + 4);
		label.setSize(longWidth + 4, label.getPreferredSize().height);
		Assertions.assertEquals("Ugo\nBartolomeo", label.getText(), "the name must wrap for this test to mean anything");
		final int lineHeight = label.getFontMetrics(label.getFont()).getHeight();
		final int firstY = label.getInsets().top + lineHeight / 2;
		final int secondY = firstY + lineHeight;

		Assertions.assertTrue(label.isTextHit(new Point(shortWidth / 2, firstY)), "on \"Ugo\"");
		Assertions.assertFalse(label.isTextHit(new Point(shortWidth + 10, firstY)),
			"right of \"Ugo\", although inside the width of the line below");
		Assertions.assertTrue(label.isTextHit(new Point(longWidth / 2, secondY)), "on \"Bartolomeo\"");
		Assertions.assertTrue(label.isTextHit(new Point(longWidth - 2, secondY)), "last glyph of \"Bartolomeo\"");
		Assertions.assertFalse(label.isTextHit(new Point(longWidth + 2, secondY)), "right of \"Bartolomeo\"");
		Assertions.assertFalse(label.isTextHit(new Point(longWidth / 2, secondY + lineHeight)), "below the last line");
	}

	@Test
	void theBlankBelowTheNameIsNotClickable(){
		final MultiLineLabel label = label("?");
		final FontMetrics fm = label.getFontMetrics(label.getFont());

		Assertions.assertFalse(label.isTextHit(new Point(1, fm.getHeight() + fm.getHeight() / 2)), "second, empty line");
	}

	@Test
	void pressingTheTextReRootsAndPressingTheBlankSelects(){
		final MultiLineLabel label = label("?");
		final JPanel panel = new JPanel(null);
		panel.add(label);
		final FLEFRecord record = FLEFRecord.createMainRecord("I1", "individual");
		final List<String> rooted = new ArrayList<>();
		final List<FLEFRecord> selected = new ArrayList<>();
		EntityMouseListeners.builder(panel)
			.nameLabel(label)
			.recordSupplier(() -> record)
			.onRootSelected(rooted::add)
			.onSelected(selected::add)
			.build()
			.install();

		final int width = textWidth(label, "?");
		press(label, onFirstLine(label, width + 30));
		Assertions.assertEquals(List.of(), rooted, "blank part of the label must not re-root");
		Assertions.assertEquals(1, selected.size(), "blank part of the label selects the box");

		press(label, onFirstLine(label, Math.max(1, width / 2)));
		Assertions.assertEquals(List.of("I1"), rooted, "the \"?\" re-roots");
		Assertions.assertEquals(1, selected.size(), "re-rooting is not also a selection");
	}

	private static void press(final MultiLineLabel label, final Point p){
		final MouseEvent e = new MouseEvent(label, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
			InputEvent.BUTTON1_DOWN_MASK, p.x, p.y, 1, false, MouseEvent.BUTTON1);
		for(final MouseListener l : label.getMouseListeners())
			l.mousePressed(e);
	}

}
