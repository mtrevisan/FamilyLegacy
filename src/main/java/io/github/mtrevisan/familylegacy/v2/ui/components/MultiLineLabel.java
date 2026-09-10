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


public class MultiLineLabel extends JTextArea{

	private static final String ELLIPSIS = "…";


	// Maximum number of visible lines
	private final int maxLines;

	private String rawText = StringUtils.EMPTY;

	// Maximum width constraint; -1 means no constraint (use component width)
	private int maxWidth = -1;


	public MultiLineLabel(final int maxLines){
		this.maxLines = Math.max(1, maxLines);

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

		final StringBuilder result = new StringBuilder();
		int wordIdx = 0;
		for(int lineIdx = 0; lineIdx < maxLines && wordIdx < words.length; lineIdx ++){
			final boolean isLastLine = (lineIdx == maxLines - 1);
			final StringBuilder currentLine = new StringBuilder();
			if(!isLastLine){
				while(wordIdx < words.length){
					final String candidateLine = (currentLine.isEmpty()
						? words[wordIdx]
						: currentLine + StringUtils.SPACE + words[wordIdx]);
					if(fm.stringWidth(candidateLine) > availWidth)
						break;

					currentLine.append(currentLine.isEmpty()? StringUtils.EMPTY: StringUtils.SPACE)
						.append(words[wordIdx]);

					wordIdx ++;
				}

				if(currentLine.isEmpty()){
					currentLine.append(words[wordIdx]);

					wordIdx ++;
				}
			}
			else{
				while(wordIdx < words.length){
					currentLine.append(currentLine.isEmpty()? StringUtils.EMPTY: StringUtils.SPACE)
						.append(words[wordIdx]);

					wordIdx ++;
				}

				String candidateLine = currentLine.toString();
				if(fm.stringWidth(candidateLine) > availWidth){
					while(!candidateLine.isEmpty() && fm.stringWidth(candidateLine + ELLIPSIS) > availWidth)
						candidateLine = candidateLine.substring(0, candidateLine.length() - 1);
					candidateLine = candidateLine.trim() + ELLIPSIS;
				}
				currentLine.setLength(0);
				currentLine.append(candidateLine);
			}

			if(!result.isEmpty())
				result.append(StringUtils.LF);
			result.append(currentLine);
		}

		setText(result.toString());
	}

	@Override
	public Dimension getPreferredSize(){
		final Font font = getFont();
		if(font == null)
			return super.getPreferredSize();

		final FontMetrics fm = getFontMetrics(font);
		final int height = fm.getHeight() * maxLines + getInsets().top + getInsets().bottom;

		// If maxWidth is set, use it as the preferred width; otherwise use the superclass width
		return new Dimension((maxWidth > 0? maxWidth: super.getPreferredSize().width), height);
	}

}