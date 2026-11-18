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
package io.github.mtrevisan.familylegacy.ui.assistant.chronology;

import io.github.mtrevisan.familylegacy.ui.assistant.AssistantTuning;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantTuning.Parameter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/** The ages and the times of the chronology are the ones of the user, and the defaults are what they always were. */
class ChronologyTuningTest{

	@Test
	void theDefaultRulesAreTheOnesTheAssistantAlwaysHad(){
		Assertions.assertEquals(ChronologyChecker.Rules.DEFAULT, ChronologyChecker.Rules.of(AssistantTuning.DEFAULTS));
	}

	@Test
	void theRulesFollowTheNumbersOfTheUser(){
		final AssistantTuning tuning = AssistantTuning.DEFAULTS.with(Parameter.MIN_MOTHER_AGE, 15).with(Parameter.MAX_FATHER_AGE, 70)
			.with(Parameter.MAX_GESTATION_DAYS, 280).with(Parameter.MAX_LIFESPAN_YEARS, 110).with(Parameter.MAX_TWIN_GAP_DAYS, 3);

		final ChronologyChecker.Rules rules = ChronologyChecker.Rules.of(tuning);

		Assertions.assertEquals(new ChronologyChecker.Rules(15, 14, 55, 70, 280, 110, 270, 3), rules);
	}

}
