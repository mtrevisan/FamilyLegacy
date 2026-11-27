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
import io.github.mtrevisan.familylegacy.io.model.ModelHistory;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


/**
 * A quick entry is a step of the undo history of the file: what it creates (the people, the events, the places, the relationships) is
 * taken back together by Undo, and put back together by Redo, as the history records it with the checkpoint at the end of the action.
 */
class QuickEntryHistoryTest{

	private static FamilyQuickEntry.Person person(final String name, final String birth, final String place){
		return new FamilyQuickEntry.Person(null, FullNameParser.parse(name), SexType.UNKNOWN, new QuickEntryService.LifeFact(
			DateStructureBuilder.fromShorthand(birth, null), QuickEntryService.PlaceRef.inside(place, List.of("Veneto"), null)));
	}


	@Test
	void aFamilyIsOneStepThatUndoTakesBackAndRedoPutsBack(){
		final FLEFModel model = new FLEFModel();
		QuickEntryService.createIndividual(model, "Giovanni Rossi", SexType.MALE, null, null);
		final ModelHistory history = new ModelHistory(model);
		final int before = model.getRecordCount();

		final FamilyQuickEntry.Created created = FamilyQuickEntry.create(model, new FamilyQuickEntry.Input(
			FamilyQuickEntry.Person.existing(model.getRecordsByType(IndividualReader.TYPE).getFirst().getId()),
			person("Maria Bianchi", "1852", "Padova"), "civil_spouse", new QuickEntryService.LifeFact(
				DateStructureBuilder.fromShorthand("5 May 1875", null), QuickEntryService.PlaceRef.named("Padova")),
			"biological_child", List.of(person("Anna Rossi", "1876", "Padova"), person("Carlo Rossi", "1878", "Padova"))));
		FamilyQuickEntry.addRelationships(model, created.relationships());
		final int after = model.getRecordCount();
		Assertions.assertTrue(after > before);

		history.checkpoint(ModelHistory.Origin.DIALOG);
		Assertions.assertTrue(history.canUndo());

		Assertions.assertTrue(history.undo());
		Assertions.assertEquals(before, model.getRecordCount());
		Assertions.assertEquals(1, model.getRecordsByType(IndividualReader.TYPE).size());
		Assertions.assertEquals(0, model.getRecordsByType(RelationshipReader.TYPE).size());
		Assertions.assertEquals(0, model.getRecordsByType(EventReader.TYPE).size());
		Assertions.assertEquals(0, model.getRecordsByType(PlaceReader.TYPE).size());
		//one step: nothing else to undo
		Assertions.assertFalse(history.canUndo());

		Assertions.assertTrue(history.redo());
		Assertions.assertEquals(after, model.getRecordCount());
		Assertions.assertEquals(4, model.getRecordsByType(IndividualReader.TYPE).size());
		Assertions.assertEquals(5, model.getRecordsByType(RelationshipReader.TYPE).size());
	}

	@Test
	void theStepKnowsEveryRecordThatTheQuickEntryAdded(){
		final FLEFModel model = new FLEFModel();
		final ModelHistory history = new ModelHistory(model);

		final FamilyQuickEntry.Created created = FamilyQuickEntry.create(model, new FamilyQuickEntry.Input(person("Giovanni Rossi", "1850", "Padova"),
			null, null, null, null, List.of()));
		history.checkpoint(ModelHistory.Origin.DIALOG);

		final List<ModelHistory.Entry> entries = history.recentChanges();
		Assertions.assertEquals(1, entries.size());
		Assertions.assertEquals(created.recordIds().size(), entries.getFirst().changes().size());
		Assertions.assertTrue(entries.getFirst().changes().stream()
			.allMatch(change -> change.kind() == ModelHistory.Kind.ADDED));
	}

}
