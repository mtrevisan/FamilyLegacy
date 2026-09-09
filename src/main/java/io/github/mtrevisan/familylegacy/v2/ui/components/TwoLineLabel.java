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
package io.github.mtrevisan.familylegacy.v2.ui.components;

import org.apache.commons.lang3.StringUtils;

import javax.swing.JTextArea;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;


public class TwoLineLabel extends JTextArea{

	private static final String ELLIPSIS = "…";


	private String rawText = StringUtils.EMPTY;

	// Maximum width constraint; -1 means no constraint (use component width)
	private int maxWidth = -1;


	public TwoLineLabel(){
		setEditable(false);
		setFocusable(false);
		setOpaque(false);
		setLineWrap(false);
		setWrapStyleWord(false);
		setMargin(new Insets(0, 0, 0, 0));
		setBorder(null);

		// Recalculate truncation when window layout sets width
		addComponentListener(new ComponentAdapter(){
			@Override
			public void componentResized(final ComponentEvent ce){
				updateFormattedText();
			}
		});
	}


	/**
	 * Sets the maximum width for the label. This width will be used for text truncation
	 * and as the preferred width limit. A value <= 0 removes the constraint.
	 */
	public void setMaxWidth(final int maxWidth){
		this.maxWidth = maxWidth;

		updateFormattedText();
	}


	public void setFormattedText(final String text){
		rawText = (text != null? text: StringUtils.EMPTY);

		updateFormattedText();
	}

	@Override
	public void setBounds(final int x, final int y, final int width, final int height){
		super.setBounds(x, y, width, height);

		updateFormattedText();
	}

	private void updateFormattedText(){
		if(rawText.isBlank()){
			setText(StringUtils.EMPTY);

			return;
		}

		// Determine available width: use maxWidth if set, otherwise component width minus insets
		final int availWidth = (maxWidth > 0? maxWidth: getWidth() - getInsets().left - getInsets().right);
		if(availWidth <= 0){
			setText(rawText);

			return;
		}

		final Font font = getFont();
		if(font == null){
			setText(rawText);

			return;
		}
		final FontMetrics fm = getFontMetrics(font);

		final String[] words = StringUtils.split(rawText);
		if(words == null || words.length == 0){
			setText(StringUtils.EMPTY);

			return;
		}

		// Construction ROW 1
		final StringBuilder line1 = new StringBuilder();
		int i = 0;
		while(i < words.length){
			final String candidateLine = (line1.isEmpty()? words[i]: line1 + StringUtils.SPACE + words[i]);
			if(fm.stringWidth(candidateLine) <= availWidth){
				line1.append(line1.isEmpty()? StringUtils.EMPTY: StringUtils.SPACE)
					.append(words[i]);

				i ++;
			}
			else
				break;
		}

		if(line1.isEmpty()){
			line1.append(words[0]);

			i = 1;
		}

		// Construction ROW 2
		final StringBuilder line2 = new StringBuilder();
		while(i < words.length){
			line2.append(line2.isEmpty()? StringUtils.EMPTY: StringUtils.SPACE)
				.append(words[i]);

			i ++;
		}

		// Truncate LINE 2 if it exceeds the available space
		if(!line2.isEmpty()){
			String candidateLine = line2.toString();
			if(fm.stringWidth(candidateLine) > availWidth){
				while(!candidateLine.isEmpty() && fm.stringWidth(candidateLine + ELLIPSIS) > availWidth)
					candidateLine = candidateLine.substring(0, candidateLine.length() - 1);
				candidateLine = candidateLine.trim() + ELLIPSIS;
			}
			setText(line1 + StringUtils.LF + candidateLine);
		}
		else
			setText(line1.toString());
	}

	@Override
	public Dimension getPreferredSize(){
		final Font font = getFont();
		if(font == null)
			return super.getPreferredSize();

		final FontMetrics fm = getFontMetrics(font);
		final int height = (fm.getHeight() * 2) + getInsets().top + getInsets().bottom;

		// If maxWidth is set, use it as the preferred width; otherwise use the superclass width
		return new Dimension((maxWidth > 0? maxWidth: super.getPreferredSize().width), height);
	}

}