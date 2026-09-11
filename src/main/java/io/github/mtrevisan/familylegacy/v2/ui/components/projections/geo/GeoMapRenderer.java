package io.github.mtrevisan.familylegacy.v2.ui.components.projections.geo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.List;

/**
 * Orchestrates the rendering of the map view.
 * <p>
 * Draw order:
 * <ol>
 *   <li>background;</li>
 *   <li>graticule (behind everything else);</li>
 *   <li>migration routes;</li>
 *   <li>place markers;</li>
 *   <li>event markers;</li>
 *   <li>clusters;</li>
 * </ol>
 * The renderer is stateless: it reads the model, the layout and the
 * viewport on every repaint.
 */
public final class GeoMapRenderer{

	/** Background color of the map. */
	public static final Color COLOR_BACKGROUND = new Color(245, 243, 236);

	/** Color of the outside-of-bounds area, drawn around the map rectangle. */
	public static final Color COLOR_OUTSIDE = new Color(220, 218, 210);


	private GeoMapRenderer(){}


	/**
	 * Draws the whole map.
	 *
	 * @param g              the graphics context
	 * @param model          the model
	 * @param layout         the layout
	 * @param projection     the projection
	 * @param viewport       the rectangle of the map area
	 * @param selectedPlace  the currently selected place, or {@code null}
	 * @param selectedEvent  the currently selected event, or {@code null}
	 * @param zoom           the current zoom level, used to decide whether
	 *                       to draw labels
	 */
	public static void draw(final Graphics2D g, final GeoMapModel model, final GeoLayout layout,
		final GeoProjection projection, final Rectangle viewport, final GeoPlaceRef selectedPlace,
		final GeoEventRef selectedEvent, final GeoZoomLevel zoom){
		if(g == null || model == null || layout == null || projection == null || viewport == null)
			return;

		// 1. Background.
		g.setColor(COLOR_OUTSIDE);
		g.fillRect(viewport.x, viewport.y, viewport.width, viewport.height);
		g.setColor(COLOR_BACKGROUND);
		g.fillRect(viewport.x, viewport.y, viewport.width, viewport.height);

		// 2. Graticule.
		GeoGraticuleRenderer.draw(g, projection, viewport);

		// 3. Routes.
		for(final GeoMigrationRoute route : model.routes())
			GeoRouteRenderer.draw(g, route, projection);

		// 4. Places.
		final boolean drawLabels = GeoMarkerRenderer.shouldDrawLabels(zoom);
		for(final GeoPlaceRef place : model.places()){
			final Point2D center = layout.positionOf(place);
			if(center == null || !viewport.contains(center))
				continue;
			final boolean selected = (selectedPlace != null && selectedPlace.equals(place));
			GeoMarkerRenderer.drawPlace(g, place, center, selected, drawLabels);
		}

		// 5. Events.
		for(final GeoEventRef event : model.events()){
			final Point2D center = layout.positionOf(event);
			if(center == null || !viewport.contains(center))
				continue;
			final boolean selected = (selectedEvent != null && selectedEvent.equals(event));
			GeoMarkerRenderer.drawEvent(g, event, center, selected, drawLabels);
		}

		// 6. Clusters.
		final List<GeoLayout.Cluster> clusters = layout.clusters();
		for(final GeoLayout.Cluster cluster : clusters){
			final Point2D center = cluster.center();
			if(center == null || !viewport.contains(center))
				continue;
			GeoMarkerRenderer.drawCluster(g, cluster, false);
		}
	}

}
