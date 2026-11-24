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

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.tools.events.EventToolRegistry;
import io.github.mtrevisan.familylegacy.ui.tools.events.HistoricalTimelineDialog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;


/** The historical timeline: it is in the Event menu, and its list has the life and the history of the individual in one table. */
class HistoricalTimelineDialogTest{

	private static final String FLEF = """
		header {
			protocol {
				name Family LEgacy Format
				version 0.2.0
			}
			date 2026-01-01T00:00:00Z
		}
		individual I1 {
			name {
				part {
					type given
					value Anna
				}
			}
		}
		place P1 {
			name.value Treviso
			type city
		}
		event E1 {
			type birth
			date.value.point.value.full_date 1850-05-01
			place.place P1
		}
		event_participation EP1 {
			event E1
			participant.individual I1
			role principal
		}
		event E2 {
			type death
			date.value.point.value.full_date 1920-03-04
			place.place P1
		}
		event_participation EP2 {
			event E2
			participant.individual I1
			role principal
		}
		historic_event HE1 {
			type war
			title The war of the town
			date.value.point.value.full_date 1866-06-20
			place.place P1
		}
		historic_event HE2 {
			type war
			title A war far away
			date.value.point.value.full_date 1870-07-19
		}
		""";


	@Test
	void theToolIsInTheEventMenu(){
		final List<String> names = new ArrayList<>();
		EventToolRegistry.analysisTools()
			.forEach(tool -> names.add(tool.getName()));

		Assertions.assertTrue(names.contains(I18N.t("menu.event.historical_timeline")), names.toString());
	}

	@Test
	void theListHasTheLifeAndTheHistoryOfTheIndividualWithTheirAges() throws InvocationTargetException, InterruptedException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");

		final FLEFModel model = new FLEFParser().parse(FLEF);
		final List<JTable> tables = new ArrayList<>();
		final List<List<String>> rows = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			final HistoricalTimelineDialog dialog = new HistoricalTimelineDialog(new ToolContext(model, "I1", null, null), "I1");
			tables.addAll(tablesOf(dialog));
			final JTable table = tables.getFirst();
			for(int row = 0; row < table.getRowCount(); row ++){
				final List<String> cells = new ArrayList<>();
				for(int column = 0; column < table.getColumnCount(); column ++)
					cells.add(String.valueOf(table.getValueAt(row, column)));
				rows.add(cells);
			}
			dialog.dispose();
		});

		final List<String> kinds = rows.stream()
			.map(cells -> cells.get(2))
			.toList();
		Assertions.assertEquals(List.of(I18N.t("text.historical.kind.life_event"), I18N.t("text.historical.kind.historic_event"),
			I18N.t("text.historical.kind.historic_event"), I18N.t("text.historical.kind.life_event")), kinds,
			"birth, the war of the town, the war far away, death: " + rows);
		Assertions.assertEquals("0", rows.getFirst().get(1), "the age at the birth");
		Assertions.assertEquals("16", rows.get(1).get(1), "the age at the war");
		Assertions.assertEquals("The war of the town", rows.get(1).get(3));
		Assertions.assertEquals(I18N.t("text.historical.scope.local"), rows.get(1).get(6));
		Assertions.assertEquals("A war far away", rows.get(2).get(3));
		Assertions.assertEquals(I18N.t("text.historical.scope.world"), rows.get(2).get(6), "with no place: a world event");
	}

	private static List<JTable> tablesOf(final Container container){
		final List<JTable> tables = new ArrayList<>();
		for(final Component child : container.getComponents()){
			if(child instanceof JTable table)
				tables.add(table);
			if(child instanceof Container nested)
				tables.addAll(tablesOf(nested));
		}
		return tables;
	}

}
