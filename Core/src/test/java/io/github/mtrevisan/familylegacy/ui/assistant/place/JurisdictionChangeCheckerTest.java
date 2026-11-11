package io.github.mtrevisan.familylegacy.ui.assistant.place;

import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


class JurisdictionChangeCheckerTest{

	private static PlaceData.Place place(final String id, final int usage){
		return new PlaceData.Place(id, List.of("Place " + id), null, null, null, usage);
	}

	private static PlaceData.Relation relation(final String id, final String subject, final String target, final String type){
		return new PlaceData.Relation(id, subject, target, type);
	}

	private static List<String> reported(final PlaceData data){
		return JurisdictionChangeChecker.findings(data).stream()
			.map(finding -> finding.recordIds().getFirst())
			.toList();
	}

	@Test
	void aPlaceThatBelongedToTwoAreasIsReported(){
		final PlaceData data = new PlaceData(List.of(place("P1", 3), place("P2", 0), place("P3", 0)),
			List.of(relation("R1", "P1", "P2", PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF),
				relation("R2", "P1", "P3", PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF)));

		Assertions.assertEquals(List.of("P1"), reported(data));
	}

	@Test
	void aSuccessionIsReportedForBothEnds(){
		final PlaceData data = new PlaceData(List.of(place("P1", 2), place("P2", 5)),
			List.of(relation("R1", "P1", "P2", PlaceRelationshipReader.ENUM_TYPE_SUCCESSOR_OF)));

		//the busiest first
		Assertions.assertEquals(List.of("P2", "P1"), reported(data));
	}

	@Test
	void aPlaceWithOneParentOrNoEventsIsNot(){
		final PlaceData data = new PlaceData(List.of(place("P1", 4), place("P2", 0), place("P3", 0), place("P4", 0)),
			List.of(relation("R1", "P1", "P2", PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF),
				relation("R2", "P4", "P2", PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF),
				relation("R3", "P4", "P3", PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF)));

		//P4 changed, but nothing happened there
		Assertions.assertTrue(reported(data).isEmpty());
	}

	@Test
	void theFindingOnlySuggestsAndHasAKey(){
		final PlaceData data = new PlaceData(List.of(place("P1", 1), place("P2", 1)),
			List.of(relation("R1", "P1", "P2", PlaceRelationshipReader.ENUM_TYPE_MERGED_FROM)));

		final Diagnostic finding = JurisdictionChangeChecker.findings(data).getFirst();
		Assertions.assertEquals(Diagnostic.Severity.INFORMATION, finding.severity());
		Assertions.assertEquals("place.jurisdiction_changed", finding.kind());
		Assertions.assertNotNull(finding.action());
	}


	@Test
	void aPlaceWithAHistoricNameBesidesTheCurrentOneIsReported(){
		final PlaceData data = new PlaceData(List.of(
			new PlaceData.Place("P1", List.of("Rijeka", "Fiume"), null, null, null, 2, 0, true),
			new PlaceData.Place("P2", List.of("Treviso"), null, null, null, 2, 0, false),
			new PlaceData.Place("P3", List.of("Zara", "Zadar"), null, null, null, 0, 0, true)), List.of());

		//P2 has no former name, P3 has no events
		Assertions.assertEquals(List.of("P1"), reported(data));
	}

}
