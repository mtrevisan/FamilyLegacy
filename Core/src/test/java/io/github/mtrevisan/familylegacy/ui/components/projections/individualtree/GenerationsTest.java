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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;


/** The generations of the tree are set with one call, the tree stays compact, and nothing is drawn over something else. */
class GenerationsTest{

	private static String person(final String id){
		return "individual " + id + " {\n\tname {\n\t\tpart {\n\t\t\ttype given\n\t\t\tvalue N" + id + "\n\t\t}\n\t}\n\tsex male\n}\n";
	}

	private static String child(final String id, final String child, final String parent){
		return "relationship " + id + " {\n\tsubject.individual " + child + "\n\ttarget.individual " + parent
			+ "\n\ttype biological_child\n}\n";
	}

	/** Four generations, with the ancestors of only one side: a pedigree full of holes. */
	private static final String FLEF = """
		header {
			protocol {
				name Family LEgacy Format
				version 0.2.0
			}
			date 2026-01-01T00:00:00Z
		}
		""" + person("I1") + person("I2") + person("I3") + person("I4") + person("I5") + person("I6") + person("I7")
		+ person("I8") + person("I9")
		+ child("R1", "I1", "I2") + child("R2", "I1", "I3") + child("R3", "I2", "I4") + child("R4", "I2", "I5")
		+ child("R5", "I4", "I6") + child("R6", "I4", "I7") + child("R7", "I6", "I8") + child("R8", "I6", "I9");


	private static void flush() throws Exception{
		for(int i = 0; i < 4; i ++){
			SwingUtilities.invokeAndWait(() -> {});
			Thread.sleep(120);
		}
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

	private static List<Rectangle> boxes(final IndividualTreeGraphPanel panel, final JFrame frame) throws Exception{
		flush();
		final List<Rectangle> result = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> collect(panel, frame.getContentPane(), result));
		return result;
	}

	private static IndividualTreeGraphPanel panel(final FLEFModel model){
		final GenealogyRepository repository = new GenealogyRepository(RelationshipReader.BIOLOGICAL, model);
		return new IndividualTreeGraphPanel(TreeLayout.VERTICAL, new TreeLayoutEngine(), repository, model)
			.withShowPartner();
	}


	@Test
	void theGenerationsAreSetWithOneCallUntilThereAreNoMoreAncestors() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The window needs a display");
		final FLEFModel model = new FLEFParser().parse(FLEF);
		final IndividualTreeGraphPanel panel = panel(model);
		final JFrame[] frame = new JFrame[1];
		SwingUtilities.invokeAndWait(() -> {
			panel.load("I1", 1);
			frame[0] = new JFrame();
			frame[0].add(panel);
			frame[0].setSize(1600, 900);
			frame[0].setVisible(true);
		});
		try{
			final int first = boxes(panel, frame[0]).size();
			SwingUtilities.invokeAndWait(() -> Assertions.assertTrue(panel.hasMoreAncestors()));

			int previous = first;
			for(int generations = 2; generations <= 6; generations ++){
				final int shown = generations;
				SwingUtilities.invokeAndWait(() -> panel.setMaxAncestors(shown));
				final int now = boxes(panel, frame[0]).size();
				Assertions.assertTrue(now >= previous, previous + " -> " + now);
				previous = now;
			}

			Assertions.assertTrue(previous > first, first + " -> " + previous);
			final boolean[] more = new boolean[1];
			SwingUtilities.invokeAndWait(() -> more[0] = panel.hasMoreAncestors());
			Assertions.assertFalse(more[0], "every ancestor of the file is shown");

			//the number is the panel's, and is bounded
			SwingUtilities.invokeAndWait(() -> panel.setMaxAncestors(100));
			Assertions.assertEquals(12, panel.getMaxAncestors());
			SwingUtilities.invokeAndWait(() -> panel.setMaxAncestors(0));
			Assertions.assertEquals(1, panel.getMaxAncestors());
		}
		finally{
			SwingUtilities.invokeAndWait(() -> frame[0].dispose());
		}
	}

	@Test
	void theGenerationsChosenAreKeptWhenAnotherPersonIsShown() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The window needs a display");
		final FLEFModel model = new FLEFParser().parse(FLEF);
		final IndividualTreeGraphPanel panel = panel(model);

		SwingUtilities.invokeAndWait(() -> {
			panel.load("I1", 2);
			panel.setMaxAncestors(4);
			//what the switcher does when another person is shown: it asks the tree how many generations it shows
			panel.load("I2", panel.getMaxAncestors());

			Assertions.assertEquals(4, panel.getMaxAncestors());
		});
	}

	@Test
	void aPedigreeFullOfHolesStaysCompactAndNothingOverlaps() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The window needs a display");
		final FLEFModel model = new FLEFParser().parse(FLEF);
		final IndividualTreeGraphPanel panel = panel(model);
		final JFrame[] frame = new JFrame[1];
		SwingUtilities.invokeAndWait(() -> {
			panel.load("I1", 4);
			frame[0] = new JFrame();
			frame[0].add(panel);
			frame[0].setSize(2400, 900);
			frame[0].setVisible(true);
		});
		try{
			final List<Rectangle> boxes = boxes(panel, frame[0]);
			Assertions.assertFalse(boxes.isEmpty());
			final String dir = System.getenv("GROUP_VIEW_IMAGES");
			if(dir != null){
				final java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(frame[0].getContentPane().getWidth(),
					frame[0].getContentPane().getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
				frame[0].getContentPane().paint(image.createGraphics());
				javax.imageio.ImageIO.write(image, "png", new java.io.File(dir, "compact.png"));
			}

			//nothing is drawn over something else
			for(int i = 0; i < boxes.size(); i ++)
				for(int j = i + 1; j < boxes.size(); j ++)
					Assertions.assertFalse(boxes.get(i).intersects(boxes.get(j)), boxes.get(i) + " over " + boxes.get(j));

			//the tree is as wide as its widest row needs, not as a complete pedigree of four generations (16 couples side by side)
			final int widest = boxes.stream()
				.mapToInt(b -> b.width)
				.max()
				.orElse(0);
			final int right = boxes.stream()
				.mapToInt(b -> b.x + b.width)
				.max()
				.orElse(0);
			final int left = boxes.stream()
				.mapToInt(b -> b.x)
				.min()
				.orElse(0);
			Assertions.assertTrue(right - left < 16 * widest, (right - left) + " of " + (16 * widest));
		}
		finally{
			SwingUtilities.invokeAndWait(() -> frame[0].dispose());
		}
	}

}
