package io.github.mtrevisan.ephemeris.engine;

import io.github.mtrevisan.ephemeris.engine.coordinates.EclipticCoordinates;
import io.github.mtrevisan.ephemeris.helpers.JulianDate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class SunPositionTest{

	@Test
	void test(){
		final double t = JulianDate.millenniumJ2000Of(2444269.5);
		final EclipticCoordinates coord = SunPosition.embTrueHeliocentricPosition(t);

		Assertions.assertEquals(3.182657571886453E-5, coord.getLatitude(), 0.000_000_1);
		Assertions.assertEquals(2.278426072988786, coord.getLongitude(), 0.000_000_1);
		Assertions.assertEquals(0.9851322735478121, coord.getDistance(), 0.000_000_1);
	}

}