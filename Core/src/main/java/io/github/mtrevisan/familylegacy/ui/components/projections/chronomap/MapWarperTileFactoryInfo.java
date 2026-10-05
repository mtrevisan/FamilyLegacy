package io.github.mtrevisan.familylegacy.ui.components.projections.chronomap;

import org.jxmapviewer.viewer.TileFactoryInfo;


/**
 * TileFactoryInfo for georeferenced historical maps hosted on MapWarper.
 */
public final class MapWarperTileFactoryInfo extends TileFactoryInfo{

	private static final int TOP_ZOOM_LEVEL = 19;
	private static final int MAX_ZOOM_LEVEL = 18;
	private static final int MIN_ZOOM_LEVEL = 1;
	private static final int TILE_SIZE = 256;


	public MapWarperTileFactoryInfo(final int mapId){
		super(
			"MapWarper-" + mapId,
			MIN_ZOOM_LEVEL,
			MAX_ZOOM_LEVEL,
			TOP_ZOOM_LEVEL,
			TILE_SIZE,
			true,
			true,
			"https://mapwarper.net/maps/tile/" + mapId,
			"x", "y", "z"
		);
	}


	@Override
	public String getTileUrl(final int x, final int y, final int zoom){
		final int z = TOP_ZOOM_LEVEL - zoom;
		return this.baseURL + "/" + z + "/" + x + "/" + y + ".png";
	}

}
