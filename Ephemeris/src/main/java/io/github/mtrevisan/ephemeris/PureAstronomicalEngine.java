package io.github.mtrevisan.ephemeris;

import io.github.mtrevisan.ephemeris.engine.MoonPosition;
import io.github.mtrevisan.ephemeris.engine.NutationCorrections;
import io.github.mtrevisan.ephemeris.engine.SunPosition;
import io.github.mtrevisan.ephemeris.helpers.DeltaT;
import io.github.mtrevisan.ephemeris.helpers.IllinoisSolver;
import io.github.mtrevisan.ephemeris.helpers.JulianDate;
import io.github.mtrevisan.ephemeris.helpers.MathHelper;


public final class PureAstronomicalEngine implements EphemerisEngine{

	private static final double ACCURACY_THRESHOLD = 1.0e-7; // ~8 milliseconds
	private static final int MAX_ITERATIONS = 50;

	// Historical JD thresholds for China/Beijing observatory contexts
	private static final double JD_1645_SHIXIAN = 2321912.0;
	private static final double JD_1929_STANDARD_TIME = 2425612.0;


	public PureAstronomicalEngine(){}


	@Override
	public double findNextNewMoon(final double approximateJdUT){
		return IllinoisSolver.solve(approximateJdUT - 3.0, approximateJdUT + 3.0, ACCURACY_THRESHOLD, MAX_ITERATIONS, jdUT -> {
			final double ttJc = getTerrestrialCenturies(jdUT);
			final double deltaPsi = NutationCorrections.calculate(ttJc).getDeltaPsi();

			final double sunLng = SunPosition.apparentSunLongitude(ttJc, deltaPsi);
			final double moonLng = MoonPosition.computeApparentMoonLongitude(ttJc, deltaPsi);

			return MathHelper.mod2pi(moonLng - sunLng + StrictMath.PI) - StrictMath.PI;
		});
	}

	@Override
	public double findSolarLongitudeEvent(final double approximateJdUT, final double targetAngleRad){
		return IllinoisSolver.solve(approximateJdUT - 5., approximateJdUT + 5., ACCURACY_THRESHOLD,
				MAX_ITERATIONS, jdUT -> {
			final double ttJc = getTerrestrialCenturies(jdUT);
			final double deltaPsi = NutationCorrections.calculate(ttJc).getDeltaPsi();

			final double sunLng = SunPosition.apparentSunLongitude(ttJc, deltaPsi);

			return MathHelper.mod2pi(sunLng - targetAngleRad + StrictMath.PI) - StrictMath.PI;
		});
	}

	@Override
	public double getLocalTimeOffset(final double jdUT){
		if(jdUT >= JD_1929_STANDARD_TIME)
			return 8.;
		if(jdUT >= JD_1645_SHIXIAN)
			return 7.761111;
		return 7.9;
	}

	private static double getTerrestrialCenturies(final double jdUT){
		final double dt = DeltaT.deltaTSecondsFromJd(jdUT);
		return JulianDate.centuryJ2000Of(jdUT + DeltaT.deltaTDays(dt));
	}

}
