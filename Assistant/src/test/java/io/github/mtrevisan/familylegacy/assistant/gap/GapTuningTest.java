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
package io.github.mtrevisan.familylegacy.assistant.gap;

import io.github.mtrevisan.familylegacy.assistant.AssistantTuning;
import io.github.mtrevisan.familylegacy.assistant.AssistantTuning.Parameter;
import io.github.mtrevisan.familylegacy.assistant.Diagnostic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;


/** The years and the weights of the gaps are the ones of the user, and the defaults are what they always were. */
public class GapTuningTest{

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 28);

	/** An individual born in 1930 (96 years old, with no death) and a task that is a week late. */
	private static GapData data(final int birthYear){
		return new GapData(TODAY, Map.of("I1", new GapData.Person("I1", "Anna", birthYear, true, false, false, false, 0)),
			List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
	}

	private static List<Diagnostic> findings(final GapData data, final AssistantTuning tuning){
		return GapChecker.findings(data, tuning).stream()
			.map(GapFinding::diagnostic)
			.toList();
	}


	@Test
	void theDefaultsAreTheNumbersTheAssistantAlwaysHad(){
		Assertions.assertEquals(GapChecker.CERTAINLY_DEAD_YEARS, AssistantTuning.DEFAULTS.get(Parameter.GAP_CERTAINLY_DEAD_YEARS));
		Assertions.assertEquals(GapChecker.PROBABLY_DEAD_YEARS, AssistantTuning.DEFAULTS.get(Parameter.GAP_PROBABLY_DEAD_YEARS));
		Assertions.assertEquals(GapChecker.GENERATION_YEARS, AssistantTuning.DEFAULTS.get(Parameter.GAP_GENERATION_YEARS));
		Assertions.assertEquals(GapChecker.HIGH_USEFULNESS, AssistantTuning.DEFAULTS.get(Parameter.GAP_HIGH));
		Assertions.assertEquals(GapChecker.MEDIUM_USEFULNESS, AssistantTuning.DEFAULTS.get(Parameter.GAP_MEDIUM));

		final Diagnostic probablyDead = findings(data(1930), AssistantTuning.DEFAULTS).getFirst();
		Assertions.assertEquals(55., probablyDead.rank());
		Assertions.assertEquals(85., findings(data(1900), AssistantTuning.DEFAULTS).getFirst().rank());
		Assertions.assertTrue(findings(data(1990), AssistantTuning.DEFAULTS).isEmpty());
	}

	@Test
	void theYearsFromWhichAnIndividualIsDeadAreTheOnesOfTheUser(){
		//96 years old: probably dead from 90, not from 100
		final AssistantTuning later = AssistantTuning.DEFAULTS.with(Parameter.GAP_PROBABLY_DEAD_YEARS, 100)
			.with(Parameter.GAP_CERTAINLY_DEAD_YEARS, 120);
		Assertions.assertTrue(findings(data(1930), later).isEmpty());

		//and certainly dead from 90 years if the user says so
		final AssistantTuning earlier = AssistantTuning.DEFAULTS.with(Parameter.GAP_CERTAINLY_DEAD_YEARS, 95)
			.with(Parameter.GAP_PROBABLY_DEAD_YEARS, 80);
		Assertions.assertEquals(85., findings(data(1930), earlier).getFirst().rank());
	}

	@Test
	void theWeightOfAGapAndTheLimitsOfItsLevelAreTheOnesOfTheUser(){
		final AssistantTuning heavier = AssistantTuning.DEFAULTS.with(Parameter.GAP_DEATH_PROBABLE, 90);

		final Diagnostic finding = findings(data(1930), heavier).getFirst();

		Assertions.assertEquals(90., finding.rank());
		Assertions.assertTrue(finding.explanation().contains("high"), finding.explanation());
		//the same weight is a low one if the user asks for more to call it medium
		final AssistantTuning stricter = AssistantTuning.DEFAULTS.with(Parameter.GAP_HIGH, 100).with(Parameter.GAP_MEDIUM, 95);
		Assertions.assertTrue(findings(data(1930), stricter).getFirst().explanation().contains("low"),
			findings(data(1930), stricter).getFirst().explanation());
	}

}
