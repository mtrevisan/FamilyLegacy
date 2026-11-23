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
package io.github.mtrevisan.familylegacy.io.model;

import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


/** The free tags of the records: they are free text, they follow merges and deletions, and the read-only views share them. */
class RecordTagsTest{

	@Test
	void aTagIsTrimmedItsSpacesCollapsedAndTheCaseIsIgnored(){
		final RecordTags tags = new RecordTags();

		tags.set("I1", List.of("  Brick   Wall ", "brick wall", "To Verify", " "));

		Assertions.assertEquals(List.of("Brick Wall", "To Verify"), tags.get("I1"));
		Assertions.assertTrue(tags.has("I1", "BRICK WALL"));
		Assertions.assertFalse(tags.has("I1", "DNA Candidate"));
		Assertions.assertTrue(tags.hasAll("I1", List.of("to verify", "brick wall")));
		Assertions.assertFalse(tags.hasAll("I1", List.of("to verify", "dna")));
		Assertions.assertTrue(tags.hasAll("I2", List.of()));
	}

	@Test
	void severalTagsTypedTogetherAreSeparatedByCommasOrSemicolons(){
		Assertions.assertEquals(List.of("Immigration", "Open Conflict", "FAN Cluster"),
			RecordTags.parse("Immigration, Open Conflict;FAN Cluster, immigration,"));
		Assertions.assertTrue(RecordTags.parse(null).isEmpty());
		Assertions.assertTrue(RecordTags.parse("  ,; ").isEmpty());
	}

	@Test
	void onlySomeRecordsCanBeTagged(){
		Assertions.assertTrue(RecordTags.isTaggable("individual"));
		Assertions.assertTrue(RecordTags.isTaggable("RESEARCH_TASK"));
		Assertions.assertFalse(RecordTags.isTaggable("EVENT"));
		Assertions.assertFalse(RecordTags.isTaggable(null));
	}

	@Test
	void tagsAreAddedAndRemovedAndTheListenersAreToldOnlyOfRealChanges(){
		final RecordTags tags = new RecordTags();
		final AtomicInteger changes = new AtomicInteger();
		tags.addChangeListener(changes::incrementAndGet);

		tags.add("I1", "Brick Wall");
		tags.add("I1", "brick wall");
		tags.add("I1", "Immigration");
		tags.remove("I1", "BRICK WALL");
		tags.remove("I1", "absent");

		Assertions.assertEquals(List.of("Immigration"), tags.get("I1"));
		Assertions.assertEquals(3, changes.get());

		tags.clear("I1");

		Assertions.assertTrue(tags.isEmpty());
		Assertions.assertEquals(4, changes.get());
	}

	@Test
	void theSurvivorOfAMergeHasTheTagsOfBothAndTheAbsorbedHasNone(){
		final RecordTags tags = new RecordTags();
		tags.set("I1", List.of("Brick Wall", "To Verify"));
		tags.set("I2", List.of("to verify", "DNA Candidate"));

		tags.merge("I2", "I1");

		Assertions.assertEquals(List.of("Brick Wall", "DNA Candidate", "To Verify"), tags.get("I1"));
		Assertions.assertTrue(tags.get("I2").isEmpty());
	}

	@Test
	void theTagsOfARecordAreForgottenWhenItIsRemovedFromTheModel(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("P1", PlaceReader.TYPE));
		model.tags().set("I1", List.of("Brick Wall"));
		model.tags().set("P1", List.of("Brick Wall"));

		model.removeRecord("I1");

		Assertions.assertTrue(model.tags().get("I1").isEmpty());
		Assertions.assertEquals(Map.of("Brick Wall", 1), model.tags().counts(model::hasRecord));
	}

	@Test
	void theCountsOfTheTagsIgnoreTheRecordsThatAreNotThere(){
		final RecordTags tags = new RecordTags();
		tags.set("I1", List.of("Brick Wall"));
		tags.set("I2", List.of("brick wall", "Immigration"));
		tags.set("gone", List.of("Immigration"));

		Assertions.assertEquals(Map.of("Brick Wall", 2, "Immigration", 1), tags.counts(id -> !id.equals("gone")));

		tags.retainOnly(id -> !id.equals("gone"));

		Assertions.assertEquals(2, tags.snapshot().size());
	}

	@Test
	void aReadOnlyViewOfTheModelSharesItsTags(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE));
		final ModelHistory history = new ModelHistory(model);
		model.tags().set("I1", List.of("Brick Wall"));

		final FLEFModel view = history.readOnlyView();

		Assertions.assertEquals(List.of("Brick Wall"), view.tags().get("I1"));
		model.tags().add("I1", "Immigration");
		Assertions.assertEquals(List.of("Brick Wall", "Immigration"), view.tags().get("I1"));
	}

	@Test
	void theTagsAreNotPartOfTheModelThatIsWrittenToTheFile(){
		final FLEFModel plain = new FLEFModel();
		plain.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE));
		final FLEFModel tagged = new FLEFModel();
		tagged.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE));

		tagged.tags().set("I1", List.of("Brick Wall"));

		Assertions.assertEquals(plain, tagged);
		Assertions.assertEquals(plain.hashCode(), tagged.hashCode());
	}

}
