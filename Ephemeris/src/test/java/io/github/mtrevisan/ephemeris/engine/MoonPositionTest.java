package io.github.mtrevisan.ephemeris.engine;

import io.github.mtrevisan.ephemeris.helpers.JulianDate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class MoonPositionTest{

	@Test
	void test(){
		final double t = JulianDate.centuryJ2000Of(2444269.5);
		final double[] xyz = MoonPosition.getInstanceDE405()
			.rectangular(t);

		Assertions.assertEquals(-186813.08162, xyz[0], 0.000_01);
		Assertions.assertEquals(349310.09818, xyz[1], 0.000_01);
		Assertions.assertEquals(-19003.33833, xyz[2], 0.000_01);
	}

}