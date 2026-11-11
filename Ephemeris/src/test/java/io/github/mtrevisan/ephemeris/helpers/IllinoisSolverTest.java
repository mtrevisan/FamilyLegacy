package io.github.mtrevisan.ephemeris.helpers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class IllinoisSolverTest{

	@Test
	void toleranceIsOnTheArgumentNotOnTheValue(){
		//a very flat function: |f| is tiny well away from the root
		final double root = IllinoisSolver.solve(0., 4., 1.e-9, 100, x -> 1.e-8 * (x - 1.));
		Assertions.assertEquals(1., root, 1.e-8);
	}

	@Test
	void cubic(){
		final double root = IllinoisSolver.solve(0., 3., 1.e-10, 100, x -> x * x * x - 2.);
		Assertions.assertEquals(StrictMath.cbrt(2.), root, 1.e-9);
	}

}
