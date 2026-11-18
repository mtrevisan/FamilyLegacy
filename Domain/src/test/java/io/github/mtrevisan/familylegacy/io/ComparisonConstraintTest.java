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
package io.github.mtrevisan.familylegacy.io;

import io.github.mtrevisan.familylegacy.io.grammar.contraints.ComparisonConstraint;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;


class ComparisonConstraintTest{

	private static List<String> validate(final String left, final String op, final String right, final String leftValue,
			final String rightValue){
		final FLEFRecord record = FLEFRecord.createChildWithTag("r")
			.addChild(FLEFRecord.createChildWithTagAndValue("a", leftValue))
			.addChild(FLEFRecord.createChildWithTagAndValue("b", rightValue));
		final List<String> errors = new ArrayList<>(0);
		new ComparisonConstraint(left, op, right).validate("r", record, null, errors);
		return errors;
	}


	@Test
	void numbersAreComparedNumerically(){
		Assertions.assertTrue(validate("a", ComparisonConstraint.LESS_THAN, "b", "9", "10").isEmpty());
		Assertions.assertFalse(validate("a", ComparisonConstraint.GREATER_THAN, "b", "9", "10").isEmpty());
	}

	@Test
	void orderingOnNonNumericValuesSaysTheyAreNotComparable(){
		final List<String> errors = validate("a", ComparisonConstraint.GREATER_THAN, "b", "abc", "def");

		Assertions.assertEquals(1, errors.size());
		Assertions.assertTrue(errors.getFirst().contains("not comparable numerically"), errors.getFirst());
	}

	@Test
	void equalityOnStringsStillWorks(){
		Assertions.assertTrue(validate("a", ComparisonConstraint.EQUALS, "b", "x", "x").isEmpty());
		Assertions.assertFalse(validate("a", ComparisonConstraint.EQUALS, "b", "x", "y").isEmpty());
	}

}
