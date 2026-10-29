package io.github.mtrevisan.familylegacy.ui.assistant.place;

import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;


class PlaceCheckerTest{

	private static PlaceData.Place place(final String id, final String name, final String type){
		return new PlaceData.Place(id, List.of(name), type, null, null, 0);
	}

	private static PlaceData.Place place(final String id, final String name, final String type, final double lat, final double lon,
			final int usage){
		return new PlaceData.Place(id, List.of(name), type, lat, lon, usage);
	}

	private static PlaceData.Place used(final String id, final String name, final String type, final int usage){
		return new PlaceData.Place(id, List.of(name), type, null, null, usage);
	}

	private static PlaceData.Relation part(final String id, final String subject, final String target){
		return new PlaceData.Relation(id, subject, target, "administrative_part_of");
	}

	private static List<PlaceFinding> findings(final List<PlaceData.Place> places, final List<PlaceData.Relation> relations){
		return PlaceChecker.findings(new PlaceData(places, relations));
	}

	private static List<PlaceFinding> ofKind(final List<PlaceFinding> findings, final String kind){
		return findings.stream().filter(f -> f.diagnostic().key().startsWith(kind + "|")).toList();
	}


	@Test
	void coordinatesAreReadFromIso6709(){
		Assertions.assertArrayEquals(new double[]{45.65, -12.2}, PlaceChecker.parseCoordinates("+45.65-012.2/"));
		Assertions.assertArrayEquals(new double[]{-33., 151.}, PlaceChecker.parseCoordinates("-33+151/"));
		Assertions.assertNull(PlaceChecker.parseCoordinates("45.65, 12.2"));
		Assertions.assertNull(PlaceChecker.parseCoordinates("+95.0+012.0/"));
		Assertions.assertNull(PlaceChecker.parseCoordinates(null));
	}

	@Test
	void namesAreComparedWithoutCaseAccentsAndPunctuation(){
		Assertions.assertEquals("san pietro di feletto", PlaceChecker.normalize("Sán  PIETRO, di Feletto!".replace("Sán", "San")));
		Assertions.assertEquals(PlaceChecker.normalize("Verona"), PlaceChecker.normalize("Vérona"));
		Assertions.assertEquals("", PlaceChecker.normalize("..."));
	}

	@Test
	void theDistanceIsOnTheSurfaceOfTheEarth(){
		//Treviso - Venice: about 26 km
		final double distance = PlaceChecker.distanceKm(place("P1", "Treviso", "city", 45.667, 12.243, 0),
			place("P2", "Venice", "city", 45.438, 12.327, 0));

		Assertions.assertEquals(26., distance, 2.);
	}


	@Test
	void twoPlacesWithTheSameNameAndTypeMayBeTheSamePlace(){
		final List<PlaceFinding> found = ofKind(findings(List.of(place("P1", "Treviso", "city"), place("P2", "treviso", "city")),
			List.of()), "place.possible_duplicate");

		Assertions.assertEquals(1, found.size());
		final Diagnostic diagnostic = found.getFirst().diagnostic();
		Assertions.assertEquals(Diagnostic.Severity.WARNING, diagnostic.severity());
		Assertions.assertEquals(List.of("P1", "P2"), diagnostic.recordIds());
		//nothing is merged: the only thing proposed is to compare them
		Assertions.assertEquals(List.of("P1", "P2"), found.getFirst().compare());
		Assertions.assertNull(found.getFirst().draft());
	}

	@Test
	void aMissingTypeMakesItOnlyAnInformation(){
		final List<PlaceFinding> found = ofKind(findings(List.of(place("P1", "Vérona", null), place("P2", "Verona", "city")),
			List.of()), "place.possible_duplicate");

		Assertions.assertEquals(1, found.size());
		Assertions.assertEquals(Diagnostic.Severity.INFORMATION, found.getFirst().diagnostic().severity());
	}

	@Test
	void aCityAndAProvinceWithTheSameNameAreDifferentPlaces(){
		Assertions.assertTrue(ofKind(findings(List.of(place("P1", "Treviso", "city"), place("P2", "Treviso", "province")), List.of()),
			"place.possible_duplicate").isEmpty());
	}

	@Test
	void placesThatAreAlreadyRelatedAreNotDuplicates(){
		final List<PlaceData.Relation> relations = List.of(new PlaceData.Relation("R1", "P2", "P1", "successor_of"));

		Assertions.assertTrue(ofKind(findings(List.of(place("P1", "Treviso", "city"), place("P2", "Treviso", "city")), relations),
			"place.possible_duplicate").isEmpty());
	}

	@Test
	void theSameNameInDifferentPlacesIsNotTheSamePlace(){
		//San Pietro in two different towns
		final List<PlaceData.Place> places = List.of(place("P1", "San Pietro", "village"), place("P2", "San Pietro", "village"),
			place("P3", "Feltre", "town"), place("P4", "Belluno", "town"));
		final List<PlaceData.Relation> relations = List.of(part("R1", "P1", "P3"), part("R2", "P2", "P4"));

		Assertions.assertTrue(ofKind(findings(places, relations), "place.possible_duplicate").isEmpty());
	}

	@Test
	void theSameNameInPlacesWithTheSameNameIsThePlaceTwice(){
		final List<PlaceData.Place> places = List.of(place("P1", "San Pietro", "village"), place("P2", "San Pietro", "village"),
			place("P3", "Feltre", "town"), place("P4", "FELTRE", "town"));
		final List<PlaceData.Relation> relations = List.of(part("R1", "P1", "P3"), part("R2", "P2", "P4"));

		final List<PlaceFinding> found = ofKind(findings(places, relations), "place.possible_duplicate");

		//the two villages, and the two towns they are in
		Assertions.assertEquals(2, found.size());
	}

	@Test
	void coordinatesTellWhetherTheyAreTheSamePlace(){
		final List<PlaceFinding> near = ofKind(findings(List.of(place("P1", "Santa Maria", "village", 45.0, 12.0, 0),
			place("P2", "Santa Maria", "village", 45.01, 12.01, 0)), List.of()), "place.possible_duplicate");
		final List<PlaceFinding> far = ofKind(findings(List.of(place("P1", "Santa Maria", "village", 45.0, 12.0, 0),
			place("P2", "Santa Maria", "village", 41.0, 14.0, 0)), List.of()), "place.possible_duplicate");

		Assertions.assertEquals(1, near.size());
		Assertions.assertTrue(near.getFirst().diagnostic().explanation().contains("km"), near.getFirst().diagnostic().explanation());
		Assertions.assertTrue(far.isEmpty());
	}

	@Test
	void theMostUsedDuplicatesComeFirst(){
		final List<PlaceFinding> found = ofKind(findings(List.of(used("P1", "A", "city", 1), used("P2", "A", "city", 1),
			used("P3", "B", "city", 5), used("P4", "B", "city", 5)), List.of()), "place.possible_duplicate");

		Assertions.assertEquals(2, found.size());
		final double first = found.get(0).diagnostic().rank();
		final double second = found.get(1).diagnostic().rank();
		Assertions.assertEquals(12., first + second);
	}


	@Test
	void theNameOfAPlaceSaysWhereItIs(){
		final List<PlaceData.Place> places = List.of(place("P1", "Treviso, Veneto", "city"), place("P2", "Veneto", "region"));

		final List<PlaceFinding> found = ofKind(findings(places, List.of()), "place.no_parent");

		Assertions.assertEquals(1, found.size());
		Assertions.assertEquals(new PlaceDraft("P1", "P2", "administrative_part_of"), found.getFirst().draft());
		Assertions.assertEquals(List.of("P1", "P2"), found.getFirst().diagnostic().recordIds());
	}

	@Test
	void aContainerThatIsNotSingleIsNotGuessed(){
		//two places named Veneto: which one is it?
		final List<PlaceData.Place> places = List.of(place("P1", "Treviso, Veneto", "city"), place("P2", "Veneto", "region"),
			place("P3", "Veneto", "region"));

		Assertions.assertTrue(ofKind(findings(places, List.of()), "place.no_parent").isEmpty());
	}

	@Test
	void aContainerThatIsSmallerIsNotProposed(){
		final List<PlaceData.Place> places = List.of(place("P1", "Veneto, Treviso", "region"), place("P2", "Treviso", "city"));

		Assertions.assertTrue(ofKind(findings(places, List.of()), "place.no_parent").isEmpty());
	}

	@Test
	void aUsedPlaceWithNoContainerIsReportedWithoutAProposal(){
		final List<PlaceFinding> used = ofKind(findings(List.of(used("P1", "Treviso", "city", 3)), List.of()), "place.no_parent");
		final List<PlaceFinding> unused = ofKind(findings(List.of(place("P1", "Treviso", "city")), List.of()), "place.no_parent");

		Assertions.assertEquals(1, used.size());
		Assertions.assertEquals(new PlaceDraft("P1", null, "administrative_part_of"), used.getFirst().draft());
		Assertions.assertEquals(3., used.getFirst().diagnostic().rank());
		//a place nobody uses, and that says nothing about where it is, is not worth a finding
		Assertions.assertTrue(unused.isEmpty());
	}

	@Test
	void aPlaceThatIsPartOfAnotherOneHasAParent(){
		final List<PlaceData.Place> places = List.of(used("P1", "Treviso", "city", 3), used("P2", "Veneto", "region", 3),
			used("P3", "Italy", "country", 3), used("P4", "Somewhere", null, 3));
		final List<PlaceData.Relation> relations = List.of(part("R1", "P1", "P2"), part("R2", "P2", "P3"));

		//the country is the top, a place with no type cannot be judged
		Assertions.assertTrue(ofKind(findings(places, relations), "place.no_parent").isEmpty());
	}

	@Test
	void manyPlacesAreSummedUp(){
		final List<PlaceData.Place> places = new ArrayList<>();
		for(int i = 0; i < 30; i ++)
			places.add(used("P" + i, "Place " + i, "town", i + 1));

		final List<PlaceFinding> found = ofKind(findings(places, List.of()), "place.no_parent");

		Assertions.assertEquals(PlaceChecker.MAX_PER_KIND, found.size());
		Assertions.assertTrue(found.stream().noneMatch(f -> f.diagnostic().recordIds().contains("P0")));
		final List<PlaceFinding> more = ofKind(findings(places, List.of()), "place.no_parent.more");
		Assertions.assertEquals(1, more.size());
		Assertions.assertTrue(more.getFirst().diagnostic().recordIds().isEmpty());
	}


	@Test
	void aParishBelongsToADiocese(){
		final List<PlaceData.Place> places = List.of(place("P1", "San Nicolò", "parish"), place("P2", "Diocese of Treviso", "diocese"));

		final List<PlaceFinding> found = ofKind(findings(places, List.of()), "place.no_diocese");

		Assertions.assertEquals(1, found.size());
		Assertions.assertEquals(new PlaceDraft("P1", "P2", "ecclesiastical_part_of"), found.getFirst().draft());
	}

	@Test
	void whenThereIsMoreThanOneDioceseNoneIsProposed(){
		final List<PlaceData.Place> places = List.of(place("P1", "San Nicolò", "parish"), place("P2", "Diocese of Treviso", "diocese"),
			place("P3", "Diocese of Padua", "diocese"));

		final List<PlaceFinding> found = ofKind(findings(places, List.of()), "place.no_diocese");

		Assertions.assertEquals(1, found.size());
		Assertions.assertEquals(new PlaceDraft("P1", null, "ecclesiastical_part_of"), found.getFirst().draft());
	}

	@Test
	void aParishAlreadyInADioceseIsFine(){
		final List<PlaceData.Place> places = List.of(place("P1", "San Nicolò", "parish"), place("P2", "Diocese of Treviso", "diocese"),
			place("P3", "Santa Maria", "parish"));
		final List<PlaceData.Relation> relations = List.of(new PlaceData.Relation("R1", "P1", "P2", "ecclesiastical_part_of"),
			//an administrative relationship to a diocese says it too
			part("R2", "P3", "P2"));

		Assertions.assertTrue(ofKind(findings(places, relations), "place.no_diocese").isEmpty());
	}


	@Test
	void aProvinceInACityIsReported(){
		final List<PlaceData.Place> places = List.of(place("P1", "Treviso", "province"), place("P2", "Treviso", "city"));

		final List<PlaceFinding> found = ofKind(findings(places, List.of(part("R1", "P1", "P2"))), "place.inverted_hierarchy");

		Assertions.assertEquals(1, found.size());
		Assertions.assertEquals(Diagnostic.Severity.WARNING, found.getFirst().diagnostic().severity());
		Assertions.assertEquals(List.of("R1", "P1", "P2"), found.getFirst().diagnostic().recordIds());
	}

	@Test
	void aCityInAProvinceIsFine(){
		final List<PlaceData.Place> places = List.of(place("P1", "Treviso", "province"), place("P2", "Treviso", "city"));

		Assertions.assertTrue(ofKind(findings(places, List.of(part("R1", "P2", "P1"))), "place.inverted_hierarchy").isEmpty());
		//a parish or a district are not on the scale: nothing can be said about them
		Assertions.assertTrue(ofKind(findings(List.of(place("P3", "X", "parish"), place("P4", "Y", "village")),
			List.of(part("R2", "P4", "P3"))), "place.inverted_hierarchy").isEmpty());
		//and the ecclesiastical relationships do not follow the scale of the territory
		Assertions.assertTrue(ofKind(findings(places, List.of(new PlaceData.Relation("R3", "P1", "P2", "ecclesiastical_part_of"))),
			"place.inverted_hierarchy").isEmpty());
	}

	@Test
	void aPlaceRelatedToItselfIsAnError(){
		final List<PlaceFinding> found = ofKind(findings(List.of(place("P1", "Treviso", "city")),
			List.of(new PlaceData.Relation("R1", "P1", "P1", "successor_of"))), "place.self_relation");

		Assertions.assertEquals(1, found.size());
		Assertions.assertEquals(Diagnostic.Severity.ERROR, found.getFirst().diagnostic().severity());
	}


	@Test
	void placesThatArePartOfEachOtherAreAnError(){
		final List<PlaceData.Place> places = List.of(place("P1", "A", "city"), place("P2", "B", "city"), place("P3", "C", "city"));
		final List<PlaceData.Relation> relations = List.of(part("R1", "P1", "P2"), part("R2", "P2", "P3"), part("R3", "P3", "P1"));

		final List<PlaceFinding> found = ofKind(findings(places, relations), "place.cycle.part_of");

		Assertions.assertEquals(1, found.size());
		Assertions.assertEquals(Diagnostic.Severity.ERROR, found.getFirst().diagnostic().severity());
		Assertions.assertEquals(List.of("P1", "P2", "P3", "R1", "R2", "R3"), found.getFirst().diagnostic().recordIds());
	}

	@Test
	void aChainIsNotACycle(){
		final List<PlaceData.Place> places = List.of(place("P1", "A", "city"), place("P2", "B", "province"), place("P3", "C", "region"));
		final List<PlaceData.Relation> relations = List.of(part("R1", "P1", "P2"), part("R2", "P2", "P3"));

		Assertions.assertTrue(ofKind(findings(places, relations), "place.cycle.part_of").isEmpty());
	}

	@Test
	void placesThatSucceedEachOtherInACircleAreAWarning(){
		final List<PlaceData.Place> places = List.of(place("P1", "A", "city"), place("P2", "B", "city"));
		final List<PlaceData.Relation> relations = List.of(new PlaceData.Relation("R1", "P1", "P2", "successor_of"),
			new PlaceData.Relation("R2", "P2", "P1", "successor_of"));

		final List<PlaceFinding> found = ofKind(findings(places, relations), "place.cycle.succession");

		Assertions.assertEquals(1, found.size());
		Assertions.assertEquals(Diagnostic.Severity.WARNING, found.getFirst().diagnostic().severity());
	}

	@Test
	void aFindingThatIsIgnoredComesBackWhenTheDataChanges(){
		final String before = ofKind(findings(List.of(place("P1", "Treviso", "city"), place("P2", "Treviso", "city")), List.of()),
			"place.possible_duplicate").getFirst().diagnostic().key();
		final String after = ofKind(findings(List.of(place("P1", "Treviso", "city"), place("P2", "TREVISO", "city")), List.of()),
			"place.possible_duplicate").getFirst().diagnostic().key();

		//the key does not depend on the language, nor on how the name is written... but on the name that matched
		Assertions.assertEquals(before, after);
		Assertions.assertFalse(before.isBlank());
	}

	@Test
	void anEmptyModelHasNothing(){
		Assertions.assertTrue(findings(List.of(), List.of()).isEmpty());
	}

}
