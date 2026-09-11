package io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal;

import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.function.Supplier;


/**
 * Translates mouse wheel and drag events into zoom and pan operations on a
 * {@link TemporalAxis}.
 * <p>
 * Interaction model:
 * <ul>
 *   <li><b>Ctrl/Cmd + wheel</b> — zoom in or out anchored at the cursor
 *       position, so that the date under the cursor stays under the cursor.</li>
 *   <li><b>Left mouse drag</b> — pan horizontally. The amount is proportional
 *       to the pixel distance and to the current visible span.</li>
 *   <li><b>Plain wheel</b> — forwarded to the enclosing {@link JScrollPane},
 *       so that vertical scrolling continues to work. Swing does not bubble
 *       mouse wheel events from a component that already has a wheel
 *       listener, so the forwarding is explicit.</li>
 * </ul>
 * The controller does not own the axis: it reads the current instance from a
 * {@link Supplier}, so that replacing the axis (e.g. after the model changes)
 * does not require re-installing the listener.
 */
public final class TemporalZoomController{

	private static final int DRAG_DEAD_ZONE_PX = 3;


	/**
	 * Listener notified whenever the axis changes.
	 */
	public interface Listener{
		/** Called after a zoom or pan operation has modified the axis. */
		void onAxisChanged();
	}


	private final Supplier<TemporalAxis> axisSupplier;

	private Listener listener;

	private Point dragAnchor;


	public TemporalZoomController(final Supplier<TemporalAxis> axisSupplier){
		if(axisSupplier == null)
			throw new IllegalArgumentException("Axis supplier must not be null");
		this.axisSupplier = axisSupplier;
	}


	public TemporalZoomController withListener(final Listener listener){
		this.listener = listener;
		return this;
	}


	/**
	 * Installs the zoom/pan listeners on the given component.
	 *
	 * @param component the target component (must not be {@code null})
	 */
	public void installOn(final JComponent component){
		if(component == null)
			throw new IllegalArgumentException("Component must not be null");

		// The source component is captured in the lambda, so that the wheel
		// handler can resolve the enclosing scroll pane on every event.
		component.addMouseWheelListener(e -> onMouseWheel(e, component));

		final MouseAdapter dragAdapter = new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent e){
				if(SwingUtilities.isLeftMouseButton(e))
					dragAnchor = e.getPoint();
			}

			@Override
			public void mouseDragged(final MouseEvent e){
				if(dragAnchor == null || !SwingUtilities.isLeftMouseButton(e))
					return;
				final int dx = e.getX() - dragAnchor.x;
				if(Math.abs(dx) < DRAG_DEAD_ZONE_PX)
					return;
				panBy(dx);
				dragAnchor = e.getPoint();
			}

			@Override
			public void mouseReleased(final MouseEvent e){
				dragAnchor = null;
			}
		};
		component.addMouseListener(dragAdapter);
		component.addMouseMotionListener(dragAdapter);
	}


	private void onMouseWheel(final MouseWheelEvent e, final JComponent source){
		// Ctrl/Cmd + wheel → zoom anchored at the cursor.
		if(e.isControlDown() || e.isMetaDown()){
			e.consume();
			final int rotations = e.getWheelRotation();
			if(rotations != 0)
				zoomAtCursor(rotations < 0, e.getX());
			return;
		}

		// Plain wheel → forward to the enclosing scroll pane, because Swing
		// does not bubble mouse wheel events from a component that already
		// has a wheel listener registered.
		final JScrollPane scrollPane = (JScrollPane)SwingUtilities.getAncestorOfClass(
			JScrollPane.class, source);
		if(scrollPane == null)
			return;

		final JScrollBar bar = scrollPane.getVerticalScrollBar();
		if(bar == null || !bar.isVisible())
			return;

		final int direction = (e.getWheelRotation() < 0? -1: 1);
		final int increment;
		if(e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL)
			increment = bar.getUnitIncrement(direction) * e.getUnitsToScroll();
		else
			increment = bar.getBlockIncrement(direction) * e.getWheelRotation();

		bar.setValue(bar.getValue() + increment);
		e.consume();
	}


	/**
	 * Zooms in or out, keeping the date under the given X coordinate fixed.
	 *
	 * @param zoomIn  {@code true} to zoom in, {@code false} to zoom out
	 * @param cursorX the X coordinate (relative to the temporal content) of the anchor
	 */
	public void zoomAtCursor(final boolean zoomIn, final int cursorX){
		final TemporalAxis axis = axisSupplier.get();
		if(axis == null || axis.isEmpty())
			return;

		final long span = axis.visibleEndJdn() - axis.visibleStartJdn();
		if(zoomIn && span <= 2L)
			return;

		final long anchorJdn = axis.xToJdn(cursorX);
		final double ratio = (double)(anchorJdn - axis.visibleStartJdn()) / (double)span;

		final long newSpan = (zoomIn? Math.max(2L, span / 2L): span * 2L);
		final long newStart = anchorJdn - (long)(newSpan * ratio);
		final long newEnd = newStart + newSpan;

		axis.setVisibleRange(newStart, newEnd);
		notifyChanged();
	}

	/**
	 * Pans the visible window by the given pixel amount.
	 *
	 * @param dxPixels positive values move the content to the right
	 *                 (i.e. pan earlier in time)
	 */
	public void panBy(final int dxPixels){
		final TemporalAxis axis = axisSupplier.get();
		if(axis == null || axis.isEmpty())
			return;

		final long span = axis.visibleEndJdn() - axis.visibleStartJdn();
		final int viewportWidth = axis.viewportWidth();
		if(viewportWidth <= 0 || span <= 0L)
			return;

		final long deltaJdn = -(long)((double)dxPixels * span / viewportWidth);
		if(deltaJdn == 0L && dxPixels != 0)
			return;

		axis.pan(deltaJdn);
		notifyChanged();
	}


	private void notifyChanged(){
		if(listener != null)
			listener.onAxisChanged();
	}

}
