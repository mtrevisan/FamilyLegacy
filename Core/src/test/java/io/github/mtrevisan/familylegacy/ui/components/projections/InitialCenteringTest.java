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
package io.github.mtrevisan.familylegacy.ui.components.projections;

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.components.projections.common.ProjectionType;
import io.github.mtrevisan.familylegacy.ui.components.projections.individualtree.TreeType;
import io.github.mtrevisan.familylegacy.ui.shell.ProjectionSwitcherPanel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;


/** The tree and the graph start centered horizontally, the ego network with the ego in the middle, when they are larger than the window. */
class InitialCenteringTest{

	private static final int WINDOW_WIDTH = 500;
	private static final int WINDOW_HEIGHT = 260;
	private static final int CHILDREN = 14;
	private static final int GENERATIONS = 4;


	/** An individual with {@value #GENERATIONS} generations of ancestors (all of them) and {@value #CHILDREN} children. */
	private static String gedcom(){
		final StringBuilder sb = new StringBuilder("0 HEAD\n1 CHAR UTF-8\n");
		//the ancestors, numbered as a heap: 1 is the root, 2 and 3 its parents, 4..7 the grandparents...
		final int ancestors = (1 << (GENERATIONS + 1)) - 1;
		for(int i = 1; i <= ancestors; i ++)
			sb.append("0 @I").append(i).append("@ INDI\n1 NAME Person").append(i).append(" /Rossi/\n1 SEX ").append(i % 2 == 0? 'M': 'F')
				.append("\n1 BIRT\n2 DATE ").append(1900 - 25 * (Integer.SIZE - Integer.numberOfLeadingZeros(i))).append('\n');
		for(int i = 1; i <= ancestors / 2; i ++)
			sb.append("0 @F").append(i).append("@ FAM\n1 HUSB @I").append(2 * i).append("@\n1 WIFE @I").append(2 * i + 1).append("@\n1 CHIL @I")
				.append(i).append("@\n");
		//the partner and the children of the root
		sb.append("0 @I900@ INDI\n1 NAME Partner /Verdi/\n1 SEX M\n");
		sb.append("0 @FC@ FAM\n1 HUSB @I900@\n1 WIFE @I1@\n");
		for(int c = 0; c < CHILDREN; c ++){
			sb.append("0 @I").append(1000 + c).append("@ INDI\n1 NAME Child").append(c).append(" /Verdi/\n1 SEX F\n");
			sb.append("1 BIRT\n2 DATE ").append(1925 + c).append('\n');
			sb.append("0 @FC").append(c).append("@ FAM\n1 HUSB @I900@\n1 WIFE @I1@\n1 CHIL @I").append(1000 + c).append("@\n");
		}
		return sb.toString();
	}

	private static FLEFModel convert() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(gedcom()));
		return new GEDCOMToFLEFConverter().convert(roots);
	}

	private static String idOf(final FLEFModel model, final String name){
		for(final FLEFRecord individual : model.getRecordsByType(IndividualReader.TYPE)){
			final String fullname = IndividualReader.extractPrimaryFullname(individual);
			if(fullname != null && fullname.startsWith(name))
				return individual.getId();
		}
		throw new IllegalArgumentException(name);
	}

	private static JScrollPane firstScrollPane(final Container container){
		for(final Component child : container.getComponents()){
			if(child instanceof JScrollPane scroll)
				return scroll;
			if(child instanceof Container inner){
				final JScrollPane found = firstScrollPane(inner);
				if(found != null)
					return found;
			}
		}
		return null;
	}

	/** The text of the name of an individual (a text area), wherever it is in the component. */
	private static JTextComponent labelOf(final Container container, final String name){
		for(final Component child : container.getComponents()){
			if(child instanceof JTextComponent label && label.getText() != null && label.getText().contains(name) && label.isShowing())
				return label;
			if(child instanceof Container inner){
				final JTextComponent found = labelOf(inner, name);
				if(found != null)
					return found;
			}
		}
		return null;
	}

	private static void flush() throws InvocationTargetException, InterruptedException{
		for(int i = 0; i < 6; i ++){
			SwingUtilities.invokeAndWait(() -> {});
			Thread.sleep(40);
		}
	}

	/** Shows the projection in a window of a known size, and gives its scroll pane. */
	private static JScrollPane show(final ProjectionSwitcherPanel[] switcherHolder, final JFrame[] frameHolder, final FLEFModel model,
			final String root, final ProjectionType projection) throws InvocationTargetException, InterruptedException{
		final JScrollPane[] result = new JScrollPane[1];
		SwingUtilities.invokeAndWait(() -> {
			final ProjectionSwitcherPanel switcher = new ProjectionSwitcherPanel(TreeType.BIOLOGICAL, model);
			final JFrame frame = new JFrame();
			frame.add(switcher);
			frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
			frame.setVisible(true);
			switcherHolder[0] = switcher;
			frameHolder[0] = frame;

			switcher.loadRoot(root);
			switcher.setProjection(projection);
		});
		flush();
		SwingUtilities.invokeAndWait(() -> result[0] = firstScrollPane(switcherHolder[0].getCurrentPanel()));
		flush();
		return result[0];
	}

	private static List<Object> measure(final JScrollPane scroll) throws InvocationTargetException, InterruptedException{
		final List<Object> values = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			final Point position = scroll.getViewport().getViewPosition();
			final Dimension extent = scroll.getViewport().getExtentSize();
			final Dimension view = scroll.getViewport().getViewSize();
			values.add(position);
			values.add(extent);
			values.add(view);
			final Component content = scroll.getViewport().getView();
			final StringBuilder geometry = new StringBuilder(content.getClass().getSimpleName()).append(content.getBounds());
			if(content instanceof Container container)
				for(final Component child : container.getComponents()){
					geometry.append("; ").append(child.getClass().getSimpleName()).append(child.getBounds());
					if(child instanceof Container inner)
						for(final Component grandchild : inner.getComponents())
							geometry.append(" [").append(grandchild.getClass().getSimpleName()).append(grandchild.getBounds()).append("]");
				}
			values.add(geometry.toString());
		});
		return values;
	}


	private void checkHorizontallyCentered(final ProjectionType projection) throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The views need a display");
		final FLEFModel model = convert();
		final ProjectionSwitcherPanel[] switcher = new ProjectionSwitcherPanel[1];
		final JFrame[] frame = new JFrame[1];
		try{
			final JScrollPane scroll = show(switcher, frame, model, idOf(model, "Person1 "), projection);
			Assertions.assertNotNull(scroll);

			final List<Object> values = measure(scroll);
			final Point position = (Point)values.get(0);
			final Dimension extent = (Dimension)values.get(1);
			final Dimension view = (Dimension)values.get(2);

			Assertions.assertTrue(view.width > extent.width, "the tree is wider than the window: " + view + " in " + extent);
			Assertions.assertEquals(view.width / 2., position.x + extent.width / 2., 3., "the middle of the tree is the middle of the window: position " + position + ", extent " + extent + ", view " + view + ", geometry " + values.get(3));
		}
		finally{
			SwingUtilities.invokeAndWait(() -> {
				if(frame[0] != null)
					frame[0].dispose();
			});
		}
	}


	@Test
	void theBiologicalTreeStartsCenteredHorizontally() throws Exception{
		checkHorizontallyCentered(ProjectionType.BIOLOGICAL_TREE);
	}

	@Test
	void theSugiyamaGraphStartsCenteredHorizontally() throws Exception{
		checkHorizontallyCentered(ProjectionType.SUGIYAMA_GRAPH);
	}

	@Test
	void theEgoNetworkStartsWithTheEgoInTheMiddle() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The views need a display");
		final FLEFModel model = convert();
		final ProjectionSwitcherPanel[] switcher = new ProjectionSwitcherPanel[1];
		final JFrame[] frame = new JFrame[1];
		try{
			final JScrollPane scroll = show(switcher, frame, model, idOf(model, "Person1 "), ProjectionType.EGO_NETWORK);
			Assertions.assertNotNull(scroll);

			final List<Object> values = measure(scroll);
			final Point position = (Point)values.get(0);
			final Dimension extent = (Dimension)values.get(1);
			final Dimension view = (Dimension)values.get(2);

			Assertions.assertTrue(view.width > extent.width, "the network is wider than the window: " + view + " in " + extent);
			//the ego is in the middle column of the network: the middle of the window is the middle of the network
			Assertions.assertEquals(view.width / 2., position.x + extent.width / 2., 3.);
			Assertions.assertTrue(view.height > extent.height, "the network is taller than the window: " + view + " in " + extent);
			//the ego row is not the first (the parents are above it): the window is not left on the top
			Assertions.assertTrue(position.y > 0, "scrolled to the ego vertically: " + position);

			//and the name of the ego is in the middle of the window, vertically (the card is a few lines high)
			final double[] egoMiddle = new double[2];
			SwingUtilities.invokeAndWait(() -> {
				final JTextComponent label = labelOf(switcher[0].getCurrentPanel(), "Person1 Rossi");
				Assertions.assertNotNull(label, "the name of the ego is shown");
				final Component content = scroll.getViewport().getView();
				final Point inView = SwingUtilities.convertPoint(label, 0, label.getHeight() / 2, content);
				egoMiddle[0] = inView.y;
				egoMiddle[1] = scroll.getViewport().getViewPosition().y + scroll.getViewport().getExtentSize().height / 2.;
			});
			Assertions.assertEquals(egoMiddle[1], egoMiddle[0], 60., "the ego is vertically in the middle of the window");
		}
		finally{
			SwingUtilities.invokeAndWait(() -> {
				if(frame[0] != null)
					frame[0].dispose();
			});
		}
	}

}
