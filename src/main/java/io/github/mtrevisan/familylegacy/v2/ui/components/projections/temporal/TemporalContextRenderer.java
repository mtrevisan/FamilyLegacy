package io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.List;


/**
 * Draws the contextual layer of the General Temporal Projection:
 * <ul>
 *   <li>the {@link TemporalContextBand} backgrounds, behind the row
 *       lifelines, one translucent rectangle per historic event or
 *       cultural norm, spanning the full vertical extent of the content
 *       area;</li>
 *   <li>the {@link ContextImpactLink} connectors, dashed vertical lines
 *       drawn on top of the rows to visually anchor a band to the element
 *       it influences, explains, constrains, motivates or causes.</li>
 * </ul>
 */
public final class TemporalContextRenderer{

	/** Fill color for historic event bands. */
	public static final Color COLOR_BAND_HISTORIC = new Color(210, 180, 140, 45);

	/** Fill color for cultural norm bands. */
	public static final Color COLOR_BAND_CULTURAL = new Color(170, 190, 220, 45);

	/** Border color shared by all bands. */
	public static final Color COLOR_BAND_BORDER = new Color(150, 140, 120, 90);

	/** Color of the impact link connectors. */
	public static final Color COLOR_IMPACT_LINE = new Color(160, 120, 60, 140);


	private TemporalContextRenderer(){
	}


	/**
	 * Draws the background bands. Call this before drawing rows so that the
	 * bands appear behind the entries.
	 *
	 * @param g             the graphics context
	 * @param bands         the bands to draw (may be {@code null} or empty)
	 * @param axis          the temporal axis
	 * @param contentBounds the rectangle of the temporal content
	 */
	public static void drawBands(final Graphics2D g, final List<TemporalContextBand> bands,
		final TemporalAxis axis, final Rectangle contentBounds){
		if(g == null || bands == null || bands.isEmpty() || axis == null || contentBounds == null)
			return;

		for(final TemporalContextBand band : bands){
			final Rectangle rect = axis.spanToRect(band.span(), contentBounds.y, contentBounds.height);
			if(rect == null)
				continue;
			rect.x += contentBounds.x;

			g.setColor(colorForBand(band));
			g.fillRect(rect.x, rect.y, rect.width, rect.height);

			g.setColor(COLOR_BAND_BORDER);
			g.setStroke(new BasicStroke(1f));
			g.drawRect(rect.x, rect.y, rect.width, rect.height);
		}
	}

	/**
	 * Draws the impact links as dashed vertical lines from the top of the
	 * content area down to the vertical position of the target element.
	 *
	 * @param g             the graphics context
	 * @param links         the impact links to draw (may be {@code null} or empty)
	 * @param layout        the projection layout, used to resolve target Y positions
	 * @param axis          the temporal axis
	 * @param contentBounds the rectangle of the temporal content
	 */
	public static void drawImpactLinks(final Graphics2D g, final List<ContextImpactLink> links,
		final TemporalProjectionLayout layout, final TemporalAxis axis, final Rectangle contentBounds){
		if(g == null || links == null || links.isEmpty() || layout == null
			|| axis == null || contentBounds == null)
			return;

		final Stroke originalStroke = g.getStroke();
		try{
			g.setColor(COLOR_IMPACT_LINE);
			g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
				10f, new float[]{3f, 4f}, 0f));

			for(final ContextImpactLink link : links){
				final int[] anchor = resolveAnchor(link.targetRef(), layout, axis, contentBounds);
				if(anchor == null)
					continue;

				final int x = anchor[0];
				final int y = anchor[1];

				g.drawLine(x, contentBounds.y, x, y);
			}
		}
		finally{
			g.setStroke(originalStroke);
		}
	}


	/* ======================================================================
	 *                          Helpers
	 * ====================================================================== */

	private static Color colorForBand(final TemporalContextBand band){
		return switch(band.entity().type()){
			case HISTORIC_EVENT -> COLOR_BAND_HISTORIC;
			case CULTURAL_NORM -> COLOR_BAND_CULTURAL;
			default -> COLOR_BAND_HISTORIC;
		};
	}

	/**
	 * Resolves the screen anchor for an impact link target.
	 *
	 * @return a two-element array {@code [x, y]}, or {@code null} if the
	 * target is not currently visible
	 */
	private static int[] resolveAnchor(final TemporalProjectionRef ref,
		final TemporalProjectionLayout layout, final TemporalAxis axis, final Rectangle contentBounds){
		if(ref instanceof TemporalProjectionRef.RowRef rowRef){
			final TemporalProjectionLayout.RowLayout rowLayout = layout.findRow(rowRef.entity());
			if(rowLayout == null)
				return null;
			// Anchor at the left edge of the header column, at the row's center.
			final int x = contentBounds.x + 4;
			final int y = contentBounds.y + rowLayout.y() + rowLayout.height() / 2;
			return new int[]{x, y};
		}
		if(ref instanceof TemporalProjectionRef.EntryRef entryRef){
			final TemporalProjectionLayout.RowLayout rowLayout = layout.findRow(entryRef.rowEntity());
			if(rowLayout == null)
				return null;
			for(final TemporalProjectionLayout.TrackLayout track : rowLayout.tracks().values()){
				if(track.entryLane().containsKey(entryRef.entry())){
					final long minJdn = entryRef.entry().span().minJdn();
					final int x = (minJdn == Long.MIN_VALUE
						? contentBounds.x
						: contentBounds.x + axis.jdnToX(minJdn));
					final int y = contentBounds.y + track.entryY(entryRef.entry());
					return new int[]{x, y};
				}
			}
			return null;
		}
		if(ref instanceof TemporalProjectionRef.ConnectionRef connRef){
			final TemporalConnection connection = connRef.connection();
			final int sourceY = layout.rowCenterY(connection.sourceRow());
			final int targetY = layout.rowCenterY(connection.targetRow());
			if(sourceY < 0 || targetY < 0)
				return null;
			final long minJdn = connection.span().minJdn();
			final int x = (minJdn == Long.MIN_VALUE
				? contentBounds.x
				: contentBounds.x + axis.jdnToX(minJdn));
			final int y = contentBounds.y + (sourceY + targetY) / 2;
			return new int[]{x, y};
		}
		return null;
	}

}
