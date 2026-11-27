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
package io.github.mtrevisan.familylegacy.ui.components.fields;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


/** A person is added to an event with one row: the person, the role in plain words, Add. */
class EventParticipantRowTest{

	@Test
	void theRolesOfAFamilyEventComeFirstAndEveryRoleIsOffered(){
		final List<String> roles = EventParticipantRow.roles();

		Assertions.assertEquals(List.of("witness", "sponsor", "officiant", "informant", "midwife", "principal"), roles.subList(0, 6));
		for(final String role : EventParticipationReader.INDIVIDUAL_OR_GROUP_ROLES)
			Assertions.assertTrue(roles.contains(role), role);
		Assertions.assertEquals(roles.size(), List.copyOf(roles).stream().distinct().count());
	}

	@Test
	void theSponsorIsCalledGodparentAndTheOthersKeepTheirName(){
		Assertions.assertTrue(EventParticipantRow.label("sponsor").startsWith("godparent"));
		Assertions.assertTrue(EventParticipantRow.label("principal").contains("the event is about"));
		Assertions.assertEquals("witness", EventParticipantRow.label("witness"));
	}

	@Test
	void addMakesTheParticipationAndTheDialogCanUndoIt(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE));
		final List<Runnable> undos = new ArrayList<>();
		final AtomicInteger added = new AtomicInteger();
		final EventParticipantRow row = new EventParticipantRow(null, model, () -> "E1", undos::add, added::incrementAndGet);

		Assertions.assertFalse(row.addButton.isEnabled(), "no person chosen yet");
		row.select(model.getRecordById("I1"));
		row.roleCombo.setSelectedItem("sponsor");
		Assertions.assertTrue(row.addButton.isEnabled());
		row.addButton.doClick();

		Assertions.assertEquals(1, added.get());
		Assertions.assertEquals(1, model.getRecordsByType(EventParticipationReader.TYPE).size());
		Assertions.assertEquals(1, undos.size());
		//the row is ready for the next person, with the same role
		Assertions.assertFalse(row.addButton.isEnabled());
		Assertions.assertEquals("sponsor", row.roleCombo.getSelectedItem());

		//the event is not saved: the participation goes with it
		undos.getFirst().run();
		Assertions.assertTrue(model.getRecordsByType(EventParticipationReader.TYPE).isEmpty());
	}

	@Test
	void theSamePersonWithTheSameRoleIsNotAddedTwice(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE));
		final List<Runnable> undos = new ArrayList<>();
		final EventParticipantRow row = new EventParticipantRow(null, model, () -> "E1", undos::add, () -> {});

		for(int i = 0; i < 2; i ++){
			row.select(model.getRecordById("I1"));
			row.roleCombo.setSelectedItem("witness");
			row.addButton.doClick();
		}

		Assertions.assertEquals(1, model.getRecordsByType(EventParticipationReader.TYPE).size());
		Assertions.assertEquals(1, undos.size());
	}

}
