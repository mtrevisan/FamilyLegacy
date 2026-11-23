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
package io.github.mtrevisan.familylegacy.quality;

import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateRange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


/** The rules on the dates, written once for Validate File and for the Chronology Assistant. */
class DateRulesTest{

	private static DateRange days(final long from, final long to){
		return new DateRange(from, to);
	}


	@Test
	void aRuleIsBrokenOnlyIfEveryDayOfOneDateIsBeforeEveryDayOfTheOther(){
		Assertions.assertTrue(DateRules.deathBeforeBirth(days(10, 20), days(30, 40)));
		//they may be the same day, or the other way round
		Assertions.assertFalse(DateRules.deathBeforeBirth(days(10, 35), days(30, 40)));
		//what is not known judges nothing
		Assertions.assertFalse(DateRules.deathBeforeBirth(null, days(30, 40)));
		Assertions.assertFalse(DateRules.deathBeforeBirth(days(10, 20), null));
	}

	@Test
	void aBirthIsNeverBeforeTheBirthAndABurialMayFollowTheDeath(){
		Assertions.assertFalse(DateRules.eventBeforeBirth(days(1, 2), days(30, 40), EventReader.ENUM_TYPE_BIRTH));
		Assertions.assertTrue(DateRules.eventBeforeBirth(days(1, 2), days(30, 40), EventReader.ENUM_TYPE_MARRIAGE));
		Assertions.assertFalse(DateRules.eventAfterDeath(days(50, 60), days(30, 40), EventReader.ENUM_TYPE_BURIAL));
		Assertions.assertTrue(DateRules.eventAfterDeath(days(50, 60), days(30, 40), EventReader.ENUM_TYPE_MARRIAGE));
	}

	@Test
	void aFatherMayDieForTheGestationBeforeTheBirth(){
		Assertions.assertFalse(DateRules.bornAfterFatherDeath(days(1_000, 1_000), days(800, 800), 300));
		Assertions.assertTrue(DateRules.bornAfterFatherDeath(days(1_000, 1_000), days(600, 600), 300));
		//a mother cannot
		Assertions.assertTrue(DateRules.bornAfterMotherDeath(days(1_000, 1_000), days(999, 999)));
		Assertions.assertFalse(DateRules.bornAfterMotherDeath(days(1_000, 1_000), days(1_000, 1_000)));
	}

	@Test
	void aPeriodIsJudgedAgainstTheLifeAndTheEndOnlyWhenTheBeginningDoesNotSayIt(){
		final DateRange birth = days(100, 100);
		final DateRange death = days(200, 200);

		Assertions.assertEquals(List.of(DateRules.PeriodProblem.BEGINS_BEFORE_BIRTH), DateRules.period(days(50, 50), null, birth, death));
		Assertions.assertEquals(List.of(DateRules.PeriodProblem.BEGINS_AFTER_DEATH), DateRules.period(days(250, 250), null, birth, death));
		Assertions.assertEquals(List.of(DateRules.PeriodProblem.ENDS_BEFORE_BIRTH), DateRules.period(null, days(50, 50), birth, death));
		//it begins before the birth: that already says it
		Assertions.assertEquals(List.of(DateRules.PeriodProblem.BEGINS_BEFORE_BIRTH), DateRules.period(days(10, 10), days(50, 50), birth, death));
		Assertions.assertTrue(DateRules.period(days(120, 120), days(150, 150), birth, death).isEmpty());
		//it goes on after the death: recorded as open, not wrong
		Assertions.assertTrue(DateRules.period(days(120, 120), days(500, 500), birth, death).isEmpty());
	}

}
