package io.github.mtrevisan.familylegacy.ui.components.projections.repository;

import io.github.mtrevisan.familylegacy.io.model.readers.date.DatePrecision;
import io.github.mtrevisan.familylegacy.io.model.readers.date.NormalizedDate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


class PresentationOrderTest{

	private static NormalizedDate day(final long jdn){
		return NormalizedDate.exact(jdn, DatePrecision.DAY);
	}


	@Test
	void datedItemsComeFirstOldestFirstAndUndatedKeepTheirOrder(){
		final Map<String, NormalizedDate> dates = Map.of("c", day(300), "a", day(100), "b", day(200));
		final List<String> input = List.of("x", "c", "y", "a", "z", "b");

		final List<String> sorted = PresentationOrder.byDate(input, dates::get);

		Assertions.assertEquals(List.of("a", "b", "c", "x", "y", "z"), sorted);
	}

	@Test
	void theInputIsNeverModified(){
		final List<String> input = new ArrayList<>(List.of("b", "a"));
		final Map<String, NormalizedDate> dates = Map.of("a", day(1), "b", day(2));

		PresentationOrder.byDate(input, dates::get);
		PresentationOrder.partners(input, PartnerOrder.UNION_CHRONOLOGY, dates::get, id -> id);
		PresentationOrder.alphabetical(input, id -> id);

		Assertions.assertEquals(List.of("b", "a"), input);
	}

	@Test
	void equalDatesKeepTheirOrderSoTheResultIsDeterministic(){
		final Map<String, NormalizedDate> dates = Map.of("a", day(5), "b", day(5), "c", day(5));

		Assertions.assertEquals(List.of("c", "a", "b"), PresentationOrder.byDate(List.of("c", "a", "b"), dates::get));
	}

	@Test
	void approximateDatesAreComparedByTheirJdn(){
		final NormalizedDate approximate = NormalizedDate.approximated(150, DatePrecision.YEAR, "gregorian", null, "P2Y");
		final Map<String, NormalizedDate> dates = Map.of("exact", day(200), "approx", approximate);

		Assertions.assertEquals(List.of("approx", "exact"), PresentationOrder.byDate(List.of("exact", "approx"), dates::get));
	}

	@Test
	void alphabeticalOrderUsesTheLocaleRulesAndKeepsUnnamedLastInTheirOrder(){
		final Map<String, String> names = Map.of("1", "Émile", "2", "Zoe", "3", "adam", "4", "Bea");

		final List<String> sorted = PresentationOrder.alphabetical(List.of("x", "2", "1", "y", "4", "3"), names::get);

		Assertions.assertEquals(List.of("3", "4", "1", "2", "x", "y"), sorted);
	}

	@Test
	void theStrategyDecidesWhatIsDone(){
		final Map<String, NormalizedDate> unions = Map.of("a", day(2), "b", day(1));
		final Map<String, String> names = Map.of("a", "Anna", "b", "Zeno");
		final List<String> input = List.of("b", "a");

		Assertions.assertEquals(List.of("b", "a"), PresentationOrder.partners(input, PartnerOrder.UNION_CHRONOLOGY, unions::get, names::get));
		Assertions.assertEquals(List.of("a", "b"), PresentationOrder.partners(input, PartnerOrder.ALPHABETICAL, unions::get, names::get));
		//existing order: nothing is re-sorted
		Assertions.assertEquals(List.of("b", "a"), PresentationOrder.partners(List.of("b", "a"), PartnerOrder.EXISTING, unions::get, names::get));
		Assertions.assertEquals(List.of("a", "b"), PresentationOrder.partners(List.of("a", "b"), PartnerOrder.EXISTING, unions::get, names::get));
	}

	@Test
	void unionDateIsTheMarriageThenTheEarliestRelatedEventThenTheRelationshipStart(){
		final var banns = new UnionChronology.SharedEvent("marriage_bann", day(10));
		final var marriage = new UnionChronology.SharedEvent("marriage", day(20));
		final var laterMarriage = new UnionChronology.SharedEvent("marriage", day(30));
		final var divorce = new UnionChronology.SharedEvent("divorce", day(40));
		final var unrelated = new UnionChronology.SharedEvent("residence", day(1));

		Assertions.assertEquals(day(20), UnionChronology.unionDate(List.of(divorce, banns, laterMarriage, marriage, unrelated), day(5)));
		Assertions.assertEquals(day(10), UnionChronology.unionDate(List.of(divorce, banns, unrelated), day(5)));
		Assertions.assertEquals(day(5), UnionChronology.unionDate(List.of(unrelated), day(5)));
		Assertions.assertNull(UnionChronology.unionDate(List.of(unrelated, new UnionChronology.SharedEvent("marriage", null)), null));
	}

}
