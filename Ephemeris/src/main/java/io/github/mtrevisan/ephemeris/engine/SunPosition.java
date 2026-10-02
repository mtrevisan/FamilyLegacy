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

import io.github.mtrevisan.ephemeris.engine.coordinates.Converter;
import io.github.mtrevisan.ephemeris.engine.coordinates.EclipticCoordinates;
import io.github.mtrevisan.ephemeris.helpers.JulianDate;
import io.github.mtrevisan.ephemeris.helpers.MathHelper;
import io.github.mtrevisan.ephemeris.readers.ResourceReader;

import java.io.IOException;
import java.util.List;
import java.util.Map;


public final class SunPosition{

	private static Map<ResourceReader.VariableIndex, List<ResourceReader.VSOP2013Data>> EMB_DATA;
	static{
		try{
			// VSOP2013 Planetary Theory
			EMB_DATA = ResourceReader.readData("VSOP2013p3.dat");
		}
		catch(final IOException ignored){}
	}

	// Standard Moon-Earth mass ratio factor
	private static final double MU = 0.0123000383;
	private static final double MU_FACTOR = MU / (1 + MU);

	// [km]
	private static final double ASTRONOMICAL_UNIT = 149_597_870.691;

	private static final int ARGUMENTS = 17;
	//ll(i) = phase + rate * T [rad], T = TDB Julian millennia from J2000.0 (VSOP2013 README)
	private static final double[] ARGUMENT_PHASE = {
		4.402608631669, 3.176134461576, 1.753470369433, 6.203500014141, 4.091360003050, 1.713740719173,
		5.598641292287, 2.805136360408, 2.326989734620, 0.599546107035, 0.874018510107, 5.481225395663,
		5.311897933164, 0., 5.198466400630, 1.627905136020, 2.355555638750
	};
	private static final double[] ARGUMENT_RATE = {
		26087.90314068555, 10213.28554743445, 6283.075850353215, 3340.612434145457, 1731.170452721855,
		1704.450855027201, 1428.948917844273, 1364.756513629990, 1361.923207632842, 529.6909615623250,
		213.2990861084880, 74.78165903077800, 38.13297222612500, 0.3595362285049309, 77713.7714481804,
		84334.6615717837, 83286.9142477147
	};

	private static final double ABERRATION_CONSTANT = 20.495_51;


	private SunPosition(){}


	/**
	 * Calculate the Earth heliocentric position.
	 *
	 * @param jme Julian Ephemeris Millennium of Terrestrial Time from J2000.0.
	 * @return The Earth heliocentric position.
	 */
	public static EclipticCoordinates embTrueHeliocentricPosition(final double jme){
		return earthMoonBarycenterHeliocentricPosition(jme);
	}

	/**
	 * Computes the apparent ecliptic longitude of the Sun with respect to the Earth.
	 * <p>
	 * This method utilizes the existing heliocentric spherical coordinates of the EMB,
	 * converts them back to rectangular J2000.0 components, subtracts the Moon's
	 * displacement to isolate the true Earth center, and then applies precession,
	 * nutation, and annual aberration.
	 * </p>
	 *
	 * @param jce      Julian Ephemeris Century of Barycentric Dynamical Time (TDB) from J2000.0
	 * @param deltaPsi nutation in longitude [rad]
	 * @return the apparent longitude of the Sun [rad], reduced to the interval [0, 2π)
	 */
	public static double apparentSunLongitude(final double jce, final double deltaPsi){
		// 1. Calculate the heliocentric spherical coordinates of the EMB in J2000.0 (VSOP2013 uses millennia)
		final double jme = jce / 10.;
		final EclipticCoordinates embSph = SunPosition.embTrueHeliocentricPosition(jme);

		// 2. Convert the EMB spherical coordinates back to rectangular J2000.0 components (in AU)
		final double cosLat = StrictMath.cos(embSph.getLatitude());
		final double xEmbJ2000 = embSph.getDistance() * StrictMath.cos(embSph.getLongitude()) * cosLat;
		final double yEmbJ2000 = embSph.getDistance() * StrictMath.sin(embSph.getLongitude()) * cosLat;
		final double zEmbJ2000 = embSph.getDistance() * StrictMath.sin(embSph.getLatitude());

		// 3. Retrieve the geocentric rectangular coordinates of the Moon in J2000.0 (in kilometers)
		final double[] rMoonJ2000Km = MoonPosition.getInstanceLLR()
			.rectangular(jce);

		// 4. Convert the Moon's vector from kilometers to Astronomical Units (AU)
		final double xMoonAu = rMoonJ2000Km[0] / ASTRONOMICAL_UNIT;
		final double yMoonAu = rMoonJ2000Km[1] / ASTRONOMICAL_UNIT;
		final double zMoonAu = rMoonJ2000Km[2] / ASTRONOMICAL_UNIT;

		// 5. Apply the standard mass ratio factor to isolate the true geometric position of the Earth in J2000.0
		//    Formula: R_earth = R_emb - (mu / (1 + mu)) * r_moon
		final double xEarthJ2000 = xEmbJ2000 - SunPosition.MU_FACTOR * xMoonAu;
		final double yEarthJ2000 = yEmbJ2000 - SunPosition.MU_FACTOR * yMoonAu;
		final double zEarthJ2000 = zEmbJ2000 - SunPosition.MU_FACTOR * zMoonAu;

		// 6. Invert the vector orientation to obtain the geocentric geometric position of the Sun in J2000.0
		final double xSunJ2000 = -xEarthJ2000;
		final double ySunJ2000 = -yEarthJ2000;
		final double zSunJ2000 = -zEarthJ2000;

		// 7. Rotate the geocentric Sun vector from the J2000.0 frame to the mean ecliptic and equinox of date
		//    Note: This passes the individual components to your updated transpose matrix conversion method
		final double[] rSunDate = Converter.toRectangularFromJ2000ToDate(jce, xSunJ2000, ySunJ2000, zSunJ2000);

		// 8. Extract the mean ecliptic longitude and the true geometric distance (radius vector R)
		final double lambdaMean = MathHelper.mod2pi(StrictMath.atan2(rSunDate[1], rSunDate[0]));
		final double distance = StrictMath.sqrt(rSunDate[0] * rSunDate[0] + rSunDate[1] * rSunDate[1]
			+ rSunDate[2] * rSunDate[2]);

		// 9. Apply physical corrections for annual aberration and nutation to obtain the final apparent longitude
		final double aberration = SunPosition.aberrationCorrection(distance);

		return MathHelper.mod2pi(lambdaMean + deltaPsi + aberration);
	}

	/**
	 * Computes the angular correction for annual solar aberration (∆τ).
	 * <p>
	 * This accounts for the apparent displacement of the Sun caused by the
	 * finite speed of light combined with the orbital velocity of the Earth.
	 * The constant 20.4955 arcseconds is divided by 3600 to convert to degrees
	 * before being scaled into radians by the Earth's radius vector.
	 * </p>
	 *
	 * @param earthRadiusVector the geometric distance between the true Earth center and the Sun [AU]
	 * @return the aberration correction angle [rad]
	 */
	public static double aberrationCorrection(final double earthRadiusVector){
		return StrictMath.toRadians(-ABERRATION_CONSTANT / (JulianDate.SECONDS_PER_HOUR * earthRadiusVector));
	}

	/**
	 * Heliocentric ecliptic coordinates of the Earth-Moon barycenter referred to the inertial mean ecliptic and dynamical equinox J2000.0.
	 *
	 * @param jme Julian Ephemeris Millennium of Barycentric Dynamical Time from J2000.0.
	 * @return The ecliptical true latitude [rad], true longitude [rad], and radius vector [AU].
	 */
	private static EclipticCoordinates earthMoonBarycenterHeliocentricPosition(final double jme){
		final double[] ll = arguments(jme);
		final double a = evaluateVariable(ResourceReader.VariableIndex.A, jme, ll);
		final double lambda = MathHelper.mod2pi(evaluateVariable(ResourceReader.VariableIndex.L, jme, ll));
		final double k = evaluateVariable(ResourceReader.VariableIndex.K, jme, ll);
		final double h = evaluateVariable(ResourceReader.VariableIndex.H, jme, ll);
		final double q = evaluateVariable(ResourceReader.VariableIndex.Q, jme, ll);
		final double p = evaluateVariable(ResourceReader.VariableIndex.P, jme, ll);

		//solve lambda = F - k sin F + h cos F for the eccentric longitude F (Newton)
		double f = lambda;
		for(int i = 0; i < 20; i ++){
			final double delta = (f - k * StrictMath.sin(f) + h * StrictMath.cos(f) - lambda)
				/ (1. - k * StrictMath.cos(f) - h * StrictMath.sin(f));
			f -= delta;
			if(StrictMath.abs(delta) < 1.e-15)
				break;
		}

		final double sinF = StrictMath.sin(f);
		final double cosF = StrictMath.cos(f);
		final double beta = 1. / (1. + StrictMath.sqrt(1. - k * k - h * h));
		//position in the orbital plane, with longitudes measured from the reference axis
		final double xp = a * ((1. - beta * h * h) * cosF + beta * h * k * sinF - k);
		final double yp = a * ((1. - beta * k * k) * sinF + beta * h * k * cosF - h);
		//rotation about the line of nodes (q = sin(i/2) cos(W), p = sin(i/2) sin(W))
		final double x = xp * (1. - 2. * p * p) + yp * 2. * p * q;
		final double y = xp * 2. * p * q + yp * (1. - 2. * q * q);
		final double z = 2. * StrictMath.sqrt(1. - p * p - q * q) * (yp * q - xp * p);

		final double distance = StrictMath.sqrt(x * x + y * y + z * z);
		return EclipticCoordinates.create(
			StrictMath.asin(z / distance),
			MathHelper.mod2pi(StrictMath.atan2(y, x)),
			distance);
	}

	private static double[] arguments(final double t){
		final double[] ll = new double[ARGUMENTS];
		for(int i = 0; i < ARGUMENTS; i ++)
			ll[i] = MathHelper.mod2pi(ARGUMENT_PHASE[i] + ARGUMENT_RATE[i] * t);
		return ll;
	}

	/**
	 * Evaluate VSOP2013 variable terms by summing up coefficients across time powers.
	 */
	private static double evaluateVariable(final ResourceReader.VariableIndex variable, final double jme,
		final double[] ll){
		double total = 0.;
		for(final ResourceReader.VSOP2013Data series : EMB_DATA.get(variable)){
			double sum = 0.;
			for(final ResourceReader.VSOP2013Coeffs coeff : series.coeffs){
				double phase = 0.;
				for(int i = 0; i < ARGUMENTS; i ++)
					phase += coeff.iphi[i] * ll[i];
				sum += coeff.sine * StrictMath.sin(phase) + coeff.cosine * StrictMath.cos(phase);
			}
			total += sum * StrictMath.pow(jme, series.timePower);
		}
		return total;
	}

}
