/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.ui.utilities;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.Objects;
import java.util.function.Consumer;


class TagComponent extends JComponent{

	@Serial
	private static final long serialVersionUID = -7410352884175789897L;

	private static final Color COLOR_TEXT = new Color(85, 85, 85);
	private static final Color COLOR_CLOSE = new Color(119, 119, 119);
	private static final Color COLOR_BACKGROUND = new Color(222, 231, 247);
	private static final Color COLOR_BORDER = new Color(202, 216, 242);

	private static final Border CLOSE_BORDER = BorderFactory.createLineBorder(COLOR_CLOSE, 1);
	private static final Border EMPTY_CLOSE_BORDER = BorderFactory.createLineBorder(COLOR_BACKGROUND, 1);

	private static final int PAD = 3;
	private static final int BORDER_THICKNESS = 1;

	private static final String TEXT_CROSS_MARK = "❌";

	/** Values for horizontal and vertical radius of corner arcs. */
	private static final Dimension CORNER_RADIUS = new Dimension(5, 5);


	TagComponent(final String text, final Consumer<TagComponent> tagRemover){
		Objects.requireNonNull(tagRemover, "Tag remover cannot be null");

		setOpaque(false);

		final JLabel textLabel = new JLabel(text.trim());
		textLabel.setForeground(COLOR_TEXT);
		Dimension ps = textLabel.getPreferredSize();
		final Dimension textLabelSize = new Dimension(ps.width + PAD * 2, ps.height + PAD * 4);
		textLabel.setPreferredSize(textLabelSize);
		textLabel.setHorizontalAlignment(SwingConstants.CENTER);

		final JLabel closeLabel = new JLabel(TEXT_CROSS_MARK);
		final Font closeFont = closeLabel.getFont();
		closeLabel.setFont(closeFont.deriveFont(closeFont.getSize() * 3.f / 4.f));
		closeLabel.setForeground(COLOR_CLOSE);
		closeLabel.setBorder(EMPTY_CLOSE_BORDER);
		closeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		closeLabel.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent evt){
				tagRemover.accept(TagComponent.this);
			}

			@Override
			public void mouseEntered(final MouseEvent evt){
				closeLabel.setBorder(CLOSE_BORDER);
			}

			@Override
			public void mouseExited(final MouseEvent evt){
				closeLabel.setBorder(EMPTY_CLOSE_BORDER);
			}
		});
		final JPanel closePanel = new JPanel();
		closePanel.setOpaque(false);
		ps = closeLabel.getPreferredSize();
		final Dimension closePanelSize = new Dimension(ps.width + PAD * 2, ps.height + PAD * 4);
		closePanel.setPreferredSize(closePanelSize);
		closePanel.setLayout(new MigLayout("insets 0,align center center"));
		closePanel.add(closeLabel);

		setLayout(new BorderLayout());
		add(textLabel, BorderLayout.WEST);
		add(closePanel, BorderLayout.EAST);
	}

	@Override
	protected void paintComponent(final Graphics g){
		super.paintComponent(g);

		if(g instanceof Graphics2D){
			final Graphics2D graphics2D = (Graphics2D)g.create();
			graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

			final int width = getWidth() - 1;
			final int height = getHeight() - PAD * 2 - 1;

			graphics2D.setColor(COLOR_BACKGROUND);
			graphics2D.fillRoundRect(0, PAD, width, height, CORNER_RADIUS.width, CORNER_RADIUS.height);
			graphics2D.setColor(COLOR_BORDER);
			graphics2D.setStroke(new BasicStroke(BORDER_THICKNESS));
			graphics2D.drawRoundRect(0, PAD, width, height, CORNER_RADIUS.width, CORNER_RADIUS.height);

			graphics2D.dispose();
		}
	}

	public String getTag(){
		return ((JLabel)getComponent(0)).getText();
	}

}
