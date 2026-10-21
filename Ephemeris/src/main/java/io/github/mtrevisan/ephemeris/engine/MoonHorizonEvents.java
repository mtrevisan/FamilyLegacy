package io.github.mtrevisan.ephemeris.engine;

import io.github.mtrevisan.ephemeris.helpers.DeltaT;
import io.github.mtrevisan.ephemeris.helpers.JulianDate;
import io.github.mtrevisan.ephemeris.helpers.MathHelper;

import java.util.function.Function;


/**
 * High-precision lunar horizon event calculator utilizing ELP/MPP02 planetary theory,
 * topocentric parallax adjustments, and dynamical semidiameter variations.
 *
 * @author Mauro Trevisan
 */
public final class MoonHorizonEvents{

	// Standard atmospheric refraction at the horizon: 34 arcminutes
	private static final double HORIZON_REFRACTION = StrictMath.toRadians(34. / 60.);

	// Earth equatorial radius in kilometers (WGS-84 standard)
	private static final double EARTH_EQUATORIAL_RADIUS_KM = 6378.137;


	private MoonHorizonEvents(){}


	/**
	 * Calculates the exact Julian Date (Universal Time) of moonset for a specific civil day and location.
	 *
	 * @param midnightUtJd      The Julian Date representing local civil midnight converted to UT (ending in .5)
	 * @param latitude          The observer's geodetic latitude in degrees (North positive)
	 * @param longitude         The observer's geodetic longitude in degrees (East positive)
	 * @param ttCenturyFunction Functional bridge to calculate Terrestrial Centuries from a Julian Date UT
	 * @return The Julian Date (UT) of the moonset event, or Double.NaN if the Moon is circumpolar
	 */
	public static double calculateMoonset(final double midnightUtJd, final double latitude, final double longitude,
			final Function<Double, Double> ttCenturyFunction){
		final double latRad = StrictMath.toRadians(latitude);

		// 1. Coarse estimation of the moonset time using standard geometric projections
		final double approximateNoonUt = midnightUtJd + 0.5 - longitude / 360.;
		final double ttJcNoon = ttCenturyFunction.apply(approximateNoonUt);

		// Fetch geocentric moon position elements
		final double[] rMoonKmNoon = MoonPosition.getInstanceLLR().rectangular(ttJcNoon);
		final double distanceNoon = StrictMath.sqrt(rMoonKmNoon[0] * rMoonKmNoon[0]
			+ rMoonKmNoon[1] * rMoonKmNoon[1] + rMoonKmNoon[2] * rMoonKmNoon[2]);

		final NutationCorrections nutationNoon = NutationCorrections.calculate(ttJcNoon);
		final double deltaPsiNoon = nutationNoon.getDeltaPsi();
		final double lambdaMoonNoon = MathHelper.mod2pi(MoonPosition.computeApparentMoonLongitude(ttJcNoon,
			deltaPsiNoon));

		final double epsilon0Noon = StrictMath.toRadians(23.43929111 - 46.836769 * ttJcNoon / 3600.);
		final double trueObliquityNoon = epsilon0Noon + nutationNoon.getDeltaEpsilon();
		final double declinationNoon = StrictMath.asin(StrictMath.sin(trueObliquityNoon)
			* StrictMath.sin(lambdaMoonNoon));

		// Compute dynamic target altitude for the coarse step
		final double semidiameterNoon = StrictMath.asin(EARTH_EQUATORIAL_RADIUS_KM * 0.2725 / distanceNoon);
		final double targetAltitudeNoon = -HORIZON_REFRACTION - semidiameterNoon;

		final double cosH0 = (StrictMath.sin(targetAltitudeNoon)
			- StrictMath.sin(latRad) * StrictMath.sin(declinationNoon))
			/ (StrictMath.cos(latRad) * StrictMath.cos(declinationNoon));

		if(cosH0 > 1. || cosH0 < -1.)
			// Moon is circumpolar
			return Double.NaN;

		final double h0Deg = StrictMath.toDegrees(StrictMath.acos(cosH0));
		final double approximateMoonsetFraction = (12. + (h0Deg - longitude) / 15.) / 24.;
		final double targetMoonsetUtJd = midnightUtJd + approximateMoonsetFraction;

		// 2. High-Precision Refinement Framework via Three-Point Inverse Interpolation
		final double hourInJd = 1. / 24.;

		// To track structural changes over a dynamic horizon, we store the threshold limits per point
		final double[] targetThresholds = new double[3];
		final double t1 = targetMoonsetUtJd - hourInJd;
		final double t2 = targetMoonsetUtJd;
		final double t3 = targetMoonsetUtJd + hourInJd;

		final double y1 = computeTopocentricMoonAltitude(t1, latRad, longitude, ttCenturyFunction, targetThresholds, 0);
		final double y2 = computeTopocentricMoonAltitude(t2, latRad, longitude, ttCenturyFunction, targetThresholds, 1);
		final double y3 = computeTopocentricMoonAltitude(t3, latRad, longitude, ttCenturyFunction, targetThresholds, 2);

		// Interpolate the functional differences against the dynamic altitude limits
		final double a = (y1 + y3) / 2. - y2;
		final double b = (y3 - y1) / 2.;
		final double c = y2 - targetThresholds[1];

		final double discriminant = b * b - 4. * a * c;
		if(discriminant < 0.)
			return targetMoonsetUtJd;

		double correctionFactor = (-b + StrictMath.sqrt(discriminant)) / (2. * a);
		if(StrictMath.abs(correctionFactor) > 1.)
			correctionFactor = (-b - StrictMath.sqrt(discriminant)) / (2. * a);

		return targetMoonsetUtJd + (correctionFactor * hourInJd);
	}

	/**
	 * Computes the apparent topocentric local altitude of the Moon, applying parallax adjustments
	 * and logging the dynamic geometric horizon boundaries.
	 */
	private static double computeTopocentricMoonAltitude(final double utJd, final double latRad, final double longitude,
			final Function<Double, Double> ttCenturyFunction, final double[] thresholds, final int thresholdIdx){
		final double ttJc = ttCenturyFunction.apply(utJd);
		final double jcUt = JulianDate.centuryJ2000Of(utJd);

		// 1. Fetch geocentric raw rectangular vectors from ELP series
		final double[] rGeocentricKm = MoonPosition.getInstanceLLR().rectangular(ttJc);
		final double distanceKm = StrictMath.sqrt(rGeocentricKm[0] * rGeocentricKm[0]
			+ rGeocentricKm[1] * rGeocentricKm[1] + rGeocentricKm[2] * rGeocentricKm[2]);

		// Extract apparent equatorial angles
		final NutationCorrections nutation = NutationCorrections.calculate(ttJc);
		final double deltaPsi = nutation.getDeltaPsi();
		final double lambdaApparent = MoonPosition.computeApparentMoonLongitude(ttJc, deltaPsi);

		final double epsilon0 = StrictMath.toRadians(23.43929111 - 46.836769 * ttJc / 3600.);
		final double trueObliquity = epsilon0 + nutation.getDeltaEpsilon();

		final double rightAscension = MathHelper.mod2pi(StrictMath.atan2(
			StrictMath.sin(lambdaApparent) * StrictMath.cos(trueObliquity), StrictMath.cos(lambdaApparent)));
		final double declination = StrictMath.asin(StrictMath.sin(trueObliquity) * StrictMath.sin(lambdaApparent));

		// 2. Compute true Earth spatial orientation (GAST)
		final double gmst = computeGMST(utJd, jcUt);
		final double gast = gmst + deltaPsi * StrictMath.cos(trueObliquity);
		final double localSiderealTime = gast + StrictMath.toRadians(longitude);
		final double hourAngle = localSiderealTime - rightAscension;

		// 3. Transform geocentric parameters into local topocentric coordinates to eliminate parallax distortion
		final double cosDec = StrictMath.cos(declination);
		final double sinDec = StrictMath.sin(declination);
		final double cosLat = StrictMath.cos(latRad);
		final double sinLat = StrictMath.sin(latRad);

		// Compute coordinate distances scaled against the Earth's radius
		final double deltaX = distanceKm * cosDec * StrictMath.cos(hourAngle) - EARTH_EQUATORIAL_RADIUS_KM * cosLat;
		final double deltaY = distanceKm * cosDec * StrictMath.sin(hourAngle);
		final double deltaZ = distanceKm * sinDec - EARTH_EQUATORIAL_RADIUS_KM * sinLat;

		// Topocentric distance and corrected horizontal orientation
		final double topocentricDistKm = StrictMath.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
		final double topocentricDec = StrictMath.asin(deltaZ / topocentricDistKm);
		final double topocentricHourAngle = StrictMath.atan2(deltaY, deltaX);

		// Compute true topocentric altitude
		final double topocentricAltitude = StrictMath.asin(sinLat * StrictMath.sin(topocentricDec)
			+ cosLat * StrictMath.cos(topocentricDec) * StrictMath.cos(topocentricHourAngle));

		// 4. Log the dynamic structural upper-limb edge criteria for the interpolation matrix
		final double dynamicSemidiameter = StrictMath.asin(EARTH_EQUATORIAL_RADIUS_KM * 0.2725 / topocentricDistKm);
		thresholds[thresholdIdx] = -HORIZON_REFRACTION - dynamicSemidiameter;

		return topocentricAltitude;
	}

	private static double computeGMST(final double utJd, final double jcUt){
		final double utFraction = (utJd + 0.5) % 1.;
		double gmstArcseconds = 24110.54841 + 8640184.812866 * jcUt + 0.093104 * jcUt * jcUt - 6.2e-6 * jcUt * jcUt * jcUt;
		gmstArcseconds += utFraction * 86400.0 * 1.002737909350795;
		return MathHelper.mod2pi(StrictMath.toRadians(gmstArcseconds / 240.0));
	}

}
