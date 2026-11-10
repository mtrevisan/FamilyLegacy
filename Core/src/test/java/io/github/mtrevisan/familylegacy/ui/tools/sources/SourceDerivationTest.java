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
package io.github.mtrevisan.familylegacy.ui.tools.sources;

import io.github.mtrevisan.familylegacy.io.grammar.XrefPathIndex;
import io.github.mtrevisan.familylegacy.io.merger.manual.MergeGrammar;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.sources.ProvenanceService;
import io.github.mtrevisan.familylegacy.sources.SourceHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;


/** The sources that a source is derived from ({@code derived_from}): a transcription of a register, a copy of the transcription. */
class SourceDerivationTest{

	private FLEFModel model;


	/** S1 is a register (an original), S2 a transcription of it (it disagrees on a page), S3 a copy of the transcription, S4 is unrelated. */
	@BeforeEach
	void buildModel(){
		model = new FLEFModel();
		model.addRecord(source("S1"));
		model.addRecord(source("S2", derivedFrom("S1", "p. 12", SourceCitationReader.STANCE_CONTRADICTS)));
		model.addRecord(source("S3", derivedFrom("S2", null, SourceCitationReader.STANCE_SUPPORTS)));
		model.addRecord(source("S4"));
	}

	private static FLEFRecord source(final String id, final FLEFRecord... derivations){
		final FLEFRecord source = FLEFRecord.createMainRecord(id, SourceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(SourceReader.TAG_TITLE)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, "Title of " + id)));
		for(final FLEFRecord derivation : derivations)
			source.addChild(derivation);
		return source;
	}

	private static FLEFRecord derivedFrom(final String originalId, final String locator, final String stance){
		final FLEFRecord citation = FLEFRecord.createChildWithTag(SourceReader.TAG_DERIVED_FROM)
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_SOURCE, originalId))
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_STANCE, stance));
		if(locator != null)
			citation.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_LOCATOR, locator));
		return citation;
	}


	@Test
	void theSourcesThatASourceIsDerivedFromAreItsDerivedFromCitations(){
		Assertions.assertEquals(List.of("S1"), SourceReader.extractDerivedFromIds(model.getRecordById("S2")));
		Assertions.assertTrue(SourceReader.extractDerivedFromIds(model.getRecordById("S1")).isEmpty());
	}

	@Test
	void aSourceDerivesFromItsOriginalDirectlyAndThroughOtherSources(){
		Assertions.assertTrue(SourceHelper.isOrDerivesFrom(model, "S2", "S1"));
		Assertions.assertTrue(SourceHelper.isOrDerivesFrom(model, "S3", "S1"));
		Assertions.assertTrue(SourceHelper.isOrDerivesFrom(model, "S3", "S2"));
		//a source is its own origin (the editor uses it to refuse a source derived from itself)
		Assertions.assertTrue(SourceHelper.isOrDerivesFrom(model, "S1", "S1"));
		//not the other way round, and not an unrelated source
		Assertions.assertFalse(SourceHelper.isOrDerivesFrom(model, "S1", "S3"));
		Assertions.assertFalse(SourceHelper.isOrDerivesFrom(model, "S4", "S1"));
		Assertions.assertFalse(SourceHelper.isOrDerivesFrom(model, null, "S1"));
	}

	@Test
	void aCircleAlreadyInTheFileDoesNotLoopForever(){
		//S1 is made derived from S3, closing the circle S1 -> S3 -> S2 -> S1
		model.getRecordById("S1").addChild(derivedFrom("S3", null, SourceCitationReader.STANCE_SUPPORTS));

		Assertions.assertTrue(SourceHelper.isOrDerivesFrom(model, "S1", "S2"));
		Assertions.assertFalse(SourceHelper.isOrDerivesFrom(model, "S1", "S4"));
	}

	@Test
	void anOriginalThatAnotherSourceIsDerivedFromIsCited(){
		final Map<String, Integer> counts = SourceHelper.countCitationsPerSource(model);

		Assertions.assertEquals(1, counts.get("S1"));
		Assertions.assertEquals(1, counts.get("S2"));
		Assertions.assertFalse(counts.containsKey("S3"));
		Assertions.assertFalse(counts.containsKey("S4"));
	}

	@Test
	void theProvenanceOfASourceIsTheSourcesItIsDerivedFrom(){
		final List<ProvenanceService.Row> rows = ProvenanceService.ofRecord(model, model.getRecordById("S2"));

		Assertions.assertEquals(1, rows.size());
		Assertions.assertEquals("S2", rows.getFirst().ownerId());
		Assertions.assertEquals("S1", rows.getFirst().sourceId());
		Assertions.assertEquals("p. 12", rows.getFirst().locator());
		Assertions.assertTrue(rows.getFirst().contradicts());
	}

	@Test
	void theProvenanceOfAnOriginalListsTheSourcesDerivedFromIt(){
		final List<ProvenanceService.Row> rows = ProvenanceService.ofSource(model, "S1");

		Assertions.assertEquals(List.of("S2"), rows.stream().map(ProvenanceService.Row::ownerId).toList());
		Assertions.assertTrue(ProvenanceService.ofSource(model, "S4").isEmpty());
	}

	@Test
	void theGrammarKnowsThatDerivedFromIsAReferenceToASource(){
		MergeGrammar.instance();

		Assertions.assertTrue(XrefPathIndex.getPaths("SourceRecord")
			.contains("source." + SourceReader.TAG_DERIVED_FROM + "[*]." + SourceCitationReader.TAG_SOURCE),
			XrefPathIndex.getPaths("SourceRecord").toString());
	}

}
