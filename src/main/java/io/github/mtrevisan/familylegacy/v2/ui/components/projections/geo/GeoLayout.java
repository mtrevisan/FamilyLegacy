/**
 * Copyright (c) 2026 Mauro Trevisan
 * ... (license header unchanged)
 */
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.geo;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes the screen positions of the markers from a {@link GeoMapModel}
 * and a {@link GeoProjection}.
 * <p>
 * Handles marker overlap through a simple cluster mechanism: if two
 * markers fall within {@link #CLUSTER_RADIUS} pixels, they are merged into
 * a single cluster marker whose display size grows with the number of
 * elements. The individual markers are still retrievable for hit-testing.
 * <p>
 * The layout is immutable. Positions are exposed both per place and per
 * event.
 */
public final class GeoLayout{

	/** Radius, in pixels, within which two markers are clustered. */
	public static final int CLUSTER_RADIUS = 12;

	/** Radius, in pixels, of a single marker. */
	public static final int MARKER_RADIUS = 6;

	/** Distance, in pixels, from the marker center to the label. */
	public static final int LABEL_OFFSET = 8;

	private static final int CLUSTER_BASE_RADIUS = 8;


	private final Map<GeoPlaceRef, Point2D> placePositions;
	private final Map<GeoEventRef, Point2D> eventPositions;
	private final List<Cluster> clusters;


	private GeoLayout(final Map<GeoPlaceRef, Point2D> placePositions,
		final Map<GeoEventRef, Point2D> eventPositions, final List<Cluster> clusters){
		this.placePositions = Collections.unmodifiableMap(placePositions);
		this.eventPositions = Collections.unmodifiableMap(eventPositions);
		this.clusters = Collections.unmodifiableList(clusters);
	}


	/**
	 * A cluster of markers that would otherwise overlap.
	 *
	 * @param center the screen position of the cluster
	 * @param places the places in the cluster
	 * @param events the events in the cluster
	 */
	public record Cluster(Point2D center, List<GeoPlaceRef> places, List<GeoEventRef> events){

		public Cluster{
			places = (places != null? List.copyOf(places): List.of());
			events = (events != null? List.copyOf(events): List.of());
		}

		public int size(){
			return places.size() + events.size();
		}

		public int radius(){
			return CLUSTER_BASE_RADIUS + Math.min(10, size());
		}
	}


	/* ======================================================================
	 *                          Factory
	 * ====================================================================== */

	/**
	 * Computes the layout of the given model through the given projection.
	 *
	 * @param model      the model (must not be {@code null})
	 * @param projection the projection (must not be {@code null})
	 * @return the layout
	 */
	public static GeoLayout compute(final GeoMapModel model, final GeoProjection projection){
		if(model == null || projection == null)
			return new GeoLayout(Map.of(), Map.of(), List.of());

		final Map<GeoPlaceRef, Point2D> placePositions = new HashMap<>();
		for(final GeoPlaceRef place : model.places())
			if(place.hasCoordinate())
				placePositions.put(place, projection.geoToScreen(place.coordinate()));

		final Map<GeoEventRef, Point2D> eventPositions = new HashMap<>();
		for(final GeoEventRef event : model.events())
			if(event.hasPlace())
				eventPositions.put(event, projection.geoToScreen(event.placeRef()
					.coordinate()));

		final List<Cluster> clusters = buildClusters(placePositions, eventPositions);
		return new GeoLayout(placePositions, eventPositions, clusters);
	}


	/* ======================================================================
	 *                          Accessors
	 * ====================================================================== */

	public Map<GeoPlaceRef, Point2D> placePositions(){ return placePositions; }
	public Map<GeoEventRef, Point2D> eventPositions(){ return eventPositions; }
	public List<Cluster> clusters(){ return clusters; }

	public boolean isEmpty(){
		return placePositions.isEmpty() && eventPositions.isEmpty();
	}

	public Point2D positionOf(final GeoPlaceRef place){
		return placePositions.get(place);
	}

	public Point2D positionOf(final GeoEventRef event){
		return eventPositions.get(event);
	}

	/**
	 * Returns the cluster whose bounding circle contains the given screen
	 * point, or {@code null} if no cluster is hit.
	 *
	 * @param x the X coordinate
	 * @param y the Y coordinate
	 * @return the hit cluster, or {@code null}
	 */
	public Cluster hitTestCluster(final int x, final int y){
		for(final Cluster cluster : clusters){
			final double dx = x - cluster.center()
				.getX();
			final double dy = y - cluster.center()
				.getY();
			final double r = cluster.radius();
			if(dx * dx + dy * dy <= r * r)
				return cluster;
		}
		return null;
	}

	/**
	 * Returns the place whose marker contains the given screen point, or
	 * {@code null}. Prefers clusters when the point is inside a cluster.
	 *
	 * @param x the X coordinate
	 * @param y the Y coordinate
	 * @return the hit place, or {@code null}
	 */
	public GeoPlaceRef hitTestPlace(final int x, final int y){
		for(final Map.Entry<GeoPlaceRef, Point2D> entry : placePositions.entrySet()){
			final double dx = x - entry.getValue()
				.getX();
			final double dy = y - entry.getValue()
				.getY();
			if(dx * dx + dy * dy <= MARKER_RADIUS * MARKER_RADIUS)
				return entry.getKey();
		}
		return null;
	}

	public GeoEventRef hitTestEvent(final int x, final int y){
		for(final Map.Entry<GeoEventRef, Point2D> entry : eventPositions.entrySet()){
			final double dx = x - entry.getValue()
				.getX();
			final double dy = y - entry.getValue()
				.getY();
			if(dx * dx + dy * dy <= MARKER_RADIUS * MARKER_RADIUS)
				return entry.getKey();
		}
		return null;
	}

	/**
	 * Returns the bounding box of the visible layout, for diagnostics.
	 */
	public Rectangle boundingBox(){
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		for(final Point2D p : placePositions.values()){
			minX = Math.min(minX, p.getX());
			minY = Math.min(minY, p.getY());
			maxX = Math.max(maxX, p.getX());
			maxY = Math.max(maxY, p.getY());
		}
		for(final Point2D p : eventPositions.values()){
			minX = Math.min(minX, p.getX());
			minY = Math.min(minY, p.getY());
			maxX = Math.max(maxX, p.getX());
			maxY = Math.max(maxY, p.getY());
		}
		if(minX > maxX || minY > maxY)
			return new Rectangle(0, 0, 0, 0);
		return new Rectangle((int)minX, (int)minY, (int)(maxX - minX), (int)(maxY - minY));
	}


	/* ======================================================================
	 *                          Clustering
	 * ====================================================================== */

	private static List<Cluster> buildClusters(final Map<GeoPlaceRef, Point2D> places,
		final Map<GeoEventRef, Point2D> events){
		final List<Cluster> result = new ArrayList<>();
		final List<GeoPlaceRef> unclusteredPlaces = new ArrayList<>(places.keySet());
		final List<GeoEventRef> unclusteredEvents = new ArrayList<>(events.keySet());

		// Greedy clustering: pick a point, absorb nearby points into the cluster.
		while(!unclusteredPlaces.isEmpty() || !unclusteredEvents.isEmpty()){
			final Point2D seed = pickSeed(unclusteredPlaces, unclusteredEvents, places, events);
			if(seed == null)
				break;

			final List<GeoPlaceRef> clusterPlaces = new ArrayList<>();
			final List<GeoEventRef> clusterEvents = new ArrayList<>();
			absorbPlaces(seed, unclusteredPlaces, places, clusterPlaces);
			absorbEvents(seed, unclusteredEvents, events, clusterEvents);

			final int total = clusterPlaces.size() + clusterEvents.size();
			if(total <= 1)
				continue;

			result.add(new Cluster(seed, clusterPlaces, clusterEvents));
		}
		return result;
	}

	private static Point2D pickSeed(final List<GeoPlaceRef> places, final List<GeoEventRef> events,
		final Map<GeoPlaceRef, Point2D> placePositions, final Map<GeoEventRef, Point2D> eventPositions){
		if(!places.isEmpty())
			return placePositions.get(places.get(0));
		if(!events.isEmpty())
			return eventPositions.get(events.get(0));
		return null;
	}

	private static void absorbPlaces(final Point2D seed, final List<GeoPlaceRef> source,
		final Map<GeoPlaceRef, Point2D> positions, final List<GeoPlaceRef> target){
		final var iterator = source.iterator();
		while(iterator.hasNext()){
			final GeoPlaceRef place = iterator.next();
			final Point2D p = positions.get(place);
			if(p != null && distance(seed, p) <= CLUSTER_RADIUS){
				target.add(place);
				iterator.remove();
			}
		}
	}

	private static void absorbEvents(final Point2D seed, final List<GeoEventRef> source,
		final Map<GeoEventRef, Point2D> positions, final List<GeoEventRef> target){
		final var iterator = source.iterator();
		while(iterator.hasNext()){
			final GeoEventRef event = iterator.next();
			final Point2D p = positions.get(event);
			if(p != null && distance(seed, p) <= CLUSTER_RADIUS){
				target.add(event);
				iterator.remove();
			}
		}
	}

	private static double distance(final Point2D a, final Point2D b){
		return Math.hypot(a.getX() - b.getX(), a.getY() - b.getY());
	}

}
