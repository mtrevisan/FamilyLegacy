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
import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
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


/** The research timeline lists the work in chronological order, with the kind in the language of the user. */
class ResearchTimelineDialogTest{

	private static final String FLEF = """
		header {
			protocol {
				name Family LEgacy Format
				version 0.2.0
			}
			date 2026-01-01T00:00:00Z
		}
		research_task T1 {
			description Ask the priest
			status open
			audit.creation.date 2026-03-01T00:00:00Z
		}
		research_task T2 {
			description Write to the diocese
			status open
			audit.creation.date 2025-12-31T00:00:00Z
		}
		research_task T3 {
			description Undated
			status open
		}
		""";

	private static void tables(final Container container, final List<JTable> found){
		for(final Component child : container.getComponents()){
			if(child instanceof JTable table)
				found.add(table);
			if(child instanceof Container nested)
				tables(nested, found);
		}
	}


	@Test
	void theOldestEntryComesFirstAndTheUndatedOnesLast() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = new FLEFParser().parse(FLEF);
		final List<String> dates = new ArrayList<>();
		final List<String> kinds = new ArrayList<>();

		SwingUtilities.invokeAndWait(() -> {
			final ResearchTimelineDialog dialog = new ResearchTimelineDialog(new ToolContext(model, null, null, null));
			final List<JTable> found = new ArrayList<>();
			tables(dialog, found);
			final JTable table = found.getFirst();
			for(int row = 0; row < table.getRowCount(); row ++){
				dates.add(String.valueOf(table.getValueAt(row, 0)));
				kinds.add(String.valueOf(table.getValueAt(row, 1)));
			}
			dialog.dispose();
		});

		Assertions.assertEquals(List.of("2025-12-31", "2026-03-01", ""), dates);
		Assertions.assertEquals(List.of(I18N.t("enum.research_timeline.kind.TASK_CREATED"), I18N.t("enum.research_timeline.kind.TASK_CREATED"),
			I18N.t("enum.research_timeline.kind.TASK_CREATED")), kinds);
	}

}
