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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PreferredSex;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.ui.components.quickentry.IndividualQuickPanel;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.IndividualRecordDialog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;


/** A new individual typed in the quick entry is saved with the records that the full editors would have written. */
class IndividualQuickEntryDialogTest{

	private FLEFModel model;


	@BeforeEach
	void setUp(){
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		model = new FLEFModel();
	}


	private static void collect(final Container container, final List<JTextField> fields, final List<JComboBox<?>> combos){
		for(final Component component : container.getComponents()){
			if(component instanceof JTextField field)
				fields.add(field);
			if(component instanceof JComboBox<?> combo && component.getParent() instanceof IndividualQuickPanel)
				combos.add(combo);
			if(component instanceof Container child)
				collect(child, fields, combos);
		}
	}

	/** The quick entry fields of the dialog: full name, birth date and place, death date and place, burial date and place, occupation. */
	private static List<JTextField> fields(final IndividualRecordDialog dialog){
		final List<JTextField> all = new ArrayList<>();
		final List<JComboBox<?>> combos = new ArrayList<>();
		collect(dialog.getContentPane(), all, combos);
		//the first fields of the Properties tab are the ones of the quick entry
		return all.subList(0, 8);
	}

	private static JComboBox<?> sexCombo(final IndividualRecordDialog dialog){
		final List<JTextField> fields = new ArrayList<>();
		final List<JComboBox<?>> combos = new ArrayList<>();
		collect(dialog.getContentPane(), fields, combos);
		return combos.getFirst();
	}


	@Test
	void theNameTheSexTheBirthAndTheDeathAreSavedWithTheIndividual(){
		final IndividualRecordDialog dialog = IndividualRecordDialog.createNew(null, model);
		final List<JTextField> fields = fields(dialog);
		fields.get(0).setText("Giovanni de Rossi");
		sexCombo(dialog).setSelectedItem(SexType.MALE);
		fields.get(1).setText("12 March 1850");
		fields.get(2).setText("Padova");
		fields.get(3).setText("abt 1920");

		dialog.save();

		Assertions.assertTrue(dialog.isSaved());
		final FLEFRecord individual = model.getRecordsByType(IndividualReader.TYPE).getFirst();
		Assertions.assertEquals("Giovanni de Rossi", IndividualReader.extractPrimaryFullname(individual));
		Assertions.assertEquals(SexType.MALE, PreferredSex.of(individual, model));
		Assertions.assertEquals(2, model.getRecordsByType(EventReader.TYPE).size());
		Assertions.assertEquals(2, model.getRecordsByType(EventParticipationReader.TYPE).size());
		Assertions.assertEquals(1, model.getRecordsByType(PlaceReader.TYPE).size());
		for(final FLEFRecord participation : model.getRecordsByType(EventParticipationReader.TYPE))
			Assertions.assertEquals(individual.getId(), FLEFRecordHelper.getChildValue(participation,
				FLEFRecordHelper.composePath(EventParticipationReader.TAG_PARTICIPANT, EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL)));
	}

	@Test
	void theBurialAndTheOccupationAreSavedToo(){
		final IndividualRecordDialog dialog = IndividualRecordDialog.createNew(null, model);
		final List<JTextField> fields = fields(dialog);
		fields.get(0).setText("Giovanni Rossi");
		fields.get(5).setText("1921");
		fields.get(6).setText("Padova");
		fields.get(7).setText("  blacksmith ");

		dialog.save();

		Assertions.assertTrue(dialog.isSaved());
		final FLEFRecord individual = model.getRecordsByType(IndividualReader.TYPE).getFirst();
		Assertions.assertEquals(1, model.getRecordsByType(EventReader.TYPE).size());
		Assertions.assertEquals(EventReader.ENUM_TYPE_BURIAL, FLEFRecordHelper.getChildValue(model.getRecordsByType(EventReader.TYPE).getFirst(),
			EventReader.TAG_TYPE));
		final FLEFRecord occupation = model.getRecordsByType(IndividualAttributeReader.TYPE).stream()
			.filter(attribute -> IndividualAttributeReader.ENUM_TYPE_OCCUPATION.equals(FLEFRecordHelper.getChildValue(attribute,
				IndividualAttributeReader.TAG_TYPE)))
			.findFirst()
			.orElseThrow();
		Assertions.assertEquals(individual.getId(), FLEFRecordHelper.getChildValue(occupation, IndividualAttributeReader.TAG_INDIVIDUAL));
		Assertions.assertEquals("blacksmith", FLEFRecordHelper.getChildValue(occupation, IndividualAttributeReader.TAG_VALUE));
	}

	@Test
	void anEmptyQuickEntrySavesAnIndividualWithNothingElse(){
		final IndividualRecordDialog dialog = IndividualRecordDialog.createNew(null, model);

		dialog.save();

		Assertions.assertTrue(dialog.isSaved());
		Assertions.assertEquals(1, model.getRecordCount());
	}

	@Test
	void cancellingAddsNothing(){
		final IndividualRecordDialog dialog = IndividualRecordDialog.createNew(null, model);
		final List<JTextField> fields = fields(dialog);
		fields.get(0).setText("Giovanni Rossi");
		fields.get(1).setText("1850");
		fields.get(2).setText("Padova");

		dialog.dispose();

		Assertions.assertEquals(0, model.getRecordCount());
	}

	@Test
	void theSexOfTheLinkThatIsBeingCreatedIsFixed(){
		final IndividualRecordDialog dialog = IndividualRecordDialog.createNew(null, model);
		dialog.requireSex(SexType.FEMALE, "a mother is female");

		final JComboBox<?> combo = sexCombo(dialog);
		Assertions.assertEquals(SexType.FEMALE, combo.getSelectedItem());
		Assertions.assertFalse(combo.isEnabled());

		dialog.save();

		final FLEFRecord individual = model.getRecordsByType(IndividualReader.TYPE).getFirst();
		Assertions.assertEquals(SexType.FEMALE, PreferredSex.of(individual, model));
		Assertions.assertEquals(1, model.getRecordsByType(IndividualAttributeReader.TYPE).size());
	}

	@Test
	void aSexPresetByTheTreeIsChosenButMayBeChanged(){
		final IndividualRecordDialog dialog = IndividualRecordDialog.createNew(null, model);
		dialog.presetSex(SexType.MALE);

		final JComboBox<?> combo = sexCombo(dialog);
		Assertions.assertEquals(SexType.MALE, combo.getSelectedItem());
		Assertions.assertTrue(combo.isEnabled());
	}

}
