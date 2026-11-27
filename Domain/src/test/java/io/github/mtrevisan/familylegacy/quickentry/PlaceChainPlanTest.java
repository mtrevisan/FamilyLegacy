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
package io.github.mtrevisan.familylegacy.quickentry;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;


/** What a chain of place names means for a file, and the creation of the new places. */
class PlaceChainPlanTest{

	private FLEFModel model;


	@BeforeEach
	void setUp(){
		model = new FLEFModel();
		place("P1", "Veneto");
		place("P2", "Treviso");
		partOf("R1", "P2", "P1");
	}


	private void place(final String id, final String name){
		model.addRecord(FLEFRecord.createMainRecord(id, PlaceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(PlaceReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue(PlaceReader.TAG_NAME_VALUE, name))));
	}

	private void partOf(final String id, final String subject, final String target){
		model.addRecord(FLEFRecord.createMainRecord(id, PlaceRelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(PlaceRelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(PlaceRelationshipReader.TAG_PLACE, subject)))
			.addChild(FLEFRecord.createChildWithTag(PlaceRelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(PlaceRelationshipReader.TAG_PLACE, target)))
			.addChild(FLEFRecord.createChildWithTagAndValue(PlaceRelationshipReader.TAG_TYPE,
				PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF)));
	}

	private List<PlaceChainPlan.Level> plan(final String enclosingId, final String text, final Integer... choices){
		return PlaceChainPlan.plan(model, enclosingId, PlaceChain.names(text), Arrays.asList(choices));
	}


	@Test
	void theNamesThatAreInTheFileAreRecognisedFromTheLargest(){
		final List<PlaceChainPlan.Level> levels = plan(null, "Borgo, Treviso, Veneto");

		Assertions.assertEquals(List.of("Veneto", "Treviso", "Borgo"), levels.stream().map(PlaceChainPlan.Level::name).toList());
		Assertions.assertEquals("P1", levels.get(0).chosen().getId());
		Assertions.assertEquals("P2", levels.get(1).chosen().getId());
		Assertions.assertTrue(levels.get(2).isNew());

		final QuickEntryService.PlaceRef ref = PlaceChainPlan.reference(null, levels);
		Assertions.assertEquals("Borgo", ref.newName());
		Assertions.assertEquals(List.of(), ref.enclosing());
		Assertions.assertEquals("P2", ref.enclosingId());
	}

	@Test
	void aNewNameMakesNewEverythingBelowIt(){
		final List<PlaceChainPlan.Level> levels = plan(null, "Borgo, Villa, Veneto");

		Assertions.assertFalse(levels.get(0).isNew());
		Assertions.assertTrue(levels.get(1).isNew());
		Assertions.assertTrue(levels.get(2).isNew());
		final QuickEntryService.PlaceRef ref = PlaceChainPlan.reference(null, levels);
		Assertions.assertEquals("Borgo", ref.newName());
		Assertions.assertEquals(List.of("Villa"), ref.enclosing());
		Assertions.assertEquals("P1", ref.enclosingId());
	}

	@Test
	void aPlaceThatIsAlreadyThereIsNotCreatedAgain(){
		final List<PlaceChainPlan.Level> levels = plan(null, "Treviso, Veneto");

		final QuickEntryService.PlaceRef ref = PlaceChainPlan.reference(null, levels);

		Assertions.assertEquals("P2", ref.existingId());
		final int before = model.getRecordCount();
		Assertions.assertEquals("P2", PlaceChainPlan.create(model, ref));
		Assertions.assertEquals(before, model.getRecordCount());
	}

	@Test
	void theUserCanChooseANewPlaceInsteadOfTheOneOfTheFile(){
		final List<PlaceChainPlan.Level> levels = plan(null, "Treviso, Veneto", -1);

		//the largest is a new Veneto, and so is everything below it
		Assertions.assertTrue(levels.get(0).isNew());
		Assertions.assertTrue(levels.get(1).isNew());
	}

	@Test
	void twoPlacesWithTheSameNameAreOfferedAndTheChoiceIsKept(){
		place("P3", "Veneto");

		final List<PlaceChainPlan.Level> levels = plan(null, "Treviso, Veneto");
		Assertions.assertEquals(2, levels.get(0).candidates().size());
		//the first by default; the second if the user says so, and then Treviso is not inside it: it is a new one
		Assertions.assertEquals("P1", levels.get(0).chosen().getId());
		final List<PlaceChainPlan.Level> second = plan(null, "Treviso, Veneto", 1);
		Assertions.assertEquals("P3", second.get(0).chosen().getId());
		Assertions.assertTrue(second.get(1).isNew());
	}

	@Test
	void theChainCanBeInsideAPlaceThatIsGiven(){
		final List<PlaceChainPlan.Level> levels = plan("P2", "Borgo");

		final QuickEntryService.PlaceRef ref = PlaceChainPlan.reference("P2", levels);
		Assertions.assertEquals("Borgo", ref.newName());
		Assertions.assertEquals("P2", ref.enclosingId());

		final String id = PlaceChainPlan.create(model, ref);
		//the new place is part of Treviso
		boolean linked = false;
		for(final FLEFRecord relationship : model.getRecordsByType(PlaceRelationshipReader.TYPE))
			linked |= id.equals(relationship.extractReferencedId(PlaceRelationshipReader.TAG_SUBJECT, PlaceReader.TYPE))
				&& "P2".equals(relationship.extractReferencedId(PlaceRelationshipReader.TAG_TARGET, PlaceReader.TYPE));
		Assertions.assertTrue(linked);
	}

	@Test
	void theNewPlacesAreCreatedEachPartOfTheNext(){
		final QuickEntryService.PlaceRef ref = PlaceChainPlan.reference(null, plan(null, "Borgo, Villa, Marca"));

		final String id = PlaceChainPlan.create(model, ref);

		Assertions.assertEquals("Borgo", FLEFRecordHelper.getChildValue(
			FLEFRecordHelper.findChild(model.getRecordById(id), PlaceReader.TAG_NAME), PlaceReader.TAG_NAME_VALUE));
		//three new places and two relationships, on top of the ones that were there
		Assertions.assertEquals(2 + 1 + 3 + 2, model.getRecordCount());
	}

	@Test
	void noNameIsNoPlace(){
		Assertions.assertTrue(PlaceChainPlan.reference(null, plan(null, "  ,  ")).isNone());
	}

}
