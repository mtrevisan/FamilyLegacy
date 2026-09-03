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

		final int availWidth = getWidth() - getInsets().left - getInsets().right;
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

		// 1. Construction ROW 1
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

		// 2. Construction ROW 2
		final StringBuilder line2 = new StringBuilder();
		while(i < words.length){
			line2.append(line2.isEmpty()? StringUtils.EMPTY: StringUtils.SPACE)
				.append(words[i]);

			i ++;
		}

		// 3. Truncate LINE 2 if it exceeds the available space
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
		return new Dimension(super.getPreferredSize().width, height);
	}

}