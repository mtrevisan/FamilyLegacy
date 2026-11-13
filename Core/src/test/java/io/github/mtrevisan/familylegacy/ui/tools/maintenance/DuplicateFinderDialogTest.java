package io.github.mtrevisan.familylegacy.ui.tools.maintenance;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;


/** The window of the duplicates searches once, and chooses among what it found when the threshold only chooses. */
class DuplicateFinderDialogTest{

	private static FLEFModel model(){
		final StringBuilder text = new StringBuilder("""
			header {
				protocol {
					name Family LEgacy Format
					version 0.2.0
				}
				date 2026-01-01T00:00:00Z
			}
			""");
		final String[] names = {"Anna", "Bianca", "Carla", "Dora", "Elena"};
		for(int pair = 0; pair < names.length; pair ++)
			for(int copy = 0; copy < 2; copy ++)
				text.append("individual I").append(pair * 2 + copy + 1).append(" {\n")
					.append("name {\npart {\ntype given\nvalue ").append(names[pair]).append("\n}\n")
					.append("part {\ntype family\nvalue Masutti\n}\n}\n")
					.append("}\n");
		return new FLEFParser().parse(text.toString());
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

	private static void waitFor(final BooleanSupplier condition) throws Exception{
		for(int i = 0; i < 200 && !condition.getAsBoolean(); i ++)
			Thread.sleep(50);
	}

	private static void setThreshold(final DuplicateFinderDialog dialog, final int threshold) throws Exception{
		SwingUtilities.invokeAndWait(() -> {
			try{
				final JSpinner spinner = (JSpinner)field(dialog, "thresholdSpinner");
				((javax.swing.SpinnerNumberModel)spinner.getModel()).setMinimum(0);
				spinner.setValue(threshold);
			}
			catch(final Exception e){
				throw new RuntimeException(e);
			}
		});
	}

	private static void search(final DuplicateFinderDialog dialog) throws Exception{
		SwingUtilities.invokeAndWait(() -> {
			try{
				final Method run = DuplicateFinderDialog.class.getDeclaredMethod("runSearch");
				run.setAccessible(true);
				run.invoke(dialog);
			}
			catch(final Exception e){
				throw new RuntimeException(e);
			}
		});
	}

	private static DuplicateFinderDialog open(final JFrame frame) throws Exception{
		final JPanel panel = new JPanel();
		frame.add(panel);
		frame.setSize(800, 600);
		frame.setVisible(true);
		final ToolContext context = new ToolContext(model(), "I1", panel, null);
		final AtomicReference<DuplicateFinderDialog> holder = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> holder.set(new DuplicateFinderDialog(context)));
		return holder.get();
	}

	@Test
	void aThresholdThatOnlyChoosesDoesNotSearchAgain() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
		final JFrame frame = new JFrame("main");
		final DuplicateFinderDialog dialog = open(frame);
		try{
			final JTable table = findTable(dialog.getContentPane());
			final JLabel status = (JLabel)field(dialog, "statusLabel");
			//the first search: with the threshold of the window, that these pairs do not reach
			waitFor(() -> !status.getText().isBlank());
			Assertions.assertEquals(1, dialog.computations());

			//a lower threshold needs the pairs that were not kept: the window says that it searches again, and it does, by itself
			setThreshold(dialog, 25);
			Assertions.assertEquals(I18N.t("text.duplicates.settings_changed"), status.getText());
			Assertions.assertEquals(1, dialog.computations());
			waitFor(() -> table.getRowCount() > 0);
			final int atTwentyFive = table.getRowCount();
			Assertions.assertEquals(2, dialog.computations());
			Assertions.assertTrue(atTwentyFive > 0);

			//a higher one chooses among them, at once, with no search
			setThreshold(dialog, 40);
			Assertions.assertEquals(2, dialog.computations());
			final int atForty = table.getRowCount();
			Assertions.assertTrue(atForty > 0 && atForty <= atTwentyFive, atForty + " of " + atTwentyFive);
			setThreshold(dialog, 400);
			Assertions.assertEquals(2, dialog.computations());
			Assertions.assertEquals(0, table.getRowCount());

			//and the search is not made again by pressing it for the same settings
			search(dialog);
			Assertions.assertEquals(2, dialog.computations());
			//nor for a threshold that is not lower than the one that was searched
			setThreshold(dialog, 30);
			search(dialog);
			Assertions.assertEquals(2, dialog.computations());
			Assertions.assertTrue(table.getRowCount() <= atTwentyFive);
		}
		finally{
			frame.dispose();
			SwingUtilities.invokeAndWait(dialog::dispose);
		}
	}

	@Test
	void aChangeMadeOfManyStepsIsOneSearch() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
		final JFrame frame = new JFrame("main");
		final DuplicateFinderDialog dialog = open(frame);
		try{
			final JTable table = findTable(dialog.getContentPane());
			final JLabel status = (JLabel)field(dialog, "statusLabel");
			waitFor(() -> !status.getText().isBlank());
			Assertions.assertEquals(1, dialog.computations());

			//the arrows of the spinner, one after the other: the pause starts again at each, and there is one search for all of them
			for(final int threshold : new int[]{50, 45, 40, 35, 30, 25})
				setThreshold(dialog, threshold);
			Assertions.assertEquals(1, dialog.computations());
			waitFor(() -> dialog.computations() == 2 && table.getRowCount() > 0);
			Thread.sleep(1000);
			Assertions.assertEquals(2, dialog.computations());
			Assertions.assertTrue(table.getRowCount() > 0);

			//a check box changes which pairs are compared: it is searched again too
			SwingUtilities.invokeAndWait(() -> {
				try{
					((javax.swing.JCheckBox)field(dialog, "skipExistingCheck")).doClick();
				}
				catch(final Exception e){
					throw new RuntimeException(e);
				}
			});
			waitFor(() -> dialog.computations() == 3);
			Assertions.assertEquals(3, dialog.computations());
		}
		finally{
			frame.dispose();
			SwingUtilities.invokeAndWait(dialog::dispose);
		}
	}

	@Test
	void theCountOfTheRowsIsAtTheBottomLeftAsInTheOtherWindows() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
		final JFrame frame = new JFrame("main");
		final DuplicateFinderDialog dialog = open(frame);
		try{
			final JLabel status = (JLabel)field(dialog, "statusLabel");
			//the label is in the part of the bar that is at the west of it, and the bar is at the bottom of the window
			final Container statusPanel = status.getParent();
			final Container footer = statusPanel.getParent();
			Assertions.assertEquals(BorderLayout.WEST, ((BorderLayout)footer.getLayout()).getConstraints(statusPanel));
			Assertions.assertEquals(BorderLayout.SOUTH, ((BorderLayout)dialog.getContentPane().getLayout()).getConstraints(footer));
		}
		finally{
			frame.dispose();
			SwingUtilities.invokeAndWait(dialog::dispose);
		}
	}

}
