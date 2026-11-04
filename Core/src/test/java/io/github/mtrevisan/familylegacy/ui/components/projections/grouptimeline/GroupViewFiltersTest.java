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
import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.temporal.TemporalProjectionModel;
import io.github.mtrevisan.familylegacy.temporal.TemporalTrackType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JSpinner;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


/** The filter by year of the group views, the axis of a view with open or undated bars, and the reuse of the model while a filter changes. */
class GroupViewFiltersTest{

	private static final String HEADER = """
		header {
			protocol {
				name Family LEgacy Format
				version 0.2.0
			}
			date 2026-01-01T00:00:00Z
		}
		""";

	private static final long UNIX_EPOCH_JDN = 2_440_588L;

	//Anna is the president of the Club from 1900 to 1910 and then a member with no end; Bruno is a member with no date; Youth is part of
	//the Club from 1905
	private static final String FLEF = HEADER
		+ person("I1", "Anna") + person("I2", "Bruno")
		+ group("G1", "Club") + group("G2", "Youth")
		+ membership("R1", "individual I1", "G1", "group_member", "president", "1900-01-01", "1910-01-01")
		+ membership("R2", "individual I1", "G1", "group_member", "member", "1910-01-01", null)
		+ membership("R3", "individual I2", "G1", "group_member", null, null, null)
		+ membership("R4", "group G2", "G1", "part_of", null, "1905-01-01", null);


	private static String person(final String id, final String name){
		return "individual " + id + " {\n\tname {\n\t\tpart {\n\t\t\ttype given\n\t\t\tvalue " + name + "\n\t\t}\n\t}\n}\n";
	}

	private static String group(final String id, final String name){
		return "group " + id + " {\n\tname {\n\t\ttype legal\n\t\tvalue " + name + "\n\t}\n}\n";
	}

	private static String membership(final String id, final String subject, final String target, final String type, final String role,
			final String from, final String to){
		final StringBuilder sb = new StringBuilder("relationship " + id + " {\n\tsubject." + subject + "\n\ttarget.group " + target
			+ "\n\ttype " + type + "\n");
		if(role != null)
			sb.append("\trole ").append(role).append('\n');
		if(from != null)
			sb.append("\tvalid_from.value.point.value.full_date ").append(from).append('\n');
		if(to != null)
			sb.append("\tvalid_to.value.point.value.full_date ").append(to).append('\n');
		return sb.append("}\n").toString();
	}

	private static FLEFModel model(){
		return new FLEFParser().parse(FLEF);
	}

	private static TemporalProjectionModel timeline(){
		return GroupTimelineBuilder.build(model(), "G1");
	}

	private static List<String> rowIds(final TemporalProjectionModel model){
		return model.rows().stream()
			.map(row -> row.entity().id())
			.toList();
	}

	private static long today(){
		return LocalDate.now().toEpochDay() + UNIX_EPOCH_JDN;
	}

	private static JSpinner spinner(final GroupYearControl control){
		for(final Component component : control.getComponents())
			if(component instanceof JSpinner spinner)
				return spinner;
		throw new IllegalArgumentException("No spinner");
	}


	@Test
	void aYearKeepsTheBarsInEffectAndTheOnesWithoutDates(){
		final TemporalProjectionModel whole = timeline();

		//1905: Anna is the president, Youth has just joined, Bruno has no date
		Assertions.assertEquals(List.of("G2", "I1", "I2"), rowIds(GroupYearFilter.filter(whole, 1905)));
		//1950: the presidency is over, Anna is a member with no end
		final TemporalProjectionModel in1950 = GroupYearFilter.filter(whole, 1950);
		Assertions.assertEquals(List.of("member"), in1950.rows().stream()
			.filter(row -> row.entity().id().equals("I1"))
			.flatMap(row -> row.tracks().stream())
			.flatMap(track -> track.entries().stream())
			.map(entry -> entry.role())
			.toList());
		//1895: nobody was in the Club but the one with no date
		Assertions.assertEquals(List.of("I2"), rowIds(GroupYearFilter.filter(whole, 1895)));
	}

	@Test
	void noYearIsTheSameModel(){
		final TemporalProjectionModel whole = timeline();

		Assertions.assertSame(whole, GroupYearFilter.filter(whole, null));
		Assertions.assertSame(whole, GroupYearFilter.filter(whole, 1910), "in 1910 every bar is in effect: nothing is taken away");
	}

	@Test
	void theYearIsAYearOfTheGregorianCalendar(){
		final long[] range = GroupYearFilter.yearRange(1970);

		Assertions.assertEquals(2_440_588L, range[0], "1 January 1970");
		Assertions.assertEquals(2_440_588L + 364L, range[1], "31 December 1970: a year of 365 days");
		final long[] leap = GroupYearFilter.yearRange(1972);
		Assertions.assertEquals(365L, leap[1] - leap[0], "1972 is a leap year: 366 days");
	}

	@Test
	void theGroupItIsPartOfHasARowOnTheTimelineOfASubgroup(){
		final TemporalProjectionModel youth = GroupTimelineBuilder.build(model(), "G2");

		Assertions.assertEquals(List.of("G1"), rowIds(youth), "the Club, that Youth is part of (Youth has no member)");
		Assertions.assertEquals("part_of", youth.rows().getFirst().findTrack(TemporalTrackType.MEMBERSHIP).entries().getFirst().type());
		Assertions.assertFalse(rowIds(timeline()).contains("G1"), "a group is not part of itself");
	}

	@Test
	void theAxisHasADomainEvenWithOpenOrUndatedBars(){
		//open: from 1900 with no end, to today
		final FLEFModel open = new FLEFParser().parse(HEADER + person("I1", "Anna") + group("G1", "Club")
			+ membership("R1", "individual I1", "G1", "group_member", "member", "1900-01-01", null));
		final TemporalProjectionModel opened = GroupTimelineBuilder.build(open, "G1");
		Assertions.assertTrue(opened.hasDomain());
		Assertions.assertEquals(2_415_021L, opened.domainStart().jdn(), "1 January 1900");
		Assertions.assertTrue(opened.domainEnd().jdn() >= today(), "an open membership lasts to today");

		//no start: a century before the end
		final FLEFModel noStart = new FLEFParser().parse(HEADER + person("I1", "Anna") + group("G1", "Club")
			+ membership("R1", "individual I1", "G1", "group_member", "member", null, "1950-01-01"));
		final TemporalProjectionModel ended = GroupTimelineBuilder.build(noStart, "G1");
		Assertions.assertTrue(ended.domainStart().jdn() < ended.domainEnd().jdn());
		Assertions.assertTrue(ended.domainEnd().jdn() - ended.domainStart().jdn() >= 36_000L, "about a century");

		//no date at all: the last century
		final FLEFModel none = new FLEFParser().parse(HEADER + person("I1", "Anna") + group("G1", "Club")
			+ membership("R1", "individual I1", "G1", "group_member", "member", null, null));
		final TemporalProjectionModel undated = GroupTimelineBuilder.build(none, "G1");
		Assertions.assertTrue(undated.hasDomain());
		Assertions.assertTrue(undated.domainEnd().jdn() >= today() - 1L && undated.domainEnd().jdn() <= today() + 1L);
	}

	@Test
	void theYearControlTellsTheListenerOnceAndShowsAYearWithoutSeparators(){
		final GroupYearControl control = new GroupYearControl();
		final int[] told = new int[1];
		control.withListener(() -> told[0] ++);
		Assertions.assertNull(control.getYear(), "any year at first");

		control.setYear(1950);

		Assertions.assertEquals(1950, control.getYear());
		Assertions.assertEquals(1, told[0], "setting the year is one change");
		Assertions.assertEquals("1950", ((JSpinner.NumberEditor)spinner(control).getEditor()).getTextField().getText(), "not 1,950");

		control.setYear(null);

		Assertions.assertNull(control.getYear());
		Assertions.assertEquals(2, told[0]);
	}

	@Test
	void theModelIsBuiltAgainOnlyWhenTheDataMayHaveChanged(){
		final GroupViewFilters filters = new GroupViewFilters();
		final int[] built = new int[1];
		final List<TemporalProjectionModel> shown = new ArrayList<>();
		final java.util.function.Supplier<TemporalProjectionModel> builder = () -> {
			built[0] ++;
			return timeline();
		};
		filters.onChange(() -> shown.add(filters.apply(builder)));

		shown.add(filters.apply(builder));
		Assertions.assertEquals(1, built[0]);

		filters.yearControl().setYear(1895);
		filters.legend().setHidden("president", true);
		Assertions.assertEquals(1, built[0], "a filter changed: the model is the one built");
		Assertions.assertEquals(3, shown.size());
		Assertions.assertEquals(List.of("I2"), rowIds(shown.getLast()), "the year first, then the roles");

		filters.apply(builder);
		Assertions.assertEquals(2, built[0], "the data may have changed: built again");
	}

	@Test
	void thePanelsFilterByYear() throws InvocationTargetException, InterruptedException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The views need a display");
		final FLEFModel model = model();

		final List<List<String>> rows = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			final GroupTimelinePanel timeline = new GroupTimelinePanel(model);
			timeline.showGroup("G1");
			rows.add(rowIds(timeline.shownModel()));
			timeline.yearControl().setYear(1895);
			rows.add(rowIds(timeline.shownModel()));
			timeline.yearControl().setYear(null);
			rows.add(rowIds(timeline.shownModel()));

			final IndividualCareerPanel career = new IndividualCareerPanel(model);
			career.showIndividual("I1");
			rows.add(rowIds(career.shownModel()));
			career.yearControl().setYear(1895);
			rows.add(rowIds(career.shownModel()));
		});

		Assertions.assertEquals(List.of("G2", "I1", "I2"), rows.get(0));
		Assertions.assertEquals(List.of("I2"), rows.get(1));
		Assertions.assertEquals(rows.get(0), rows.get(2));
		Assertions.assertEquals(List.of("G1"), rows.get(3));
		Assertions.assertTrue(rows.get(4).isEmpty(), "Anna was not in the Club in 1895");
	}

}
