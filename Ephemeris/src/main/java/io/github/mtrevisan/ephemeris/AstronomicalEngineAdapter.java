package io.github.mtrevisan.ephemeris;

import io.github.mtrevisan.ephemeris.engine.MoonPosition;
import io.github.mtrevisan.ephemeris.engine.NutationCorrections;
import io.github.mtrevisan.ephemeris.engine.SunPosition;
import io.github.mtrevisan.ephemeris.helpers.DeltaT;
import io.github.mtrevisan.ephemeris.helpers.JulianDate;
import io.github.mtrevisan.ephemeris.helpers.MathHelper;
import io.github.mtrevisan.familylegacy.services.AstronomicalEngine;


/**
 * Astronomical engine implementation strictly using VSOP2013 theory for solar position
 * and ELP2000-82B for lunar position.
 */
public final class AstronomicalEngineAdapter implements AstronomicalEngine{

	// Precision target for root-finding (~0.08 seconds of day)
	private static final double TIME_PRECISION = 1.e-6;
	private static final int MAX_ITERATIONS = 20;

	// Mean synodic month derived from IAU 2010 mean elongation motion
	private static final double MEAN_SYNODIC_MONTH = (360. * JulianDate.CIVIL_SAECULUM)
		/ (NutationCorrections.MOON_ELONGATION_SUN[1] / JulianDate.SECONDS_PER_HOUR);

	// [rad/day]
	private static final double SYNODIC_MONTH_SPEED = 0.5079177;
	// [rad/day]
	private static final double MEAN_SOLAR_SPEED = 0.01720279;


	public static void main(String[] args){
		int year = 1980;
		int month = 1;
		boolean isLeap = false;
		int day = 1;

		double resultJdUT = ChineseCalendarAstronomicalEngine.chineseDateToJulianDateUT(year, month, isLeap, day);

		System.out.println("Julian Date UT: " + resultJdUT);
		System.out.println("Julian Date UT: 2444285.16666");
	}

	@Override
	public boolean isAvailable(){
		return true;
	}

	@Override
	public double getNextNewMoonJdn(final double approxJdn, final double utcOffset){
		// Convergence loop using Brent's / Newton's method for Moon-Sun elongation = 0
		double tJdn = approxJdn;
		for(int i = 0; i < MAX_ITERATIONS; i ++){
			final double dt = DeltaT.deltaTSecondsFromJd(tJdn);
			final double dtDays = DeltaT.deltaTDays(dt);
			final double tdbJc = JulianDate.centuryJ2000Of(tJdn + dtDays);
			final double tdbJme = tdbJc / 10.;

			final NutationCorrections nutation = NutationCorrections.calculate(tdbJc);
			final double sunLong = SunPosition.apparentSunLongitude(tdbJme, nutation.getDeltaPsi());
			final double moonLong = MoonPosition.getInstanceDE405()
				.longitudeOfDate(tdbJc);

			double diff = MathHelper.mod2pi(moonLong - sunLong);
			if(diff > Math.PI)
				diff -= 2. * Math.PI;

			final double deltaDays = diff / SYNODIC_MONTH_SPEED;
			tJdn -= deltaDays;
			if(Math.abs(deltaDays) < TIME_PRECISION)
				break;
		}

		// Return astronomical UT JDN plus timezone offset in days
		return tJdn + utcOffset / JulianDate.HOURS_PER_DAY;
	}

	@Override
	public double getSolarLongitudeJdn(final int year, final double targetLongitude, final double utcOffset){
		final double targetRad = Math.toRadians(targetLongitude);

		// Accurate initial estimate based on astronomical longitude offset from Vernal Equinox (~March 20)
		// Longitude 0° is ~March 20 (Day 79), 90° is ~June 21, 180° is ~Sept 23, 270° is ~Dec 21
		final double dayOfYearEstimate = 79.25 + (targetLongitude / 360.) * JulianDate.MEAN_TROPICAL_YEAR_LENGTH;
		double tJdn = JulianDate.of(year, 1, 1) + (dayOfYearEstimate % JulianDate.MEAN_TROPICAL_YEAR_LENGTH);
		for(int i = 0; i < MAX_ITERATIONS; i ++){
			final double dtDays = DeltaT.deltaTDays(DeltaT.deltaTSecondsFromJd(tJdn));
			final double tdbJc = JulianDate.centuryJ2000Of(tJdn + dtDays);
			final double tdbJme = tdbJc / 10.;

			final NutationCorrections nutation = NutationCorrections.calculate(tdbJc);
			final double sunLong = SunPosition.apparentSunLongitude(tdbJme, nutation.getDeltaPsi());

			double diff = MathHelper.mod2pi(sunLong - targetRad);
			if(diff > Math.PI)
				diff -= 2. * Math.PI;

			final double deltaDays = diff / MEAN_SOLAR_SPEED;
			tJdn -= deltaDays;
			if(Math.abs(deltaDays) < TIME_PRECISION)
				return tJdn + utcOffset / JulianDate.HOURS_PER_DAY;
		}
		return -1.;
	}

}

