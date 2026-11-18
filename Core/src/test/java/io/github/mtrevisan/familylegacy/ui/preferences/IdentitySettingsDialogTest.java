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
package io.github.mtrevisan.familylegacy.ui.preferences;

import io.github.mtrevisan.familylegacy.quality.IdentitySettings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;


/** The dialog of the settings of the Identity Assistant gives back what its spinners show, in range. */
class IdentitySettingsDialogTest{

	private static JSpinner spinner(final int value){
		return new JSpinner(new SpinnerNumberModel(value, 0, 1_000, 1));
	}


	@Test
	void theSettingsAreWhatTheSpinnersShow(){
		final IdentitySettings settings = IdentitySettingsDialog.read(spinner(55), spinner(40), spinner(95), spinner(75), spinner(80));

		Assertions.assertEquals(new IdentitySettings(55, 40, 95, 75, 80), settings);
	}

	@Test
	void aValueOutOfRangeIsBroughtIntoRange(){
		final IdentitySettings settings = IdentitySettingsDialog.read(spinner(0), spinner(900), spinner(60), spinner(80), spinner(10));

		Assertions.assertEquals(IdentitySettings.MIN_THRESHOLD, settings.threshold());
		Assertions.assertEquals(IdentitySettings.MAX_MAX_PAIRS, settings.maxPairs());
		//the medium score cannot be above the high one
		Assertions.assertEquals(60, settings.mediumScore());
		Assertions.assertEquals(IdentitySettings.MIN_OTHER_PERCENT, settings.otherPercent());
	}

}
