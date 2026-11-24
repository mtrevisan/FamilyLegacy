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
package io.github.mtrevisan.familylegacy.sources;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.ExtractReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/** What an extract supports and what it contradicts. */
class ExtractEvidenceTest{

	private static FLEFRecord citation(final String extractId, final String stance){
		return FLEFRecord.createChildWithTag(SourceCitationReader.TAG_SOURCE)
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_SOURCE, "S1"))
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_STANCE, stance))
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_EXTRACT, extractId));
	}

	private static FLEFModel model(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("A", IndividualReader.TYPE)
			.addChild(citation("X1", SourceCitationReader.STANCE_SUPPORTS)));
		model.addRecord(FLEFRecord.createMainRecord("B", IndividualReader.TYPE)
			.addChild(citation("X1", SourceCitationReader.STANCE_CONTRADICTS)));
		model.addRecord(FLEFRecord.createMainRecord("C", IndividualReader.TYPE)
			.addChild(citation("X2", SourceCitationReader.STANCE_SUPPORTS)));
		model.addRecord(FLEFRecord.createMainRecord("X1", ExtractReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(ExtractReader.TAG_SOURCE, "S1")));
		model.addRecord(FLEFRecord.createMainRecord("X2", ExtractReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("S1", SourceReader.TYPE));
		return model;
	}


	@Test
	void anExtractSplitsItsAssertionsBySupportAndContradiction(){
		final ExtractEvidence evidence = ExtractEvidence.of(model(), "X1");

		Assertions.assertEquals("S1", evidence.source().getId());
		Assertions.assertEquals(1, evidence.supports().size());
		Assertions.assertEquals("A", evidence.supports().getFirst().owner().getId());
		Assertions.assertEquals("A", evidence.supports().getFirst().individuals().getFirst().getId());
		Assertions.assertEquals(1, evidence.contradicts().size());
		Assertions.assertEquals("B", evidence.contradicts().getFirst().owner().getId());
	}

	@Test
	void anExtractNobodyReferencesHasNothingAndANonExtractHasNoEvidence(){
		final FLEFModel model = model();

		Assertions.assertTrue(ExtractEvidence.of(model, "X2").contradicts().isEmpty());
		Assertions.assertEquals(1, ExtractEvidence.of(model, "X2").supports().size());
		Assertions.assertNull(ExtractEvidence.of(model, "A"));
		Assertions.assertNull(ExtractEvidence.of(model, null));
	}

}
