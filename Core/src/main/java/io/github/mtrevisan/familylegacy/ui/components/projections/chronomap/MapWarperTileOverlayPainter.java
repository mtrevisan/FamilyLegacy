package io.github.mtrevisan.familylegacy.ui.components.projections.chronomap;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.Tile;
import org.jxmapviewer.viewer.TileFactory;
import org.jxmapviewer.viewer.TileFactoryInfo;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;


public final class MapWarperTileOverlayPainter implements ChronomapLayer{

	private final String name;
	private final TileFactory tileFactory;

	private HistoricalMapMetaData metaData;
	private double currentTime;
	private float alpha = 0.9f;
	private boolean visible = true;
	private boolean alignmentChecked;

	public MapWarperTileOverlayPainter(final HistoricalMapMetaData metaData, final TileFactory tileFactory){
		this.metaData = metaData;
		this.name = metaData.name();
		this.tileFactory = tileFactory;
	}

	public void setMetaData(final HistoricalMapMetaData metaData){
		this.metaData = metaData;
	}

	public boolean setCurrentTime(final double currentTime){
		if(Double.compare(this.currentTime, currentTime) != 0){
			final boolean wasAvailable = metaData != null && metaData.isAvailableAt(this.currentTime);
			this.currentTime = currentTime;
			final boolean isAvailable = metaData != null && metaData.isAvailableAt(this.currentTime);

			// Return true if the visibility state shifted across validity boundary
			return wasAvailable != isAvailable;
		}
		return false;
	}

	public void setAlpha(final float alpha){
		this.alpha = Math.clamp(alpha, 0.0f, 1.0f);
	}

	public float getAlpha(){
		return alpha;
	}

	@Override
	public String getName(){
		return name;
	}

	@Override
	public boolean isVisible(){
		return visible;
	}

	@Override
	public void setVisible(final boolean visible){
		this.visible = visible;
	}

	@Override
	public void paint(final Graphics2D g, final JXMapViewer map, final int width, final int height){
		if(!visible || metaData == null || tileFactory == null)
			return;

		// Hide historical overlay if current time is outside the validity range
		if(currentTime > 0 && !metaData.isAvailableAt(currentTime))
			return;

		checkAlignment(map);

		final int currentZoom = map.getZoom();
		final TileFactoryInfo info = tileFactory.getInfo();

		// Clamp zoom level to overlay limits to avoid 404 tile requests
		final int effectiveZoom = Math.clamp(currentZoom, info.getMinimumZoomLevel(), info.getMaximumZoomLevel());

		final Graphics2D g2 = (Graphics2D)g.create();
		try{
			final Composite oldComposite = g2.getComposite();
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

			final TileFactory mapFactory = map.getTileFactory();
			final int tileSize = mapFactory.getTileSize(currentZoom);
			final Rectangle viewportBounds = map.getViewportBounds();

			// Calculate geographical bounds in world pixel coordinates for current zoom
			final Point2D nwPixel = mapFactory.geoToPixel(metaData.northWest(), currentZoom);
			final Point2D sePixel = mapFactory.geoToPixel(metaData.southEast(), currentZoom);

			final double mapMinX = Math.min(nwPixel.getX(), sePixel.getX());
			final double mapMaxX = Math.max(nwPixel.getX(), sePixel.getX());
			final double mapMinY = Math.min(nwPixel.getY(), sePixel.getY());
			final double mapMaxY = Math.max(nwPixel.getY(), sePixel.getY());

			// Skip rendering if map overlay is outside the current viewport
			if(mapMaxX < viewportBounds.getX() || mapMinX > viewportBounds.getX() + viewportBounds.getWidth()
				|| mapMaxY < viewportBounds.getY() || mapMinY > viewportBounds.getY() + viewportBounds.getHeight()){
				return;
			}

			final int minX = (int)Math.floor(mapMinX / tileSize);
			final int maxX = (int)Math.ceil(mapMaxX / tileSize);
			final int minY = (int)Math.floor(mapMinY / tileSize);
			final int maxY = (int)Math.ceil(mapMaxY / tileSize);

			for(int x = minX; x <= maxX; x++){
				for(int y = minY; y <= maxY; y++){
					final Tile tile = tileFactory.getTile(x, y, effectiveZoom);
					if(tile == null)
						continue;

					if(tile.isLoaded()){
						final BufferedImage image = tile.getImage();
						if(image != null){
							final int px = (int)(x * tileSize - viewportBounds.getX());
							final int py = (int)(y * tileSize - viewportBounds.getY());
							g2.drawImage(image, px, py, tileSize, tileSize, null);
						}
					}
					else{
						// Trigger map repaint as soon as tile loading finishes
						tile.addPropertyChangeListener("loaded", evt -> {
							if(Boolean.TRUE.equals(evt.getNewValue())){
								map.repaint();
							}
						});
					}
				}
			}

			g2.setComposite(oldComposite);
		}
		finally{
			g2.dispose();
		}
	}

	private void checkAlignment(final JXMapViewer map){
		if(alignmentChecked)
			return;

		alignmentChecked = true;

		final TileFactoryInfo baseInfo = map.getTileFactory().getInfo();
		final TileFactoryInfo overlayInfo = tileFactory.getInfo();

		if(baseInfo.getTotalMapZoom() != overlayInfo.getTotalMapZoom()){
			throw new IllegalStateException("MapWarper overlay and base map use different totalMapZoom");
		}
	}

}
