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
package io.github.mtrevisan.familylegacy.groups;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.temporal.TemporalContextBand;
import io.github.mtrevisan.familylegacy.temporal.TemporalProjectionModel;
import io.github.mtrevisan.familylegacy.temporal.TemporalRow;
import io.github.mtrevisan.familylegacy.temporal.TemporalSvgWriter;
import io.github.mtrevisan.familylegacy.temporal.TemporalTrack;
import io.github.mtrevisan.familylegacy.temporal.TemporalTrackType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;


/** Several individuals on a shared axis, with the historic context of their places, and its drawing as SVG. */
class LifespanComparisonTest{

	private static final String HEADER = """
		header {
			protocol {
				name Family LEgacy Format
				version 0.2.0
			}
			date 2026-01-01T00:00:00Z
		}
		""";

	//I1 (1850-1920) and I2 (1880-1950) lived in Treviso, in Veneto; I3 (1900-1960) lived in Lyon, in France; I4 has no date
	//a war in Veneto (1866), a pandemic with no place (1918), a war in France (1914)
	private static final String FLEF = HEADER
		+ place("P1", "Treviso", "city") + place("P2", "Veneto", "region") + place("P3", "Lyon", "city") + place("P4", "France", "country")
		+ relation("PR1", "P1", "P2") + relation("PR2", "P3", "P4")
		+ person("I1", "Mario") + person("I2", "Anna") + person("I3", "Paul") + person("I4", "Nobody")
		+ event("E1", "birth", "1850-05-01", "P1") + participation("EP1", "E1", "I1")
		+ event("E2", "death", "1920-03-04", "P1") + participation("EP2", "E2", "I1")
		+ event("E3", "marriage", "1875-06-10", "P1") + participation("EP3", "E3", "I1")
		+ event("E4", "birth", "1880-01-01", "P1") + participation("EP4", "E4", "I2")
		+ event("E5", "death", "1950-01-01", "P1") + participation("EP5", "E5", "I2")
		+ event("E6", "birth", "1900-01-01", "P3") + participation("EP6", "E6", "I3")
		+ event("E7", "death", "1960-01-01", "P3") + participation("EP7", "E7", "I3")
		+ historic("HE1", "Third War of Independence", "1866-06-20", "1866-10-12", "P2")
		+ historic("HE2", "Spanish flu", "1918-03-01", "1920-12-01", null)
		+ historic("HE3", "Great War in France", "1914-07-28", "1918-11-11", "P4");


	private static String place(final String id, final String name, final String type){
		return "place " + id + " {\n\tname.value " + name + "\n\ttype " + type + "\n\taudit.creation.date 2020-01-01T00:00:00Z\n}\n";
	}

	private static String relation(final String id, final String subject, final String target){
		return "place_relationship " + id + " {\n\tsubject.place " + subject + "\n\ttarget.place " + target
			+ "\n\ttype administrative_part_of\n\taudit.creation.date 2020-01-01T00:00:00Z\n}\n";
	}

	private static String person(final String id, final String name){
		return "individual " + id + " {\n\tname {\n\t\tpart {\n\t\t\ttype given\n\t\t\tvalue " + name + "\n\t\t}\n\t}\n}\n";
	}

	private static String event(final String id, final String type, final String date, final String place){
		return "event " + id + " {\n\ttype " + type + "\n\tdate.value.point.value.full_date " + date + "\n\tplace.place " + place + "\n}\n";
	}

	private static String participation(final String id, final String eventId, final String individual){
		return "event_participation " + id + " {\n\tevent " + eventId + "\n\tparticipant.individual " + individual + "\n\trole principal\n}\n";
	}

	private static String historic(final String id, final String title, final String from, final String to, final String place){
		return "historic_event " + id + " {\n\ttype war\n\ttitle " + title + "\n\tdate {\n\t\tvalue {\n\t\t\tspanning {\n\t\t\t\tfrom.value.full_date "
			+ from + "\n\t\t\t\tto.value.full_date " + to + "\n\t\t\t}\n\t\t}\n\t}\n" + (place != null? "\tplace.place " + place + "\n": "") + "}\n";
	}


	private static FLEFModel model(){
		return new FLEFParser().parse(FLEF);
	}

	private static TemporalProjectionModel build(final List<String> ids, final LifespanComparisonBuilder.Options options){
		return LifespanComparisonBuilder.build(model(), ids, options);
	}

	private static List<String> bands(final TemporalProjectionModel projection){
		return projection.bands().stream()
			.map(TemporalContextBand::label)
			.toList();
	}


	@Test
	void theRowsAreTheIndividualsTheEarliestBornFirst(){
		final TemporalProjectionModel projection = build(List.of("I3", "I2", "I1"), LifespanComparisonBuilder.Options.defaults());

		Assertions.assertEquals(List.of("I1", "I2", "I3"), projection.rows().stream().map(row -> row.entity().id()).toList());
		Assertions.assertEquals(List.of(0, 1, 2), projection.rows().stream().map(TemporalRow::sortOrder).toList());
		Assertions.assertTrue(projection.hasDomain());
	}

	@Test
	void everyRowHasTheBarOfItsLifeAboveItsEvents(){
		final TemporalRow row = build(List.of("I1"), LifespanComparisonBuilder.Options.defaults())
			.rows()
			.getFirst();

		final TemporalTrack life = row.findTrack(TemporalTrackType.LIFESPAN);
		Assertions.assertNotNull(life);
		Assertions.assertEquals(1, life.size());
		Assertions.assertTrue(life.entries().getFirst().span().minJdn() < life.entries().getFirst().span().maxJdn());
		Assertions.assertTrue(life.sortOrder() < row.findTrack(TemporalTrackType.EVENT).sortOrder(), "the life is the first track");
		Assertions.assertEquals(3, row.findTrack(TemporalTrackType.EVENT).size());
	}

	@Test
	void onlyTheMainEventsCanBeAskedFor(){
		final LifespanComparisonBuilder.Options options = new LifespanComparisonBuilder.Options(true, false, true, true);

		Assertions.assertEquals(3, build(List.of("I1"), options).rows().getFirst().findTrack(TemporalTrackType.EVENT).size(),
			"birth, marriage and death are main events");
	}

	@Test
	void theHistoricContextIsTheOneOfThePlacesOfTheIndividuals(){
		final List<String> trevisoOnly = bands(build(List.of("I1", "I2"), LifespanComparisonBuilder.Options.defaults()));

		Assertions.assertTrue(trevisoOnly.contains("Third War of Independence"), "in the region of Treviso: " + trevisoOnly);
		Assertions.assertTrue(trevisoOnly.contains("Spanish flu"), "with no place: it is of the world");
		Assertions.assertFalse(trevisoOnly.contains("Great War in France"), "in a country that has nothing to do with them");

		final List<String> withLyon = bands(build(List.of("I1", "I3"), LifespanComparisonBuilder.Options.defaults()));
		Assertions.assertTrue(withLyon.contains("Great War in France"), "Lyon is in France: " + withLyon);
	}

	@Test
	void theBandsOfAStripAreThoseOfThePlacesOfItsIndividuals(){
		final TemporalProjectionModel full = build(List.of("I1"), LifespanComparisonBuilder.Options.defaults());
		final List<String> bands = LifespanComparisonBuilder.contextBandsOf(model(), List.of("I1"), full.domainStart(), full.domainEnd())
			.stream()
			.map(TemporalContextBand::label)
			.toList();

		Assertions.assertEquals(bands, bands(full));
		Assertions.assertTrue(bands.contains("Third War of Independence") && !bands.contains("Great War in France"), bands.toString());
	}

	@Test
	void thePlaceAwareContextCanBeTurnedOffOrTheContextRemoved(){
		final List<String> all = bands(build(List.of("I1"), new LifespanComparisonBuilder.Options(false, false, true, false)));
		Assertions.assertTrue(all.contains("Great War in France"));

		Assertions.assertTrue(build(List.of("I1"), new LifespanComparisonBuilder.Options(false, false, false, true)).bands().isEmpty());
	}

	@Test
	void anIndividualThatIsNotThereOrHasNoDateIsLeftOut(){
		Assertions.assertTrue(build(List.of("I4", "I99"), LifespanComparisonBuilder.Options.defaults()).isEmpty());
		Assertions.assertEquals(1, build(List.of("I4", "I1", "I1"), LifespanComparisonBuilder.Options.defaults()).rows().size(),
			"no date, a duplicate");
	}


	@Test
	void theSvgIsWellFormedAndHasARowForEveryIndividual() throws Exception{
		final String svg = TemporalSvgWriter.write(build(List.of("I1", "I2", "I3"), LifespanComparisonBuilder.Options.defaults()));

		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		final Document document = factory.newDocumentBuilder()
			.parse(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)));

		Assertions.assertEquals("svg", document.getDocumentElement().getNodeName());
		final NodeList titles = document.getElementsByTagName("title");
		Assertions.assertTrue(titles.getLength() > 6, "a tooltip for each bar and band: " + titles.getLength());
		Assertions.assertTrue(svg.contains(">I1<") && svg.contains(">I2<") && svg.contains(">I3<"), "the individuals are in the row headers");
		Assertions.assertTrue(svg.contains("Third War of Independence"), "the historic context is drawn");
		Assertions.assertTrue(svg.contains(">1900<") || svg.contains(">1850<"), "the axis has years");
	}

	@Test
	void anEmptyModelGivesAnEmptyDocumentAndANarrowOneIsWidened(){
		final String empty = TemporalSvgWriter.write(TemporalProjectionModel.empty());
		Assertions.assertTrue(empty.contains("<svg") && empty.contains("</svg>"));
		Assertions.assertFalse(empty.contains("<rect x="), "nothing is drawn");

		final String svg = TemporalSvgWriter.write(build(List.of("I1"), LifespanComparisonBuilder.Options.defaults()), 100);
		Assertions.assertTrue(svg.contains("width=\"480\""), "a narrow document is widened to a usable size");
	}

}
