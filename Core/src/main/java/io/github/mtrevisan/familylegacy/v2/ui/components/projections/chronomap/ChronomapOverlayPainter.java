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

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * Marker layer.
 * <p>
 * For every visible owner, the painter interpolates the position between
 * the anchors around the current time. An anchor is a position valid
 * over an interval: an event is an anchor of zero duration, an attribute
 * with {@code valid_from} / {@code valid_to} is an anchor with a real
 * duration. While the current time is inside an attribute, the marker
 * stays still at that position; between two anchors, the marker moves
 * along a great-circle arc.
 * <p>
 * Events can be filtered by type: when a filter is set, only the event
 * anchors whose type is in the filter contribute to the interpolation
 * and are drawn. Attributes are never filtered.
 */
public final class ChronomapOverlayPainter implements ChronomapLayer{

	private static final Color MARKER_BORDER = new Color(20, 20, 20);
	private static final Color LABEL_COLOR = new Color(30, 30, 30);
	private static final Color LABEL_SHADOW = new Color(255, 255, 255, 210);
	private static final int MARKER_RADIUS = 6;

	private final FLEFModel model;
	private final ChronomapIndex index;

	private final List<String> visibleIds = new ArrayList<>();
	private double currentTime;
	private Set<String> enabledEventTypes;
	private boolean visible = true;


	public ChronomapOverlayPainter(final FLEFModel model, final ChronomapIndex index){
		this.model = model;
		this.index = index;
	}

	public void setVisibleIndividuals(final List<String> ids){
		visibleIds.clear();
		if(ids != null)
			visibleIds.addAll(ids);
	}

	public void setCurrentTime(final double jdn){
		this.currentTime = jdn;
	}

	/** {@code null} means "all event types enabled". */
	public void setEnabledEventTypes(final Set<String> types){
		this.enabledEventTypes = (types != null? Set.copyOf(types): null);
	}

	public List<String> getVisibleIndividuals(){
		return visibleIds;
	}

	@Override
	public String getName(){
		return "Markers";
	}

	@Override
	public boolean isVisible(){
		return visible;
	}

	@Override
	public void setVisible(final boolean visible){
		this.visible = visible;
	}


	@Override
	public void paint(final Graphics2D g, final JXMapViewer map, final int w, final int h){
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		for(final String id : visibleIds){
			final List<GeoAnchor> anchors = filter(index.anchorsOf(id));
			if(anchors.isEmpty())
				continue;

			final GeoCoordinate pos = interpolate(anchors, currentTime);
			if(pos == null)
				continue;

			final Point2D p = map.convertGeoPositionToPoint(
				new GeoPosition(pos.latitude(), pos.longitude()));
			drawMarker(g, (int)p.getX(), (int)p.getY(), colorFor(id), labelFor(id));
		}
	}

	private List<GeoAnchor> filter(final List<GeoAnchor> anchors){
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


	private void drawMarker(final Graphics2D g, final int x, final int y, final Color color, final String label){
		g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 70));
		g.fill(new Ellipse2D.Double(x - MARKER_RADIUS * 3, y - MARKER_RADIUS * 3,
			MARKER_RADIUS * 6, MARKER_RADIUS * 6));

		g.setColor(color);
		g.fill(new Ellipse2D.Double(x - MARKER_RADIUS, y - MARKER_RADIUS,
			MARKER_RADIUS * 2, MARKER_RADIUS * 2));
		g.setColor(MARKER_BORDER);
		g.draw(new Ellipse2D.Double(x - MARKER_RADIUS, y - MARKER_RADIUS,
			MARKER_RADIUS * 2, MARKER_RADIUS * 2));

		if(label != null && !label.isEmpty()){
			g.setFont(g.getFont().deriveFont(11f));
			final FontMetrics fm = g.getFontMetrics();
			final int tx = x + MARKER_RADIUS + 3;
			final int ty = y + fm.getAscent() / 2 - 1;
			g.setColor(LABEL_SHADOW);
			g.drawString(label, tx + 1, ty + 1);
			g.setColor(LABEL_COLOR);
			g.drawString(label, tx, ty);
		}
	}


	/* ======================================================================
	 *                          Interpolation
	 * ====================================================================== */

	/**
	 * Resolves the position of the marker at the given time.
	 * <p>
	 * Resolution order:
	 * <ol>
	 *   <li>an event happening exactly at {@code time};</li>
	 *   <li>an attribute whose validity interval contains {@code time};</li>
	 *   <li>interpolation along a great-circle arc between the previous
	 *       and the next anchor;</li>
	 *   <li>{@code null} when the time is outside the documented range.</li>
	 * </ol>
	 * Events take priority over attributes at the same instant.
	 */
	public static GeoCoordinate interpolate(final List<GeoAnchor> anchors, final double time){
		if(anchors.isEmpty())
			return null;

		// 1. Exact event match.
		for(final GeoAnchor a : anchors)
			if(a.startJdn() == a.endJdn() && a.startJdn() == time)
				return a.position();

		// 2. Inside an attribute interval.
		for(final GeoAnchor a : anchors)
			if(time >= a.startJdn() && time <= a.endJdn())
				return a.position();

		// 3. Closest anchors around the current time.
		GeoAnchor before = null;
		GeoAnchor after = null;
		for(final GeoAnchor a : anchors){
			if(a.endJdn() < time && (before == null || a.endJdn() > before.endJdn()))
				before = a;
			if(a.startJdn() > time && (after == null || a.startJdn() < after.startJdn()))
				after = a;
		}

		if(before == null || after == null)
			return null;

		final long gapStart = before.endJdn();
		final long gapEnd = after.startJdn();
		if(gapEnd <= gapStart)
			return before.position();

		double t = (time - gapStart) / (double)(gapEnd - gapStart);
		t = Math.clamp(t, 0., 1.);

		final GeoCoordinate p1 = before.position();
		final GeoCoordinate p2 = after.position();
		if(p1.latitude() == p2.latitude() && p1.longitude() == p2.longitude())
			return p1;

		final double[] latLon = slerp(p1.latitude(), p1.longitude(),
			p2.latitude(), p2.longitude(), t);
		return new GeoCoordinate(latLon[0], latLon[1]);
	}

	private static double[] slerp(final double lat1, final double lon1,
		final double lat2, final double lon2, final double t){
		final double phi1 = Math.toRadians(lat1), lam1 = Math.toRadians(lon1);
		final double phi2 = Math.toRadians(lat2), lam2 = Math.toRadians(lon2);

		final double x1 = Math.cos(phi1) * Math.cos(lam1);
		final double y1 = Math.cos(phi1) * Math.sin(lam1);
		final double z1 = Math.sin(phi1);
		final double x2 = Math.cos(phi2) * Math.cos(lam2);
		final double y2 = Math.cos(phi2) * Math.sin(lam2);
		final double z2 = Math.sin(phi2);

		double dot = x1 * x2 + y1 * y2 + z1 * z2;
		dot = Math.clamp(dot, -1., 1.);
		final double omega = Math.acos(dot);
		if(omega < 1e-9)
			return new double[]{lat1, lon1};

		final double sinOmega = Math.sin(omega);
		final double a = Math.sin((1. - t) * omega) / sinOmega;
		final double b = Math.sin(t * omega) / sinOmega;

		final double x = a * x1 + b * x2;
		final double y = a * y1 + b * y2;
		final double z = a * z1 + b * z2;
		final double lat = Math.toDegrees(Math.atan2(z, Math.sqrt(x * x + y * y)));
		final double lon = Math.toDegrees(Math.atan2(y, x));
		return new double[]{lat, lon};
	}


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
