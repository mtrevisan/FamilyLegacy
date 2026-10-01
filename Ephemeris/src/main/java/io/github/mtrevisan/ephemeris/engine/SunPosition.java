/**
 * Copyright (c) 2023 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.ephemeris.engine;

import io.github.mtrevisan.ephemeris.engine.coordinates.EclipticCoordinate;
import io.github.mtrevisan.ephemeris.engine.coordinates.EquatorialCoordinate;
import io.github.mtrevisan.ephemeris.helpers.JulianDate;
import io.github.mtrevisan.ephemeris.helpers.MathHelper;
import io.github.mtrevisan.ephemeris.readers.ResourceReader;

import java.io.IOException;
import java.util.List;
import java.util.Map;


public final class SunPosition{

	private static Map<ResourceReader.VariableIndex, List<ResourceReader.VSOP2013Data>> EARTH_HELIOCENTRIC_DATA;
	static{
		try{
			// VSOP2013 Planetary Theory
			EARTH_HELIOCENTRIC_DATA = ResourceReader.readData("VSOP2013p3.dat");
		}
		catch(final IOException ignored){}
	}

	private static final double[] OBLIQUITY_COEFFS = {
		84381.448, -4680.93, -1.55, 1999.25, -51.38, -249.67, -39.05, 7.12, 27.87, 5.79, 2.45
	};

	private static final double ABERRATION_CONSTANT = 20.495_51;


	private SunPosition(){}


	/**
	 * Calculate the Sun equatorial position.
	 *
	 * @param jme Julian Ephemeris Millennium of Terrestrial Time from J2000.0.
	 * @param deltaPsi Nutation in longitude [rad].
	 * @param trueEclipticObliquity Obliquity of the ecliptic, corrected for nutation [rad].
	 * @return The equatorial position of the Sun with respect to Earth.
	 */
	public static EquatorialCoordinate sunEquatorialPosition(final double jme, final double deltaPsi,
			final double trueEclipticObliquity){
		final EclipticCoordinate sunGeocentricPosition = sunGeocentricPosition(jme);
		final double aberrationCorrection = aberrationCorrection(sunGeocentricPosition.getDistance());
		final double apparentSunLongitude = sunGeocentricPosition.getLongitude() + deltaPsi + aberrationCorrection;

		final double rightAscension = geocentricSunRightAscension(sunGeocentricPosition.getLatitude(),
			trueEclipticObliquity, apparentSunLongitude);
		final double declination = geocentricSunDeclination(sunGeocentricPosition.getLatitude(), trueEclipticObliquity,
			apparentSunLongitude);

		return EquatorialCoordinate.create(rightAscension, declination);
	}

	/**
	 * Calculate the Earth heliocentric position.
	 *
	 * @param jme Julian Ephemeris Millennium of Terrestrial Time from J2000.0.
	 * @return The Earth heliocentric position.
	 */
	public static EclipticCoordinate earthHeliocentricPosition(final double jme){
		final double earthHeliocentricLatitude = earthHeliocentricLatitude(jme);
		final double earthHeliocentricLongitude = earthHeliocentricLongitude(jme);
		final double earthRadiusVector = radiusVector(jme);

		return EclipticCoordinate.create(earthHeliocentricLatitude, earthHeliocentricLongitude, earthRadiusVector);
	}

	/**
	 * Calculate the Sun geocentric position.
	 *
	 * @param jme Julian Ephemeris Millennium of Terrestrial Time from J2000.0.
	 * @return The Sun geocentric position.
	 */
	public static EclipticCoordinate sunGeocentricPosition(final double jme){
		final EclipticCoordinate earthHeliocentricPosition = earthHeliocentricPosition(jme);
		final double sunGeocentricLatitude = -earthHeliocentricPosition.getLatitude();
		final double sunGeocentricLongitude = MathHelper.mod2pi(earthHeliocentricPosition.getLongitude()
			+ StrictMath.PI);

		return EclipticCoordinate.create(sunGeocentricLatitude, sunGeocentricLongitude,
			earthHeliocentricPosition.getDistance());
	}

	/**
	 * Calculate the Sun apparent longitude.
	 *
	 * @param jme Julian Ephemeris Millennium of Terrestrial Time from J2000.0.
	 * @param deltaPsi Nutation in longitude [rad].
	 * @return The apparent longitude of the Sun with respect to Earth.
	 */
	public static double apparentSunLongitude(final double jme, final double deltaPsi){
		final double earthHeliocentricLongitude = earthHeliocentricLongitude(jme);
		final double earthRadiusVector = radiusVector(jme);
		final double sunGeocentricLongitude = MathHelper.mod2pi(earthHeliocentricLongitude + StrictMath.PI);
		final double aberrationCorrection = aberrationCorrection(earthRadiusVector);

		return sunGeocentricLongitude + deltaPsi + aberrationCorrection;
	}

	/**
	 * Calculate the correction for annual aberration (∆τ).
	 *
	 * @param earthRadiusVector Earth radius vector [AU].
	 * @return The correction for aberration [rad].
	 */
	public static double aberrationCorrection(final double earthRadiusVector){
		return StrictMath.toRadians(-ABERRATION_CONSTANT / (JulianDate.SECONDS_PER_HOUR * earthRadiusVector));
	}

	private static double geocentricSunRightAscension(final double beta, final double epsilon, final double lambda){
		return MathHelper.mod2pi(
			StrictMath.atan2(StrictMath.sin(lambda) * StrictMath.cos(epsilon)
				- StrictMath.tan(beta) * StrictMath.sin(epsilon), StrictMath.cos(lambda))
		);
	}

	private static double geocentricSunDeclination(final double beta, final double epsilon, final double lambda){
		return StrictMath.asin(StrictMath.sin(beta) * StrictMath.cos(epsilon)
			+ StrictMath.cos(beta) * StrictMath.sin(epsilon) * StrictMath.sin(lambda));
	}

	/**
	 * Evaluate VSOP2013 variable terms by summing up coefficients across time powers.
	 */
	private static double evaluateVariable(final ResourceReader.VariableIndex variable, final double jme){
		final List<ResourceReader.VSOP2013Data> seriesList = EARTH_HELIOCENTRIC_DATA.get(variable);
		if(seriesList == null || seriesList.isEmpty())
			return 0.;

		double totalSum = 0.;
		for(final ResourceReader.VSOP2013Data series : seriesList){
			double seriesSum = 0.;
			for(final ResourceReader.VSOP2013Coeffs coeff : series.coeffs)
				seriesSum += coeff.cosine * StrictMath.cos(coeff.sine) + coeff.sine * StrictMath.sin(coeff.cosine);
			totalSum += seriesSum * StrictMath.pow(jme, series.timePower);
		}
		return totalSum;
	}

	/**
	 * Calculate the heliocentric latitude of the Earth.
	 *
	 * @param jme Julian Ephemeris Millennium of Terrestrial Time from J2000.0.
	 * @return The ecliptical latitude [rad].
	 */
	public static double earthHeliocentricLatitude(final double jme){
		return MathHelper.modpipi(evaluateVariable(ResourceReader.VariableIndex.Q, jme));
	}

	/**
	 * Calculate the heliocentric longitude of the Earth.
	 *
	 * @param jme Julian Ephemeris Millennium of Terrestrial Time from J2000.0.
	 * @return The ecliptical longitude [rad].
	 */
	public static double earthHeliocentricLongitude(final double jme){
		return MathHelper.mod2pi(evaluateVariable(ResourceReader.VariableIndex.L, jme));
	}

	/**
	 * Calculate the radius vector (distance between Sun and Earth).
	 *
	 * @param jme Julian Ephemeris Millennium of Terrestrial Time from J2000.0.
	 * @return Distance [AU].
	 */
	public static double radiusVector(final double jme){
		return evaluateVariable(ResourceReader.VariableIndex.A, jme);
	}

	/**
	 * Calculate the mean obliquity of the ecliptic, ε0.
	 *
	 * @param jce Julian Century of Terrestrial Time from J2000.0.
	 * @return Mean obliquity of the ecliptic [rad].
	 */
	public static double meanEclipticObliquity(final double jce){
		return StrictMath.toRadians(
			MathHelper.polynomial(jce / 100., OBLIQUITY_COEFFS) / JulianDate.SECONDS_PER_HOUR
		);
	}

	/**
	 * Calculate the true obliquity of the ecliptic corrected for nutation, ε.
	 *
	 * @param meanEclipticObliquity Mean obliquity of the ecliptic [rad].
	 * @param obliquityNutation Corrections of nutation in obliquity (∆ε) [rad].
	 * @return True obliquity of the ecliptic [rad].
	 */
	public static double trueEclipticObliquity(final double meanEclipticObliquity, final double obliquityNutation){
		return meanEclipticObliquity + obliquityNutation;
	}

}
