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


/** The two compare buttons of the duplicates dialog open their windows. */
class DuplicateFinderCompareTest{

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

	private static FLEFModel big() throws Exception{
		final long t = System.currentTimeMillis();
		final FLEFModel m = new FLEFParser().parse(java.nio.file.Files.readString(java.nio.file.Path.of("C:/Users/MAUROS~1/AppData/Local/Temp/tmp-27888-Vhe7Y8n27Nre-TGMZ.flef")));
		System.out.println("PARSED ms " + (System.currentTimeMillis() - t));
		return m;
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

	private void check(final String method, final Class<?> expected) throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
		final JPanel panel = new JPanel();
		final javax.swing.JFrame frame = new javax.swing.JFrame("main");
		frame.add(panel);
		frame.setSize(800, 600);
		frame.setVisible(true);
		final ToolContext context = new ToolContext(big(), "I1", panel, null);
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
		System.out.println("PARENT VISIBLE " + dialog.isVisible());
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
		for(int i = 0; i < 2400 && table.getRowCount() == 0; i ++)
			Thread.sleep(50);
		System.out.println("ROWS " + table.getRowCount());
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
			for(int row = 0; row < Math.min(table.getRowCount(), 400); row ++){
				final int r = row;
				final long t0 = System.currentTimeMillis();
				SwingUtilities.invokeAndWait(() -> {
					try{
						table.setRowSelectionInterval(r, r);
						final Method m = DuplicateFinderDialog.class.getDeclaredMethod(method);
						m.setAccessible(true);
						m.invoke(dialog);
					}
					catch(final Exception e){
						errors.add(e);
					}
				});
				Thread.sleep(120);
				System.out.println("ROW " + r + " " + (System.currentTimeMillis() - t0) + " ms, shown " + shown.size());
			}
			Thread.sleep(1500);
		}
		finally{
			closer.stop();
			frame.dispose();
			SwingUtilities.invokeAndWait(dialog::dispose);
		}
		System.out.println("SHOWN " + shown + " ERRORS " + errors);
		errors.forEach(Throwable::printStackTrace);
		Assertions.assertTrue(errors.isEmpty(), errors.toString());
		Assertions.assertTrue(shown.stream().anyMatch(s -> s.endsWith(expected.getSimpleName())), shown.toString());
	}

	@Test
	void compareRecords() throws Exception{
		check("compareSelected", io.github.mtrevisan.familylegacy.ui.dialogs.RecordDiffDialog.class);
	}

	@Test
	void compareFacts() throws Exception{
		check("compareFactsOfSelected", FactsComparisonDialog.class);
	}

}
