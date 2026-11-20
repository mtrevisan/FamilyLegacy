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
