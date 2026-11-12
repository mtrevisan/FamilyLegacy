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
package io.github.mtrevisan.familylegacy.ui.tools.research;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/** The proof summary and the evidence qualifiers windows, driven: what the tabs list, and how the filters narrow it. */
class ResearchSummaryDialogsTest{

	private static FLEFRecord conclusion(final String id, final String status){
		final FLEFRecord record = FLEFRecord.createMainRecord(id, ConclusionReader.TYPE);
		if(status != null)
			record.addChild(FLEFRecord.createChildWithTagAndValue(ConclusionReader.TAG_PROOF_STATUS, status));
		return record;
	}

	private static <T extends Component> void find(final Container container, final Class<T> type, final List<T> found){
		for(final Component child : container.getComponents()){
			if(type.isInstance(child))
				found.add(type.cast(child));
			if(child instanceof Container nested)
				find(nested, type, found);
		}
	}


	@Test
	void theProofSummaryCountsEveryStatusAndTheFilterNarrowsTheConclusions() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = new FLEFModel();
		model.addRecord(conclusion("C1", ConclusionReader.ENUM_PROOF_STATUS_PROVEN));
		model.addRecord(conclusion("C2", ConclusionReader.ENUM_PROOF_STATUS_PROVEN));
		model.addRecord(conclusion("C3", ConclusionReader.ENUM_PROOF_STATUS_DISPROVEN));
		model.addRecord(conclusion("C4", null));
		final int[] sizes = new int[3];

		SwingUtilities.invokeAndWait(() -> {
			final ProofSummaryDialog dialog = new ProofSummaryDialog(null, model);
			final List<JTable> tables = new ArrayList<>();
			find(dialog, JTable.class, tables);
			final List<JComboBox<?>> combos = new ArrayList<>();
			@SuppressWarnings({"unchecked", "rawtypes"})
			final List<JComboBox> raw = (List)combos;
			find(dialog, JComboBox.class, raw);

			//4 declared statuses (even the empty ones) plus the one of the conclusions without a status
			sizes[0] = tables.getFirst().getRowCount();
			sizes[1] = tables.get(1).getRowCount();
			//"Proven" is the third declared status: the first entry of the filter is "all"
			combos.getFirst().setSelectedIndex(3);
			sizes[2] = tables.get(1).getRowCount();
			dialog.dispose();
		});

		Assertions.assertEquals(5, sizes[0]);
		Assertions.assertEquals(4, sizes[1]);
		Assertions.assertEquals(2, sizes[2]);
	}

	@Test
	void theEvidenceQualifiersListTheRecordsWithoutQualifiers() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = new FLEFModel();
		final FLEFRecord first = FLEFRecord.createMainRecord("I1", IndividualReader.TYPE);
		final FLEFRecord second = FLEFRecord.createMainRecord("I2", IndividualReader.TYPE);
		model.addRecord(first);
		model.addRecord(second);
		final int[] rows = new int[2];

		SwingUtilities.invokeAndWait(() -> {
			final EvidenceQualifiersDialog dialog = new EvidenceQualifiersDialog(null, model, Map.of("original", 3),
				Map.of(), Map.of(), 3, 2, List.of(first, second));
			final List<JTable> tables = new ArrayList<>();
			find(dialog, JTable.class, tables);
			//overview, source type (has a value), then the list of the records without qualifiers (the other two dimensions are empty)
			rows[0] = tables.size();
			rows[1] = tables.getLast().getRowCount();
			dialog.dispose();
		});

		Assertions.assertEquals(3, rows[0]);
		Assertions.assertEquals(2, rows[1]);
	}

}
