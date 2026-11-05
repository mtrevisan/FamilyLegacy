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
package io.github.mtrevisan.familylegacy.ui.tools.research;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchActivityReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;


/** The research log reads as a log: the oldest activity first, the undated ones last, in the language of the user. */
class ResearchLogDialogTest{

	private static FLEFRecord activity(final String id, final String status, final String date){
		final FLEFRecord record = FLEFRecord.createMainRecord(id, ResearchActivityReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue("action", "Action " + id))
			.addChild(FLEFRecord.createChildWithTagAndValue("status", status));
		if(date != null)
			record.addChild(FLEFRecord.createChildWithTag("audit")
				.addChild(FLEFRecord.createChildWithTag("creation")
					.addChild(FLEFRecord.createChildWithTagAndValue("date", date))));
		return record;
	}

	private static void tables(final Container container, final List<JTable> found){
		for(final Component child : container.getComponents()){
			if(child instanceof JTable table)
				found.add(table);
			if(child instanceof Container nested)
				tables(nested, found);
		}
	}


	@Test
	void theOldestActivityComesFirstAndTheUndatedOnesLast() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = new FLEFModel();
		model.addRecord(activity("RA1", "completed", "2026-03-01"));
		model.addRecord(activity("RA2", "planned", null));
		model.addRecord(activity("RA3", "in_progress", "2025-12-31"));
		final List<String> firstColumn = new ArrayList<>();
		final List<String> statuses = new ArrayList<>();

		SwingUtilities.invokeAndWait(() -> {
			final ResearchLogDialog dialog = new ResearchLogDialog(new ToolContext(model, null, null, null));
			final List<JTable> found = new ArrayList<>();
			tables(dialog, found);
			final JTable table = found.getFirst();
			for(int row = 0; row < table.getRowCount(); row ++){
				firstColumn.add(String.valueOf(table.getValueAt(row, 0)));
				statuses.add(String.valueOf(table.getValueAt(row, 3)));
			}
			dialog.dispose();
		});

		Assertions.assertEquals(List.of("2025-12-31", "2026-03-01", "null"), firstColumn);
		Assertions.assertEquals(List.of(I18N.t("enum.research_activity.status.in_progress"),
			I18N.t("enum.research_activity.status.completed"), I18N.t("enum.research_activity.status.planned")), statuses);
	}

}
