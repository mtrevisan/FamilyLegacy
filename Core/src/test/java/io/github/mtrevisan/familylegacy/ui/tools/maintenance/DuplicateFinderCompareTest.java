package io.github.mtrevisan.familylegacy.ui.tools.maintenance;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


/**
 * The button that compares the facts of the duplicates dialog opens its window (the comparison of the records is the review of the
 * merge, that the other buttons open).
 * <p>
 * Only the notable selections are tried (the first row, the last, one in the middle, all of them), on a small file made here: every
 * window that is opened and closed costs a moment, and the rows of a large file add nothing the few ones do not already show.
 */
class DuplicateFinderCompareTest{

	/** The individuals of the file: more than the six that the comparison of the facts can show, in pairs of the same name. */
	private static final int PAIRS = 5;


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
		for(int pair = 0; pair < PAIRS; pair ++)
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

	/** The rows to select, as the first and the last of each selection: the first, the last, one in the middle, all of them. */
	private static List<int[]> notableSelections(final int rows){
		return List.of(new int[]{0, 0}, new int[]{rows - 1, rows - 1}, new int[]{rows / 2, rows / 2}, new int[]{0, rows - 1});
	}

	private void check(final String method, final Class<?> expected) throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
		final JPanel panel = new JPanel();
		final javax.swing.JFrame frame = new javax.swing.JFrame("main");
		frame.add(panel);
		frame.setSize(800, 600);
		frame.setVisible(true);
		final ToolContext context = new ToolContext(model(), "I1", panel, null);
		final AtomicReference<DuplicateFinderDialog> holder = new AtomicReference<>();
		final List<String> shown = new ArrayList<>();
		final List<Throwable> errors = new ArrayList<>();
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> errors.add(e));

		SwingUtilities.invokeLater(() -> {
			final DuplicateFinderDialog d = new DuplicateFinderDialog(context);
			holder.set(d);
			d.setVisible(true);
		});
		for(int i = 0; i < 100 && (holder.get() == null || !holder.get().isVisible()); i ++)
			Thread.sleep(50);
		final DuplicateFinderDialog dialog = holder.get();
		final JTable table = findTable(dialog.getContentPane());
		SwingUtilities.invokeAndWait(() -> {
			try{
				final java.lang.reflect.Field f = DuplicateFinderDialog.class.getDeclaredField("thresholdSpinner");
				f.setAccessible(true);
				final javax.swing.JSpinner spinner = (javax.swing.JSpinner)f.get(dialog);
				((javax.swing.SpinnerNumberModel)spinner.getModel()).setMinimum(0);
				spinner.setValue(25);
				final Method run = DuplicateFinderDialog.class.getDeclaredMethod("runSearch");
				run.setAccessible(true);
				run.invoke(dialog);
			}
			catch(final Exception e){
				throw new RuntimeException(e);
			}
		});
		for(int i = 0; i < 200 && table.getRowCount() == 0; i ++)
			Thread.sleep(50);
		Assertions.assertTrue(table.getRowCount() > 0);

		final Timer closer = new Timer(100, e -> {
			for(final Window window : Window.getWindows())
				if(window.isVisible() && window != dialog && window != frame){
					shown.add(window.getClass().getName());
					window.dispose();
				}
		});
		closer.start();
		try{
			for(final int[] selection : notableSelections(table.getRowCount())){
				SwingUtilities.invokeAndWait(() -> {
					try{
						table.setRowSelectionInterval(selection[0], selection[1]);
						final Method m = DuplicateFinderDialog.class.getDeclaredMethod(method);
						m.setAccessible(true);
						m.invoke(dialog);
					}
					catch(final Exception e){
						errors.add(e);
					}
				});
				Thread.sleep(150);
			}
			Thread.sleep(300);
		}
		finally{
			closer.stop();
			frame.dispose();
			SwingUtilities.invokeAndWait(dialog::dispose);
		}

		errors.forEach(Throwable::printStackTrace);
		Assertions.assertTrue(errors.isEmpty(), errors.toString());
		Assertions.assertTrue(shown.stream().anyMatch(s -> s.endsWith(expected.getSimpleName())), shown.toString());
	}

	@Test
	void compareFacts() throws Exception{
		check("compareFactsOfSelected", FactsComparisonDialog.class);
	}

}
