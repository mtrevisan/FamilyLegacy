package io.github.mtrevisan.ephemeris.engine;

import io.github.mtrevisan.ephemeris.helpers.MathHelper;
import io.github.mtrevisan.ephemeris.helpers.ResourceReader;

import java.io.IOException;
import java.util.List;


/**
 * @see <a href="https://www.research.unipd.it/retrieve/e14fb26f-e069-3de1-e053-1705fe0ac030/PhD_Thesis_Zoccarato.pdf">Determinazione Orbitale Precisa di satelliti in orbita LEO per la Radio Occultazione attraverso sistemi GNSS</a>
 */
public class NutationCorrections{

	private static List<double[]> NUTATION_DATA;
	static{
		try{
			NUTATION_DATA = ResourceReader.readPlain("nutation.dat");
		}
		catch(final IOException ignored){}
	}


	/** IAU 1980 coefficients */
	public static final double[] MOON_MEAN_ELONGATION_COEFFS = {297.85036, 445267.111480, -0.0019142, 0.0000053};
	private static final double[] SUN_GEOCENTRIC_MEAN_ANOMALY_COEFFS = {357.52772, 35999.050340, -0.0001603, -0.0000003};
	private static final double[] MOON_MEAN_ANOMALY_COEFFS = {134.96298, 477198.867398, 0.0086972, 0.0000178};
	private static final double[] MOON_ARGUMENT_OF_LATITUDE_COEFFS = {93.27209, 483202.017527, -0.0036825, 0.0000031};
	private static final double[] MOON_LONGITUDE_ASCENDING_NODE_COEFFS = {125.04452, -1934.136261, 0.0020708, 0.0000022};
	private static final double[][] NUTATION_COEFFS = {
		MOON_MEAN_ELONGATION_COEFFS,
		SUN_GEOCENTRIC_MEAN_ANOMALY_COEFFS,
		MOON_MEAN_ANOMALY_COEFFS,
		MOON_ARGUMENT_OF_LATITUDE_COEFFS,
		MOON_LONGITUDE_ASCENDING_NODE_COEFFS
	};


	/** Calculate <code>Δψ</code> [rad]. */
	private final double deltaPsi;
	/** Calculate <code>Δε</code> [rad]. */
	private final double deltaEpsilon;


	public static NutationCorrections calculate(final double ut){
		return new NutationCorrections(ut);
	}


	/**
	 * Calculate nutation corrections following IAU 1980.
	 *
	 * @param jce	Julian Century of Terrestrial Time from J2000.0.
	 */
	private NutationCorrections(final double jce){
		//calculate nutation corrections
		final double[] nutationTerms = nutationTerms(jce);
		final double[] deltaPsiCoeffs = deltaPsiCoeffs(jce, nutationTerms, NUTATION_DATA);
		final double[] deltaEpsilonCoeffs = deltaEpsilonCoeffs(jce, nutationTerms, NUTATION_DATA);
		deltaPsi = deltaPsiEpsilon(deltaPsiCoeffs);
		deltaEpsilon = deltaPsiEpsilon(deltaEpsilonCoeffs);
	}

	private static double[] nutationTerms(final double jce){
		final double[] x = new double[NUTATION_COEFFS.length];
		for(int i = 0; i < NUTATION_COEFFS.length; i ++)
			x[i] = MathHelper.polynomial(jce, NUTATION_COEFFS[i]);
		return x;
	}

	private static double[] deltaPsiCoeffs(final double jce, final double[] nutationTerms, final List<double[]> elements){
		final double[] deltaPsiI = new double[elements.size()];
		for(int i = 0; i < deltaPsiI.length; i ++){
			final double[] params = elements.get(i);
			final double a = params[5];
			final double b = params[6];
			deltaPsiI[i] = (a + b * jce) * StrictMath.sin(calculateXYTermSum(i, nutationTerms, params));
		}
		return deltaPsiI;
	}

	private static double[] deltaEpsilonCoeffs(final double jce, final double[] nutationTerms, final List<double[]> elements){
		final double[] deltaEpsilonI = new double[elements.size()];
		for(int i = 0; i < deltaEpsilonI.length; i ++){
			final double[] params = elements.get(i);
			final double c = params[7];
			final double d = params[8];
			deltaEpsilonI[i] = (c + d * jce) * StrictMath.cos(calculateXYTermSum(i, nutationTerms, params));
		}
		return deltaEpsilonI;
	}

	private static double calculateXYTermSum(final int i, final double[] x, final double[] params){
		double result = 0.;
		for(int j = 0; j < x.length; j ++)
			result += x[j] * params[j];
		return StrictMath.toRadians(result);
	}

	private static double deltaPsiEpsilon(final double[] deltaPsiOrEpsilonI){
		double result = 0.;
		for(int i = 0; i < deltaPsiOrEpsilonI.length; i ++)
			result += deltaPsiOrEpsilonI[i];
		return StrictMath.toRadians(result / 36_000_000.);
	}

	/**
	 * @param jce	Julian Century of Terrestrial Time from J2000.0.
	 * @return	Longitude of the ascending node of the Moon [rad].
	 */
	public static double moonLongitudeAscendingNode(final double jce){
		return StrictMath.toRadians(MathHelper.polynomial(jce, MOON_LONGITUDE_ASCENDING_NODE_COEFFS));
	}


	public double getDeltaPsi(){
		return deltaPsi;
	}

	public double getDeltaEpsilon(){
		return deltaEpsilon;
	}

}

