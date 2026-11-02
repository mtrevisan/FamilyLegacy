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

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;


/**
 * What the network of the groups adds to the neighborhood: the kinds of relationship, the members of the subgroups, the role and the dates
 * of a relationship, the zoom, the tooltips and the labels. The data are those of {@link GroupNetworkTest}.
 */
class GroupNetworkFeaturesTest{

	private static FLEFModel model(){
		return GroupNetworkTest.model();
	}

	/** The text of a relationship with a line added before the closing brace. */
	private static String withLine(final String record, final String line){
		return record.substring(0, record.lastIndexOf('}')) + "\t" + line + "\n}\n";
	}

	private static FontMetrics metrics(){
		final Graphics2D g = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).createGraphics();
		try{
			return g.getFontMetrics();
		}
		finally{
			g.dispose();
		}
	}


	@Test
	void aKindThatIsLeftOutIsNeitherShownNorFollowed(){
		final FLEFModel model = model();

		final GroupNetwork hierarchy = GroupNetworkBuilder.build(model, "G2", 2, null, EnumSet.of(GroupNetwork.Kind.PART_OF), false);
		Assertions.assertEquals(Set.of("G1", "G2", "G3"), GroupNetworkTest.ids(hierarchy), "the Rivals are not reached through an association");
		Assertions.assertEquals(2, hierarchy.edges().size());

		final GroupNetwork associations = GroupNetworkBuilder.build(model, "G2", 2, null, EnumSet.of(GroupNetwork.Kind.ASSOCIATE), false);
		Assertions.assertEquals(Set.of("G2", "G4", "G5"), GroupNetworkTest.ids(associations));
		Assertions.assertEquals(2, associations.edges().size());

		Assertions.assertEquals(Set.of("G2"), GroupNetworkTest.ids(GroupNetworkBuilder.build(model, "G2", 2, null,
			EnumSet.noneOf(GroupNetwork.Kind.class), false)));
	}

	@Test
	void theMembersOfTheSubgroupsCountOnceWhenAsked(){
		final FLEFModel model = model();
		final Set<GroupNetwork.Kind> all = EnumSet.allOf(GroupNetwork.Kind.class);

		final GroupNetwork direct = GroupNetworkBuilder.build(model, "G2", 1, null, all, false);
		final GroupNetwork withSubgroups = GroupNetworkBuilder.build(model, "G2", 1, null, all, true);

		Assertions.assertEquals(0, GroupNetworkTest.node(direct, "G1").members());
		Assertions.assertEquals(3, GroupNetworkTest.node(withSubgroups, "G1").members(), "Anna and Bruno of the Club, Carla of Youth");
		Assertions.assertEquals(3, GroupNetworkTest.node(withSubgroups, "G2").members(), "its own two and the one of Youth");
		Assertions.assertEquals(1, GroupNetworkTest.node(withSubgroups, "G3").members());
		Assertions.assertEquals(0, GroupNetworkTest.node(withSubgroups, "G4").members(), "an associate is not a subgroup");
	}

	@Test
	void theMembersOfTheSubgroupsFollowTheYear(){
		//in 1915 Youth is not part of the Club yet, and the membership of Anna has ended: the Federation has the one of Bruno
		final GroupNetwork in1915 = GroupNetworkBuilder.build(model(), "G2", 2, 1915, EnumSet.allOf(GroupNetwork.Kind.class), true);

		Assertions.assertEquals(1, GroupNetworkTest.node(in1915, "G1").members());
		Assertions.assertEquals(1, GroupNetworkTest.node(in1915, "G2").members());
	}

	@Test
	void aCycleOfPartOfDoesNotLoopCountingTheMembers(){
		final FLEFModel model = new FLEFParser().parse(GroupNetworkTest.HEADER + GroupNetworkTest.person("I1", "Anna")
			+ GroupNetworkTest.group("G6", "Six") + GroupNetworkTest.group("G7", "Seven")
			+ GroupNetworkTest.relationship("R1", "group G6", "G7", "part_of", null, null)
			+ GroupNetworkTest.relationship("R2", "group G7", "G6", "part_of", null, null)
			+ GroupNetworkTest.relationship("R3", "individual I1", "G6", "group_member", null, null));

		final GroupNetwork network = GroupNetworkBuilder.build(model, "G6", 2, null, EnumSet.allOf(GroupNetwork.Kind.class), true);

		Assertions.assertEquals(1, GroupNetworkTest.node(network, "G6").members());
		Assertions.assertEquals(1, GroupNetworkTest.node(network, "G7").members());
	}

	@Test
	void aRelationshipTellsItsDatesAndItsRole(){
		final FLEFModel model = new FLEFParser().parse(GroupNetworkTest.HEADER + GroupNetworkTest.group("G1", "Federation")
			+ GroupNetworkTest.group("G2", "Club") + GroupNetworkTest.group("G3", "Youth")
			+ withLine(GroupNetworkTest.relationship("R1", "group G2", "G1", "part_of", "1900-01-01", "1950-01-01"), "role founder")
			+ GroupNetworkTest.relationship("R2", "group G3", "G2", "part_of", null, null));

		final GroupNetwork network = GroupNetworkBuilder.build(model, "G2", 1, null);

		final GroupNetwork.Edge dated = network.edges().stream().filter(edge -> edge.firstId().equals("G2")).findFirst().orElseThrow();
		Assertions.assertEquals("founder", dated.role());
		Assertions.assertTrue(dated.dates().contains("1900") && dated.dates().contains("1950"), dated.dates());
		final GroupNetwork.Edge undated = network.edges().stream().filter(edge -> edge.firstId().equals("G3")).findFirst().orElseThrow();
		Assertions.assertTrue(undated.dates().isEmpty());
		Assertions.assertNull(undated.role());
	}

	@Test
	void aLongNameIsOnTwoLinesAndWhatDoesNotFitIsCut(){
		final FontMetrics metrics = metrics();
		final int width = metrics.stringWidth("Federation") + 4;

		Assertions.assertEquals(List.of("Federation"), GroupNetworkPanel.wrap(metrics, "Federation", width));

		final List<String> lines = GroupNetworkPanel.wrap(metrics, "Federation of the clubs of the valley", width);
		Assertions.assertEquals(2, lines.size());
		Assertions.assertTrue(lines.stream().allMatch(line -> metrics.stringWidth(line) <= width), lines.toString());
		Assertions.assertTrue(lines.getLast().endsWith("…"), "the second line is cut: " + lines);

		final List<String> word = GroupNetworkPanel.wrap(metrics, "Supercalifragilisticexpialidocious", width);
		Assertions.assertEquals(1, word.size(), "no space to break at");
		Assertions.assertTrue(word.getFirst().endsWith("…"));
	}

	@Test
	void theZoomStaysBetweenItsLimits(){
		final GroupNetworkPanel panel = new GroupNetworkPanel(model());

		panel.setZoom(100.);
		Assertions.assertEquals(3., panel.getZoom(), 1e-12);
		panel.setZoom(0.0001);
		Assertions.assertEquals(0.3, panel.getZoom(), 1e-12);
		panel.setZoom(1.5);
		Assertions.assertEquals(1.5, panel.getZoom(), 1e-12);
	}

	@Test
	void theTooltipTellsTheGroupAndTheRelationshipUnderThePointer() throws InvocationTargetException, InterruptedException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The views need a display");
		final FLEFModel model = model();

		final List<String> tips = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			final GroupNetworkPanel panel = new GroupNetworkPanel(model);
			panel.showGroup("G2");

			final Rectangle club = panel.nodeBounds("G2");
			tips.add(panel.toolTipAt(new Point(club.x + 10, club.y + club.height / 2)));

			//just under the Federation, on the line to the Club
			final Rectangle federation = panel.nodeBounds("G1");
			tips.add(panel.toolTipAt(new Point(federation.x + federation.width / 2, federation.y + federation.height + 5)));

			tips.add(panel.toolTipAt(new Point(0, 0)));
		});

		Assertions.assertTrue(tips.get(0).contains("Club") && tips.get(0).contains("2"), tips.get(0));
		Assertions.assertTrue(tips.get(1).contains("Club") && tips.get(1).contains("Federation") && tips.get(1).contains("1900"), tips.get(1));
		Assertions.assertNull(tips.get(2), "nothing there");
	}

	@Test
	void thePanelFollowsTheKindsAndTheCountAndPaintsAtEveryZoom() throws InvocationTargetException, InterruptedException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The views need a display");
		final FLEFModel model = model();

		final List<Integer> numbers = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			final GroupNetworkPanel panel = new GroupNetworkPanel(model);
			panel.showGroup("G2");
			numbers.add(panel.getNetwork().nodes().size());

			panel.setKindShown(GroupNetwork.Kind.ASSOCIATE, false);
			numbers.add(panel.getNetwork().nodes().size());
			panel.setKindShown(GroupNetwork.Kind.ASSOCIATE, true);

			panel.setIncludeSubgroupMembers(true);
			numbers.add(GroupNetworkTest.node(panel.getNetwork(), "G1").members());

			//in a window (not shown) the scroll pane has a size, and the view can be fit in it
			final JFrame frame = new JFrame();
			try{
				frame.add(panel);
				frame.pack();
				frame.setSize(900, 600);
				frame.validate();
				for(final double zoom : new double[]{0.5, 1., 2.}){
					panel.setZoom(zoom);
					final BufferedImage image = new BufferedImage(900, 600, BufferedImage.TYPE_INT_RGB);
					final Graphics2D g = image.createGraphics();
					try{
						panel.paint(g);
					}
					finally{
						g.dispose();
					}
				}
				panel.zoomToFit();
				numbers.add((int)Math.round(panel.getZoom() * 100.));
			}
			finally{
				frame.dispose();
			}
		});

		Assertions.assertEquals(5, numbers.get(0));
		Assertions.assertEquals(3, numbers.get(1), "the Club, the Federation and Youth: no association");
		Assertions.assertEquals(3, numbers.get(2), "the members of the subgroups");
		Assertions.assertTrue(numbers.get(3) >= 30 && numbers.get(3) <= 100, "fits, never more than the natural size: " + numbers);
	}

}
