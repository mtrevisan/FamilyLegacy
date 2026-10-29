package io.github.mtrevisan.familylegacy.ui.components.projections.chronomap;

import org.jxmapviewer.viewer.GeoPosition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


/** The maps of the service are read from its answer, and remembered on disk. */
class MapWarperSearchTest{

	private static final String ANSWER = """
		{"data":[
		{"id":"113424","type":"maps","attributes":{"title":"Terlago 1890","description":"","width":2833,"height":1756,"status":"warped","bbox":"11.0404828,46.074323,11.0503951,46.1126484","date_depicted":""},"relationships":{"layers":{"data":[]},"added_by":{"data":{"id":"1","type":"users"}}},"links":{"self":"x"}},
		{"id":"113425","type":"maps","attributes":{"title":"Biennale 1808-1811. Cadasto \\"Napoleonico\\" \\u00e8","status":"warped","bbox":"11.0,46.0,11.5,46.5","date_depicted":"1809"},"relationships":{"added_by":{"data":{"id":"2","type":"users"}}}},
		{"id":"113426","type":"maps","attributes":{"title":"Not yet warped 1850","status":"available","bbox":"11.0,46.0,11.5,46.5","date_depicted":""},"relationships":{}},
		{"id":"113427","type":"maps","attributes":{"title":"No year at all","status":"warped","bbox":"11.0,46.0,11.5,46.5","date_depicted":""},"relationships":{}},
		{"id":"113428","type":"maps","attributes":{"title":"Satellite","status":"warped","bbox":"11.0,46.0,11.5,46.5","date_depicted":"3874"},"relationships":{}}
		],"meta":{"total_entries":5,"total_pages":3}}
		""";


	@Test
	void onlyTheWarpedMapsWithAYearAreRead(){
		final MapWarperSearch.Page page = MapWarperSearch.parse(ANSWER);

		Assertions.assertEquals(3, page.totalPages());
		Assertions.assertEquals(List.of(113424, 113425), page.maps().stream().map(HistoricalMapMetaData::mapId).toList());
	}

	@Test
	void theYearIsTakenFromTheTitleWhenTheDateIsMissing(){
		final HistoricalMapMetaData map = MapWarperSearch.parse(ANSWER).maps().getFirst();

		Assertions.assertEquals("Terlago 1890", map.name());
		Assertions.assertEquals(MapWarperSearch.firstDayOfYear(1890), map.startJdn());
		Assertions.assertEquals(MapWarperSearch.lastDayOfYear(1890), map.endJdn());
		//north-west and south-east corners
		Assertions.assertEquals(46.1126484, map.northWest().getLatitude());
		Assertions.assertEquals(11.0404828, map.northWest().getLongitude());
		Assertions.assertEquals(46.074323, map.southEast().getLatitude());
		Assertions.assertEquals(11.0503951, map.southEast().getLongitude());
	}

	@Test
	void theDateOfTheMapWinsOverTheTitleAndEscapesAreResolved(){
		final HistoricalMapMetaData map = MapWarperSearch.parse(ANSWER).maps().get(1);

		Assertions.assertEquals("Biennale 1808-1811. Cadasto \"Napoleonico\" è [1809]", map.name());
		Assertions.assertEquals(MapWarperSearch.firstDayOfYear(1809), map.startJdn());
		Assertions.assertEquals(MapWarperSearch.lastDayOfYear(1809), map.endJdn());
	}

	@Test
	void aPeriodIsReadAsARange(){
		Assertions.assertArrayEquals(new int[]{1808, 1811}, MapWarperSearch.years(null, "Cadasto 1808-1811"));
		Assertions.assertArrayEquals(new int[]{1808, 1811}, MapWarperSearch.years("1811/1808", null));
		Assertions.assertArrayEquals(new int[]{1815, 1815}, MapWarperSearch.years("", "1815 Italy"));
		Assertions.assertNull(MapWarperSearch.years("3874", "Satellite"));
	}

	@Test
	void anInvalidBoundingBoxIsRefused(){
		Assertions.assertNull(MapWarperSearch.corners(null));
		Assertions.assertNull(MapWarperSearch.corners("1,2,3"));
		Assertions.assertNull(MapWarperSearch.corners("a,b,c,d"));
		Assertions.assertNull(MapWarperSearch.corners("11,46,10,47"));
		Assertions.assertNull(MapWarperSearch.corners("11,46,12,95"));
	}

	@Test
	void theMapsFoundAreRememberedAndAvailableWithoutNetwork() throws IOException{
		final Path file = Files.createTempFile("historical-maps", ".properties");
		try{
			final HistoricalMapMetaData map = new HistoricalMapMetaData(7, "Terlago 1890", 2411000L, 2411364L,
				new GeoPosition(46.1, 11.0), new GeoPosition(46.0, 11.1));
			Files.writeString(file, "map.7=Terlago 1890\t2411000\t2411364\t46.1\t11.0\t46.0\t11.1\n"
				+ "map.8=broken\n");

			final HistoricalMapCatalog catalog = new HistoricalMapCatalog(file);

			Assertions.assertEquals(List.of(map), catalog.maps());
			Assertions.assertFalse(catalog.isOffline());
		}
		finally{
			Files.deleteIfExists(file);
		}
	}

}
