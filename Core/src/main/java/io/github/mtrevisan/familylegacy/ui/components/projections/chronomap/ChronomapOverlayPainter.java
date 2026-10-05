package io.github.mtrevisan.familylegacy.ui.components.projections.chronomap;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.chronomap.ChronomapIndex.GeoAnchor;
import io.github.mtrevisan.familylegacy.ui.components.projections.chronomap.ChronomapIndex.GeoCoordinate;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * Marker layer with uncertainty circles and dynamic directional indicators during transit.
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
	private boolean showUncertainty = true;

	public record InterpolatedPosition(
		GeoCoordinate coordinate,
		GeoCoordinate fromCoordinate,
		GeoCoordinate toCoordinate,
		Double screenHeading,
		boolean isMoving
	){
	}

	public ChronomapOverlayPainter(final FLEFModel model, final ChronomapIndex index){
		this.model = model;
		this.index = index;
	}

	public void setVisibleIndividuals(final List<String> ids){
		visibleIds.clear();
		if(ids != null){
			visibleIds.addAll(ids);
		}
	}

	public void setCurrentTime(final double jdn){
		this.currentTime = jdn;
	}

	public void setEnabledEventTypes(final Set<String> types){
		this.enabledEventTypes = (types != null ? Set.copyOf(types) : null);
	}

	public void setShowUncertainty(final boolean showUncertainty){
		this.showUncertainty = showUncertainty;
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

			final InterpolatedPosition state = interpolatePosition(map, anchors, currentTime);
			if(state == null)
				continue;

			final GeoCoordinate pos = state.coordinate();
			final Point2D p = map.convertGeoPositionToPoint(new GeoPosition(pos.latitude(), pos.longitude()));

			if(showUncertainty && pos.uncertainty() > 0.0)
				drawUncertaintyCircle(g, map, pos, p);

			if(state.isMoving()){
				// 1. Draw opacified orthodromic route from origin to destination
				drawGreatCircleRoute(g, map, state.fromCoordinate(), state.toCoordinate(), colorFor(id));

				// 2. Draw elongated ellipse marker rotated along 2D screen direction
				drawMovingMarker(g, (int)p.getX(), (int)p.getY(), state.screenHeading(), colorFor(id), labelFor(id));
			}
			else
				drawMarker(g, (int)p.getX(), (int)p.getY(), colorFor(id), labelFor(id));
		}
	}

	public static InterpolatedPosition interpolatePosition(final JXMapViewer map, final List<GeoAnchor> anchors, final double time){
		if(anchors.isEmpty())
			return null;

		// 1. Exact event match
		for(final GeoAnchor a : anchors)
			if(a.startJdn() == a.endJdn() && a.startJdn() == time)
				return new InterpolatedPosition(a.position(), null, null, null, false);

		// 2. Inside an attribute interval
		for(final GeoAnchor a : anchors)
			if(time >= a.startJdn() && time <= a.endJdn())
				return new InterpolatedPosition(a.position(), null, null, null, false);

		// 3. Transit between anchors
		GeoAnchor before = null;
		GeoAnchor after = null;
		for(final GeoAnchor a : anchors){
			if(a.endJdn() < time && (before == null || a.endJdn() > before.endJdn())){
				before = a;
			}
			if(a.startJdn() > time && (after == null || a.startJdn() < after.startJdn())){
				after = a;
			}
		}

		if(before == null || after == null){
			return null;
		}

		final long gapStart = before.endJdn();
		final long gapEnd = after.startJdn();
		if(gapEnd <= gapStart){
			return new InterpolatedPosition(before.position(), null, null, null, false);
		}

		double t = (time - gapStart) / (double)(gapEnd - gapStart);
		t = Math.clamp(t, 0., 1.);

		final GeoCoordinate p1 = before.position();
		final GeoCoordinate p2 = after.position();
		if(p1.latitude() == p2.latitude() && p1.longitude() == p2.longitude()){
			return new InterpolatedPosition(p1, null, null, null, false);
		}

		final double[] latLon = slerp(p1.latitude(), p1.longitude(), p2.latitude(), p2.longitude(), t);
		final int uncert = (int)Math.ceil(interpolate(p1.uncertainty(), p2.uncertainty(), t));
		final GeoCoordinate currentCoord = new GeoCoordinate(latLon[0], latLon[1], uncert);

		// Calculate 2D screen space heading angle
		Double screenHeading = null;
		if(map != null){
			final double deltaT = 0.005;
			final double tNext = Math.min(1.0, t + deltaT);
			final double[] latLonNext = slerp(p1.latitude(), p1.longitude(), p2.latitude(), p2.longitude(), tNext);

			final Point2D pCurrentScreen = map.convertGeoPositionToPoint(new GeoPosition(latLon[0], latLon[1]));
			final Point2D pNextScreen = map.convertGeoPositionToPoint(new GeoPosition(latLonNext[0], latLonNext[1]));

			final double dx = pNextScreen.getX() - pCurrentScreen.getX();
			final double dy = pNextScreen.getY() - pCurrentScreen.getY();
			screenHeading = Math.toDegrees(Math.atan2(dy, dx));
		}

		return new InterpolatedPosition(currentCoord, p1, p2, screenHeading, true);
	}

	private static void drawUncertaintyCircle(final Graphics2D g, final JXMapViewer map,
			final GeoCoordinate pos, final Point2D center){
		final double metersPerPixel = getMetersPerPixel(pos.latitude(), map.getZoom());
		final int pixelRadius = (int)Math.round(pos.uncertainty() / metersPerPixel);

		if(pixelRadius < 1){
			return;
		}

		final int x = (int)center.getX() - pixelRadius;
		final int y = (int)center.getY() - pixelRadius;
		final int diameter = pixelRadius * 2;

		g.setColor(new Color(255, 165, 0, 35));
		g.fillOval(x, y, diameter, diameter);

		g.setColor(new Color(230, 120, 0, 160));
		g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[]{4.0f, 4.0f}, 0.0f));
		g.drawOval(x, y, diameter, diameter);
		g.setStroke(new BasicStroke(1.0f));
	}

	private static double getMetersPerPixel(final double latitude, final int zoomLevel){
		final double earthCircumferenceMeters = 40_075_016.686;
		return (earthCircumferenceMeters * Math.cos(Math.toRadians(latitude))) / (256.0 * Math.pow(2, zoomLevel));
	}

	private void drawGreatCircleRoute(final Graphics2D g, final JXMapViewer map,
			final GeoCoordinate p1, final GeoCoordinate p2, final Color color){
		final Graphics2D g2 = (Graphics2D)g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 90));
		g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[]{5.0f, 5.0f}, 0.0f));

		final int steps = 30;
		Point2D prevPoint = null;
		for(int i = 0; i <= steps; i++){
			final double t = i / (double)steps;
			final double[] latLon = slerp(p1.latitude(), p1.longitude(), p2.latitude(), p2.longitude(), t);
			final Point2D currentPoint = map.convertGeoPositionToPoint(new GeoPosition(latLon[0], latLon[1]));

			if(prevPoint != null){
				g2.drawLine((int)prevPoint.getX(), (int)prevPoint.getY(), (int)currentPoint.getX(), (int)currentPoint.getY());
			}
			prevPoint = currentPoint;
		}

		g2.dispose();
	}

	private void drawMovingMarker(final Graphics2D g, final int x, final int y,
		final Double screenHeading, final Color color, final String label){
		final Graphics2D g2 = (Graphics2D)g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g2.translate(x, y);
		if(screenHeading != null){
			// Align coordinate frame: +X points in direction of motion
			g2.rotate(Math.toRadians(screenHeading));
		}

		// 1. Motion trail behind marker (-X axis)
		final int trailLength = MARKER_RADIUS * 4;
		final GradientPaint trailGradient = new GradientPaint(
			0, 0, new Color(color.getRed(), color.getGreen(), color.getBlue(), 140),
			-trailLength, 0, new Color(color.getRed(), color.getGreen(), color.getBlue(), 0)
		);
		g2.setPaint(trailGradient);
		final Polygon trail = new Polygon();
		trail.addPoint(0, -MARKER_RADIUS + 2);
		trail.addPoint(0, MARKER_RADIUS - 2);
		trail.addPoint(-trailLength, 0);
		g2.fill(trail);

		// 2. Elongated ellipse: major diameter along trajectory (X axis)
		final int height = MARKER_RADIUS * 2 - 2;  // Transverse diameter (Y axis)
		final int width = MARKER_RADIUS * 3 + 2;   // Trajectory diameter (X axis)
		final Ellipse2D.Double ellipse = new Ellipse2D.Double(-width / 2.0, -height / 2.0, width, height);

		g2.setColor(color);
		g2.fill(ellipse);

		// 3. Dashed border
		g2.setColor(MARKER_BORDER);
		g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{3.0f, 3.0f}, 0.0f));
		g2.draw(ellipse);

		g2.dispose();

		// 4. Unrotated label text
		if(label != null && !label.isEmpty()){
			g.setFont(g.getFont().deriveFont(11f));
			final FontMetrics fm = g.getFontMetrics();
			final int tx = x + MARKER_RADIUS + 4;
			final int ty = y + fm.getAscent() / 2 - 1;
			g.setColor(LABEL_SHADOW);
			g.drawString(label, tx + 1, ty + 1);
			g.setColor(LABEL_COLOR);
			g.drawString(label, tx, ty);
		}
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

	private List<GeoAnchor> filter(final List<GeoAnchor> anchors){
		if(enabledEventTypes == null){
			return anchors;
		}

		final List<GeoAnchor> result = new ArrayList<>(anchors.size());
		for(final GeoAnchor a : anchors){
			if(isEventEnabled(a)){
				result.add(a);
			}
		}
		return result;
	}

	private boolean isEventEnabled(final GeoAnchor a){
		if(enabledEventTypes == null){
			return true;
		}

		final String kind = a.kind();
		if(!kind.startsWith("event:")){
			return true;
		}

		return enabledEventTypes.contains(kind.substring("event:".length()));
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
		if(omega < 1e-9){
			return new double[]{lat1, lon1};
		}

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

	private static double interpolate(final double x1, final double x2, final double t){
		return x1 + (x2 - x1) * t;
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
