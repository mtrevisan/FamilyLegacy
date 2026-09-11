package io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;


/**
 * Orchestrates the rendering of the General Temporal Projection.
 * <p>
 * The draw order is fixed and encodes the visual layering of the
 * projection:
 * <ol>
 *   <li>background;</li>
 *   <li>context bands ({@link TemporalContextRenderer#drawBands});</li>
 *   <li>vertical grid lines ({@link TemporalAxisRenderer#drawVerticalGrid});</li>
 *   <li>row lifelines ({@link TemporalRowRenderer#drawRow});</li>
 *   <li>connection arcs ({@link TemporalConnectionRenderer#drawConnections});</li>
 *   <li>impact link connectors ({@link TemporalContextRenderer#drawImpactLinks});</li>
 *   <li>axis strip ({@link TemporalAxisRenderer#drawAxisStrip}).</li>
 * </ol>
 * All coordinates are computed on the fly from the {@link TemporalAxis}
 * (horizontal) and from the {@link TemporalProjectionLayout} (vertical);
 * the renderer is stateless and therefore safe to invoke on every repaint
 * after pan, zoom, or selection changes.
 */
public final class TemporalProjectionRenderer{

	/** Overall background of the projection. */
	public static final Color COLOR_BACKGROUND = new Color(252, 251, 248);

	/** Height of the axis strip at the top of the panel, in pixels. */
	public static final int AXIS_STRIP_HEIGHT = 32;


	private TemporalProjectionRenderer(){
	}


	/**
	 * Draws the whole projection.
	 *
	 * @param g              the graphics context
	 * @param model          the projection model (must not be {@code null})
	 * @param layout         the projection layout (must not be {@code null})
	 * @param axis           the temporal axis (must not be {@code null})
	 * @param bounds         the full drawing area of the panel
	 * @param selectedEntity the currently selected row entity, or {@code null}
	 */
	public static void draw(final Graphics2D g, final TemporalProjectionModel model,
		final TemporalProjectionLayout layout, final TemporalAxis axis, final Rectangle bounds,
		final TemporalEntityRef selectedEntity){
		if(g == null || model == null || layout == null || axis == null || bounds == null)
			return;

		// Derive the panel sub-areas from the full bounds.
		final Rectangle headerBounds = new Rectangle(
			bounds.x,
			bounds.y + AXIS_STRIP_HEIGHT,
			TemporalProjectionLayout.ROW_HEADER_WIDTH,
			Math.max(0, bounds.height - AXIS_STRIP_HEIGHT));

		final Rectangle axisBounds = new Rectangle(
			bounds.x + TemporalProjectionLayout.ROW_HEADER_WIDTH,
			bounds.y,
			Math.max(0, bounds.width - TemporalProjectionLayout.ROW_HEADER_WIDTH),
			AXIS_STRIP_HEIGHT);

		final Rectangle contentBounds = new Rectangle(
			bounds.x + TemporalProjectionLayout.ROW_HEADER_WIDTH,
			bounds.y + AXIS_STRIP_HEIGHT,
			Math.max(0, bounds.width - TemporalProjectionLayout.ROW_HEADER_WIDTH),
			Math.max(0, bounds.height - AXIS_STRIP_HEIGHT));

		// 1. Background
		g.setColor(COLOR_BACKGROUND);
		g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

		// 2. Context bands (behind everything else)
		TemporalContextRenderer.drawBands(g, model.bands(), axis, contentBounds);

		// 3. Vertical grid
		TemporalAxisRenderer.drawVerticalGrid(g, axis, contentBounds);

		// 4. Rows
		for(final TemporalProjectionLayout.RowLayout rowLayout : layout.rows()){
			final TemporalRow row = findRow(model, rowLayout.entity());
			if(row == null)
				continue;
			final boolean selected = (selectedEntity != null && selectedEntity.equals(rowLayout.entity()));
			TemporalRowRenderer.drawRow(g, row, rowLayout, axis, headerBounds, contentBounds, selected);
		}

		// 5. Connection arcs
		TemporalConnectionRenderer.drawConnections(g, model.connections(), layout, axis, contentBounds);

		// 6. Impact links
		TemporalContextRenderer.drawImpactLinks(g, model.impactLinks(), layout, axis, contentBounds);

		// 7. Axis strip on top
		TemporalAxisRenderer.drawAxisStrip(g, axis, axisBounds, contentBounds);
	}


	private static TemporalRow findRow(final TemporalProjectionModel model, final TemporalEntityRef entity){
		for(final TemporalRow row : model.rows())
			if(row.entity().equals(entity))
				return row;
		return null;
	}

}
