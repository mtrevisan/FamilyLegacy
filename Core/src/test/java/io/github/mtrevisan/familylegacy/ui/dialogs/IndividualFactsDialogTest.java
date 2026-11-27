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

import io.github.mtrevisan.familylegacy.events.EventHelper;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PreferredSex;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.quickentry.DateStructureBuilder;
import io.github.mtrevisan.familylegacy.quickentry.LifeFactUpdater;
import io.github.mtrevisan.familylegacy.quickentry.QuickEntryService;
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


/** The sex, the birth and the death of an individual that exists are changed in place from the quick entry, and only if they are changed. */
class IndividualFactsDialogTest{

	private FLEFModel model;
	private FLEFRecord individual;


	@BeforeEach
	void setUp(){
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		model = new FLEFModel();
		final QuickEntryService.Created created = QuickEntryService.createIndividual(model, "Giovanni Rossi", SexType.MALE,
			new QuickEntryService.LifeFact(DateStructureBuilder.fromShorthand("12 March 1850", null), QuickEntryService.PlaceRef.named("Padova")),
			QuickEntryService.LifeFact.NONE);
		individual = model.getRecordById(created.individualId());
	}


	private static void collect(final Container container, final List<JTextField> fields, final List<JComboBox<?>> combos){
		for(final Component component : container.getComponents()){
			if(component instanceof JTextField field)
				fields.add(field);
			if(component instanceof JComboBox<?> combo && combos.isEmpty())
				combos.add(combo);
			if(component instanceof Container child)
				collect(child, fields, combos);
		}
	}

	/** The fields of the quick entry of an existing individual: birth, death and burial (date, place), occupation. */
	private static List<JTextField> fields(final IndividualRecordDialog dialog){
		final List<JTextField> all = new ArrayList<>();
		collect(dialog.getContentPane(), all, new ArrayList<>());
		return all.subList(0, 7);
	}

	private static JComboBox<?> sexCombo(final IndividualRecordDialog dialog){
		final List<JComboBox<?>> combos = new ArrayList<>();
		collect(dialog.getContentPane(), new ArrayList<>(), combos);
		return combos.getFirst();
	}

	private FLEFRecord birth(){
		return LifeFactUpdater.principalEvents(model, individual.getId(), EventReader.ENUM_TYPE_BIRTH).getFirst();
	}

	private IndividualRecordDialog open(){
		return IndividualRecordDialog.createEdit(null, model, individual);
	}


	@Test
	void theFactsOfTheFileAreShown(){
		final IndividualRecordDialog dialog = open();

		final List<JTextField> fields = fields(dialog);
		Assertions.assertEquals("12 March 1850", fields.get(0).getText());
		Assertions.assertTrue(fields.get(1).getText().contains("Padova"), fields.get(1).getText());
		Assertions.assertEquals("", fields.get(2).getText());
		Assertions.assertEquals(SexType.MALE, sexCombo(dialog).getSelectedItem());
		dialog.dispose();
	}

	@Test
	void savingWithoutChangingAnythingLeavesTheFactsAsTheyAre(){
		final FLEFRecord event = birth();
		final FLEFRecord before = FLEFRecord.createEmpty();
		event.deepCopyTo(before);
		final int records = model.getRecordCount();

		final IndividualRecordDialog dialog = open();
		dialog.save();

		Assertions.assertTrue(dialog.isSaved());
		Assertions.assertEquals(records, model.getRecordCount());
		Assertions.assertEquals(before, event);
		Assertions.assertEquals(SexType.MALE, PreferredSex.of(individual, model));
	}

	@Test
	void aChangedDateAndSexAreChangedInPlace(){
		final String place = EventHelper.eventPlaceId(birth());
		final int records = model.getRecordCount();

		final IndividualRecordDialog dialog = open();
		fields(dialog).get(0).setText("abt 1851");
		sexCombo(dialog).setSelectedItem(SexType.FEMALE);
		dialog.save();

		Assertions.assertTrue(dialog.isSaved());
		Assertions.assertEquals("1851", FLEFRecordHelper.getChildValue(birth(), "date.value.point.value.full_date"));
		Assertions.assertEquals(place, EventHelper.eventPlaceId(birth()));
		Assertions.assertEquals(SexType.FEMALE, PreferredSex.of(individual, model));
		Assertions.assertEquals(1, model.getRecordsByType(IndividualAttributeReader.TYPE).size());
		Assertions.assertEquals(records, model.getRecordCount());
	}

	@Test
	void cancellingAfterTypingChangesNothing(){
		final FLEFRecord before = FLEFRecord.createEmpty();
		birth().deepCopyTo(before);

		final IndividualRecordDialog dialog = open();
		fields(dialog).get(0).setText("1900");
		fields(dialog).get(2).setText("1920");
		sexCombo(dialog).setSelectedItem(SexType.FEMALE);
		dialog.dispose();

		Assertions.assertEquals(before, birth());
		Assertions.assertEquals(SexType.MALE, PreferredSex.of(individual, model));
		Assertions.assertTrue(LifeFactUpdater.principalEvents(model, individual.getId(), EventReader.ENUM_TYPE_DEATH).isEmpty());
	}

	@Test
	void aDeathTheIndividualDidNotHaveIsANewEvent(){
		final IndividualRecordDialog dialog = open();
		fields(dialog).get(2).setText("1920");
		fields(dialog).get(3).setText("Venezia");
		dialog.save();

		final List<FLEFRecord> deaths = LifeFactUpdater.principalEvents(model, individual.getId(), EventReader.ENUM_TYPE_DEATH);
		Assertions.assertEquals(1, deaths.size());
		Assertions.assertEquals("1920", FLEFRecordHelper.getChildValue(deaths.getFirst(), "date.value.point.value.full_date"));
		Assertions.assertEquals(2, model.getRecordsByType(PlaceReader.TYPE).size());
		Assertions.assertEquals(2, model.getRecordsByType(EventReader.TYPE).size());
	}

	@Test
	void aClearedDateIsRemovedAndANewPlaceNameIsANewPlace(){
		final IndividualRecordDialog dialog = open();
		fields(dialog).get(0).setText("");
		fields(dialog).get(1).setText("Verona");
		dialog.save();

		Assertions.assertNull(FLEFRecordHelper.findChild(birth(), EventReader.TAG_DATE));
		Assertions.assertEquals(2, model.getRecordsByType(PlaceReader.TYPE).size());
		Assertions.assertTrue(PlaceReader.extractPrimaryName(model.getRecordById(EventHelper.eventPlaceId(birth()))).contains("Verona"));
	}

	@Test
	void aCitedDateCannotBeEditedHere(){
		FLEFRecordHelper.findChild(birth(), EventReader.TAG_DATE)
			.addChild(FLEFRecord.createChildWithTag("source")
				.addChild(FLEFRecord.createChildWithTagAndValue("source", "S1")));

		final IndividualRecordDialog dialog = open();

		Assertions.assertFalse(fields(dialog).get(0).isEnabled());
		Assertions.assertTrue(fields(dialog).get(1).isEnabled());
		dialog.save();
		Assertions.assertTrue(dialog.isSaved());
		Assertions.assertEquals("12 March 1850", EventHelper.eventDateRaw(birth()));
		dialog.dispose();
	}

	@Test
	void theOccupationIsShownAndChangedInPlace(){
		QuickEntryService.addOccupation(model, individual.getId(), "blacksmith");

		final IndividualRecordDialog dialog = open();
		Assertions.assertEquals("blacksmith", fields(dialog).get(6).getText());
		fields(dialog).get(6).setText("farmer");
		dialog.save();

		Assertions.assertTrue(dialog.isSaved());
		final List<FLEFRecord> occupations = LifeFactUpdater.occupations(model, individual.getId());
		Assertions.assertEquals(1, occupations.size());
		Assertions.assertEquals("farmer", FLEFRecordHelper.getChildValue(occupations.getFirst(), IndividualAttributeReader.TAG_VALUE));
	}

	@Test
	void anOccupationTypedForAPersonWithNoneIsAddedAndABlankOneRemovesIt(){
		IndividualRecordDialog dialog = open();
		fields(dialog).get(6).setText("miller");
		dialog.save();
		Assertions.assertEquals("miller", FLEFRecordHelper.getChildValue(LifeFactUpdater.occupations(model, individual.getId()).getFirst(),
			IndividualAttributeReader.TAG_VALUE));

		dialog = open();
		fields(dialog).get(6).setText("");
		dialog.save();
		Assertions.assertTrue(LifeFactUpdater.occupations(model, individual.getId()).isEmpty());
	}

	@Test
	void anOccupationWithDetailsOrSeveralOccupationsCannotBeEditedHere(){
		QuickEntryService.addOccupation(model, individual.getId(), "blacksmith");
		LifeFactUpdater.occupations(model, individual.getId()).getFirst()
			.addChild(FLEFRecord.createChildWithTagAndValue("valid_from", "x"));

		IndividualRecordDialog dialog = open();
		Assertions.assertFalse(fields(dialog).get(6).isEnabled());
		dialog.dispose();

		QuickEntryService.addOccupation(model, individual.getId(), "farmer");
		dialog = open();
		Assertions.assertFalse(fields(dialog).get(6).isEnabled());
		dialog.dispose();
	}

	@Test
	void aBurialThePersonDidNotHaveIsANewEventAndOneThatExistsIsChangedInPlace(){
		final IndividualRecordDialog dialog = open();
		fields(dialog).get(4).setText("1921");
		dialog.save();

		final List<FLEFRecord> burials = LifeFactUpdater.principalEvents(model, individual.getId(), EventReader.ENUM_TYPE_BURIAL);
		Assertions.assertEquals(1, burials.size());

		final IndividualRecordDialog second = open();
		Assertions.assertEquals("1921", fields(second).get(4).getText());
		fields(second).get(4).setText("1922");
		second.save();

		Assertions.assertEquals(1, LifeFactUpdater.principalEvents(model, individual.getId(), EventReader.ENUM_TYPE_BURIAL).size());
		Assertions.assertEquals("1922", EventHelper.eventDateRaw(LifeFactUpdater.principalEvents(model, individual.getId(),
			EventReader.ENUM_TYPE_BURIAL).getFirst()));
	}

	@Test
	void severalSexAttributesAreLeftToTheAttributesTab(){
		model.addRecord(FLEFRecord.createMainRecord(IndividualAttributeReader.TYPE, IndividualAttributeReader.ID_PREFIX, model)
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_INDIVIDUAL, individual.getId()))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_TYPE, IndividualAttributeReader.ENUM_TYPE_SEX))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_VALUE, "female")));

		final IndividualRecordDialog dialog = open();

		Assertions.assertFalse(sexCombo(dialog).isEnabled());
		dialog.dispose();
	}

	@Test
	void aSeveralBirthsIndividualEditsTheFirstOne(){
		final QuickEntryService.Created other = QuickEntryService.createIndividual(model, "Maria Bianchi", null,
			new QuickEntryService.LifeFact(DateStructureBuilder.fromShorthand("1800", null), QuickEntryService.PlaceRef.NONE), null);
		QuickEntryService.addLifeEvents(model, other.individualId(),
			new QuickEntryService.LifeFact(DateStructureBuilder.fromShorthand("1801", null), QuickEntryService.PlaceRef.NONE), null);
		final FLEFRecord maria = model.getRecordById(other.individualId());

		final IndividualRecordDialog dialog = IndividualRecordDialog.createEdit(null, model, maria);
		final List<JTextField> fields = fields(dialog);
		Assertions.assertEquals("1800", fields.get(0).getText());
		fields.get(0).setText("1802");
		dialog.save();

		final List<FLEFRecord> births = LifeFactUpdater.principalEvents(model, maria.getId(), EventReader.ENUM_TYPE_BIRTH);
		Assertions.assertEquals(2, births.size());
		Assertions.assertEquals("1802", EventHelper.eventDateRaw(births.get(0)));
		Assertions.assertEquals("1801", EventHelper.eventDateRaw(births.get(1)));
	}

}
