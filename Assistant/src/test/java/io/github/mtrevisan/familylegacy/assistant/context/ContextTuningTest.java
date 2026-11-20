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
package io.github.mtrevisan.familylegacy.assistant.context;

import io.github.mtrevisan.familylegacy.assistant.AssistantTuning;
import io.github.mtrevisan.familylegacy.assistant.AssistantTuning.Parameter;
import io.github.mtrevisan.familylegacy.io.model.readers.ConfidenceReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/** The limits of the levels of the context are the ones of the user, and the defaults are what they always were. */
public class ContextTuningTest{

	@Test
	void theDefaultsAreTheOnesTheContextAlwaysHad(){
		Assertions.assertEquals(ContextChecker.HIGH_USEFULNESS, AssistantTuning.DEFAULTS.get(Parameter.CONTEXT_HIGH));
		Assertions.assertEquals(ContextChecker.MEDIUM_USEFULNESS, AssistantTuning.DEFAULTS.get(Parameter.CONTEXT_MEDIUM));
		Assertions.assertEquals(55, AssistantTuning.DEFAULTS.get(Parameter.CONTEXT_WITH_PLACE));
		Assertions.assertEquals(35, AssistantTuning.DEFAULTS.get(Parameter.CONTEXT_WITHOUT_PLACE));
		Assertions.assertEquals(ConfidenceReader.ENUM_MEDIUM, ContextChecker.level(55));
	}

	@Test
	void theLevelFollowsTheLimitsOfTheUser(){
		final AssistantTuning tuning = AssistantTuning.DEFAULTS.with(Parameter.CONTEXT_HIGH, 50);

		Assertions.assertEquals(ConfidenceReader.ENUM_HIGH, ContextChecker.level(55, tuning));
	}

}
