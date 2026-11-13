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
package io.github.mtrevisan.familylegacy.ui.tools.maintenance;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.tools.maintenance.merge.MergeRecordReviewDialog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


/** The list of the duplicates opens the merge of the selected pair, already set. */
class DuplicateFinderMergeTest{

	private static FLEFModel model(){
		return new FLEFParser().parse("""
			header {
				protocol {
					name Family LEgacy Format
					version 0.2.0
				}
				date 2026-01-01T00:00:00Z
			}
			individual I1 {
				name {
					part {
						type given
						value Anna
					}
				}
			}
			individual I2 {
				name {
					part {
						type given
						value Anna
					}
				}
			}
			""");
	}

	private static JTable findTable(final Container container){
		for(final Component child : container.getComponents()){
			if(child instanceof JTable table)
				return table;
			if(child instanceof Container nested){
				final JTable found = findTable(nested);
				if(found != null)
					return found;
			}
		}
		return null;
	}

	private static Object field(final Object owner, final String name) throws Exception{
		final Field f = owner.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.get(owner);
	}

	@Test
	void mergeOpensThePairAlreadySet() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
		final JPanel panel = new JPanel();
		final JFrame frame = new JFrame("main");
		frame.add(panel);
		frame.setSize(800, 600);
		frame.setVisible(true);
		final ToolContext context = new ToolContext(model(), "I1", panel, null);
		final AtomicReference<DuplicateFinderDialog> holder = new AtomicReference<>();
		SwingUtilities.invokeLater(() -> {
			final DuplicateFinderDialog d = new DuplicateFinderDialog(context);
			holder.set(d);
			d.setVisible(true);
		});
		for(int i = 0; i < 100 && (holder.get() == null || !holder.get().isVisible()); i ++)
			Thread.sleep(50);
		final DuplicateFinderDialog dialog = holder.get();
		final JTable table = findTable(dialog.getContentPane());
		final JButton merge = (JButton)field(dialog, "mergeButton");
		final JButton different = (JButton)field(dialog, "differentButton");

		final List<Throwable> errors = new ArrayList<>();
		final List<String> reviews = new ArrayList<>();
		final List<String> others = new ArrayList<>();
		final Timer closer = new Timer(100, e -> {
			for(final Window window : Window.getWindows())
				if(window.isVisible() && window != dialog && window != frame){
					if(window instanceof MergeRecordReviewDialog review)
						reviews.add(review.getTitle());
					else
						others.add(window.getClass().getName());
					window.dispose();
				}
		});
		try{
			SwingUtilities.invokeAndWait(() -> {
				try{
					final JSpinner spinner = (JSpinner)field(dialog, "thresholdSpinner");
					((SpinnerNumberModel)spinner.getModel()).setMinimum(0);
					spinner.setValue(0);
					final Method run = DuplicateFinderDialog.class.getDeclaredMethod("runSearch");
					run.setAccessible(true);
					run.invoke(dialog);
				}
				catch(final Exception e){
					errors.add(e);
				}
			});
			for(int i = 0; i < 200 && table.getRowCount() == 0; i ++)
				Thread.sleep(50);
			Assertions.assertTrue(table.getRowCount() > 0, "no candidate found");

			SwingUtilities.invokeAndWait(() -> {
				table.clearSelection();
			});
			Assertions.assertFalse(merge.isEnabled());
			Assertions.assertFalse(different.isEnabled());

			SwingUtilities.invokeAndWait(() -> table.setRowSelectionInterval(0, 0));
			Assertions.assertTrue(merge.isEnabled());
			Assertions.assertTrue(different.isEnabled());

			closer.start();
			SwingUtilities.invokeAndWait(merge::doClick);
			Thread.sleep(300);
		}
		finally{
			closer.stop();
			frame.dispose();
			SwingUtilities.invokeAndWait(dialog::dispose);
		}

		Assertions.assertTrue(errors.isEmpty(), errors.toString());
		//the review of the merge opens at once, with the two individuals of the pair (the one kept first): no window to pick them again
		Assertions.assertEquals(1, reviews.size(), reviews.toString());
		Assertions.assertTrue(reviews.get(0).contains("[I1]") && reviews.get(0).contains("[I2]"), reviews.get(0));
		Assertions.assertTrue(reviews.get(0).indexOf("[I1]") < reviews.get(0).indexOf("[I2]"), reviews.get(0));
		Assertions.assertTrue(others.isEmpty(), others.toString());
	}

}
