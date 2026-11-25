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
package io.github.mtrevisan.familylegacy.ui.components.projections.individualtree;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.tree.GenealogyRepository;
import io.github.mtrevisan.familylegacy.ui.components.projections.common.TreeLayout;
import io.github.mtrevisan.familylegacy.ui.components.projections.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.ui.components.projections.individualtree.layout.TreeLayoutEngine;
import io.github.mtrevisan.familylegacy.ui.components.projections.individualtree.layout.sugiyama.GraphLayoutEngine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;


/** The biological tree and the Sugiyama graph place the same individuals at the same size and distances. */
class TreeSugiyamaUniformityTest{

	private static final String FLEF = """
		header {
			protocol {
				name Family LEgacy Format
				version 0.2.0
			}
			date 2026-01-01T00:00:00Z
		}
		""" + person("A", "Agnol", "male") + person("B", "Antonia", "female") + person("C1", "Lucia", "female")
		+ person("C2", "Antonio", "male") + person("C3", "Francesco", "male") + person("FA", "Francesco", "male")
		+ person("MA", "Mother", "female") + person("FB", "Hieronimo", "male") + person("MB", "Menega", "female")
		+ rel("R1", "spouse", "A", "B") + rel("R2", "biological_child", "C1", "A") + rel("R3", "biological_child", "C1", "B")
		+ rel("R4", "biological_child", "C2", "A") + rel("R5", "biological_child", "C2", "B")
		+ rel("R6", "biological_child", "C3", "A") + rel("R7", "biological_child", "C3", "B")
		+ rel("R8", "biological_child", "A", "FA") + rel("R9", "biological_child", "A", "MA")
		+ rel("R10", "biological_child", "B", "FB") + rel("R11", "biological_child", "B", "MB");


	private static String person(final String id, final String name, final String sex){
		return "individual " + id + " {\n\tname {\n\t\tpart {\n\t\t\ttype given\n\t\t\tvalue " + name + "\n\t\t}\n\t}\n\tsex " + sex + "\n}\n";
	}

	private static String rel(final String id, final String type, final String subject, final String target){
		return "relationship " + id + " {\n\tsubject.individual " + subject + "\n\ttarget.individual " + target + "\n\ttype " + type + "\n}\n";
	}


	@Test
	void sameGeometry() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

		final FLEFModel model = new FLEFParser().parse(FLEF);
		final GenealogyRepository repository = new GenealogyRepository(RelationshipReader.BIOLOGICAL, model);
		final IndividualTreeGraphPanel panel = new IndividualTreeGraphPanel(TreeLayout.VERTICAL, new TreeLayoutEngine(), repository, model)
			.withShowPartner();
		final JFrame[] frame = new JFrame[1];
		SwingUtilities.invokeAndWait(() -> {
			panel.load("B", 2);
			frame[0] = new JFrame();
			frame[0].add(panel);
			frame[0].setSize(1000, 700);
			frame[0].setVisible(true);
		});
		flush();
		final List<Rectangle> tree = boxes(panel, frame[0], "tree");

		SwingUtilities.invokeAndWait(() -> panel.setLayoutEngine(new GraphLayoutEngine()));
		flush();
		final List<Rectangle> graph = boxes(panel, frame[0], "graph");
		SwingUtilities.invokeAndWait(() -> frame[0].dispose());



		Assertions.assertEquals(tree, graph);
	}


	@Test
	void rootCoupleIsCenteredUnderTheParentsRow() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

		//the grandparents of the father only: the mother side is one generation shorter
		final FLEFModel model = new FLEFParser().parse(FLEF + person("GF", "GrandFather", "male") + person("GM", "GrandMother", "female")
			+ person("GF2", "GrandFather2", "male") + person("GM2", "GrandMother2", "female")
			+ rel("R20", "biological_child", "FA", "GF") + rel("R21", "biological_child", "FA", "GM")
			+ rel("R22", "biological_child", "MA", "GF2") + rel("R23", "biological_child", "MA", "GM2"));
		final GenealogyRepository repository = new GenealogyRepository(RelationshipReader.BIOLOGICAL, model);
		final IndividualTreeGraphPanel panel = new IndividualTreeGraphPanel(TreeLayout.VERTICAL, new TreeLayoutEngine(), repository, model)
			.withShowPartner();
		final JFrame[] frame = new JFrame[1];
		SwingUtilities.invokeAndWait(() -> {
			panel.load("B", 3);
			frame[0] = new JFrame();
			frame[0].add(panel);
			frame[0].setSize(1400, 800);
			frame[0].setVisible(true);
		});
		flush();
		final List<Rectangle> tree = boxes(panel, frame[0], "tree3");
		SwingUtilities.invokeAndWait(() -> frame[0].dispose());

		//rows by y: grandparents, parents, root couple, children
		final List<Integer> ys = tree.stream().map(r -> r.y).distinct().sorted().toList();
		final int rootY = ys.get(ys.size() - 2);
		final int parentsY = ys.get(ys.size() - 3);
		final Rectangle parents = tree.stream().filter(r -> r.y == parentsY).reduce(Rectangle::union).orElseThrow();
		final Rectangle root = tree.stream().filter(r -> r.y == rootY).reduce(Rectangle::union).orElseThrow();
		Assertions.assertEquals(parents.getCenterX(), root.getCenterX(), 2., "parents " + parents + " root " + root);
	}

	@Test
	void parentsAreCenteredOverARootWithoutPartner() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

		final FLEFModel model = new FLEFParser().parse("""
			header {
				protocol {
					name Family LEgacy Format
					version 0.2.0
				}
				date 2026-01-01T00:00:00Z
			}
			""" + person("A", "Agnol", "male") + person("B", "Antonia", "female") + person("C1", "Lucia", "female")
			+ person("FA", "Francesco", "male") + person("MA", "Mother", "female") + person("FB", "Hieronimo", "male")
			+ person("MB", "Menega", "female")
			+ rel("R1", "spouse", "A", "B") + rel("R2", "biological_child", "C1", "A") + rel("R3", "biological_child", "C1", "B")
			+ rel("R8", "biological_child", "A", "FA") + rel("R9", "biological_child", "A", "MA")
			+ rel("R10", "biological_child", "B", "FB") + rel("R11", "biological_child", "B", "MB"));
		final GenealogyRepository repository = new GenealogyRepository(RelationshipReader.BIOLOGICAL, model);
		final IndividualTreeGraphPanel panel = new IndividualTreeGraphPanel(TreeLayout.VERTICAL, new TreeLayoutEngine(), repository, model)
			.withShowPartner();
		final JFrame[] frame = new JFrame[1];
		SwingUtilities.invokeAndWait(() -> {
			panel.load("C1", 2);
			frame[0] = new JFrame();
			frame[0].add(panel);
			frame[0].setSize(1000, 700);
			frame[0].setVisible(true);
		});
		flush();
		final List<Rectangle> tree = boxes(panel, frame[0], "singleChild");
		SwingUtilities.invokeAndWait(() -> frame[0].dispose());

		final List<Integer> ys = tree.stream().map(r -> r.y).distinct().sorted().toList();
		//a root without a partner is alone on its row: its parents are centered over it
		final Rectangle root = tree.stream().filter(r -> r.y == ys.getLast()).reduce(Rectangle::union).orElseThrow();
		final Rectangle parents = tree.stream().filter(r -> r.y == ys.get(ys.size() - 2)).reduce(Rectangle::union).orElseThrow();
		Assertions.assertEquals(root.getCenterX(), parents.getCenterX(), 2., "root " + root + " parents " + parents);
	}

	private static void flush() throws Exception{
		for(int i = 0; i < 5; i ++){
			SwingUtilities.invokeAndWait(() -> {});
			Thread.sleep(150);
		}
	}

	private static List<Rectangle> boxes(final Container root, final JFrame frame, final String name) throws Exception{
		final List<Rectangle> result = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> collect(root, frame.getContentPane(), result));
		result.sort((a, b) -> (a.y != b.y? Integer.compare(a.y, b.y): Integer.compare(a.x, b.x)));
		final String dir = System.getenv("GROUP_VIEW_IMAGES");
		if(dir != null){
			final BufferedImage image = new BufferedImage(frame.getContentPane().getWidth(), frame.getContentPane().getHeight(), BufferedImage.TYPE_INT_RGB);
			frame.getContentPane().paint(image.createGraphics());
			ImageIO.write(image, "png", new File(dir, name + ".png"));
		}
		return result;
	}

	private static void collect(final Component c, final Component origin, final List<Rectangle> out){
		if(c instanceof IndividualPanel && c.isShowing()){
			final Point p = SwingUtilities.convertPoint(c.getParent(), c.getLocation(), origin);
			out.add(new Rectangle(p.x, p.y, c.getWidth(), c.getHeight()));
		}
		else if(c instanceof Container container)
			for(final Component child : container.getComponents())
				collect(child, origin, out);
	}

}
