package io.github.mtrevisan.familylegacy.v2.ui.components.projections.geo;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.function.Supplier;

/**
 * Translates mouse wheel and drag events into zoom and pan operations on
 * a {@link GeoMapViewport}.
 * <p>
 * Interaction model:
 * <ul>
 *   <li><b>Wheel</b> — zoom in or out, anchored at the cursor position.</li>
 *   <li><b>Left mouse drag</b> — pan the map. The amount is proportional to
 *       the pixel distance and to the current visible span.</li>
 * </ul>
 */
public final class GeoMapController{

	private static final int DRAG_DEAD_ZONE_PX = 3;
	private static final double DEGREES_PER_WHEEL = 0.5;


	/**
	 * Listener notified whenever the viewport changes.
	 */
	public interface Listener{
		/** Called after a zoom or pan operation has modified the viewport. */
		void onViewportChanged();
	}


	private final Supplier<GeoMapViewport> viewportSupplier;
	private Listener listener;
	private Point dragAnchor;
	private GeoCoordinate dragStartGeo;


	public GeoMapController(final Supplier<GeoMapViewport> viewportSupplier){
		if(viewportSupplier == null)
			throw new IllegalArgumentException("Viewport supplier must not be null");
		this.viewportSupplier = viewportSupplier;
	}


	public GeoMapController withListener(final Listener listener){
		this.listener = listener;
		return this;
	}


	public void installOn(final JComponent component){
		if(component == null)
			throw new IllegalArgumentException("Component must not be null");

		component.addMouseWheelListener(this::onWheel);

		final MouseAdapter dragAdapter = new MouseAdapter(){
			@Override public void mousePressed(final MouseEvent e){
				if(SwingUtilities.isLeftMouseButton(e)){
					final GeoMapViewport vp = viewportSupplier.get();
					if(vp == null)
						return;
					dragAnchor = e.getPoint();
					dragStartGeo = vp.createProjection()
						.screenToGeo(e.getPoint());
				}
			}

			@Override public void mouseDragged(final MouseEvent e){
				if(dragAnchor == null || !SwingUtilities.isLeftMouseButton(e))
					return;
				final int dx = e.getX() - dragAnchor.x;
				final int dy = e.getY() - dragAnchor.y;
				if(Math.abs(dx) < DRAG_DEAD_ZONE_PX && Math.abs(dy) < DRAG_DEAD_ZONE_PX)
					return;
				panBy(dx, dy);
				dragAnchor = e.getPoint();
			}

			@Override public void mouseReleased(final MouseEvent e){
				dragAnchor = null;
				dragStartGeo = null;
			}
		};
		component.addMouseListener(dragAdapter);
		component.addMouseMotionListener(dragAdapter);
	}


	private void onWheel(final MouseWheelEvent e){
		final GeoMapViewport vp = viewportSupplier.get();
		if(vp == null)
			return;
		e.consume();

		final int rotations = e.getWheelRotation();
		if(rotations == 0)
			return;

		final GeoProjection projection = vp.createProjection();
		final GeoCoordinate anchor = projection.screenToGeo(e.getPoint());

		if(rotations < 0)
			vp.zoomIn();
		else
			vp.zoomOut();

		recenterOn(vp, anchor);
		notifyChanged();
	}

	/**
	 * Pans the viewport by the given pixel offsets.
	 */
	public void panBy(final int dxPixels, final int dyPixels){
		final GeoMapViewport vp = viewportSupplier.get();
		if(vp == null)
			return;

		final GeoBounds bounds = vp.bounds();
		final int w = Math.max(1, vp.width());
		final int h = Math.max(1, vp.height());
		final double deltaLon = -(double)dxPixels * bounds.spanLongitude() / w;
		final double deltaLat = (double)dyPixels * bounds.spanLatitude() / h;
		vp.pan(deltaLon, deltaLat);
		notifyChanged();
	}


	private void recenterOn(final GeoMapViewport vp, final GeoCoordinate anchor){
		final GeoBounds b = vp.bounds();
		final double newMinLat = anchor.latitude() - b.spanLatitude() / 2.;
		final double newMaxLat = anchor.latitude() + b.spanLatitude() / 2.;
		final double newMinLon = anchor.longitude() - b.spanLongitude() / 2.;
		final double newMaxLon = anchor.longitude() + b.spanLongitude() / 2.;
		vp.setBounds(new GeoBounds(
			Math.max(GeoCoordinate.MIN_LATITUDE, newMinLat),
			Math.max(GeoCoordinate.MIN_LONGITUDE, newMinLon),
			Math.min(GeoCoordinate.MAX_LATITUDE, newMaxLat),
			Math.min(GeoCoordinate.MAX_LONGITUDE, newMaxLon)));
	}

	private void notifyChanged(){
		if(listener != null)
			listener.onViewportChanged();
	}

	// Reserved for future precision wheel control.
	static double degreesPerWheel(){
		return DEGREES_PER_WHEEL;
	}

}
