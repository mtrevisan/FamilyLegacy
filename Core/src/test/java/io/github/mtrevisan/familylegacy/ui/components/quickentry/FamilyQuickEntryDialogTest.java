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
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.quickentry.FamilyQuickEntry;
import io.github.mtrevisan.familylegacy.quickentry.QuickEntryService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;


/** The quick entry of a family: a person, the partner, the union, the children; only what is filled is asked to be created. */
class FamilyQuickEntryDialogTest{

	private FLEFModel model;
	private List<String> reported;


	@BeforeEach
	void setUp(){
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		model = new FLEFModel();
		reported = new ArrayList<>();
	}


	private FamilyQuickEntryDialog open(final FLEFRecord selected){
		final FamilyQuickEntryDialog dialog = new FamilyQuickEntryDialog(null, model, selected);
		dialog.setReporter((parent, message) -> reported.add(message));
		return dialog;
	}


	@Test
	void nothingTypedIsNothingToCreate(){
		final FamilyQuickEntryDialog dialog = open(null);

		dialog.onOk();

		final FamilyQuickEntry.Input input = dialog.result();
		Assertions.assertNotNull(input);
		Assertions.assertNull(FamilyQuickEntry.create(model, input));
		Assertions.assertEquals(0, model.getRecordCount());
	}

	@Test
	void aFamilyTypedIsAFamilyToCreate(){
		final FamilyQuickEntryDialog dialog = open(null);
		dialog.personRow().setFullName("Giovanni Rossi");
		dialog.personRow().setSex(SexType.MALE);
		dialog.partnerRow().setFullName("Maria de la Cruz");
		dialog.partnerRow().setSex(SexType.FEMALE);
		dialog.unionRow().type("5 May 1875", "Padova");
		dialog.childRows().getFirst().setFullName("Anna Rossi");
		dialog.addChild();
		dialog.childRows().get(1).setFullName("Carlo Rossi");
		dialog.childRows().get(1).birthRow().type("1878", "");

		dialog.onOk();

		final FamilyQuickEntry.Input input = dialog.result();
		Assertions.assertEquals("spouse", input.unionType());
		Assertions.assertEquals("biological_child", input.childType());
		Assertions.assertEquals(2, input.children().size());
		Assertions.assertNotNull(input.union().date());
		Assertions.assertEquals("Padova", input.union().place().newName());
		final FamilyQuickEntry.Created created = FamilyQuickEntry.create(model, input);
		Assertions.assertEquals(4, model.getRecordsByType(IndividualReader.TYPE).size());
		Assertions.assertEquals("Maria de la Cruz", IndividualReader.extractPrimaryFullname(model.getRecordsByType(IndividualReader.TYPE).get(1)));
		Assertions.assertEquals(5, created.relationships().size());
	}

	@Test
	void theDeathOfEveryPersonIsAskedToo(){
		final FamilyQuickEntryDialog dialog = open(null);
		dialog.personRow().setFullName("Giovanni Rossi");
		dialog.personRow().deathRow().type("1920", "Padova");
		dialog.partnerRow().setFullName("Maria Bianchi");
		dialog.partnerRow().deathRow().type("abt 1930", "");
		dialog.childRows().getFirst().setFullName("Anna");
		dialog.childRows().getFirst().deathRow().type("1900", "");

		dialog.onOk();

		final FamilyQuickEntry.Input input = dialog.result();
		Assertions.assertNotNull(input.first().death().date());
		Assertions.assertEquals("Padova", input.first().death().place().newName());
		Assertions.assertNotNull(input.partner().death().date());
		Assertions.assertNotNull(input.children().getFirst().death().date());
		final FamilyQuickEntry.Created created = FamilyQuickEntry.create(model, input);
		//three people, three deaths
		Assertions.assertEquals(3, model.getRecordsByType(io.github.mtrevisan.familylegacy.io.model.readers.EventReader.TYPE).stream()
			.filter(event -> "death".equals(io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper.getChildValue(event, "type")))
			.count());
		Assertions.assertNotNull(created);
	}

	@Test
	void aPersonWithOnlyADeathIsNotEmptyAndItsRowCannotBeRemoved(){
		final FamilyQuickEntryDialog dialog = open(null);
		Assertions.assertFalse(dialog.removeButton(0).isEnabled());

		dialog.childRows().getFirst().deathRow().type("1900", "");

		Assertions.assertFalse(dialog.childRows().getFirst().isEmpty());
		Assertions.assertTrue(dialog.removeButton(0).isEnabled());
		dialog.dispose();
	}

	@Test
	void theUnionIsAskedOnlyForAPartnerAndForTheTypesThatHaveAnEvent(){
		final FamilyQuickEntryDialog dialog = open(null);
		dialog.unionRow().type("1875", "");
		dialog.personRow().setFullName("Giovanni Rossi");

		//no partner: no union
		dialog.onOk();
		Assertions.assertNull(dialog.result().partner());
		Assertions.assertTrue(dialog.result().union().isEmpty());

		final FamilyQuickEntryDialog withPartner = open(null);
		withPartner.personRow().setFullName("Giovanni Rossi");
		withPartner.partnerRow().setFullName("Maria Bianchi");
		withPartner.unionRow().type("1875", "");
		withPartner.unionTypeCombo().setSelectedItem(RelationshipReader.ENUM_TYPE_COHABITING_PARTNER);
		withPartner.onOk();
		Assertions.assertTrue(withPartner.result().union().isEmpty());

		final FamilyQuickEntryDialog engaged = open(null);
		engaged.personRow().setFullName("Giovanni Rossi");
		engaged.partnerRow().setFullName("Maria Bianchi");
		engaged.unionTypeCombo().setSelectedItem(RelationshipReader.ENUM_TYPE_ENGAGED_PARTNER);
		engaged.unionRow().type("1874", "");
		engaged.onOk();
		Assertions.assertNotNull(engaged.result().union().date());
	}

	@Test
	void theSelectedIndividualIsTheFirstPersonUnlessAnotherIsTyped(){
		final QuickEntryService.Created created = QuickEntryService.createIndividual(model, "Giovanni Rossi", SexType.MALE, null, null);
		final FLEFRecord selected = model.getRecordById(created.individualId());

		final FamilyQuickEntryDialog dialog = open(selected);
		dialog.personRow().setFullName("Someone Else");
		dialog.childRows().getFirst().setFullName("Anna Rossi");
		dialog.onOk();

		Assertions.assertEquals(created.individualId(), dialog.result().first().existingId());
		Assertions.assertEquals(1, dialog.result().children().size());
	}

	@Test
	void aDateThatCannotBeReadStopsTheDialog(){
		final FamilyQuickEntryDialog dialog = open(null);
		dialog.personRow().setFullName("Giovanni Rossi");
		dialog.childRows().getFirst().birthRow().type("bet 1850", "");
		//the row tells the user (a message window would wait for the user)
		dialog.childRows().getFirst().birthRow().setReporter((parent, message) -> reported.add(message));

		dialog.onOk();

		Assertions.assertNull(dialog.result());
		Assertions.assertEquals(1, reported.size());
		dialog.dispose();
	}

	@Test
	void twoParentsOfTheSameSexCannotHaveBiologicalChildren(){
		final FamilyQuickEntryDialog dialog = open(null);
		dialog.personRow().setFullName("Giovanni Rossi");
		dialog.personRow().setSex(SexType.MALE);
		dialog.partnerRow().setFullName("Luigi Bianchi");
		dialog.partnerRow().setSex(SexType.MALE);
		dialog.childRows().getFirst().setFullName("Anna");

		dialog.onOk();

		Assertions.assertNull(dialog.result());
		Assertions.assertEquals(1, reported.size());

		//with another kind of child the same parents are fine
		dialog.childTypeCombo().setSelectedItem(RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD);
		dialog.onOk();
		Assertions.assertEquals(RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD, dialog.result().childType());
	}

	@Test
	void aRowCanBeRemovedOnlyIfThereIsSomethingToRemove(){
		final FamilyQuickEntryDialog dialog = open(null);

		//the only row, empty: nothing to remove
		Assertions.assertFalse(dialog.removeButton(0).isEnabled());

		dialog.childRows().getFirst().setFullName("Anna");
		Assertions.assertTrue(dialog.removeButton(0).isEnabled());

		dialog.childRows().getFirst().setFullName("");
		Assertions.assertFalse(dialog.removeButton(0).isEnabled());

		//what is typed in the birth counts too
		dialog.childRows().getFirst().birthRow().type("1850", "");
		Assertions.assertTrue(dialog.removeButton(0).isEnabled());
		dialog.childRows().getFirst().birthRow().type("", "");
		Assertions.assertFalse(dialog.removeButton(0).isEnabled());

		//with another row, an empty one can be taken away (it was added by mistake)
		dialog.addChild();
		Assertions.assertTrue(dialog.removeButton(0).isEnabled());
		Assertions.assertTrue(dialog.removeButton(1).isEnabled());

		dialog.removeChild(1);
		Assertions.assertFalse(dialog.removeButton(0).isEnabled());
		dialog.dispose();
	}

	@Test
	void childrenAreAddedAndRemovedAndThereIsAlwaysARowToTypeIn(){
		final FamilyQuickEntryDialog dialog = open(null);
		Assertions.assertEquals(1, dialog.childRows().size());

		dialog.addChild();
		dialog.addChild();
		Assertions.assertEquals(3, dialog.childRows().size());

		dialog.removeChild(0);
		dialog.removeChild(0);
		dialog.removeChild(0);
		Assertions.assertEquals(1, dialog.childRows().size());
		dialog.dispose();
	}

	@Test
	void theParentsAreAskedEachWithItsOwnType(){
		final FamilyQuickEntryDialog dialog = open(null);
		dialog.personRow().setFullName("Anna Rossi");
		dialog.fatherRow().setFullName("Giovanni Rossi");
		dialog.motherRow().setFullName("Maria Bianchi");
		dialog.motherTypeCombo().setSelectedItem("adoptive_child");

		dialog.onOk();

		final FamilyQuickEntry.Input input = dialog.result();
		Assertions.assertEquals("biological_child", input.fatherType());
		Assertions.assertEquals("adoptive_child", input.motherType());
		//the father is a man and the mother a woman, unless said otherwise
		Assertions.assertEquals(SexType.MALE, input.father().sex());
		Assertions.assertEquals(SexType.FEMALE, input.mother().sex());
		final FamilyQuickEntry.Created created = FamilyQuickEntry.create(model, input);
		Assertions.assertEquals(3, model.getRecordsByType(IndividualReader.TYPE).size());
		Assertions.assertEquals(2, created.relationships().size());
	}

	@Test
	void noParentTypedIsNoParent(){
		final FamilyQuickEntryDialog dialog = open(null);
		dialog.personRow().setFullName("Anna Rossi");

		dialog.onOk();

		Assertions.assertNull(dialog.result().father());
		Assertions.assertNull(dialog.result().mother());
	}

	@Test
	void twoBiologicalParentsOfTheSameSexAreRefused(){
		final FamilyQuickEntryDialog dialog = open(null);
		dialog.personRow().setFullName("Anna Rossi");
		dialog.fatherRow().setFullName("Giovanni Rossi");
		dialog.motherRow().setFullName("Luigi Rossi");
		dialog.motherRow().setSex(SexType.MALE);

		dialog.onOk();

		Assertions.assertNull(dialog.result());
		Assertions.assertEquals(1, reported.size());
	}

}
