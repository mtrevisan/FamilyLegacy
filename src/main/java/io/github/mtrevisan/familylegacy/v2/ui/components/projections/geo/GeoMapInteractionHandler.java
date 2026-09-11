package io.github.mtrevisan.familylegacy.v2.ui.components.projections.geo;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import org.apache.commons.lang3.StringUtils;

import java.awt.Point;
import java.awt.geom.Point2D;

/**
 * Static helpers for hit-testing and tooltip generation in the map view.
 * <p>
 * The handler is stateless: the caller passes the model, the layout and
 * the projection on every query.
 */
public final class GeoMapInteractionHandler{


	private GeoMapInteractionHandler(){}


	/**
	 * Returns the element under the given screen point, or {@code null}.
	 * Priority: cluster, then place, then event.
	 *
	 * @param point  the screen point
	 * @param layout the layout
	 * @return the hit element, or {@code null}
	 */
	public static Object hitTest(final Point point, final GeoLayout layout){
		if(point == null || layout == null)
			return null;

		final int x = point.x;
		final int y = point.y;

		final GeoLayout.Cluster cluster = layout.hitTestCluster(x, y);
		if(cluster != null)
			return cluster;

		final GeoPlaceRef place = layout.hitTestPlace(x, y);
		if(place != null)
			return place;

		return layout.hitTestEvent(x, y);
	}

	/**
	 * Builds an HTML tooltip for the given element.
	 *
	 * @param element the element returned by
	 *                {@link #hitTest(Point, GeoLayout)}; may be {@code null}
	 * @return the tooltip text, or {@code null}
	 */
	public static String buildTooltip(final Object element){
		if(element == null)
			return null;

		final StringBuilder sb = new StringBuilder("<html>");
		if(element instanceof GeoLayout.Cluster cluster){
			sb.append("<b>")
				.append(cluster.size())
				.append(" elements</b><br>")
				.append(cluster.places()
					.size())
				.append(" places, ")
				.append(cluster.events()
					.size())
				.append(" events");
		}
		else if(element instanceof GeoPlaceRef place){
			sb.append("<b>").append(escape(place.displayName())).append("</b>");
			if(place.hasHistoricalName())
				sb.append("<br>Source: ").append(escape(place.historicalName()));
			if(place.hasCoordinate())
				sb.append("<br>").append(place.coordinate()
					.format());
		}
		else if(element instanceof GeoEventRef event){
			sb.append("<b>").append(event.kind()
				.getDisplayLabel()).append("</b>");
			sb.append("<br>Place: ").append(escape(event.placeRef()
				.displayName()));
			if(event.hasDate())
				sb.append("<br>Date: JDN ").append(event.date()
					.jdn());
		}
		else
			return null;

		return sb.append("</html>").toString();
	}

	/**
	 * Returns the FLEF record backing the given element, for opening the
	 * edit dialog.
	 *
	 * @param element the element
	 * @return the backing record, or {@code null}
	 */
	public static FLEFRecord sourceRecordOf(final Object element){
		if(element instanceof GeoPlaceRef place)
			return place.entity()
				.record();
		if(element instanceof GeoEventRef event)
			return event.eventRecord();
		return null;
	}

	/**
	 * Returns the screen point of the given place, or {@code null}.
	 */
	public static Point2D screenPointOf(final GeoPlaceRef place, final GeoLayout layout){
		return (place != null && layout != null? layout.positionOf(place): null);
	}


	private static String escape(final String s){
		if(s == null)
			return StringUtils.EMPTY;

		return s.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;");
	}

}
