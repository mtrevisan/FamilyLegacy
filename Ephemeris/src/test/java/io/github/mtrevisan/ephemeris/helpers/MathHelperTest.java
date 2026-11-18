package io.github.mtrevisan.ephemeris.helpers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class MathHelperTest{

	@Test
	void modWrapsNegativeValues(){
		Assertions.assertEquals(350., MathHelper.mod(-10., 360.), 1.e-12);
		Assertions.assertEquals(10., MathHelper.mod(370., 360.), 1.e-12);
	}

	@Test
	void modWithMinimum(){
		Assertions.assertEquals(-170., MathHelper.mod(190., -180., 180.), 1.e-12);
		Assertions.assertEquals(170., MathHelper.mod(-190., -180., 180.), 1.e-12);
		Assertions.assertEquals(-90., MathHelper.mod(-90., -180., 180.), 1.e-12);
	}

	@Test
	void angleReduction(){
		Assertions.assertEquals(StrictMath.PI / 2., MathHelper.mod2pi(StrictMath.PI / 2. - 4. * StrictMath.PI), 1.e-12);
		Assertions.assertEquals(-StrictMath.PI / 2., MathHelper.modpipi(1.5 * StrictMath.PI), 1.e-12);
	}

	@Test
	void hourAndDayRanges(){
		Assertions.assertEquals(23., MathHelper.limitRangeHour(-1.), 1.e-12);
		Assertions.assertEquals(1., MathHelper.limitRangeHour(25.), 1.e-12);
		Assertions.assertEquals(0.75, MathHelper.limitRangeDay(-0.25), 1.e-12);
		Assertions.assertEquals(0.25, MathHelper.limitRangeDay(3.25), 1.e-12);
	}

	@Test
	void polynomialUsesHorner(){
		//1 + 2x + 3x²  at x = 2
		Assertions.assertEquals(17., MathHelper.polynomial(2., new double[]{1., 2., 3.}), 1.e-12);
	}

}
