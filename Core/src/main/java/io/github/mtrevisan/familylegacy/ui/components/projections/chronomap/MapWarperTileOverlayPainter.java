package io.github.mtrevisan.familylegacy.ui.components.projections.chronomap;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.Tile;
import org.jxmapviewer.viewer.TileFactory;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;


/**
 * Overlay painter that renders historical map tiles on top of a modern base map with custom transparency.
 */
public class MapWarperTileOverlayPainter implements Painter<JXMapViewer>, ChronomapLayer{

	private final String name;
	private final TileFactory tileFactory;
	private HistoricalMapMetaData metaData;
	private float alpha = 0.9f;
	private boolean visible = true;


	public MapWarperTileOverlayPainter(final HistoricalMapMetaData metaData, final TileFactory tileFactory){
		this.metaData = metaData;
		this.name = metaData.name();
		this.tileFactory = tileFactory;
	}


	public void setMetaData(final HistoricalMapMetaData metaData){
		this.metaData = metaData;
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
		if(!visible || metaData == null || tileFactory == null){
			return;
		}

		final Graphics2D g2 = (Graphics2D)g.create();
		try{
			final Composite oldComposite = g2.getComposite();
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

			final Rectangle viewportBounds = map.getViewportBounds();
			final int zoom = map.getZoom();

			final Point2D topLeft = map.getTileFactory().geoToPixel(metaData.northWest(), zoom);
			final Point2D bottomRight = map.getTileFactory().geoToPixel(metaData.southEast(), zoom);

			final int minX = (int)Math.floor(topLeft.getX() / tileFactory.getTileSize(zoom));
			final int maxX = (int)Math.ceil(bottomRight.getX() / tileFactory.getTileSize(zoom));
			final int minY = (int)Math.floor(topLeft.getY() / tileFactory.getTileSize(zoom));
			final int maxY = (int)Math.ceil(bottomRight.getY() / tileFactory.getTileSize(zoom));

			for(int x = minX; x <= maxX; x++){
				for(int y = minY; y <= maxY; y++){
					final Tile tile = tileFactory.getTile(x, y, zoom);
					final BufferedImage image = tile.getImage();
					if(image != null){
						final int px = (int)(x * tileFactory.getTileSize(zoom) - viewportBounds.getX());
						final int py = (int)(y * tileFactory.getTileSize(zoom) - viewportBounds.getY());
						g2.drawImage(image, px, py, null);
					}
				}
			}

			g2.setComposite(oldComposite);
		}
		finally{
			g2.dispose();
		}
	}

}
