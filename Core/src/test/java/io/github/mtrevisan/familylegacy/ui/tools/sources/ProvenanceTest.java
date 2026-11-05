package io.github.mtrevisan.familylegacy.ui.tools.sources;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
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
			.addChild(FLEFRecord.createChildWithTag("name")
				.addChild(FLEFRecord.createChildWithTagAndValue("value", "State Archive")));
		final FLEFRecord register = FLEFRecord.createMainRecord("S1", SourceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag("title")
				.addChild(FLEFRecord.createChildWithTagAndValue("value", "Parish register")))
			.addChild(FLEFRecord.createChildWithTag("repository")
				.addChild(FLEFRecord.createChildWithTagAndValue("repository", "R1")));
		final FLEFRecord census = FLEFRecord.createMainRecord("S2", SourceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag("title")
				.addChild(FLEFRecord.createChildWithTagAndValue("value", "Census 1881")));

		individual = FLEFRecord.createMainRecord("I1", IndividualReader.TYPE)
			.addChild(citation("S1", "p. 12", "supports",
				FLEFRecord.createChildWithTag("evidence")
					.addChild(FLEFRecord.createChildWithTagAndValue("source_type", "original"))
					.addChild(FLEFRecord.createChildWithTagAndValue("information_type", "primary"))
					.addChild(FLEFRecord.createChildWithTagAndValue("evidence_type", "direct")),
				FLEFRecord.createChildWithTag("extract")
					.addChild(FLEFRecord.createChildWithTagAndValue("text", "born on the 3rd"))
					.addChild(FLEFRecord.createChildWithTagAndValue("type", "verbatim"))
					.addChild(FLEFRecord.createChildWithTag("media_part")
						.addChild(FLEFRecord.createChildWithTagAndValue("media", "M1")))));
		event = FLEFRecord.createMainRecord("E1", EventReader.TYPE)
			.addChild(citation("S2", null, "contradicts"));
		final FLEFRecord participation = FLEFRecord.createMainRecord("P1", EventParticipationReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag("participant")
				.addChild(FLEFRecord.createChildWithTagAndValue("individual", "I1")))
			.addChild(FLEFRecord.createChildWithTagAndValue("event", "E1"));
		attribute = FLEFRecord.createMainRecord("A1", IndividualAttributeReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue("individual", "I1"))
			.addChild(citation("S1", "p. 40", "supports"));

		for(final FLEFRecord record : List.of(repository, register, census, individual, event, participation, attribute))
			model.addRecord(record);
	}

	private static FLEFRecord citation(final String sourceId, final String locator, final String stance, final FLEFRecord... more){
		final FLEFRecord citation = FLEFRecord.createChildWithTag("source")
			.addChild(FLEFRecord.createChildWithTagAndValue("source", sourceId))
			.addChild(FLEFRecord.createChildWithTagAndValue("stance", stance));
		if(locator != null)
			citation.addChild(FLEFRecord.createChildWithTagAndValue("locator", locator));
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
		Assertions.assertEquals("supports", row.stance());
		Assertions.assertEquals("born on the 3rd", row.extractText());
		Assertions.assertEquals("verbatim", row.extractType());
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
