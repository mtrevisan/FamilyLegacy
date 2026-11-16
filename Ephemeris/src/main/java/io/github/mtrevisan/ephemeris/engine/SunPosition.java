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
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;


public final class SunPosition{

	/**
	 * A VSOP2013 series, whose terms refer to the table of the sines and cosines of the multiples of the arguments (see
	 * {@link #angleTables(double)}): the multiplier {@code m} of the argument {@code i} is the slot
	 * {@code SLOT_BASE[i] + MAX_MULTIPLIER[i] + m}.
	 */
	private record Series(int timePower, double[] sines, double[] cosines, int[] termStart, int[] slots){}

	private static final int ARGUMENTS = 17;

	/** The greatest absolute multiplier of each argument in the whole theory. */
	private static final int[] MAX_MULTIPLIER = new int[ARGUMENTS];
	/** The first slot of the table of each argument (the slots of an argument are the multipliers from -max to +max). */
	private static final int[] SLOT_BASE = new int[ARGUMENTS + 1];

	private static final Map<ResourceReader.VariableIndex, List<Series>> EMB_DATA;
	static{
		final Map<ResourceReader.VariableIndex, List<ResourceReader.VSOP2013Data>> raw;
		try{
			// VSOP2013 Planetary Theory
			raw = ResourceReader.readData("VSOP2013p3.dat");
		}
		catch(final IOException e){
			throw new UncheckedIOException("Cannot read the VSOP2013 data", e);
		}

		for(final List<ResourceReader.VSOP2013Data> seriesList : raw.values())
			for(final ResourceReader.VSOP2013Data data : seriesList)
				for(int i = 0; i < data.argumentIndex.length; i ++)
					MAX_MULTIPLIER[data.argumentIndex[i]] = Math.max(MAX_MULTIPLIER[data.argumentIndex[i]],
						Math.abs(data.argumentMultiplier[i]));
		for(int i = 0; i < ARGUMENTS; i ++)
			SLOT_BASE[i + 1] = SLOT_BASE[i] + 2 * MAX_MULTIPLIER[i] + 1;

		final Map<ResourceReader.VariableIndex, List<Series>> data = new EnumMap<>(ResourceReader.VariableIndex.class);
		for(final Map.Entry<ResourceReader.VariableIndex, List<ResourceReader.VSOP2013Data>> entry : raw.entrySet()){
			final List<Series> converted = new ArrayList<>(entry.getValue().size());
			for(final ResourceReader.VSOP2013Data series : entry.getValue()){
				final int[] slots = new int[series.argumentIndex.length];
				for(int i = 0; i < slots.length; i ++){
					final int argument = series.argumentIndex[i];
					slots[i] = SLOT_BASE[argument] + MAX_MULTIPLIER[argument] + series.argumentMultiplier[i];
				}
				converted.add(new Series(series.timePower, series.sines, series.cosines, series.argumentStart, slots));
			}
			data.put(entry.getKey(), converted);
		}
		EMB_DATA = data;
	}

	// Standard Moon-Earth mass ratio factor
	private static final double MU = 0.0123000383;
	private static final double MU_FACTOR = MU / (1 + MU);

	// [km]
	private static final double ASTRONOMICAL_UNIT = 149_597_870.691;

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
		final double[] rMoonJ2000Km = MoonPosition.getInstanceLLR()
			.rectangular(jce);
		return apparentSunLongitude(jce, deltaPsi, rMoonJ2000Km);
	}

	/**
	 * Computes the apparent ecliptic longitude of the Sun with respect to the Earth, from an already computed position
	 * of the Moon.
	 *
	 * @param jce          Julian Ephemeris Century of Barycentric Dynamical Time (TDB) from J2000.0
	 * @param deltaPsi     nutation in longitude [rad]
	 * @param rMoonJ2000Km the geocentric rectangular coordinates of the Moon at {@code jce} in J2000.0 [km], as returned
	 *                     by {@link MoonPosition#rectangular(double)}
	 * @return the apparent longitude of the Sun [rad], reduced to the interval [0, 2π)
	 */
	public static double apparentSunLongitude(final double jce, final double deltaPsi, final double[] rMoonJ2000Km){
		// 1. Calculate the heliocentric spherical coordinates of the EMB in J2000.0 (VSOP2013 uses millennia)
		final double jme = jce / 10.;
		final EclipticCoordinates embSph = SunPosition.embTrueHeliocentricPosition(jme);

		// 2. Convert the EMB spherical coordinates back to rectangular J2000.0 components (in AU)
		final double cosLat = StrictMath.cos(embSph.latitude());
		final double xEmbJ2000 = embSph.distance() * StrictMath.cos(embSph.longitude()) * cosLat;
		final double yEmbJ2000 = embSph.distance() * StrictMath.sin(embSph.longitude()) * cosLat;
		final double zEmbJ2000 = embSph.distance() * StrictMath.sin(embSph.latitude());

		// 3. Convert the Moon's vector from kilometers to Astronomical Units (AU)
		final double xMoonAu = rMoonJ2000Km[0] / ASTRONOMICAL_UNIT;
		final double yMoonAu = rMoonJ2000Km[1] / ASTRONOMICAL_UNIT;
		final double zMoonAu = rMoonJ2000Km[2] / ASTRONOMICAL_UNIT;

		// 4. Apply the standard mass ratio factor to isolate the true geometric position of the Earth in J2000.0
		//    Formula: R_earth = R_emb - (mu / (1 + mu)) * r_moon
		final double xEarthJ2000 = xEmbJ2000 - SunPosition.MU_FACTOR * xMoonAu;
		final double yEarthJ2000 = yEmbJ2000 - SunPosition.MU_FACTOR * yMoonAu;
		final double zEarthJ2000 = zEmbJ2000 - SunPosition.MU_FACTOR * zMoonAu;

		// 5. Invert the vector orientation to obtain the geocentric geometric position of the Sun in J2000.0
		final double xSunJ2000 = -xEarthJ2000;
		final double ySunJ2000 = -yEarthJ2000;
		final double zSunJ2000 = -zEarthJ2000;

		// 6. Rotate the geocentric Sun vector from the J2000.0 frame to the mean ecliptic and equinox of date
		//    Note: This passes the individual components to your updated transpose matrix conversion method
		final double[] rSunDate = Converter.toRectangularFromJ2000ToDate(jce, xSunJ2000, ySunJ2000, zSunJ2000);

		// 7. Extract the mean ecliptic longitude and the true geometric distance (radius vector R)
		final double lambdaMean = MathHelper.mod2pi(StrictMath.atan2(rSunDate[1], rSunDate[0]));
		final double distance = StrictMath.sqrt(rSunDate[0] * rSunDate[0] + rSunDate[1] * rSunDate[1]
			+ rSunDate[2] * rSunDate[2]);

		// 8. Apply physical corrections for annual aberration and nutation to obtain the final apparent longitude
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
		final double[][] tables = angleTables(jme);
		final double[] sinTable = tables[0];
		final double[] cosTable = tables[1];
		final double a = evaluateVariable(ResourceReader.VariableIndex.A, jme, sinTable, cosTable);
		final double lambda = MathHelper.mod2pi(evaluateVariable(ResourceReader.VariableIndex.L, jme, sinTable, cosTable));
		final double k = evaluateVariable(ResourceReader.VariableIndex.K, jme, sinTable, cosTable);
		final double h = evaluateVariable(ResourceReader.VariableIndex.H, jme, sinTable, cosTable);
		final double q = evaluateVariable(ResourceReader.VariableIndex.Q, jme, sinTable, cosTable);
		final double p = evaluateVariable(ResourceReader.VariableIndex.P, jme, sinTable, cosTable);

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
		final double latitude = StrictMath.asin(z / distance);
		final double longitude = MathHelper.mod2pi(StrictMath.atan2(y, x));
		return EclipticCoordinates.create(latitude, longitude, distance);
	}

	/**
	 * The sines and the cosines of all the multiples of the arguments that the theory uses (the multiples from -max to +max
	 * of each argument, in the slots of {@link #SLOT_BASE}).
	 * <p>
	 * The phase of a term is a sum of multiples of the arguments, so its sine and cosine are obtained by composing the
	 * ones of the multiples with the angle addition formulas (a few multiplications) instead of calling the sine and the
	 * cosine for each of the ~300k terms.
	 * </p>
	 *
	 * @param t Julian millennia
	 * @return the sines and the cosines
	 */
	private static double[][] angleTables(final double t){
		final double[] sinTable = new double[SLOT_BASE[ARGUMENTS]];
		final double[] cosTable = new double[SLOT_BASE[ARGUMENTS]];
		for(int i = 0; i < ARGUMENTS; i ++){
			final double argument = MathHelper.mod2pi(ARGUMENT_PHASE[i] + ARGUMENT_RATE[i] * t);
			final int max = MAX_MULTIPLIER[i];
			final int zero = SLOT_BASE[i] + max;
			sinTable[zero] = 0.;
			cosTable[zero] = 1.;
			for(int m = 1; m <= max; m ++){
				final double sin = Math.sin(m * argument);
				final double cos = Math.cos(m * argument);
				sinTable[zero + m] = sin;
				cosTable[zero + m] = cos;
				sinTable[zero - m] = -sin;
				cosTable[zero - m] = cos;
			}
		}
		return new double[][]{sinTable, cosTable};
	}

	/**
	 * Evaluate VSOP2013 variable terms by summing up coefficients across time powers.
	 */
	private static double evaluateVariable(final ResourceReader.VariableIndex variable, final double jme,
			final double[] sinTable, final double[] cosTable){
		double total = 0.;
		final List<Series> seriesList = EMB_DATA.get(variable);
		for(int k = 0, size = seriesList.size(); k < size; k ++){
			final Series series = seriesList.get(k);

			final double[] sines = series.sines();
			final double[] cosines = series.cosines();
			final int[] termStart = series.termStart();
			final int[] slots = series.slots();
			double sum = 0.;
			//hot loop over ~300k terms: sin(phase) and cos(phase) of each term are composed from the tables
			for(int j = 0, terms = sines.length; j < terms; j ++){
				int i = termStart[j];
				final int end = termStart[j + 1];
				if(i == end){
					//no argument: the phase is zero
					sum += cosines[j];
					continue;
				}

				int slot = slots[i];
				double sin = sinTable[slot];
				double cos = cosTable[slot];
				for(i ++; i < end; i ++){
					slot = slots[i];
					final double sinNext = sinTable[slot];
					final double cosNext = cosTable[slot];
					final double newSin = sin * cosNext + cos * sinNext;
					cos = cos * cosNext - sin * sinNext;
					sin = newSin;
				}
				sum += sines[j] * sin + cosines[j] * cos;
			}
			total += sum * StrictMath.pow(jme, series.timePower());
		}
		return total;
	}

}
