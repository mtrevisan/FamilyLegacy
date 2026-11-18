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
package io.github.mtrevisan.ephemeris.readers;

import java.util.List;


/**
 * Immutable container for the ELP/MPP02 coefficients loaded from the 14
 * data files.
 *
 * <p>The coefficients are split into the <b>main problem</b> (three series
 * of 4-argument terms, one per coordinate) and the <b>perturbations</b>
 * (eleven series of 13-argument terms, multiplied by successive powers of
 * the time variable T).</p>
 *
 * <p>The main-problem series carry their final amplitude, already corrected
 * by the factors {@code fA}, {@code fB1}..{@code fB5} from the chosen
 * parameter set (LLR or DE405). The perturbation series carry their raw
 * amplitude and phase, which are independent of the parameter set.</p>
 *
 * @param mainLongitude           ----- Main problem -----
 * @param perturbationLongitudeT0 ----- Perturbations, longitude -----
 * @param perturbationLatitudeT0  ----- Perturbations, latitude -----
 * @param perturbationDistanceT0  ----- Perturbations, distance -----
 */
public record ElpCoefficients(List<MainTerm> mainLongitude, List<MainTerm> mainLatitude, List<MainTerm> mainDistance,
										List<PertTerm> perturbationLongitudeT0, List<PertTerm> perturbationLongitudeT1,
										List<PertTerm> perturbationLongitudeT2, List<PertTerm> perturbationLongitudeT3,
										List<PertTerm> perturbationLatitudeT0, List<PertTerm> perturbationLatitudeT1,
										List<PertTerm> perturbationLatitudeT2, List<PertTerm> perturbationDistanceT0,
										List<PertTerm> perturbationDistanceT1, List<PertTerm> perturbationDistanceT2,
										List<PertTerm> perturbationDistanceT3){

	/**
	 * A term of the main-problem series: four integer multipliers of the
	 * Delaunay arguments and one corrected amplitude.
	 */
	public record MainTerm(int i1, int i2, int i3, int i4, double amplitude){}

	/**
	 * A term of a perturbation series: thirteen integer multipliers of the
	 * fundamental arguments, one amplitude and one phase offset.
	 */
	public record PertTerm(int[] multipliers, double amplitude, double phase){}


}
