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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.io.Serial;


/**
 * Thin horizontal bar used to toggle the visibility of a collapsible
 * section at the bottom of a view.
 * <p>
 * The bar displays a triangle arrow (right when the section is collapsed,
 * down when expanded) followed by a text label. Clicking anywhere on the
 * bar notifies the installed listener. The bar is independent from the
 * section it controls: it only tracks its own visual state, and the
 * caller is responsible for calling {@link #setExpanded(boolean)}
 * whenever the section is toggled by other means (for example via a
 * keyboard shortcut).
 * <p>
 * The arrow is drawn with {@code Path2D} rather than with a Unicode
 * triangle glyph, because the geometric-shape characters are not present
 * in every platform font and would render as replacement glyphs on some
 * systems.
 */
public final class CollapsibleBar extends JPanel{

	@Serial
	private static final long serialVersionUID = -7810294758192847103L;


	/** Preferred height of the bar, in pixels. */
	private static final int BAR_HEIGHT = 20;
	/** Side length of the triangle arrow, in pixels. */
	private static final int ARROW_SIZE = 8;
	/** Left margin of the arrow, in pixels. */
	private static final int ARROW_LEFT = 10;
	/** Horizontal gap between the arrow and the label, in pixels. */
	private static final int ARROW_LABEL_GAP = 6;

	private static final Color BG = new Color(238, 234, 226);
	private static final Color BG_HOVER = new Color(230, 225, 214);
	private static final Color BORDER = new Color(210, 205, 195);
	private static final Color TEXT = new Color(70, 60, 40);
	private static final Font FONT = new Font("Tahoma", Font.PLAIN, 11);


	/**
	 * Listener notified whenever the user clicks the bar.
	 */
	public interface Listener{
		/**
		 * Called when the user clicks the bar. The listener is
		 * responsible for toggling the controlled section and for
		 * calling {@link CollapsibleBar#setExpanded(boolean)} to keep
		 * the arrow in sync.
		 */
		void onToggleRequested();
	}


	private final String label;

	private Listener listener;
	private boolean expanded;
	private boolean hovered;


	/**
	 * Constructor.
	 *
	 * @param label the text shown next to the arrow (must not be
	 *              {@code null}; may be empty)
	 */
	public CollapsibleBar(final String label){
		if(label == null)
			throw new IllegalArgumentException("Label must not be null");

		this.label = label;

		setPreferredSize(new Dimension(0, BAR_HEIGHT));
		setMinimumSize(new Dimension(0, BAR_HEIGHT));
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		setOpaque(false);

		addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(listener != null)
					listener.onToggleRequested();
			}

			@Override
			public void mouseEntered(final MouseEvent e){
				hovered = true;

				repaint();
			}

			@Override
			public void mouseExited(final MouseEvent e){
				hovered = false;

				repaint();
			}
		});
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	/**
	 * Installs the listener that is notified when the user clicks the
	 * bar.
	 *
	 * @param listener the listener, or {@code null} to remove any
	 *                 previously installed listener
	 * @return this bar, for chaining
	 */
	public CollapsibleBar withListener(final Listener listener){
		this.listener = listener;

		return this;
	}

	/**
	 * Sets the visual state of the arrow.
	 * <p>
	 * This method must be called by the caller whenever the controlled
	 * section is toggled by other means (keyboard shortcut, programmatic
	 * action), so that the arrow reflects the actual state of the
	 * section. It is not called automatically when the user clicks the
	 * bar, because the caller is expected to control the section and
	 * then sync the bar.
	 *
	 * @param expanded {@code true} to show the arrow pointing down
	 *                 (expanded), {@code false} to show the arrow
	 *                 pointing right (collapsed)
	 */
	public void setExpanded(final boolean expanded){
		if(this.expanded != expanded){
			this.expanded = expanded;

			repaint();
		}
	}

	/**
	 * Returns whether the bar is currently showing the expanded state.
	 *
	 * @return {@code true} if the arrow points down
	 */
	public boolean isExpanded(){
		return expanded;
	}


	/* ======================================================================
	 *                          Painting
	 * ====================================================================== */

	@Override
	protected void paintComponent(final Graphics g){
		super.paintComponent(g);

		if(!(g instanceof Graphics2D g2))
			return;

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Background
		g2.setColor(hovered? BG_HOVER: BG);
		g2.fillRect(0, 0, getWidth(), getHeight());

		// Top separator
		g2.setColor(BORDER);
		g2.drawLine(0, 0, getWidth(), 0);

		// Arrow
		final int arrowY = (getHeight() - ARROW_SIZE) / 2;
		drawArrow(g2, ARROW_LEFT, arrowY, ARROW_SIZE, expanded);

		// Label
		g2.setFont(FONT);
		g2.setColor(TEXT);
		final FontMetrics fm = g2.getFontMetrics();
		g2.drawString(label, ARROW_LEFT + ARROW_SIZE + ARROW_LABEL_GAP,
			(getHeight() + fm.getAscent()) / 2 - 2);
	}

	/**
	 * Draws a solid triangle arrow.
	 * <p>
	 * When {@code expanded} is {@code true} the triangle points down;
	 * otherwise it points right.
	 */
	private static void drawArrow(final Graphics2D g2, final int x, final int y, final int size,
			final boolean expanded){
		final Path2D.Double path = new Path2D.Double();
		if(expanded){
			// Downward triangle: top-left, top-right, bottom-center
			path.moveTo(x, y);
			path.lineTo(x + size, y);
			path.lineTo(x + size / 2., y + size);
		}
		else{
			// Rightward triangle: top-left, bottom-left, right-center
			path.moveTo(x, y);
			path.lineTo(x, y + size);
			path.lineTo(x + size, y + size / 2.);
		}
		path.closePath();

		g2.setColor(TEXT);
		g2.fill(path);
	}

}
