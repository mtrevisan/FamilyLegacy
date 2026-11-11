package io.github.mtrevisan.ephemeris.helpers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class DeltaTTest{

	@Test
	void observedValuesAreNotAltered(){
		//IERS observed: 2026-02-01 is 69.1133 s
		Assertions.assertEquals(69.1133, DeltaT.deltaTSeconds(2026. + 1. / 12.), 0.02);
		Assertions.assertEquals(66.07, DeltaT.deltaTSeconds(2010.), 0.05);
	}

	@Test
	void continuousAtTheBoundaries(){
		for(final double y : new double[]{1973.1, 2026.25, 2036.25})
			Assertions.assertEquals(DeltaT.deltaTSeconds(y - 1.e-6), DeltaT.deltaTSeconds(y + 1.e-6), 1.e-3);
	}

}
