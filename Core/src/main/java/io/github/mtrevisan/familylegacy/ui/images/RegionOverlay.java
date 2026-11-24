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
package io.github.mtrevisan.familylegacy.ui.images;

import io.github.mtrevisan.familylegacy.imaging.MediaRegions;
import io.github.mtrevisan.familylegacy.imaging.MediaRegions.Region;
import io.github.mtrevisan.familylegacy.util.math.AffineTransform;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;


/**
 * The regions of a media drawn over its image, with what the viewer does with them: which ones are under the mouse (the smallest first), the
 * text of the tooltip, the click.
 * <p>
 * A region is a rectangle with a thin white line over a dark one, so it can be seen on any photograph. The one under the mouse is lit. A region
 * that is the portrait of one of its people has a small bust in a circle at its top right corner; where the region is too small for it on the
 * screen the line is thicker instead (the tooltip tells it anyway).
 * </p>
 */
public final class RegionOverlay{

	/** A region to draw. */
	public record Mark(Region region, boolean portrait){}


	/** The side of the badge of a portrait, in pixels of the screen (not scaled with the zoom). */
	private static final int BADGE_SIZE = 18;
	private static final int BADGE_MARGIN = 2;
	/** Under this size on the screen (width or height) a region has no room for the badge. */
	private static final int BADGE_MIN_REGION = 24;

	private static final Color HALO_COLOR = new Color(0, 0, 0, 170);
	private static final Color LINE_COLOR = Color.WHITE;
	private static final Color HOVER_FILL = new Color(255, 214, 0, 60);
	private static final Color HOVER_LINE = new Color(255, 214, 0);
	private static final Color BADGE_COLOR = new Color(0, 110, 200);

	private static final Stroke HALO = new BasicStroke(3.f);
	private static final Stroke LINE = new BasicStroke(1.2f);
	private static final Stroke HALO_THICK = new BasicStroke(5.f);
	private static final Stroke LINE_THICK = new BasicStroke(2.6f);


	/** Largest first, so the smallest (the most specific) are drawn on top. */
	private volatile List<Mark> marks = List.of();
	private volatile Integer page;
	private volatile boolean visible = true;
	private volatile Region hovered;

	private volatile Function<List<Region>, String> tooltip;
	private volatile BiConsumer<List<Region>, Point> clickListener;


	/**
	 * @param marks the regions to draw
	 * @param page  the 1-based page shown ({@code null} for the media that has no pages): the regions of the other pages are not drawn
	 */
	public void setMarks(final List<Mark> marks, final Integer page){
		final int shown = (page != null? page: 1);
		final List<Mark> ordered = new ArrayList<>(marks.size());
		for(final Mark mark : marks)
			if(mark.region().effectivePage() == shown)
				ordered.add(mark);
		ordered.sort(Comparator.comparingLong((Mark mark) -> mark.region().areaSize()).reversed());

		this.marks = List.copyOf(ordered);
		this.page = page;
		hovered = null;
	}

	/** @return the marks that are drawn, largest first */
	public List<Mark> getMarks(){
		return marks;
	}

	public void setVisible(final boolean visible){
		this.visible = visible;
		hovered = null;
	}

	public boolean isVisible(){
		return visible;
	}

	/** @return whether there is something to hit: the regions are shown and there is at least one */
	public boolean isActive(){
		return (visible && !marks.isEmpty());
	}

	/** @param tooltip	the text of the tooltip for the regions under the mouse, smallest first ({@code null} for none) */
	public void setTooltip(final Function<List<Region>, String> tooltip){
		this.tooltip = tooltip;
	}

	/** @param clickListener	what to do with the regions under a click (smallest first) and the point of the click, in the pixels of the component */
	public void setClickListener(final BiConsumer<List<Region>, Point> clickListener){
		this.clickListener = clickListener;
	}

	public boolean hasTooltip(){
		return (tooltip != null);
	}

	public boolean hasClickListener(){
		return (clickListener != null);
	}


	/**
	 * @param x the horizontal position, in pixels of the image
	 * @param y the vertical position, in pixels of the image
	 * @return the regions under the point, smallest first (none if the regions are hidden)
	 */
	public List<Region> regionsAt(final double x, final double y){
		if(!isActive())
			return List.of();

		final List<Region> regions = new ArrayList<>(marks.size());
		for(final Mark mark : marks)
			regions.add(mark.region());
		return MediaRegions.at(regions, x, y, page);
	}

	/**
	 * Sets which regions the mouse is over.
	 *
	 * @return whether it changed (the image has to be painted again)
	 */
	public boolean hover(final List<Region> under){
		final Region next = (under.isEmpty()? null: under.getFirst());
		if(next == null? hovered == null: next.equals(hovered))
			return false;

		hovered = next;
		return true;
	}

	/** @return the text of the tooltip for the regions, {@code null} if there is none */
	public String tooltipFor(final List<Region> under){
		final Function<List<Region>, String> function = tooltip;
		return (function != null && !under.isEmpty()? function.apply(under): null);
	}

	/** Tells the listener that the regions were clicked. */
	public void click(final List<Region> under, final Point point){
		final BiConsumer<List<Region>, Point> listener = clickListener;
		if(listener != null && !under.isEmpty())
			listener.accept(under, point);
	}


	/**
	 * Draws the regions.
	 *
	 * @param g           the graphics of the component
	 * @param transform   the position and the scale of the image in the component
	 * @param imageWidth  the width of the image, in pixels
	 * @param imageHeight the height of the image, in pixels
	 */
	public void paint(final Graphics2D g, final AffineTransform transform, final int imageWidth, final int imageHeight){
		if(!isActive() || imageWidth <= 0 || imageHeight <= 0)
			return;

		final Stroke oldStroke = g.getStroke();
		final Region lit = hovered;
		for(final Mark mark : marks){
			final Region region = mark.region();
			final Rectangle pixels = region.area();
			final int x1 = transform.transformX(pixels.x);
			final int y1 = transform.transformY(pixels.y);
			final int width = transform.transformX(pixels.x + pixels.width) - x1;
			final int height = transform.transformY(pixels.y + pixels.height) - y1;
			final boolean room = (width >= BADGE_MIN_REGION && height >= BADGE_MIN_REGION);
			final boolean thick = (mark.portrait() && !room);
			final boolean isLit = region.equals(lit);

			if(isLit){
				g.setColor(HOVER_FILL);
				g.fillRect(x1, y1, width, height);
			}
			g.setStroke(thick? HALO_THICK: HALO);
			g.setColor(HALO_COLOR);
			g.drawRect(x1, y1, width, height);
			g.setStroke(thick? LINE_THICK: LINE);
			g.setColor(isLit? HOVER_LINE: LINE_COLOR);
			g.drawRect(x1, y1, width, height);

			if(mark.portrait() && room)
				paintBadge(g, x1 + width - BADGE_SIZE - BADGE_MARGIN, y1 + BADGE_MARGIN);
		}
		g.setStroke(oldStroke);
	}

	/** A bust (head and shoulders) in a circle. */
	private static void paintBadge(final Graphics2D g, final int x, final int y){
		g.setStroke(LINE);
		g.setColor(BADGE_COLOR);
		g.fillOval(x, y, BADGE_SIZE, BADGE_SIZE);
		g.setColor(LINE_COLOR);
		g.drawOval(x, y, BADGE_SIZE, BADGE_SIZE);

		final double centerX = x + BADGE_SIZE / 2.;
		final double head = BADGE_SIZE * .20;
		g.fill(new Ellipse2D.Double(centerX - head, y + BADGE_SIZE * .20, head * 2., head * 2.));
		final double shoulders = BADGE_SIZE * .34;
		g.fill(new Arc2D.Double(centerX - shoulders, y + BADGE_SIZE * .56, shoulders * 2., shoulders * 1.4, 0., 180., Arc2D.CHORD));
	}

}
