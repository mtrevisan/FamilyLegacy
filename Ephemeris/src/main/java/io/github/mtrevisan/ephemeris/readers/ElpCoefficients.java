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
 */
public final class ElpCoefficients{

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


	/* ----- Main problem ----- */
	public final List<MainTerm> mainLongitude;
	public final List<MainTerm> mainLatitude;
	public final List<MainTerm> mainDistance;

	/* ----- Perturbations, longitude ----- */
	public final List<PertTerm> perturbationLongitudeT0;
	public final List<PertTerm> perturbationLongitudeT1;
	public final List<PertTerm> perturbationLongitudeT2;
	public final List<PertTerm> perturbationLongitudeT3;

	/* ----- Perturbations, latitude ----- */
	public final List<PertTerm> perturbationLatitudeT0;
	public final List<PertTerm> perturbationLatitudeT1;
	public final List<PertTerm> perturbationLatitudeT2;

	/* ----- Perturbations, distance ----- */
	public final List<PertTerm> perturbationDistanceT0;
	public final List<PertTerm> perturbationDistanceT1;
	public final List<PertTerm> perturbationDistanceT2;
	public final List<PertTerm> perturbationDistanceT3;


	public ElpCoefficients(final List<MainTerm> mainLongitude, final List<MainTerm> mainLatitude,
			final List<MainTerm> mainDistance,
			final List<PertTerm> perturbationLongitudeT0, final List<PertTerm> perturbationLongitudeT1,
			final List<PertTerm> perturbationLongitudeT2, final List<PertTerm> perturbationLongitudeT3,
			final List<PertTerm> perturbationLatitudeT0, final List<PertTerm> perturbationLatitudeT1,
			final List<PertTerm> perturbationLatitudeT2,
			final List<PertTerm> perturbationDistanceT0, final List<PertTerm> perturbationDistanceT1,
			final List<PertTerm> perturbationDistanceT2, final List<PertTerm> perturbationDistanceT3){
		this.mainLongitude = mainLongitude;
		this.mainLatitude = mainLatitude;
		this.mainDistance = mainDistance;
		this.perturbationLongitudeT0 = perturbationLongitudeT0;
		this.perturbationLongitudeT1 = perturbationLongitudeT1;
		this.perturbationLongitudeT2 = perturbationLongitudeT2;
		this.perturbationLongitudeT3 = perturbationLongitudeT3;
		this.perturbationLatitudeT0 = perturbationLatitudeT0;
		this.perturbationLatitudeT1 = perturbationLatitudeT1;
		this.perturbationLatitudeT2 = perturbationLatitudeT2;
		this.perturbationDistanceT0 = perturbationDistanceT0;
		this.perturbationDistanceT1 = perturbationDistanceT1;
		this.perturbationDistanceT2 = perturbationDistanceT2;
		this.perturbationDistanceT3 = perturbationDistanceT3;
	}

}
