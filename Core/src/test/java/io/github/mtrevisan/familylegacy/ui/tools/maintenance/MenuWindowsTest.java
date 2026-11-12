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
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.quality.StatisticsCalculator;
import io.github.mtrevisan.familylegacy.reports.ReportType;
import io.github.mtrevisan.familylegacy.ui.api.RecordDialog;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.groups.GroupViewDialog;
import io.github.mtrevisan.familylegacy.ui.components.searches.service.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceRelationshipHandler;
import io.github.mtrevisan.familylegacy.ui.tools.maintenance.merge.MergeRecordsDialog;
import io.github.mtrevisan.familylegacy.ui.tools.reports.operations.ReportDialog;
import io.github.mtrevisan.familylegacy.ui.tools.statistics.StatisticsDialog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.Constructor;
import java.util.function.Supplier;


/** The windows of the Place, Reports and Tools menus, built and read: each one has content and is closed without error. */
class MenuWindowsTest{

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
			place P1 {
				name {
					value Treviso
				}
			}
			""");
	}

	private static void dispose(final Object dialog){
		if(dialog instanceof RecordDialog record)
			record.dispose();
		else
			((JDialog)dialog).dispose();
	}

	private static int count(final Container container){
		int count = 0;
		for(final Component child : container.getComponents()){
			count ++;
			if(child instanceof Container nested)
				count += count(nested);
		}
		return count;
	}

	private static void open(final Supplier<?> factory) throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final int[] components = new int[1];
		SwingUtilities.invokeAndWait(() -> {
			final Object dialog = factory.get();
			//a record editor is only a RecordDialog: when it is not a window, being built without error is all that can be seen
			components[0] = (dialog instanceof JDialog window? count(window.getContentPane()): 4);
			dispose(dialog);
		});
		Assertions.assertTrue(components[0] > 3, "the window is empty");
	}


	@Test
	void thePlaceWindowsOpen() throws Exception{
		final FLEFModel model = model();

		open(() -> RecordSelectionDialog.createForManagement(null, model, PlaceRelationshipHandler.class));
		open(() -> RecordSelectionDialog.createForManagement(null, model, PlaceHandler.class));
		open(() -> PlaceHandler.getInstance().createNewDialog(null, model));
		open(() -> GroupViewDialog.forPlace(new ToolContext(model, "P1", new JPanel(), null), "P1"));
	}

	@Test
	void theDuplicatesMergeAndStatisticsWindowsOpen() throws Exception{
		final FLEFModel model = model();
		final ToolContext context = new ToolContext(model, "I1", new JPanel(), null);

		open(() -> new DuplicateFinderDialog(context));
		open(() -> new MergeRecordsDialog(context, IndividualHandler.class));
		open(() -> new StatisticsDialog(null, new StatisticsCalculator(model).compute((percent, message) -> {}), model));
	}

	@Test
	void theReportDialogOpensForEveryReport() throws Exception{
		final FLEFModel model = model();
		final ToolContext context = new ToolContext(model, "I1", new JPanel(), null);
		final Constructor<ReportDialog> constructor = ReportDialog.class.getDeclaredConstructor(ToolContext.class, ReportType.class,
			FLEFRecord.class);
		constructor.setAccessible(true);

		for(final ReportType type : ReportType.values())
			open(() -> {
				try{
					return constructor.newInstance(context, type, model.getRecordById("P1"));
				}
				catch(final ReflectiveOperationException e){
					throw new IllegalStateException(type.name(), e);
				}
			});
	}

}
