package io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal;

import org.apache.commons.lang3.StringUtils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Map;


/**
 * Draws a single row of the General Temporal Projection: the row header
 * (label column) and all the track entries.
 * <p>
 * Entries are drawn only when they intersect the visible window; the
 * intersection test is delegated to
 * {@link TemporalAxis#spanToRect(TemporalSpan, int, int)}, which returns
 * {@code null} for out‑of‑range spans.
 */
public final class TemporalRowRenderer{

	/** Background of the row header area. */
	public static final Color COLOR_HEADER_BACKGROUND = new Color(244, 240, 232);

	/** Background of a selected row's header. */
	public static final Color COLOR_HEADER_SELECTED = new Color(230, 218, 196);

	/** Border between two row headers. */
	public static final Color COLOR_HEADER_BORDER = new Color(214, 208, 196);

	/** Color of the row label text. */
	public static final Color COLOR_LABEL = new Color(50, 40, 30);

	/** Base color for the {@link TemporalTrackType#EVENT} track. */
	public static final Color COLOR_EVENT = new Color(70, 120, 190);

	/** Base color for the {@link TemporalTrackType#ATTRIBUTE} track. */
	public static final Color COLOR_ATTRIBUTE = new Color(80, 150, 90);

	/** Base color for the {@link TemporalTrackType#CONTEXT} track. */
	public static final Color COLOR_CONTEXT = new Color(190, 130, 70);

	/** Background of a selected row in the content area (behind the entries). */
	public static final Color COLOR_CONTENT_SELECTED = new Color(238, 230, 214);

	/** Font for the row labels. */
	private static final Font FONT_LABEL = new Font("Tahoma", Font.PLAIN, 13);


	private static final int LABEL_PADDING = 6;
	private static final String TRUNCATION_SUFFIX = "…";


	private TemporalRowRenderer(){
	}


	/**
	 * Draws one row (header and tracks).
	 *
	 * @param g             the graphics context
	 * @param row           the row model (must not be {@code null})
	 * @param rowLayout     the row layout (must not be {@code null})
	 * @param axis          the temporal axis
	 * @param headerBounds  the rectangle reserved for row labels
	 * @param contentBounds the rectangle of the temporal content
	 * @param selected      whether the row is currently selected
	 */
	public static void drawRow(final Graphics2D g, final TemporalRow row,
		final TemporalProjectionLayout.RowLayout rowLayout, final TemporalAxis axis,
		final Rectangle headerBounds, final Rectangle contentBounds, final boolean selected){
		if(g == null || row == null || rowLayout == null || axis == null
			|| headerBounds == null || contentBounds == null)
			return;

		final int screenY = contentBounds.y + rowLayout.y();
		final int rowHeight = rowLayout.height();

		drawHeader(g, row, headerBounds, screenY, rowHeight, selected);

		if(row.collapsed())
			return;

		drawTracks(g, row, rowLayout, axis, contentBounds, selected);
	}

	/**
	 * Draws only the row header (label column).
	 */
	public static void drawRowHeader(final Graphics2D g, final TemporalRow row,
		final TemporalProjectionLayout.RowLayout rowLayout, final Rectangle headerBounds,
		final boolean selected){
		if(g == null || row == null || rowLayout == null || headerBounds == null)
			return;
		drawHeader(g, row, headerBounds, rowLayout.y(), rowLayout.height(), selected);
	}

	/**
	 * Draws only the entry bars of a row.
	 */
	public static void drawRowTracks(final Graphics2D g, final TemporalRow row,
		final TemporalProjectionLayout.RowLayout rowLayout, final TemporalAxis axis,
		final Rectangle contentBounds, final boolean selected){
		if(g == null || row == null || rowLayout == null || axis == null || contentBounds == null)
			return;
		if(row.collapsed())
			return;
		drawTracks(g, row, rowLayout, axis, contentBounds, selected);
	}

	/**
	 * Draws the selection background of a row across the whole temporal
	 * content area. Must be called before {@link #drawRowTracks}, so that the
	 * entries are drawn on top of the selection.
	 *
	 * @param g             the graphics context
	 * @param rowLayout     the row layout
	 * @param contentBounds the rectangle of the temporal content
	 * @param selected      whether the row is currently selected
	 */
	public static void drawRowSelectionBackground(final Graphics2D g,
		final TemporalProjectionLayout.RowLayout rowLayout, final Rectangle contentBounds,
		final boolean selected){
		if(!selected || g == null || rowLayout == null || contentBounds == null)
			return;
		g.setColor(COLOR_CONTENT_SELECTED);
		g.fillRect(contentBounds.x, contentBounds.y + rowLayout.y(),
			contentBounds.width, rowLayout.height());
	}


	/* ======================================================================
	 *                          Header
	 * ====================================================================== */

	private static void drawHeader(final Graphics2D g, final TemporalRow row,
		final Rectangle headerBounds, final int screenY, final int rowHeight, final boolean selected){
		// Background
		g.setColor(selected? COLOR_HEADER_SELECTED: COLOR_HEADER_BACKGROUND);
		g.fillRect(headerBounds.x, screenY, headerBounds.width, rowHeight);

		// Top border
		g.setColor(COLOR_HEADER_BORDER);
		g.setStroke(new BasicStroke(1f));
		g.drawLine(headerBounds.x, screenY, headerBounds.x + headerBounds.width - 1, screenY);

		// Label
		final Font originalFont = g.getFont();
		try{
			g.setFont(FONT_LABEL);
			g.setColor(COLOR_LABEL);
			final FontMetrics fm = g.getFontMetrics();
			final String rawLabel = row.entity().displayLabel();
			final String label = truncate(g, rawLabel, headerBounds.width - 2 * LABEL_PADDING);
			final int labelY = screenY + fm.getAscent() + (rowHeight - fm.getHeight()) / 2;
			g.drawString(label, headerBounds.x + LABEL_PADDING, labelY);
		}
		finally{
			g.setFont(originalFont);
		}
	}

	private static String truncate(final Graphics2D g, final String text, final int maxWidth){
		if(text == null)
			return StringUtils.EMPTY;
		final FontMetrics fm = g.getFontMetrics();
		if(fm.stringWidth(text) <= maxWidth)
			return text;
		final int suffixWidth = fm.stringWidth(TRUNCATION_SUFFIX);
		int end = text.length();
		while(end > 0 && fm.stringWidth(text.substring(0, end)) + suffixWidth > maxWidth)
			end--;
		return (end > 0? text.substring(0, end) + TRUNCATION_SUFFIX: TRUNCATION_SUFFIX);
	}


	/* ======================================================================
	 *                          Tracks
	 * ====================================================================== */

	private static void drawTracks(final Graphics2D g, final TemporalRow row,
		final TemporalProjectionLayout.RowLayout rowLayout, final TemporalAxis axis,
		final Rectangle contentBounds, final boolean selected){
		for(final Map.Entry<TemporalTrackType, TemporalProjectionLayout.TrackLayout> trackEntry
			: rowLayout.tracks().entrySet()){
			final TemporalTrackType trackType = trackEntry.getKey();
			final TemporalProjectionLayout.TrackLayout trackLayout = trackEntry.getValue();
			final Color baseColor = colorForTrack(trackType);

			for(final TemporalEntry entry : trackLayout.entryLane().keySet()){
				final int entryY = contentBounds.y + trackLayout.entryY(entry);
				final Rectangle rect = axis.spanToRect(entry.span(), entryY,
					TemporalProjectionLayout.ENTRY_HEIGHT);
				if(rect == null)
					continue;
				rect.x += contentBounds.x;
				TemporalSpanRenderer.drawSpan(g, rect, entry.span(), baseColor, selected);
			}
		}
	}


	/* ======================================================================
	 *                          Track palette
	 * ====================================================================== */

	/**
	 * Returns the base color for entries of the given track type.
	 *
	 * @param type the track type (must not be {@code null})
	 * @return the base color
	 */
	public static Color colorForTrack(final TemporalTrackType type){
		return switch(type){
			case EVENT -> COLOR_EVENT;
			case ATTRIBUTE -> COLOR_ATTRIBUTE;
			case CONTEXT -> COLOR_CONTEXT;
		};
	}

}
