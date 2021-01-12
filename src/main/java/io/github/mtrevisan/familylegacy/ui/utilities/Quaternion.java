/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.ui.utilities;


/**
 * Quaternions extends a rotation in three dimensions to a rotation in four dimensions.
 * <p>This avoids "gimbal lock" and allows for smooth continuous rotation.</p>
 *
 * @see <a href="https://github.com/jherico/java-math/blob/master/src/main/java/org/saintandreas/math/Quaternion.java">Quaternion</a>
 * @see <a href="http://home.apache.org/~luc/commons-math-3.6-RC2-site/jacoco/org.apache.commons.math3.complex/Quaternion.java.html">Quaternion</a>
 */
public class Quaternion{

	/** Exponent offset in IEEE754 representation. */
	private static final long EXPONENT_OFFSET = 1023l;
	/**
	 * Safe minimum, such that {@code 1 / SAFE_MIN} does not overflow.
	 * <br/>
	 * In IEEE 754 arithmetic, this is also the smallest normalized number 2<sup>-1022</sup>.
	 */
	private static final double SAFE_MIN = Double.longBitsToDouble((EXPONENT_OFFSET - 1022l) << 52);

	public static final Quaternion IDENTITY = new Quaternion(1., 0., 0., 0.);
	public static final Quaternion ZERO = new Quaternion(0., 0., 0., 0.);
	public static final Quaternion I = new Quaternion(0., 1., 0., 0.);
	public static final Quaternion J = new Quaternion(0., 0., 1., 0.);
	public static final Quaternion K = new Quaternion(0., 0., 0., 1.);


	private double x;
	private double y;
	private double z;
	private double w;


	/**
	 * Builds a Quaternion from the Euler rotation angles (x,y,z) aka (pitch, yaw, rall)).
	 * <p>Note that we are applying in order: (y, z, x) aka (yaw, roll, pitch) but we've ordered them in x, y, and z for convenience.</p>
	 *
	 * @param xAngle	The Euler pitch of rotation (in radians). (aka Attitude, often rotation around x)
	 * @param yAngle	The Euler yaw of rotation (in radians). (aka Heading, often rotation around y)
	 * @param zAngle	The Euler roll of rotation (in radians). (aka Bank, often rotation around z)
	 * @see <a href="http://www.euclideanspace.com/maths/geometry/rotations/conversions/eulerToQuaternion/index.htm">http://www.euclideanspace.com/maths/geometry/rotations/conversions/eulerToQuaternion/index.htm</a>
	 */
	public static Quaternion fromAngles(final double xAngle, final double yAngle, final double zAngle) throws ZeroException{
		double angle = zAngle * 0.5;
		final double sinZ = Math.sin(angle);
		final double cosZ = Math.cos(angle);
		angle = yAngle * 0.5;
		final double sinY = Math.sin(angle);
		final double cosY = Math.cos(angle);
		angle = xAngle * 0.5;
		final double sinX = Math.sin(angle);
		final double cosX = Math.cos(angle);

		//variables used to reduce multiplication calls
		final double cosYXcosZ = cosY * cosZ;
		final double sinYXsinZ = sinY * sinZ;
		final double cosYXsinZ = cosY * sinZ;
		final double sinYXcosZ = sinY * cosZ;

		final Quaternion q = new Quaternion();
		q.w = cosYXcosZ * cosX - sinYXsinZ * sinX;
		q.x = cosYXcosZ * sinX + sinYXsinZ * cosX;
		q.y = sinYXcosZ * cosX + cosYXsinZ * sinX;
		q.z = cosYXsinZ * cosX - sinYXcosZ * sinX;

		return q.normalize();
	}


	private Quaternion(){}

	/**
	 * Builds a quaternion from its components.
	 *
	 * @param w	Scalar component.
	 * @param x	First vector component.
	 * @param y	Second vector component.
	 * @param z	Third vector component.
	 */
	public Quaternion(final double w, final double x, final double y, final double z){
		this.w = w;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * Computes the norm of the quaternion.
	 *
	 * @return	The norm.
	 */
	public double getNorm(){
		return Math.sqrt(getNormSquared());
	}

	/**
	 * Computes the squared norm of the quaternion.
	 *
	 * @return	The norm.
	 */
	private double getNormSquared(){
		return dotProduct(this);
	}

	/**
	 * Computes the normalized quaternion (the versor of the instance).
	 * <p>The norm of the quaternion must not be zero.</p>
	 *
	 * @return	A normalized quaternion.
	 * @throws ZeroException	If the norm of the quaternion is zero.
	 */
	public Quaternion normalize() throws ZeroException{
		final double normSquared = getNormSquared();
		if(normSquared < SAFE_MIN)
			throw ZeroException.create("Cannot normalize quaternion: norm is near to zero ({})", Math.sqrt(normSquared));

		final double norm = Math.sqrt(normSquared);
		w /= norm;
		x /= norm;
		y /= norm;
		x /= norm;
		return this;
	}

	/**
	 * Returns the conjugate quaternion of the instance.
	 *
	 * @return	The conjugate quaternion.
	 */
	public Quaternion getConjugate(){
		return new Quaternion(w, -x, -y, -z);
	}

	/**
	 * Returns the inverse of this quaternion.
	 * <p>The norm of the quaternion must not be zero.</p>
	 *
	 * @return	The inverse.
	 * @throws ZeroException	If the norm of the quaternion is zero.
	 */
	public Quaternion getInverse() throws ZeroException{
		return getConjugate().normalize();
	}

	/**
	 * Returns the Hamilton product of the instance by a quaternion.
	 *
	 * @param q	Quaternion.
	 * @return	The product of this instance with {@code q}, in that order.
	 */
	public Quaternion multiply(final Quaternion q){
		return multiply(this, q);
	}

	/**
	 * Returns the Hamilton product of two quaternions.
	 *
	 * @param q1	First quaternion.
	 * @param q2	Second quaternion.
	 * @return	The product {@code q1} and {@code q2}, in that order.
	 */
	public static Quaternion multiply(final Quaternion q1, final Quaternion q2){
		//components of the first quaternion
		final double q1w = q1.w;
		final double q1x = q1.x;
		final double q1y = q1.y;
		final double q1z = q1.z;

		//components of the second quaternion
		final double q2w = q2.w;
		final double q2x = q2.x;
		final double q2y = q2.y;
		final double q2z = q2.z;

		//components of the product
		final double w = q1w * q2w - q1x * q2x - q1y * q2y - q1z * q2z;
		final double x = q1w * q2x + q1x * q2w + q1y * q2z - q1z * q2y;
		final double y = q1w * q2y - q1x * q2z + q1y * q2w + q1z * q2x;
		final double z = q1w * q2z + q1x * q2y - q1y * q2x + q1z * q2w;

		return new Quaternion(w, x, y, z);
	}

	/**
	 * Computes the dot-product of the instance by a quaternion.
	 *
	 * @param q	Quaternion.
	 * @return	The dot product of this instance and {@code q}.
	 */
	public double dotProduct(final Quaternion q){
		return dotProduct(this, q);
	}

	/**
	 * Computes the dot-product of two quaternions.
	 *
	 * @param q1	Quaternion.
	 * @param q2	Quaternion.
	 * @return	The dot product of {@code q1} and {@code q2}.
	 */
	public static double dotProduct(final Quaternion q1, final Quaternion q2){
		return (q1.w * q2.w + q1.x * q2.x + q1.y * q2.y + q1.z * q2.z);
	}

	public final void applyRotation(final double[] vector){
		applyRotation(vector, vector);
	}

	public final double[] applyRotation(final double[] vector, final double[] destination){
		//compute: x' = q * v * q^-1 = q * v * q* / (q · q) = q * v * q* (since it represents a rotation),
		//otherwise x' = (w^2 - q.v · q.v) * v + 2 * w * (q.v × v) + 2 * (q.v · v) * q.v
		final double vx = vector[0];
		final double vy = vector[1];
		final double vz = vector[2];
		final double w2 = 2. * w;
		final double dot2 = 2. * (x * vx + y * vy + z * vz);
		final double tmp = w * w - x * x - y * y - z * z;
		final double[] res = (destination != null? destination: new double[3]);
		res[0] = dot2 * x + w2 * (vy * z - vz * y) + tmp * vx;
		res[1] = dot2 * y + w2 * (vz * x - vx * z) + tmp * vy;
		res[2] = dot2 * z + w2 * (vx * y - vy * x) + tmp * vz;
		return res;
	}

	/**
	 * Checks whether the instance is a unit quaternion within a given tolerance.
	 *
	 * @param tolerance	Tolerance (absolute error).
	 * @return	Whether the norm is 1 within the given tolerance
	 */
	public boolean isUnitQuaternion(final double tolerance){
		return (Math.abs(getNormSquared() - 1.) < tolerance * tolerance);
	}

	@Override
	public String toString(){
		final StringBuilder sb = new StringBuilder();
		return sb.append("[")
			.append(w).append(' ')
			.append(x).append(' ')
			.append(y).append(' ')
			.append(z)
			.append("]")
			.toString();
	}

}
