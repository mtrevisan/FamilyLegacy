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
package io.github.mtrevisan.familylegacy.ui.components.lists;

import io.github.mtrevisan.familylegacy.privacy.PrivacyLevel;
import io.github.mtrevisan.familylegacy.privacy.PrivacyResolver;
import org.apache.commons.lang3.StringUtils;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;


/**
 * A small padlock that marks a restricted (amber) or confidential (red) record; a filled lock when the restriction is
 * declared by the record, an open outline when it is inherited or presumed (a living person, a national id).
 */
public final class PrivacyBadge implements Icon{

	private static final int WIDTH = 10;
	private static final int HEIGHT = 12;
	private static final Color RESTRICTED_COLOR = new Color(0xD98E04);
	private static final Color CONFIDENTIAL_COLOR = new Color(0xC62828);


	private final PrivacyLevel level;
	private final boolean declared;


	private PrivacyBadge(final PrivacyLevel level, final boolean declared){
		this.level = level;
		this.declared = declared;
	}


	/**
	 * The badge for the privacy of a record.
	 *
	 * @param privacy	The effective privacy.
	 * @return	The badge, or {@code null} when there is no restriction to signal.
	 */
	public static PrivacyBadge of(final PrivacyResolver.Effective privacy){
		return (privacy != null && privacy.isRestricted()
			? new PrivacyBadge(privacy.level(), privacy.origin() == PrivacyResolver.Origin.DECLARED)
			: null);
	}

	/**
	 * Draws a ring around a marker of a restricted (amber) or confidential (red) record: the padlock is too big for a
	 * marker. Nothing is drawn when there is no restriction.
	 *
	 * @param g	The graphics.
	 * @param privacy	The effective privacy of the record the marker stands for.
	 * @param centerX	The x of the center of the marker.
	 * @param centerY	The y of the center of the marker.
	 * @param radius	The radius of the marker; the ring is drawn just outside it.
	 */
	public static void paintRing(final Graphics2D g, final PrivacyResolver.Effective privacy, final int centerX,
			final int centerY, final int radius){
		if(privacy == null || !privacy.isRestricted())
			return;

		final Graphics2D g2 = (Graphics2D)g.create();
		try{
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(privacy.level() == PrivacyLevel.CONFIDENTIAL? CONFIDENTIAL_COLOR: RESTRICTED_COLOR);
			g2.setStroke(new BasicStroke(2f));
			final int ringRadius = radius + 2;
			g2.drawOval(centerX - ringRadius, centerY - ringRadius, 2 * ringRadius, 2 * ringRadius);
		}
		finally{
			g2.dispose();
		}
	}

	/** The tooltip text describing the privacy, or {@code null} when there is no restriction. */
	public static String tooltip(final PrivacyResolver.Effective privacy){
		if(privacy == null || !privacy.isRestricted())
			return null;

		final StringBuilder sb = new StringBuilder(privacy.level().name()
			.toLowerCase(java.util.Locale.ROOT));
		switch(privacy.origin()){
			case INHERITED -> sb.append(" (inherited)");
			case LIVING_PERSON -> sb.append(" (may be living)");
			case NATIONAL_ID -> sb.append(" (national id)");
			default -> {}
		}
		if(StringUtils.isNotBlank(privacy.reason()))
			sb.append(", ")
				.append(privacy.reason());
		if(StringUtils.isNotBlank(privacy.expires()))
			sb.append(", until ")
				.append(privacy.expires());
		return sb.toString();
	}


	@Override
	public void paintIcon(final Component c, final Graphics g, final int x, final int y){
		final Graphics2D g2 = (Graphics2D)g.create();
		try{
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(level == PrivacyLevel.CONFIDENTIAL? CONFIDENTIAL_COLOR: RESTRICTED_COLOR);
			g2.setStroke(new BasicStroke(1.5f));

			//shackle
			g2.drawArc(x + 2, y, WIDTH - 4, 8, 0, 180);
			g2.drawLine(x + 2, y + 4, x + 2, y + 6);
			if(declared)
				g2.drawLine(x + WIDTH - 3, y + 4, x + WIDTH - 3, y + 6);

			//body
			if(declared)
				g2.fillRoundRect(x, y + 5, WIDTH, HEIGHT - 5, 2, 2);
			else
				g2.drawRoundRect(x, y + 5, WIDTH - 1, HEIGHT - 6, 2, 2);
		}
		finally{
			g2.dispose();
		}
	}

	@Override
	public int getIconWidth(){
		return WIDTH;
	}

	@Override
	public int getIconHeight(){
		return HEIGHT;
	}

}
