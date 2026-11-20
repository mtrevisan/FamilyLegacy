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
package io.github.mtrevisan.familylegacy.ui.dialogs;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.Action;
import javax.swing.JComponent;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.util.List;


/** The list of where a media is used shows every kind of record that cites it, and closes with ESC. */
class RecordUsageDialogTest{

	private static final String AUDIT = "\taudit.creation.date 2020-01-01T00:00:00Z\n";

	private static FLEFModel model(){
		return new FLEFParser().parse("media M1 {\n\turi a.jpg\n\tkind image\n" + AUDIT + "}\n"
			+ "individual I1 {\n\tmedia {\n\t\tmedia M1\n\t\tcaption One\n\t}\n" + AUDIT + "}\n"
			+ "place P1 {\n\tname.value Town\n\tmedia {\n\t\tmedia M1\n\t}\n" + AUDIT + "}\n"
			+ "repository R1 {\n\tname.value Archive\n\tmedia {\n\t\tmedia M1\n\t}\n" + AUDIT + "}\n");
	}


	@Test
	void everyRecordThatCitesTheMediaIsListed(){
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = model();

		final RecordUsageDialog dialog = new RecordUsageDialog(null, model, model.getRecordById("M1"));

		Assertions.assertEquals(List.of("I1", "P1", "R1"), dialog.uses().stream()
			.map(use -> use.owner().getId())
			.toList());
		Assertions.assertEquals("media.media", dialog.uses().getFirst().path());

		final Object binding = dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(GUIHelper.ESCAPE_STROKE);
		Assertions.assertNotNull(binding);
		final Action action = dialog.getRootPane().getActionMap().get(binding);
		action.actionPerformed(new ActionEvent(dialog, ActionEvent.ACTION_PERFORMED, "escape"));
		Assertions.assertFalse(dialog.isDisplayable());
	}

}
