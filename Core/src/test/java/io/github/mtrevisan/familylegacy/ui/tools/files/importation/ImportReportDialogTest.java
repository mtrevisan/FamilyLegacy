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
package io.github.mtrevisan.familylegacy.ui.tools.files.importation;

import io.github.mtrevisan.familylegacy.importer.ImportReport;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.Action;
import javax.swing.JComponent;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;


class ImportReportDialogTest{

	private static ImportReport report(){
		final ImportReport report = new ImportReport();
		report.discard("import.discard.identifier", "UID abc", "INDI @I1@");
		report.discard("import.discard.media_detail", "CHECKSUM", "OBJE @M1@");
		return report;
	}


	@Test
	void theTextHasOneLinePerIssue(){
		final String text = ImportReportDialog.toText(report());

		//the header plus one line per issue
		Assertions.assertEquals(3, StringUtils.countMatches(text, '\n'));
		Assertions.assertTrue(text.contains("UID abc") && text.contains("INDI @I1@"), text);
	}

	@Test
	void theDialogClosesWithEscAndOnlyItself(){
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialog needs a display");
		final ImportReportDialog dialog = ImportReportDialog.create(null, report());

		final Object binding = dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(GUIHelper.ESCAPE_STROKE);
		Assertions.assertNotNull(binding);
		final Action action = dialog.getRootPane().getActionMap().get(binding);
		Assertions.assertNotNull(action);
		action.actionPerformed(new ActionEvent(dialog, ActionEvent.ACTION_PERFORMED, "escape"));
		Assertions.assertFalse(dialog.isDisplayable());
	}

}
