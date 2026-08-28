/**
 * Copyright (c) 2022 Mauro Trevisan
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

import javax.swing.JLabel;
import javax.swing.JToolTip;
import java.awt.FontMetrics;
import java.awt.SystemColor;
import java.io.Serial;


/**
 * A JLabel that automatically shows a tooltip with the full text only when the text
 * is clipped (i.e., the preferred width exceeds the actual width).
 * <p>
 * The tooltip is computed on‑the‑fly when the mouse hovers, so no manual
 * {@code manageToolTip()} calls are needed.
 * <p>
 * Usage:
 * <pre>
 * LabelAutoToolTip label = new LabelAutoToolTip();
 * label.setText("Very long text that may be clipped");
 * // The tooltip will appear automatically when the text is clipped.
 * </pre>
 */
public class LabelAutoToolTip extends JLabel{

	@Serial
	private static final long serialVersionUID = -3850625229732307986L;


	private static final String TAG_HTML_OPEN = "<html>";
	private static final String TAG_HTML_CLOSE = "</html>";
	private static final String TAG_BR = "<br>";


	public LabelAutoToolTip(){
		super();
	}


	public LabelAutoToolTip(final String text){
		super(text);
	}


	/**
	 * Returns the tooltip text only if the current text is clipped.
	 * The comparison is done using font metrics and the current component width.
	 *
	 * @return the full text if clipped, otherwise {@code null}
	 */
	@Override
	public String getToolTipText(){
		final String text = getText();
		if(StringUtils.isEmpty(text))
			return null;

		// Check if the text is clipped
		final FontMetrics fm = getFontMetrics(getFont());
		final int textWidth = fm.stringWidth(text);
		if(textWidth <= getWidth())
			// text fits, no tooltip needed
			return null;

		if(text.toLowerCase().startsWith(TAG_HTML_OPEN))
			return text;

		// Convert newlines to <br> and wrap in HTML
		final String htmlText = text.replace(StringUtils.LF, TAG_BR)
			.replace(StringUtils.CR, StringUtils.EMPTY);
		return TAG_HTML_OPEN + htmlText + TAG_HTML_CLOSE;
	}

	/**
	 * Creates a custom tooltip with a system info background.
	 *
	 * @return a new JToolTip
	 */
	@Override
	public JToolTip createToolTip(){
		final JToolTip tip = new JToolTip();
		tip.setBackground(SystemColor.info);
		tip.setComponent(this);
		return tip;
	}

	/**
	 * Forces a tooltip refresh. This is a no‑op because the tooltip is
	 * computed on‑the‑fly. Calling this method may be useful to trigger
	 * a repaint if needed.
	 */
	public void refreshToolTip(){
		// The tooltip is computed dynamically; no action needed.
		// Optionally, repaint to force the tooltip manager to update.
		repaint();
	}

}
