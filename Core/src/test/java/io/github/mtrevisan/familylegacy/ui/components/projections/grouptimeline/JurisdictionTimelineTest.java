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
package io.github.mtrevisan.familylegacy.ui.components.projections.grouptimeline;

import io.github.mtrevisan.familylegacy.groups.GroupTimelineBuilder;
import io.github.mtrevisan.familylegacy.groups.GroupYearFilter;
import io.github.mtrevisan.familylegacy.groups.JurisdictionTimelineBuilder;
import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.temporal.TemporalEntityType;
import io.github.mtrevisan.familylegacy.temporal.TemporalEntry;
import io.github.mtrevisan.familylegacy.temporal.TemporalProjectionModel;
import io.github.mtrevisan.familylegacy.temporal.TemporalRow;
import io.github.mtrevisan.familylegacy.temporal.TemporalTrackType;
import io.github.mtrevisan.familylegacy.ui.components.projections.temporal.TemporalRoleColors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.List;


/** The timeline of the jurisdictions of a place: a row for each related place, a bar for each place relationship, colored by type. */
class JurisdictionTimelineTest{

	private static final String HEADER = """
		header {
			protocol {
				name Family LEgacy Format
				version 0.2.0
			}
			date 2026-01-01T00:00:00Z
		}
		""";

	//the parish (P1) was in the diocese of Treviso (P2) until 1818, then in the one of Ceneda (P3); it is in the municipality (P4) with no
	//date; a chapel (P5) is part of the parish from 1900
	private static final String FLEF = HEADER
		+ place("P1", "San Martino") + place("P2", "Diocesi di Treviso") + place("P3", "Diocesi di Ceneda") + place("P4", "Comune")
		+ place("P5", "Cappella") + place("P6", "Elsewhere")
		+ relation("PR1", "P1", "P2", PlaceRelationshipReader.ENUM_TYPE_ECCLESIASTICAL_PART_OF, "1700-01-01", "1818-01-01")
		+ relation("PR2", "P1", "P3", PlaceRelationshipReader.ENUM_TYPE_ECCLESIASTICAL_PART_OF, "1818-01-01", null)
		+ relation("PR3", "P1", "P4", PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF, null, null)
		+ relation("PR4", "P5", "P1", PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF, "1900-01-01", null)
		+ relation("PR5", "P6", "P4", PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF, "1900-01-01", null);


	private static String place(final String id, final String name){
		return "place " + id + " {\n\tname.value " + name + "\n\ttype city\n\taudit.creation.date 2020-01-01T00:00:00Z\n}\n";
	}

	private static String relation(final String id, final String subject, final String target, final String type, final String from,
			final String to){
		final StringBuilder sb = new StringBuilder("place_relationship " + id + " {\n\tsubject.place " + subject + "\n\ttarget.place "
			+ target + "\n\ttype " + type + "\n");
		if(from != null)
			sb.append("\tvalid_from.value.point.value.full_date ").append(from).append('\n');
		if(to != null)
			sb.append("\tvalid_to.value.point.value.full_date ").append(to).append('\n');
		return sb.append("\taudit.creation.date 2020-01-01T00:00:00Z\n}\n").toString();
	}

	private static TemporalProjectionModel build(final String placeId){
		return JurisdictionTimelineBuilder.build(new FLEFParser().parse(FLEF), placeId);
	}

	private static TemporalRow row(final TemporalProjectionModel model, final String id){
		return model.rows().stream()
			.filter(row -> row.entity().id().equals(id))
			.findFirst()
			.orElse(null);
	}

	private static List<TemporalEntry> bars(final TemporalRow row){
		return row.findTrack(TemporalTrackType.MEMBERSHIP).entries();
	}

	private static List<String> ids(final TemporalProjectionModel model){
		return model.rows().stream().map(row -> row.entity().id()).toList();
	}


	@Test
	void theRowsAreThePlacesItIsPartOfThenThePlacesThatArePartOfIt(){
		final TemporalProjectionModel model = build("P1");

		Assertions.assertEquals(List.of("P2", "P3", "P4", "P5"), ids(model),
			"the earliest first, the undated last; then the incoming ones");
		Assertions.assertNull(row(model, "P6"), "the other place is related to the municipality, not to the parish");
		Assertions.assertTrue(model.rows().stream().allMatch(row -> row.entity().type() == TemporalEntityType.PLACE));
	}

	@Test
	void theBarIsColoredAndLabeledByTheJurisdiction(){
		final TemporalProjectionModel model = build("P1");
		final TemporalEntry diocese = bars(row(model, "P2")).getFirst();
		final TemporalEntry municipality = bars(row(model, "P4")).getFirst();

		Assertions.assertEquals(PlaceRelationshipReader.ENUM_TYPE_ECCLESIASTICAL_PART_OF, diocese.role());
		Assertions.assertEquals(PlaceRelationshipReader.ENUM_TYPE_ECCLESIASTICAL_PART_OF, diocese.type());
		Assertions.assertEquals(TemporalRoleColors.of(PlaceRelationshipReader.ENUM_TYPE_ECCLESIASTICAL_PART_OF), TemporalRoleColors.of(diocese.role()));
		Assertions.assertEquals(PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF, municipality.role());
		Assertions.assertTrue(diocese.label().startsWith(PlaceRelationshipReader.ENUM_TYPE_ECCLESIASTICAL_PART_OF), diocese.label());
	}

	@Test
	void aChangeOfJurisdictionIsTwoBarsOneAfterTheOther(){
		final TemporalProjectionModel model = build("P1");
		final TemporalEntry treviso = bars(row(model, "P2")).getFirst();
		final TemporalEntry ceneda = bars(row(model, "P3")).getFirst();

		Assertions.assertTrue(treviso.span().maxJdn() <= ceneda.span().minJdn(), "Treviso ends when Ceneda begins");
		final long today = LocalDate.now().toEpochDay() + 2_440_588L;
		Assertions.assertEquals(today, ceneda.span().maxJdn(), "the last one has no end: it ends today");
		Assertions.assertTrue(ceneda.label().contains(GroupTimelineBuilder.ONGOING), ceneda.label());
		Assertions.assertFalse(treviso.label().contains(GroupTimelineBuilder.ONGOING), treviso.label());
	}

	@Test
	void noBarGoesBeyondToday(){
		final long today = LocalDate.now().toEpochDay() + 2_440_588L;
		final TemporalProjectionModel model = build("P1");

		Assertions.assertEquals(today, model.domainEnd().jdn(), "Ceneda has no end: the axis ends today");
		for(final TemporalRow row : model.rows())
			for(final TemporalEntry entry : bars(row))
				Assertions.assertTrue(entry.span().maxJdn() <= today, "a bar goes on beyond today: " + entry.label());
		final TemporalEntry undated = bars(row(model, "P4")).getFirst();
		Assertions.assertEquals(today, undated.span().maxJdn(), "a relationship with no date ends today too");
		Assertions.assertEquals(Long.MIN_VALUE, undated.span().minJdn(), "and begins at the edge of the window");
		Assertions.assertTrue(undated.label().contains(GroupTimelineBuilder.DATES_UNKNOWN));
		Assertions.assertFalse(undated.label().contains(GroupTimelineBuilder.ONGOING), "it is not known when it began");
		Assertions.assertEquals(today, GroupYearFilter.filter(model, 1750).domainEnd().jdn(), "also in a year that is filtered");

		//everything has ended: the axis stops at the last end
		final FLEFModel closed = new FLEFParser().parse(HEADER + place("P1", "A") + place("P2", "B")
			+ relation("PR1", "P1", "P2", PlaceRelationshipReader.ENUM_TYPE_ECCLESIASTICAL_PART_OF, "1700-01-01", "1818-01-01"));
		Assertions.assertTrue(JurisdictionTimelineBuilder.build(closed, "P1").domainEnd().jdn() < today);
	}

	@Test
	void aRelationshipWithoutDatesIsKeptAndSaysSo(){
		final TemporalProjectionModel model = build("P1");
		final TemporalEntry municipality = bars(row(model, "P4")).getFirst();

		Assertions.assertTrue(municipality.label().contains(GroupTimelineBuilder.DATES_UNKNOWN), municipality.label());
		Assertions.assertTrue(model.hasDomain());
		Assertions.assertTrue(model.domainStart().jdn() > Long.MIN_VALUE && model.domainEnd().jdn() < Long.MAX_VALUE,
			"the undated bar does not stretch the axis");
		Assertions.assertTrue(model.domainStart().jdn() < model.domainEnd().jdn());
	}

	@Test
	void aRelationshipThatPointsToThePlaceSaysIncoming(){
		final TemporalProjectionModel model = build("P1");

		Assertions.assertTrue(bars(row(model, "P5")).getFirst().label().contains(JurisdictionTimelineBuilder.INCOMING));
		Assertions.assertFalse(bars(row(model, "P2")).getFirst().label().contains(JurisdictionTimelineBuilder.INCOMING));
	}

	@Test
	void theChapelHasTheParishAsItsJurisdiction(){
		final TemporalProjectionModel model = build("P5");

		Assertions.assertEquals(List.of("P1"), ids(model));
		Assertions.assertEquals(PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF, bars(row(model, "P1")).getFirst().type());
	}

	@Test
	void theYearFilterKeepsWhatWasInEffectAndTheUndated(){
		final TemporalProjectionModel model = build("P1");

		Assertions.assertEquals(List.of("P2", "P4"), ids(GroupYearFilter.filter(model, 1750)), "Treviso and the undated municipality");
		Assertions.assertEquals(List.of("P3", "P4", "P5"), ids(GroupYearFilter.filter(model, 1950)));
	}

	@Test
	void whatIsNotAPlaceHasNoTimeline(){
		Assertions.assertTrue(build("PR1").isEmpty());
		Assertions.assertTrue(build("nothing").isEmpty());
		Assertions.assertTrue(build(null).isEmpty());
	}

	@Test
	void aPlaceWithNoRelationshipHasNoRows(){
		final FLEFModel model = new FLEFParser().parse(HEADER + place("P1", "Alone"));

		Assertions.assertTrue(JurisdictionTimelineBuilder.build(model, "P1").rows().isEmpty());
	}

	@Test
	void thePanelShowsThePlaceAndRefreshesIt() throws InvocationTargetException, InterruptedException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The views need a display");
		final FLEFModel model = new FLEFParser().parse(FLEF);

		final JurisdictionTimelinePanel[] panel = new JurisdictionTimelinePanel[1];
		SwingUtilities.invokeAndWait(() -> {
			panel[0] = new JurisdictionTimelinePanel(model);
			panel[0].showPlace("P1");
			panel[0].refresh();
		});

		Assertions.assertEquals("P1", panel[0].getPlaceId());
		Assertions.assertEquals(List.of("P2", "P3", "P4", "P5"), ids(panel[0].shownModel()));
	}

}
