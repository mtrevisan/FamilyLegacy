/**
 * Copyright (c) 2026 Mauro Trevisan
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
package io.github.mtrevisan.ephemeris.helpers;

import java.util.function.DoubleUnaryOperator;


/**
 * A robust numerical root-finder implementing the Illinois variant of the False Position method.
 * <p>
 * This solver guarantees convergence by scaling down the stagnant bracket value by half
 * whenever the root remains stuck on one side of the interval for two consecutive iterations.
 * </p>
 */
public final class IllinoisSolver{

	private IllinoisSolver(){}


	/**
	 * Finds a root within a tightly bound bracket [x0, x1].
	 *
	 * @param x0        the lower bound of the search window (e.g., Julian Date)
	 * @param x1        the upper bound of the search window (e.g., Julian Date)
	 * @param tolerance the target precision threshold (e.g., 1e-7 days ≈ 8 milliseconds)
	 * @param maxIter   maximum allowed iterations to guard against infinite loops
	 * @param f         the objective function mapping a coordinate to its angular delta
	 * @return the optimized root value (Julian Date)
	 */
	public static double solve(double x0, double x1, final double tolerance, final int maxIter,
			final DoubleUnaryOperator f){
		double y0 = f.applyAsDouble(x0);
		double y1 = f.applyAsDouble(x1);

		if(StrictMath.abs(y0) < tolerance)
			return x0;
		if(StrictMath.abs(y1) < tolerance)
			return x1;

		if(y0 * y1 > 0.)
			throw new IllegalArgumentException("The root is not strictly bracketed between the provided boundaries.");

		double x2 = x0;
		// Tracks side stagnation: -1 for left side, +1 for right side
		int side = 0;
		for(int i = 0; i < maxIter; i ++){
			// Linear interpolation to find the next guess
			x2 = (x0 * y1 - x1 * y0) / (y1 - y0);
			final double y2 = f.applyAsDouble(x2);

			if(StrictMath.abs(x1 - x0) < tolerance || StrictMath.abs(y2) < tolerance)
				return x2;

			if(y2 * y1 < 0.){
				// Root is between x2 and x1
				x0 = x1;
				y0 = y1;
				x1 = x2;
				y1 = y2;
				if(side == 1)
					// Illinois modification: scale down stagnant bracket
					y0 *= 0.5;
				side = 1;
			}
			else{
				// Root is between x0 and x2
				x1 = x2;
				y1 = y2;
				if(side == -1)
					// Illinois modification: scale down stagnant bracket
					y0 *= 0.5;
				side = -1;
			}
		}
		return x2;
	}

}
