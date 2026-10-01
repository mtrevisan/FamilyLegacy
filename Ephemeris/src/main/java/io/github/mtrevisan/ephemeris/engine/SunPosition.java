/**
 * Copyright (c) 2023 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
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

import io.github.mtrevisan.ephemeris.helpers.ResourceReader;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;


/*
https://github.com/mikereedell/sunrisesunsetlib-java

https://web.archive.org/web/20161202180207/http://williams.best.vwh.net/sunrise_sunset_algorithm.htm
https://ebvalaim.pl/en/2015/12/22/calculating-sunrise-and-sunset-times/
https://en.wikipedia.org/wiki/Position_of_the_Sun
https://edwilliams.org/sunrise_sunset_algorithm.htm

others
https://github.com/MenoData/Time4J/blob/master/base/src/main/java/net/time4j/calendar/astro/SolarTime.java
https://github.com/shred/commons-suncalc/blob/master/src/main/java/org/shredzone/commons/suncalc/SunTimes.java
https://github.com/buelowp/sunset/blob/master/src/sunset.cpp

https://frinklang.org/frinksamp/sun.frink
https://www.astrouw.edu.pl/~jskowron/pracownia/praca/sunspot_answerbook_expl/expl-5.html
http://co2.aos.wisc.edu/data/code/idl-lib/util/sunrise.pro
https://ebvalaim.pl/en/2015/12/22/calculating-sunrise-and-sunset-times/

https://www.iers.org/SharedDocs/Publikationen/EN/IERS/Publications/tn/TechnNote13/tn13.pdf?__blob=publicationFile&v=1
https://squarewidget.com/solar-coordinates/

https://dspace.mit.edu/bitstream/handle/1721.1/112396/1013461937-MIT.pdf
https://github.com/asif31iqbal/adhanalarm/blob/master/AdhanAlarm/src/net/sourceforge/jitl/astro/AstroLib.java
https://www.scielo.br/j/jatm/a/ZZ6L3gWKF9LpvW8tLcQPx8p/?lang=en&format=pdf
https://ftp.imcce.fr/pub/ephem/planets/vsop2013/solution/
https://hal-mines-paristech.archives-ouvertes.fr/hal-00725987/document
*/
public final class SunPosition{

	private static Map<ResourceReader.VariableIndex, List<ResourceReader.VSOP2013Data>> EARTH_HELIOCENTRIC_DATA;
	static{
		try{
			//VSOP2013 Planetary Theory
			EARTH_HELIOCENTRIC_DATA = ResourceReader.readData("VSOP2013p3.dat");
		}
		catch(final IOException ignored){}
	}


	//https://neoprogrammics.com/obliquity-of-the-ecliptic/laskar_paper/index.php
	//https://www.aanda.org/articles/aa/pdf/2003/48/aa4068.pdf
	private static final double[] OBLIQUITY_COEFFS = {
		84381.448, -4680.93, -1.55, 1999.25, -51.38, -249.67, -39.05, 7.12, 27.87, 5.79, 2.45
	};

//	static final double EARTH_FLATTENING = 1. / 298.25642;
//	//[m]
//	static final double EARTH_EQUATORIAL_RADIUS = 6378136.6;
//	private static final double[] EARTH_ORBIT_ECCENTRICITY = {0.016_708_634, -0.000_042_037, -0.000_000_126_7};

	/** Sun equatorial radius [m]. */
	public static final double SUN_EQUATORIAL_RADIUS = 6.95700e8;
	/**
	 * Astronomical unit [m], <code>±4 m</code>.
	 *
	 * <a href="https://iers-conventions.obspm.fr/content/tn36.pdf">IERS Conventions 2010</a>
	 * <a href="https://perfectastronomy.com/astronomy-course/astronomical-constants/"></a>
	 */
	public static final double ASTRONOMICAL_UNIT = 149_597_870_691.;

//	private static final double[] SUN_GEOCENTRIC_MEAN_LONGITUDE_PARAMETERS = {280.466_46, 36_000.769_83, 0.000_303_2};
//	private static final double[] SUN_GEOCENTRIC_MEAN_ANOMALY_PARAMETERS = {357.527_723_33, 35_999.050_34, -0.000_160_28, -0.000_003_33};
//	private static final double[] SUN_EQUATION_OF_CENTER_1 = {1.914_602, -0.004_817, -0.000_014};
//	private static final double[] SUN_EQUATION_OF_CENTER_2 = {0.019_993, -0.000_101};
//
//	//http://vadimchazov.narod.ru/besmfore.htm
//
//	private static final double[] MOON_GEOCENTRIC_MEAN_LONGITUDE = {218.3165, 481_267.8813};

	//["]
	private static final double ABERRATION_CONSTANT = 20.495_51;


	private SunPosition(){}


	/**
	 * Calculated the Sun equatorial position.
	 *
	 * @param jme	Julian Ephemeris Millennium of Terrestrial Time from J2000.0.
	 * @param deltaPsi	Nutation in longitude [rad].
	 * @param trueEclipticObliquity	Obliquity of the ecliptic, corrected for nutation [rad].
	 * @return	The equatorial position of the Sun with respect to Earth.
	 *
	 * @see <a href="https://squarewidget.com/solar-coordinates/">Solar coordinates</a>
	 */
	public static EquatorialCoordinate sunEquatorialPosition(final double jme, final double deltaPsi, final double trueEclipticObliquity){
		final EclipticCoordinate sunGeocentricPosition = sunGeocentricPosition(jme);

		//calculate aberration correction, <code>delta tau</code> [rad]
		final double aberrationCorrection = aberrationCorrection(sunGeocentricPosition.getDistance());

		//calculate the apparent sun longitude, <code>lambda</code> [rad]
		final double apparentSunLongitude = sunGeocentricPosition.getLongitude() + deltaPsi + aberrationCorrection;

		//calculate the geocentric sun right ascension [rad]
		final double rightAscension = geocentricSunRightAscension(sunGeocentricPosition.getLatitude(), trueEclipticObliquity,
			apparentSunLongitude);
		//calculate geocentric sun declination [rad]
		final double declination = geocentricSunDeclination(sunGeocentricPosition.getLatitude(), trueEclipticObliquity,
			apparentSunLongitude);

		return EquatorialCoordinate.create(rightAscension, declination);
	}

	/**
	 * Calculated the Earth heliocentric position.
	 *
	 * @param jme	Julian Ephemeris Millennium of Terrestrial Time from J2000.0.
	 * @return	The Earth heliocentric position.
	 */
	public static EclipticCoordinate earthHeliocentricPosition(final double jme){
		//calculate Earth heliocentric latitude, <code>B</code> [rad]
		final double earthHeliocentricLatitude = earthHeliocentricLatitude(jme);
		//calculate Earth heliocentric longitude, <code>L</code> [rad]
		final double earthHeliocentricLongitude = earthHeliocentricLongitude(jme);
		//calculate Earth radius vector, <code>R</code> [AU]
		final double earthRadiusVector = radiusVector(jme);

		return EclipticCoordinate.create(earthHeliocentricLatitude, earthHeliocentricLongitude, earthRadiusVector);
	}

	/**
	 * Calculated the Sun geocentric position.
	 *
	 * @param jme	Julian Ephemeris Millennium of Terrestrial Time from J2000.0.
	 * @return	The Sun geocentric position.
	 */
	public static EclipticCoordinate sunGeocentricPosition(final double jme){
		final EclipticCoordinate earthHeliocentricPosition = earthHeliocentricPosition(jme);

		//calculate geocentric latitude, <code>beta</code> [rad]
		final double sunGeocentricLatitude = -earthHeliocentricPosition.getLatitude();
		//calculate geocentric longitude, <code>theta</code> [rad]
		final double sunGeocentricLongitude = MathHelper.mod2pi(earthHeliocentricPosition.getLongitude() + StrictMath.PI);

		return EclipticCoordinate.create(sunGeocentricLatitude, sunGeocentricLongitude, earthHeliocentricPosition.getDistance());
	}

	/**
	 * Calculated the Sun apparent longitude.
	 *
	 * @param jme	Julian Ephemeris Millennium of Terrestrial Time from J2000.0.
	 * @param deltaPsi	Nutation in longitude [rad].
	 * @return	The apparent longitude of the Sun with respect to Earth.
	 *
	 * @see <a href="https://squarewidget.com/solar-coordinates/">Solar coordinates</a>
	 */
	public static double apparentSunLongitude(final double jme, final double deltaPsi){
		//calculate Earth heliocentric longitude, <code>L</code> [rad]
		final double earthHeliocentricLongitude = earthHeliocentricLongitude(jme);
		//calculate Earth radius vector, <code>R</code> [AU]
		final double earthRadiusVector = radiusVector(jme);

		//calculate geocentric longitude, <code>theta</code> [rad]
		final double sunGeocentricLongitude = MathHelper.mod2pi(earthHeliocentricLongitude + StrictMath.PI);

		//calculate aberration correction, <code>delta tau</code> [deg]
		final double aberrationCorrection = aberrationCorrection(earthRadiusVector);

		//calculate the apparent sun longitude, <code>lambda</code> [rad]
		return sunGeocentricLongitude + deltaPsi + aberrationCorrection;
	}

	/**
	 * Calculate the correction for aberration, the annual aberration (<code>∆τ</code>).
	 * <p>
	 * As the Earth revolves around the Sun, it is moving at a velocity of approximately 29.78 km/s. The speed of light is approximately
	 * 300,000 km/s. In the special case where the Earth is moving perpendicularly to the direction of the star, the angle of displacement,
	 * would therefore be (in radians) the ratio of the two velocities, i.e. <code>vₑ = 2 ⋅ π ⋅ 1 AU / (365.25 ⋅ d) = 29.75</code>,
	 * and <code>vₑ / c = 0.00009935 rad</code>, or about <code>20.5 arcseconds</code>.
	 * <br/>
	 * This quantity is known as the constant of aberration, and is conventionally represented by <code>κ</code>. Its precise accepted value
	 * is <code>20".49552</code> (at J2000).
	 * </p>
	 *
	 * @param earthRadiusVector	Earth radius vector [AU].
	 * @return	The correction for aberration [rad].
	 */
	public static double aberrationCorrection(final double earthRadiusVector){
		return StrictMath.toRadians(
			-ABERRATION_CONSTANT / (JulianDate.SECONDS_PER_HOUR * earthRadiusVector)
		);
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
	 * Calculate the heliocentric mean latitude of the Earth, referred to the mean equinox of the date, <code>β</code>.
	 *
	 * @param jme	Julian Ephemeris Millennium of Terrestrial Time from J2000.0.
	 * @return	The ecliptical mean latitude of the Sun [rad].
	 *
	 * @see <a href="https://squarewidget.com/solar-coordinates/">Solar coordinates</a>
	 */
	public static double earthHeliocentricLatitude(final double jme){
		final double[] parameters = new double[6];
		for(int i = 0; i < parameters.length; i ++){
			final Collection<double[]> elements = EARTH_HELIOCENTRIC_DATA.get("B" + i);
			if(elements != null){
				double parameter = 0.;
				for(final double[] element : elements)
					parameter += element[0] * StrictMath.cos(element[1] + element[2] * jme);
				parameters[i] = parameter;
			}
		}
		return MathHelper.modpipi(
			MathHelper.polynomial(jme, parameters) / 100_000_000.
		);
	}

	/**
	 * Calculate the heliocentric mean longitude of the Earth, referred to the mean equinox of the date, <code>L</code>.
	 *
	 * @param jme	Julian Ephemeris Millennium of Terrestrial Time from J2000.0.
	 * @return	The ecliptical mean longitude of the Sun [rad].
	 *
	 * @see <a href="https://squarewidget.com/solar-coordinates/">Solar coordinates</a>
	 */
	public static double earthHeliocentricLongitude(final double jme){
		final double[] parameters = new double[6];
		for(int i = 0; i < parameters.length; i ++){
			final Collection<double[]> elements = EARTH_HELIOCENTRIC_DATA.get("L" + i);
			if(elements != null){
				double parameter = 0.;
				for(final double[] element : elements)
					parameter += element[0] * StrictMath.cos(element[1] + element[2] * jme);
				parameters[i] = parameter;
			}
		}
		return MathHelper.mod2pi(
			MathHelper.polynomial(jme, parameters) / 100_000_000.
		);
	}

	/**
	 * Calculate the distance between the center of the Sun and the center of the Earth, <code>R</code>.
	 * <p>U.S. Naval Observatory function.</p>
	 *
	 * @param jme	Julian Ephemeris Millennium of Terrestrial Time from J2000.0.
	 * @return	Distance between the center of the Sun and the center of the Earth [AU].
	 */
	public static double radiusVector(final double jme){
		final double[] parameters = new double[6];
		for(int i = 0; i < parameters.length; i ++){
			final Collection<double[]> elements = EARTH_HELIOCENTRIC_DATA.get("R" + i);
			if(elements != null){
				double parameter = 0.;
				for(final double[] element : elements)
					parameter += element[0] * StrictMath.cos(element[1] + element[2] * jme);
				parameters[i] = parameter;
			}
		}
		return MathHelper.polynomial(jme, parameters) / 100_000_000.;
	}


	/**
	 * Calculate the mean obliquity of the ecliptic, <code>ɛ0</code>.
	 *
	 * @param jce	Julian Century of Terrestrial Time from J2000.0.
	 * @return	Apparent longitude of the Sun [rad].
	 */
	public static double meanEclipticObliquity(final double jce){
		return StrictMath.toRadians(
			MathHelper.polynomial(jce / 100., OBLIQUITY_COEFFS) / JulianDate.SECONDS_PER_HOUR
		);
	}

	/**
	 * True obliquity of the ecliptic corrected for nutation, <code>ɛ</code>.
	 *
	 * @param meanEclipticObliquity	Obliquity of the ecliptic, corrected for parallax [rad].
	 * @param obliquityNutation	Corrections of nutation in obliquity (<code>∆ε</code>) [rad].
	 * @return	Apparent longitude of the Sun [rad].
	 */
	public static double trueEclipticObliquity(final double meanEclipticObliquity, final double obliquityNutation){
		return meanEclipticObliquity + obliquityNutation;
	}


//	/**
//	 * Calculated the Sun topocentric position.
//	 *
//	 * @param location	Location of the place.
//	 * @param atmosphericModel	Atmospheric model of the place.
//	 * @param eclipticCoord	Mean ecliptic coordinate of the Sun.
//	 * @param jd	Julian Day of Terrestrial Time from J2000.0.
//	 * @return	The Sun topocentric position.
//	 */
//	public static HorizontalCoordinate sunTopocentricPosition(final GeographicLocation location, final AtmosphericModel atmosphericModel,
//			final EclipticCoordinate eclipticCoord, final double jd){
//		final LocalDateTime date = JulianDate.dateTimeOf(jd);
//		final double dt = TimeHelper.deltaT(date.getYear());
//		final double ut = TimeHelper.terrestrialTimeToUniversalTime(jd, dt);
//		final double jce = JulianDate.centuryJ2000Of(jd);
//
//		final EquatorialCoordinate equatorialCoord = sunEquatorialPosition(eclipticCoord, jd);
//		final double sunMeanAnomaly = SunPosition.geocentricMeanAnomaly(jce);
//		final double[] nutation = nutationCorrection(sunMeanAnomaly, jce);
//		final double meanEclipticObliquity = meanEclipticObliquity(jce);
//		final double trueEclipticObliquity = trueEclipticObliquity(meanEclipticObliquity, nutation[1]);
//
//		final double meanSiderealTime = TimeHelper.greenwichMeanSiderealTime(ut);
//		final double apparentSiderealTime = TimeHelper.greenwichApparentSiderealTime(meanSiderealTime, trueEclipticObliquity, nutation[0]);
//		final double localMeanSiderealTime = TimeHelper.localMeanSiderealTime(apparentSiderealTime, location);
//		final double rightAscension = equatorialCoord.getRightAscension();
//		final double localHourAngle = TimeHelper.localHourAngle(localMeanSiderealTime, rightAscension);
//
//		//compute the sun position (right ascension and declination) with respect to the observer local position at the Earth surface:
//		final double equatorialHorizontalParallax = equatorialHorizontalParallax(eclipticCoord.getDistance());
//		final double latitude = location.getLatitude();
//		final double u = StrictMath.atan((1. - EARTH_FLATTENING) * StrictMath.tan(latitude));
//		final double height = location.getAltitude() / EARTH_EQUATORIAL_RADIUS;
//		final double x = StrictMath.cos(u) + height * StrictMath.cos(latitude);
//		final double y = 0.99664719 * StrictMath.sin(u) + height * StrictMath.sin(latitude);
//		final double declination = equatorialCoord.getDeclination();
//		final double deltaRightAscension = StrictMath.atan2(
//			-x * StrictMath.sin(equatorialHorizontalParallax) * StrictMath.sin(localHourAngle),
//			StrictMath.cos(declination) - x * StrictMath.sin(equatorialHorizontalParallax)
//				* StrictMath.cos(localHourAngle)
//		);
//		//calculate the topocentric Sun Right Ascension: α'
//		//final double topocentricRightAscension = rightAscension + deltaRightAscension;
//		//calculate the topocentric Sun declination: δ'
//		final double topocentricDeclination = StrictMath.atan2(
//			(StrictMath.sin(declination) - y * StrictMath.sin(equatorialHorizontalParallax)) * StrictMath.cos(deltaRightAscension),
//			StrictMath.cos(declination) - y * StrictMath.sin(equatorialHorizontalParallax) * StrictMath.cos(localHourAngle)
//		);
//		//calculate the topocentric local hour angle: H’
//		final double topocentricLocalHourAngle = localHourAngle - deltaRightAscension;
//		//calculate the true elevation angle without atmospheric refraction correction: e0
//		final double trueElevation = StrictMath.asin(
//			StrictMath.sin(latitude) * StrictMath.sin(topocentricDeclination)
//				+ StrictMath.cos(latitude) * StrictMath.cos(topocentricDeclination) * StrictMath.cos(topocentricLocalHourAngle)
//		);
//		//calculate the atmospheric refraction correction: Δe
//		final double deltaElevation = atmosphericModel.atmosphericRefractionCorrection(trueElevation);
//		//calculate the topocentric elevation angle: e
//		final double topocentricElevation = MathHelper.modpi(trueElevation + deltaElevation);
//		//calculate the topocentric zenith angle: θ
//		//final double topocentricZenith = StrictMath.PI / 2. - topocentricElevation;
//		//calculate the topocentric astronomers azimuth angle (measured westward from south): Γ
//		final double topocentricAzimuth = StrictMath.atan2(
//			StrictMath.sin(topocentricLocalHourAngle),
//			StrictMath.cos(topocentricLocalHourAngle) * StrictMath.sin(latitude)
//				- StrictMath.tan(topocentricDeclination) * StrictMath.cos(latitude)
//		);
//		//calculate the (navigators) topocentric azimuth angle (measured westward from north): M
//		final double topocentricAzimuthNavigators = MathHelper.mod2pi(topocentricAzimuth + StrictMath.PI);
//		return HorizontalCoordinate.create(topocentricAzimuthNavigators, topocentricElevation, eclipticCoord.getDistance());
//	}


//	/**
//	 * Calculate the Sun's equation of center, <code>C</code>.
//	 *
//	 * @param geocentricMeanAnomaly	The mean anomaly of the Sun [rad].
//	 * @param tt	Julian Century of Terrestrial Time from J2000.0.
//	 * @return	The Sun's equation of center [rad].
//	 */
//	public static double equationOfCenter(final double geocentricMeanAnomaly, final double tt){
//		return Math.toRadians(
//			MathHelper.polynomial(tt, SUN_EQUATION_OF_CENTER_1) * StrictMath.sin(geocentricMeanAnomaly)
//				+ MathHelper.polynomial(tt, SUN_EQUATION_OF_CENTER_2) * StrictMath.sin(2. * geocentricMeanAnomaly)
//				+ 0.000_289 * StrictMath.sin(3. * geocentricMeanAnomaly)
//		);
//	}
//
//	/**
//	 * Calculate the Sun's true longitude, <code>☉ = Ltrue</code>.
//	 *
//	 * @param meanLongitude	The geocentric mean longitude of the Sun [rad].
//	 * @param equationOfCenter	The Sun's equation of center [rad].
//	 * @return	The Sun's true longitude [rad].
//	 */
//	public static double trueLongitude(final double meanLongitude, final double equationOfCenter){
//		return MathHelper.mod2pi(meanLongitude + equationOfCenter);
//	}
//
//	/**
//	 * Calculate the Sun's true anomaly, <code>v</code>.
//	 *
//	 * @param meanAnomaly	The geocentric mean longitude of the Sun [rad].
//	 * @param equationOfCenter	The Sun's equation of center [rad].
//	 * @return	The Sun's true anomaly [rad].
//	 */
//	public static double trueAnomaly(final double meanAnomaly, final double equationOfCenter){
//		return MathHelper.mod2pi(meanAnomaly + equationOfCenter);
//	}
//
//	/**
//	 * Calculate the geocentric apparent longitude of the Sun, referred to the true equinox of the date, <code>λ = Lapp</code>.
//	 *
//	 * @param geocentricTrueLongitude	Suns geocentric true longitude [rad].
//	 * @param ascendingLongitudeMoon	Longitude of the ascending node of the Moon's mean orbit on the ecliptic [rad].
//	 * @param sunMeanAnomaly	The geocentric mean anomaly of the Sun [rad].
//	 * @param jce	Julian Century of Terrestrial Time from J2000.0.
//	 * @return	The apparent true longitude of the Sun [rad].
//	 */
//	public static double apparentLongitude(final double geocentricTrueLongitude, final double ascendingLongitudeMoon, final double sunMeanAnomaly, final double jce){
//		final double nutationCorrection = nutationAndAberrationCorrection(ascendingLongitudeMoon, sunMeanAnomaly, jce);
//		return geocentricTrueLongitude - nutationCorrection;
//	}
//
//	/**
//	 * Calculate the Sun's apparent declination.
//	 *
//	 * @param meanEclipticObliquity	Mean obliquity of the ecliptic [rad].
//	 * @param apparentLongitude	The Sun's apparent longitude [rad].
//	 * @return	The Sun's apparent declination [rad].
//	 */
//	public static double apparentDeclination(final double meanEclipticObliquity, final double apparentLongitude){
//		return StrictMath.asin(StrictMath.sin(meanEclipticObliquity) * StrictMath.sin(apparentLongitude));
//	}
//
//	/**
//	 * Calculate the geocentric mean longitude of the Moon, referred to the mean equinox of the date, <code>L'</code>.
//	 *
//	 * @param tt	Julian Century of Terrestrial Time from J2000.0.
//	 * @return	The geocentric mean longitude of the Moon [rad].
//	 */
//	private static double moonGeocentricMeanLongitude(final double tt){
//		return MathHelper.mod2pi(StrictMath.toRadians(MathHelper.polynomial(tt, MOON_GEOCENTRIC_MEAN_LONGITUDE)));
//	}
//
//	/**
//	 * Calculate the eccentricity of Earth's orbit, <code>e</code>.
//	 *
//	 * @param jce	Julian Century of Terrestrial Time from J2000.0.
//	 * @return	The eccentricity of Earth's orbit.
//	 */
//	public static double earthOrbitEccentricity(final double jce){
//		return MathHelper.polynomial(jce, EARTH_ORBIT_ECCENTRICITY);
//	}
//
//
//	/**
//	 * Calculate corrections of nutation in longitude (<code>∆ψ</code>) and obliquity (<code>∆ε</code>).
//	 *
//	 * @param sunMeanAnomaly	The geocentric mean anomaly of the Sun [rad].
//	 * @param jce	Julian Century of Terrestrial Time from J2000.0.
//	 * @return	An array where the first element is <code>∆ψ</code> [rad], and the second <code>∆ε</code> [rad].
//	 *
//	 * http://physics.uwyo.edu/~wiro/planet/nutate.c
//	 * https://iopscience.iop.org/article/10.1086/375641/pdf
//	 */
//	public static double[] nutationCorrection(final double sunMeanAnomaly, final double jce){
//		final double d = meanElongationMoonSun(jce);
//		final double m = sunMeanAnomaly;
//		final double mp = meanAnomalyMoon(jce);
//		final double f = argumentLatitudeMoon(jce);
//		final double omega = ascendingLongitudeMoon(jce);
//
//		final Collection<Double[]> elements = NUTATION_DATA.get("coeffs");
//		final double[] x = {d, m, mp, f, omega};
//		double deltaPsi = 0.;
//		double deltaEpsilon = 0.;
//		for(final Double[] element : elements){
//			final double parameter = xyTermSummation(x, element);
//			deltaPsi += (element[x.length] + element[x.length + 1] * jce) * StrictMath.sin(parameter);
//			deltaEpsilon += (element[x.length + 2] + element[x.length + 3] * jce) * StrictMath.cos(parameter);
//		}
//		return new double[]{
//			StrictMath.toRadians(deltaPsi / (JulianDate.SECONDS_PER_HOUR * 10_000.)),
//			StrictMath.toRadians(deltaEpsilon / (JulianDate.SECONDS_PER_HOUR * 10_000.))
//		};
//	}
//
//	//mean elongation of the Moon from the Sun [rad]
//	private static double meanElongationMoonSun(final double tt){
//		return MathHelper.mod2pi(StrictMath.toRadians(
//			MathHelper.polynomial(tt, MOON_MEAN_ELONGATION_PARAMETERS)
//		));
//	}
//
//	/**
//	 * Calculate the geocentric mean longitude of the Sun, <code>L0</code>.
//	 *
//	 * @param tdb	Julian Century of Terrestrial Time from J2000.0.
//	 * @return	The geocentric mean longitude of the Sun [rad].
//	 */
//	public static double geocentricMeanLongitude(final double tdb){
//		return MathHelper.mod2pi(StrictMath.toRadians(
//			MathHelper.polynomial(tdb, SUN_GEOCENTRIC_MEAN_LONGITUDE_PARAMETERS)
//		));
//	}
//
//	/**
//	 * Calculate the geocentric mean anomaly of the Sun, <code>M</code>.
//	 *
//	 * @param tdb	Julian Century of Terrestrial Time from J2000.0.
//	 * @return	The geocentric mean anomaly of the Sun [rad].
//	 */
//	public static double geocentricMeanAnomaly(final double tdb){
//		return MathHelper.mod2pi(StrictMath.toRadians(
//			MathHelper.polynomial(tdb, SUN_GEOCENTRIC_MEAN_ANOMALY_PARAMETERS)
//		));
//	}
//
//	//mean anomaly of the Moon [rad]
//	private static double meanAnomalyMoon(final double tt){
//		return MathHelper.mod2pi(StrictMath.toRadians(
//			MathHelper.polynomial(tt, MOON_MEAN_ANOMALY_PARAMETERS)
//		));
//	}
//
//	//Moon's argument of Latitude [rad]
//	private static double argumentLatitudeMoon(final double tt){
//		return MathHelper.mod2pi(StrictMath.toRadians(
//			MathHelper.polynomial(tt, MOON_ARGUMENT_OF_LATITUDE)
//		));
//	}
//
//	/**
//	 * Longitude of the ascending node of the Moon's mean orbit on the ecliptic measured from the mean equinox of the date, <code>☊</code>.
//	 *
//	 * @param tt	Julian Century of Terrestrial Time from J2000.0.
//	 * @return	Longitude of the ascending node of the Moon's mean orbit on the ecliptic [rad].
//	 */
//	public static double ascendingLongitudeMoon(final double tt){
//		return MathHelper.mod2pi(StrictMath.toRadians(
//			MathHelper.polynomial(tt, MOON_LONGITUDE_ASCENDING_NODE)
//		));
//	}
//
//	private static double xyTermSummation(final double[] x, final Double[] terms){
//		double result = 0.;
//		for(int j = 0; j < x.length; j ++)
//			result += x[j] * terms[j];
//		return result;
//	}

//	/**
//	 * Calculate the apparent longitude of the Sun, <code>Lapp = λ</code>.
//	 *
//	 * @param geocentricMeanLongitude	Sun's true geocentric longitude [rad].
//	 * @param deltaPsi	Nutation in longitude [rad].
//	 * @param deltaAberration	Aberration [rad].
//	 * @return	Apparent longitude of the Sun [rad].
//	 */
//	public static double apparentGeocentricLongitude(final double geocentricMeanLongitude, final double deltaPsi,
//			final double deltaAberration){
//		return geocentricMeanLongitude + deltaPsi + deltaAberration;
//	}

//	/**
//	 * Calculate the mean obliquity of the ecliptic, corrected for parallax, <code>ɛ'</code>.
//	 *
//	 * @param meanEclipticObliquity	Mean obliquity of the ecliptic [rad].
//	 * @param tt	Julian Century of Terrestrial Time from J2000.0.
//	 * @return	Apparent longitude of the Sun [rad].
//	 */
//	public static double apparentEclipticObliquity(final double meanEclipticObliquity, final double tt){
//		return meanEclipticObliquity + StrictMath.toRadians(0.002_56 * StrictMath.cos(ascendingLongitudeMoon(tt)));
//	}

//
//	/**
//	 * Calculate the equatorial horizontal parallax of the Sun, <code>ξ</code>.
//	 *
//	 * @param radiusVector	Radius vector of the Earth [AU].
//	 * @return	The equatorial horizontal parallax of the Sun [rad].
//	 */
//	public static double equatorialHorizontalParallax(double radiusVector){
//		return StrictMath.toRadians(8.794 / (JulianDate.SECONDS_PER_HOUR * radiusVector));
//	}

}
