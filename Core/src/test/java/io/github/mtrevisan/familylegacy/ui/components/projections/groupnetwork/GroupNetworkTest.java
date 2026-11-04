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
package io.github.mtrevisan.familylegacy.ui.components.projections.groupnetwork;

import io.github.mtrevisan.familylegacy.groups.GroupNetwork;
import io.github.mtrevisan.familylegacy.groups.GroupNetworkBuilder;
import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.groups.GroupViewDialog;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/** The network of the groups around a group: the neighborhood, the filter by year, the members of each group and the layout. */
public class GroupNetworkTest{

	public static final String HEADER = """
		header {
			protocol {
				name Family LEgacy Format
				version 0.2.0
			}
			date 2026-01-01T00:00:00Z
		}
		""";

	//the Club (G2) is part of the Federation (G1) from 1900 to 1950; Youth (G3) is part of the Club from 1920; the Rivals (G4) are
	//associated with the Club from 1930; Remote (G5) is associated with the Rivals, with no date
	//Anna (I1) is a member of the Club from 1900 to 1910, Bruno (I2) with no date, Carla (I3) of Youth from 1925
	public static final String FLEF = HEADER
		+ person("I1", "Anna") + person("I2", "Bruno") + person("I3", "Carla")
		+ group("G1", "Federation") + group("G2", "Club") + group("G3", "Youth") + group("G4", "Rivals") + group("G5", "Remote")
		+ relationship("R1", "group G2", "G1", "part_of", "1900-01-01", "1950-01-01")
		+ relationship("R2", "group G3", "G2", "part_of", "1920-01-01", null)
		+ relationship("R3", "group G4", "G2", "associate", "1930-01-01", null)
		+ relationship("R4", "group G5", "G4", "associate", null, null)
		+ relationship("R5", "individual I1", "G2", "group_member", "1900-01-01", "1910-01-01")
		+ relationship("R6", "individual I2", "G2", "group_member", null, null)
		+ relationship("R7", "individual I3", "G3", "group_member", "1925-01-01", null);


	public static String person(final String id, final String name){
		return "individual " + id + " {\n\tname {\n\t\tpart {\n\t\t\ttype given\n\t\t\tvalue " + name + "\n\t\t}\n\t}\n}\n";
	}

	public static String group(final String id, final String name){
		return "group " + id + " {\n\tname {\n\t\ttype legal\n\t\tvalue " + name + "\n\t}\n}\n";
	}

	public static String relationship(final String id, final String subject, final String target, final String type, final String from,
			final String to){
		final StringBuilder sb = new StringBuilder("relationship " + id + " {\n\tsubject." + subject + "\n\ttarget.group " + target
			+ "\n\ttype " + type + "\n");
		if(from != null)
			sb.append("\tvalid_from.value.point.value.full_date ").append(from).append('\n');
		if(to != null)
			sb.append("\tvalid_to.value.point.value.full_date ").append(to).append('\n');
		return sb.append("}\n").toString();
	}

	public static FLEFModel model(){
		return new FLEFParser().parse(FLEF);
	}

	public static Set<String> ids(final GroupNetwork network){
		return network.nodes().stream()
			.map(GroupNetwork.Node::id)
			.collect(Collectors.toSet());
	}

	public static GroupNetwork.Node node(final GroupNetwork network, final String id){
		return network.nodes().stream()
			.filter(node -> node.id().equals(id))
			.findFirst()
			.orElseThrow();
	}


	@Test
	void theNetworkIsTheNeighborhoodOfTheCenter(){
		final GroupNetwork network = GroupNetworkBuilder.build(model(), "G2", 1, null);

		Assertions.assertEquals(Set.of("G1", "G2", "G3", "G4"), ids(network), "the groups one relationship away, not Remote");
		Assertions.assertEquals("G2", network.nodes().getFirst().id(), "the center first");
		Assertions.assertTrue(network.nodes().getFirst().center());
		Assertions.assertEquals(3, network.edges().size());
	}

	@Test
	void theDepthSaysHowFarTheNetworkGoes(){
		final FLEFModel model = model();

		Assertions.assertEquals(Set.of("G2"), ids(GroupNetworkBuilder.build(model, "G2", 0, null)));
		Assertions.assertEquals(Set.of("G1", "G2", "G3", "G4", "G5"), ids(GroupNetworkBuilder.build(model, "G2", 2, null)));
		Assertions.assertEquals(4, GroupNetworkBuilder.build(model, "G2", 2, null).edges().size());
		//from the other end Remote is the center, and the Club is two away
		Assertions.assertEquals(Set.of("G5", "G4", "G2"), ids(GroupNetworkBuilder.build(model, "G5", 2, null)));
	}

	@Test
	void theKindOfEachRelationshipIsKept(){
		final GroupNetwork network = GroupNetworkBuilder.build(model(), "G2", 1, null);

		final GroupNetwork.Edge partOf = network.edges().stream().filter(edge -> edge.secondId().equals("G1")).findFirst().orElseThrow();
		Assertions.assertEquals(GroupNetwork.Kind.PART_OF, partOf.kind());
		Assertions.assertEquals("G2", partOf.firstId(), "the part is the subject, the whole the target");
		Assertions.assertEquals(1, network.edges().stream().filter(edge -> edge.kind() == GroupNetwork.Kind.ASSOCIATE).count());
	}

	@Test
	void theBadgeCountsTheMembers(){
		final GroupNetwork network = GroupNetworkBuilder.build(model(), "G2", 1, null);

		Assertions.assertEquals(2, node(network, "G2").members(), "Anna and Bruno");
		Assertions.assertEquals(1, node(network, "G3").members(), "Carla");
		Assertions.assertEquals(0, node(network, "G1").members());
	}

	@Test
	void aYearKeepsWhatWasInEffectAndWhatHasNoDate(){
		final FLEFModel model = model();

		//1905: the Club is part of the Federation; Youth and the Rivals do not exist yet; Anna is a member, Bruno has no date
		final GroupNetwork in1905 = GroupNetworkBuilder.build(model, "G2", 2, 1905);
		Assertions.assertEquals(Set.of("G1", "G2"), ids(in1905));
		Assertions.assertEquals(2, node(in1905, "G2").members());
		//1915: the membership of Anna has ended
		Assertions.assertEquals(1, node(GroupNetworkBuilder.build(model, "G2", 2, 1915), "G2").members());
		//1930: all the relationships are in effect
		Assertions.assertEquals(Set.of("G1", "G2", "G3", "G4", "G5"), ids(GroupNetworkBuilder.build(model, "G2", 2, 1930)));
		//1960: the Club is not part of the Federation any more; a relationship with no date is kept
		final GroupNetwork in1960 = GroupNetworkBuilder.build(model, "G2", 2, 1960);
		Assertions.assertEquals(Set.of("G2", "G3", "G4", "G5"), ids(in1960));
		Assertions.assertEquals(1960, in1960.year());
	}

	@Test
	void whatIsNotAGroupHasNoNetwork(){
		final FLEFModel model = model();

		Assertions.assertTrue(GroupNetworkBuilder.build(model, "I1", 2, null).isEmpty());
		Assertions.assertTrue(GroupNetworkBuilder.build(model, "nothing", 2, null).isEmpty());
		Assertions.assertTrue(GroupNetworkBuilder.build(model, null, 2, null).isEmpty());
	}

	@Test
	void theWholesAreAboveTheirPartsAndAnAssociateIsOnTheLevelOfItsAssociate(){
		final GroupNetworkLayout.Layout layout = GroupNetworkLayout.compute(GroupNetworkBuilder.build(model(), "G2", 2, null));
		final Map<String, Integer> levels = layout.levels();

		Assertions.assertEquals(0, levels.get("G1"));
		Assertions.assertEquals(1, levels.get("G2"));
		Assertions.assertEquals(2, levels.get("G3"));
		Assertions.assertEquals(1, levels.get("G4"), "associated with the Club, not part of it");
		Assertions.assertEquals(1, levels.get("G5"), "associated with the Rivals");
		final Map<String, Rectangle> bounds = layout.bounds();
		Assertions.assertTrue(bounds.get("G1").y < bounds.get("G2").y);
		Assertions.assertEquals(bounds.get("G2").y, bounds.get("G4").y);
		Assertions.assertTrue(bounds.get("G2").y < bounds.get("G3").y);
	}

	@Test
	void noGroupCoversAnotherAndEverythingIsInsideTheSize(){
		final GroupNetworkLayout.Layout layout = GroupNetworkLayout.compute(GroupNetworkBuilder.build(model(), "G2", 2, null));
		final List<Rectangle> boxes = new ArrayList<>(layout.bounds().values());

		for(int i = 0; i < boxes.size(); i ++){
			Assertions.assertTrue(boxes.get(i).x >= 0 && boxes.get(i).getMaxX() <= layout.size().width);
			Assertions.assertTrue(boxes.get(i).y >= 0 && boxes.get(i).getMaxY() <= layout.size().height);
			for(int j = i + 1; j < boxes.size(); j ++)
				Assertions.assertFalse(boxes.get(i).intersects(boxes.get(j)), boxes.get(i) + " over " + boxes.get(j));
		}
	}

	@Test
	void theLayoutIsTheSameEveryTime(){
		final GroupNetwork network = GroupNetworkBuilder.build(model(), "G2", 2, null);

		Assertions.assertEquals(GroupNetworkLayout.compute(network).bounds(), GroupNetworkLayout.compute(network).bounds());
	}

	@Test
	void aCycleOfPartOfDoesNotLoop(){
		final FLEFModel model = new FLEFParser().parse(HEADER + group("G6", "Six") + group("G7", "Seven")
			+ relationship("R1", "group G6", "G7", "part_of", null, null)
			+ relationship("R2", "group G7", "G6", "part_of", null, null));

		final GroupNetworkLayout.Layout layout = GroupNetworkLayout.compute(GroupNetworkBuilder.build(model, "G6", 3, null));

		Assertions.assertEquals(Set.of("G6", "G7"), layout.bounds().keySet());
	}

	@Test
	void anEmptyNetworkHasAnEmptyLayout(){
		final GroupNetworkLayout.Layout layout = GroupNetworkLayout.compute(GroupNetwork.empty());

		Assertions.assertTrue(layout.bounds().isEmpty());
	}

	@Test
	void thePanelFollowsTheLevelsAndTheYear() throws InvocationTargetException, InterruptedException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The views need a display");
		final FLEFModel model = model();

		final GroupNetworkPanel[] panel = new GroupNetworkPanel[1];
		final List<Integer> sizes = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			panel[0] = new GroupNetworkPanel(model);
			panel[0].showGroup("G2");
			sizes.add(panel[0].getNetwork().nodes().size());
			panel[0].setDepth(0);
			sizes.add(panel[0].getNetwork().nodes().size());
			panel[0].setDepth(2);
			panel[0].setYear(1905);
			sizes.add(panel[0].getNetwork().nodes().size());
			panel[0].setYear(null);
			sizes.add(panel[0].getNetwork().nodes().size());
		});

		Assertions.assertEquals(List.of(5, 1, 2, 5), sizes, "two levels by default (all five); the Club alone; the Club and the Federation in 1905; all");
		Assertions.assertEquals("G2", panel[0].getCenterId());
		Assertions.assertNull(panel[0].getYear());
	}

	@Test
	void theDialogClosesWithEsc() throws InvocationTargetException, InterruptedException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = model();

		SwingUtilities.invokeAndWait(() -> {
			final JDialog dialog = GroupViewDialog.forNetwork(new ToolContext(model, null, new JPanel(), null), "G2");

			final Object binding = dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(GUIHelper.ESCAPE_STROKE);
			Assertions.assertNotNull(binding, "ESC is not bound");
			final Action action = dialog.getRootPane().getActionMap().get(binding);
			Assertions.assertNotNull(action);
			Assertions.assertTrue(dialog.isDisplayable());
			action.actionPerformed(new ActionEvent(dialog, ActionEvent.ACTION_PERFORMED, "escape"));
			Assertions.assertFalse(dialog.isDisplayable(), "ESC closes the dialog");
		});
	}

}
