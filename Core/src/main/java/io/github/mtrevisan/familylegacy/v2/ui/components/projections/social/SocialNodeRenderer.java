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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.social;

import org.apache.commons.lang3.StringUtils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;


/**
 * Draws the node cards of the Social Network views.
 * <p>
 * A node card is a rounded rectangle tinted with the node's primary
 * category, showing the entity name and — when the node is not the focus —
 * a small badge with its degree of separation from the focus.
 * <p>
 * The focus node uses a larger card with a stronger border. The selected
 * node uses a highlight border. The two states are independent, so the
 * focus can also be selected without visual conflicts.
 */
public final class SocialNodeRenderer{

	/** Corner radius of the node card. */
	private static final int ARC = 10;

	/** Inner padding between the card border and the text. */
	private static final int PADDING = 8;

	/** Diameter of the degree badge. */
	private static final int BADGE_DIAMETER = 18;

	/** Font used for the node label. */
	private static final Font FONT_NAME = new Font("Tahoma", Font.BOLD, 12);

	/** Font used for the focus node label. */
	private static final Font FONT_FOCUS = new Font("Tahoma", Font.BOLD, 14);

	/** Font used for the degree badge. */
	private static final Font FONT_BADGE = new Font("Tahoma", Font.BOLD, 10);

	/** Suffix appended to a truncated label. */
	private static final String TRUNCATION_SUFFIX = "…";

	/** Text color on every node card. */
	private static final Color TEXT_COLOR = new Color(30, 30, 30);

	/** Text color on the focus node card. */
	private static final Color TEXT_FOCUS_COLOR = new Color(10, 10, 10);

	/** Border color of a regular node card. */
	private static final Color BORDER_REGULAR = new Color(90, 90, 90, 180);

	/** Border color of the focus node card. */
	private static final Color BORDER_FOCUS = new Color(20, 20, 20, 220);

	/** Border color of a selected node card. */
	private static final Color BORDER_SELECTED = new Color(220, 100, 60);

	/** Thickness of the selection border. */
	private static final float SELECTED_BORDER_THICKNESS = 2.5f;

	/** Background color of the degree badge. */
	private static final Color BADGE_BACKGROUND = new Color(240, 240, 240, 220);

	/** Text color of the degree badge. */
	private static final Color BADGE_TEXT = new Color(60, 60, 60);


	private SocialNodeRenderer(){
	}


	/**
	 * Draws a single node card.
	 *
	 * @param g        the graphics context
	 * @param node     the node to draw
	 * @param bounds   the bounding box of the node
	 * @param selected whether the node is currently selected
	 */
	public static void drawNode(final Graphics2D g, final SocialNodeRef node, final Rectangle bounds,
		final boolean selected){
		if(g == null || node == null || bounds == null)
			return;

		final boolean focus = node.isCenter();
		final Color baseColor = colorForCategory(node.primaryCategory());
		final Color backgroundColor = tint(baseColor, 0.85f);
		final Color borderColor = (selected
			? BORDER_SELECTED
			: focus? BORDER_FOCUS: BORDER_REGULAR);
		final Color textColor = (focus? TEXT_FOCUS_COLOR: TEXT_COLOR);

		// Card body.
		final RoundRectangle2D shape = new RoundRectangle2D.Double(
			bounds.x, bounds.y, bounds.width, bounds.height, ARC, ARC);
		g.setColor(backgroundColor);
		g.fill(shape);

		// Left accent bar in the category color.
		g.setColor(baseColor);
		g.fillRoundRect(bounds.x, bounds.y, 5, bounds.height, ARC, ARC);

		// Border.
		g.setColor(borderColor);
		g.setStroke(new BasicStroke(selected? SELECTED_BORDER_THICKNESS: (focus? 1.8f: 1f)));
		g.draw(shape);

		// Label.
		g.setFont(focus? FONT_FOCUS: FONT_NAME);
		g.setColor(textColor);
		final FontMetrics fm = g.getFontMetrics();
		final int textLeft = bounds.x + PADDING + 5;
		final int textRight = bounds.x + bounds.width - PADDING;
		final int maxTextWidth = textRight - textLeft;
		final String label = truncate(fm, node.entity()
			.displayLabel(), maxTextWidth);
		final int labelY = bounds.y + (bounds.height + fm.getAscent() - fm.getDescent()) / 2;
		g.drawString(label, textLeft, labelY);

		// Degree badge (only for non-focus nodes).
		if(!focus && node.degree() > 0)
			drawDegreeBadge(g, bounds, node.degree());
	}


	/* ======================================================================
	 *                          Category palette
	 * ====================================================================== */

	/**
	 * Returns the base color associated with the given category. The same
	 * palette is used by {@link SocialEdgeRenderer} so that nodes and their
	 * incident edges share the same visual identity.
	 *
	 * @param category the category (must not be {@code null})
	 * @return the base color
	 */
	public static Color colorForCategory(final SocialRelationCategory category){
		return switch(category){
			case FAMILY -> new Color(199, 123, 158);
			case RELIGIOUS -> new Color(74, 120, 184);
			case PROFESSIONAL -> new Color(78, 158, 92);
			case LEGAL -> new Color(90, 90, 90);
			case COMMUNITY -> new Color(210, 138, 68);
			case POLITICAL -> new Color(180, 74, 74);
			case OTHER -> new Color(156, 156, 156);
		};
	}


	/* ======================================================================
	 *                          Helpers
	 * ====================================================================== */

	private static void drawDegreeBadge(final Graphics2D g, final Rectangle bounds, final int degree){
		final int cx = bounds.x + bounds.width - BADGE_DIAMETER / 2 - 4;
		final int cy = bounds.y + BADGE_DIAMETER / 2 + 4;
		final Ellipse2D badge = new Ellipse2D.Double(
			cx - BADGE_DIAMETER / 2., cy - BADGE_DIAMETER / 2.,
			BADGE_DIAMETER, BADGE_DIAMETER);

		g.setColor(BADGE_BACKGROUND);
		g.fill(badge);
		g.setColor(BORDER_REGULAR);
		g.setStroke(new BasicStroke(1f));
		g.draw(badge);

		g.setFont(FONT_BADGE);
		g.setColor(BADGE_TEXT);
		final FontMetrics fm = g.getFontMetrics();
		final String text = Integer.toString(degree);
		final int textWidth = fm.stringWidth(text);
		g.drawString(text, cx - textWidth / 2, cy + fm.getAscent() / 2 - 1);
	}

	private static String truncate(final FontMetrics fm, final String text, final int maxWidth){
		if(text == null)
			return StringUtils.EMPTY;
		if(fm.stringWidth(text) <= maxWidth)
			return text;
		final int suffixWidth = fm.stringWidth(TRUNCATION_SUFFIX);
		int end = text.length();
		while(end > 0 && fm.stringWidth(text.substring(0, end)) + suffixWidth > maxWidth)
			end--;
		return (end > 0? text.substring(0, end) + TRUNCATION_SUFFIX: TRUNCATION_SUFFIX);
	}

	/**
	 * Returns a lighter tint of the given color, usable as a card
	 * background. The amount is the fraction of white mixed in (0 = no
	 * mixing, 1 = fully white).
	 */
	private static Color tint(final Color color, final float amount){
		final float clamped = Math.max(0f, Math.min(1f, amount));
		final int r = (int)(color.getRed() + (255 - color.getRed()) * clamped);
		final int g = (int)(color.getGreen() + (255 - color.getGreen()) * clamped);
		final int b = (int)(color.getBlue() + (255 - color.getBlue()) * clamped);
		return new Color(r, g, b, 230);
	}

}
