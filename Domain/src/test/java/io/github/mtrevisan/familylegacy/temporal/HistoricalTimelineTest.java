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
package io.github.mtrevisan.familylegacy.temporal;

import io.github.mtrevisan.familylegacy.groups.LifespanComparisonBuilder;
import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;


/**
 * The timeline of a person against history: which historic events and cultural norms are listed, by time and by how far from the places of
 * the person they happened, and the ages.
 */
class HistoricalTimelineTest{

	private static final String HEADER = """
		header {
			protocol {
				name Family LEgacy Format
				version 0.2.0
			}
			date 2026-01-01T00:00:00Z
		}
		""";

	//Treviso (city) is part of Veneto (region), part of Italy (country); Lyon (city) is somewhere else
	//I1 was born in Treviso in 1850, married in 1880 in Veneto, died in Treviso in 1920
	private static final String FLEF = HEADER
		+ place("P1", "Treviso", "city") + place("P2", "Veneto", "region") + place("P3", "Italy", "country") + place("P4", "Lyon", "city")
		+ place("P5", "Europe", "continent")
		+ relation("PR1", "P1", "P2") + relation("PR2", "P2", "P3")
		+ "individual I1 {\n\tname {\n\t\tpart {\n\t\t\ttype given\n\t\t\tvalue Mario\n\t\t}\n\t}\n}\n"
		+ event("E1", "birth", "1850-05-01", "P1") + participation("EP1", "E1")
		+ event("E2", "marriage", "1880-06-10", "P2") + participation("EP2", "E2")
		+ event("E3", "death", "1920-03-04", "P1") + participation("EP3", "E3")
		+ historic("HE1", "Third War of Independence", "war", "1866-06-20", "P2")
		+ historic("HE2", "Spanish flu", "pandemic", "1918-03-01", null)
		+ historic("HE3", "Franco-Prussian War", "war", "1870-07-19", "P4")
		+ historic("HE4", "Napoleonic Wars", "war", "1700-01-01", "P2")
		+ historic("HE5", "Cholera", "epidemic", "1840-01-01", "P4")
		+ historic("HE6", "Unification of Italy", "political_change", "1861-03-17", "P3")
		+ historic("HE7", "Continental drift", "natural_disaster", "1900-01-01", "P5")
		+ impact("CI1", "HE5", "I1", "explains")
		+ "cultural_norm CN1 {\n\ttitle Marriage banns\n\ttype marriage_practice\n\tplace.place P1\n"
		+ "\tvalid_from.value.point.value.full_date 1860-01-01\n\tvalid_to.value.point.value.full_date 1900-01-01\n}\n";


	private static String place(final String id, final String name, final String type){
		return "place " + id + " {\n\tname.value " + name + "\n\ttype " + type + "\n\taudit.creation.date 2020-01-01T00:00:00Z\n}\n";
	}

	private static String relation(final String id, final String subject, final String target){
		return "place_relationship " + id + " {\n\tsubject.place " + subject + "\n\ttarget.place " + target
			+ "\n\ttype administrative_part_of\n\taudit.creation.date 2020-01-01T00:00:00Z\n}\n";
	}

	private static String event(final String id, final String type, final String date, final String place){
		return "event " + id + " {\n\ttype " + type + "\n\tdate.value.point.value.full_date " + date + "\n\tplace.place " + place + "\n}\n";
	}

	private static String participation(final String id, final String eventId){
		return "event_participation " + id + " {\n\tevent " + eventId + "\n\tparticipant.individual I1\n\trole principal\n}\n";
	}

	private static String historic(final String id, final String title, final String type, final String date, final String place){
		return "historic_event " + id + " {\n\ttype " + type + "\n\ttitle " + title + "\n\tdate.value.point.value.full_date " + date + "\n"
			+ (place != null? "\tplace.place " + place + "\n": "") + "}\n";
	}

	private static String impact(final String id, final String historicEvent, final String individual, final String type){
		return "context_impact " + id + " {\n\tcontext.historic_event " + historicEvent + "\n\ttarget.individual " + individual
			+ "\n\timpact_type " + type + "\n}\n";
	}


	private static FLEFModel model(){
		return new FLEFParser().parse(FLEF);
	}

	private static HistoricalTimeline.Result build(final HistoricalTimeline.Options options){
		return HistoricalTimeline.build(model(), "I1", options);
	}

	private static List<String> labels(final HistoricalTimeline.Result result, final HistoricalTimeline.Kind kind){
		return result.items().stream()
			.filter(item -> item.kind() == kind)
			.map(HistoricalTimeline.Item::label)
			.toList();
	}

	private static HistoricalTimeline.Item item(final HistoricalTimeline.Result result, final String label){
		return result.items().stream()
			.filter(item -> item.label().equals(label))
			.findFirst()
			.orElseThrow(() -> new AssertionError("no item " + label + " in " + result.items()));
	}


	@Test
	void theLifetimeIsFromTheBirthToTheDeath(){
		final Lifetime lifetime = build(HistoricalTimeline.Options.defaults())
			.lifetime();

		Assertions.assertTrue(lifetime.isKnown());
		Assertions.assertFalse(lifetime.startEstimated());
		Assertions.assertFalse(lifetime.endEstimated());
		Assertions.assertTrue(lifetime.start() < lifetime.end());
	}

	@Test
	void theHistoricEventsOfTheTimeAndPlaceAreListedAndTheOthersAreNot(){
		final List<String> historic = labels(build(HistoricalTimeline.Options.defaults()), HistoricalTimeline.Kind.HISTORIC_EVENT);

		Assertions.assertTrue(historic.contains("Third War of Independence"), "in the region of the person, during its life: " + historic);
		Assertions.assertTrue(historic.contains("Unification of Italy"), "in the country, during its life");
		Assertions.assertTrue(historic.contains("Spanish flu"), "with no place: it is a world event");
		Assertions.assertFalse(historic.contains("Franco-Prussian War"), "in a place with no relation to the places of the person");
		Assertions.assertFalse(historic.contains("Napoleonic Wars"), "before the birth");
	}

	@Test
	void theScopeSaysHowFarThePlaceIs(){
		final HistoricalTimeline.Result result = build(HistoricalTimeline.Options.all());

		Assertions.assertEquals(HistoricalScope.REGIONAL, item(result, "Third War of Independence").scope());
		Assertions.assertEquals(HistoricalScope.NATIONAL, item(result, "Unification of Italy").scope());
		Assertions.assertEquals(HistoricalScope.WORLD, item(result, "Spanish flu").scope());
		Assertions.assertEquals(HistoricalScope.WORLD, item(result, "Continental drift").scope(), "a continent is the world");
		Assertions.assertEquals(HistoricalScope.ELSEWHERE, item(result, "Franco-Prussian War").scope());
		Assertions.assertEquals(HistoricalScope.LOCAL, item(result, "Marriage banns").scope(), "a norm of the city of the person");
		Assertions.assertTrue(labels(result, HistoricalTimeline.Kind.HISTORIC_EVENT).contains("Franco-Prussian War"),
			"with every scope asked for, the events elsewhere are listed");
	}

	@Test
	void aScopeThatIsNotAskedForIsLeftOut(){
		final HistoricalTimeline.Result result = build(new HistoricalTimeline.Options(EnumSet.of(HistoricalScope.LOCAL), true, true));

		final List<String> historic = labels(result, HistoricalTimeline.Kind.HISTORIC_EVENT);
		Assertions.assertFalse(historic.contains("Third War of Independence"));
		Assertions.assertFalse(historic.contains("Spanish flu"));
		Assertions.assertTrue(labels(result, HistoricalTimeline.Kind.CULTURAL_NORM).contains("Marriage banns"));
	}

	@Test
	void whatAContextImpactLinksIsAlwaysListedEvenOutOfTheLifeAndTheScope(){
		final HistoricalTimeline.Result result = build(new HistoricalTimeline.Options(Set.of(), false, false));

		final HistoricalTimeline.Item cholera = item(result, "Cholera");
		Assertions.assertTrue(cholera.isLinked(), "linked to the individual");
		Assertions.assertEquals("explains", cholera.impact());
		Assertions.assertEquals(HistoricalScope.ELSEWHERE, cholera.scope(), "it happened in Lyon, and it is before the birth");
		Assertions.assertEquals(1, labels(result, HistoricalTimeline.Kind.HISTORIC_EVENT).size(), "nothing else, with no scope asked for");
	}

	@Test
	void theLifeAndTheHistoryAreInOneChronologicalList(){
		final HistoricalTimeline.Result result = build(HistoricalTimeline.Options.defaults());

		long previous = Long.MIN_VALUE;
		for(final HistoricalTimeline.Item item : result.items()){
			Assertions.assertTrue(item.span().minJdn() >= previous, "not in order: " + item.label());
			previous = item.span().minJdn();
		}
		final List<HistoricalTimeline.Kind> kinds = result.items().stream()
			.map(HistoricalTimeline.Item::kind)
			.distinct()
			.toList();
		Assertions.assertTrue(kinds.containsAll(List.of(HistoricalTimeline.Kind.LIFE_EVENT, HistoricalTimeline.Kind.HISTORIC_EVENT,
			HistoricalTimeline.Kind.CULTURAL_NORM)), kinds.toString());
	}

	@Test
	void theAgeIsTheOneOfTheIndividualAtTheStartOfTheItem(){
		final HistoricalTimeline.Result result = build(HistoricalTimeline.Options.defaults());

		Assertions.assertEquals(0, item(result, "birth").age());
		Assertions.assertEquals(30, item(result, "marriage").age());
		Assertions.assertEquals(16, item(result, "Third War of Independence").age());
		Assertions.assertEquals(9, item(result, "Marriage banns").age(), "the norm begins in 1860");
	}

	@Test
	void anIndividualThatIsNotThereHasNothing(){
		Assertions.assertTrue(HistoricalTimeline.build(model(), "I99", HistoricalTimeline.Options.defaults()).items().isEmpty());
		Assertions.assertTrue(HistoricalTimeline.build(model(), null, HistoricalTimeline.Options.defaults()).items().isEmpty());
		Assertions.assertFalse(Lifetime.of(List.of()).isKnown());
	}

	@Test
	void theKeyEventTypesAreAmongTheEventTypes(){
		Assertions.assertTrue(List.of(EventReader.TYPES).containsAll(
			LifespanComparisonBuilder.KEY_EVENT_TYPES));
	}

}
