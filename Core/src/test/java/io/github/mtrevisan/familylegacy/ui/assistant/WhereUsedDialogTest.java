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
package io.github.mtrevisan.familylegacy.ui.assistant;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


/** The records that point to the records of a finding: what the researcher looks at before deciding what to do with a duplicate. */
class WhereUsedDialogTest{

	private static FLEFModel model(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("P1", "place"));
		model.addRecord(FLEFRecord.createMainRecord("P2", "place"));
		model.addRecord(FLEFRecord.createMainRecord("E1", "event")
			.addChild(FLEFRecord.createChildWithTag("place")
				.addChild(FLEFRecord.createChildWithTagAndValue("place", "P1"))));
		model.addRecord(FLEFRecord.createMainRecord("E2", "event")
			.addChild(FLEFRecord.createChildWithTag("place")
				.addChild(FLEFRecord.createChildWithTagAndValue("place", "P1"))));
		return model;
	}


	@Test
	void everyRecordOfTheFindingIsFollowedByTheRecordsThatPointToItOrByTheSayingThatNoneDoes(){
		final FLEFModel model = model();

		final List<WhereUsedDialog.Row> rows = WhereUsedDialog.rows(model,
			List.of(model.getRecordById("P1"), model.getRecordById("P2")));

		Assertions.assertEquals(5, rows.size(), rows.toString());
		Assertions.assertEquals(new WhereUsedDialog.Subject(model.getRecordById("P1"), 2), rows.get(0));
		Assertions.assertEquals("E1", ((WhereUsedDialog.Use)rows.get(1)).owner().getId());
		Assertions.assertEquals("place.place", ((WhereUsedDialog.Use)rows.get(1)).path());
		Assertions.assertEquals("E2", ((WhereUsedDialog.Use)rows.get(2)).owner().getId());
		Assertions.assertEquals(new WhereUsedDialog.Subject(model.getRecordById("P2"), 0), rows.get(3));
		Assertions.assertInstanceOf(WhereUsedDialog.None.class, rows.get(4));
	}

	@Test
	void everyFindingAboutRecordsIsOneKeyAwayFromWhereItsRecordsAreUsed(){
		final Diagnostic finding = new Diagnostic(Diagnostic.Severity.WARNING, AssistantCategory.PLACES, "Possible duplicate Place", "why",
			List.of("P1", "P2"));

		Assertions.assertFalse(finding.recordIds().isEmpty());
		Assertions.assertEquals("Where Used…", new WhereUsedAction(finding.recordIds()).title());
	}

}
