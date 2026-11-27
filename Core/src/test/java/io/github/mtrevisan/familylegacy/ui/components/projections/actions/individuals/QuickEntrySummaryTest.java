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
package io.github.mtrevisan.familylegacy.ui.components.projections.actions.individuals;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.quickentry.DateStructureBuilder;
import io.github.mtrevisan.familylegacy.quickentry.FamilyQuickEntry;
import io.github.mtrevisan.familylegacy.quickentry.FullNameParser;
import io.github.mtrevisan.familylegacy.quickentry.QuickEntryService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


/** The line that tells what a quick entry created, and how to take it back. */
class QuickEntrySummaryTest{

	private static FamilyQuickEntry.Person person(final String name, final String birth, final String place){
		return new FamilyQuickEntry.Person(null, FullNameParser.parse(name), SexType.UNKNOWN, new QuickEntryService.LifeFact(
			DateStructureBuilder.fromShorthand(birth, null), QuickEntryService.PlaceRef.named(place)));
	}


	@Test
	void itCountsThePeopleTheEventsThePlacesAndTheRelationships(){
		final FLEFModel model = new FLEFModel();
		final FamilyQuickEntry.Created created = FamilyQuickEntry.create(model, new FamilyQuickEntry.Input(person("Giovanni Rossi", "1850", "Padova"),
			person("Maria Bianchi", "1852", "Padova"), "spouse", null, "biological_child", List.of(person("Anna Rossi", "1876", null))));

		final String summary = QuickEntryTool.summary(model, created);

		Assertions.assertTrue(summary.contains("3 people"), summary);
		Assertions.assertTrue(summary.contains("3 events"), summary);
		Assertions.assertTrue(summary.contains("1 place"), summary);
		Assertions.assertFalse(summary.contains("1 places"), summary);
		Assertions.assertTrue(summary.contains("3 relationships"), summary);
		Assertions.assertTrue(summary.contains("Undo"), summary);
	}

	@Test
	void whatWasNotCreatedIsNotMentionedAndOneIsSingular(){
		final FLEFModel model = new FLEFModel();
		final FamilyQuickEntry.Created created = FamilyQuickEntry.create(model, new FamilyQuickEntry.Input(person("Giovanni Rossi", null, null),
			null, null, null, null, List.of()));

		final String summary = QuickEntryTool.summary(model, created);

		Assertions.assertTrue(summary.contains("1 person"), summary);
		Assertions.assertFalse(summary.contains("people"), summary);
		Assertions.assertFalse(summary.contains("event"), summary);
		Assertions.assertFalse(summary.contains("place"), summary);
		Assertions.assertFalse(summary.contains("relationship"), summary);
	}

}
