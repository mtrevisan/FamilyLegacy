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
package io.github.mtrevisan.familylegacy.ui.shell;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.ui.components.projections.individualtree.TreeType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.JSpinner;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;


/** The Generations spinner of the toolbar changes the tree of the real window. */
class GenerationsSpinnerTest{

	private static String person(final String id){
		return "individual " + id + " {\n\tname {\n\t\tpart {\n\t\t\ttype given\n\t\t\tvalue N" + id + "\n\t\t}\n\t}\n\tsex male\n}\n";
	}

	private static String child(final String id, final String child, final String parent){
		return "relationship " + id + " {\n\tsubject.individual " + child + "\n\ttarget.individual " + parent
			+ "\n\ttype biological_child\n}\n";
	}

	private static final String FLEF = """
		header {
			protocol {
				name Family LEgacy Format
				version 0.2.0
			}
			date 2026-01-01T00:00:00Z
		}
		""" + person("I1") + person("I2") + person("I3") + person("I4") + person("I5") + person("I6") + person("I7")
		+ child("R1", "I1", "I2") + child("R2", "I1", "I3") + child("R3", "I2", "I4") + child("R4", "I2", "I5")
		+ child("R5", "I4", "I6") + child("R6", "I4", "I7");


	private static void waitFor(final BooleanSupplier condition) throws InterruptedException{
		final long deadline = System.currentTimeMillis() + 20_000;
		while(!condition.getAsBoolean() && System.currentTimeMillis() < deadline)
			Thread.sleep(50);
	}

	private static int countPanels(final Container container){
		int total = 0;
		for(final Component child : container.getComponents()){
			if(child instanceof IndividualPanel && child.isShowing() && child.getWidth() > 0)
				total ++;
			else if(child instanceof Container c)
				total += countPanels(c);
		}
		return total;
	}

	private static JSpinner spinner(final Container container){
		for(final Component child : container.getComponents()){
			if(child instanceof JSpinner s)
				return s;
			if(child instanceof Container c){
				final JSpinner found = spinner(c);
				if(found != null)
					return found;
			}
		}
		return null;
	}

	private static int panels(final FamilyLegacyFrame frame) throws Exception{
		for(int i = 0; i < 4; i ++){
			SwingUtilities.invokeAndWait(() -> {});
			Thread.sleep(150);
		}
		final int[] total = {0};
		SwingUtilities.invokeAndWait(() -> total[0] = countPanels(frame.switcher()));
		return total[0];
	}


	@Test
	void theSpinnerChangesTheTreeAndTheNumberIsKept(@TempDir final Path directory) throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The window needs a display");
		final File file = directory.resolve("g.flef").toFile();
		Files.writeString(file.toPath(), FLEF, StandardCharsets.UTF_8);
		final FamilyLegacyFrame[] holder = new FamilyLegacyFrame[1];
		SwingUtilities.invokeAndWait(() -> {
			holder[0] = new FamilyLegacyFrame(TreeType.FAMILY, new FLEFModel());
			holder[0].setTerminator(() -> {});
			holder[0].setSize(1600, 900);
			holder[0].setVisible(true);
		});
		final FamilyLegacyFrame frame = holder[0];
		try{
			SwingUtilities.invokeLater(() -> frame.openFile(file));
			waitFor(() -> file.equals(frame.fileController().currentFile()));
			final int normal = panels(frame);

			final JSpinner[] spinner = new JSpinner[1];
			SwingUtilities.invokeAndWait(() -> spinner[0] = spinner(frame.getContentPane()));
			Assertions.assertNotNull(spinner[0]);
			Assertions.assertTrue(spinner[0].isEnabled());

			SwingUtilities.invokeAndWait(() -> spinner[0].setValue(4));
			final int more = panels(frame);
			SwingUtilities.invokeAndWait(() -> spinner[0].setValue(1));
			final int less = panels(frame);

			Assertions.assertTrue(more > normal, normal + " -> " + more);
			Assertions.assertTrue(less < normal, normal + " -> " + less);

			//the number goes on being the same when another person is shown
			SwingUtilities.invokeAndWait(() -> spinner[0].setValue(3));
			SwingUtilities.invokeAndWait(() -> frame.loadRoot("I2"));
			Assertions.assertEquals(3, frame.switcher().getGenerations());
		}
		finally{
			SwingUtilities.invokeAndWait(frame::dispose);
		}
	}

}
