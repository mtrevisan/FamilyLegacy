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
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.tools.reports.operations.AncestorReportOperation;
import io.github.mtrevisan.familylegacy.ui.tools.reports.operations.BibliographyOperation;
import io.github.mtrevisan.familylegacy.ui.tools.reports.operations.FamilyBookOperation;
import io.github.mtrevisan.familylegacy.ui.tools.reports.operations.FullBibliographyOperation;
import io.github.mtrevisan.familylegacy.ui.tools.reports.operations.PlaceReportOperation;
import io.github.mtrevisan.familylegacy.ui.tools.reports.operations.RelationshipReportOperation;
import io.github.mtrevisan.familylegacy.ui.tools.reports.operations.ResearchQuestionReportOperation;
import io.github.mtrevisan.familylegacy.ui.tools.reports.operations.SourceReportOperation;
import io.github.mtrevisan.familylegacy.ui.tools.reports.operations.SurnamesOperation;
import io.github.mtrevisan.familylegacy.ui.tools.reports.operations.ToVerifyOperation;
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

	@Test
	void theReportsOnAPlaceASourceOrAQuestionNeedOneInTheFile(){
		final FLEFModel without = new FLEFModel();
		without.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE));
		final FLEFModel with = new FLEFModel();
		with.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE));
		with.addRecord(FLEFRecord.createMainRecord("P1", PlaceReader.TYPE));
		with.addRecord(FLEFRecord.createMainRecord("S1", SourceReader.TYPE));
		with.addRecord(FLEFRecord.createMainRecord("RQ1", ResearchQuestionReader.TYPE));

		//no selection is needed: the user picks the record
		final ToolContext none = new ToolContext(without, null, null, null);
		final ToolContext all = new ToolContext(with, null, null, null);

		Assertions.assertFalse(new PlaceReportOperation().isEnabled(none));
		Assertions.assertFalse(new SourceReportOperation().isEnabled(none));
		Assertions.assertFalse(new ResearchQuestionReportOperation().isEnabled(none));
		Assertions.assertTrue(new PlaceReportOperation().isEnabled(all));
		Assertions.assertTrue(new SourceReportOperation().isEnabled(all));
		Assertions.assertTrue(new ResearchQuestionReportOperation().isEnabled(all));
	}

	@Test
	void theBooksNeedOnlyWhatTheyAreMadeOf(){
		final FLEFModel nothing = new FLEFModel();
		final FLEFModel people = new FLEFModel();
		people.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE));
		final FLEFModel sources = new FLEFModel();
		sources.addRecord(FLEFRecord.createMainRecord("S1", SourceReader.TYPE));

		Assertions.assertFalse(new FamilyBookOperation().isEnabled(new ToolContext(nothing, null, null, null)));
		Assertions.assertFalse(new FullBibliographyOperation().isEnabled(new ToolContext(nothing, null, null, null)));
		Assertions.assertTrue(new FamilyBookOperation().isEnabled(new ToolContext(people, null, null, null)));
		Assertions.assertFalse(new FullBibliographyOperation().isEnabled(new ToolContext(people, null, null, null)));
		Assertions.assertTrue(new FullBibliographyOperation().isEnabled(new ToolContext(sources, null, null, null)));
		Assertions.assertFalse(new FamilyBookOperation().isEnabled(new ToolContext(sources, null, null, null)));

		//to verify: whatever there is a person or a source to look at; surnames: a person
		Assertions.assertFalse(new ToVerifyOperation().isEnabled(new ToolContext(nothing, null, null, null)));
		Assertions.assertTrue(new ToVerifyOperation().isEnabled(new ToolContext(people, null, null, null)));
		Assertions.assertTrue(new ToVerifyOperation().isEnabled(new ToolContext(sources, null, null, null)));

		//surnames: whatever there is a person
		Assertions.assertFalse(new SurnamesOperation().isEnabled(new ToolContext(nothing, null, null, null)));
		Assertions.assertTrue(new SurnamesOperation().isEnabled(new ToolContext(people, null, null, null)));
		Assertions.assertFalse(new SurnamesOperation().isEnabled(new ToolContext(sources, null, null, null)));
	}

}
