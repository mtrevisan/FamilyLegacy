package io.github.mtrevisan.ephemeris;

import io.github.mtrevisan.ephemeris.engine.NutationCorrections;
import io.github.mtrevisan.ephemeris.helpers.JulianDate;
import io.github.mtrevisan.ephemeris.helpers.MathHelper;
import io.github.mtrevisan.ephemeris.helpers.ResourceReader;
import io.github.mtrevisan.familylegacy.v2.services.AstronomicalEngine;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;


/**
 * Astronomical engine implementation strictly using VSOP2013 theory for solar position
 * and ELP2000-82B for lunar position.
 */
public final class AstronomicalEngineAdapter implements AstronomicalEngine{

	// Precision target for root-finding (~0.008 seconds of day)
	private static final double TIME_PRECISION = 1.e-7;

	// Mean synodic month derived from IAU 2010 mean elongation motion
	private static final double MEAN_SYNODIC_MONTH = (360. * JulianDate.CIVIL_SAECULUM) / NutationCorrections.MOON_MEAN_ELONGATION_COEFFS[1];

	private final Map<ResourceReader.VariableIndex, List<ResourceReader.VSOP2013Data>> vsop2013Data;


	public AstronomicalEngineAdapter(){
		try{
			// Strictly loads VSOP2013 dataset from classpath
			vsop2013Data = ResourceReader.readData("VSOP2013p3.dat");
			if(vsop2013Data == null || vsop2013Data.isEmpty()){
				throw new IllegalStateException("VSOP2013 dataset 'VSOP2013p3.dat' is empty or invalid.");
			}
		}
		catch(final IOException e){
			throw new IllegalStateException("Failed to load required VSOP2013 dataset 'VSOP2013p3.dat'", e);
		}
	}


	@Override
	public boolean isAvailable(){
		return true;
	}

	@Override
	public long getNextNewMoonJdn(final double approxJdn, final double utcOffset){
		double tt = approxJdn;
		double correction;

		// Newton-Raphson iteration for exact Moon-Sun longitude conjunction (0 rad)
		do{
			final double jce = JulianDate.centuryJ2000Of(tt);
			final NutationCorrections nutation = NutationCorrections.calculate(jce);

			// Geocentric apparent solar longitude strictly using VSOP2013
			final double sunLong = calculateVsop2013SunApparentLongitude(jce, nutation.getDeltaPsi());

			// Geocentric apparent lunar longitude using ELP2000-82B series
			final double moonLong = calculateHighPrecisionMoonLongitude(jce, nutation.getDeltaPsi());

			final double phaseAngle = MathHelper.mod2pi(moonLong - sunLong);
			final double diff = (phaseAngle > Math.PI ? phaseAngle - MathHelper.TWO_PI : phaseAngle);

			correction = -diff * (MEAN_SYNODIC_MONTH / MathHelper.TWO_PI);
			tt += correction;
		}while(Math.abs(correction) > TIME_PRECISION);

		// Convert TT to local timezone Julian Day Number
		return Math.round(tt + (utcOffset / 24.0));
	}

	@Override
	public double getSolarLongitudeJdn(final int year, final double targetLongitudeDeg, final double utcOffset){
		final double targetRad = Math.toRadians(targetLongitudeDeg);
		final LocalDate date = LocalDate.of(year, 1, 1);
		final int yearLength = date.isLeapYear() ? 366 : 365;

		double tt = JulianDate.of(date);
		final double minUT = tt;
		double correction;

		do{
			final double jce = JulianDate.centuryJ2000Of(tt);
			final NutationCorrections nutation = NutationCorrections.calculate(jce);
			final double sunApparentLongitude = calculateVsop2013SunApparentLongitude(jce, nutation.getDeltaPsi());

			// Inverse solar longitude solver
			correction = 58.0 * StrictMath.sin(targetRad - sunApparentLongitude);
			tt += correction;
			if(tt < minUT){
				tt += yearLength;
			}
		}while(Math.abs(correction) > TIME_PRECISION);

		return tt + (utcOffset / 24.0);
	}

	/**
	 * Computes geocentric apparent solar longitude strictly using VSOP2013 series evaluation.
	 */
	private double calculateVsop2013SunApparentLongitude(final double jce, final double deltaPsi){
		final double jme = jce / 10.0; // Julian Ephemeris Millennium

		final double earthLong = evaluateVsop2013Variable(ResourceReader.VariableIndex.L, jme);
		final double earthRadius = evaluateVsop2013Variable(ResourceReader.VariableIndex.A, jme);

		final double sunGeocentricLongitude = MathHelper.mod2pi(earthLong + Math.PI);
		final double aberration = SunPosition.aberrationCorrection(earthRadius);

		return MathHelper.mod2pi(sunGeocentricLongitude + deltaPsi + aberration);
	}

	/**
	 * Evaluates VSOP2013 trigonometric series polynomial sum for a specific variable.
	 */
	private double evaluateVsop2013Variable(final ResourceReader.VariableIndex varIdx, final double jme){
		final List<ResourceReader.VSOP2013Data> datasets = vsop2013Data.get(varIdx);
		if(datasets == null || datasets.isEmpty()){
			throw new IllegalStateException("Missing VSOP2013 data series for variable: " + varIdx);
		}

		double result = 0.0;
		for(final ResourceReader.VSOP2013Data data : datasets){
			double subSum = 0.0;
			for(final ResourceReader.VSOP2013Coeffs coeff : data.coeffs){
				subSum += coeff.cosine * StrictMath.cos(coeff.iphi[0] * jme)
					+ coeff.sine * StrictMath.sin(coeff.iphi[0] * jme);
			}
			result += subSum * StrictMath.pow(jme, data.timePower);
		}
		return result;
	}

	/**
	 * Computes geocentric ecliptic apparent longitude of the Moon using ELP2000-82B main series.
	 */
	private static double calculateHighPrecisionMoonLongitude(final double jce, final double deltaPsi){
		// Fundamental Arguments (IAU 2010 / ELP2000)
		final double L0 = Math.toRadians(MathHelper.polynomial(jce, new double[]{218.3164477, 481267.88123421, -0.0015786, 1.0 / 538841.0, -1.0 / 65194000.0}));
		final double D = Math.toRadians(MathHelper.polynomial(jce, new double[]{297.8501921, 445267.1114034, -0.0018819, 1.0 / 545868.0, -1.0 / 113065000.0}));
		final double M = Math.toRadians(MathHelper.polynomial(jce, new double[]{357.5291092, 35999.0502909, -0.0001536, 1.0 / 24490000.0}));
		final double Mp = Math.toRadians(MathHelper.polynomial(jce, new double[]{134.9633964, 477198.8675055, 0.0087414, 1.0 / 69699.0, -1.0 / 14712000.0}));
		final double F = Math.toRadians(MathHelper.polynomial(jce, new double[]{93.2720950, 483202.0175233, -0.0036539, -1.0 / 3526000.0, 1.0 / 863310000.0}));

		// Planetary perturbation arguments
		final double A1 = Math.toRadians(119.75 + 131.849 * jce);
		final double A2 = Math.toRadians(53.09 + 479264.290 * jce);
		final double A3 = Math.toRadians(313.45 + 481266.484 * jce);
		final double E = 1.0 - 0.002516 * jce - 0.0000074 * jce * jce;

		// Major Periodic Terms for Lunar Longitude (in 0.000001 degrees)
		double Σl = 6288774.0 * StrictMath.sin(Mp)
			+ 1274027.0 * StrictMath.sin(2.0 * D - Mp)
			+ 658314.0 * StrictMath.sin(2.0 * D)
			+ 213618.0 * StrictMath.sin(2.0 * Mp)
			- 185116.0 * E * StrictMath.sin(M)
			- 114332.0 * StrictMath.sin(2.0 * F)
			+ 58793.0 * StrictMath.sin(2.0 * D - 2.0 * Mp)
			+ 57066.0 * E * StrictMath.sin(2.0 * D - M - Mp)
			+ 53322.0 * StrictMath.sin(2.0 * D + Mp)
			+ 45758.0 * E * StrictMath.sin(2.0 * D - M)
			- 40923.0 * E * StrictMath.sin(M - Mp)
			- 34720.0 * StrictMath.sin(D)
			- 30383.0 * E * StrictMath.sin(M + Mp)
			+ 15327.0 * StrictMath.sin(2.0 * D - 2.0 * F)
			- 12528.0 * StrictMath.sin(Mp + 2.0 * F)
			+ 10980.0 * StrictMath.sin(Mp - 2.0 * F)
			+ 10675.0 * StrictMath.sin(4.0 * D - Mp)
			+ 10075.0 * StrictMath.sin(3.0 * Mp)
			+ 8548.0 * StrictMath.sin(4.0 * D - 2.0 * Mp)
			- 7888.0 * E * StrictMath.sin(2.0 * D + M - Mp)
			- 6766.0 * E * StrictMath.sin(2.0 * D + M)
			- 5163.0 * StrictMath.sin(D - Mp)
			+ 4987.0 * E * StrictMath.sin(D + M)
			+ 4026.0 * StrictMath.sin(2.0 * D + 2.0 * Mp)
			+ 3660.0 * StrictMath.sin(4.0 * D)
			- 3549.0 * StrictMath.sin(2.0 * D + 2.0 * F)
			- 2469.0 * StrictMath.sin(3.0 * Mp - 2.0 * D)
			+ 2249.0 * E * StrictMath.sin(2.0 * D - M - 2.0 * Mp)
			- 2073.0 * StrictMath.sin(2.0 * D - 3.0 * Mp)
			+ 2235.0 * StrictMath.sin(A1)
			+ 3820.0 * StrictMath.sin(A2)
			+ 1750.0 * StrictMath.sin(A3);

		final double moonGeocentricLong = L0 + Math.toRadians(Σl / 1000000.0);

		return MathHelper.mod2pi(moonGeocentricLong + deltaPsi);
	}

}

