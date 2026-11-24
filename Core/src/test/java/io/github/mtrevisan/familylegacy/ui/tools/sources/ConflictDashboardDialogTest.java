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

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;


/** The conflict dashboard, built on a real model: the conflicts and the open conclusions are listed, and the competing assertions shown. */
class ConflictDashboardDialogTest{

	private static final String GEDCOM = """
		0 HEAD
		1 CHAR UTF-8
		0 @I1@ INDI
		1 NAME Anna /Rossi/
		1 SEX F
		1 BIRT
		2 DATE 1 JAN 1850
		1 BIRT
		2 DATE 1 JAN 1860
		""";


	private static FLEFModel model() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(GEDCOM));
		return new GEDCOMToFLEFConverter().convert(roots);
	}

	private static void tables(final Container container, final List<JTable> found){
		for(final Component child : container.getComponents()){
			if(child instanceof JTable table)
				found.add(table);
			if(child instanceof Container nested)
				tables(nested, found);
		}
	}


	@Test
	void theConflictsAndTheOpenConclusionsAreListedAndSelectingOneShowsItsAssertions() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = model();
		//a conclusion that evaluates nothing yet
		model.addRecord(FLEFRecord.createMainRecord("C1", ConclusionReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(ConclusionReader.TAG_ISSUE, "birth_date")));

		final int[] rows = new int[3];
		SwingUtilities.invokeAndWait(() -> {
			final ConflictDashboardDialog dialog = new ConflictDashboardDialog(null, model);
			final List<JTable> tables = new ArrayList<>();
			tables(dialog, tables);
			Assertions.assertEquals(2, tables.size());

			final JTable entries = tables.get(0);
			final JTable assertions = tables.get(1);
			rows[0] = entries.getRowCount();
			rows[1] = assertions.getRowCount();
			//the unresolved births come first
			entries.setRowSelectionInterval(0, 0);
			rows[2] = assertions.getRowCount();
			dialog.dispose();
		});

		Assertions.assertEquals(2, rows[0]);
		Assertions.assertEquals(0, rows[1]);
		Assertions.assertEquals(2, rows[2]);
	}

	@Test
	void anEmptyFileHasNothingInConflict() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");

		final int[] rows = new int[1];
		SwingUtilities.invokeAndWait(() -> {
			final ConflictDashboardDialog dialog = new ConflictDashboardDialog(null, new FLEFModel());
			final List<JTable> tables = new ArrayList<>();
			tables(dialog, tables);
			rows[0] = tables.getFirst().getRowCount();
			dialog.dispose();
		});

		Assertions.assertEquals(0, rows[0]);
	}

}
