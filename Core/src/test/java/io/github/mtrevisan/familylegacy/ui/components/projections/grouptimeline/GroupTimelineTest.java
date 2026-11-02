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

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DatePrecision;
import io.github.mtrevisan.familylegacy.io.model.readers.date.NormalizedDate;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import io.github.mtrevisan.familylegacy.ui.components.projections.temporal.TemporalEntry;
import io.github.mtrevisan.familylegacy.ui.components.projections.temporal.TemporalProjectionModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.temporal.TemporalRoleColors;
import io.github.mtrevisan.familylegacy.ui.components.projections.temporal.TemporalRow;
import io.github.mtrevisan.familylegacy.ui.components.projections.temporal.TemporalSpanRenderer;
import io.github.mtrevisan.familylegacy.ui.components.projections.temporal.TemporalTrackType;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.ui.tools.groups.GroupViewDialog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.List;


/** The timeline of a group: a row for its subgroups and members, a bar for each membership, colored by role. */
class GroupTimelineTest{

	private static final String HEADER = """
		header {
			protocol {
				name Family LEgacy Format
				version 0.2.0
			}
			date 2026-01-01T00:00:00Z
		}
		""";

	//Club (G1) has a subgroup (Youth, G2, from 1905), and the members Anna (president 1900-1910, then member from 1910),
	//Bruno (no date) and, through the subgroup only, Carla (not a member of the Club); Dino is in no group
	private static final String FLEF = HEADER
		+ person("I1", "Anna") + person("I2", "Bruno") + person("I3", "Carla") + person("I4", "Dino")
		+ group("G1", "Club") + group("G2", "Youth")
		+ membership("R1", "individual I1", "G1", "group_member", "president", "1900-01-01", "1910-01-01")
		+ membership("R2", "individual I1", "G1", "group_member", "member", "1910-01-01", null)
		+ membership("R3", "individual I2", "G1", "group_member", null, null, null)
		+ membership("R4", "group G2", "G1", "part_of", null, "1905-01-01", null)
		+ membership("R5", "individual I3", "G2", "group_member", "scout", "1920-01-01", null);


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

	private static TemporalProjectionModel build(final String groupId){
		final FLEFModel model = new FLEFParser().parse(FLEF);
		return GroupTimelineBuilder.build(model, groupId);
	}

	private static TemporalRow row(final TemporalProjectionModel model, final String id){
		return model.rows().stream()
			.filter(row -> row.entity().id().equals(id))
			.findFirst()
			.orElse(null);
	}

	private static List<TemporalEntry> memberships(final TemporalRow row){
		return row.findTrack(TemporalTrackType.MEMBERSHIP).entries();
	}


	@Test
	void theRowsAreTheSubgroupsThenTheMembersTheEarliestFirst(){
		final TemporalProjectionModel model = build("G1");

		Assertions.assertEquals(List.of("G2", "I1", "I2"), model.rows().stream().map(row -> row.entity().id()).toList());
		Assertions.assertNull(row(model, "I3"), "Carla is a member of the subgroup only");
	}

	@Test
	void aMemberWithTwoRolesHasBothBarsOnItsRow(){
		final List<TemporalEntry> entries = memberships(row(build("G1"), "I1"));

		Assertions.assertEquals(2, entries.size());
		Assertions.assertEquals(List.of("member", "president"), entries.stream().map(TemporalEntry::role).sorted().toList());
		Assertions.assertTrue(entries.stream().allMatch(entry -> entry.hasType("group_member")));
	}

	@Test
	void theBarOfARelationshipWithoutDatesIsKeptAndSaysSo(){
		final TemporalProjectionModel model = build("G1");
		final TemporalEntry entry = memberships(row(model, "I2")).getFirst();

		Assertions.assertTrue(entry.label().contains(GroupTimelineBuilder.DATES_UNKNOWN), entry.label());
		Assertions.assertEquals(Long.MIN_VALUE, entry.span().start().jdn());
		Assertions.assertEquals(Long.MAX_VALUE, entry.span().end().jdn());
	}

	@Test
	void theUndatedBarDoesNotStretchTheAxis(){
		final TemporalProjectionModel model = build("G1");

		Assertions.assertTrue(model.hasDomain());
		Assertions.assertTrue(model.domainStart().jdn() > Long.MIN_VALUE);
		Assertions.assertTrue(model.domainEnd().jdn() < Long.MAX_VALUE);
		Assertions.assertTrue(model.domainStart().jdn() < model.domainEnd().jdn());
	}

	@Test
	void aSubgroupIsAGroupRowWithItsOwnMembership(){
		final TemporalRow subgroup = row(build("G1"), "G2");

		Assertions.assertEquals("part_of", memberships(subgroup).getFirst().type());
		Assertions.assertEquals(1, memberships(subgroup).size());
	}

	@Test
	void whatIsNotAGroupHasNoTimeline(){
		Assertions.assertTrue(build("I1").isEmpty());
		Assertions.assertTrue(build("nothing").isEmpty());
		Assertions.assertTrue(build(null).isEmpty());
	}

	@Test
	void theSubgroupHasItsOwnTimelineWithTheGroupItIsPartOf(){
		final TemporalProjectionModel model = build("G2");

		Assertions.assertEquals(List.of("G1", "I3"), model.rows().stream().map(row -> row.entity().id()).toList(),
			"the Club it is part of, then its member");
		Assertions.assertEquals("part_of", memberships(row(model, "G1")).getFirst().type());
	}

	@Test
	void aRoleHasTheSameColorWhateverItsCase(){
		Assertions.assertEquals(TemporalRoleColors.of("President"), TemporalRoleColors.of(" president "));
		Assertions.assertEquals(TemporalRoleColors.NO_ROLE, TemporalRoleColors.of(null));
		Assertions.assertEquals(TemporalRoleColors.NO_ROLE, TemporalRoleColors.of("  "));
	}

	@Test
	void thePanelShowsTheGroupAndRefreshesIt() throws InvocationTargetException, InterruptedException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The views need a display");
		final FLEFModel model = new FLEFParser().parse(FLEF);

		final GroupTimelinePanel[] panel = new GroupTimelinePanel[1];
		SwingUtilities.invokeAndWait(() -> {
			panel[0] = new GroupTimelinePanel(model);
			panel[0].showGroup("G1");
			panel[0].refresh();
		});

		Assertions.assertEquals("G1", panel[0].getGroupId());
	}

	@Test
	void theCareerHasARowForEachGroupOfTheIndividual(){
		final FLEFModel model = new FLEFParser().parse(FLEF);

		final TemporalProjectionModel anna = IndividualCareerBuilder.build(model, "I1");
		Assertions.assertEquals(List.of("G1"), anna.rows().stream().map(row -> row.entity().id()).toList(),
			"Anna has no dated event: only the group");
		Assertions.assertEquals(List.of("member", "president"),
			memberships(row(anna, "G1")).stream().map(TemporalEntry::role).sorted().toList(), "the two periods, with their roles");

		//Carla is in the subgroup, not in the Club
		Assertions.assertEquals(List.of("G2"), IndividualCareerBuilder.build(model, "I3").rows().stream()
			.map(row -> row.entity().id()).toList());
	}

	@Test
	void theCareerKeepsAMembershipWithoutDatesAndDoesNotStretchTheAxis(){
		final FLEFModel model = new FLEFParser().parse(FLEF);

		final TemporalProjectionModel bruno = IndividualCareerBuilder.build(model, "I2");
		Assertions.assertTrue(memberships(row(bruno, "G1")).getFirst().label().contains(GroupTimelineBuilder.DATES_UNKNOWN));
		Assertions.assertTrue(bruno.hasDomain(), "no date at all: the axis shows the last century, to show the bar");
		Assertions.assertTrue(bruno.domainStart().jdn() > Long.MIN_VALUE && bruno.domainEnd().jdn() < Long.MAX_VALUE);
		Assertions.assertTrue(bruno.domainStart().jdn() < bruno.domainEnd().jdn());

		final TemporalProjectionModel anna = IndividualCareerBuilder.build(model, "I1");
		Assertions.assertTrue(anna.domainStart().jdn() > Long.MIN_VALUE);
		Assertions.assertTrue(anna.domainEnd().jdn() < Long.MAX_VALUE);
	}

	@Test
	void theCareerOfWhatIsNotAnIndividualOrHasNoGroupIsEmpty(){
		final FLEFModel model = new FLEFParser().parse(FLEF);

		Assertions.assertTrue(IndividualCareerBuilder.build(model, "I4").isEmpty(), "Dino is in no group");
		Assertions.assertTrue(IndividualCareerBuilder.build(model, "G1").isEmpty());
		Assertions.assertTrue(IndividualCareerBuilder.build(model, "nothing").isEmpty());
		Assertions.assertTrue(IndividualCareerBuilder.build(model, null).isEmpty());
	}

	@Test
	void theCareerPanelShowsTheIndividualAndRefreshesIt() throws InvocationTargetException, InterruptedException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The views need a display");
		final FLEFModel model = new FLEFParser().parse(FLEF);

		final IndividualCareerPanel[] panel = new IndividualCareerPanel[1];
		SwingUtilities.invokeAndWait(() -> {
			panel[0] = new IndividualCareerPanel(model);
			panel[0].showIndividual("I1");
			panel[0].refresh();
		});

		Assertions.assertEquals("I1", panel[0].getIndividualId());
	}

	@Test
	void theDialogsCloseWithEsc() throws InvocationTargetException, InterruptedException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = new FLEFParser().parse(FLEF);

		final List<JDialog> dialogs = new java.util.ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			final ToolContext context = new ToolContext(model, null, new JPanel(), null);
			dialogs.add(GroupViewDialog.forGroup(context, "G1"));
			dialogs.add(GroupViewDialog.forIndividual(context, "I1"));

			for(final JDialog dialog : dialogs){
				final Object binding = dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(GUIHelper.ESCAPE_STROKE);
				Assertions.assertNotNull(binding, "ESC is not bound");
				final Action action = dialog.getRootPane().getActionMap().get(binding);
				Assertions.assertNotNull(action);
				Assertions.assertTrue(dialog.isDisplayable());
				action.actionPerformed(new ActionEvent(dialog, ActionEvent.ACTION_PERFORMED, "escape"));
				Assertions.assertFalse(dialog.isDisplayable(), "ESC closes the dialog");
			}
		});
	}

	@Test
	void theLegendListsTheRolesOnTheScreenAndThenTheMembershipsWithoutRole(){
		final FLEFModel model = new FLEFParser().parse(FLEF);

		final List<GroupRoleLegend.Role> roles = GroupRoleLegend.roles(GroupTimelineBuilder.build(model, "G1"));

		Assertions.assertEquals(java.util.Arrays.asList("member", "president", null), roles.stream().map(GroupRoleLegend.Role::label).toList(),
			"Bruno and the subgroup have no role");
		Assertions.assertEquals(TemporalRoleColors.of("president"), roles.get(1).color(), "the color of the bars");
		Assertions.assertEquals(TemporalRoleColors.NO_ROLE, roles.get(2).color());
		//the career of Anna has only her two roles
		Assertions.assertEquals(List.of("member", "president"), GroupRoleLegend.roles(IndividualCareerBuilder.build(model, "I1")).stream()
			.map(GroupRoleLegend.Role::label).toList());
	}

	@Test
	void aRoleWrittenInMoreWaysIsOneInTheLegend(){
		final FLEFModel model = new FLEFParser().parse(HEADER + person("I1", "Anna") + person("I2", "Bruno") + group("G1", "Club")
			+ membership("R1", "individual I1", "G1", "group_member", "President", null, null)
			+ membership("R2", "individual I2", "G1", "group_member", "president", null, null));

		final List<GroupRoleLegend.Role> roles = GroupRoleLegend.roles(GroupTimelineBuilder.build(model, "G1"));

		Assertions.assertEquals(1, roles.size());
		Assertions.assertTrue("President".equalsIgnoreCase(roles.getFirst().label()), roles.getFirst().label());
	}

	@Test
	void theLegendHasNothingWhenThereAreNoMemberships(){
		Assertions.assertTrue(GroupRoleLegend.roles(null).isEmpty());
		Assertions.assertTrue(GroupRoleLegend.roles(TemporalProjectionModel.empty()).isEmpty());
	}

	@Test
	void theLegendIsShownOnlyWithRoles(){
		final FLEFModel model = new FLEFParser().parse(FLEF);
		final GroupRoleLegend legend = new GroupRoleLegend();
		Assertions.assertFalse(legend.isVisible());

		legend.update(GroupTimelineBuilder.build(model, "G1"));
		Assertions.assertTrue(legend.isVisible());
		Assertions.assertEquals(1 + 3 + 1, legend.getComponentCount(), "the title, the three roles and Show all");

		legend.update(GroupTimelineBuilder.build(model, "nothing"));
		Assertions.assertFalse(legend.isVisible());
		Assertions.assertEquals(0, legend.getComponentCount());
	}

	/** The text of a record with a line added before the closing brace. */
	private static String withStatus(final String record, final String status){
		return record.substring(0, record.lastIndexOf('}')) + "\tstatus " + status + "\n}\n";
	}

	@Test
	void theStatusOfTheRelationshipIsTheStatusOfTheBar(){
		final FLEFModel model = new FLEFParser().parse(HEADER + person("I1", "Anna") + person("I2", "Bruno") + person("I3", "Carla")
			+ group("G1", "Club")
			+ withStatus(membership("R1", "individual I1", "G1", "group_member", "guest", "1900-01-01", "1901-01-01"), "ended")
			+ withStatus(membership("R2", "individual I2", "G1", "group_member", "host", "1900-01-01", null), "active")
			+ membership("R3", "individual I3", "G1", "group_member", "owner", "1900-01-01", null));
		final TemporalProjectionModel timeline = GroupTimelineBuilder.build(model, "G1");

		final TemporalEntry ended = memberships(row(timeline, "I1")).getFirst();
		Assertions.assertEquals(TemporalSpan.STATUS_ENDED, ended.span().status());
		Assertions.assertTrue(ended.label().contains(GroupTimelineBuilder.STATUS_ENDED_LABEL), ended.label());
		Assertions.assertEquals(TemporalSpan.STATUS_ACTIVE, memberships(row(timeline, "I2")).getFirst().span().status());
		Assertions.assertFalse(memberships(row(timeline, "I2")).getFirst().label().contains(GroupTimelineBuilder.STATUS_ENDED_LABEL));
		Assertions.assertEquals(TemporalSpan.STATUS_UNKNOWN, memberships(row(timeline, "I3")).getFirst().span().status(),
			"a relationship that says nothing");
	}

	@Test
	void aMembershipThatEndedWithoutDatesKeepsBothNotes(){
		final FLEFModel model = new FLEFParser().parse(HEADER + person("I1", "Anna") + group("G1", "Club")
			+ withStatus(membership("R1", "individual I1", "G1", "group_member", "guest", null, null), "ended"));

		final TemporalEntry entry = memberships(row(GroupTimelineBuilder.build(model, "G1"), "I1")).getFirst();

		Assertions.assertEquals(TemporalSpan.STATUS_ENDED, entry.span().status());
		Assertions.assertTrue(entry.label().contains(GroupTimelineBuilder.STATUS_ENDED_LABEL) && entry.label().contains(GroupTimelineBuilder.DATES_UNKNOWN),
			entry.label());
	}

	@Test
	void theCareerHasTheStatusToo(){
		final FLEFModel model = new FLEFParser().parse(HEADER + person("I1", "Anna") + group("G1", "Club")
			+ withStatus(membership("R1", "individual I1", "G1", "group_member", "guest", "1900-01-01", "1901-01-01"), "ended"));

		Assertions.assertEquals(TemporalSpan.STATUS_ENDED,
			memberships(row(IndividualCareerBuilder.build(model, "I1"), "G1")).getFirst().span().status());
	}

	@Test
	void aBarThatEndedIsFainterThanOneThatIsActive(){
		final TemporalSpan active = TemporalSpan.spanning(NormalizedDate.exact(2_415_021L, DatePrecision.DAY),
			NormalizedDate.exact(2_416_000L, DatePrecision.DAY), TemporalSpan.STATUS_ACTIVE);
		final TemporalSpan ended = TemporalSpan.spanning(NormalizedDate.exact(2_415_021L, DatePrecision.DAY),
			NormalizedDate.exact(2_416_000L, DatePrecision.DAY), TemporalSpan.STATUS_ENDED);
		final TemporalSpan unknown = TemporalSpan.spanning(NormalizedDate.exact(2_415_021L, DatePrecision.DAY),
			NormalizedDate.exact(2_416_000L, DatePrecision.DAY), TemporalSpan.STATUS_UNKNOWN);
		final Color base = new Color(70, 120, 190);

		final int activePixel = centerPixel(active, base);
		final int endedPixel = centerPixel(ended, base);

		Assertions.assertEquals(base.getRGB(), activePixel, "an active bar is solid");
		Assertions.assertEquals(activePixel, centerPixel(unknown, base), "an unknown status is drawn as it always was");
		Assertions.assertNotEquals(activePixel, endedPixel);
		final Color faded = new Color(endedPixel);
		Assertions.assertTrue(faded.getRed() > base.getRed() && faded.getGreen() > base.getGreen() && faded.getBlue() > base.getBlue(),
			"fainter: nearer to the white of the background");
	}

	/** The color at the center of a bar drawn on white. */
	private static int centerPixel(final TemporalSpan span, final Color base){
		final BufferedImage image = new BufferedImage(120, 30, BufferedImage.TYPE_INT_RGB);
		final Graphics2D g = image.createGraphics();
		try{
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, 120, 30);
			TemporalSpanRenderer.drawSpan(g, new Rectangle(10, 5, 100, 20), span, base, false);
		}
		finally{
			g.dispose();
		}
		return image.getRGB(60, 15);
	}

}
