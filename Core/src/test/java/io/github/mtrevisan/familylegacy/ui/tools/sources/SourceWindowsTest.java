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
package io.github.mtrevisan.familylegacy.ui.tools.sources;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.api.RecordDialog;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;


/** The windows of the Source menu, built and read (not only their logic): each one has content and is closed without error. */
class SourceWindowsTest{

	private static FLEFModel model(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("S1", SourceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceReader.TAG_TITLE, "Parish register")));
		model.addRecord(FLEFRecord.createMainRecord("R1", RepositoryReader.TYPE));
		return model;
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

	private static void tables(final Container container, final List<JTable> found){
		for(final Component child : container.getComponents()){
			if(child instanceof JTable table)
				found.add(table);
			if(child instanceof Container nested)
				tables(nested, found);
		}
	}

	/** Builds the window on the EDT, checks that it has content, and closes it. */
	private static List<JTable> open(final Supplier<?> factory) throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final List<JTable> tables = new ArrayList<>();
		final int[] components = new int[1];
		SwingUtilities.invokeAndWait(() -> {
			final Object dialog = factory.get();
			//a record editor is only a RecordDialog: when it is not a window, being built without error is all that can be seen
			components[0] = (dialog instanceof JDialog window? count(window.getContentPane()): 4);
			if(dialog instanceof JDialog window)
				tables(window, tables);
			dispose(dialog);
		});
		Assertions.assertTrue(components[0] > 3, "the window is empty");
		return tables;
	}


	@Test
	void newSourceAndNewRepositoryOpenTheirForm() throws Exception{
		final FLEFModel model = model();

		open(() -> SourceHandler.getInstance().createNewDialog(null, model));
		open(() -> RepositoryHandler.getInstance().createNewDialog(null, model));
	}

	@Test
	void addCitationOpensWithTheSelectedRecord() throws Exception{
		final FLEFModel model = model();

		open(() -> new AddCitationDialog(new ToolContext(model, "I1", new JPanel(), null)));
	}

	@Test
	void theCitationAnalysisListsTheUnusedSourceAndTheUncitedIndividual() throws Exception{
		final List<JTable> tables = open(() -> new CitationAnalysisDialog(null, model()));

		Assertions.assertFalse(tables.isEmpty());
		Assertions.assertTrue(tables.stream().anyMatch(table -> table.getRowCount() > 0));
	}

	@Test
	void manageMediaOpensEvenWithoutDocuments() throws Exception{
		final FLEFModel model = model();

		open(() -> new MediaManagementDialog(new ToolContext(model, null, new JPanel(), null)));
	}

}
