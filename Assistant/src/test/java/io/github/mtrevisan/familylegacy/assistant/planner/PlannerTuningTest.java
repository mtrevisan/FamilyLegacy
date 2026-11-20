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
package io.github.mtrevisan.familylegacy.assistant.planner;

import io.github.mtrevisan.familylegacy.assistant.AssistantTuning;
import io.github.mtrevisan.familylegacy.assistant.AssistantTuning.Parameter;
import io.github.mtrevisan.familylegacy.io.model.readers.ConfidenceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/** The limits of the levels of the plan are the ones of the user, and the defaults are what they always were. */
public class PlannerTuningTest{

	@Test
	void theDefaultsAreTheOnesThePlanAlwaysHad(){
		Assertions.assertEquals(PlannerChecker.HIGH_USEFULNESS, AssistantTuning.DEFAULTS.get(Parameter.PLANNER_HIGH));
		Assertions.assertEquals(PlannerChecker.MEDIUM_USEFULNESS, AssistantTuning.DEFAULTS.get(Parameter.PLANNER_MEDIUM));
		Assertions.assertEquals(ConfidenceReader.ENUM_HIGH, PlannerChecker.level(70));
		Assertions.assertEquals(ConfidenceReader.ENUM_MEDIUM, PlannerChecker.level(69));
		Assertions.assertEquals(ResearchTaskReader.ENUM_PRIORITY_LOW, PlannerChecker.priority(44));
	}

	@Test
	void theLevelAndThePriorityFollowTheLimitsOfTheUser(){
		final AssistantTuning tuning = AssistantTuning.DEFAULTS.with(Parameter.PLANNER_HIGH, 50).with(Parameter.PLANNER_MEDIUM, 20);

		Assertions.assertEquals(ConfidenceReader.ENUM_HIGH, PlannerChecker.level(50, tuning));
		Assertions.assertEquals(ConfidenceReader.ENUM_LOW, PlannerChecker.level(19, tuning));
		Assertions.assertEquals(ResearchTaskReader.ENUM_PRIORITY_HIGH, PlannerChecker.priority(60, tuning));
		Assertions.assertEquals(ResearchTaskReader.ENUM_PRIORITY_NORMAL, PlannerChecker.priority(30, tuning));
	}

}
