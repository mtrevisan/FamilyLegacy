package io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.List;


/**
 * Draws the temporal connections (arcs) between rows of the General
 * Temporal Projection.
 * <p>
 * Each connection is rendered as an orthogonal three‑segment polyline that
 * links the two rows at the temporal extent of the relationship:
 * <ol>
 *   <li>horizontal segment on the source row, from the span start to its
 *       midpoint;</li>
 *   <li>vertical segment connecting the two rows at that midpoint;</li>
 *   <li>horizontal segment on the target row, from the midpoint to the span
 *       end.</li>
 * </ol>
 * Open‑ended spans are extended to the right edge of the visible window.
 * The stroke style encodes the {@link TemporalSpan#status()}: solid for
 * {@code active}, dashed for {@code ended}, dotted for {@code unknown}.
 * A small arrow head on the target end reflects the FLEF direction
 * ({@code subject} → {@code target}).
 */
public final class TemporalConnectionRenderer{

	/** Base color of a connection. */
	public static final Color COLOR_CONNECTION = new Color(90, 90, 90);

	private static final int ARROW_SIZE = 6;
	private static final Color COLOR_ARROW = new Color(60, 60, 60);


	private TemporalConnectionRenderer(){
	}


	/**
	 * Draws all connections.
	 *
	 * @param g             the graphics context
	 * @param connections   the connections to draw (may be {@code null} or empty)
	 * @param layout        the projection layout, used to resolve row Y positions
	 * @param axis          the temporal axis
	 * @param contentBounds the rectangle of the temporal content
	 */
	public static void drawConnections(final Graphics2D g, final List<TemporalConnection> connections,
		final TemporalProjectionLayout layout, final TemporalAxis axis, final Rectangle contentBounds){
		if(g == null || connections == null || connections.isEmpty() || layout == null
			|| axis == null || contentBounds == null)
			return;

		final Stroke originalStroke = g.getStroke();
		try{
			for(final TemporalConnection connection : connections)
				drawConnection(g, connection, layout, axis, contentBounds);
		}
		finally{
			g.setStroke(originalStroke);
		}
	}


	private static void drawConnection(final Graphics2D g, final TemporalConnection connection,
		final TemporalProjectionLayout layout, final TemporalAxis axis, final Rectangle contentBounds){
		final int sourceY = layout.rowCenterY(connection.sourceRow());
		final int targetY = layout.rowCenterY(connection.targetRow());
		if(sourceY < 0 || targetY < 0)
			return;

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

		// Skip connections entirely outside the visible window
		if(endX < contentBounds.x || startX > contentBounds.x + contentBounds.width)
			return;

		final int midX = (startX + endX) / 2;

		final Stroke stroke = strokeForStatus(connection.span().status());
		g.setStroke(stroke);
		g.setColor(COLOR_CONNECTION);

		// Three-segment orthogonal polyline
		g.drawLine(startX, screenSourceY, midX, screenSourceY);
		g.drawLine(midX, screenSourceY, midX, screenTargetY);
		g.drawLine(midX, screenTargetY, endX, screenTargetY);

		// Arrow head at the target end
		drawArrowHead(g, endX, screenTargetY, midX < endX);
	}

	private static Stroke strokeForStatus(final String status){
		return switch(status){
			case TemporalSpan.STATUS_ACTIVE -> new BasicStroke(1.2f);
			case TemporalSpan.STATUS_ENDED -> new BasicStroke(1.2f, BasicStroke.CAP_BUTT,
				BasicStroke.JOIN_ROUND, 10f, new float[]{5f, 4f}, 0f);
			default -> new BasicStroke(1.2f, BasicStroke.CAP_BUTT,
				BasicStroke.JOIN_ROUND, 10f, new float[]{2f, 3f}, 0f);
		};
	}

	private static void drawArrowHead(final Graphics2D g, final int tipX, final int tipY,
		final boolean pointRight){
		final int direction = (pointRight? -1: 1);
		g.setColor(COLOR_ARROW);
		g.setStroke(new BasicStroke(1f));
		g.drawLine(tipX, tipY, tipX + direction * ARROW_SIZE, tipY - ARROW_SIZE / 2);
		g.drawLine(tipX, tipY, tipX + direction * ARROW_SIZE, tipY + ARROW_SIZE / 2);
	}

}
