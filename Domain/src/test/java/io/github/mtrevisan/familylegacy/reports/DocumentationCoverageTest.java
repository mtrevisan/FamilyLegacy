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
package io.github.mtrevisan.familylegacy.reports;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


/** What the documentation coverage counts as documented, where it finds the gaps and which branch it calls the weakest. */
class DocumentationCoverageTest{

	private static FLEFRecord citation(final String sourceId, final String stance){
		return FLEFRecord.createChildWithTag(SourceCitationReader.TAG_SOURCE)
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_SOURCE, sourceId))
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_STANCE, stance));
	}

	private static FLEFRecord individual(final String id){
		return FLEFRecord.createMainRecord(id, IndividualReader.TYPE);
	}

	private static FLEFRecord relationship(final String id, final String subject, final String target, final String type){
		return FLEFRecord.createMainRecord(id, RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_INDIVIDUAL, subject)))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_INDIVIDUAL, target)))
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, type));
	}

	private static FLEFRecord event(final String id, final String type, final String placeId){
		final FLEFRecord event = FLEFRecord.createMainRecord(id, EventReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_TYPE, type));
		if(placeId != null)
			event.addChild(FLEFRecord.createChildWithTag(EventReader.TAG_PLACE)
				.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_PLACE, placeId)));
		return event;
	}

	private static List<String> ids(final List<FLEFRecord> records){
		return records.stream()
			.map(FLEFRecord::getId)
			.toList();
	}

	/**
	 * <pre>
	 * A (founder) --+-- B       A's birth is cited; B has nothing; C has a cited attribute
	 *               +-- C       W is A's cited spouse, V the uncited one
	 * D (alone)
	 * </pre>
	 */
	private static FLEFModel model(){
		final FLEFModel model = new FLEFModel();
		for(final String id : new String[]{"A", "B", "C", "D", "V", "W"})
			model.addRecord(individual(id));
		model.addRecord(relationship("R1", "B", "A", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD));
		model.addRecord(relationship("R2", "C", "A", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD));
		model.addRecord(relationship("R3", "W", "A", RelationshipReader.ENUM_TYPE_SPOUSE)
			.addChild(citation("S1", SourceCitationReader.STANCE_SUPPORTS)));
		model.addRecord(relationship("R4", "V", "A", RelationshipReader.ENUM_TYPE_SPOUSE)
			//a citation that contradicts documents nothing
			.addChild(citation("S1", SourceCitationReader.STANCE_CONTRADICTS)));
		model.addRecord(FLEFRecord.createMainRecord("AT1", IndividualAttributeReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_INDIVIDUAL, "C"))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_TYPE, "occupation"))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_VALUE, "weaver"))
			.addChild(citation("S1", SourceCitationReader.STANCE_SUPPORTS)));

		model.addRecord(FLEFRecord.createMainRecord("P1", PlaceReader.TYPE)
			.addChild(citation("S1", SourceCitationReader.STANCE_SUPPORTS)));
		model.addRecord(FLEFRecord.createMainRecord("P2", PlaceReader.TYPE));
		model.addRecord(event("E1", EventReader.ENUM_TYPE_BIRTH, "P2")
			.addChild(citation("S1", SourceCitationReader.STANCE_SUPPORTS)));
		model.addRecord(event("E2", EventReader.ENUM_TYPE_DEATH, "P2"));
		model.addRecord(event("E3", EventReader.ENUM_TYPE_DEATH, null));
		model.addRecord(FLEFRecord.createMainRecord("EP1", EventParticipationReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(EventParticipationReader.TAG_PARTICIPANT)
				.addChild(FLEFRecord.createChildWithTagAndValue(
					EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, "A")))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, "E1"))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_ROLE,
				EventParticipationReader.ENUM_ROLE_PRINCIPAL)));

		//the source refers to M1, nobody refers to M2
		model.addRecord(FLEFRecord.createMainRecord("S1", SourceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceReader.TAG_TITLE, "Parish register"))
			.addChild(FLEFRecord.createChildWithTagAndValue(MediaReader.TYPE.toLowerCase(), "M1")));
		model.addRecord(FLEFRecord.createMainRecord("M1", MediaReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("M2", MediaReader.TYPE));
		return model;
	}


	@Test
	void anIndividualIsDocumentedByItsFactsAndAContradictingCitationDocumentsNothing(){
		final DocumentationCoverage coverage = DocumentationCoverage.analyze(model());

		final DocumentationCoverage.KindCoverage individuals = coverage.of(DocumentationCoverage.Kind.INDIVIDUAL);
		Assertions.assertEquals(6, individuals.total());
		//A through the birth, C through the attribute, W through the cited partnership
		Assertions.assertEquals(List.of("B", "D", "V"), ids(individuals.undocumented()));
		Assertions.assertEquals(50., individuals.percentage(), 1e-9);
	}

	@Test
	void aFamilyIsAPartnershipAndItNeedsItsOwnCitation(){
		final DocumentationCoverage.KindCoverage families = DocumentationCoverage.analyze(model()).of(DocumentationCoverage.Kind.FAMILY);

		Assertions.assertEquals(2, families.total());
		Assertions.assertEquals(List.of("R4"), ids(families.undocumented()));
	}

	@Test
	void aCitedMarriageEventOfTheCoupleDocumentsTheFamily(){
		final FLEFModel model = model();
		model.addRecord(event("E4", EventReader.ENUM_TYPE_MARRIAGE, null)
			.addChild(citation("S1", SourceCitationReader.STANCE_SUPPORTS)));
		for(final String person : new String[]{"A", "V"})
			model.addRecord(FLEFRecord.createMainRecord("EP4" + person, EventParticipationReader.TYPE)
				.addChild(FLEFRecord.createChildWithTag(EventParticipationReader.TAG_PARTICIPANT)
					.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, person)))
				.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, "E4"))
				.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_ROLE, EventParticipationReader.ENUM_ROLE_PRINCIPAL)));

		final DocumentationCoverage.KindCoverage families = DocumentationCoverage.analyze(model).of(DocumentationCoverage.Kind.FAMILY);

		Assertions.assertEquals(2, families.total());
		Assertions.assertTrue(families.undocumented().isEmpty());
	}

	@Test
	void eventsPlacesAndMediaAreCountedEachByItsOwnRule(){
		final DocumentationCoverage coverage = DocumentationCoverage.analyze(model());

		Assertions.assertEquals(List.of("E2", "E3"), ids(coverage.of(DocumentationCoverage.Kind.EVENT).undocumented()));
		Assertions.assertEquals(List.of("P2"), ids(coverage.of(DocumentationCoverage.Kind.PLACE).undocumented()));
		Assertions.assertEquals(2, coverage.of(DocumentationCoverage.Kind.MEDIA).total());
		Assertions.assertEquals(List.of("M2"), ids(coverage.of(DocumentationCoverage.Kind.MEDIA).undocumented()));
	}

	@Test
	void theHotspotsAreThePlacesAndTheKindsOfEventWithTheMostGaps(){
		final DocumentationCoverage coverage = DocumentationCoverage.analyze(model());

		Assertions.assertEquals(1, coverage.placeHotspots().size());
		final DocumentationCoverage.PlaceHotspot place = coverage.placeHotspots().getFirst();
		Assertions.assertEquals("P2", place.place().getId());
		Assertions.assertEquals(1, place.undocumented());
		Assertions.assertEquals(2, place.total());

		//the births are all cited: only the deaths are a hotspot
		Assertions.assertEquals(1, coverage.eventTypeHotspots().size());
		final DocumentationCoverage.EventTypeHotspot type = coverage.eventTypeHotspots().getFirst();
		Assertions.assertEquals(EventReader.ENUM_TYPE_DEATH, type.type());
		Assertions.assertEquals(2, type.undocumented());
		Assertions.assertEquals(2, type.total());
	}

	@Test
	void aBranchIsAFounderAndTheDescendantsAndALoneIndividualIsNotOne(){
		final DocumentationCoverage coverage = DocumentationCoverage.analyze(model());

		Assertions.assertEquals(1, coverage.branches().size());
		final DocumentationCoverage.Branch branch = coverage.branches().getFirst();
		Assertions.assertEquals("A", branch.founder().getId());
		Assertions.assertEquals(3, branch.size());
		//A and C are documented, B is not
		Assertions.assertEquals(2, branch.documented());
	}

	@Test
	void theOverallFiguresAddUpTheKinds(){
		final DocumentationCoverage coverage = DocumentationCoverage.analyze(model());

		//3 individuals + 1 family + 2 events + 1 place + 1 medium
		Assertions.assertEquals(8, coverage.totalUndocumented());
		//6 + 2 + 3 + 2 + 2 = 15 records, 7 documented
		Assertions.assertEquals(100. * 7 / 15, coverage.overallPercentage(), 1e-9);
	}

	@Test
	void anEmptyFileHasNothingToDocument(){
		final DocumentationCoverage coverage = DocumentationCoverage.analyze(new FLEFModel());

		Assertions.assertEquals(0, coverage.totalUndocumented());
		Assertions.assertEquals(100., coverage.overallPercentage(), 1e-9);
		Assertions.assertTrue(coverage.branches().isEmpty());
		Assertions.assertTrue(coverage.placeHotspots().isEmpty());
	}

}
