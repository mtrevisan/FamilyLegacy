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
package io.github.mtrevisan.familylegacy.ui.assistant;

import io.github.mtrevisan.familylegacy.assistant.AssistantSettings;
import io.github.mtrevisan.familylegacy.assistant.AssistantTuning;
import io.github.mtrevisan.familylegacy.assistant.AssistantTuning.Group;
import io.github.mtrevisan.familylegacy.assistant.AssistantTuning.Parameter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.JSpinner;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


/** The numbers the assistants use as conventions: the defaults, the ranges, how they are kept and how the user changes them. */
class AssistantTuningTest{

	@Test
	void everyParameterStartsInsideItsRangeAndHasWords(){
		for(final Parameter parameter : Parameter.values()){
			Assertions.assertTrue(parameter.min() <= parameter.defaultValue() && parameter.defaultValue() <= parameter.max(), parameter.name());
			Assertions.assertEquals(parameter.defaultValue(), AssistantTuning.DEFAULTS.get(parameter), parameter.name());
			//the label is in the messages (a missing one would be the key itself)
			Assertions.assertFalse(parameter.label().startsWith("assistant.tuning."), parameter.name());
		}
		for(final Group group : Group.values()){
			Assertions.assertFalse(AssistantTuning.of(group).isEmpty(), group.name());
			Assertions.assertFalse(group.title().startsWith("assistant.tuning."), group.name());
		}
	}

	@Test
	void aValueIsBroughtIntoItsRange(){
		final AssistantTuning tuning = AssistantTuning.DEFAULTS.with(Parameter.MAX_TWIN_GAP_DAYS, 500)
			.with(Parameter.GAP_BIRTH, -4);

		Assertions.assertEquals(Parameter.MAX_TWIN_GAP_DAYS.max(), tuning.get(Parameter.MAX_TWIN_GAP_DAYS));
		Assertions.assertEquals(0, tuning.get(Parameter.GAP_BIRTH));
	}

	@Test
	void theLimitsOfTheLevelsAreKeptInOrder(){
		final AssistantTuning tuning = AssistantTuning.DEFAULTS.with(Parameter.GAP_HIGH, 30)
			.with(Parameter.GAP_CERTAINLY_DEAD_YEARS, 85)
			.with(Parameter.MAX_MOTHER_AGE, 40);

		Assertions.assertEquals(30, tuning.get(Parameter.GAP_MEDIUM));
		Assertions.assertEquals(85, tuning.get(Parameter.GAP_PROBABLY_DEAD_YEARS));
		Assertions.assertEquals(Parameter.MAX_MOTHER_AGE.min(), tuning.get(Parameter.MAX_MOTHER_AGE));
		Assertions.assertTrue(tuning.get(Parameter.MIN_MOTHER_AGE) <= tuning.get(Parameter.MAX_MOTHER_AGE));
	}

	@Test
	void theNumbersAreKeptInTheSettingsAndReadAgain(){
		final AssistantSettings settings = AssistantSettings.inMemory();
		Assertions.assertEquals(AssistantTuning.DEFAULTS, settings.tuning());

		settings.setTuning(AssistantTuning.DEFAULTS.with(Parameter.MAX_FATHER_AGE, 80).with(Parameter.PLANNER_CENSUS, 10));

		Assertions.assertEquals(80, settings.tuning().get(Parameter.MAX_FATHER_AGE));
		Assertions.assertEquals(10, settings.tuning().get(Parameter.PLANNER_CENSUS));
		Assertions.assertEquals(Parameter.GAP_BIRTH.defaultValue(), settings.tuning().get(Parameter.GAP_BIRTH));
	}

	@Test
	void whatIsNotANumberIsTheDefault(){
		final AssistantTuning tuning = AssistantTuning.read(key -> (key.equals(Parameter.GAP_BIRTH.key())? "many"
			: (key.equals(Parameter.GAP_PROBABLY_DEAD_YEARS.key())? " 95 ": null)));

		Assertions.assertEquals(Parameter.GAP_BIRTH.defaultValue(), tuning.get(Parameter.GAP_BIRTH));
		Assertions.assertEquals(95, tuning.get(Parameter.GAP_PROBABLY_DEAD_YEARS));
	}

	@Test
	void theListenersAreToldWhenTheNumbersChangeAndOnlyThen(){
		final AssistantSettings settings = AssistantSettings.inMemory();
		final AtomicInteger told = new AtomicInteger();
		settings.addListener(told::incrementAndGet);

		settings.setTuning(AssistantTuning.DEFAULTS);
		Assertions.assertEquals(0, told.get());
		settings.setTuning(AssistantTuning.DEFAULTS.with(Parameter.GAP_BIRTH, 50));
		Assertions.assertEquals(1, told.get());
	}

	@Test
	void theTabOfAnAssistantKeepsEveryChangeAndRestoresTheDefaults(){
		final AssistantSettings settings = AssistantSettings.inMemory();
		final javax.swing.JPanel panel = ResearchAssistantSettingsDialog.tuningPanel(settings, Group.GAP);
		final List<JSpinner> spinners = new ArrayList<>();
		for(final Component component : panel.getComponents())
			if(component instanceof JSpinner spinner)
				spinners.add(spinner);
		Assertions.assertEquals(AssistantTuning.of(Group.GAP).size(), spinners.size());

		//the first is the years from which an individual is certainly dead; the second is the probable one
		spinners.get(1).setValue(100);
		Assertions.assertEquals(100, settings.tuning().get(Parameter.GAP_PROBABLY_DEAD_YEARS));
		//a probable death cannot come after the certain one: the other number is brought back, and shown so
		spinners.get(0).setValue(95);
		Assertions.assertEquals(95, settings.tuning().get(Parameter.GAP_PROBABLY_DEAD_YEARS));
		Assertions.assertEquals(95, spinners.get(1).getValue());

		for(final Component component : panel.getComponents())
			if(component instanceof javax.swing.JButton button)
				button.doClick();
		Assertions.assertEquals(AssistantTuning.DEFAULTS, settings.tuning());
		Assertions.assertEquals(Parameter.GAP_CERTAINLY_DEAD_YEARS.defaultValue(), spinners.get(0).getValue());
	}

}
