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
package io.github.mtrevisan.familylegacy.ui.components.dates;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;


/**
 * The assistant opened from the date of an event starts from the reform of the place of the event, and the user can
 * choose another one.
 */
class AssistantPlaceReformTest{

	private static FLEFModel model(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(place("P1", "Innsbruck"));
		model.addRecord(place("P2", "Tyrol"));
		model.addRecord(place("P3", "Austria"));
		model.addRecord(place("P4", "Wien"));
		model.addRecord(relationship("PR1", "P1", "P2"));
		model.addRecord(relationship("PR2", "P2", "P3"));
		model.addRecord(relationship("PR3", "P4", "P3"));
		return model;
	}

	private static List<JComboBox<?>> combos(final Container container){
		final List<JComboBox<?>> result = new ArrayList<>();
		for(final Component component : container.getComponents()){
			if(component instanceof JComboBox<?> combo)
				result.add(combo);
			if(component instanceof Container child)
				result.addAll(combos(child));
		}
		return result;
	}

	private static boolean hasSelected(final HistoricalDateAssistantDialog dialog, final String text){
		return combos(dialog).stream()
			.anyMatch(combo -> text.equals(String.valueOf(combo.getSelectedItem())));
	}


	@Test
	void reformOfThePlaceIsPreselectedAndCanBeOverridden() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");

		SwingUtilities.invokeAndWait(() -> {
			final HistoricalDateAssistantDialog dialog = new HistoricalDateAssistantDialog(null,
				HistoricalDateAssistantDialog.Tool.LITURGICAL, null, null, model(), null, "P1");
			Assertions.assertTrue(hasSelected(dialog, "Austria/Tyrol"));

			//the user can choose another reform
			final JComboBox<?> reform = combos(dialog).stream()
				.filter(combo -> "Austria/Tyrol".equals(String.valueOf(combo.getSelectedItem())))
				.findFirst().orElseThrow();
			reform.setSelectedIndex(0);
			Assertions.assertFalse(hasSelected(dialog, "Austria/Tyrol"));
			dialog.dispose();
		});
	}

	@Test
	void unknownJurisdictionLeavesTheDefault() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");

		SwingUtilities.invokeAndWait(() -> {
			//Austria alone has no single reform
			final HistoricalDateAssistantDialog dialog = new HistoricalDateAssistantDialog(null,
				HistoricalDateAssistantDialog.Tool.LITURGICAL, null, null, model(), null, "P4");
			Assertions.assertFalse(hasSelected(dialog, "Austria/Tyrol"));
			dialog.dispose();
		});
	}


	private static FLEFRecord place(final String id, final String name){
		return FLEFRecord.createMainRecord(id, PlaceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue("value", name)));
	}

	private static FLEFRecord relationship(final String id, final String subject, final String target){
		return FLEFRecord.createMainRecord(id, PlaceRelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag("subject")
				.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_PLACE, subject)))
			.addChild(FLEFRecord.createChildWithTag("target")
				.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_PLACE, target)));
	}

}
