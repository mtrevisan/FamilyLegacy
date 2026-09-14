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
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceRelationshipHandler;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;


/**
 * Place hierarchy layer.
 * <p>
 * FLEF models the administrative, ecclesiastical, judicial and cadastral
 * relationships between places through {@code place_relationship}
 * records. This layer draws each relationship as a connector line
 * between the coordinates of the subject and the target place.
 * <p>
 * The result is a visual sketch of the administrative hierarchy: a
 * village hangs from its municipality, the municipality from its
 * province, the province from its region. FLEF does not carry polygon
 * data, so true boundary lines cannot be rendered without an external
 * geodata source; this layer is the closest approximation that the
 * protocol supports out of the box.
 */
public final class ChronomapPlaceHierarchyLayer implements ChronomapLayer{

	private static final Color LINE_COLOR = new Color(120, 90, 60, 160);
	private static final Color NODE_COLOR = new Color(120, 90, 60, 220);
	private static final Color NODE_BORDER = new Color(60, 40, 20, 220);
	private static final float[] DASH = {6f, 4f};
	private static final int NODE_RADIUS = 3;

	private static final String TAG_TYPE = "type";
	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_TARGET = "target";


	private final FLEFModel model;
	private final PlaceCoordinateResolver resolver;

	private boolean visible;
	private List<Edge> edges;


	public ChronomapPlaceHierarchyLayer(final FLEFModel model, final PlaceCoordinateResolver resolver){
		this.model = model;
		this.resolver = resolver;
		this.edges = computeEdges();
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	public void rebuild(){
		edges = computeEdges();
	}

	/** Enables the layer and materializes its edges for the first time. */
	public void setVisibleAndRebuild(final boolean visible){
		this.visible = visible;
		if(visible)
			edges = computeEdges();
	}

	@Override
	public String getName(){
		return "Place hierarchy";
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
		if(edges.isEmpty())
			return;

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0f, DASH, 0f));

		for(final Edge e : edges){
			final Point2D p1 = map.convertGeoPositionToPoint(
				new GeoPosition(e.subject().latitude(), e.subject().longitude()));
			final Point2D p2 = map.convertGeoPositionToPoint(
				new GeoPosition(e.target().latitude(), e.target().longitude()));

			g.setColor(LINE_COLOR);
			g.drawLine((int)p1.getX(), (int)p1.getY(), (int)p2.getX(), (int)p2.getY());

			g.setColor(NODE_COLOR);
			g.fill(new Ellipse2D.Double(p1.getX() - NODE_RADIUS, p1.getY() - NODE_RADIUS,
				NODE_RADIUS * 2, NODE_RADIUS * 2));
			g.setColor(NODE_BORDER);
			g.draw(new Ellipse2D.Double(p1.getX() - NODE_RADIUS, p1.getY() - NODE_RADIUS,
				NODE_RADIUS * 2, NODE_RADIUS * 2));
		}
	}


	/* ======================================================================
	 *                          Edge computation
	 * ====================================================================== */

	private List<Edge> computeEdges(){
		final List<Edge> result = new ArrayList<>();
		if(resolver == null)
			return result;

		for(final FLEFRecord rel : model.getRecordsByType(PlaceRelationshipHandler.TYPE)){
			final String subjectId = extractPlaceRef(rel, TAG_SUBJECT);
			final String targetId = extractPlaceRef(rel, TAG_TARGET);
			if(subjectId == null || targetId == null)
				continue;

			final PlaceCoordinateResolver.Resolved s = resolver.resolve(subjectId);
			final PlaceCoordinateResolver.Resolved t = resolver.resolve(targetId);
			if(s == null || t == null)
				continue;

			// Skip degenerate edges: same coordinates, likely the result
			// of an inherited parent position.
			if(s.coordinate().latitude() == t.coordinate().latitude()
				&& s.coordinate().longitude() == t.coordinate().longitude())
				continue;

			final String type = FLEFRecordHelper.getChildValue(rel, TAG_TYPE);
			result.add(new Edge(s.coordinate(), t.coordinate(), (type != null? type: "")));
		}
		return result;
	}

	private static String extractPlaceRef(final FLEFRecord rel, final String fieldTag){
		final FLEFRecord field = FLEFRecordHelper.findChild(rel, fieldTag);
		if(field == null)
			return null;

		final FLEFRecord placeRef = FLEFRecordHelper.findChild(field, PlaceHandler.TYPE);
		if(placeRef != null && placeRef.getValue() != null)
			return placeRef.getValue();

		final FLEFRecord inner = field.getTheOnlyChild();
		if(inner != null){
			final FLEFRecord placeInner = FLEFRecordHelper.findChild(inner, PlaceHandler.TYPE);
			if(placeInner != null && placeInner.getValue() != null)
				return placeInner.getValue();
		}
		return null;
	}


	private record Edge(ChronomapIndex.GeoCoordinate subject, ChronomapIndex.GeoCoordinate target, String type){}

}
