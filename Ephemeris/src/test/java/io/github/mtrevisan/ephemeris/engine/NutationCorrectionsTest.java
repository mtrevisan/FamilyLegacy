package io.github.mtrevisan.ephemeris.engine;

import io.github.mtrevisan.ephemeris.helpers.JulianDate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class NutationCorrectionsTest{

	@Test
	void past(){
		final double jd = JulianDate.of(1950, 1, 1);
		final double jc = JulianDate.centuryJ2000Of(jd);
		final NutationCorrections corrections = NutationCorrections.calculate(jc);

		Assertions.assertEquals(-1.6012487704991984e-5, corrections.getDeltaPsi(), 0.000_000_001);
		Assertions.assertEquals(4.035072836631418e-5, corrections.getDeltaEpsilon(), 0.000_000_001);


		final double mlan = NutationCorrections.moonLongitudeAscendingNode(jc);
		Assertions.assertEquals(0.21141530376878137, mlan, 0.000_000_1);
	}

	@Test
	void present(){
		final double jd = JulianDate.of(2026, 10, 1);
		final double jc = JulianDate.centuryJ2000Of(jd);
		final NutationCorrections corrections = NutationCorrections.calculate(jc);

		Assertions.assertEquals(4.007513875177848e-5, corrections.getDeltaPsi(), 0.000_000_001);
		Assertions.assertEquals(3.978719433686745e-5, corrections.getDeltaEpsilon(), 0.000_000_001);


		final double mlan = NutationCorrections.moonLongitudeAscendingNode(jc);
		Assertions.assertEquals(5.719669064760243, mlan, 0.000_000_1);
	}

	@Test
	void future(){
		final double jd = JulianDate.of(2050, 1, 1);
		final double jc = JulianDate.centuryJ2000Of(jd);
		final NutationCorrections corrections = NutationCorrections.calculate(jc);

		Assertions.assertEquals(7.355226541589181e-5, corrections.getDeltaPsi(), 0.000_000_001);
		Assertions.assertEquals(-2.5840848107024025e-5, corrections.getDeltaEpsilon(), 0.000_000_001);


		final double mlan = NutationCorrections.moonLongitudeAscendingNode(jc);
		Assertions.assertEquals(4.153481202550509, mlan, 0.000_000_1);
	}

}