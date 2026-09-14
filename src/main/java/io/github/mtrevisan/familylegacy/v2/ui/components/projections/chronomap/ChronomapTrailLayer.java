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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.chronomap;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.chronomap.ChronomapIndex.GeoAnchor;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.chronomap.ChronomapIndex.GeoCoordinate;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


/**
 * Trail layer.
 * <p>
 * For every trailed owner, draws a polyline through the positions of
 * the anchors that precede the current time, with a small dot at each
 * waypoint, plus the interpolated current position. The trail reads as
 * the movement of the person across the map: where they were, in
 * chronological order, up to the present moment.
 * <p>
 * The layer shares the event type filter with the marker layer, so
 * disabling an event type hides both its marker and its contribution to
 * the trail.
 */
public final class ChronomapTrailLayer implements ChronomapLayer{

	private static final int WAYPOINT_RADIUS = 3;
	private static final int CURRENT_RADIUS = 5;

	private static final Color CURRENT_BORDER = new Color(20, 20, 20);
	private static final Color LABEL_COLOR = new Color(30, 30, 30);
	private static final Color LABEL_SHADOW = new Color(255, 255, 255, 210);


	private final FLEFModel model;
	private final ChronomapIndex index;

	private final Set<String> trailIds = new LinkedHashSet<>();
	private Set<String> enabledEventTypes;
	private double currentTime;
	private boolean visible = true;


	public ChronomapTrailLayer(final FLEFModel model, final ChronomapIndex index){
		this.model = model;
		this.index = index;
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	public void setTrailIndividuals(final List<String> ids){
		trailIds.clear();
		if(ids != null)
			trailIds.addAll(ids);
	}

	public void setCurrentTime(final double jdn){
		this.currentTime = jdn;
	}

	/** {@code null} means "all event types enabled". */
	public void setEnabledEventTypes(final Set<String> types){
		this.enabledEventTypes = (types != null? Set.copyOf(types): null);
	}

	@Override
	public String getName(){
		return "Trails";
	}

	@Override
	public boolean isVisible(){
		return visible;
	}

	@Override
	public void setVisible(final boolean visible){
		this.visible = visible;
	}


	/* ======================================================================
	 *                          Painting
	 * ====================================================================== */

	@Override
	public void paint(final Graphics2D g, final JXMapViewer map, final int w, final int h){
		if(trailIds.isEmpty())
			return;

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		for(final String id : trailIds){
			final List<GeoAnchor> filtered = filterAnchors(index.anchorsOf(id));
			if(filtered.isEmpty())
				continue;

			final List<GeoCoordinate> path = buildPath(filtered);
			if(path.size() < 2)
				continue;

			final Color base = colorFor(id);
			drawTrail(g, map, path, base);
			drawWaypoints(g, map, path, base);
			drawLabel(g, map, path.get(path.size() - 1), labelFor(id));
		}
	}

	private List<GeoAnchor> filterAnchors(final List<GeoAnchor> anchors){
		if(enabledEventTypes == null)
			return anchors;

		final List<GeoAnchor> result = new ArrayList<>(anchors.size());
		for(final GeoAnchor a : anchors)
			if(isEventEnabled(a))
				result.add(a);
		return result;
	}

	private boolean isEventEnabled(final GeoAnchor a){
		if(enabledEventTypes == null)
			return true;
		final String kind = a.kind();
		if(!kind.startsWith("event:"))
			return true;
		return enabledEventTypes.contains(kind.substring("event:".length()));
	}

	/**
	 * Builds the ordered list of positions of the trail, up to the
	 * current time. Attributes with an open start date
	 * ({@code Long.MIN_VALUE}) are skipped as waypoints, because they
	 * have no defined start on the timeline, but still contribute to
	 * the interpolation.
	 */
	private List<GeoCoordinate> buildPath(final List<GeoAnchor> anchors){
		final List<GeoCoordinate> path = new ArrayList<>(anchors.size() + 1);
		for(final GeoAnchor a : anchors)
			if(a.startJdn() <= currentTime && a.startJdn() != Long.MIN_VALUE)
				path.add(a.position());

		final GeoCoordinate current = ChronomapOverlayPainter.interpolate(anchors, currentTime);
		if(current != null)
			path.add(current);

		return path;
	}

	private void drawTrail(final Graphics2D g, final JXMapViewer map,
		final List<GeoCoordinate> path, final Color base){
		final Path2D.Double line = new Path2D.Double();
		boolean first = true;
		for(final GeoCoordinate p : path){
			final Point2D pt = map.convertGeoPositionToPoint(
				new GeoPosition(p.latitude(), p.longitude()));
			if(first){
				line.moveTo(pt.getX(), pt.getY());
				first = false;
			}
			else
				line.lineTo(pt.getX(), pt.getY());
		}

		g.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 150));
		g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.draw(line);
	}

	private void drawWaypoints(final Graphics2D g, final JXMapViewer map,
		final List<GeoCoordinate> path, final Color base){
		// All points except the last are waypoints; the last is the
		// current position and receives a slightly larger dot.
		for(int i = 0; i < path.size() - 1; i++){
			final GeoCoordinate p = path.get(i);
			final Point2D pt = map.convertGeoPositionToPoint(
				new GeoPosition(p.latitude(), p.longitude()));
			g.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 200));
			g.fill(new Ellipse2D.Double(pt.getX() - WAYPOINT_RADIUS, pt.getY() - WAYPOINT_RADIUS,
				WAYPOINT_RADIUS * 2, WAYPOINT_RADIUS * 2));
		}

		final GeoCoordinate last = path.get(path.size() - 1);
		final Point2D pt = map.convertGeoPositionToPoint(
			new GeoPosition(last.latitude(), last.longitude()));
		g.setColor(base);
		g.fill(new Ellipse2D.Double(pt.getX() - CURRENT_RADIUS, pt.getY() - CURRENT_RADIUS,
			CURRENT_RADIUS * 2, CURRENT_RADIUS * 2));
		g.setColor(CURRENT_BORDER);
		g.draw(new Ellipse2D.Double(pt.getX() - CURRENT_RADIUS, pt.getY() - CURRENT_RADIUS,
			CURRENT_RADIUS * 2, CURRENT_RADIUS * 2));
	}

	private void drawLabel(final Graphics2D g, final JXMapViewer map,
		final GeoCoordinate pos, final String label){
		if(label == null || label.isEmpty())
			return;

		final Point2D pt = map.convertGeoPositionToPoint(
			new GeoPosition(pos.latitude(), pos.longitude()));
		g.setFont(g.getFont().deriveFont(11f));
		final FontMetrics fm = g.getFontMetrics();
		final int tx = (int)pt.getX() + CURRENT_RADIUS + 3;
		final int ty = (int)pt.getY() + fm.getAscent() / 2 - 1;
		g.setColor(LABEL_SHADOW);
		g.drawString(label, tx + 1, ty + 1);
		g.setColor(LABEL_COLOR);
		g.drawString(label, tx, ty);
	}


	/* ======================================================================
	 *                          Helpers
	 * ====================================================================== */

	private Color colorFor(final String id){
		final int h = Math.abs(id.hashCode());
		return Color.getHSBColor((h % 360) / 360f, 0.70f, 0.75f);
	}

	private String labelFor(final String id){
		try{
			return IndividualHandler.getInstance()
				.getDisplayText(model.getRecordById(id), model);
		}
		catch(final RuntimeException ignored){
			return id;
		}
	}

}
