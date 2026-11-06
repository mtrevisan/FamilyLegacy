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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EvidenceQualifiersReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.sources.ProvenanceService;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.provenance.ProvenanceDialog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;


/** The provenance of an assertion: the chain from the record to the citation, the source and the repository. */
class ProvenanceTest{

	private FLEFModel model;
	private FLEFRecord individual;
	private FLEFRecord event;
	private FLEFRecord attribute;


	@BeforeEach
	void buildModel(){
		model = new FLEFModel();

		final FLEFRecord repository = FLEFRecord.createMainRecord("R1", RepositoryReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, "State Archive")));
		final FLEFRecord register = FLEFRecord.createMainRecord("S1", SourceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(SourceReader.TAG_TITLE)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, "Parish register")))
			.addChild(FLEFRecord.createChildWithTag(SourceReader.TAG_REPOSITORY)
				.addChild(FLEFRecord.createChildWithTagAndValue(SourceReader.TAG_REPOSITORY, "R1")));
		final FLEFRecord census = FLEFRecord.createMainRecord("S2", SourceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(SourceReader.TAG_TITLE)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, "Census 1881")));

		individual = FLEFRecord.createMainRecord("I1", IndividualReader.TYPE)
			.addChild(citation("S1", "p. 12", SourceCitationReader.STANCE_SUPPORTS,
				FLEFRecord.createChildWithTag(SourceCitationReader.TAG_EVIDENCE)
					.addChild(FLEFRecord.createChildWithTagAndValue(EvidenceQualifiersReader.TAG_SOURCE_TYPE, EvidenceQualifiersReader.ENUM_SOURCE_TYPE_ORIGINAL))
					.addChild(FLEFRecord.createChildWithTagAndValue(EvidenceQualifiersReader.TAG_INFORMATION_TYPE, EvidenceQualifiersReader.INFORMATION_TYPE_PRIMARY))
					.addChild(FLEFRecord.createChildWithTagAndValue(EvidenceQualifiersReader.TAG_EVIDENCE_TYPE, EvidenceQualifiersReader.ENUM_EVIDENCE_TYPE_DIRECT)),
				FLEFRecord.createChildWithTag(SourceCitationReader.TAG_EXTRACT)
					.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_TEXT, "born on the 3rd"))
					.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_TYPE, SourceCitationReader.ENUM_EXTRACT_TYPE_VERBATIM))
					.addChild(FLEFRecord.createChildWithTag(SourceCitationReader.TAG_MEDIA_PART)
						.addChild(FLEFRecord.createChildWithTagAndValue(SourceReader.TAG_MEDIA, "M1")))));
		event = FLEFRecord.createMainRecord("E1", EventReader.TYPE)
			.addChild(citation("S2", null, SourceCitationReader.STANCE_CONTRADICTS));
		final FLEFRecord participation = FLEFRecord.createMainRecord("P1", EventParticipationReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(EventParticipationReader.TAG_PARTICIPANT)
				.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, "I1")))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, "E1"));
		attribute = FLEFRecord.createMainRecord("A1", IndividualAttributeReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, "I1"))
			.addChild(citation("S1", "p. 40", SourceCitationReader.STANCE_SUPPORTS));

		for(final FLEFRecord record : List.of(repository, register, census, individual, event, participation, attribute))
			model.addRecord(record);
	}

	private static FLEFRecord citation(final String sourceId, final String locator, final String stance, final FLEFRecord... more){
		final FLEFRecord citation = FLEFRecord.createChildWithTag(SourceCitationReader.TAG_SOURCE)
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_SOURCE, sourceId))
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_STANCE, stance));
		if(locator != null)
			citation.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_LOCATOR, locator));
		for(final FLEFRecord child : more)
			citation.addChild(child);
		return citation;
	}


	@Test
	void aRecordGivesItsOwnCitationWithTheWholeChain(){
		final List<ProvenanceService.Row> rows = ProvenanceService.ofRecord(model, individual);

		Assertions.assertEquals(1, rows.size());
		final ProvenanceService.Row row = rows.getFirst();
		Assertions.assertEquals("I1", row.ownerId());
		Assertions.assertEquals("S1", row.sourceId());
		Assertions.assertEquals("Parish register", row.sourceTitle());
		Assertions.assertEquals("p. 12", row.locator());
		Assertions.assertEquals(SourceCitationReader.STANCE_SUPPORTS, row.stance());
		Assertions.assertEquals("born on the 3rd", row.extractText());
		Assertions.assertEquals(SourceCitationReader.ENUM_EXTRACT_TYPE_VERBATIM, row.extractType());
		Assertions.assertEquals("original, primary, direct", row.evidence());
		Assertions.assertEquals("State Archive", row.repositories());
		Assertions.assertEquals("M1", row.documents());
		Assertions.assertTrue(row.isAssessed());
		Assertions.assertFalse(row.contradicts());
	}

	@Test
	void aCitationWithoutEvidenceOrLocatorIsNotAssessedAndSaysSo(){
		final ProvenanceService.Row row = ProvenanceService.ofRecord(model, event).getFirst();

		Assertions.assertEquals("", row.locator());
		Assertions.assertEquals("", row.evidence());
		Assertions.assertFalse(row.isAssessed());
		Assertions.assertTrue(row.contradicts());
		Assertions.assertEquals("", row.repositories());
	}

	@Test
	void aRecordWithoutCitationsHasNoRows(){
		Assertions.assertTrue(ProvenanceService.ofRecord(model, FLEFRecord.createMainRecord("I9", IndividualReader.TYPE)).isEmpty());
		Assertions.assertTrue(ProvenanceService.ofRecord(model, null).isEmpty());
	}

	@Test
	void anIndividualCanIncludeItsEventsAndAttributes(){
		final List<ProvenanceService.Row> rows = ProvenanceService.ofIndividual(model, "I1");

		Assertions.assertEquals(List.of("I1", "E1", "A1"), rows.stream().map(ProvenanceService.Row::ownerId).toList());
		Assertions.assertEquals(1, rows.stream().filter(ProvenanceService.Row::contradicts).count());
		//the record alone does not
		Assertions.assertEquals(1, ProvenanceService.ofRecord(model, individual).size());
	}

	@Test
	void aSourceGivesEverythingThatCitesIt(){
		final List<ProvenanceService.Row> rows = ProvenanceService.ofSource(model, "S1");

		Assertions.assertEquals(2, rows.size());
		Assertions.assertTrue(rows.stream().map(ProvenanceService.Row::ownerId).toList().containsAll(List.of("I1", "A1")));
		Assertions.assertEquals(List.of("p. 12", "p. 40"), rows.stream().map(ProvenanceService.Row::locator).sorted().toList());

		Assertions.assertTrue(ProvenanceService.ofSource(model, "S9").isEmpty());
		Assertions.assertTrue(ProvenanceService.ofSource(model, null).isEmpty());
	}

	@Test
	void theSummaryCountsTheStancesAndTheAssessedCitations(){
		final List<ProvenanceService.Row> rows = ProvenanceService.ofIndividual(model, "I1");

		Assertions.assertEquals("3 citation(s): 2 support, 1 contradict; evidence assessed in 1.", ProvenanceDialog.summaryOf(rows, false));
		Assertions.assertTrue(ProvenanceDialog.summaryOf(List.of(), false).startsWith("No citation"));
		Assertions.assertTrue(ProvenanceDialog.summaryOf(List.of(), true).startsWith("No record cites"));
	}

}
