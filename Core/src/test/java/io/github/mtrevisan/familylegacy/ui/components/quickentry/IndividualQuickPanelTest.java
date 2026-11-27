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
package io.github.mtrevisan.familylegacy.ui.components.quickentry;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.quickentry.QuickEntryService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;


/** What the quick entry of a new individual gives to the editor: the name split in parts, the sex, the birth and the death. */
class IndividualQuickPanelTest{

	private FLEFModel model;
	private List<FLEFRecord> names;
	private IndividualQuickPanel panel;


	@BeforeEach
	void setUp(){
		model = new FLEFModel();
		names = new ArrayList<>();
		panel = new IndividualQuickPanel(null, model, names::add);
	}


	/** The text fields of the panel, in order: full name, birth date, birth place, death date, death place. */
	private List<JTextField> fields(){
		final List<JTextField> fields = new ArrayList<>();
		collect(panel, fields);
		return fields;
	}

	private static void collect(final Container container, final List<JTextField> fields){
		for(final Component component : container.getComponents()){
			if(component instanceof JTextField field)
				fields.add(field);
			if(component instanceof Container child)
				collect(child, fields);
		}
	}

	private void type(final int field, final String text){
		//the text is changed on the event thread, where the listeners that change the other components are safe
		LifeFactRow.onEventThread(() -> fields().get(field).setText(text));
	}

	private static List<String> parts(final FLEFRecord name){
		return FLEFRecordHelper.findChildren(name, NameReader.TAG_PART).stream()
			.map(part -> NameReader.extractPartType(part) + ":" + NameReader.extractPartValue(part))
			.toList();
	}

	private void addPlace(final String id, final String name){
		model.addRecord(FLEFRecord.createMainRecord(id, PlaceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(PlaceReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue(PlaceReader.TAG_NAME_VALUE, name))));
	}


	@Test
	void theFullNameIsSplitIntoPartsAndHandedToTheEditorAndTheFieldIsEmptied(){
		type(0, "Maria de la Cruz");
		fields().getFirst().postActionEvent();

		Assertions.assertEquals(1, names.size());
		Assertions.assertEquals(List.of("given:Maria", "family_prefix:de la", "family:Cruz"), parts(names.getFirst()));
		Assertions.assertEquals("", fields().getFirst().getText());
	}

	@Test
	void aNameStillTypedWhenTheEditorIsSavedIsAName(){
		type(0, "Rossi, Giovanni");

		panel.flushName();

		Assertions.assertEquals(List.of("given:Giovanni", "family:Rossi"), parts(names.getFirst()));
		panel.flushName();
		Assertions.assertEquals(1, names.size());
	}

	private void partOf(final String id, final String subject, final String target){
		model.addRecord(FLEFRecord.createMainRecord(id, io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag("subject")
				.addChild(FLEFRecord.createChildWithTagAndValue("place", subject)))
			.addChild(FLEFRecord.createChildWithTag("target")
				.addChild(FLEFRecord.createChildWithTagAndValue("place", target))));
	}

	/** Answers the questions about the places with the index, and counts them. */
	private List<List<String>> answering(final int... answers){
		final List<List<String>> asked = new ArrayList<>();
		panel.birthPanel().setAsker((parent, message, title, options) -> {
			asked.add(options);
			return answers[Math.min(asked.size(), answers.length) - 1];
		});
		return asked;
	}

	@Test
	void aChainOfPlacesNotInTheFileIsANewPlaceInsideNewPlaces(){
		type(2, "Padova, Veneto, Italy");

		Assertions.assertTrue(panel.prepare());

		final QuickEntryService.PlaceRef place = panel.birth().place();
		Assertions.assertEquals("Padova", place.newName());
		Assertions.assertEquals(List.of("Veneto", "Italy"), place.enclosing());
		Assertions.assertNull(place.enclosingId());
	}

	@Test
	void theLargestPlaceIsAskedAboutAndWhatIsInsideItIsFoundWithoutAsking(){
		addPlace("P1", "Italy");
		addPlace("P2", "Veneto");
		partOf("PR1", "P2", "P1");
		final List<List<String>> asked = answering(0);
		type(2, "Padova, Veneto, Italy");

		Assertions.assertTrue(panel.prepare());

		//only the largest place is asked about: Veneto inside Italy is the one that the file has
		Assertions.assertEquals(1, asked.size());
		final QuickEntryService.PlaceRef place = panel.birth().place();
		Assertions.assertEquals("Padova", place.newName());
		Assertions.assertTrue(place.enclosing().isEmpty());
		Assertions.assertEquals("P2", place.enclosingId());
	}

	@Test
	void aWholeChainThatTheFileHasIsThatPlace(){
		addPlace("P1", "Italy");
		addPlace("P2", "Veneto");
		addPlace("P3", "Padova");
		partOf("PR1", "P2", "P1");
		partOf("PR2", "P3", "P2");
		final List<List<String>> asked = answering(0);
		type(2, "Padova, Veneto, Italy");

		Assertions.assertTrue(panel.prepare());

		Assertions.assertEquals(1, asked.size());
		Assertions.assertEquals("P3", panel.birth().place().existingId());
	}

	@Test
	void aPlaceInsideAnotherPlaceOfTheSameNameIsNotTheOneOfTheFile(){
		addPlace("P1", "Italy");
		addPlace("P2", "Veneto");
		addPlace("P3", "Padova");
		partOf("PR1", "P2", "P1");
		partOf("PR2", "P3", "P2");
		answering(0);
		type(2, "Padova, Lombardia, Italy");

		Assertions.assertTrue(panel.prepare());

		//Lombardia is not in Italy for the file: new, and so is Padova inside it
		final QuickEntryService.PlaceRef place = panel.birth().place();
		Assertions.assertNull(place.existingId());
		Assertions.assertEquals("Padova", place.newName());
		Assertions.assertEquals(List.of("Lombardia"), place.enclosing());
		Assertions.assertEquals("P1", place.enclosingId());
	}

	@Test
	void theUserMayMakeTheLargestPlaceNewAndTheWholeChainWithIt(){
		addPlace("P1", "Italy");
		answering(1);
		type(2, "Padova, Veneto, Italy");

		Assertions.assertTrue(panel.prepare());

		final QuickEntryService.PlaceRef place = panel.birth().place();
		Assertions.assertEquals(List.of("Veneto", "Italy"), place.enclosing());
		Assertions.assertNull(place.enclosingId());
	}

	@Test
	void notChoosingThePlaceOfAChainStopsTheSave(){
		addPlace("P1", "Italy");
		answering(-1);
		type(2, "Padova, Veneto, Italy");

		Assertions.assertFalse(panel.prepare());
	}

	@Test
	void twoPlacesWithTheNameInsideTheSamePlaceAreAskedAbout(){
		addPlace("P1", "Italy");
		addPlace("P2", "Veneto");
		addPlace("P3", "Veneto");
		partOf("PR1", "P2", "P1");
		partOf("PR2", "P3", "P1");
		final List<List<String>> asked = answering(0, 1);
		type(2, "Padova, Veneto, Italy");

		Assertions.assertTrue(panel.prepare());

		Assertions.assertEquals(2, asked.size());
		Assertions.assertEquals("P3", panel.birth().place().enclosingId());
	}

	@Test
	void theButtonToAddANameWorksOnlyWhenSomethingIsTyped(){
		final JButton add = (JButton)find(panel, JButton.class);
		Assertions.assertFalse(add.isEnabled());

		type(0, "Giovanni");
		Assertions.assertTrue(add.isEnabled());

		type(0, "   ");
		Assertions.assertFalse(add.isEnabled());

		type(0, "Giovanni Rossi");
		add.doClick();
		Assertions.assertEquals(1, names.size());
		Assertions.assertFalse(add.isEnabled());
	}

	@Test
	void anEmptyNameGivesNothing(){
		type(0, "   ");

		panel.flushName();

		Assertions.assertTrue(names.isEmpty());
	}

	@Test
	void theSexIsUnknownUntilChosenAndMayBeFixed(){
		Assertions.assertEquals(SexType.UNKNOWN, panel.sex());

		panel.setSex(SexType.FEMALE, true);

		Assertions.assertEquals(SexType.FEMALE, panel.sex());
		final JComboBox<?> combo = (JComboBox<?>)find(panel, JComboBox.class);
		Assertions.assertFalse(combo.isEnabled());

		panel.setSex(SexType.MALE, false);
		Assertions.assertTrue(combo.isEnabled());
		Assertions.assertEquals(SexType.MALE, panel.sex());
	}

	private static Component find(final Container container, final Class<?> type){
		for(final Component component : container.getComponents()){
			if(type.isInstance(component))
				return component;
			if(component instanceof Container child){
				final Component found = find(child, type);
				if(found != null)
					return found;
			}
		}
		return null;
	}

	@Test
	void theBirthAndTheDeathAreWhatWasTyped(){
		type(1, "12 March 1850");
		type(2, "Padova");
		type(3, "abt 1920");

		Assertions.assertTrue(panel.prepare());

		final QuickEntryService.LifeFact birth = panel.birth();
		Assertions.assertNotNull(birth.date());
		Assertions.assertEquals("Padova", birth.place().newName());
		Assertions.assertNull(birth.place().existingId());
		final QuickEntryService.LifeFact death = panel.death();
		Assertions.assertNotNull(death.date());
		Assertions.assertTrue(death.place().isNone());
	}

	@Test
	void nothingTypedIsNoFact(){
		Assertions.assertTrue(panel.prepare());

		Assertions.assertTrue(panel.birth().isEmpty());
		Assertions.assertTrue(panel.death().isEmpty());
	}

	@Test
	void aDateThatIsNotADateIsAskedAboutAndTheDefaultIsToCorrectIt(){
		final List<List<String>> asked = new ArrayList<>();
		panel.birthPanel().setAsker((parent, message, title, options) -> {
			asked.add(options);
			return 0;
		});
		type(1, "31 February 1850");

		Assertions.assertFalse(panel.prepare());

		Assertions.assertEquals(1, asked.size());
		Assertions.assertEquals(2, asked.getFirst().size());
	}

	@Test
	void aDateThatIsNotADateMayBeKeptAsWritten(){
		panel.birthPanel().setAsker((parent, message, title, options) -> 1);
		type(1, "33 Foo 1850");

		Assertions.assertTrue(panel.prepare());

		Assertions.assertEquals("33 Foo 1850", io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper.getChildValue(panel.birth().date(),
			"value.point.value.full_date"));
	}

	@Test
	void aDateThatIsNotADateAndLeftWithEscapeStopsTheSave(){
		panel.birthPanel().setAsker((parent, message, title, options) -> -1);
		type(1, "ABC");

		Assertions.assertFalse(panel.prepare());
	}

	@Test
	void anAmbiguousDateIsWrittenWithTheReadingTheUserChose(){
		final List<List<String>> asked = new ArrayList<>();
		panel.birthPanel().setAsker((parent, message, title, options) -> {
			asked.add(options);
			return 1;
		});
		type(1, "est 1850");

		Assertions.assertTrue(panel.prepare());

		Assertions.assertEquals(1, asked.size());
		final String written = io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper.getChildValue(panel.birth().date(),
			"value.point.value.full_date");
		Assertions.assertTrue(written.startsWith("est 1850 ("), written);
	}

	@Test
	void theDateIsOfTheCalendarChosen(){
		//the 29th of February 1700 is a date of the Julian calendar, not of the Gregorian one
		panel.birthPanel().setAsker((parent, message, title, options) -> -1);
		type(1, "29 February 1700");
		Assertions.assertFalse(panel.prepare());

		panel.birthPanel().setAsker((parent, message, title, options) -> {
			throw new AssertionError("asked");
		});
		panel.birthPanel().setCalendar("julian");
		Assertions.assertTrue(panel.prepare());

		final FLEFRecord date = panel.birth().date();
		Assertions.assertEquals("29 February 1700", io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper.getChildValue(date,
			"value.point.value.full_date"));
		Assertions.assertEquals("julian", io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper.getChildValue(date,
			"value.point.calendar"));
	}

	@Test
	void theCalendarIsGregorianUnlessChosen(){
		Assertions.assertEquals("gregorian", panel.birthPanel().calendarCode());
		Assertions.assertEquals("gregorian", panel.deathPanel().calendarCode());
		type(1, "12 March 1850");
		Assertions.assertTrue(panel.prepare());

		Assertions.assertEquals("gregorian", io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper.getChildValue(panel.birth().date(),
			"value.point.calendar"));
	}

	@Test
	void theDateOfAnExistingEventIsShownInItsCalendarAndTheCalendarAloneIsAChange(){
		final var created = QuickEntryService.createIndividual(model, "Giovanni Rossi", null, new QuickEntryService.LifeFact(
			io.github.mtrevisan.familylegacy.quickentry.DateStructureBuilder.fromShorthand("29 February 1700", "julian"),
			QuickEntryService.PlaceRef.NONE), null);
		final IndividualQuickPanel existing = new IndividualQuickPanel(null, model, null);
		existing.load(model, model.getRecordById(created.individualId()));

		Assertions.assertEquals("julian", existing.birthPanel().calendarCode());
		//nothing was touched: the date is not read again, and it is not a change (although the Gregorian calendar would refuse it)
		Assertions.assertTrue(existing.prepare());
		Assertions.assertFalse(existing.birthPanel().edit().dateChanged());

		existing.birthPanel().setCalendar("gregorian");
		existing.birthPanel().setReporter((parent, message) -> {});
		existing.birthPanel().setAsker((parent, message, title, options) -> 1);
		Assertions.assertTrue(existing.prepare());
		Assertions.assertTrue(existing.birthPanel().edit().dateChanged());
	}

	@Test
	void aValidDateIsNotAskedAbout(){
		panel.birthPanel().setAsker((parent, message, title, options) -> {
			throw new AssertionError("asked");
		});
		type(1, "12 March 1850");
		type(3, "abt 1920");

		Assertions.assertTrue(panel.prepare());
	}

	@Test
	void aDateThatCannotBeReadStopsTheSaveAndTellsTheUser(){
		final List<String> reported = new ArrayList<>();
		panel.birthPanel().setReporter((parent, message) -> reported.add(message));
		panel.deathPanel().setReporter((parent, message) -> reported.add(message));
		type(1, "bet 1850");

		Assertions.assertFalse(panel.prepare());

		Assertions.assertEquals(1, reported.size());
	}


	/** A place named like the one typed: the user is asked, and nothing is chosen for them. */
	private LifeFactRow birthPanelAsking(final int answer, final List<List<String>> asked){
		for(final LifeFactRow facts : List.of(panel.birthPanel(), panel.deathPanel()))
			facts.setAsker((parent, message, title, options) -> {
				asked.add(options);
				return answer;
			});
		return panel.birthPanel();
	}

	@Test
	void aPlaceThatTheFileHasIsOfferedAndTheUserMayUseIt(){
		addPlace("P1", "Padova");
		addPlace("P2", "Padova");
		final List<List<String>> asked = new ArrayList<>();
		birthPanelAsking(1, asked);
		type(2, "Padova");

		Assertions.assertTrue(panel.prepare());

		Assertions.assertEquals(1, asked.size());
		//both places, then the new one
		Assertions.assertEquals(3, asked.getFirst().size());
		Assertions.assertEquals("P2", panel.birth().place().existingId());
	}

	@Test
	void theUserMayCreateANewPlaceInsteadOfUsingTheOnesOfTheFile(){
		addPlace("P1", "Padova");
		final List<List<String>> asked = new ArrayList<>();
		birthPanelAsking(1, asked);
		type(2, "Padova");

		Assertions.assertTrue(panel.prepare());

		Assertions.assertEquals("Padova", panel.birth().place().newName());
		Assertions.assertNull(panel.birth().place().existingId());
	}

	@Test
	void notChoosingAPlaceStopsTheSaveAndTheUserIsNotAskedAgainOnceDecided(){
		addPlace("P1", "Padova");
		final List<List<String>> asked = new ArrayList<>();
		birthPanelAsking(-1, asked);
		type(2, "Padova");

		Assertions.assertFalse(panel.prepare());

		birthPanelAsking(0, asked);
		Assertions.assertTrue(panel.prepare());
		Assertions.assertEquals("P1", panel.birth().place().existingId());
		//a second check of the same text does not ask again
		Assertions.assertTrue(panel.prepare());
		Assertions.assertEquals(2, asked.size());
	}

}
