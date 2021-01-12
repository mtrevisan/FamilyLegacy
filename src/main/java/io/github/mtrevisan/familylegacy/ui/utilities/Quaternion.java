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


//https://github.com/jherico/java-math/blob/master/src/main/java/org/saintandreas/math/Quaternion.java
//http://home.apache.org/~luc/commons-math-3.6-RC2-site/jacoco/org.apache.commons.math3.complex/Quaternion.java.html
public class Quaternion{

	/** Exponent offset in IEEE754 representation. */
	private static final long EXPONENT_OFFSET = 1023l;
	/**
	 * Safe minimum, such that {@code 1 / SAFE_MIN} does not overflow.
	 * <br/>
	 * In IEEE 754 arithmetic, this is also the smallest normalized number 2<sup>-1022</sup>.
	 */
	private static final double SAFE_MIN = Double.longBitsToDouble((EXPONENT_OFFSET - 1022l) << 52);


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
	 * @param a	Scalar component.
	 * @param x	First vector component.
	 * @param y	Second vector component.
	 * @param z	Third vector component.
	 */
	public Quaternion(final double a, final double x, final double y, final double z){
		this.w = a;
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
		return Math.sqrt(w * w + x * x + y * y + z * z);
	}

	/**
	 * Computes the normalized quaternion (the versor of the instance).
	 * <p>The norm of the quaternion must not be zero.</p>
	 *
	 * @return	A normalized quaternion.
	 * @throws ZeroException	If the norm of the quaternion is zero.
	 */
	public Quaternion normalize() throws ZeroException{
		final double normSquared = w * w + x * x + y * y + z * z;
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

}
