package io.github.mtrevisan.familylegacy.ui.assistant.conclusion;

import io.github.mtrevisan.familylegacy.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.gedcom.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.ConclusionRecordDialog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;


/** The editor of the conclusion, prepared as the "Create Conclusion Draft" action does (without showing it). */
class ConclusionDraftDialogTest{

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


	private static FLEFModel convert() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(GEDCOM));
		return new GEDCOMToFLEFConverter().convert(roots);
	}


	@Test
	void theDraftIsSavedOnlyIfTheResearcherSavesIt() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The editor needs a display");
		final FLEFModel model = convert();
		final ConclusionFinding finding = ConclusionChecker.findings(ConclusionDataLoader.load(model)).getFirst();
		Assertions.assertEquals("birth_date", finding.draft().issue());

		//what the action does, but for showing the editor
		final ConclusionRecordDialog dialog = ConclusionRecordDialog.createNew(null, model)
			.withDraft("unresearched", finding.draft().narrative());
		for(final String id : finding.draft().resolves())
			dialog.withResolvedTarget(model.getRecordById(id), finding.draft().issue());

		//the draft is in the editor only: no record in the model, the conflict is still open
		Assertions.assertTrue(model.getRecordsByType("conclusion").isEmpty());
		Assertions.assertFalse(dialog.isSaved());
		Assertions.assertEquals(1, ConclusionChecker.check(ConclusionDataLoader.load(model)).size());

		//the researcher saves it as it is
		dialog.save();

		Assertions.assertTrue(dialog.isSaved());
		final List<FLEFRecord> conclusions = model.getRecordsByType("conclusion");
		Assertions.assertEquals(1, conclusions.size());
		final FLEFRecord conclusion = conclusions.getFirst();
		Assertions.assertEquals("birth_date", FLEFRecordHelper.getChildValue(conclusion, "issue"));
		Assertions.assertEquals("unresearched", FLEFRecordHelper.getChildValue(conclusion, "proof_status"));
		Assertions.assertTrue(FLEFRecordHelper.getChildValue(conclusion, "narrative").startsWith("Conflicting birth dates: Anna"));
		Assertions.assertEquals(2, FLEFRecordHelper.findChildren(conclusion, "resolves").size());
		Assertions.assertNull(FLEFRecordHelper.findChild(conclusion, "preferred"));

		//a draft without a judgement does not settle the conflict
		Assertions.assertEquals(1, ConclusionChecker.check(ConclusionDataLoader.load(model)).size());
	}

}
