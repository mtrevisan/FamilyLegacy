/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.ui.components.projections.chronomap;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;
import org.jxmapviewer.viewer.GeoPosition;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.util.Locale;


/**
 * A map to choose a place by clicking on it: the same map of the chronomap, with the modern base map and the historical maps that
 * depict the given day (chosen automatically or by hand, as in the chronomap).
 * <p>
 * The dialog is modal: after it has been shown, {@link #selectedPosition()} tells the place chosen, or {@code null} if the user
 * closed the dialog without confirming.
 */
public final class PlaceMapPickerDialog extends JDialog{

	/** The zoom (smaller is closer) of the map when it opens on a place. */
	private static final int PLACE_ZOOM = 8;
	/** The zoom of the map when there is no place to open on: the whole world. */
	private static final int WORLD_ZOOM = 18;
	/** The span of the timeline: from the year 500 to the year 2000 (Julian day numbers), widened to include the date entered. */
	private static final long TIMELINE_START = 1_903_700L;
	private static final long TIMELINE_END = 2_451_000L;
	private static final int PIN_RADIUS = 7;
	private static final Color PIN_COLOR = new Color(0xD32F2F);


	/** The red pin on the place chosen, painted over every map. */
	private static final class PinLayer implements ChronomapLayer{

		private GeoPosition position;


		@Override
		public String getName(){
			return "pin";
		}

		@Override
		public boolean isVisible(){
			return true;
		}

		@Override
		public void setVisible(final boolean visible){}

		@Override
		public void setTotalMapZoom(final int totalMapZoom){}

		@Override
		public void paint(final Graphics2D g, final JXMapViewer map, final int width, final int height){
			if(position == null)
				return;

			final Point2D pixel = map.getTileFactory()
				.geoToPixel(position, map.getZoom());
			final Rectangle viewport = map.getViewportBounds();
			final int x = (int)Math.round(pixel.getX() - viewport.getX());
			final int y = (int)Math.round(pixel.getY() - viewport.getY());

			final Graphics2D g2 = (Graphics2D)g.create();
			try{
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setStroke(new BasicStroke(2.f));
				g2.setColor(Color.WHITE);
				g2.drawOval(x - PIN_RADIUS - 1, y - PIN_RADIUS - 1, 2 * PIN_RADIUS + 2, 2 * PIN_RADIUS + 2);
				g2.setColor(PIN_COLOR);
				g2.drawOval(x - PIN_RADIUS, y - PIN_RADIUS, 2 * PIN_RADIUS, 2 * PIN_RADIUS);
				g2.drawLine(x - PIN_RADIUS - 4, y, x + PIN_RADIUS + 4, y);
				g2.drawLine(x, y - PIN_RADIUS - 4, x, y + PIN_RADIUS + 4);
			}
			finally{
				g2.dispose();
			}
		}

	}


	private final JXMapViewer mapViewer = new JXMapViewer();
	private final ChronomapTimeline timeline = new ChronomapTimeline();
	private final PinLayer pin = new PinLayer();
	private final JLabel positionLabel = new JLabel(I18N.t("dialog.map_picker.none"));
	private final JButton okButton = new JButton(I18N.t("button.ok"));
	private final CancellableTileFactory osmTileFactory;
	private final HistoricalMapOverlays historicalMaps;

	private GeoPosition chosen;
	private GeoPosition confirmed;


	/**
	 * @param parent  the window the dialog opens over
	 * @param jdn     the day (Julian day number) the historical maps must depict, or {@link Double#NaN} if it is not known (only the
	 *                modern map is shown then)
	 * @param initial the place to open on and to start with, or {@code null} to open on the whole world
	 */
	public PlaceMapPickerDialog(final Window parent, final double jdn, final GeoPosition initial){
		super(parent, I18N.t("dialog.map_picker.title"), ModalityType.APPLICATION_MODAL);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		osmTileFactory = HistoricalMapOverlays.createTileFactory(new OSMTileFactoryInfo("OpenStreetMap", "https://tile.openstreetmap.org"));
		final ChronomapLayerManager layers = new ChronomapLayerManager();
		layers.addLayer(pin);
		historicalMaps = new HistoricalMapOverlays(mapViewer, layers, osmTileFactory, timeline::getCurrentTime);

		mapViewer.setTileFactory(osmTileFactory);
		mapViewer.setOverlayPainter(layers);

		//the same timeline of the chronomap, to choose the year of the maps; it starts on the date entered
		final long start = (Double.isFinite(jdn)? Math.min((long)jdn, TIMELINE_START): TIMELINE_START);
		final long end = (Double.isFinite(jdn)? Math.max((long)jdn, TIMELINE_END): TIMELINE_END);
		timeline.setDomain(start, end);
		timeline.withTimeListener(time -> {
			historicalMaps.setCurrentTime(time);

			mapViewer.repaint();
		});
		if(Double.isFinite(jdn))
			timeline.setCurrentTime(jdn);

		if(initial != null){
			mapViewer.setAddressLocation(initial);
			mapViewer.setZoom(PLACE_ZOOM);
			choose(initial);
			//the viewer is not laid out yet: the place is centered again once it has its size
			mapViewer.addComponentListener(new ComponentAdapter(){
				@Override
				public void componentResized(final ComponentEvent e){
					mapViewer.removeComponentListener(this);
					mapViewer.setAddressLocation(initial);
				}
			});
		}
		else{
			mapViewer.setAddressLocation(new GeoPosition(0., 0.));
			mapViewer.setZoom(WORLD_ZOOM);
		}

		final MouseInputListener pan = new PanMouseInputListener(mapViewer);
		mapViewer.addMouseListener(pan);
		mapViewer.addMouseMotionListener(pan);
		mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCursor(mapViewer));
		mapViewer.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1)
					choose(mapViewer.convertPointToGeoPosition(e.getPoint()));
			}
		});
		historicalMaps.update();

		okButton.setEnabled(chosen != null);
		okButton.addActionListener(e -> {
			confirmed = chosen;
			dispose();
		});
		final JButton cancelButton = new JButton(I18N.t("button.close"));
		cancelButton.addActionListener(e -> dispose());

		final JPanel toolbar = new JPanel(new MigLayout("insets 6", "[][grow,fill]"));
		toolbar.add(historicalMaps.createButton());
		toolbar.add(new JLabel("<html><i>" + I18N.t("dialog.map_picker.hint") + "</i></html>"));

		final JPanel south = new JPanel(new MigLayout("insets 6", "[grow,fill][][]"));
		south.add(positionLabel);
		south.add(okButton, "sg b");
		south.add(cancelButton, "sg b");

		mapViewer.setPreferredSize(new Dimension(820, 560));
		setLayout(new BorderLayout());
		add(toolbar, BorderLayout.NORTH);
		add(mapViewer, BorderLayout.CENTER);
		final JPanel bottom = new JPanel(new BorderLayout());
		bottom.add(timeline, BorderLayout.NORTH);
		bottom.add(south, BorderLayout.CENTER);
		add(bottom, BorderLayout.SOUTH);

		GUIHelper.installEscapeToClose(this);
		//every tile factory has its own pool of threads and its cache of tiles: they are released with the dialog
		addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosed(final WindowEvent e){
				osmTileFactory.dispose();
				historicalMaps.dispose();
			}
		});

		pack();
		setLocationRelativeTo(parent);
	}


	private void choose(final GeoPosition position){
		chosen = position;
		pin.position = position;
		positionLabel.setText(I18N.tf("dialog.map_picker.position", format(position.getLatitude()), format(position.getLongitude())));
		okButton.setEnabled(true);

		mapViewer.repaint();
	}

	/** Decimal degrees with the dot, whatever the locale: it goes into the fields of the coordinates. */
	public static String format(final double degrees){
		return String.format(Locale.ROOT, "%.4f", degrees);
	}

	/** The place confirmed by the user, or {@code null} if the dialog was closed without confirming. */
	public GeoPosition selectedPosition(){
		return confirmed;
	}

}
