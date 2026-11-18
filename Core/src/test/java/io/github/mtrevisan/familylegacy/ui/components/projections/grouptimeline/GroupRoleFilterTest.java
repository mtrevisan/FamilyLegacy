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
import io.github.mtrevisan.familylegacy.groups.IndividualCareerBuilder;
import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.temporal.TemporalEntry;
import io.github.mtrevisan.familylegacy.temporal.TemporalProjectionModel;
import io.github.mtrevisan.familylegacy.temporal.TemporalRow;
import io.github.mtrevisan.familylegacy.temporal.TemporalTrackType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/** The legend of the roles filters them: a role that is not checked has no bars, and what is left with no bar goes away with them. */
class GroupRoleFilterTest{

	private static final String HEADER = """
		header {
			protocol {
				name Family LEgacy Format
				version 0.2.0
			}
			date 2026-01-01T00:00:00Z
		}
		""";

	//Anna is the president of the Club from 1900 to 1910 and then a member; Bruno is a member with no role and no date; Youth is part of
	//the Club from 1905, with no role
	private static final String FLEF = HEADER
		+ person("I1", "Anna") + person("I2", "Bruno")
		+ group("G1", "Club") + group("G2", "Youth")
		+ membership("R1", "individual I1", "G1", RelationshipReader.ENUM_TYPE_GROUP_MEMBER, "president", "1900-01-01", "1910-01-01")
		+ membership("R2", "individual I1", "G1", RelationshipReader.ENUM_TYPE_GROUP_MEMBER, "member", "1910-01-01", null)
		+ membership("R3", "individual I2", "G1", RelationshipReader.ENUM_TYPE_GROUP_MEMBER, null, null, null)
		+ membership("R4", "group G2", "G1", RelationshipReader.ENUM_TYPE_PART_OF, null, "1905-01-01", null);


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

	private static TemporalRow row(final TemporalProjectionModel model, final String id){
		return model.rows().stream()
			.filter(row -> row.entity().id().equals(id))
			.findFirst()
			.orElse(null);
	}

	private static List<String> roles(final TemporalRow row){
		return row.findTrack(TemporalTrackType.MEMBERSHIP).entries().stream()
			.map(TemporalEntry::role)
			.sorted()
			.toList();
	}

	private static JCheckBox box(final GroupRoleLegend legend, final String text){
		for(final Component component : legend.getComponents())
			if(component instanceof JCheckBox box && box.getText().equals(text))
				return box;
		throw new IllegalArgumentException("No box " + text);
	}

	private static JButton showAllButton(final GroupRoleLegend legend){
		for(final Component component : legend.getComponents())
			if(component instanceof JButton button)
				return button;
		throw new IllegalArgumentException("No button");
	}


	@Test
	void hidingARoleTakesAwayItsBarsOnly(){
		final TemporalProjectionModel filtered = GroupRoleLegend.filter(timeline(), Set.of("president"));

		Assertions.assertEquals(List.of("member"), roles(row(filtered, "I1")), "Anna keeps the bar of the member");
		Assertions.assertNotNull(row(filtered, "I2"));
		Assertions.assertNotNull(row(filtered, "G2"));
	}

	@Test
	void aRowWithNoBarLeftGoesAway(){
		final TemporalProjectionModel whole = timeline();

		Assertions.assertEquals(List.of("G2", "I2"), rowIds(GroupRoleLegend.filter(whole, Set.of("president", "member"))),
			"Anna has no bar left");
		Assertions.assertEquals(List.of("I1"), rowIds(GroupRoleLegend.filter(whole, Set.of(GroupRoleLegend.NO_ROLE_KEY))),
			"the memberships without a role are the ones of Bruno and of the subgroup");
		Assertions.assertTrue(GroupRoleLegend.filter(whole, Set.of("president", "member", GroupRoleLegend.NO_ROLE_KEY)).rows().isEmpty());
	}

	@Test
	void nothingHiddenIsTheSameModel(){
		final TemporalProjectionModel whole = timeline();

		Assertions.assertSame(whole, GroupRoleLegend.filter(whole, Set.of()));
		Assertions.assertSame(whole, GroupRoleLegend.filter(whole, Set.of("a role that is not there")),
			"a role that has no bar changes nothing");
	}

	@Test
	void theAxisIsComputedAgainOnWhatIsLeft(){
		final TemporalProjectionModel whole = timeline();

		final TemporalProjectionModel withoutPresident = GroupRoleLegend.filter(whole, Set.of("president"));

		Assertions.assertTrue(withoutPresident.domainStart().jdn() > whole.domainStart().jdn(),
			"the bar from 1900 is gone: the axis starts later");
	}

	@Test
	void theRowOfTheIndividualStaysWithItsOtherBarsAndTheCareerOfSomeoneHiddenIsEmpty(){
		final TemporalProjectionModel career = IndividualCareerBuilder.build(model(), "I1");

		Assertions.assertTrue(GroupRoleLegend.filter(career, Set.of("president", "member")).isEmpty());
		Assertions.assertEquals(List.of("G1"), rowIds(GroupRoleLegend.filter(career, Set.of("president"))));
	}

	@Test
	void aKeyIsTheSameWhateverTheCaseAndTheBlanks(){
		Assertions.assertEquals("president", GroupRoleLegend.key("  President "));
		Assertions.assertEquals(GroupRoleLegend.NO_ROLE_KEY, GroupRoleLegend.key(null));
		Assertions.assertEquals(GroupRoleLegend.NO_ROLE_KEY, GroupRoleLegend.key("   "));
		Assertions.assertEquals("president", GroupRoleLegend.roles(timeline()).get(1).key());
	}

	@Test
	void aBoxHidesTheRoleAndShowAllBringsItBack(){
		final GroupRoleLegend legend = new GroupRoleLegend();
		final int[] told = new int[1];
		legend.withListener(() -> told[0] ++);
		legend.update(timeline());
		Assertions.assertFalse(showAllButton(legend).isVisible(), "nothing is hidden");

		box(legend, "president").doClick();

		Assertions.assertEquals(Set.of("president"), legend.hiddenRoles());
		Assertions.assertFalse(box(legend, "president").isSelected());
		Assertions.assertTrue(box(legend, "member").isSelected());
		Assertions.assertEquals(1, told[0]);
		Assertions.assertTrue(showAllButton(legend).isVisible());
		Assertions.assertEquals(List.of("member"), roles(row(legend.filter(timeline()), "I1")));

		showAllButton(legend).doClick();

		Assertions.assertTrue(legend.hiddenRoles().isEmpty());
		Assertions.assertTrue(box(legend, "president").isSelected());
		Assertions.assertFalse(showAllButton(legend).isVisible());
		Assertions.assertEquals(2, told[0]);
		legend.showAll();
		Assertions.assertEquals(2, told[0], "nothing was hidden: nothing to tell");
	}

	@Test
	void theBoxesAreKeptWhileTheRolesAreTheSame(){
		final GroupRoleLegend legend = new GroupRoleLegend();
		legend.update(timeline());
		final JCheckBox box = box(legend, "president");
		box.doClick();

		legend.update(timeline());

		Assertions.assertSame(box, box(legend, "president"), "the click is not undone by showing the view again");
		Assertions.assertFalse(box.isSelected());
	}

	@Test
	void resetForgetsWhatWasHidden(){
		final GroupRoleLegend legend = new GroupRoleLegend();
		legend.update(timeline());
		box(legend, "president").doClick();

		legend.reset();
		legend.update(timeline());

		Assertions.assertTrue(legend.hiddenRoles().isEmpty());
		Assertions.assertTrue(box(legend, "president").isSelected(), "the boxes are built again, checked");
	}

	@Test
	void thePanelShowsWhatTheLegendLeavesAndStartsAgainWithAnotherGroup() throws InvocationTargetException, InterruptedException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The views need a display");
		final FLEFModel model = model();

		final List<Integer> bars = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			final GroupTimelinePanel panel = new GroupTimelinePanel(model);
			panel.showGroup("G1");
			bars.add(row(panel.shownModel(), "I1").findTrack(TemporalTrackType.MEMBERSHIP).entries().size());

			panel.legend().setHidden("president", true);
			bars.add(row(panel.shownModel(), "I1").findTrack(TemporalTrackType.MEMBERSHIP).entries().size());

			panel.legend().showAll();
			bars.add(row(panel.shownModel(), "I1").findTrack(TemporalTrackType.MEMBERSHIP).entries().size());

			panel.legend().setHidden("president", true);
			panel.showGroup("G1");
			bars.add(panel.legend().hiddenRoles().size());
			bars.add(row(panel.shownModel(), "I1").findTrack(TemporalTrackType.MEMBERSHIP).entries().size());
		});

		Assertions.assertEquals(List.of(2, 1, 2, 0, 2), bars, "both bars; the president hidden; shown again; a new view forgets; both bars");
	}

	@Test
	void theCareerPanelFiltersToo() throws InvocationTargetException, InterruptedException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The views need a display");
		final FLEFModel model = model();

		final List<Integer> rows = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			final IndividualCareerPanel panel = new IndividualCareerPanel(model);
			panel.showIndividual("I1");
			rows.add(panel.shownModel().rows().size());

			panel.legend().setHidden("president", true);
			panel.legend().setHidden("member", true);
			rows.add(panel.shownModel().rows().size());
		});

		Assertions.assertEquals(List.of(1, 0), rows);
	}

}
