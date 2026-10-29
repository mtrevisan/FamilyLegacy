package io.github.mtrevisan.familylegacy.ui.components.projections.chronomap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;


/** What is drawn of the circle of uncertainty, in a map of 800x600 pixels. */
class UncertaintyAreaTest{

	private static final int WIDTH = 800;
	private static final int HEIGHT = 600;


	@Test
	void aCircleSmallerThanAPixelIsNotDrawn(){
		Assertions.assertNull(ChronomapOverlayPainter.uncertaintyArea(400, 300, 0.4, WIDTH, HEIGHT));
		Assertions.assertNull(ChronomapOverlayPainter.uncertaintyArea(400, 300, 0., WIDTH, HEIGHT));
		Assertions.assertNull(ChronomapOverlayPainter.uncertaintyArea(400, 300, Double.NaN, WIDTH, HEIGHT));
	}

	@Test
	void aCircleInsideTheMapIsDrawnAsIs(){
		final Shape area = ChronomapOverlayPainter.uncertaintyArea(400, 300, 50, WIDTH, HEIGHT);

		final Ellipse2D circle = Assertions.assertInstanceOf(Ellipse2D.class, area);
		Assertions.assertEquals(350., circle.getX(), 1e-9);
		Assertions.assertEquals(250., circle.getY(), 1e-9);
		Assertions.assertEquals(100., circle.getWidth(), 1e-9);
		Assertions.assertEquals(100., circle.getHeight(), 1e-9);
	}

	@Test
	void aCircleThatCrossesTheEdgeOfTheMapIsDrawnAsIs(){
		Assertions.assertInstanceOf(Ellipse2D.class, ChronomapOverlayPainter.uncertaintyArea(10, 300, 100, WIDTH, HEIGHT));
	}

	@Test
	void aCircleOutOfTheMapIsNotDrawn(){
		//the center is 300 pixels to the left of the map, the radius is 100
		Assertions.assertNull(ChronomapOverlayPainter.uncertaintyArea(-300, 300, 100, WIDTH, HEIGHT));
		//the corner is the closest point: 141.4 pixels away
		Assertions.assertNull(ChronomapOverlayPainter.uncertaintyArea(-100, -100, 100, WIDTH, HEIGHT));
		Assertions.assertInstanceOf(Ellipse2D.class, ChronomapOverlayPainter.uncertaintyArea(-100, -100, 150, WIDTH, HEIGHT));
	}

	@Test
	void aCircleThatCoversTheMapIsTheWholeMap(){
		final Shape area = ChronomapOverlayPainter.uncertaintyArea(400, 300, 1_000, WIDTH, HEIGHT);

		final Rectangle2D map = Assertions.assertInstanceOf(Rectangle2D.class, area);
		Assertions.assertEquals(WIDTH, map.getWidth(), 1e-9);
		Assertions.assertEquals(HEIGHT, map.getHeight(), 1e-9);
	}

	@Test
	void aHugeCircleIsNeverDrawnAsACircle(){
		//the uncertainty of a region at a high zoom, and the infinite radius at the poles
		Assertions.assertInstanceOf(Rectangle2D.class, ChronomapOverlayPainter.uncertaintyArea(400, 300, 80_000, WIDTH, HEIGHT));
		Assertions.assertInstanceOf(Rectangle2D.class, ChronomapOverlayPainter.uncertaintyArea(400, 300, Double.POSITIVE_INFINITY, WIDTH,
			HEIGHT));
		//a huge circle that has the map out of it
		Assertions.assertNull(ChronomapOverlayPainter.uncertaintyArea(-1_000_000, 300, 80_000, WIDTH, HEIGHT));
	}

	@Test
	void theRadiusIsTheUncertaintyDividedByTheMetersOfAPixel(){
		//at the equator, at the zoom 10 (OpenStreetMap), a pixel is 152.87 meters wide
		Assertions.assertEquals(1_000. / 152.8734, ChronomapOverlayPainter.pixelRadius(1_000., 0., 10), 1e-3);
		//a meter is wider in the pixels of the high latitudes (the projection stretches them): the circle is bigger
		Assertions.assertEquals(ChronomapOverlayPainter.pixelRadius(1_000., 0., 10) / Math.cos(Math.toRadians(60.)),
			ChronomapOverlayPainter.pixelRadius(1_000., 60., 10), 1e-9);
	}

	@Test
	void theRadiusOfAHugeUncertaintyIsLimited(){
		final double limit = ChronomapOverlayPainter.pixelRadius(ChronomapOverlayPainter.MAX_UNCERTAINTY_METERS, 45., 4);

		//a country, a continent, half the globe: all the same
		Assertions.assertEquals(limit, ChronomapOverlayPainter.pixelRadius(700_000., 45., 4), 1e-9);
		Assertions.assertEquals(limit, ChronomapOverlayPainter.pixelRadius(10_000_000., 45., 4), 1e-9);
		Assertions.assertTrue(ChronomapOverlayPainter.pixelRadius(50_000., 45., 4) < limit);
	}

	@Test
	void theRadiusAtThePolesIsHuge(){
		Assertions.assertTrue(ChronomapOverlayPainter.pixelRadius(1_000., 90., 10) > 1e9);
	}

}
