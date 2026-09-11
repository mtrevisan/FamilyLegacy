package io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal;

import org.apache.commons.lang3.StringUtils;

import java.awt.Rectangle;
import java.util.Map;


/**
 * Static hit‑testing and tooltip utilities for the General Temporal
 * Projection.
 * <p>
 * The handler is stateless: callers pass the current model, layout, axis and
 * content bounds on every query. Hit testing follows the visual stacking
 * order (connections first, then entries), so that the topmost element under
 * the cursor is returned.
 */
public final class TemporalInteractionHandler{

	/** Tolerance, in pixels, applied to connection hit testing. */
	private static final int CONNECTION_HIT_TOLERANCE = 4;


	private TemporalInteractionHandler(){
	}


	/**
	 * Returns the topmost projection element under the given point, or
	 * {@code null} if the point does not hit anything.
	 *
	 * @param x             the X coordinate, in content‑canvas pixels
	 * @param y             the Y coordinate, in content‑canvas pixels
	 * @param model         the projection model (must not be {@code null})
	 * @param layout        the projection layout (must not be {@code null})
	 * @param axis          the temporal axis (must not be {@code null})
	 * @param contentBounds the rectangle of the temporal content
	 * @return the hit element, or {@code null}
	 */
	public static TemporalProjectionRef hitTest(final int x, final int y,
		final TemporalProjectionModel model, final TemporalProjectionLayout layout,
		final TemporalAxis axis, final Rectangle contentBounds){
		if(model == null || layout == null || axis == null || contentBounds == null)
			return null;

		// 1. Entries (top layer for clicking purposes)
		for(final TemporalRow row : model.rows()){
			final TemporalProjectionLayout.RowLayout rowLayout = layout.findRow(row.entity());
			if(rowLayout == null)
				continue;
			for(final Map.Entry<TemporalTrackType, TemporalProjectionLayout.TrackLayout> entry
				: rowLayout.tracks().entrySet()){
				final TemporalProjectionLayout.TrackLayout trackLayout = entry.getValue();
				for(final TemporalEntry candidate : trackLayout.entryLane().keySet()){
					final int entryY = contentBounds.y + trackLayout.entryY(candidate);
					final Rectangle rect = axis.spanToRect(candidate.span(), entryY,
						TemporalProjectionLayout.ENTRY_HEIGHT);
					if(rect == null)
						continue;
					rect.x += contentBounds.x;
					if(rect.contains(x, y))
						return new TemporalProjectionRef.EntryRef(row.entity(), candidate);
				}
			}
		}

		// 2. Connections
		for(final TemporalConnection connection : model.connections()){
			if(hitConnection(x, y, connection, layout, axis, contentBounds))
				return new TemporalProjectionRef.ConnectionRef(connection);
		}

		return null;
	}

	/**
	 * Builds an HTML tooltip for the given element.
	 *
	 * @param ref the element (must not be {@code null})
	 * @return the tooltip text, or {@code null} if no text is available
	 */
	public static String buildTooltip(final TemporalProjectionRef ref){
		if(ref == null)
			return null;

		final StringBuilder sb = new StringBuilder("<html>");
		if(ref instanceof TemporalProjectionRef.EntryRef entryRef){
			final TemporalEntry entry = entryRef.entry();
			sb.append("<b>").append(entry.label()).append("</b>");
			if(!entry.type().isEmpty())
				sb.append("<br>Type: ").append(entry.type());
			if(entry.hasRole())
				sb.append("<br>Role: ").append(entry.role());
			sb.append("<br>").append(describeSpan(entry.span()));
			sb.append("<br><i>Double-click to edit</i>");
			return sb.append("</html>").toString();
		}
		if(ref instanceof TemporalProjectionRef.ConnectionRef connectionRef){
			final TemporalConnection connection = connectionRef.connection();
			sb.append("<b>").append(connection.relationshipType()).append("</b>");
			if(connection.hasRole())
				sb.append("<br>Role: ").append(connection.role());
			sb.append("<br>Status: ").append(connection.span().status());
			sb.append("<br>").append(describeSpan(connection.span()));
			sb.append("<br><i>Double-click to edit</i>");
			return sb.append("</html>").toString();
		}
		if(ref instanceof TemporalProjectionRef.RowRef rowRef)
			return "<html><b>" + rowRef.entity().displayLabel() + "</b></html>";

		return null;
	}


	/* ======================================================================
	 *                          Helpers
	 * ====================================================================== */

	private static boolean hitConnection(final int x, final int y, final TemporalConnection connection,
		final TemporalProjectionLayout layout, final TemporalAxis axis, final Rectangle contentBounds){
		final int sourceY = layout.rowCenterY(connection.sourceRow());
		final int targetY = layout.rowCenterY(connection.targetRow());
		if(sourceY < 0 || targetY < 0)
			return false;

		final int screenSourceY = contentBounds.y + sourceY;
		final int screenTargetY = contentBounds.y + targetY;

		final long minJdn = connection.span().minJdn();
		final long maxJdn = connection.span().maxJdn();

		final int startX = (minJdn == Long.MIN_VALUE
			? contentBounds.x
			: contentBounds.x + axis.jdnToX(minJdn));
		final int endX = (maxJdn == Long.MAX_VALUE
			? contentBounds.x + contentBounds.width
			: contentBounds.x + axis.jdnToX(maxJdn));
		final int midX = (startX + endX) / 2;

		// Bounding box of the three-segment polyline, expanded by tolerance.
		final int minX = Math.min(startX, Math.min(midX, endX)) - CONNECTION_HIT_TOLERANCE;
		final int maxX = Math.max(startX, Math.max(midX, endX)) + CONNECTION_HIT_TOLERANCE;
		final int minY = Math.min(screenSourceY, screenTargetY) - CONNECTION_HIT_TOLERANCE;
		final int maxY = Math.max(screenSourceY, screenTargetY) + CONNECTION_HIT_TOLERANCE;
		return (x >= minX && x <= maxX && y >= minY && y <= maxY);
	}

	private static String describeSpan(final TemporalSpan span){
		if(span == null)
			return StringUtils.EMPTY;
		if(span.isPoint()){
			final NormalizedDate start = span.start();
			final String calendar = (start.isGregorian()? StringUtils.EMPTY: " (" + start.calendar() + ")");
			final String approx = (start.approximate()? " ~": StringUtils.EMPTY);
			return "Date: " + start.jdn() + approx + calendar;
		}
		final String startText = (span.start() != null? Long.toString(span.start().jdn()): "(open)");
		final String endText = (span.end() != null? Long.toString(span.end().jdn()): "(open)");
		return span.kind() + ": " + startText + " .. " + endText + " [" + span.status() + "]";
	}

}
