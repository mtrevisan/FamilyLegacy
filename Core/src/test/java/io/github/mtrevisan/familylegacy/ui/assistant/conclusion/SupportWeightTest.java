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
package io.github.mtrevisan.familylegacy.ui.assistant.conclusion;

import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EvidenceQualifiersReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/** What the citations of an assertion weigh, with their evidence qualifiers. */
class SupportWeightTest{

	private static FLEFRecord citation(final String... qualifiers){
		final FLEFRecord citation = FLEFRecord.createChildWithTagAndValue(EventReader.TAG_SOURCE, "S1");
		if(qualifiers.length > 0){
			final FLEFRecord evidence = FLEFRecord.createChildWithTagAndValue(EvidenceQualifiersReader.TAG_EVIDENCE, null);
			for(int i = 0; i < qualifiers.length; i += 2)
				evidence.addChild(FLEFRecord.createChildWithTagAndValue(qualifiers[i], qualifiers[i + 1]));
			citation.addChild(evidence);
		}
		return citation;
	}

	private static FLEFRecord event(final FLEFRecord... citations){
		final FLEFRecord event = FLEFRecord.createMainRecord("E1", EventReader.TYPE);
		for(final FLEFRecord citation : citations)
			event.addChild(citation);
		return event;
	}


	@Test
	void noCitationWeighsNothing(){
		Assertions.assertEquals(0, SourceCitationReader.supportWeight(event()));
	}

	@Test
	void aPlainCitationCountsOne(){
		Assertions.assertEquals(2, SourceCitationReader.supportWeight(event(citation(), citation())));
	}

	@Test
	void eachQualifierThatStrengthensTheEvidenceAddsOne(){
		Assertions.assertEquals(4, SourceCitationReader.supportWeight(event(citation(
			EvidenceQualifiersReader.TAG_SOURCE_TYPE, EvidenceQualifiersReader.ENUM_SOURCE_TYPE_ORIGINAL,
			EvidenceQualifiersReader.TAG_INFORMATION_TYPE, EvidenceQualifiersReader.INFORMATION_TYPE_PRIMARY,
			EvidenceQualifiersReader.TAG_EVIDENCE_TYPE, EvidenceQualifiersReader.ENUM_EVIDENCE_TYPE_DIRECT))));
		Assertions.assertEquals(1, SourceCitationReader.supportWeight(event(citation(
			EvidenceQualifiersReader.TAG_SOURCE_TYPE, EvidenceQualifiersReader.ENUM_SOURCE_TYPE_DERIVED,
			EvidenceQualifiersReader.TAG_INFORMATION_TYPE, EvidenceQualifiersReader.INFORMATION_TYPE_SECONDARY,
			EvidenceQualifiersReader.TAG_EVIDENCE_TYPE, EvidenceQualifiersReader.ENUM_EVIDENCE_TYPE_INDIRECT))));
	}

	@Test
	void aContradictingOrNegativeCitationCountsAgainst(){
		final FLEFRecord contradicting = citation();
		contradicting.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_STANCE, SourceCitationReader.STANCE_CONTRADICTS));

		Assertions.assertEquals(-1, SourceCitationReader.supportWeight(event(contradicting)));
		Assertions.assertEquals(-1, SourceCitationReader.supportWeight(event(citation(
			EvidenceQualifiersReader.TAG_EVIDENCE_TYPE, EvidenceQualifiersReader.EVIDENCE_TYPE_NEGATIVE))));
	}

}
