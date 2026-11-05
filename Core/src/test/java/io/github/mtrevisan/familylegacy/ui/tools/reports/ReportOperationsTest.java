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
package io.github.mtrevisan.familylegacy.ui.tools.reports;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.tools.reports.operations.AncestorReportOperation;
import io.github.mtrevisan.familylegacy.ui.tools.reports.operations.BibliographyOperation;
import io.github.mtrevisan.familylegacy.ui.tools.reports.operations.RelationshipReportOperation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/** Every report is about a record: its command is available only when there is one to start from. */
class ReportOperationsTest{

	private static FLEFModel model(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("S1", SourceReader.TYPE));
		return model;
	}


	@Test
	void withoutASelectionNoReportIsAvailable(){
		final ToolContext context = new ToolContext(model(), null, null, null);

		Assertions.assertFalse(new AncestorReportOperation().isEnabled(context));
		Assertions.assertFalse(new RelationshipReportOperation().isEnabled(context));
		Assertions.assertFalse(new BibliographyOperation().isEnabled(context));
	}

	@Test
	void withAnIndividualSelectedTheReportsAreAvailable(){
		final ToolContext context = new ToolContext(model(), "I1", null, null);

		Assertions.assertTrue(new AncestorReportOperation().isEnabled(context));
		Assertions.assertTrue(new RelationshipReportOperation().isEnabled(context));
		Assertions.assertTrue(new BibliographyOperation().isEnabled(context));
	}

}
